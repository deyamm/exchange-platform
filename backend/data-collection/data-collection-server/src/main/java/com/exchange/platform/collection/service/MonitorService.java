package com.exchange.platform.collection.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.exchange.platform.collection.api.request.execution.TaskRunRequest;
import com.exchange.platform.collection.api.response.monitor.ExceptionRecordView;
import com.exchange.platform.collection.api.response.monitor.ProblemRecordView;
import com.exchange.platform.collection.api.response.monitor.RunLogView;
import com.exchange.platform.collection.api.response.monitor.TraceView;
import com.exchange.platform.collection.entity.monitor.RunLog;
import com.exchange.platform.common.core.page.PageResult;


public interface MonitorService extends IService<RunLog> {
    
    /**
     * 分页查询运行日志
     * @param params 查询参数
     * @return 分页结果
     */
    PageResult<RunLogView> queryRunLogs(TaskRunRequest params);

    /**
     * 分页查询异常记录
     * @param params 查询参数
     * @return 分页结果
     */
    PageResult<ExceptionRecordView> queryExceptionRecords(TaskRunRequest params);

    /**
     * 分页查询问题记录
     * @param params 查询参数
     * @return 分页结果
     */
    PageResult<ProblemRecordView> queryProblemRecords(TaskRunRequest params);

    /**
     * 按runId聚合查询历史执行主线
     * 聚合执行配置快照、任务模板版本、结果摘要、数据集和运行留痕索引。
     * @param runId 执行记录编号
     * @return 历史执行主线视图
     */
    TraceView getTrace(String runId);
}
