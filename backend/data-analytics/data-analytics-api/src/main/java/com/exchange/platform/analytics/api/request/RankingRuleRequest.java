package com.exchange.platform.analytics.api.request;

import javax.validation.constraints.NotBlank;

/** 单个排行键；列表顺序即排序优先级。 */
public record RankingRuleRequest(
        @NotBlank(message = "排行指标不能为空") String metricCode,
        String sortDirection,
        Integer priority) {
}
