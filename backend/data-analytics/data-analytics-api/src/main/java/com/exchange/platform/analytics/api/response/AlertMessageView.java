package com.exchange.platform.analytics.api.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AlertMessageView {
    private Long id;
    private Long ruleId;
    private String targetType;
    private String targetCode;
    private String targetNameSnapshot;
    private String metricCode;
    private String metricNameSnapshot;
    private String unitSnapshot;
    private String operator;
    private BigDecimal threshold;
    private BigDecimal triggerValue;
    private String messageTitle;
    private String messageContent;
    private LocalDateTime dataTime;
    private LocalDateTime triggeredAt;
    private String readStatus;
    private String processStatus;
}
