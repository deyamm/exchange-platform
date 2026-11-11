package com.exchange.platform.analytics.api.request;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;

/** 两层条件树中的一组条件；group 内只允许 AND 或 OR，不支持继续嵌套。 */
public record MetricConditionGroupRequest(
        @NotBlank(message = "条件组连接符不能为空") String operator,
        @NotEmpty(message = "条件组至少需要一个条件") List<@Valid MetricConditionRequest> conditions) {
}
