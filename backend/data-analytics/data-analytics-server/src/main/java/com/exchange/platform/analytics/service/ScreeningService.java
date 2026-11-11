package com.exchange.platform.analytics.service;

import com.exchange.platform.analytics.api.request.ScreeningQueryRequest;
import com.exchange.platform.analytics.api.request.ScreeningResultSaveRequest;
import com.exchange.platform.analytics.api.response.ScreeningResultView;
import com.exchange.platform.analytics.api.response.ScreeningSnapshotView;
import com.exchange.platform.analytics.api.response.TargetScopeOptionsView;
import com.exchange.platform.common.core.enums.AssetType;

/** 指标条件筛选的业务入口；与统一指标读取、排行和对比保持分层。 */
public interface ScreeningService {
    ScreeningResultView execute(ScreeningQueryRequest request);

    TargetScopeOptionsView getScopeOptions(AssetType targetType);

    ScreeningSnapshotView saveResult(ScreeningResultSaveRequest request);

    ScreeningSnapshotView getSavedResult(String resultId);
}
