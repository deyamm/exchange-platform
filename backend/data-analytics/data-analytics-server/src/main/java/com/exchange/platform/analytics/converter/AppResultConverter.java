package com.exchange.platform.analytics.converter;

import com.alibaba.fastjson.JSON;
import com.exchange.platform.analytics.api.response.AppResultView;
import com.exchange.platform.analytics.entity.AppResult;


public class AppResultConverter {
    private AppResultConverter() {
    }

    public static AppResultView toView(AppResult result) {
        if (result == null) {
            return null;
        }
        AppResultView view = new AppResultView();
        view.setResultId(result.getResultId());
        view.setResultType(result.getResultType());
        view.setSceneCode(result.getSceneCode());
        view.setTargetCode(result.getTargetCode());
        view.setMetricCode(result.getMetricCode());
        view.setRelatedObjectType(result.getRelatedObjectType());
        view.setRelatedObjectId(result.getRelatedObjectId());
        view.setResultSummary(JSON.parseObject(result.getResultSummaryJson()));
        view.setSourceDataTime(result.getSourceDataTime());
        view.setCreatedAt(result.getCreatedAt());
        return view;
    }
}
