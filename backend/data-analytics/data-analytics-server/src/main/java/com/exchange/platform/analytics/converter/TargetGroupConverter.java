package com.exchange.platform.analytics.converter;

import com.exchange.platform.analytics.api.response.TargetGroupView;
import com.exchange.platform.analytics.entity.TargetGroup;

public class TargetGroupConverter {

    private TargetGroupConverter() {
    }

    public static TargetGroupView toTargetGroupView(TargetGroup entity) {
        if (entity == null) {
            return null;
        }
        TargetGroupView view = new TargetGroupView();
        view.setGroupCode(entity.getGroupCode());
        view.setGroupName(entity.getGroupName());
        view.setSortNo(entity.getSortNo());
        view.setStatus(entity.getStatus());
        view.setDescription(entity.getDescription());
        view.setCreatedAt(entity.getCreatedAt());
        view.setUpdatedAt(entity.getUpdatedAt());
        return view;
    }
}