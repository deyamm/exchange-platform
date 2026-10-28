import type { RangeCode } from '/@/types/marketoverview/constants'
import type { ChartGroup, MarketQuote } from '/@/types/marketoverview/view'
import type { TargetDetailMeta } from './common'

/* -------------------------------------------------------------------------- */
/*                                   行情类                                    */
/*                         行情走势、技术分析、区间表现                          */
/* -------------------------------------------------------------------------- */

/** 复权类型。 */
export type AdjustType = 'NONE' | 'FORWARD' | 'BACKWARD'

export const adjustTypeOptions: Array<{ label: string; value: AdjustType }> = [
    { label: '除权', value: 'NONE' },
    { label: '前复权', value: 'FORWARD' },
    { label: '后复权', value: 'BACKWARD' }
]

export type TechnicalLevelTone = 'up' | 'down' | 'flat' | 'risk'

export interface QuoteTechnicalLevel {
    /** 趋势 / 动能 / 量能 / 波动 / 风险 */
    key: string
    label: string
    /** 1-5 级，用于页面展示五级图标 */
    level: number
    tone: TechnicalLevelTone
    text: string
}

export interface QuoteTechnicalConclusion {
    /** 例如：强势上涨、震荡偏强、中性震荡、超买警戒、趋势反转观察 */
    label: string
    summary: string
    /** 0-100 综合评分 */
    score: number
    signalStrength: string
    period: string
    updatedAt: string
    levels: QuoteTechnicalLevel[]
}

export interface QuoteTechnicalDimensionScore {
    /** 趋势 / 动能 / 量能 / 波动 / 价格位置 / 信号一致性 */
    dimension: string
    /** 0-100 分项评分 */
    score: number
    explanation: string
}

export interface QuoteTechnicalSignal {
    /** MA / EMA / BOLL / SAR / VWAP / SuperTrend / VOL / MACD / KDJ / RSI / CCI / DMI / ATR / OBV */
    indicator: string
    status: string
    /** 偏多 / 偏空 / 中性 / 谨慎 / 支持上涨 / 风险增加 等展示文案 */
    direction: string
    explanation: string
}

export interface QuoteSignalConflict {
    bullishCount: number
    bearishCount: number
    neutralCount: number
    consistency: string
    supports: string[]
    risks: string[]
}

/** 技术指标综合分析结果。 */
export interface QuoteTechnicalAnalysis {
    /** 综合结论。 */
    conclusion: QuoteTechnicalConclusion
    /** 多维评分。 */
    dimensionScores: QuoteTechnicalDimensionScore[]
    /** 指标信号明细。 */
    indicatorSignals: QuoteTechnicalSignal[]
    /** 信号冲突与风险提示。 */
    signalConflict: QuoteSignalConflict
    /** 说明。 */
    explanation?: string
}

/** 区间对比分析数据；当前物理表无法直接提供时允许为空，由页面显示为“-”。 */
export interface QuoteRangeComparison {
    /** 相对大盘收益，正数表示跑赢，单位为百分点。 */
    marketExcessReturn?: number | null
    /** 相对行业收益，正数表示跑赢，单位为百分点。 */
    industryExcessReturn?: number | null
    /** 行业排名文本，例如 12/86。 */
    industryRankText?: string | null
    /** 市场排名文本，例如 536/5200。 */
    marketRankText?: string | null
    /** 区间 Alpha。 */
    alpha?: number | null
    /** 区间 Beta。 */
    beta?: number | null
    /** 与对比基准相关性。 */
    correlation?: number | null
    /** 相对强弱评分，0-100。 */
    relativeStrengthScore?: number | null
}

/** 通用 K 线周期。 */
export type TargetQuotePeriod = 'DAILY' | 'WEEKLY' | 'MONTHLY'

/** 行情 Tab 接口查询参数。 */
export interface TargetQuoteDetailQuery {
    /** 完整代码，包含交易所后缀。 */
    fullSymbol: string
    /** 查询截止交易日；不传时由后端解析最近可用交易日。 */
    tradeDate?: string | null
    /** K 线与区间统计使用的时间范围。 */
    rangeCode?: RangeCode
    /** 复权类型；K 线和技术指标使用相同复权口径。 */
    adjustType: AdjustType
    /** 可选：对比基准代码，例如沪深300。 */
    benchmarkCode?: string | null
    /** 可选：行业代码，用于行业超额收益和行业排名。 */
    industryCode?: string | null
}

/** 区间收益风险表现。 */
export interface RangeReturnRiskPerformance {
    /** 区间收益率。 */
    changePercent?: number | null
    /** 最大回撤。 */
    maxDrawdown?: number | null
    /** 波动率。 */
    volatility?: number | null
    /** 排名文本。 */
    rankingText?: string | null
    /** 区间最高价。 */
    high?: number | null
    /** 区间最低价。 */
    low?: number | null
    /** 区间振幅，单位为百分比。 */
    amplitude?: number | null
    /** 年化收益率，单位为百分比。 */
    annualizedReturn?: number | null
    /** 夏普比率。 */
    sharpeRatio?: number | null
    /** 卡玛比率。 */
    calmarRatio?: number | null
}

/** 区间成交表现。 */
export interface RangeVolumePerformance {
    /** 区间成交量。 */
    volume?: number | null
    /** 区间成交额。 */
    turnover?: number | null
    /** 区间换手率。 */
    turnoverRate?: number | null
    /** 日均成交量。 */
    avgVolume?: number | null
    /** 日均成交额。 */
    avgTurnover?: number | null
    /** 平均换手率。 */
    avgTurnoverRate?: number | null
    /** 最大成交量。 */
    maxVolume?: number | null
    /** 最大成交量日期。 */
    maxVolumeTradeDate?: string | null
    /** 最大成交额。 */
    maxTurnover?: number | null
    /** 最大成交额日期。 */
    maxTurnoverTradeDate?: string | null
}

/** 区间价格位置与区间结构表现。 */
export interface RangePriceStructurePerformance {
    /** 区间开盘价。 */
    open?: number | null
    /** 区间最高价。 */
    high?: number | null
    /** 区间最低价。 */
    low?: number | null
    /** 区间收盘价或最新价。 */
    close?: number | null
    /** 区间起点昨收价。 */
    preClose?: number | null
    /** 当前价格在区间内的位置，单位为百分比。 */
    pricePositionPercent?: number | null
    /** 距区间最高价回撤，单位为百分比。 */
    drawdownFromHigh?: number | null
    /** 距区间最低价涨幅，单位为百分比。 */
    reboundFromLow?: number | null
    /** 区间新高次数。 */
    newHighCount?: number | null
    /** 区间新低次数。 */
    newLowCount?: number | null
    /** 价格集中区间展示文本。 */
    priceRangeText?: string | null
    /** 成交密集区间展示文本。 */
    turnoverRangeText?: string | null
}

/** 行情 Tab 区间表现聚合数据。 */
export interface RangePerformance {
    rangeCode?: RangeCode
    startDate?: string
    endDate?: string
    /** 收益风险。 */
    returnRisk?: RangeReturnRiskPerformance
    /** 成交。 */
    volume?: RangeVolumePerformance
    /** 对比分析。 */
    comparison?: QuoteRangeComparison
    /** 价格位置与区间结构。 */
    priceStructure?: RangePriceStructurePerformance
}

/** 行情 Tab 接口返回。 */
export interface TargetQuoteDetail {
    fullSymbol?: string
    /** 当前区间末端行情快照，用于组件定位代码和交易日。 */
    quote?: MarketQuote
    /** K 线与技术指标图表数据；技术指标由后端计算。 */
    charts?: ChartGroup[]
    /** 技术指标综合分析。 */
    quoteTechnicalAnalysis?: QuoteTechnicalAnalysis
    /** 当前区间的收益风险、成交、对比和价格结构统计。 */
    rangePerformance?: RangePerformance
    /** 当前返回价格使用的复权类型。 */
    adjustType: AdjustType
    /** 数据元信息。 */
    meta: TargetDetailMeta
}

/* -------------------------------------------------------------------------- */
/*                        行情类：资金流向（资金 Tab）                          */
/* -------------------------------------------------------------------------- */

/** 资金流摘要。 */
export interface CapitalFlowInfo {
    /** 交易日期。 */
    tradeDate?: string | null
    /** 净流入额。 */
    netMfAmount?: number | null
    /** 特大单净流入额。 */
    elgNetMfAmount?: number | null
    /** 大单净流入额。 */
    lgNetMfAmount?: number | null
    /** 中单净流入额。 */
    mdNetMfAmount?: number | null
    /** 小单净流入额。 */
    smNetMfAmount?: number | null
    /** 结论。 */
    conclusion?: string | null
}

/**
 * 股票资金流向明细行。
 *
 * 字段来源：type_exchange_data.stock_moneyflow_daily。
 */
export interface StockMoneyflowDailyRow {
    /** 股票代码全称。 */
    fullSymbol: string
    /** 交易日期。 */
    tradeDate: string
    /** 小单买入量，单位为股。 */
    buySmVol?: number | null
    /** 小单买入金额，单位为元。 */
    buySmAmount?: number | null
    /** 小单卖出量，单位为股。 */
    sellSmVol?: number | null
    /** 小单卖出金额，单位为元。 */
    sellSmAmount?: number | null
    /** 中单买入量，单位为股。 */
    buyMdVol?: number | null
    /** 中单买入金额，单位为元。 */
    buyMdAmount?: number | null
    /** 中单卖出量，单位为股。 */
    sellMdVol?: number | null
    /** 中单卖出金额，单位为元。 */
    sellMdAmount?: number | null
    /** 大单买入量，单位为股。 */
    buyLgVol?: number | null
    /** 大单买入金额，单位为元。 */
    buyLgAmount?: number | null
    /** 大单卖出量，单位为股。 */
    sellLgVol?: number | null
    /** 大单卖出金额，单位为元。 */
    sellLgAmount?: number | null
    /** 特大单买入量，单位为股。 */
    buyElgVol?: number | null
    /** 特大单买入金额，单位为元。 */
    buyElgAmount?: number | null
    /** 特大单卖出量，单位为股。 */
    sellElgVol?: number | null
    /** 特大单卖出金额，单位为元。 */
    sellElgAmount?: number | null
    /** 净流入量，单位为股。 */
    netMfVol?: number | null
    /** 净流入额，单位为元。 */
    netMfAmount?: number | null
}

/** 资金 Tab 接口查询参数。 */
export interface TargetCapitalFlowDetailQuery {
    /** 完整代码，包含交易所后缀。 */
    fullSymbol: string
    /** 查询截止交易日；不传时由后端解析最近可用交易日。 */
    tradeDate?: string | null
    /** 历史数据条数；与 rangeCode 二选一，同时传入时以 historyLimit 为准。 */
    historyLimit?: number | null
    /** 时间范围编码；不传 historyLimit 时由后端按该区间换算条数。 */
    rangeCode?: RangeCode
    /** 是否同时返回价格联动所需行情。 */
    includeQuoteRows?: boolean
}

/** 资金 Tab 接口返回。 */
export interface TargetCapitalFlowDetail {
    fullSymbol?: string
    /** 资金流摘要。 */
    capitalFlow?: CapitalFlowInfo | null
    /** 股票资金流明细，来自 stock_moneyflow_daily。 */
    moneyFlowRows?: StockMoneyflowDailyRow[]
    /** 资金与价格联动图所需行情。 */
    close?: Array<number | null> | null
    /** 数据元信息。 */
    meta: TargetDetailMeta
}
