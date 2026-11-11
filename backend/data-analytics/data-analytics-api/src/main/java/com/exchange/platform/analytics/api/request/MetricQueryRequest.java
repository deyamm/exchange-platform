package com.exchange.platform.analytics.api.request;

import java.time.LocalDateTime;
import java.util.List;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.exchange.platform.common.core.enums.AssetType;

/** 业务页面提交的统一指标查询请求；数据集和字段身份只能由服务端解析。 */
public record MetricQueryRequest(
        @NotNull(message = "标的类型不能为空") AssetType targetType,
        @NotEmpty(message = "标的编码不能为空") List<@NotBlank(message = "标的编码不能为空") String> targetCodes,
        @NotEmpty(message = "指标编码不能为空") List<@NotBlank(message = "指标编码不能为空") String> metricCodes,
        /** null 代表取每个指标当前可用的最新值。 */
        LocalDateTime asOfTime) {
}
