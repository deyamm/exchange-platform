package com.exchange.platform.collection.api.response.monitor;

import java.util.List;

import com.exchange.platform.collection.api.response.execution.ExecutionConfigSnapshotView;
import com.exchange.platform.collection.api.response.execution.ResultSummaryView;
import com.exchange.platform.collection.api.response.execution.TaskRunView;
import com.exchange.platform.collection.api.response.tasks.TaskTemplateVersionView;

import lombok.Data;

/**
 * 历史追溯视图（I-33 聚合对象）
 * 以 runId 为主线聚合执行配置快照、模板版本、结果摘要、数据集和运行留痕索引。
 */
@Data
public class TraceView {

    /** 执行记录基础信息 */
    private TaskRunView taskRun;

    /** 执行配置快照（执行时上下文） */
    private ExecutionConfigSnapshotView snapshot;

    /** 执行时任务模板版本 */
    private TaskTemplateVersionView templateVersion;

    /** 结果摘要 */
    private ResultSummaryView resultSummary;

    /** 运行日志索引 */
    private List<RunLogView> runLogs;

    /** 异常记录索引 */
    private List<ExceptionRecordView> exceptionRecords;

    /** 问题数据索引 */
    private List<ProblemRecordView> problemRecords;
}
