package com.exchange.platform.analytics.api.request;

import java.time.LocalDateTime;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.exchange.platform.common.core.enums.AssetType;

public record RankingQueryRequest(
        @NotNull AssetType targetType,
        @NotNull @Valid TargetScopeRequest targetScope,
        LocalDateTime asOfTime,
        @NotEmpty @Valid List<RankingRuleRequest> rankingRules,
        Integer topN) {
}
