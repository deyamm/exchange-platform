package com.exchange.platform.analytics.api.request;

import java.util.List;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import com.exchange.platform.analytics.api.enums.WatchStatus;


public record WatchTargetSaveRequest(
    @NotBlank(message = "标的编码不能为空") @Size(max = 64) String targetCode,
    @NotBlank(message = "标的名称不能为空") @Size(max = 128) String targetName,
    @NotBlank(message = "标的类型不能为空") String targetType,
    String marketCode,
    List<String> groupCodes,
    Integer sortNo,
    Boolean importantFlag,
    WatchStatus watchStatus,
    String watchReason
) {}
