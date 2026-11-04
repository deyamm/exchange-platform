import type { IndexConstituents } from '/@/types/indexdetail/indexConstituentsType'
import type { IndexConstituentDetail } from '/@/types/marketoverview/view'
import http from '/@/utils/http'
import type { PageResult } from '/@/types/common'

/**
 * 查询指数成分股统计数据。
 *
 * 返回成分股统计、行业权重、涨跌幅分布与图表，
 * 后端同时会把全量成分股列表缓存到 Redis，供分页接口读取。
 */
export function queryIndexConstituents(fullSymbol: string, tradeDate?: string): Promise<IndexConstituents> | any {
    return http.get<IndexConstituents>(
        `/api/v1/analytics/watch-targets/${fullSymbol}/index-constituents`, {
            params: {
                tradeDate
            }
        }
    )
}

/**
 * 分页查询指数成分股列表。
 *
 * 数据来自 index-constituents 接口缓存在 Redis 中的全量成分股。
 */
export function queryIndexConstituentPage(fullSymbol: string, tradeDate?: string, page?: number, pageSize?: number): Promise<PageResult<IndexConstituentDetail>> | any {
    return http.get<PageResult<IndexConstituentDetail>>(
        `/api/v1/analytics/watch-targets/${fullSymbol}/index-constituent-page`, {
            params: {
                tradeDate,
                page,
                pageSize
            }
        }
    )
}
