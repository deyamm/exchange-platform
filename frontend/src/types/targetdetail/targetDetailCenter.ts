import type { OverallView, RiskLevel, TargetType, ValuationStatus } from '/@/types/marketoverview/constants'
import type {
    AlertRule,
    MarketQuote,
    MetricCard,
    RelatedTarget,
    TargetEvent,
    TargetWatchInfo
} from '/@/types/marketoverview/view'
import type { TargetDetailMeta } from './common'
import type {
    CapitalFlowInfo,
    TargetCapitalFlowDetail,
    TargetQuoteDetail
} from './quoteDetailType'
import type {
    TargetDividendTrendDetail,
    TargetValuationChannelDetail,
    TargetValuationSummaryDetail,
    TargetValuationTrendDetail,
    ValuationInfo
} from './valuationDetailType'
import type { FinancialSummary, TargetFinancialDetail } from './financialDetailType'
import type { IndustryInfo, StockProfile, TargetIndustryDetail } from './industryDetailType'

/* -------------------------------------------------------------------------- */
/*                              页面骨架 / 首屏                                */
/*        标的基础信息、综合概览、首屏轻量接口，以及各分接口的响应类型映射        */
/* -------------------------------------------------------------------------- */

/** 标的基础展示信息。 */
export interface TargetProfile {
    /** 完整代码，包含交易所后缀。 */
    fullSymbol: string
    /** 简短代码，不含交易所后缀。 */
    symbol?: string | null
    /** 标的名称。 */
    name: string
    /** 标的类型。 */
    targetType: TargetType
    /** 市场代码。 */
    marketCode?: string | null
    /** 市场类型展示名。 */
    marketTypeName?: string | null
    /** 交易所展示名。 */
    exchangeName?: string | null
    /** 行业展示名。 */
    industryName?: string | null
    /** 交易货币。 */
    currencyType?: string | null
    /** 展示标签。 */
    tags?: string[]
}

/** 综合概览。 */
export interface TargetOverview {
    /** 综合观点。 */
    overallView?: OverallView | null
    /** 综合观点展示文本。 */
    overallViewText?: string | null
    /** 风险等级。 */
    riskLevel?: RiskLevel | null
    /** 估值状态。 */
    valuationStatus?: ValuationStatus | null
    /** 摘要。 */
    summary?: string | null
    /** 支撑理由。 */
    reasons?: string[]
    /** 风险点。 */
    risks?: string[]
    /** 建议。 */
    suggestions?: string[]
    /** 置信度，0-1。 */
    confidence?: number | null
}

/** 标的详情页 Tab 编码。 */
export type TargetDetailTabCode = 'QUOTE' | 'VALUATION' | 'FINANCIAL' | 'CAPITAL_FLOW' | 'INDUSTRY'

/** 首屏 / 综合分析接口通用查询参数。 */
export interface TargetDetailBaseQuery {
    /** 完整代码，包含交易所后缀。 */
    fullSymbol: string
    /** 查询截止交易日；不传时由后端解析最近可用交易日。 */
    tradeDate?: string | null
}

/**
 * 首屏轻量接口返回。
 *
 * 对应页面顶部：股票基本信息、最新行情、指标卡片，以及可用 Tab 等轻量数据。
 * 不返回 valuationRows / financialDetail / moneyFlowRows 等大数组。
 */
export interface TargetDetailShell {
    targetProfile: TargetProfile
    /** 最新行情。 */
    quote?: MarketQuote
    /** 顶部指标卡。 */
    metricCards?: MetricCard[]
    /** 估值摘要。 */
    valuation?: ValuationInfo | null
    /** 财务摘要。 */
    financial?: FinancialSummary | null
    /** 资金流摘要。 */
    capitalFlow?: CapitalFlowInfo | null
    /** 行业摘要，包含成分摘要。 */
    industry?: IndustryInfo | null
    /** 关注信息。 */
    watch?: TargetWatchInfo | null
    /** 提醒规则。 */
    alerts?: AlertRule[]
    /** 事件列表。 */
    events?: TargetEvent[]
    /** 相关标的。 */
    relatedTargets?: RelatedTarget[]
    /** 股票基础信息，来自 stock_basic_data。 */
    stockProfile?: StockProfile
    /** 当前标的可用的详情 Tab。 */
    availableTabs?: TargetDetailTabCode[]
    /** 数据元信息。 */
    meta: TargetDetailMeta
}

/** 综合分析接口返回。 */
export interface TargetOverviewDetail {
    fullSymbol: string
    /** 综合概览。 */
    overview?: TargetOverview
    /** 数据元信息。 */
    meta: TargetDetailMeta
}

/**
 * 标的详情页接口响应类型映射。
 *
 * 每个字段对应一个独立接口；页面按 Tab 组合，不再依赖一次性返回的聚合类型。
 */
export interface TargetDetailApiResponseMap {
    shell: TargetDetailShell
    overview: TargetOverviewDetail
    quote: TargetQuoteDetail
    valuationSummary: TargetValuationSummaryDetail
    valuationChannel: TargetValuationChannelDetail
    valuationTrend: TargetValuationTrendDetail
    dividendTrend: TargetDividendTrendDetail
    financial: TargetFinancialDetail
    capitalFlow: TargetCapitalFlowDetail
    industry: TargetIndustryDetail
}
