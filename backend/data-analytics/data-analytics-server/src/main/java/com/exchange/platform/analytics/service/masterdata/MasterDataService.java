package com.exchange.platform.analytics.service.masterdata;

import java.time.LocalDateTime;
import java.util.List;

import com.exchange.platform.analytics.api.enums.TriggerType;
import com.exchange.platform.analytics.api.request.MarketAssetListRequest;
import com.exchange.platform.analytics.api.request.MarketAssetRef;
import com.exchange.platform.analytics.api.request.ScopeResolveRequest;
import com.exchange.platform.analytics.api.response.AssetMemberView;
import com.exchange.platform.analytics.api.response.MarketAssetView;
import com.exchange.platform.analytics.api.response.SyncBatchView;
import com.exchange.platform.common.core.enums.MarketAssetType;
import com.exchange.platform.common.core.page.PageResult;

/**
 * 主数据查询、范围解析及发布能力的应用服务接口。
 *
 * <p>
 * 对外 API 保持不变；内部发布实现切换为
 * PublishPlan + ApplicationService + Executor 的单向依赖结构。
 * </p>
 */
public interface MasterDataService {

    /** 按代码或名称搜索资产，结果数量由 limit 限制。 */
    List<MarketAssetView> search(
            String keyword,
            MarketAssetType marketAssetType,
            int limit);

    /** 分页查询资产主数据。 */
    PageResult<MarketAssetView> list(
            MarketAssetListRequest request);

    /** 查询单个资产及其股票/指数扩展属性。 */
    MarketAssetView detail(
            MarketAssetType marketAssetType,
            String assetCode);

    /** 批量查询资产详情，保持输入顺序。 */
    List<MarketAssetView> batchDetail(
            List<MarketAssetRef> assets);

    /** 查询指定时点仍然有效的指数成分关系。 */
    List<AssetMemberView> members(
            String indexCode,
            LocalDateTime asOfTime);

    /** 将业务范围表达式解析为资产列表。 */
    List<MarketAssetView> resolveScope(
            ScopeResolveRequest request);

    /** 分页查询历史同步批次及其执行统计。 */
    PageResult<SyncBatchView> batches(
            String masterDataType,
            String datasetCode,
            String status,
            Long bindingId,
            String triggerType,
            int pageNo,
            int pageSize);

    /** 使用标准字段 Dataset 创建手工发布批次。 */
    SyncBatchView publish(
            String masterDataType,
            String sourceCode,
            String taskRunId,
            String datasetCode,
            String syncType);

    /** 重新执行既有批次；Binding 批次使用当前最新绑定配置。 */
    SyncBatchView republish(Long batchId);

    /** 使用来源绑定配置创建发布批次。 */
    SyncBatchView publishBinding(
            Long bindingId,
            String taskRunId,
            String syncType,
            TriggerType triggerType);
}
