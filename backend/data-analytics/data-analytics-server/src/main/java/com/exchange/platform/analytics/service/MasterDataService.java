package com.exchange.platform.analytics.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.exchange.platform.analytics.api.enums.IndexCategory;
import com.exchange.platform.analytics.api.enums.StockMarketType;
import com.exchange.platform.analytics.api.enums.TriggerType;
import com.exchange.platform.analytics.api.response.SyncBatchView;
import com.exchange.platform.common.core.enums.MarketAssetType;
import com.exchange.platform.common.core.page.PageResult;

/**
 * 主数据查询、范围解析及发布能力的应用服务接口。
 *
 * <p>
 * 对外 API 保持不变；内部发布实现切换为 PublishPlan + ApplicationService + Executor 的单向依赖结构。
 * </p>
 */
public interface MasterDataService {

    /** 按代码或名称搜索资产，结果数量由 limit 限制。 */
    List<Map<String, Object>> search(String keyword, MarketAssetType marketAssetType, int limit);

    /** 分页查询资产主数据。 */
    PageResult<Map<String, Object>> list(String keyword, MarketAssetType marketAssetType,
            StockMarketType marketType, String exchangeCode, String publisherCode,
            IndexCategory indexCategory, String schemeCode, Integer level, int pageNo, int pageSize);

    /** 查询单个资产及其股票/指数扩展属性。 */
    Map<String, Object> detail(MarketAssetType marketAssetType, String assetCode);

    /** 按资产代码批量查询主数据。 */
    List<Map<String, Object>> findByCodes(MarketAssetType marketAssetType, Collection<String> assetCodes);

    /**
     * 将业务范围解析为主数据记录。
     *
     * <p>
     * 当前支持 ALL、ASSET_CODE、INDEX_TYPE、PUBLISHER、MARKET。
     * BOARD/指数成分能力不属于 MasterData，由独立指数成分领域负责。
     * </p>
     */
    List<Map<String, Object>> resolveScope(MarketAssetType marketAssetType, String scopeType,
            List<String> scopeValues);

    /** 分页查询历史同步批次及其执行统计。 */
    PageResult<SyncBatchView> batches(String masterDataType, String datasetCode, String status, Long bindingId,
            String triggerType, int pageNo, int pageSize);

    /** 使用标准字段 Dataset 创建手工发布批次。 */
    SyncBatchView publish(String masterDataType, String sourceCode, String taskRunId, String datasetCode,
            String syncType);

    /** 重新执行既有批次；Binding 批次使用当前最新绑定配置。 */
    SyncBatchView republish(Long batchId);

    /** 使用来源绑定配置创建发布批次。 */
    SyncBatchView publishBinding(Long bindingId, String taskRunId, String syncType, TriggerType triggerType);
}
