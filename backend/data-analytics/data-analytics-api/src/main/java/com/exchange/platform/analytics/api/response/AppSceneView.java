package com.exchange.platform.analytics.api.response;

import java.time.LocalDateTime;

import com.exchange.platform.analytics.api.enums.AppSceneType;
import com.exchange.platform.analytics.api.enums.CommonStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AppSceneView {

    private String sceneCode;
    private String sceneName;
    private AppSceneType sceneType;
    private Integer sortNo;
    private CommonStatus status;
    private String description;
    private LocalDateTime createdAt;
}
