import type { IndexMarket } from '/@/types/indexdetail/indexMarketType'
import http from '/@/utils/http'

/**
 * 查询指数行情走势数据。
 *
 * 返回 K 线 / 技术指标图表、技术指标综合分析和区间表现，
 * 由行情走势 Tab 按当前时间范围单独加载。
 */
export function queryIndexMarket(fullSymbol: string, tradeDate?: string, historyLimit?: number): Promise<IndexMarket> | any {
    return http.get<IndexMarket>(
        `/api/v1/analytics/watch-targets/${fullSymbol}/index-market`, {
            params: {
                tradeDate,
                historyLimit
            }
        }
    )
}
