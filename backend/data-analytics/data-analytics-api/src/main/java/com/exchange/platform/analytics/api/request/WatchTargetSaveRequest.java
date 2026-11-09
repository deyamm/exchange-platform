package com.exchange.platform.analytics.api.request;

import java.util.List;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.exchange.platform.analytics.api.enums.WatchStatus;
import com.exchange.platform.common.core.enums.AssetType;


public record WatchTargetSaveRequest(
    @NotBlank(message = "标的编码不能为空") @Size(max = 64) String targetCode,
    @NotBlank(message = "标的名称不能为空") @Size(max = 128) String targetName,
    /** 标的类型与应用指标使用同一套协议值，拒绝 OTHER、中文等非标准编码。 */
    @NotNull(message = "标的类型不能为空") AssetType targetType,
    String marketCode,
    List<String> groupCodes,
    Integer sortNo,
    Boolean importantFlag,
    WatchStatus watchStatus,
    String watchReason
) {}
