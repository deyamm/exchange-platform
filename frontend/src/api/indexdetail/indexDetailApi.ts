import type { IndexConstituents, IndexDetail, IndexMarket } from '/@/types/indexdetail/indexDetailCenter'
import http from '/@/utils/http';
import type { PageResult } from '/@/types/common';


/**
 * 指数详情查询 API。
 *
 * 当前文件提供 mock 实现，便于页面直接预览。
 *
 * 生产接入建议：
 * 1. 后端按 fullSymbol 查询 type_basic_data.index_basic_data。
 * 2. 按 fullSymbol + tradeDate 查询最新 type_exchange_data.index_k_daily。
 * 3. 按 fullSymbol + tradeDate 查询最新 type_exchange_data.index_indicator_daily。
 * 4. 按 indexCode + tradeDate 查询 type_basic_data.index_constituent_data。
 * 5. 用 stockCode 关联 stock_basic_data 补齐 name / industry，并可关联 stock_k_daily 补齐 close / chg_pct。
 * 6. 返回 snake_case 物理字段时，可继续复用 buildIndexDetailFromPhysicalRows 做 camelCase 转换。
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

export function queryIndexConstituents(fullSymbol: string, tradeDate?: string): Promise<IndexConstituents> | any {
    return http.get<IndexConstituents>(
        `/api/v1/analytics/watch-targets/${fullSymbol}/index-constituents`, { 
            params: {
                tradeDate
            } 
        }
    )
}

export function queryIndexConstituentPage(fullSymbol: string, tradeDate?: string, page?: number, pageSize?: number): Promise<PageResult<IndexConstituents>> | any {
    return http.get<PageResult<IndexConstituents>>(
        `/api/v1/analytics/watch-targets/${fullSymbol}/index-constituent-page`, { 
            params: {
                tradeDate,
                page,
                pageSize
            } 
        }
    )
}