package com.exchange.platform.collection.service;

import java.util.List;

import com.exchange.platform.collection.api.request.execution.DatasetMetadataUpdateRequest;
import com.exchange.platform.collection.api.request.execution.DatasetMetricBindingQueryRequest;
import com.exchange.platform.collection.api.response.execution.DatasetDetailView;
import com.exchange.platform.collection.api.response.execution.DatasetMetricBindingView;
import com.exchange.platform.collection.api.response.execution.DatasetMetadataView;
import com.exchange.platform.collection.api.response.execution.DatasetView;
import com.exchange.platform.common.core.page.PageResult;
import com.exchange.platform.common.core.enums.AssetType;

/**
 * 数据集管理服务。
 * <p>负责数据集目录查询、详情查看以及元数据维护。</p>
 */
public interface DatasetService {
    /** 按条件分页查询数据集目录。 */
    PageResult<DatasetView> query(String datasetCode, String datasetName, String topicCode, String dataTypeCode, AssetType assetType,
        Integer datasetStatus, String taskConfigCode, String startTime, String endTime, Integer pageNo, Integer pageSize);

    /** 查询数据集详情及最近执行信息。 */
    DatasetDetailView getDetail(String datasetCode);
    /** 查询数据集扩展元数据。 */
    List<DatasetMetadataView> getMetadata(String datasetCode);
    /** 更新用户可维护的数据集扩展元数据。 */
    void updateMetadata(String datasetCode, DatasetMetadataUpdateRequest request);
    /** 查询当前数据类型版本的字段结构。 */
    List<?> getStructure(String datasetCode);
    /** 批量返回指标配置所需的 Dataset 状态和当前版本标准字段。 */
    List<DatasetMetricBindingView> getMetricBindings(DatasetMetricBindingQueryRequest request);
}
