package com.exchange.platform.analytics.service.masterdata;

import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exchange.platform.analytics.api.enums.*;
import com.exchange.platform.analytics.api.registry.*;
import com.exchange.platform.analytics.api.request.*;
import com.exchange.platform.analytics.api.response.*;
import com.exchange.platform.analytics.entity.masterdata.*;
import com.exchange.platform.analytics.mapper.masterdata.*;
import com.exchange.platform.analytics.service.support.CollectionDatasetMetadataClient;
import com.exchange.platform.common.core.exception.BusinessException;
import com.exchange.platform.common.core.page.PageResult;

@Service
public class SourceBindingServiceImpl implements SourceBindingService {
    private final MdSourceBindingMapper bindingMapper;
    private final MdSourceFieldMappingMapper mappingMapper;
    private final MasterDataTypeRegistry registry;
    private final CollectionDatasetMetadataClient metadataClient;
    private final MasterDataPublishOrchestrator orchestrator;

    public SourceBindingServiceImpl(MdSourceBindingMapper bindingMapper, MdSourceFieldMappingMapper mappingMapper,
            MasterDataTypeRegistry registry, CollectionDatasetMetadataClient metadataClient,
            MasterDataPublishOrchestrator orchestrator) {
        this.bindingMapper = bindingMapper;
        this.mappingMapper = mappingMapper;
        this.registry = registry;
        this.metadataClient = metadataClient;
        this.orchestrator = orchestrator;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<SourceBindingView> list(String keyword, String datasetCode, String masterDataType,
            String triggerMode,
            Boolean enabled, String readiness, int pageNo, int pageSize) {
        // 数据库条件负责高选择性过滤；readiness 是依赖 Dataset 元数据的运行时值，只能在映射后计算。
        Page<MdSourceBinding> page = bindingMapper.selectPage(
                new Page<>(Math.max(pageNo, 1), Math.min(Math.max(pageSize, 1), 200)),
                new LambdaQueryWrapper<MdSourceBinding>()
                        .and(StringUtils.hasText(keyword),
                                w -> w.like(MdSourceBinding::getBindingCode, keyword).or()
                                        .like(MdSourceBinding::getBindingName, keyword))
                        .eq(StringUtils.hasText(datasetCode), MdSourceBinding::getDatasetCode, datasetCode)
                        .eq(StringUtils.hasText(masterDataType), MdSourceBinding::getMasterDataType, masterDataType)
                        .eq(StringUtils.hasText(triggerMode), MdSourceBinding::getTriggerMode, triggerMode)
                        .eq(enabled != null, MdSourceBinding::getEnabled, enabled)
                        .orderByDesc(MdSourceBinding::getUpdatedAt));
        List<SourceBindingView> records = page.getRecords().stream().map(this::toView)
                .filter(view -> !StringUtils.hasText(readiness) || view.readiness().name().equals(readiness)).toList();
        // readiness 是运行时计算值，当前版本在分页后过滤；后续可迁移为专门的查询投影。
        long filteredTotal = StringUtils.hasText(readiness) ? records.size() : page.getTotal();
        return new PageResult<>((int) page.getCurrent(), (int) page.getSize(), filteredTotal, records);
    }

    @Override
    @Transactional(readOnly = true)
    public SourceBindingView detail(Long id) {
        return toView(require(id));
    }

    @Override
    @Transactional
    public SourceBindingView save(Long id, SourceBindingSaveRequest request) {
        // 基本配置与字段映射分开保存，避免半成品映射影响已有发布。
        MdSourceBinding entity = id == null ? new MdSourceBinding() : require(id);
        entity.setBindingCode(request.bindingCode().trim()).setBindingName(request.bindingName().trim())
                .setMasterDataType(request.masterDataType()).setDatasetCode(request.datasetCode().trim())
                .setTriggerMode(
                        StringUtils.hasText(request.triggerMode()) ? request.triggerMode().toUpperCase(Locale.ROOT)
                                : "MANUAL")
                .setEnabled(request.enabled() == null || request.enabled()).setDescription(request.description());
        if (!Set.of("MANUAL", "AUTO", "REPUBLISH").contains(entity.getTriggerMode()))
            throw new BusinessException(40001, "不支持的触发方式");
        if (id == null)
            bindingMapper.insert(entity);
        else
            bindingMapper.updateById(entity);
        return toView(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SourceFieldMappingView> mappings(Long id) {
        require(id);
        return mappingMapper
                .selectList(new LambdaQueryWrapper<MdSourceFieldMapping>().eq(MdSourceFieldMapping::getBindingId, id)
                        .orderByAsc(MdSourceFieldMapping::getTargetField))
                .stream().map(this::toView).toList();
    }

    @Override
    @Transactional
    public List<SourceFieldMappingView> saveMappings(Long id, SourceFieldMappingSaveRequest request) {
        // 先完整校验新集合，再删除旧映射，确保替换操作不会留下不完整配置。
        MdSourceBinding binding = require(id);
        validateMappings(binding, request.mappings());
        mappingMapper.delete(new LambdaQueryWrapper<MdSourceFieldMapping>().eq(MdSourceFieldMapping::getBindingId, id));
        for (SourceFieldMappingSaveRequest.Item item : request.mappings()) {
            MappingMode mode = parseMode(item.mappingMode());
            mappingMapper.insert(new MdSourceFieldMapping().setBindingId(id).setTargetField(item.targetField())
                    .setMappingMode(mode).setSourceFieldCode(blankToNull(item.sourceFieldCode()))
                    .setConstantValue(blankToNull(item.constantValue())));
        }
        return mappings(binding.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public ReadinessView readiness(Long id) {
        return readinessOf(require(id));
    }

    @Override
    @Transactional
    public void updateEnabled(Long id, boolean enabled) {
        MdSourceBinding binding = require(id);
        binding.setEnabled(enabled);
        bindingMapper.updateById(binding);
    }

    @Override
    public SyncBatchView publish(Long id, String taskRunId, String syncType, TriggerType triggerType) {
        return orchestrator.publish(id, taskRunId, syncType, triggerType);
    }

    ReadinessView readinessOf(MdSourceBinding binding) {
        // readiness 同时检查启用状态、类型注册、Dataset 元数据和每个字段映射。
        List<String> errors = new ArrayList<>();
        if (!Boolean.TRUE.equals(binding.getEnabled()))
            errors.add("来源已停用");
        MasterDataTypeDefinition definition = registry.get(binding.getMasterDataType());
        if (definition == null)
            errors.add("MasterDataType 未注册");
        CollectionDatasetMetadataClient.DatasetBinding dataset = null;
        try {
            dataset = metadataClient.getBindings(List.of(binding.getDatasetCode())).get(binding.getDatasetCode());
        } catch (RuntimeException ex) {
            errors.add("Dataset 元数据不可读：" + ex.getMessage());
        }
        if (dataset == null)
            errors.add("Dataset 不存在或不可用");
        Set<String> sourceFields = dataset == null ? Set.of()
                : dataset.fields().stream().map(CollectionDatasetMetadataClient.FieldDefinition::fieldCode)
                        .collect(Collectors.toSet());
        Map<String, MdSourceFieldMapping> mappings = mappingMapper
                .selectList(new LambdaQueryWrapper<MdSourceFieldMapping>().eq(MdSourceFieldMapping::getBindingId,
                        binding.getId()))
                .stream().collect(Collectors.toMap(MdSourceFieldMapping::getTargetField, x -> x, (a, b) -> a));
        if (definition != null)
            for (TargetFieldDefinition field : definition.fields()) {
                MdSourceFieldMapping mapping = mappings.get(field.fieldCode());
                if (field.required() && mapping == null)
                    errors.add("缺少 required mapping: " + field.fieldCode());
                if (mapping != null && mapping.getMappingMode() == MappingMode.FIELD
                        && !sourceFields.contains(mapping.getSourceFieldCode()))
                    errors.add("Dataset 不存在 fieldCode: " + mapping.getSourceFieldCode());
                if (mapping != null && mapping.getMappingMode() == MappingMode.CONSTANT
                        && !StringUtils.hasText(mapping.getConstantValue()))
                    errors.add("CONSTANT 值不能为空: " + field.fieldCode());
            }
        return new ReadinessView(errors.isEmpty() ? ReadinessStatus.READY
                : (!Boolean.TRUE.equals(binding.getEnabled()) ? ReadinessStatus.DISABLED : ReadinessStatus.INVALID),
                errors);
    }

    private void validateMappings(MdSourceBinding binding, List<SourceFieldMappingSaveRequest.Item> items) {
        // 保存前执行严格契约校验，使后续发布可以假设 targetField 唯一且来源合法。
        MasterDataTypeDefinition definition = registry.get(binding.getMasterDataType());
        if (definition == null)
            throw new BusinessException(40001, "MasterDataType 未注册");
        Map<String, TargetFieldDefinition> fields = definition.fields().stream()
                .collect(Collectors.toMap(TargetFieldDefinition::fieldCode, x -> x));
        Set<String> datasetFields = metadataClient.getBindings(List.of(binding.getDatasetCode()))
                .getOrDefault(binding.getDatasetCode(),
                        new CollectionDatasetMetadataClient.DatasetBinding("", "", "", "", List.of()))
                .fields().stream()
                .map(CollectionDatasetMetadataClient.FieldDefinition::fieldCode).collect(Collectors.toSet());
        Set<String> seen = new HashSet<>();
        for (SourceFieldMappingSaveRequest.Item item : items) {
            if (!fields.containsKey(item.targetField()) || !seen.add(item.targetField()))
                throw new BusinessException(40001, "targetField 非法或重复：" + item.targetField());
            MappingMode mode = parseMode(item.mappingMode());
            if (mode == MappingMode.FIELD && !StringUtils.hasText(item.sourceFieldCode()))
                throw new BusinessException(40001, "FIELD mapping 必须填写 sourceFieldCode");
            if (mode == MappingMode.FIELD && !datasetFields.contains(item.sourceFieldCode()))
                throw new BusinessException(40001, "Dataset 不存在 fieldCode：" + item.sourceFieldCode());
            if (mode == MappingMode.CONSTANT && !StringUtils.hasText(item.constantValue()))
                throw new BusinessException(40001, "CONSTANT mapping 必须填写 constantValue");
            if (mode == MappingMode.FIELD && StringUtils.hasText(item.constantValue())
                    || mode == MappingMode.CONSTANT && StringUtils.hasText(item.sourceFieldCode()))
                throw new BusinessException(40001, "FIELD/CONSTANT 只能填写对应的一侧");
        }
        for (TargetFieldDefinition field : definition.fields())
            if (field.required() && !seen.contains(field.fieldCode()))
                throw new BusinessException(40001, "缺少 required mapping: " + field.fieldCode());
    }

    /** 读取绑定并统一处理不存在场景。 */
    private MdSourceBinding require(Long id) {
        MdSourceBinding value = bindingMapper.selectById(id);
        if (value == null)
            throw new BusinessException(40004, "来源绑定不存在：" + id);
        return value;
    }

    /** 将绑定实体转换为包含实时 readiness 的视图。 */
    private SourceBindingView toView(MdSourceBinding value) {
        ReadinessView r = readinessOf(value);
        return new SourceBindingView(value.getId(), value.getBindingCode(), value.getBindingName(),
                value.getMasterDataType(), value.getDatasetCode(), value.getTriggerMode(), value.getEnabled(),
                value.getDescription(), r.status(), r.errors(), null, value.getCreatedAt(), value.getUpdatedAt());
    }

    /** 将字段映射实体转换为配置页面所需的精简视图。 */
    private SourceFieldMappingView toView(MdSourceFieldMapping value) {
        return new SourceFieldMappingView(value.getId(), value.getTargetField(), value.getMappingMode(),
                value.getSourceFieldCode(), value.getConstantValue());
    }

    /** 大小写不敏感解析映射模式，非法值转换为业务参数错误。 */
    private MappingMode parseMode(String value) {
        try {
            return MappingMode.valueOf((value == null ? "" : value).toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new BusinessException(40001, "不支持的 mappingMode：" + value);
        }
    }

    /** 将空白字符串规范化为 null，避免持久化无意义的空值。 */
    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
