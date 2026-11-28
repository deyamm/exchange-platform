package com.exchange.platform.analytics.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.exchange.platform.analytics.api.enums.MasterDataType;
import com.exchange.platform.analytics.api.enums.TriggerType;
import com.exchange.platform.analytics.api.request.PublishedMasterDataQueryRequest;
import com.exchange.platform.analytics.api.response.PublishedMasterDataTypeDetailView;
import com.exchange.platform.analytics.api.response.PublishedMasterDataTypeView;
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

    /** 查询至少成功发布过一次的主数据类型。 */
    List<PublishedMasterDataTypeView> publishedTypes(String keyword);

    /** 查询已发布主数据类型的页面基础信息。 */
    PublishedMasterDataTypeDetailView publishedTypeDetail(MasterDataType type);

    /** 按注册表逻辑字段分页查询已发布主数据。 */
    PageResult<Map<String, Object>> queryPublished(PublishedMasterDataQueryRequest request);

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
