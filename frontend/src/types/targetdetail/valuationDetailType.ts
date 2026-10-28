import type { ValuationMetricCode, ValuationTimeRangeCode } from '/@/types/marketoverview/constants'
import type { TargetDetailMeta } from './common'

/* -------------------------------------------------------------------------- */
/*                                   估值类                                    */
/*                估值摘要、估值通道、估值走势、分红走势、估值明细               */
/* -------------------------------------------------------------------------- */

/** 估值摘要。 */
export interface ValuationInfo {
    /** 市盈率。 */
    pe?: number | null
    /** 市盈率 TTM。 */
    peTtm?: number | null
    /** 市净率。 */
    pb?: number | null
    /** 市销率。 */
    ps?: number | null
    /** 市销率 TTM。 */
    psTtm?: number | null
    /** 股息率 TTM。 */
    dvTtm?: number | null
    /** 总市值。 */
    totalMarketValue?: number | null
}

/**
 * 估值指标摘要。
 *
 * 用于估值走势图周围展示当前股票估值、所选时间范围内分位值、
 * 30 分位与 70 分位等摘要信息。
 */
export interface ValuationMetricSummary {
    /** 指标编码。 */
    metricCode: ValuationMetricCode
    /** 指标名称。 */
    metricName?: string | null
    /** 当前股票估值。 */
    currentValue?: number | null
    /** 所选时间范围内的当前分位，单位为百分比。 */
    percentileValue?: number | null
    /** 所选时间范围内 30 分位估值。 */
    percentile30Value?: number | null
    /** 所选时间范围内 70 分位估值。 */
    percentile70Value?: number | null
    /** 行业平均估值。 */
    industryAverageValue?: number | null
    /** 行业中位数估值。 */
    industryMedianValue?: number | null
    /** 最新交易日期。 */
    latestTradeDate?: string | null
    /** 补充说明。 */
    description?: string | null
}

/**
 * 估值通道明细行。
 *
 * 页面只负责展示后端已经计算好的通道线，不在页面中说明通道价格计算方法。
 * close 表示股票收盘价；channelPrice1-5 表示 5 条估值通道线对应的股票价格；
 * channelMultiple1-5 表示每条通道线对应的估值倍数，图例展示为 11x 这类格式。
 */
export interface ValuationChannelRow {
    /** 股票或指数代码全称。 */
    fullSymbol: string
    /** 交易日期。 */
    tradeDate: string
    /** 估值指标编码。 */
    metricCode: ValuationMetricCode
    /** 收盘价曲线。 */
    close?: number | null
    /** 第 1 条估值通道倍数。 */
    channelMultiple1?: number | null
    /** 第 1 条估值通道价格。 */
    channelPrice1?: number | null
    /** 第 2 条估值通道倍数。 */
    channelMultiple2?: number | null
    /** 第 2 条估值通道价格。 */
    channelPrice2?: number | null
    /** 第 3 条估值通道倍数。 */
    channelMultiple3?: number | null
    /** 第 3 条估值通道价格。 */
    channelPrice3?: number | null
    /** 第 4 条估值通道倍数。 */
    channelMultiple4?: number | null
    /** 第 4 条估值通道价格。 */
    channelPrice4?: number | null
    /** 第 5 条估值通道倍数。 */
    channelMultiple5?: number | null
    /** 第 5 条估值通道价格。 */
    channelPrice5?: number | null
}

/**
 * 估值走势明细行。
 *
 * 用于展示股票自身估值、行业平均估值、行业中位数估值，
 * 以及所选时间范围内 30 分位、70 分位两条虚线。
 */
export interface ValuationTrendRow {
    /** 股票或指数代码全称。 */
    fullSymbol: string
    /** 交易日期。 */
    tradeDate: string
    /** 估值指标编码。 */
    metricCode: ValuationMetricCode
    /** 股票自身收盘估值，不展示股票价格。 */
    stockValue?: number | null
    /** 所在行业平均估值。 */
    industryAverageValue?: number | null
    /** 所在行业中位数估值。 */
    industryMedianValue?: number | null
    /** 所选时间范围内 30 分位估值。 */
    percentile30Value?: number | null
    /** 所选时间范围内 70 分位估值。 */
    percentile70Value?: number | null
    /** 当前估值在所选时间范围内的分位，单位为百分比。 */
    percentileValue?: number | null
}

/**
 * 分红走势明细行。
 *
 * 用于展示股票分红 TTM、行业平均分红率、行业中位数分红率。
 */
export interface DividendTrendRow {
    /** 股票或指数代码全称。 */
    fullSymbol: string
    /** 交易日期。 */
    tradeDate: string
    /** 股票分红 TTM，单位为百分比。 */
    dvTtm?: number | null
    /** 所在行业平均分红率，单位为百分比。 */
    industryAverageDvTtm?: number | null
    /** 所在行业中位数分红率，单位为百分比。 */
    industryMedianDvTtm?: number | null
}

/**
 * 估值详情页扩展数据。
 *
 * valuationRows 继续承接 stock_indicator_daily / index_indicator_daily 的物理表字段；
 * 本结构承接估值详情页新增的估值通道、行业估值走势和分红走势等衍生结果。
 */
export interface ValuationDetailData {
    /** 估值走势图周围的指标摘要。 */
    metricSummaries?: ValuationMetricSummary[]
    /** 估值通道图明细。 */
    channelRows?: ValuationChannelRow[]
    /** 估值走势明细。 */
    trendRows?: ValuationTrendRow[]
    /** 分红走势明细。 */
    dividendRows?: DividendTrendRow[]
}

/**
 * 股票每日估值指标行。
 *
 * 字段来源：
 * - 股票：type_exchange_data.stock_indicator_daily
 */
export interface ValuationDailyRow {
    /** 股票或指数代码全称。 */
    fullSymbol: string
    /** 交易日期。 */
    tradeDate: string
    /** 当日收盘价。 */
    close?: number | null
    /** 总市值，单位为元。 */
    totalMarketValue?: number | null
    /** 流通市值，单位为元。 */
    floatMarketValue?: number | null
    /** 总股本，单位为股。 */
    totalShare?: number | null
    /** 流通股本，单位为股。 */
    floatShare?: number | null
    /** 自由流通股本，单位为股。 */
    freeShare?: number | null
    /** 换手率。 */
    turnoverRate?: number | null
    /** 基于自由流通股本的换手率。 */
    turnoverRateFree?: number | null
    /** 市盈率。 */
    pe?: number | null
    /** 市盈率 TTM。 */
    peTtm?: number | null
    /** 市净率。 */
    pb?: number | null
    /** 市销率，仅股票指标表提供。 */
    ps?: number | null
    /** 市销率 TTM，仅股票指标表提供。 */
    psTtm?: number | null
    /** 股息率，仅股票指标表提供。 */
    dvRatio?: number | null
    /** 股息率 TTM，仅股票指标表提供。 */
    dvTtm?: number | null
    /** PE TTM 行业平均值，估值详情页衍生字段。 */
    industryAveragePeTtm?: number | null
    /** PE TTM 行业中位数，估值详情页衍生字段。 */
    industryMedianPeTtm?: number | null
    /** PB 行业平均值，估值详情页衍生字段。 */
    industryAveragePb?: number | null
    /** PB 行业中位数，估值详情页衍生字段。 */
    industryMedianPb?: number | null
    /** PS TTM 行业平均值，估值详情页衍生字段。 */
    industryAveragePsTtm?: number | null
    /** PS TTM 行业中位数，估值详情页衍生字段。 */
    industryMedianPsTtm?: number | null
    /** 股息率 TTM 行业平均值，估值详情页衍生字段。 */
    industryAverageDvTtm?: number | null
    /** 股息率 TTM 行业中位数，估值详情页衍生字段。 */
    industryMedianDvTtm?: number | null
}

/** 估值摘要接口查询参数。 */
export interface TargetValuationSummaryQuery {
    /** 完整代码，包含交易所后缀。 */
    fullSymbol: string
    /** 查询截止交易日；不传时由后端解析最近可用交易日。 */
    tradeDate?: string | null
    /** 历史数据条数；与 rangeCode 二选一。 */
    historyLimit?: number | null
    /** 时间范围编码，例如 1Y、3Y、5Y、10Y。 */
    rangeCode?: ValuationTimeRangeCode
}

/** 估值指标类接口查询参数。 */
export interface TargetValuationMetricQuery extends TargetValuationSummaryQuery {
    /** 估值指标编码。 */
    metricCode: ValuationMetricCode
}

/** 估值摘要接口返回。 */
export interface TargetValuationSummaryDetail {
    fullSymbol: string
    /** 估值摘要。 */
    valuation?: ValuationInfo | null
    /** 估值详情明细，来自 indicator_daily。 */
    valuationRows?: ValuationDailyRow[]
    /** 数据元信息。 */
    meta: TargetDetailMeta
}

/** 估值通道接口返回。 */
export interface TargetValuationChannelDetail {
    fullSymbol: string
    /** 估值指标编码。 */
    metricCode: ValuationMetricCode
    /** 估值通道图明细。 */
    channelRows?: ValuationChannelRow[]
    /** 数据元信息。 */
    meta: TargetDetailMeta
}

/** 估值趋势接口返回。 */
export interface TargetValuationTrendDetail {
    fullSymbol: string
    /** 估值指标编码。 */
    metricCode: ValuationMetricCode
    /** 估值走势图周围的指标摘要。 */
    metricSummaries?: ValuationMetricSummary[]
    /** 估值走势明细。 */
    trendRows?: ValuationTrendRow[]
    /** 数据元信息。 */
    meta: TargetDetailMeta
}

/** 分红走势接口查询参数。 */
export interface TargetDividendTrendQuery {
    /** 完整代码，包含交易所后缀。 */
    fullSymbol: string
    /** 查询截止交易日；不传时由后端解析最近可用交易日。 */
    tradeDate?: string | null
    /** 历史数据条数；与 rangeCode 二选一。 */
    historyLimit?: number | null
    /** 时间范围编码，例如 1Y、3Y、5Y、10Y。 */
    rangeCode?: ValuationTimeRangeCode
}

/** 分红走势接口返回。 */
export interface TargetDividendTrendDetail {
    fullSymbol?: string
    /** 分红走势明细。 */
    dividendRows?: DividendTrendRow[]
    /** 数据元信息。 */
    meta: TargetDetailMeta
}
