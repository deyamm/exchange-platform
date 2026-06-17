package com.exchange.platform.analytics.converter;

import com.alibaba.fastjson.JSON;
import com.exchange.platform.analytics.api.response.ViewConfigView;
import com.exchange.platform.analytics.entity.ViewConfig;


public class ViewConfigConverter {

    private ViewConfigConverter() {
    }

    public static ViewConfigView toViewConfigView(ViewConfig entity) {
        if (entity == null) {
            return null;
        }
        ViewConfigView view = new ViewConfigView();
        view.setViewCode(entity.getViewCode());
        view.setViewName(entity.getViewName());
        view.setSceneCode(entity.getSceneCode());
        view.setViewType(entity.getViewType());
        view.setDefaultFlag(entity.getDefaultFlag());
        view.setFieldConfig(JSON.parse(entity.getFieldConfigJson()));
        view.setSortConfig(JSON.parse(entity.getSortConfigJson()));
        view.setFilterConfig(JSON.parse(entity.getFilterConfigJson()));
        view.setStatus(entity.getStatus());
        view.setCreatedAt(entity.getCreatedAt());
        view.setUpdatedAt(entity.getUpdatedAt());
        return view;
    }
}