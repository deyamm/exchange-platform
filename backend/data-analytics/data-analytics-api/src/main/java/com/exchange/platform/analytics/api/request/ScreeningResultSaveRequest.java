package com.exchange.platform.analytics.api.request;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.exchange.platform.analytics.api.response.ScreeningResultView;

/**
 * 保存一次已经执行完成的筛选快照。
 *
 * <p>result 是本次执行时已经返回给用户的矩阵摘要，而不是仅保存条件后在回看时重新取“最新”值。
 * 这样历史结果的 asOfTime、命中列表和指标值均保持可复现。</p>
 */
public record ScreeningResultSaveRequest(
        @NotNull(message = "筛选请求不能为空") @Valid ScreeningQueryRequest request,
        @NotNull(message = "筛选结果不能为空") @Valid ScreeningResultView result) {
}
