package com.exchange.platform.collection.api.response.execution;

import lombok.Data;

/**
 * 执行记录详情视图（I-27 聚合对象）
 * 聚合执行基础信息、执行配置快照、结果摘要和数据集信息。
 */
@Data
public class TaskRunDetailView {

    /** 执行记录基础信息 */
    private TaskRunView taskRun;

    /** 执行配置快照 */
    private ExecutionConfigSnapshotView snapshot;

    /** 结果摘要 */
    private ResultSummaryView resultSummary;

}