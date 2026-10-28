import http from '/@/utils/http'
import type {
    TargetIndustryDetail,
    TargetIndustryDetailQuery
} from '/@/types/targetdetail/industryDetailType'

/**
 * 行业详情：申万行业归属、所属指数、同行业对比与指数成分。
 *
 * GET /api/v1/analytics/watch-targets/{fullSymbol}/industry-detail
 */
export function queryTargetIndustryDetail(
    params: TargetIndustryDetailQuery
): Promise<TargetIndustryDetail> | any{
    const { fullSymbol, tradeDate } = params
    return http.get<TargetIndustryDetail>(
        `/api/v1/analytics/watch-targets/${fullSymbol}/industry-detail`,
        { params: { tradeDate } }
    )
}
