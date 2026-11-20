package com.exchange.platform.analytics.service.masterdata;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.exchange.platform.analytics.api.enums.MasterDataType;
import com.exchange.platform.analytics.api.enums.SyncBatchStatus;
import com.exchange.platform.analytics.api.enums.SyncType;
import com.exchange.platform.analytics.api.enums.TriggerType;
import com.exchange.platform.analytics.api.registry.MasterDataTypeDefinition;
import com.exchange.platform.analytics.api.response.SyncBatchView;
import com.exchange.platform.analytics.entity.masterdata.MdSourceBinding;
import com.exchange.platform.analytics.entity.masterdata.MdSyncBatch;
import com.exchange.platform.analytics.mapper.masterdata.MdSourceBindingMapper;
import com.exchange.platform.analytics.mapper.masterdata.MdSyncBatchMapper;
import com.exchange.platform.common.core.enums.MarketAssetType;
import com.exchange.platform.common.core.exception.BusinessException;

/**
 * 主数据发布用例的应用服务。
 *
 * <p>
 * 本类只负责编排一次发布如何开始、使用什么计划、创建哪个批次，
 * 以及失败后如何记录结果；不承担正式主数据表写入。
 * 正式事务执行交给 {@link MasterDataPublishExecutor}。
 * </p>
 */
@Service
public class MasterDataPublishApplicationService {

    private final MdSyncBatchMapper batchMapper;
    private final MdSourceBindingMapper bindingMapper;
    private final MasterDataTypeRegistry registry;
    private final SourceBindingResolver bindingResolver;
    private final MasterDataPublishExecutor executor;

    public MasterDataPublishApplicationService(
            MdSyncBatchMapper batchMapper,
            MdSourceBindingMapper bindingMapper,
            MasterDataTypeRegistry registry,
            SourceBindingResolver bindingResolver,
            MasterDataPublishExecutor executor) {

        this.batchMapper = batchMapper;
        this.bindingMapper = bindingMapper;
        this.registry = registry;
        this.bindingResolver = bindingResolver;
        this.executor = executor;
    }

    /** 使用标准字段 Dataset 直接发布股票/指数主数据。 */
    public SyncBatchView publish(
            MasterDataType masterDataType,
            String sourceCode,
            String taskRunId,
            String datasetCode,
            String syncType) {

        return publishDirect(
                masterDataType,
                sourceCode,
                taskRunId,
                datasetCode,
                syncType,
                TriggerType.MANUAL);
    }

    /**
     * 使用来源绑定生成 PublishPlan 后执行发布。
     *
     * <p>
     * Resolver 完成 readiness 校验并返回同一份不可变计划。
     * 创建批次以后，Executor 不再读取 Binding/Mapping。
     * </p>
     */
    public SyncBatchView publishBinding(
            Long bindingId,
            String taskRunId,
            String syncType,
            TriggerType triggerType) {

        PublishPlan plan =
                bindingResolver.requireReady(bindingId);

        MdSyncBatch batch = newBatch(
                plan,
                plan.bindingCode(),
                taskRunId,
                parseSyncType(syncType),
                triggerType == null
                        ? TriggerType.MANUAL
                        : triggerType);

        return executeBatch(batch, plan);
    }

    /**
     * 重新执行历史批次。
     *
     * <p>
     * 对 Binding 批次保持现有语义：读取当前最新 Binding 配置，
     * 生成新的 PublishPlan 后执行，而不是回放历史映射快照。
     * </p>
     */
    public SyncBatchView republish(Long batchId) {
        MdSyncBatch previous =
                batchMapper.selectById(batchId);

        if (previous == null) {
            throw new BusinessException(
                    40004,
                    "同步批次不存在：" + batchId);
        }

        String syncType = previous.getSyncType() == null
                ? SyncType.FULL.name()
                : previous.getSyncType().name();

        if (previous.getBindingId() != null) {
            return publishBinding(
                    previous.getBindingId(),
                    previous.getTaskRunId(),
                    syncType,
                    TriggerType.REPUBLISH);
        }

        return publishDirect(
                previous.getMasterDataType(),
                previous.getSourceCode(),
                previous.getTaskRunId(),
                previous.getDatasetCode(),
                syncType,
                TriggerType.REPUBLISH);
    }

    /**
     * 将同步批次实体转换为统一 API 视图。
     *
     * <p>
     * Binding 名称只用于展示，不参与执行，因此这里读取当前 Binding
     * 不会破坏 PublishPlan 的执行快照语义。
     * </p>
     */
    public SyncBatchView toView(MdSyncBatch batch) {
        MdSourceBinding binding =
                batch.getBindingId() == null
                        ? null
                        : bindingMapper.selectById(
                                batch.getBindingId());

        return new SyncBatchView(
                batch.getId(),
                batch.getMasterDataType(),
                batch.getBindingId(),
                binding == null
                        ? null
                        : binding.getBindingCode(),
                binding == null
                        ? null
                        : binding.getBindingName(),
                batch.getTriggerType(),
                batch.getSourceCode(),
                batch.getTaskRunId(),
                batch.getDatasetCode(),
                batch.getSyncType(),
                batch.getStatus(),
                n(batch.getTotalCount()),
                n(batch.getAddedCount()),
                n(batch.getUpdatedCount()),
                n(batch.getRemovedCount()),
                batch.getStartedAt(),
                batch.getPublishedAt(),
                batch.getErrorMessage());
    }

    private SyncBatchView publishDirect(
            MasterDataType masterDataType,
            String sourceCode,
            String taskRunId,
            String datasetCode,
            String syncType,
            TriggerType triggerType) {

        PublishPlan plan =
                createDirectPlan(
                        masterDataType,
                        datasetCode);

        MdSyncBatch batch = newBatch(
                plan,
                sourceCode,
                taskRunId,
                parseSyncType(syncType),
                triggerType);

        return executeBatch(batch, plan);
    }

    /**
     * 直接发布同样使用 PublishPlan，避免 Executor 再维护一套 STOCK_FIELDS/
     * INDEX_FIELDS。字段定义统一来自 MasterDataTypeRegistry。
     */
    private PublishPlan createDirectPlan(
            MasterDataType masterDataType,
            String datasetCode) {

        if (masterDataType == null) {
            throw new BusinessException(
                    40001,
                    "masterDataType 不能为空");
        }

        if (!StringUtils.hasText(datasetCode)) {
            throw new BusinessException(
                    40001,
                    "datasetCode 不能为空");
        }

        MarketAssetType marketAssetType =
                directMarketAssetTypeOf(masterDataType);

        if (marketAssetType == null) {
            throw new BusinessException(
                    40001,
                    "当前 MasterDataType 不支持直接发布："
                            + masterDataType);
        }

        MasterDataTypeDefinition definition =
                registry.get(masterDataType);

        if (definition == null) {
            throw new BusinessException(
                    40001,
                    "MasterDataType 未注册："
                            + masterDataType);
        }

        List<String> fieldCodes =
                definition.fields().stream()
                        .map(field -> field.fieldCode())
                        .toList();

        return new PublishPlan(
                null,
                null,
                null,
                masterDataType,
                marketAssetType,
                datasetCode.trim(),
                fieldCodes,
                List.of());
    }

    /**
     * 批次先在事务外创建。
     *
     * <p>
     * Executor 失败时正式数据事务整体回滚，而 CREATED 批次仍然存在；
     * ApplicationService 随后把它更新成 FAILED，保留执行事实。
     * </p>
     */
    private SyncBatchView executeBatch(
            MdSyncBatch batch,
            PublishPlan plan) {

        batchMapper.insert(batch);

        try {
            executor.execute(batch, plan);
        } catch (Exception ex) {
            batch.setStatus(SyncBatchStatus.FAILED)
                    .setPublishedAt(null)
                    .setErrorMessage(trimError(ex));

            batchMapper.updateById(batch);
        }

        return toView(batch);
    }

    private MdSyncBatch newBatch(
            PublishPlan plan,
            String sourceCode,
            String taskRunId,
            SyncType syncType,
            TriggerType triggerType) {

        return new MdSyncBatch()
                .setBindingId(plan.bindingId())
                .setMasterDataType(plan.masterDataType())
                .setTriggerType(
                        triggerType == null
                                ? TriggerType.MANUAL
                                : triggerType)
                .setSourceCode(sourceCode)
                .setTaskRunId(taskRunId)
                .setDatasetCode(plan.datasetCode())
                .setSyncType(syncType)
                .setStatus(SyncBatchStatus.CREATED)
                .setStartedAt(LocalDateTime.now())
                .setTotalCount(0)
                .setAddedCount(0)
                .setUpdatedCount(0)
                .setRemovedCount(0);
    }

    private SyncType parseSyncType(String value) {
        if (!StringUtils.hasText(value)) {
            return SyncType.FULL;
        }

        try {
            return SyncType.valueOf(
                    value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(
                    40001,
                    "不支持的 syncType：" + value);
        }
    }

    private MarketAssetType directMarketAssetTypeOf(
            MasterDataType type) {

        return switch (type) {
            case MARKET_ASSET_STOCK -> MarketAssetType.STOCK;
            case MARKET_ASSET_INDEX -> MarketAssetType.INDEX;
            case MARKET_ASSET_REL_INDEX_MEMBER -> null;
        };
    }

    private String trimError(Exception ex) {
        String value = ex.getMessage() == null
                ? ex.getClass().getSimpleName()
                : ex.getMessage();

        return value.length() > 1900
                ? value.substring(0, 1900)
                : value;
    }

    private int n(Integer value) {
        return value == null ? 0 : value;
    }
}
