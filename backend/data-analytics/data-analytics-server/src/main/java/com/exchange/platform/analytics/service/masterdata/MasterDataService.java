package com.exchange.platform.analytics.service.masterdata;

import java.time.LocalDateTime;
import java.util.List;
import com.exchange.platform.analytics.api.request.*;
import com.exchange.platform.analytics.api.response.*;
import com.exchange.platform.common.core.enums.MarketAssetType;
import com.exchange.platform.common.core.page.PageResult;
import com.exchange.platform.analytics.api.enums.TriggerType;

/**
 * 主数据查询、范围解析及发布能力的应用服务接口。
 * 查询方法面向前端/其他领域提供稳定视图，发布方法负责创建同步批次并驱动
 * Dataset 读取、质量校验、差异写入和成分关系维护。
 */
public interface MasterDataService {
    /** 按代码或名称搜索资产，结果数量由 limit 限制。 */
    List<MarketAssetView> search(String keyword, MarketAssetType marketAssetType, int limit);
    /** 分页查询资产主数据。 */
    PageResult<MarketAssetView> list(MarketAssetListRequest request);
    /** 查询单个资产及其股票/指数扩展属性。 */
    MarketAssetView detail(MarketAssetType marketAssetType, String assetCode);
    /** 批量查询资产详情，保持输入顺序。 */
    List<MarketAssetView> batchDetail(List<MarketAssetRef> assets);
    /** 查询指定时点仍然有效的指数成分关系。 */
    List<AssetMemberView> members(String indexCode, LocalDateTime asOfTime);
    /** 将业务范围表达式解析为资产列表。 */
    List<MarketAssetView> resolveScope(ScopeResolveRequest request);
    /** 分页查询历史同步批次及其执行统计。 */
    PageResult<SyncBatchView> batches(String masterDataType, String datasetCode, String status, Long bindingId, String triggerType, int pageNo, int pageSize);
    /** 使用旧式直接 Dataset 参数创建手工发布批次。 */
    SyncBatchView publish(String masterDataType, String sourceCode, String taskRunId, String datasetCode, String syncType);
    /** 重新执行既有批次，绑定来源时沿用来源配置。 */
    SyncBatchView republish(Long batchId);
    /** 使用来源绑定配置创建发布批次。 */
    SyncBatchView publishBinding(Long bindingId, String taskRunId, String syncType, TriggerType triggerType);
}
