package com.exchange.platform.analytics.api.request;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import com.exchange.platform.analytics.api.enums.AppSceneType;
import com.exchange.platform.analytics.api.enums.CommonStatus;


public record SceneSaveRequest(
    @NotBlank(message = "场景编码不能为空") @Size(max = 32, message = "场景编码长度不能超过32个字符") 
    String sceneCode,

    @Size(max = 64, message = "场景名称长度不能超过64个字符") 
    String sceneName,

    AppSceneType sceneType,

    Integer sortNo,

    CommonStatus status,
    
    String description
) {}
