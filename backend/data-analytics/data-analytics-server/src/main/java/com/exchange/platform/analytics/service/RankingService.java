package com.exchange.platform.analytics.service;

import com.exchange.platform.analytics.api.request.RankingQueryRequest;
import com.exchange.platform.analytics.api.request.RankingResultSaveRequest;
import com.exchange.platform.analytics.api.response.RankingResultView;
import com.exchange.platform.analytics.api.response.RankingSnapshotView;

public interface RankingService {
    RankingResultView execute(RankingQueryRequest request);
    RankingSnapshotView saveResult(RankingResultSaveRequest request);
    RankingSnapshotView getSavedResult(String resultId);
}
