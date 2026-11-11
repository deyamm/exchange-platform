package com.exchange.platform.analytics.service;

import com.exchange.platform.analytics.api.request.ComparisonQueryRequest;
import com.exchange.platform.analytics.api.request.ComparisonResultSaveRequest;
import com.exchange.platform.analytics.api.response.ComparisonResultView;
import com.exchange.platform.analytics.api.response.ComparisonSnapshotView;

public interface ComparisonService {
    ComparisonResultView execute(ComparisonQueryRequest request);
    ComparisonSnapshotView saveResult(ComparisonResultSaveRequest request);
    ComparisonSnapshotView getSavedResult(String resultId);
}
