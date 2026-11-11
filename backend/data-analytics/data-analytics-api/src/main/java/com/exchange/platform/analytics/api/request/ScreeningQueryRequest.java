package com.exchange.platform.analytics.api.request;

import java.time.LocalDateTime;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.exchange.platform.common.core.enums.AssetType;

/**
 * 条件筛选的完整执行请求。
 *
 * <p>sortMetricCode/sortDirection 只描述结果表列头状态，不是筛选条件的一部分。
 * groupOperator 仅连接第一层条件组，因此模型严格限制为两层。</p>
 */
public record ScreeningQueryRequest(
    @NotNull(message = "标的类型不能为空") AssetType targetType,
    @NotNull(message = "标的范围不能为空") @Valid TargetScopeRequest targetScope,
    LocalDateTime asOfTime,
    @NotEmpty(message = "至少需要一个条件组") List<@Valid MetricConditionGroupRequest> conditionGroups,
    @NotBlank(message = "条件组连接符不能为空") String groupOperator,
    String sortMetricCode,
    String sortDirection
) {}
