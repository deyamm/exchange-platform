package com.exchange.platform.analytics.api.response;

import java.time.LocalDateTime;

import com.exchange.platform.analytics.api.request.ScreeningQueryRequest;

import lombok.Data;

/** 可回看的筛选快照，包含当次请求、命中结果和保存时间，不会触发重新计算。 */
@Data
public class ScreeningSnapshotView {
    private String resultId;
    private LocalDateTime executedAt;
    private ScreeningQueryRequest request;
    private ScreeningResultView result;
}
