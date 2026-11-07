package com.exchange.platform.collection.service;

import java.util.List;

import com.exchange.platform.collection.api.request.execution.DatasetMetadataUpdateRequest;
import com.exchange.platform.collection.api.response.execution.DatasetDetailView;
import com.exchange.platform.collection.api.response.execution.DatasetMetadataView;
import com.exchange.platform.collection.api.response.execution.DatasetView;
import com.exchange.platform.collection.entity.execution.ExecutionConfigSnapshot;
import com.exchange.platform.collection.entity.execution.ResultSummary;
import com.exchange.platform.collection.entity.execution.TaskRun;
import com.exchange.platform.collection.entity.tasks.StorageMapping;
import com.exchange.platform.common.core.page.PageResult;

/**
 * 数据集管理服务。
 * <p>负责执行前数据集校验、成功执行后的数据集登记以及数据集查询维护。</p>
 */
public interface DatasetService {
    /** 校验任务执行所使用的数据集身份和当前状态。 */
    void validateForExecution(StorageMapping mapping);
    /** 根据一次成功执行结果创建或更新数据集主档及存储元数据。 */
    void registerSuccessfulRun(TaskRun taskRun, ExecutionConfigSnapshot snapshot, StorageMapping mapping, ResultSummary summary);
    /** 按条件分页查询数据集目录。 */
    PageResult<DatasetView> query(String datasetCode, String datasetName, String topicCode, String dataTypeCode,
        Integer datasetStatus, String taskConfigCode, String startTime, String endTime, Integer pageNo, Integer pageSize);
    /** 查询数据集详情及最近执行信息。 */
    DatasetDetailView getDetail(String datasetCode);
    /** 查询数据集扩展元数据。 */
    List<DatasetMetadataView> getMetadata(String datasetCode);
    /** 更新用户可维护的数据集扩展元数据。 */
    void updateMetadata(String datasetCode, DatasetMetadataUpdateRequest request);
    /** 查询当前数据类型版本的字段结构。 */
    List<?> getStructure(String datasetCode);
}
