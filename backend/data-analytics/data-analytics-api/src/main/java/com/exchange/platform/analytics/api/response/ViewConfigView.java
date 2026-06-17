package com.exchange.platform.analytics.api.response;

import java.time.LocalDateTime;

import com.exchange.platform.analytics.api.enums.CommonStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ViewConfigView {

    private String viewCode;
    private String viewName;
    private String sceneCode;
    private String viewType;
    private Boolean defaultFlag;
    private Object fieldConfig;
    private Object sortConfig;
    private Object filterConfig;
    private CommonStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}