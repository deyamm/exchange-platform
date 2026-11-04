import type { MetricTrend, RangeCode } from '/@/types/marketoverview/constants'
import type { ChartGroup } from '/@/types/marketoverview/view'

/* -------------------------------------------------------------------------- */
/*                                   行情类                                    */
/*                       行情图表、技术指标分析与区间表现                      */
/* -------------------------------------------------------------------------- */

/**
 * 行情走势 Tab 接口返回。
 *
 * 对应 index-market 接口：K 线 / 技术指标图表、技术指标综合分析和区间表现。
 */
export interface IndexMarket {
    marketCharts: ChartGroup[]
    quoteAnalysis?: QuoteTechnicalAnalysis
    /** 行情走势页区间表现，可由后端按当前区间返回；前端也可基于 K 线数据兜底计算 */
    rangePerformance?: IndexRangePerformance
}

export type TechnicalLevelTone = 'up' | 'down' | 'flat' | 'risk'

export interface IndexTechnicalLevel {
    /** 趋势 / 动能 / 量能 / 波动 / 风险 */
    key: string
    label: string
    /** 1-5 级，用于页面展示五级图标 */
    level: number
    tone: TechnicalLevelTone
    text: string
}

export interface IndexTechnicalConclusion {
    /** 例如：强势上涨、震荡偏强、中性震荡、超买警戒、趋势反转观察 */
    label: string
    summary: string
    /** 0-100 综合评分 */
    score: number
    signalStrength: string
    period: string
    updatedAt: string
    levels: IndexTechnicalLevel[]
}

export interface IndexTechnicalDimensionScore {
    /** 趋势 / 动能 / 量能 / 波动 / 价格位置 / 信号一致性 */
    dimension: string
    /** 0-100 分项评分 */
    score: number
    explanation: string
}

export interface IndexTechnicalIndicatorSignal {
    /** MA / EMA / BOLL / SAR / VWAP / SuperTrend / VOL / MACD / KDJ / RSI / CCI / DMI / ATR / OBV */
    indicator: string
    status: string
    /** 偏多 / 偏空 / 中性 / 谨慎 / 支持上涨 / 风险增加 等展示文案 */
    direction: string
    explanation: string
}

export interface IndexSignalConflict {
    bullishCount: number
    bearishCount: number
    neutralCount: number
    consistency: string
    supports: string[]
    risks: string[]
}

export interface QuoteTechnicalAnalysis {
    conclusion: IndexTechnicalConclusion
    dimensionScores: IndexTechnicalDimensionScore[]
    indicatorSignals: IndexTechnicalIndicatorSignal[]
    signalConflict: IndexSignalConflict
    explanation: string
}

export interface IndexRangeRiskReturn {
    highPrice?: number
    lowPrice?: number
    periodReturn?: number
    annualizedReturn?: number
    amplitude?: number
    volatility?: number
    maxDrawdown?: number
    sharpeRatio?: number
    calmarRatio?: number
}

export interface IndexRangeTrading {
    periodTurnover?: number
    averageTurnover?: number
    periodTurnoverRate?: number
    volumeExpansionDays?: number
    maxVolumeDate?: string
    turnoverChangeVsPrevious?: number
}

export interface IndexRangeStructure {
    currentPricePercentile?: number
    newHighCount?: number
    newLowCount?: number
    priceConcentrationRange?: string
    turnoverConcentrationRange?: string
}

export interface IndexRangePerformanceSection {
    title: string
    items: Array<{
        label: string
        value: string | number
        unit?: string
        trend?: MetricTrend
    }>
}

export interface IndexRangePerformance {
    rangeCode?: RangeCode
    startDate?: string
    endDate?: string
    riskReturn: IndexRangeRiskReturn
    trading: IndexRangeTrading
    structure: IndexRangeStructure
    /** 后端若希望完全控制展示，也可以直接返回分组明细 */
    sections?: IndexRangePerformanceSection[]
}
