
/**
 * 指数详情查询参数。
 *
 * fullSymbol 对齐：
 * - type_basic_data.index_basic_data.full_symbol
 * - type_exchange_data.index_k_daily.full_symbol
 * - type_exchange_data.index_indicator_daily.full_symbol
 */
export interface IndexDetailQuery {
    fullSymbol: string
    tradeDate?: string
    includeHistory?: boolean
    includeIndicator?: boolean
    includeConstituents?: boolean
}
