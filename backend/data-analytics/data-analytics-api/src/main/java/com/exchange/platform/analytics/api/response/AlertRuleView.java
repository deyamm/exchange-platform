package com.exchange.platform.analytics.api.response;

import java.time.LocalDateTime;

import com.exchange.platform.analytics.api.enums.CommonStatus;
import com.exchange.platform.analytics.api.enums.RuleType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AlertRuleView {

    private String ruleCode;
    private String ruleName;
    private RuleType ruleType;
    private String targetCode;
    private String groupCode;
    private String metricCode;
    private Object condition;
    private Object noticeChannels;
    private LocalDateTime effectiveStartTime;
    private LocalDateTime effectiveEndTime;
    private CommonStatus status;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}