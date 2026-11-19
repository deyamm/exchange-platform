package com.exchange.platform.analytics.service.masterdata;

import java.time.LocalDateTime;
import java.util.*;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exchange.platform.analytics.api.enums.*;
import com.exchange.platform.analytics.api.response.SyncBatchView;
import com.exchange.platform.analytics.entity.masterdata.*;
import com.exchange.platform.analytics.mapper.masterdata.*;
import com.exchange.platform.common.core.exception.BusinessException;

/**
 * MANUAL、REPUBLISH（以及未来 AUTO）的唯一发布入口。
 * <p>
 * 它只负责解析来源配置与创建执行批次，领域校验、Diff、Quality Guard 和事务写入
 * 统一委托给 {@link MasterDataPublishService}，避免多套发布逻辑逐渐分叉。
 * </p>
 */
@Service
public class MasterDataPublishOrchestrator {
    private final MdSourceBindingMapper bindingMapper;
    private final MdSourceFieldMappingMapper mappingMapper;
    private final MdSyncBatchMapper batchMapper;
    private final SourceBindingServiceImpl sourceBindingService;
    private final MasterDataPublishService publishService;

    public MasterDataPublishOrchestrator(MdSourceBindingMapper bindingMapper, MdSourceFieldMappingMapper mappingMapper, MdSyncBatchMapper batchMapper, MasterDataTypeRegistry registry, @Lazy SourceBindingServiceImpl sourceBindingService, MasterDataPublishService publishService) {
        this.bindingMapper = bindingMapper;
        this.mappingMapper = mappingMapper;
        this.batchMapper = batchMapper;
        this.sourceBindingService = sourceBindingService;
        this.publishService = publishService;
    }

    public SyncBatchView publish(Long bindingId, String taskRunId, String syncType, TriggerType triggerType) {
        // 发布入口先检查来源 readiness，再创建批次；未通过时不会产生执行记录。
        MdSourceBinding binding = requireBinding(bindingId);
        ReadinessStatus readiness = sourceBindingService.readinessOf(binding).status();
        if (readiness != ReadinessStatus.READY)
            throw new BusinessException(40001, "来源不可发布，请先修复 readiness");
        MdSyncBatch batch = new MdSyncBatch().setBindingId(bindingId).setMasterDataType(binding.getMasterDataType())
                .setTriggerType(triggerType == null ? TriggerType.MANUAL : triggerType)
                .setSourceCode(binding.getBindingCode())
                .setTaskRunId(taskRunId).setDatasetCode(binding.getDatasetCode()).setSyncType(parseSyncType(syncType))
                .setStatus(SyncBatchStatus.CREATED).setStartedAt(LocalDateTime.now()).setTotalCount(0)
                .setAddedCount(0).setUpdatedCount(0).setRemovedCount(0);
        batchMapper.insert(batch);
        try {
            // 正式写入交给独立事务服务，保证代理事务边界覆盖整个发布过程。
            publishService.publish(batch);
        } catch (Exception ex) {
            // 事务内部已回滚正式表；这里只保留 FAILED 执行事实。
            batch.setStatus(SyncBatchStatus.FAILED).setErrorMessage(trimError(ex));
            batchMapper.updateById(batch);
        }
        return publishService.toView(batch);
    }

    public List<String> fieldCodes(Long bindingId) {
        // 仅返回 FIELD 映射；CONSTANT 不需要从 Dataset 读取。
        return mappingMapper
                .selectList(new LambdaQueryWrapper<MdSourceFieldMapping>().eq(MdSourceFieldMapping::getBindingId,
                        bindingId))
                .stream()
                .filter(item -> item.getMappingMode() == MappingMode.FIELD
                        && StringUtils.hasText(item.getSourceFieldCode()))
                .map(MdSourceFieldMapping::getSourceFieldCode).distinct().toList();
    }

    public List<MdSourceFieldMapping> mappings(Long bindingId) {
        // 编排器复用原始映射实体，供读取阶段按绑定构造标准行。
        return mappingMapper.selectList(
                new LambdaQueryWrapper<MdSourceFieldMapping>().eq(MdSourceFieldMapping::getBindingId, bindingId));
    }

    /** 读取绑定并将缺失配置转换为统一业务错误。 */
    private MdSourceBinding requireBinding(Long id) {
        MdSourceBinding value = bindingMapper.selectById(id);
        if (value == null)
            throw new BusinessException(40004, "来源绑定不存在：" + id);
        return value;
    }

    /** 解析同步模式，缺省采用全量同步。 */
    private SyncType parseSyncType(String value) {
        return value == null || value.isBlank() ? SyncType.FULL : SyncType.valueOf(value.toUpperCase(Locale.ROOT));
    }

    /** 截断异常文本，避免错误信息超过批次表字段长度。 */
    private String trimError(Exception ex) {
        String value = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
        return value.length() > 1900 ? value.substring(0, 1900) : value;
    }

}
