package com.exchange.platform.analytics.api.request;

import java.time.LocalDateTime;
import java.math.BigDecimal;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import com.exchange.platform.analytics.api.enums.CommonStatus;
import com.exchange.platform.analytics.api.enums.RuleType;
import com.exchange.platform.common.core.enums.AssetType;


public record AlertRuleSaveRequest(
    @NotBlank(message = "规则编码不能为空") @Size(max = 64, message = "规则编码长度不能超过64个字符") String ruleCode,
    @NotBlank(message = "规则名称不能为空") @Size(max = 128, message = "规则名称长度不能超过128个字符") String ruleName,
    RuleType ruleType,
    String targetCode,
    /** 具体标的提醒使用 targetType + targetCode 作为标的身份。 */
    AssetType targetType,
    String groupCode,
    String metricCode,
    Object condition,
    /** 指标提醒条件的规范字段；旧客户端仍可通过 condition JSON 提交。 */
    String operator,
    BigDecimal threshold,
    Object noticeChannels,
    LocalDateTime effectiveStartTime,
    LocalDateTime effectiveEndTime,
    CommonStatus status,
    String description
) {}
