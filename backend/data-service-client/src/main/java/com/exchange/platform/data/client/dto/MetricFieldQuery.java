package com.exchange.platform.data.client.dto;

/** 一个应用指标在某个 Dataset 中对应的受控物理字段。 */
public record MetricFieldQuery(
        String metricCode,
        String physicalColumnName) {
}
