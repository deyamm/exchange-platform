package com.exchange.platform.analytics.converter;


import com.alibaba.fastjson.JSON;
import com.exchange.platform.analytics.api.response.AppMetricView;
import com.exchange.platform.analytics.entity.AppMetric;

public class MetricConverter {

    
    private MetricConverter() {
    }

    public static AppMetricView toMetricView(AppMetric entity) {
        if (entity == null) {
            return null;
        }
        AppMetricView view = new AppMetricView();
        view.setMetricCode(entity.getMetricCode());
        view.setMetricName(entity.getMetricName());
        view.setMetricCategory(entity.getMetricCategory());
        view.setUnit(entity.getUnit());
        view.setDisplayFormat(entity.getDisplayFormat());
        view.setDataSourceCode(entity.getDataSourceCode());
        view.setCalcPeriod(entity.getCalcPeriod());
        view.setSceneCodes(JSON.parseArray(entity.getSceneCodesJson(), String.class));
        view.setStatus(entity.getStatus());
        view.setCaliberDesc(entity.getCaliberDesc());
        view.setCreatedAt(entity.getCreatedAt());
        view.setUpdatedAt(entity.getUpdatedAt());
        return view;
    }
}