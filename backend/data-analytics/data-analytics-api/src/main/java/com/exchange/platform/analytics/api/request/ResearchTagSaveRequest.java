package com.exchange.platform.analytics.api.request;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import com.exchange.platform.analytics.api.enums.CommonStatus;


public record ResearchTagSaveRequest(
    @NotBlank(message = "标签编码不能为空") @Size(max = 64, message = "标签编码长度不能超过64个字符") String tagCode,
    @NotBlank(message = "标签名称不能为空") @Size(max = 128, message = "标签名称长度不能超过128个字符") String tagName,
    String tagCategory,
    CommonStatus status,
    String description
) {}