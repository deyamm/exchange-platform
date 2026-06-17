package com.exchange.platform.analytics.api.request;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import com.exchange.platform.analytics.api.enums.CommonStatus;


public record ViewConfigSaveRequest(
    @NotBlank(message = "视图编码不能为空") @Size(max = 64, message = "视图编码长度不能超过64个字符") String viewCode,
    @NotBlank(message = "视图名称不能为空") @Size(max = 128, message = "视图名称长度不能超过128个字符") String viewName,
    @NotBlank(message = "场景编码不能为空") @Size(max = 64, message = "场景编码长度不能超过64个字符") String sceneCode,
    @NotBlank(message = "视图类型不能为空") @Size(max = 64, message = "视图类型长度不能超过64个字符") String viewType,
    Boolean defaultFlag,
    Object fieldConfig,
    Object sortConfig,
    Object filterConfig,
    CommonStatus status
) {}