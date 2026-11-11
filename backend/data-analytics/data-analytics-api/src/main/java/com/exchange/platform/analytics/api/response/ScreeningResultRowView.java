package com.exchange.platform.analytics.api.response;

import java.util.Map;

import lombok.Data;

/** 结果表的一行；只含代码、名称和本次条件涉及的指标值，不返回数据时间列。 */
@Data
public class ScreeningResultRowView {
    private String targetCode;
    private String targetName;
    /** key 为 metricCode；value 为 null 时表示该标的该指标缺失。 */
    private Map<String, Object> metricValues;
}
