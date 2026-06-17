package com.exchange.platform.analytics.api.response;

import java.time.LocalDateTime;

import com.exchange.platform.analytics.api.enums.CommonStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TargetGroupView {

    private String groupCode;
    private String groupName;
    private Integer sortNo;
    private CommonStatus status;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}