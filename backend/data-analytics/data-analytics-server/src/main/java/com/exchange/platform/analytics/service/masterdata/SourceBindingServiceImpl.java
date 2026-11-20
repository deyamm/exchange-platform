package com.exchange.platform.analytics.service.masterdata;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exchange.platform.analytics.api.enums.MappingMode;
import com.exchange.platform.analytics.api.enums.TriggerType;
import com.exchange.platform.analytics.api.registry.MasterDataTypeDefinition;
import com.exchange.platform.analytics.api.registry.TargetFieldDefinition;
import com.exchange.platform.analytics.api.request.SourceBindingSaveRequest;
import com.exchange.platform.analytics.api.request.SourceFieldMappingSaveRequest;
import com.exchange.platform.analytics.api.response.ReadinessView;
import com.exchange.platform.analytics.api.response.SourceBindingView;
import com.exchange.platform.analytics.api.response.SourceFieldMappingView;
import com.exchange.platform.analytics.api.response.SyncBatchView;
import com.exchange.platform.analytics.entity.masterdata.MdSourceBinding;
import com.exchange.platform.analytics.entity.masterdata.MdSourceFieldMapping;
import com.exchange.platform.analytics.mapper.masterdata.MdSourceBindingMapper;
import com.exchange.platform.analytics.mapper.masterdata.MdSourceFieldMappingMapper;
import com.exchange.platform.analytics.service.support.CollectionDatasetMetadataClient;
import com.exchange.platform.common.core.exception.BusinessException;
import com.exchange.platform.common.core.page.PageResult;

/**
 * 来源绑定配置管理服务。
 *
 * <p>
 * 本类负责 Binding/Mapping 的 CRUD 和配置页面展示，不再承担发布编排。
 * readiness 统一委托给 {@link SourceBindingResolver}，发布统一委托给
 * {@link MasterDataPublishApplicationService}。
 * </p>
 *
 * <p>
 * 因此依赖方向为：
 * SourceBindingServiceImpl -> Resolver / ApplicationService。
 * Resolver 和 ApplicationService 都不会反向依赖本类，循环依赖被消除。
 * </p>
 */
@Service
public class SourceBindingServiceImpl implements SourceBindingService {

    private static final Logger log =
            LoggerFactory.getLogger(SourceBindingServiceImpl.class);

    private final MdSourceBindingMapper bindingMapper;
    private final MdSourceFieldMappingMapper mappingMapper;
    private final MasterDataTypeRegistry registry;
    private final CollectionDatasetMetadataClient metadataClient;
    private final SourceBindingResolver bindingResolver;
    private final MasterDataPublishApplicationService publishApplicationService;

    public SourceBindingServiceImpl(
            MdSourceBindingMapper bindingMapper,
            MdSourceFieldMappingMapper mappingMapper,
            MasterDataTypeRegistry registry,
            CollectionDatasetMetadataClient metadataClient,
            SourceBindingResolver bindingResolver,
            MasterDataPublishApplicationService publishApplicationService) {

        this.bindingMapper = bindingMapper;
        this.mappingMapper = mappingMapper;
        this.registry = registry;
        this.metadataClient = metadataClient;
        this.bindingResolver = bindingResolver;
        this.publishApplicationService = publishApplicationService;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<SourceBindingView> list(
            String keyword,
            String datasetCode,
            String masterDataType,
            String triggerMode,
            Boolean enabled,
            String readiness,
            int pageNo,
            int pageSize) {

        log.info(
                "[Service] list keyword={}, datasetCode={}, masterDataType={}, "
                        + "triggerMode={}, enabled={}, readiness={}, pageNo={}, pageSize={}",
                keyword,
                datasetCode,
                masterDataType,
                triggerMode,
                enabled,
                readiness,
                pageNo,
                pageSize);

        /*
         * 数据库条件负责高选择性过滤；readiness 依赖 Dataset 元数据，
         * 仍然属于运行时值，因此当前版本继续在页内计算。
         *
         * 注意：传 readiness 过滤时，total 仍然只能代表当前页过滤结果。
         * 这是原实现已有的分页语义问题，本次改造不改变 API 行为。
         */
        Page<MdSourceBinding> page = bindingMapper.selectPage(
                new Page<>(
                        Math.max(pageNo, 1),
                        Math.min(Math.max(pageSize, 1), 200)),
                new LambdaQueryWrapper<MdSourceBinding>()
                        .and(
                                StringUtils.hasText(keyword),
                                wrapper -> wrapper
                                        .like(
                                                MdSourceBinding::getBindingCode,
                                                keyword)
                                        .or()
                                        .like(
                                                MdSourceBinding::getBindingName,
                                                keyword))
                        .eq(
                                StringUtils.hasText(datasetCode),
                                MdSourceBinding::getDatasetCode,
                                datasetCode)
                        .eq(
                                StringUtils.hasText(masterDataType),
                                MdSourceBinding::getMasterDataType,
                                masterDataType)
                        .eq(
                                StringUtils.hasText(triggerMode),
                                MdSourceBinding::getTriggerMode,
                                triggerMode)
                        .eq(
                                enabled != null,
                                MdSourceBinding::getEnabled,
                                enabled)
                        .orderByDesc(MdSourceBinding::getUpdatedAt));

        List<SourceBindingView> records = page.getRecords().stream()
                .map(this::toView)
                .filter(view ->
                        !StringUtils.hasText(readiness)
                                || view.readiness()
                                        .name()
                                        .equalsIgnoreCase(readiness))
                .toList();

        long filteredTotal = StringUtils.hasText(readiness)
                ? records.size()
                : page.getTotal();

        return new PageResult<>(
                (int) page.getCurrent(),
                (int) page.getSize(),
                filteredTotal,
                records);
    }

    @Override
    @Transactional(readOnly = true)
    public SourceBindingView detail(Long id) {
        log.info("[Service] detail id={}", id);
        return toView(require(id));
    }

    @Override
    @Transactional
    public SourceBindingView save(
            Long id,
            SourceBindingSaveRequest request) {

        log.info("[Service] save id={}, request={}", id, request);

        MdSourceBinding entity =
                id == null
                        ? new MdSourceBinding()
                        : require(id);

        entity.setBindingCode(request.bindingCode().trim())
                .setBindingName(request.bindingName().trim())
                .setMasterDataType(request.masterDataType())
                .setDatasetCode(request.datasetCode().trim())
                .setTriggerMode(
                        StringUtils.hasText(request.triggerMode())
                                ? request.triggerMode()
                                        .trim()
                                        .toUpperCase(Locale.ROOT)
                                : "MANUAL")
                .setEnabled(
                        request.enabled() == null
                                || request.enabled())
                .setDescription(request.description());

        if (!Set.of("MANUAL", "AUTO", "REPUBLISH")
                .contains(entity.getTriggerMode())) {

            throw new BusinessException(
                    40001,
                    "不支持的触发方式");
        }

        if (id == null) {
            bindingMapper.insert(entity);
        } else {
            bindingMapper.updateById(entity);
        }

        return toView(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SourceFieldMappingView> mappings(Long id) {
        require(id);

        return mappingMapper.selectList(
                        new LambdaQueryWrapper<MdSourceFieldMapping>()
                                .eq(
                                        MdSourceFieldMapping::getBindingId,
                                        id)
                                .orderByAsc(
                                        MdSourceFieldMapping::getTargetField))
                .stream()
                .map(this::toView)
                .toList();
    }

    @Override
    @Transactional
    public List<SourceFieldMappingView> saveMappings(
            Long id,
            SourceFieldMappingSaveRequest request) {

        MdSourceBinding binding = require(id);

        List<SourceFieldMappingSaveRequest.Item> items =
                request == null
                                || request.mappings() == null
                        ? List.of()
                        : request.mappings();

        /*
         * 必须先完整校验新集合，再删除旧映射。
         * 这样任意字段不合法时，原有可用配置仍然保留。
         */
        validateMappings(binding, items);

        mappingMapper.delete(
                new LambdaQueryWrapper<MdSourceFieldMapping>()
                        .eq(
                                MdSourceFieldMapping::getBindingId,
                                id));

        for (SourceFieldMappingSaveRequest.Item item : items) {
            MappingMode mode =
                    parseMode(item.mappingMode());

            mappingMapper.insert(
                    new MdSourceFieldMapping()
                            .setBindingId(id)
                            .setTargetField(item.targetField())
                            .setMappingMode(mode)
                            .setSourceFieldCode(
                                    blankToNull(
                                            item.sourceFieldCode()))
                            .setConstantValue(
                                    blankToNull(
                                            item.constantValue())));
        }

        return mappings(binding.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public ReadinessView readiness(Long id) {
        SourceBindingResolver.Resolution resolution =
                bindingResolver.resolve(require(id));

        return toReadinessView(resolution);
    }

    @Override
    @Transactional
    public void updateEnabled(
            Long id,
            boolean enabled) {

        MdSourceBinding binding = require(id);
        binding.setEnabled(enabled);

        bindingMapper.updateById(binding);
    }

    @Override
    public SyncBatchView publish(
            Long id,
            String taskRunId,
            String syncType,
            TriggerType triggerType) {

        return publishApplicationService.publishBinding(
                id,
                taskRunId,
                syncType,
                triggerType);
    }

    /**
     * 保存配置时的前置校验。
     *
     * <p>
     * Resolver 负责“已持久化配置 -> PublishPlan”的运行时解析；
     * 这里负责“用户提交的新映射 -> 是否允许持久化”的写入前校验。
     * </p>
     */
    private void validateMappings(
            MdSourceBinding binding,
            List<SourceFieldMappingSaveRequest.Item> items) {

        MasterDataTypeDefinition definition =
                registry.get(binding.getMasterDataType());

        if (definition == null) {
            throw new BusinessException(
                    40001,
                    "MasterDataType 未注册");
        }

        Map<String, TargetFieldDefinition> fields =
                definition.fields().stream()
                        .collect(
                                Collectors.toMap(
                                        TargetFieldDefinition::fieldCode,
                                        field -> field));

        CollectionDatasetMetadataClient.DatasetBinding dataset =
                metadataClient
                        .getBindings(
                                List.of(binding.getDatasetCode()))
                        .get(binding.getDatasetCode());

        if (dataset == null) {
            throw new BusinessException(
                    40001,
                    "Dataset 不存在或不可用："
                            + binding.getDatasetCode());
        }

        Set<String> datasetFields =
                dataset.fields().stream()
                        .map(
                                CollectionDatasetMetadataClient
                                        .FieldDefinition
                                        ::fieldCode)
                        .collect(Collectors.toSet());

        Set<String> seen = new HashSet<>();

        for (SourceFieldMappingSaveRequest.Item item : items) {
            if (!fields.containsKey(item.targetField())
                    || !seen.add(item.targetField())) {

                throw new BusinessException(
                        40001,
                        "targetField 非法或重复："
                                + item.targetField());
            }

            MappingMode mode =
                    parseMode(item.mappingMode());

            if (mode == MappingMode.FIELD
                    && !StringUtils.hasText(
                            item.sourceFieldCode())) {

                throw new BusinessException(
                        40001,
                        "FIELD mapping 必须填写 sourceFieldCode");
            }

            if (mode == MappingMode.FIELD
                    && !datasetFields.contains(
                            item.sourceFieldCode())) {

                throw new BusinessException(
                        40001,
                        "Dataset 不存在 fieldCode："
                                + item.sourceFieldCode());
            }

            if (mode == MappingMode.CONSTANT
                    && !StringUtils.hasText(
                            item.constantValue())) {

                throw new BusinessException(
                        40001,
                        "CONSTANT mapping 必须填写 constantValue");
            }

            boolean fieldHasConstant =
                    mode == MappingMode.FIELD
                            && StringUtils.hasText(
                                    item.constantValue());

            boolean constantHasSource =
                    mode == MappingMode.CONSTANT
                            && StringUtils.hasText(
                                    item.sourceFieldCode());

            if (fieldHasConstant || constantHasSource) {
                throw new BusinessException(
                        40001,
                        "FIELD/CONSTANT 只能填写对应的一侧");
            }
        }

        for (TargetFieldDefinition field : definition.fields()) {
            if (field.required()
                    && !seen.contains(field.fieldCode())) {

                throw new BusinessException(
                        40001,
                        "缺少 required mapping: "
                                + field.fieldCode());
            }
        }
    }

    private MdSourceBinding require(Long id) {
        MdSourceBinding value =
                bindingMapper.selectById(id);

        if (value == null) {
            throw new BusinessException(
                    40004,
                    "来源绑定不存在：" + id);
        }

        return value;
    }

    /**
     * Binding 详情展示的 readiness 与正式发布使用同一个 Resolver。
     */
    private SourceBindingView toView(
            MdSourceBinding value) {

        SourceBindingResolver.Resolution resolution =
                bindingResolver.resolve(value);

        ReadinessView readinessView =
                toReadinessView(resolution);

        return new SourceBindingView(
                value.getId(),
                value.getBindingCode(),
                value.getBindingName(),
                value.getMasterDataType(),
                value.getDatasetCode(),
                value.getTriggerMode(),
                value.getEnabled(),
                value.getDescription(),
                readinessView.status(),
                readinessView.errors(),
                null,
                value.getCreatedAt(),
                value.getUpdatedAt());
    }

    private ReadinessView toReadinessView(
            SourceBindingResolver.Resolution resolution) {

        return new ReadinessView(
                resolution.status(),
                resolution.errors());
    }

    private SourceFieldMappingView toView(
            MdSourceFieldMapping value) {

        return new SourceFieldMappingView(
                value.getId(),
                value.getTargetField(),
                value.getMappingMode(),
                value.getSourceFieldCode(),
                value.getConstantValue());
    }

    private MappingMode parseMode(String value) {
        try {
            return MappingMode.valueOf(
                    (value == null ? "" : value)
                            .trim()
                            .toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new BusinessException(
                    40001,
                    "不支持的 mappingMode：" + value);
        }
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value)
                ? value.trim()
                : null;
    }
}
