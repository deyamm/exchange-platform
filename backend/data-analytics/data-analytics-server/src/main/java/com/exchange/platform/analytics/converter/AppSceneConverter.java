package com.exchange.platform.analytics.converter;

import com.exchange.platform.analytics.api.response.AppSceneView;
import com.exchange.platform.analytics.entity.AppScene;

public class AppSceneConverter {
    
    private AppSceneConverter() {}

    public static AppSceneView toView(AppScene appScene) {
        if (appScene == null) {
            return null;
        }
        AppSceneView view = new AppSceneView();
        view.setSceneCode(appScene.getSceneCode());
        view.setSceneName(appScene.getSceneName());
        view.setSceneType(appScene.getSceneType());
        view.setSortNo(appScene.getSortNo());
        view.setStatus(appScene.getStatus());
        view.setDescription(appScene.getDescription());
        view.setCreatedAt(appScene.getCreatedAt());
        return view;
    }
}
