import http from '/@/utils/http'
import type {
    TargetFinancialDetail,
    TargetFinancialDetailQuery
} from '/@/types/targetdetail/financialDetailType'

/**
 * 财务详情：财务摘要、三大报表与财务指标明细。
 *
 * GET /api/v1/analytics/watch-targets/{fullSymbol}/financial-detail
 */
export function queryTargetFinancialDetail(
    params: TargetFinancialDetailQuery
): Promise<TargetFinancialDetail> | any {
    const { fullSymbol, historyLimit } = params
    return http.get<TargetFinancialDetail>(
        `/api/v1/analytics/watch-targets/${fullSymbol}/financial-detail`,
        { params: { historyLimit } }
    )
}
