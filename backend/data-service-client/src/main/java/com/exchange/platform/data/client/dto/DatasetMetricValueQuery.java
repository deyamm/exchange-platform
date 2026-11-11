package com.exchange.platform.data.client.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring 按 Dataset 聚合后提交给 FastAPI 的单 Dataset 批量指标请求。
 * 此 DTO 只存在于服务间调用，页面始终只提交 metricCode。
 */
public record DatasetMetricValueQuery(
        String targetType,
        String datasetCode,
        String physicalSchemaName,
        String physicalTableName,
        String targetCodeColumn,
        String dataTimeColumn,
        List<String> targetCodes,
        List<MetricFieldQuery> metrics,
        LocalDateTime asOfTime) {
}
