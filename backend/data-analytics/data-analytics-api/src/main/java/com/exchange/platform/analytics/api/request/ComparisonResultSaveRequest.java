package com.exchange.platform.analytics.api.request;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.exchange.platform.analytics.api.response.ComparisonResultView;

public record ComparisonResultSaveRequest(
        @NotNull @Valid ComparisonQueryRequest request,
        @NotNull ComparisonResultView result) {
}
