import type { IndexDetail } from '/@/types/indexdetail/indexDetailCenter'
import http from '/@/utils/http'

/**
 * 查询指数详情首屏数据。
 *
 * 只返回指数档案、最新行情、估值指标和总览结论；
 * 行情走势与成分股分别由 indexMarketApi / indexConstituentsApi 提供。
 */
export function queryIndexDetail(fullSymbol: string, tradeDate?: string): Promise<IndexDetail> | any {
    return http.get<IndexDetail>(
        `/api/v1/analytics/watch-targets/${fullSymbol}/index-details`, {
            params: {
                tradeDate
            }
        }
    )
}
