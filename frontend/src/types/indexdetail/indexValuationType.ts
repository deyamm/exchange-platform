import type { ValuationStatus } from '/@/types/marketoverview/constants'

/* -------------------------------------------------------------------------- */
/*                                  估值与规模                                 */
/* -------------------------------------------------------------------------- */

/**
 * 指数估值与规模指标。
 *
 * 来源表：type_exchange_data.index_indicator_daily
 */
export interface IndexIndicatorDaily {
    /** type_exchange_data.index_indicator_daily.full_symbol */
    fullSymbol: string

    /** type_exchange_data.index_indicator_daily.trade_date */
    tradeDate: string

    /** type_exchange_data.index_indicator_daily.total_market_value：总市值，单位元 */
    totalMarketValue?: number

    /** type_exchange_data.index_indicator_daily.float_market_value：流通市值，单位元 */
    floatMarketValue?: number

    /** type_exchange_data.index_indicator_daily.total_share：总股本，单位股 */
    totalShare?: number

    /** type_exchange_data.index_indicator_daily.float_share：流通股本，单位股 */
    floatShare?: number

    /** type_exchange_data.index_indicator_daily.free_share：自由流通股本，单位股 */
    freeShare?: number

    /** type_exchange_data.index_indicator_daily.turnover_rate：换手率 */
    turnoverRate?: number

    /** type_exchange_data.index_indicator_daily.turnover_rate_free：自由流通换手率 */
    turnoverRateFree?: number

    /** type_exchange_data.index_indicator_daily.pe：市盈率 */
    pe?: number

    /** type_exchange_data.index_indicator_daily.pe_ttm：市盈率 TTM */
    peTtm?: number

    /** type_exchange_data.index_indicator_daily.pb：市净率 */
    pb?: number

    /** 页面层估值状态，可由 peTtm / pb 分位或业务规则计算 */
    valuationStatus?: ValuationStatus

    /** 页面层估值结论，非物理表字段 */
    conclusion?: string
}
