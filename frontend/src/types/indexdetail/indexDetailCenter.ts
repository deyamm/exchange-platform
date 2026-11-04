import type {
    OverallView,
    RiskLevel,
    ValuationStatus
} from '/@/types/marketoverview/constants'
import type { MetricCard, MarketQuote, IndexProfile } from '/@/types/marketoverview/view'
import type { ResponseMeta } from './indexCommon'
import type { IndexIndicatorDaily } from './indexValuationType'

/**
 * 指数详情首屏数据。
 *
 * 对应 index-details 接口：只包含指数档案、最新行情、估值指标和总览结论。
 * 行情走势、成分股等数据分别由 index-market / index-constituents 接口提供，
 * 见 indexMarketType.ts、indexConstituentsType.ts。
 */
export interface IndexDetail {
    profile: IndexProfile
    quote?: MarketQuote
    indicator?: IndexIndicatorDaily
    overview: IndexOverview
    metricCards: MetricCard[]
    meta: ResponseMeta
}

export interface IndexOverview {
    overallView: OverallView
    overallViewText: string
    valuationStatus?: ValuationStatus
    riskLevel: RiskLevel
    confidence?: number
    summary: string
    reasons: string[]
    risks: string[]
    suggestions: string[]
}
