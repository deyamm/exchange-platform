package com.exchange.platform.analytics.api.request;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import com.exchange.platform.analytics.api.enums.CommonStatus;


public record TargetGroupSaveRequest(
    @NotBlank(message = "分组编码不能为空") @Size(max = 64, message = "分组编码长度不能超过64个字符") 
    String groupCode,
    @NotBlank(message = "分组名称不能为空") @Size(max = 128, message = "分组名称长度不能超过128个字符") 
    String groupName,
    Integer sortNo,
    CommonStatus status,
    String description
) {}