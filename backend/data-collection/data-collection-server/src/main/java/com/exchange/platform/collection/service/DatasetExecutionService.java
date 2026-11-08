package com.exchange.platform.collection.service;

import com.exchange.platform.collection.entity.execution.ExecutionConfigSnapshot;
import com.exchange.platform.collection.entity.execution.ResultSummary;
import com.exchange.platform.collection.entity.execution.TaskRun;
import com.exchange.platform.collection.entity.tasks.StorageMapping;

/**
 * 数据集执行流程内部服务。
 * <p>仅供执行服务调用，不作为数据集控制器的对外查询维护接口。</p>
 */
public interface DatasetExecutionService {

    /** 校验任务执行所使用的数据集身份和当前状态。 */
    void validateForExecution(StorageMapping mapping);

    /** 根据一次成功执行结果创建或更新数据集主档及存储元数据。 */
    void registerSuccessfulRun(TaskRun taskRun, ExecutionConfigSnapshot snapshot,
            StorageMapping mapping, ResultSummary summary);
}
