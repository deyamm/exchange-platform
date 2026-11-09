package com.exchange.platform.analytics.converter;


import com.exchange.platform.analytics.api.response.AppMetricView;
import com.exchange.platform.analytics.entity.AppMetric;
import com.exchange.platform.common.core.enums.AssetType;

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
        view.setTargetType(AssetType.fromCode(entity.getTargetType()));
        view.setTargetTypeName(entity.getTargetType() == null ? null : entity.getTargetType());
        view.setUnit(entity.getUnit());
        view.setDatasetCode(entity.getDatasetCode());
        view.setFieldCode(entity.getFieldCode());
        view.setStatus(entity.getStatus());
        view.setDescription(entity.getDescription());
        view.setCreatedAt(entity.getCreatedAt());
        view.setUpdatedAt(entity.getUpdatedAt());
        return view;
    }
}
