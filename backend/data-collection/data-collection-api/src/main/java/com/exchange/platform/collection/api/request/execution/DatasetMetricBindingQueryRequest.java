package com.exchange.platform.collection.api.request.execution;

import java.util.List;

import javax.validation.constraints.NotEmpty;

/**
 * 批量读取指标配置所需数据集元数据的只读请求。
 * <p>该请求只返回 Dataset 主档与当前版本字段快照，不暴露物理表或存储映射。</p>
 */
public record DatasetMetricBindingQueryRequest(
    @NotEmpty(message = "数据集编码不能为空") List<String> datasetCodes
) {}
