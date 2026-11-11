package com.exchange.platform.data.client.dto;

/** 统一指标值；value 为 null 代表截至时间内缺失或源值为空，绝不转为 0。 */
public record MetricValue(
        String targetType,
        String targetCode,
        String metricCode,
        Object value,
        String dataTime) {
}
