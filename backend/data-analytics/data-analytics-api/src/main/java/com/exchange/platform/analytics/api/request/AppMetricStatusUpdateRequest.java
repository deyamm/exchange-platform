package com.exchange.platform.analytics.api.request;

import javax.validation.constraints.NotNull;

import com.exchange.platform.analytics.api.enums.CommonStatus;

/** 单独更新应用指标启停状态，防止普通编辑绕过停用引用校验。 */
public record AppMetricStatusUpdateRequest(
    @NotNull(message = "指标状态不能为空") CommonStatus status
) {}
