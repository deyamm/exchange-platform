package com.exchange.platform.analytics.api.request;

import java.time.LocalDateTime;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.exchange.platform.common.core.enums.MarketAssetType;

public record RankingQueryRequest(
        @NotNull MarketAssetType targetType,
        @NotNull @Valid TargetScopeRequest targetScope,
        LocalDateTime asOfTime,
        @NotEmpty @Valid List<RankingRuleRequest> rankingRules,
        Integer topN) {
}
