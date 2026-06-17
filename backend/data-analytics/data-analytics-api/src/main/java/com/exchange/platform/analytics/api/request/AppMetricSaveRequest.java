package com.exchange.platform.analytics.api.request;

import java.util.List;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import com.exchange.platform.analytics.api.enums.CommonStatus;


public record AppMetricSaveRequest(
    @NotBlank(message = "指标编码不能为空") @Size(max = 64, message = "指标编码长度不能超过64个字符") String metricCode,
    @NotBlank(message = "指标名称不能为空") @Size(max = 128, message = "指标名称长度不能超过128个字符") String metricName,
    @NotBlank(message = "指标分类不能为空") @Size(max = 64, message = "指标分类长度不能超过64个字符") String metricCategory,
    String unit,
    String displayFormat,
    String dataSourceCode,
    String calcPeriod,
    List<String> sceneCodes,
    CommonStatus status,
    String caliberDesc
) {}