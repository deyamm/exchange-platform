package com.exchange.platform.analytics.api.response;

import java.time.LocalDateTime;

import lombok.Data;

import com.exchange.platform.analytics.api.request.RankingQueryRequest;

@Data
public class RankingSnapshotView {
    private String resultId;
    private LocalDateTime executedAt;
    private RankingQueryRequest request;
    private RankingResultView result;
}
