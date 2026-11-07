package com.exchange.platform.collection.api.response.execution;

import java.util.List;

import lombok.Data;

/** 数据集详情聚合视图，包含主档、元数据、字段结构和最近执行信息。 */
@Data
public class DatasetDetailView {
    /** 数据集主档信息。 */
    private DatasetView dataset;
    /** 数据集扩展元数据。 */
    private List<DatasetMetadataView> metadata;
    /** 当前数据类型版本的字段结构。 */
    private List<?> structure;
    /** 最近一次成功执行记录。 */
    private TaskRunView latestTaskRun;
    /** 最近一次执行结果摘要。 */
    private ResultSummaryView latestResultSummary;
}
