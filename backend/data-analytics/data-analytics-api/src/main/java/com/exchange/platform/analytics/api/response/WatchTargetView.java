package com.exchange.platform.analytics.api.response;

import java.time.LocalDateTime;
import java.util.List;

import com.exchange.platform.analytics.api.enums.WatchStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WatchTargetView {

    private String targetCode;
    private String targetName;
    private String targetType;
    private String marketCode;
    private List<TargetGroupView> targetGroups;
    private Integer sortNo;
    private Boolean importantFlag;
    private WatchStatus watchStatus;
    private String watchReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}