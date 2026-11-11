package com.exchange.platform.analytics.api.response;

import java.time.LocalDateTime;
import java.math.BigDecimal;

import com.exchange.platform.analytics.api.enums.CommonStatus;
import com.exchange.platform.analytics.api.enums.RuleType;
import com.exchange.platform.common.core.enums.AssetType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AlertRuleView {

    private String ruleCode;
    private String ruleName;
    private RuleType ruleType;
    private String targetCode;
    /** 具体标的提醒的标准标的类型。 */
    private AssetType targetType;
    private String groupCode;
    private String metricCode;
    private String operator;
    private BigDecimal threshold;
    private Object condition;
    private Object noticeChannels;
    private LocalDateTime effectiveStartTime;
    private LocalDateTime effectiveEndTime;
    private CommonStatus status;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
