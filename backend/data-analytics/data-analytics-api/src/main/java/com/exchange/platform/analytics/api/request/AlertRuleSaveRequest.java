package com.exchange.platform.analytics.api.request;

import java.time.LocalDateTime;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import com.exchange.platform.analytics.api.enums.CommonStatus;
import com.exchange.platform.analytics.api.enums.RuleType;


public record AlertRuleSaveRequest(
    @NotBlank(message = "规则编码不能为空") @Size(max = 64, message = "规则编码长度不能超过64个字符") String ruleCode,
    @NotBlank(message = "规则名称不能为空") @Size(max = 128, message = "规则名称长度不能超过128个字符") String ruleName,
    RuleType ruleType,
    String targetCode,
    String groupCode,
    String metricCode,
    Object condition,
    Object noticeChannels,
    LocalDateTime effectiveStartTime,
    LocalDateTime effectiveEndTime,
    CommonStatus status,
    String description
) {}