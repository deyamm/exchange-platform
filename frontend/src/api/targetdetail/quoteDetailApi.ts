import http from '/@/utils/http'
import type {
    TargetCapitalFlowDetail,
    TargetCapitalFlowDetailQuery,
    TargetQuoteDetail,
    TargetQuoteDetailQuery
} from '/@/types/targetdetail/quoteDetailType'

/**
 * 行情详情：K 线、技术指标、技术分析与区间表现。
 *
 * GET /api/v1/analytics/watch-targets/{fullSymbol}/quote-detail
 */
export function queryTargetQuoteDetail(params: TargetQuoteDetailQuery): Promise<TargetQuoteDetail> | any {
    const { fullSymbol, tradeDate, rangeCode, adjustType, benchmarkCode, industryCode } = params
    return http.get<TargetQuoteDetail>(
        `/api/v1/analytics/watch-targets/${fullSymbol}/quote-detail`,
        { params: { tradeDate, rangeCode, adjustType, benchmarkCode, industryCode } }
    )
}

/**
 * 资金流向详情：资金流摘要、历史明细与价格联动。
 *
 * GET /api/v1/analytics/watch-targets/{fullSymbol}/capital-flow-detail
 */
export function queryTargetCapitalFlowDetail(
    params: TargetCapitalFlowDetailQuery
): Promise<TargetCapitalFlowDetail> | any {
    const { fullSymbol, tradeDate, historyLimit, rangeCode, includeQuoteRows } = params
    return http.get<TargetCapitalFlowDetail>(
        `/api/v1/analytics/watch-targets/${fullSymbol}/capital-flow-detail`,
        { params: { tradeDate, historyLimit, rangeCode, includeQuoteRows } }
    )
}
