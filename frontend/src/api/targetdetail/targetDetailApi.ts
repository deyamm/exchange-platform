import http from '/@/utils/http'
import type {
    TargetDetailBaseQuery,
    TargetDetailShell,
    TargetOverviewDetail
} from '/@/types/targetdetail/targetDetailCenter'

/**
 * 标的详情首屏轻量数据。
 *
 * GET /api/v1/analytics/watch-targets/{fullSymbol}/details
 *
 * 仅包含顶部展示所需的标的基础信息、最新行情、指标卡和可用 Tab。
 */
export function queryTargetDetailShell(params: TargetDetailBaseQuery): Promise<TargetDetailShell> | any{
    const { fullSymbol, tradeDate } = params
    return http.get<TargetDetailShell>(
        `/api/v1/analytics/watch-targets/${fullSymbol}/details`,
        { params: { tradeDate } }
    )
}

/**
 * 标的综合概览。
 *
 * GET /api/v1/analytics/watch-targets/{fullSymbol}/overview
 */
export function queryTargetOverviewDetail(params: TargetDetailBaseQuery): Promise<TargetOverviewDetail> | any{
    const { fullSymbol, tradeDate } = params
    return http.get<TargetOverviewDetail>(
        `/api/v1/analytics/watch-targets/${fullSymbol}/overview`,
        { params: { tradeDate } }
    )
}
