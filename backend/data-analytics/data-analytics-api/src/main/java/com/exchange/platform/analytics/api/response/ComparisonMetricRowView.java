package com.exchange.platform.analytics.api.response;

import java.util.Map;

import lombok.Data;

@Data
public class ComparisonMetricRowView {
    private String metricCode;
    private String metricName;
    private String unit;
    private Map<String, Object> values;
    private Map<String, String> dataTimes;
}
