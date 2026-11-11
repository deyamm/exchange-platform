package com.exchange.platform.analytics.api.request;

import javax.validation.constraints.NotBlank;

/**
 * 单个指标筛选条件。value 保留为 JSON 值，便于 BETWEEN 使用两个数值、IN 使用数组；
 * 实际比较、数值解析与缺失值处理全部由服务端完成。
 */
public record MetricConditionRequest(
        @NotBlank(message = "指标编码不能为空") String metricCode,
        @NotBlank(message = "比较操作符不能为空") String operator,
        Object value) {
}
