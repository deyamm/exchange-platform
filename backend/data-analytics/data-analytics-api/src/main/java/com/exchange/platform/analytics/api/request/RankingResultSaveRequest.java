package com.exchange.platform.analytics.api.request;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.exchange.platform.analytics.api.response.RankingResultView;

public record RankingResultSaveRequest(
        @NotNull @Valid RankingQueryRequest request,
        @NotNull RankingResultView result) {
}
