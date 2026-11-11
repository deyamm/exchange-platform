package com.exchange.platform.analytics.api.response;

import java.util.Map;

import lombok.Data;

@Data
public class RankingResultRowView {
    private long rank;
    private String targetCode;
    private String targetName;
    private Map<String, Object> metricValues;
}
