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
import com.exchange.platform.analytics.api.enums.ReadinessStatus;
import com.exchange.platform.analytics.api.registry.MasterDataTypeDefinition;
import com.exchange.platform.analytics.api.registry.MdFieldDefinition;
import com.exchange.platform.analytics.entity.masterdata.MdSourceBinding;
import com.exchange.platform.analytics.entity.masterdata.MdSourceFieldMapping;
import com.exchange.platform.analytics.mapper.masterdata.MdSourceBindingMapper;
import com.exchange.platform.analytics.mapper.masterdata.MdSourceFieldMappingMapper;
import com.exchange.platform.analytics.service.support.CollectionDatasetMetadataClient;
import com.exchange.platform.collection.api.response.dataType.DataTypeFieldView;
import com.exchange.platform.collection.api.response.execution.MetricDatasetConfigView;
import com.exchange.platform.common.core.exception.BusinessException;

/**
 * 将来源绑定配置解析为可执行的 {@link PublishPlan}。
 *
 * <p>
 * Resolver 是 SourceBinding 配置合法性的唯一判断入口：保存 Mapping 前的校验、 readiness
 * 展示以及正式发布都复用同一套规则，避免 Service 与 Resolver 各维护 一份字段校验逻辑。
 * </p>
 */
@Component
public class SourceBindingResolver {

    private final MdSourceBindingMapper bindingMapper;
    private final MdSourceFieldMappingMapper mappingMapper;
    private final MasterDataTypeRegistry registry;
    private final CollectionDatasetMetadataClient metadataClient;

    public SourceBindingResolver(MdSourceBindingMapper bindingMapper, MdSourceFieldMappingMapper mappingMapper,
            MasterDataTypeRegistry registry, CollectionDatasetMetadataClient metadataClient) {

        this.bindingMapper = bindingMapper;
        this.mappingMapper = mappingMapper;
        this.registry = registry;
        this.metadataClient = metadataClient;
    }

    /** 按 bindingId 解析来源绑定。 */
    public Resolution resolve(Long bindingId) {
        return resolve(requireBinding(bindingId));
    }

    /**
     * 解析已经读取出的 Binding，用于列表/详情场景，避免再次查询 Binding 实体。
     */
    public Resolution resolve(MdSourceBinding binding) {
        List<MdSourceFieldMapping> mappings = loadMappings(binding.getId());
        List<String> errors = new ArrayList<>();

        if (!Boolean.TRUE.equals(binding.getEnabled())) {
            errors.add("来源已停用");
        }

        errors.addAll(validateMappings(binding, mappings));

        PublishPlan plan = errors.isEmpty() ? buildPlan(binding, mappings) : null;

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
     * 校验一组待保存或已持久化的字段映射。
     *
     * <p>
     * 该方法只检查“类型定义 + Dataset 元数据 + Mapping”是否匹配，不检查 Binding 是否
     * enabled。这样停用状态的来源仍然可以先修复字段映射，再重新启用。
     * </p>
     */
    public List<String> validateMappings(MdSourceBinding binding, List<MdSourceFieldMapping> mappings) {

        List<String> errors = new ArrayList<>();

        MasterDataTypeDefinition definition = registry.get(binding.getMasterDataType());

        if (definition == null) {
            errors.add("未注册的 MasterDataType: " + binding.getMasterDataType());
            return List.copyOf(errors);
        }

        MetricDatasetConfigView dataset = loadDataset(binding.getDatasetCode(), errors);

        Set<String> datasetFields = datasetFieldCodes(dataset);
        List<MdSourceFieldMapping> safeMappings = mappings == null ? List.of() : mappings;

        Map<String, MdSourceFieldMapping> mappingByTarget = indexMappings(safeMappings, errors);

        validateMappings(definition, safeMappings, mappingByTarget, datasetFields, errors);

        return List.copyOf(errors);
    }

    /** readiness 未通过时直接阻止创建执行批次。 */
    public PublishPlan requireReady(Long bindingId) {
        Resolution resolution = resolve(bindingId);

        if (!resolution.ready()) {
            throw new BusinessException(40001, "来源不可发布：" + String.join("；", resolution.errors()));
        }

        return resolution.plan();
    }

    private List<MdSourceFieldMapping> loadMappings(Long bindingId) {
        return mappingMapper.selectList(new LambdaQueryWrapper<MdSourceFieldMapping>()
                .eq(MdSourceFieldMapping::getBindingId, bindingId).orderByAsc(MdSourceFieldMapping::getTargetField));
    }

    private MetricDatasetConfigView loadDataset(String datasetCode, List<String> errors) {

        if (!StringUtils.hasText(datasetCode)) {
            errors.add("datasetCode 不能为空");
            return null;
        }

        try {
            MetricDatasetConfigView dataset = metadataClient
                    .getDatasetsForMetricConfiguration(List.of(datasetCode)).get(datasetCode);

            if (dataset == null) {
                errors.add("Dataset 不存在或不可用");
            }

            return dataset;
        } catch (RuntimeException ex) {
            errors.add("Dataset 元数据不可读：" + safeMessage(ex));
            return null;
        }
    }

    private Set<String> datasetFieldCodes(MetricDatasetConfigView dataset) {

        if (dataset == null || dataset.getFields() == null) {
            return Set.of();
        }

        Set<String> fields = new LinkedHashSet<>();

        for (DataTypeFieldView field : dataset.getFields()) {
            if (field != null && StringUtils.hasText(field.getFieldCode())) {
                fields.add(field.getFieldCode());
            }
        }

        return fields;
    }

    /** 以 targetField 建索引，并识别数据库中可能遗留的重复配置。 */
    private Map<String, MdSourceFieldMapping> indexMappings(List<MdSourceFieldMapping> mappings, List<String> errors) {

        Map<String, MdSourceFieldMapping> result = new LinkedHashMap<>();

        for (MdSourceFieldMapping mapping : mappings) {
            String targetField = mapping.getTargetField();

            if (!StringUtils.hasText(targetField)) {
                errors.add("存在 targetField 为空的字段映射");
                continue;
            }

            MdSourceFieldMapping previous = result.putIfAbsent(targetField, mapping);
            if (previous != null) {
                errors.add("targetField 重复：" + targetField);
            }
        }

        return result;
    }

    private void validateMappings(MasterDataTypeDefinition definition, List<MdSourceFieldMapping> mappings,
            Map<String, MdSourceFieldMapping> mappingByTarget, Set<String> datasetFields, List<String> errors) {

        Map<String, MdFieldDefinition> definitionByField = new LinkedHashMap<>();
        for (MdFieldDefinition field : definition.fields()) {
            definitionByField.put(field.fieldCode(), field);
        }

        for (MdSourceFieldMapping mapping : mappings) {
            if (!definitionByField.containsKey(mapping.getTargetField())) {
                errors.add("targetField 未注册：" + mapping.getTargetField());
            }
        }

        for (MdFieldDefinition field : definition.fields()) {
            MdSourceFieldMapping mapping = mappingByTarget.get(field.fieldCode());

            if (mapping == null) {
                if (field.required()) {
                    errors.add("缺少 required mapping: " + field.fieldCode());
                }
                continue;
            }

            if (mapping.getMappingMode() == null) {
                errors.add("mappingMode 不能为空: " + field.fieldCode());
                continue;
            }

            if (mapping.getMappingMode() == MappingMode.FIELD) {
                validateFieldMapping(field, mapping, datasetFields, errors);
            } else if (mapping.getMappingMode() == MappingMode.CONSTANT) {
                validateConstantMapping(field, mapping, errors);
            }
        }
    }

    private void validateFieldMapping(MdFieldDefinition field, MdSourceFieldMapping mapping, Set<String> datasetFields,
            List<String> errors) {

        if (!StringUtils.hasText(mapping.getSourceFieldCode())) {
            errors.add("FIELD sourceFieldCode 不能为空: " + field.fieldCode());
        } else if (!datasetFields.contains(mapping.getSourceFieldCode())) {
            errors.add("Dataset 不存在 fieldCode: " + mapping.getSourceFieldCode());
        }

        if (StringUtils.hasText(mapping.getConstantValue())) {
            errors.add("FIELD mapping 不应同时填写 constantValue: " + field.fieldCode());
        }
    }

    private void validateConstantMapping(MdFieldDefinition field, MdSourceFieldMapping mapping, List<String> errors) {

        if (!StringUtils.hasText(mapping.getConstantValue())) {
            errors.add("CONSTANT 值不能为空: " + field.fieldCode());
        }

        if (StringUtils.hasText(mapping.getSourceFieldCode())) {
            errors.add("CONSTANT mapping 不应同时填写 sourceFieldCode: " + field.fieldCode());
        }
    }

    private PublishPlan buildPlan(MdSourceBinding binding, List<MdSourceFieldMapping> persistedMappings) {

        List<PublishPlan.FieldMapping> fieldMappings = persistedMappings.stream()
                .map(mapping -> new PublishPlan.FieldMapping(mapping.getTargetField(), mapping.getMappingMode(),
                        mapping.getSourceFieldCode(), mapping.getConstantValue()))
                .toList();

        List<String> sourceFieldCodes = persistedMappings.stream()
                .filter(mapping -> mapping.getMappingMode() == MappingMode.FIELD)
                .map(MdSourceFieldMapping::getSourceFieldCode).filter(StringUtils::hasText).distinct().toList();

        return new PublishPlan(binding.getId(), binding.getBindingCode(), binding.getMasterDataType(),
                binding.getDatasetCode(), sourceFieldCodes, fieldMappings);
    }

    private MdSourceBinding requireBinding(Long bindingId) {
        MdSourceBinding binding = bindingMapper.selectById(bindingId);

        if (binding == null) {
            throw new BusinessException(40004, "来源绑定不存在：" + bindingId);
        }

        return binding;
    }

    private String safeMessage(RuntimeException ex) {
        return ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
    }

    /** Binding 解析结果。只有 READY 状态才包含 plan。 */
    public record Resolution(ReadinessStatus status, List<String> errors, PublishPlan plan) {

        public Resolution {
            errors = errors == null ? List.of() : List.copyOf(errors);
        }

        public boolean ready() {
            return status == ReadinessStatus.READY && plan != null;
        }
    }
}
