package com.exchange.platform.analytics.api.response;

import java.time.LocalDateTime;
import java.util.List;

import com.exchange.platform.analytics.api.enums.CommonStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AppMetricView {

    private String metricCode;
    private String metricName;
    private String metricCategory;
    private String unit;
    private String displayFormat;
    private String dataSourceCode;
    private String calcPeriod;
    private List<String> sceneCodes;
    private CommonStatus status;
    private String caliberDesc;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}