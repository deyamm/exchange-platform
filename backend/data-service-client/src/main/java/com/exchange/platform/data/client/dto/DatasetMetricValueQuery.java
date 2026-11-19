package com.exchange.platform.data.client.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 平台统一 Dataset 读取请求。页面始终只提交 metricCode；物理存储和列映射
 * 由数据读取执行端根据 datasetCode 的平台元数据解析。
 */
public record DatasetMetricValueQuery(
        String targetType,
        String datasetCode,
        String storageType,
        String dataSourceCode,
        String schemaName,
        String tableName,
        String targetCodeColumn,
        String dataTimeColumn,
        List<String> targetCodes,
        List<MetricFieldQuery> metrics,
        LocalDateTime asOfTime) {
}
