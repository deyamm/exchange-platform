package com.exchange.platform.analytics.api.request;

import java.time.LocalDateTime;
import java.util.List;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.exchange.platform.common.core.enums.AssetType;

public record ComparisonQueryRequest(
        @NotNull AssetType targetType,
        @NotEmpty List<String> targetCodes,
        @NotEmpty List<String> metricCodes,
        LocalDateTime asOfTime) {
}
