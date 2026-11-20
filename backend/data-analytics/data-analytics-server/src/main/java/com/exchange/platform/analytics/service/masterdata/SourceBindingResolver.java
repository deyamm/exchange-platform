package com.exchange.platform.analytics.service.masterdata;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exchange.platform.analytics.api.enums.MappingMode;
import com.exchange.platform.analytics.api.enums.MasterDataType;
import com.exchange.platform.analytics.api.enums.ReadinessStatus;
import com.exchange.platform.analytics.api.registry.MasterDataTypeDefinition;
import com.exchange.platform.analytics.api.registry.TargetFieldDefinition;
import com.exchange.platform.analytics.entity.masterdata.MdSourceBinding;
import com.exchange.platform.analytics.entity.masterdata.MdSourceFieldMapping;
import com.exchange.platform.analytics.mapper.masterdata.MdSourceBindingMapper;
import com.exchange.platform.analytics.mapper.masterdata.MdSourceFieldMappingMapper;
import com.exchange.platform.analytics.service.support.CollectionDatasetMetadataClient;
import com.exchange.platform.common.core.enums.MarketAssetType;
import com.exchange.platform.common.core.exception.BusinessException;

/**
 * 把来源绑定配置解析为可执行的 {@link PublishPlan}。
 *
 * <p>
 * 这个组件是配置“解析/编译器”：
 * 它读取 Binding、Mapping 和 Dataset 元数据，验证它们之间的就否符合发布的规则，
 * 并在配置完整时生成后续发布唯一使用的 PublishPlan。
 * </p>
 *
 * <p>
 * SourceBindingService 用它展示 readiness；
 * 发布应用服务也通过它取得 requireReady 的执行计划。
 * </p>
 */
@Component
public class SourceBindingResolver {

    private final MdSourceBindingMapper bindingMapper;
    private final MdSourceFieldMappingMapper mappingMapper;
    private final MasterDataTypeRegistry registry;
    private final CollectionDatasetMetadataClient metadataClient;

    public SourceBindingResolver(
            MdSourceBindingMapper bindingMapper,
            MdSourceFieldMappingMapper mappingMapper,
            MasterDataTypeRegistry registry,
            CollectionDatasetMetadataClient metadataClient) {

        this.bindingMapper = bindingMapper;
        this.mappingMapper = mappingMapper;
        this.registry = registry;
        this.metadataClient = metadataClient;
    }

    /**
     * 按 bindingId 解析 source binding。
     *
     * @throws BusinessException Binding 不存在
     */
    public Resolution resolve(Long bindingId) {
        return resolve(requireBinding(bindingId));
    }

    /**
     * 解析已经读取出的 Binding。
     *
     * <p>
     * 该重载用于列表/详情场景，避免为了 readiness 再查询一次 Binding。
     * </p>
     */
    public Resolution resolve(MdSourceBinding binding) {
        List<String> errors = new ArrayList<>();

        if (!Boolean.TRUE.equals(binding.getEnabled())) {
            errors.add("来源已停用");
        }

        MasterDataTypeDefinition definition =
                registry.get(binding.getMasterDataType());

        if (definition == null) {
            errors.add("MasterDataType 未注册");
        }

        CollectionDatasetMetadataClient.DatasetBinding dataset =
                loadDataset(binding.getDatasetCode(), errors);

        Set<String> datasetFields = datasetFieldCodes(dataset);

        List<MdSourceFieldMapping> persistedMappings =
                mappingMapper.selectList(
                        new LambdaQueryWrapper<MdSourceFieldMapping>()
                                .eq(
                                        MdSourceFieldMapping::getBindingId,
                                        binding.getId())
                                .orderByAsc(
                                        MdSourceFieldMapping::getTargetField));

        Map<String, MdSourceFieldMapping> mappingByTarget =
                indexMappings(persistedMappings, errors);

        validateMappings(
                definition,
                persistedMappings,
                mappingByTarget,
                datasetFields,
                errors);

        PublishPlan plan = errors.isEmpty()
                ? buildPlan(binding, persistedMappings)
                : null;

        ReadinessStatus status;
        if (errors.isEmpty()) {
            status = ReadinessStatus.READY;
        } else if (!Boolean.TRUE.equals(binding.getEnabled())) {
            status = ReadinessStatus.DISABLED;
        } else {
            status = ReadinessStatus.INVALID;
        }

        return new Resolution(status, errors, plan);
    }

    /**
     * 获取可执行 PublishPlan。
     *
     * <p>
     * readiness 未通过时直接抛出业务异常，调用方不会创建执行批次。
     * </p>
     */
    public PublishPlan requireReady(Long bindingId) {
        Resolution resolution = resolve(bindingId);

        if (!resolution.ready()) {
            throw new BusinessException(
                    40001,
                    "来源不可发布：" + String.join("；", resolution.errors()));
        }

        return resolution.plan();
    }

    private CollectionDatasetMetadataClient.DatasetBinding loadDataset(
            String datasetCode,
            List<String> errors) {

        if (!StringUtils.hasText(datasetCode)) {
            errors.add("datasetCode 不能为空");
            return null;
        }

        try {
            CollectionDatasetMetadataClient.DatasetBinding dataset =
                    metadataClient
                            .getBindings(List.of(datasetCode))
                            .get(datasetCode);

            if (dataset == null) {
                errors.add("Dataset 不存在或不可用");
            }

            return dataset;
        } catch (RuntimeException ex) {
            errors.add("Dataset 元数据不可读：" + safeMessage(ex));
            return null;
        }
    }

    private Set<String> datasetFieldCodes(
            CollectionDatasetMetadataClient.DatasetBinding dataset) {

        if (dataset == null || dataset.fields() == null) {
            return Set.of();
        }

        Set<String> fields = new LinkedHashSet<>();

        for (CollectionDatasetMetadataClient.FieldDefinition field
                : dataset.fields()) {

            if (field != null
                    && StringUtils.hasText(field.fieldCode())) {
                fields.add(field.fieldCode());
            }
        }

        return fields;
    }

    /**
     * 以 targetField 建索引，并识别数据库中可能遗留的重复配置。
     */
    private Map<String, MdSourceFieldMapping> indexMappings(
            List<MdSourceFieldMapping> mappings,
            List<String> errors) {

        Map<String, MdSourceFieldMapping> result =
                new LinkedHashMap<>();

        for (MdSourceFieldMapping mapping : mappings) {
            String targetField = mapping.getTargetField();

            if (!StringUtils.hasText(targetField)) {
                errors.add("存在 targetField 为空的字段映射");
                continue;
            }
            
            // 如果不 absent，会返回之前的 mapping，说明数据库中存在重复配置
            MdSourceFieldMapping previous =
                    result.putIfAbsent(targetField, mapping);

            if (previous != null) {
                errors.add("targetField 重复：" + targetField);
            }
        }

        return result;
    }

    private void validateMappings(
            MasterDataTypeDefinition definition,
            List<MdSourceFieldMapping> mappings,
            Map<String, MdSourceFieldMapping> mappingByTarget,
            Set<String> datasetFields,
            List<String> errors) {

        if (definition == null) {
            return;
        }

        Map<String, TargetFieldDefinition> definitionByField =
                new LinkedHashMap<>();

        for (TargetFieldDefinition field : definition.fields()) {
            definitionByField.put(field.fieldCode(), field);
        }

        /*
         * saveMappings 正常情况下不会写入未注册字段，
         * Resolver 仍要能识别历史数据、人工改库等异常。
         */
        for (MdSourceFieldMapping mapping : mappings) {
            if (!definitionByField.containsKey(
                    mapping.getTargetField())) {

                errors.add(
                        "targetField 未注册："
                                + mapping.getTargetField());
            }
        }

        for (TargetFieldDefinition field : definition.fields()) {
            MdSourceFieldMapping mapping =
                    mappingByTarget.get(field.fieldCode());

            if (mapping == null) {
                if (field.required()) {
                    errors.add(
                            "缺少 required mapping: "
                                    + field.fieldCode());
                }
                continue;
            }

            if (mapping.getMappingMode() == null) {
                errors.add(
                        "mappingMode 不能为空: "
                                + field.fieldCode());
                continue;
            }

            if (mapping.getMappingMode() == MappingMode.FIELD) {
                if (!StringUtils.hasText(
                        mapping.getSourceFieldCode())) {

                    errors.add(
                            "FIELD sourceFieldCode 不能为空: "
                                    + field.fieldCode());

                } else if (!datasetFields.contains(
                        mapping.getSourceFieldCode())) {

                    errors.add(
                            "Dataset 不存在 fieldCode: "
                                    + mapping.getSourceFieldCode());
                }

                if (StringUtils.hasText(
                        mapping.getConstantValue())) {

                    errors.add(
                            "FIELD mapping 不应同时填写 constantValue: "
                                    + field.fieldCode());
                }
            }

            if (mapping.getMappingMode() == MappingMode.CONSTANT) {
                if (!StringUtils.hasText(
                        mapping.getConstantValue())) {

                    errors.add(
                            "CONSTANT 值不能为空: "
                                    + field.fieldCode());
                }

                if (StringUtils.hasText(
                        mapping.getSourceFieldCode())) {

                    errors.add(
                            "CONSTANT mapping 不应同时填写 sourceFieldCode: "
                                    + field.fieldCode());
                }
            }
        }
    }

    private PublishPlan buildPlan(
            MdSourceBinding binding,
            List<MdSourceFieldMapping> persistedMappings) {

        List<PublishPlan.FieldMapping> fieldMappings =
                persistedMappings.stream()
                        .map(mapping ->
                                new PublishPlan.FieldMapping(
                                        mapping.getTargetField(),
                                        mapping.getMappingMode(),
                                        mapping.getSourceFieldCode(),
                                        mapping.getConstantValue()))
                        .toList();

        List<String> sourceFieldCodes =
                persistedMappings.stream()
                        .filter(mapping ->
                                mapping.getMappingMode()
                                        == MappingMode.FIELD)
                        .map(MdSourceFieldMapping::getSourceFieldCode)
                        .filter(StringUtils::hasText)
                        .distinct()
                        .toList();

        return new PublishPlan(
                binding.getId(),
                binding.getBindingCode(),
                binding.getBindingName(),
                binding.getMasterDataType(),
                marketAssetTypeOf(binding.getMasterDataType()),
                binding.getDatasetCode(),
                sourceFieldCodes,
                fieldMappings);
    }

    private MdSourceBinding requireBinding(Long bindingId) {
        MdSourceBinding binding =
                bindingMapper.selectById(bindingId);

        if (binding == null) {
            throw new BusinessException(
                    40004,
                    "来源绑定不存在：" + bindingId);
        }

        return binding;
    }

    /**
     * 关系主数据不是独立 MarketAsset，因此返回 null。
     * Executor 会在关系分支直接处理，不会使用 marketAssetType。
     */
    private MarketAssetType marketAssetTypeOf(
            MasterDataType type) {

        if (type == null) {
            return null;
        }

        return switch (type) {
            case MARKET_ASSET_STOCK -> MarketAssetType.STOCK;
            case MARKET_ASSET_INDEX -> MarketAssetType.INDEX;
            case MARKET_ASSET_REL_INDEX_MEMBER -> null;
        };
    }

    private String safeMessage(RuntimeException ex) {
        return ex.getMessage() == null
                ? ex.getClass().getSimpleName()
                : ex.getMessage();
    }

    /**
     * Binding 解析结果。只有 READY 状态才会包含 plan。
     */
    public record Resolution(
            ReadinessStatus status,
            List<String> errors,
            PublishPlan plan) {

        public Resolution {
            errors = errors == null
                    ? List.of()
                    : List.copyOf(errors);
        }

        public boolean ready() {
            return status == ReadinessStatus.READY
                    && plan != null;
        }
    }
}
