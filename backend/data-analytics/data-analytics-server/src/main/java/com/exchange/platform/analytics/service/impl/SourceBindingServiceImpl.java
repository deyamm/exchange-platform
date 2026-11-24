package com.exchange.platform.analytics.service.impl;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exchange.platform.analytics.api.enums.MappingMode;
import com.exchange.platform.analytics.api.request.SourceBindingSaveRequest;
import com.exchange.platform.analytics.api.request.SourceFieldMappingSaveRequest;
import com.exchange.platform.analytics.api.response.ReadinessView;
import com.exchange.platform.analytics.api.response.SourceBindingView;
import com.exchange.platform.analytics.api.response.SourceFieldMappingView;
import com.exchange.platform.analytics.entity.masterdata.MdSourceBinding;
import com.exchange.platform.analytics.entity.masterdata.MdSourceFieldMapping;
import com.exchange.platform.analytics.mapper.masterdata.MdSourceBindingMapper;
import com.exchange.platform.analytics.mapper.masterdata.MdSourceFieldMappingMapper;
import com.exchange.platform.analytics.service.SourceBindingService;
import com.exchange.platform.analytics.service.masterdata.MasterDataPublishApplicationService;
import com.exchange.platform.analytics.service.masterdata.SourceBindingResolver;
import com.exchange.platform.common.core.exception.BusinessException;
import com.exchange.platform.common.core.page.PageResult;

/**
 * SourceBinding / FieldMapping 的配置管理服务。
 *
 * <p>
 * 本类只负责 CRUD 和配置展示。字段映射是否合法由 {@link SourceBindingResolver} 统一判断，因此这里不再直接依赖
 * Registry 或 Dataset Metadata，也不再维护第二套 Mapping 校验规则。
 * </p>
 */
@Service
public class SourceBindingServiceImpl implements SourceBindingService {

    private static final Logger log = LoggerFactory.getLogger(SourceBindingServiceImpl.class);

    private static final Set<String> SUPPORTED_TRIGGER_MODES = Set.of("MANUAL", "AUTO", "REPUBLISH");

    private final MdSourceBindingMapper bindingMapper;
    private final MdSourceFieldMappingMapper mappingMapper;
    private final SourceBindingResolver bindingResolver;

    public SourceBindingServiceImpl(MdSourceBindingMapper bindingMapper, MdSourceFieldMappingMapper mappingMapper,
            SourceBindingResolver bindingResolver, MasterDataPublishApplicationService publishApplicationService) {

        this.bindingMapper = bindingMapper;
        this.mappingMapper = mappingMapper;
        this.bindingResolver = bindingResolver;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<SourceBindingView> list(String keyword, String datasetCode, String masterDataType,
            String triggerMode, Boolean enabled, String readiness, int pageNo, int pageSize) {

        log.info(
                "[Service] list keyword={}, datasetCode={}, masterDataType={}, "
                        + "triggerMode={}, enabled={}, readiness={}, pageNo={}, pageSize={}",
                keyword, datasetCode, masterDataType, triggerMode, enabled, readiness, pageNo, pageSize);

        Page<MdSourceBinding> page = bindingMapper.selectPage(
                new Page<>(Math.max(pageNo, 1), Math.min(Math.max(pageSize, 1), 200)),
                new LambdaQueryWrapper<MdSourceBinding>()
                    .and(StringUtils.hasText(keyword),
                            wrapper -> wrapper.like(MdSourceBinding::getBindingCode, keyword).or()
                                    .like(MdSourceBinding::getBindingName, keyword))
                    .eq(StringUtils.hasText(datasetCode), MdSourceBinding::getDatasetCode, datasetCode)
                    .eq(StringUtils.hasText(masterDataType), MdSourceBinding::getMasterDataType, masterDataType)
                    .eq(StringUtils.hasText(triggerMode), MdSourceBinding::getTriggerMode, triggerMode)
                    .eq(enabled != null, MdSourceBinding::getEnabled, enabled)
                    .orderByDesc(MdSourceBinding::getUpdatedAt));

        List<SourceBindingView> records = page.getRecords().stream().map(this::toView)
                .filter(view -> !StringUtils.hasText(readiness) || view.readiness().name().equalsIgnoreCase(readiness))
                .toList();

        /*
         * readiness 依赖外部 Dataset 元数据，当前仍在数据库分页之后计算。 因此传 readiness 过滤时 total
         * 只能代表当前页过滤后的数量；如果后续需要 精确 total，应单独设计 readiness 查询投影，不在 CRUD Service 中继续堆逻辑。
         */
        long filteredTotal = StringUtils.hasText(readiness) ? records.size() : page.getTotal();

        return new PageResult<>((int) page.getCurrent(), (int) page.getSize(), filteredTotal, records);
    }

    @Override
    @Transactional(readOnly = true)
    public SourceBindingView detail(Long id) {
        log.info("[Service] detail id={}", id);
        return toView(require(id));
    }

    @Override
    @Transactional
    public SourceBindingView save(Long id, SourceBindingSaveRequest request) {

        log.info("[Service] save id={}, request={}", id, request);

        MdSourceBinding entity = id == null ? new MdSourceBinding() : require(id);

        String triggerMode = StringUtils.hasText(request.triggerMode())
                ? request.triggerMode().trim().toUpperCase(Locale.ROOT)
                : "MANUAL";

        if (!SUPPORTED_TRIGGER_MODES.contains(triggerMode)) {
            throw new BusinessException(40001, "不支持的触发方式");
        }

        entity.setBindingCode(request.bindingCode().trim())
            .setBindingName(request.bindingName().trim())
            .setMasterDataType(request.masterDataType())
            .setDatasetCode(request.datasetCode().trim())
            .setTriggerMode(triggerMode)
            .setEnabled(request.enabled() == null || request.enabled())
            .setDescription(request.description());

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
        return loadMappings(id).stream().map(this::toView).toList();
    }

    @Override
    @Transactional
    public List<SourceFieldMappingView> saveMappings(Long id, SourceFieldMappingSaveRequest request) {

        MdSourceBinding binding = require(id);

        List<SourceFieldMappingSaveRequest.Item> items = request == null || request.mappings() == null ? List.of()
                : request.mappings();

        /*
         * 先把请求转换成与持久化完全一致的 Mapping 实体，再交给 Resolver 校验。 只有全部通过后才删除旧配置，避免非法请求破坏已有可用
         * Mapping。
         */
        List<MdSourceFieldMapping> pendingMappings = items.stream().map(item -> toEntity(id, item)).toList();

        List<String> errors = bindingResolver.validateMappings(binding, pendingMappings);

        if (!errors.isEmpty()) {
            throw new BusinessException(40001, "字段映射不合法：" + String.join("；", errors));
        }

        mappingMapper.delete(new LambdaQueryWrapper<MdSourceFieldMapping>().eq(MdSourceFieldMapping::getBindingId, id));

        for (MdSourceFieldMapping mapping : pendingMappings) {
            mappingMapper.insert(mapping);
        }

        return loadMappings(id).stream().map(this::toView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ReadinessView readiness(Long id) {
        return toReadinessView(bindingResolver.resolve(require(id)));
    }

    @Override
    @Transactional
    public void updateEnabled(Long id, boolean enabled) {
        MdSourceBinding binding = require(id);
        binding.setEnabled(enabled);
        bindingMapper.updateById(binding);
    }

    private List<MdSourceFieldMapping> loadMappings(Long bindingId) {
        return mappingMapper.selectList(new LambdaQueryWrapper<MdSourceFieldMapping>()
                .eq(MdSourceFieldMapping::getBindingId, bindingId).orderByAsc(MdSourceFieldMapping::getTargetField));
    }

    private MdSourceFieldMapping toEntity(Long bindingId, SourceFieldMappingSaveRequest.Item item) {

        return new MdSourceFieldMapping().setBindingId(bindingId).setTargetField(item.targetField())
                .setMappingMode(parseMode(item.mappingMode())).setSourceFieldCode(blankToNull(item.sourceFieldCode()))
                .setConstantValue(blankToNull(item.constantValue()));
    }

    private MdSourceBinding require(Long id) {
        MdSourceBinding value = bindingMapper.selectById(id);

        if (value == null) {
            throw new BusinessException(40004, "来源绑定不存在：" + id);
        }

        return value;
    }

    private SourceBindingView toView(MdSourceBinding value) {
        ReadinessView readiness = toReadinessView(bindingResolver.resolve(value));

        return new SourceBindingView(value.getId(), value.getBindingCode(), value.getBindingName(),
                value.getMasterDataType(), value.getDatasetCode(), value.getTriggerMode(), value.getEnabled(),
                value.getDescription(), readiness.status(), readiness.errors(), null, value.getCreatedAt(),
                value.getUpdatedAt());
    }

    private ReadinessView toReadinessView(SourceBindingResolver.Resolution resolution) {

        return new ReadinessView(resolution.status(), resolution.errors());
    }

    private SourceFieldMappingView toView(MdSourceFieldMapping value) {
        return new SourceFieldMappingView(value.getId(), value.getTargetField(), value.getMappingMode(),
                value.getSourceFieldCode(), value.getConstantValue());
    }

    private MappingMode parseMode(String value) {
        try {
            return MappingMode.valueOf((value == null ? "" : value).trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new BusinessException(40001, "不支持的 mappingMode：" + value);
        }
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
