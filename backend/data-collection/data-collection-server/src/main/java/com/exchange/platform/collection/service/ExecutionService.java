package com.exchange.platform.collection.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.exchange.platform.collection.api.request.execution.TaskRunCreateRequest;
import com.exchange.platform.collection.api.request.execution.TaskRunRequest;
import com.exchange.platform.collection.api.response.execution.TaskRunDetailView;
import com.exchange.platform.collection.api.response.execution.TaskRunView;
import com.exchange.platform.collection.entity.execution.TaskRun;
import com.exchange.platform.common.core.page.PageResult;
import com.exchange.platform.data.client.dto.TaskExecuteResult;


public interface ExecutionService extends IService<TaskRun> {
    
    /** 
     * 创建执行记录并生成执行配置快照 
     */
    TaskRunView createTaskRun(TaskRunCreateRequest request);

    /**
     * 删除执行记录
     * @param runId
     * @return
     */
    void deleteTaskRun(String runId);

    /**
     * 异步执行任务运行，该方法会立刻收到任务处理层返回的状态，并通过回调接口（processTaskRunCallback）接收最终的执行结果
     * @param runId
     * @return
     */
    TaskRunView executeTaskRun(String runId);

    /**
     * 分页查询执行记录列表
     */
    PageResult<TaskRunView> queryTaskRuns(TaskRunRequest params);

    /**
     * 根据条件查询执行记录详情
     * @param runId
     * @return
     */
    TaskRunDetailView getTaskRunDetail(String runId);
    
    /**
     * 处理任务处理层回调（异步执行完成后回传执行结果、日志、问题和异常）
     */
    void processTaskRunCallback(TaskExecuteResult callbackRequest);
    
}
