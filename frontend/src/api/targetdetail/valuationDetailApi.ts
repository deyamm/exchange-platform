import http from '/@/utils/http'
import type {
    TargetDividendTrendDetail,
    TargetDividendTrendQuery,
    TargetValuationChannelDetail,
    TargetValuationMetricQuery,
    TargetValuationSummaryDetail,
    TargetValuationSummaryQuery,
    TargetValuationTrendDetail
} from '/@/types/targetdetail/valuationDetailType'

/**
 * 估值摘要：顶部 PE/PB/PS/股息率与总市值。
 *
 * GET /api/v1/analytics/watch-targets/{fullSymbol}/valuation-summary
 */
export function queryTargetValuationSummary(
    params: TargetValuationSummaryQuery
): Promise<TargetValuationSummaryDetail> | any {
    const { fullSymbol, tradeDate, historyLimit, rangeCode } = params
    return http.get<TargetValuationSummaryDetail>(
        `/api/v1/analytics/watch-targets/${fullSymbol}/valuation-summary`,
        { params: { tradeDate, historyLimit, rangeCode } }
    )
}

/**
 * 估值通道：收盘价与 5 条估值通道线。
 *
 * GET /api/v1/analytics/watch-targets/{fullSymbol}/valuation-channel
 */
export function queryTargetValuationChannel(
    params: TargetValuationMetricQuery
): Promise<TargetValuationChannelDetail> | any {
    const { fullSymbol, tradeDate, metricCode, historyLimit, rangeCode } = params
    return http.get<TargetValuationChannelDetail>(
        `/api/v1/analytics/watch-targets/${fullSymbol}/valuation-channel`,
        { params: { tradeDate, metricCode, historyLimit, rangeCode } }
    )
}

/**
 * 估值走势：股票估值、行业平均 / 中位数与 30/70 分位。
 *
 * GET /api/v1/analytics/watch-targets/{fullSymbol}/valuation-trend
 */
export function queryTargetValuationTrend(
    params: TargetValuationMetricQuery
): Promise<TargetValuationTrendDetail> | any {
    const { fullSymbol, tradeDate, metricCode, historyLimit, rangeCode } = params
    return http.get<TargetValuationTrendDetail>(
        `/api/v1/analytics/watch-targets/${fullSymbol}/valuation-trend`,
        { params: { tradeDate, metricCode, historyLimit, rangeCode } }
    )
}

/**
 * 分红走势：股票分红 TTM、行业平均 / 中位数分红率。
 *
 * GET /api/v1/analytics/watch-targets/{fullSymbol}/dividend-trend
 */
export function queryTargetDividendTrend(
    params: TargetDividendTrendQuery
): Promise<TargetDividendTrendDetail> | any {
    const { fullSymbol, tradeDate, historyLimit, rangeCode } = params
    return http.get<TargetDividendTrendDetail>(
        `/api/v1/analytics/watch-targets/${fullSymbol}/dividend-trend`,
        { params: { tradeDate, historyLimit, rangeCode } }
    )
}
