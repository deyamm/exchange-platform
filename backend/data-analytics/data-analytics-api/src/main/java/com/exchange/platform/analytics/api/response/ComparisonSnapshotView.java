package com.exchange.platform.analytics.api.response;

import java.time.LocalDateTime;

import lombok.Data;

import com.exchange.platform.analytics.api.request.ComparisonQueryRequest;

@Data
public class ComparisonSnapshotView {
    private String resultId;
    private LocalDateTime executedAt;
    private ComparisonQueryRequest request;
    private ComparisonResultView result;
}
