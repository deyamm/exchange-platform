package com.exchange.platform.analytics.converter;

import com.exchange.platform.analytics.api.response.ResearchTagView;
import com.exchange.platform.analytics.entity.ResearchTag;

public class ResearchTagConverter {

    private ResearchTagConverter() {
    }

    public static ResearchTagView toResearchTagView(ResearchTag entity) {
        if (entity == null) {
            return null;
        }
        ResearchTagView view = new ResearchTagView();
        view.setTagCode(entity.getTagCode());
        view.setTagName(entity.getTagName());
        view.setTagCategory(entity.getTagCategory());
        view.setStatus(entity.getStatus());
        view.setDescription(entity.getDescription());
        view.setCreatedAt(entity.getCreatedAt());
        view.setUpdatedAt(entity.getUpdatedAt());
        return view;
    }
}