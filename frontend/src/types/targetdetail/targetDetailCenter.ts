export type TargetType =
    | 'STOCK'
    | 'INDEX'
    | 'ETF'
    | 'FUND'
    | 'SECTOR'
    | 'BOND'
    | 'FUTURE'
    | 'FOREX'
    | 'CRYPTO'

export type MarketStatus =
    | 'PRE_MARKET'
    | 'TRADING'
    | 'MIDDAY_BREAK'
    | 'CLOSED'
    | 'SUSPENDED'
    | 'UNKNOWN'

export type OverallView =
    | 'STRONGLY_BULLISH'
    | 'BULLISH'
    | 'NEUTRAL'
    | 'BEARISH'
    | 'STRONGLY_BEARISH'

export type TrendStatus = 'UPTREND' | 'DOWNTREND' | 'SIDEWAYS' | 'UNKNOWN'
export type ValuationStatus = 'UNDERVALUED' | 'FAIR' | 'OVERVALUED' | 'UNKNOWN'
export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'EXTREME'
export type SignalStrength = 'WEAK' | 'MEDIUM' | 'STRONG'

export type MetricTrend = 'UP' | 'DOWN' | 'FLAT' | 'UNKNOWN'
export type MetricStatus = 'GOOD' | 'NORMAL' | 'WARNING' | 'DANGER'

export interface TargetDetailQuery {
    targetCode: string
    targetType?: TargetType | ''
    marketCode?: string
    date?: string
    includeCharts?: boolean
    includeFinancial?: boolean
    includeNews?: boolean
    includeHistory?: boolean
}

export interface TargetChartQuery {
    targetCode: string
    chartType?: 'INTRADAY' | 'KLINE' | 'VALUATION' | 'CAPITAL_FLOW'
    period?: '1D' | '5D' | '1M' | '3M' | '6M' | '1Y' | '3Y' | '5Y'
    frequency?: 'MINUTE' | 'DAILY' | 'WEEKLY' | 'MONTHLY'
}

export interface AnalysisHistoryQuery {
    targetCode: string
    sceneCode?: string
    conclusion?: OverallView | ''
    riskLevel?: RiskLevel | ''
    startDate?: string
    endDate?: string
    pageNo: number
    pageSize: number
}

export interface PageResult<T> {
    records: T[]
    total: number
    pageNo: number
    pageSize: number
}

export interface TargetDetailDTO {
    target: TargetProfile
    quote?: MarketQuote
    overview: TargetOverview
    metricCards: MetricCard[]
    charts: ChartGroup[]
    performance?: TargetPerformanceInterval[]
    valuation?: ValuationInfo
    financial?: FinancialSummary
    capitalFlow?: CapitalFlowInfo
    industry?: IndustryInfo
    constituents?: ConstituentInfo
    fundInfo?: FundInfo
    watch?: TargetWatchInfo
    events: TargetEvent[]
    news: TargetNews[]
    alerts: AlertRule[]
    analysisHistory: AnalysisRecord[]
    relatedTargets: RelatedTarget[]
    features?: TargetFeatureFlags
    permissions?: TargetPermissions
    meta: ResponseMeta
}

export interface TargetProfile {
    targetId: string
    targetCode: string
    targetName: string
    targetType: TargetType

    marketCode: string
    marketName: string

    exchangeCode?: string
    exchangeName?: string

    currency?: string
    listingDate?: string
    delisted?: boolean

    logoUrl?: string
    tags?: string[]

    industryCode?: string
    industryName?: string

    description?: string
}

export interface MarketQuote {
    tradeDate: string
    tradeTime: string

    lastPrice?: number
    previousClose?: number
    open?: number
    high?: number
    low?: number

    changeAmount?: number
    changePercent?: number
    amplitude?: number

    volume?: number
    amount?: number
    turnoverRate?: number

    limitUpPrice?: number
    limitDownPrice?: number

    marketStatus: MarketStatus
    priceUnit?: string
}

export interface TargetOverview {
    overallView: OverallView
    overallViewText: string

    trendStatus: TrendStatus
    valuationStatus?: ValuationStatus

    riskLevel: RiskLevel
    confidence?: number

    summary: string
    reasons: string[]
    risks: string[]
    suggestions: string[]
}

export interface MetricCard {
    key: string
    title: string
    value: string | number
    unit?: string

    displayValue?: string

    trend?: MetricTrend
    status?: MetricStatus

    description?: string
    compareText?: string

    sortOrder?: number
}

export interface TargetPerformanceInterval {
    periodCode: string
    periodName: string
    changePercent?: number
    maxDrawdown?: number
    volatility?: number
    rankingText?: string
}

export interface ChartGroup {
    chartId: string
    title: string

    chartType:
        | 'LINE'
        | 'CANDLESTICK'
        | 'BAR'
        | 'AREA'
        | 'PIE'
        | 'SCATTER'
        | 'HEATMAP'
        | 'TABLE'

    period?: 'INTRADAY' | 'DAILY' | 'WEEKLY' | 'MONTHLY'

    xAxis?: string[]
    series: ChartSeries[]

    options?: Record<string, unknown>
}

export interface ChartSeries {
    name: string
    data: Array<number | string | null | OHLCData>
    unit?: string
}

export interface OHLCData {
    date: string
    open: number
    high: number
    low: number
    close: number
    volume?: number
    amount?: number
}

export interface ValuationInfo {
    peTtm?: number
    peStatic?: number
    pb?: number
    ps?: number
    peg?: number
    dividendYield?: number

    marketCap?: number
    floatMarketCap?: number

    pePercentile?: number
    pbPercentile?: number
    psPercentile?: number

    industryPeMedian?: number
    industryPbMedian?: number

    valuationStatus: ValuationStatus
    conclusion?: string
}

export interface FinancialSummary {
    reportPeriod: string
    reportType: 'Q1' | 'HALF_YEAR' | 'Q3' | 'ANNUAL'

    revenue?: number
    revenueYoY?: number

    netProfit?: number
    netProfitYoY?: number

    deductedNetProfit?: number
    deductedNetProfitYoY?: number

    grossMargin?: number
    netMargin?: number
    roe?: number
    roa?: number

    debtAssetRatio?: number
    operatingCashFlow?: number
    freeCashFlow?: number

    eps?: number
    bps?: number

    dividendPerShare?: number
    dividendPayoutRatio?: number

    conclusion?: string
}

export interface CapitalFlowInfo {
  tradeDate: string

  mainNetInflow?: number
  superLargeNetInflow?: number
  largeNetInflow?: number
  mediumNetInflow?: number
  smallNetInflow?: number

  northboundHoldingValue?: number
  northboundHoldingRatio?: number
  northboundChange?: number

  marginBalance?: number
  marginNetBuy?: number

  etfShareChange?: number
  etfNetInflow?: number

  conclusion?: string

  /**
   * 小单 / 中单 / 大单 / 特大单历史净流入。
   * 单位建议与当前资金流字段保持一致，例如：元。
   */
  historyFlows?: CapitalFlowHistoryItem[]
}

export interface CapitalFlowHistoryItem {
  tradeDate: string
  smallNetInflow?: number
  mediumNetInflow?: number
  largeNetInflow?: number
  superLargeNetInflow?: number
}


export interface IndustryInfo {
    industryCode: string
    industryName: string

    level?: number
    parentIndustryName?: string

    industryRankings?: IndustryRanking[]

    peerTargets?: RelatedTarget[]

    conclusion?: string
}

export interface IndustryRanking {
    metricCode: string
    metricName: string
    rank: number
    total: number
    percentile?: number
    displayValue: string
}

export interface ConstituentInfo {
    totalCount: number

    upCount?: number
    downCount?: number
    flatCount?: number

    topWeights?: ConstituentItem[]
    topGainers?: ConstituentItem[]
    topLosers?: ConstituentItem[]

    industryWeights?: WeightItem[]
}

export interface ConstituentItem {
    targetCode: string
    targetName: string
    weight?: number
    lastPrice?: number
    changePercent?: number
    contribution?: number
}

export interface WeightItem {
    name: string
    weight: number
}

export interface FundInfo {
    fundCompany?: string
    fundManager?: string
    inceptionDate?: string

    trackingIndexCode?: string
    trackingIndexName?: string

    fundSize?: number
    shareAmount?: number

    nav?: number
    accumulatedNav?: number
    estimatedNav?: number

    premiumRate?: number
    trackingError?: number

    managementFee?: number
    custodyFee?: number
}

export interface TargetWatchInfo {
    watchStatus: 'WATCHING' | 'KEY_WATCH' | 'PAUSED' | 'REMOVED' | 'NONE'
    watchStatusText: string
    groupNames?: string[]
    watchReason?: string
    firstWatchTime?: string
    lastUpdatedTime?: string
}

export interface TargetEvent {
    eventId: string

    eventType:
        | 'ANNOUNCEMENT'
        | 'EARNINGS'
        | 'DIVIDEND'
        | 'INDEX_REBALANCE'
        | 'SHAREHOLDER_MEETING'
        | 'RISK'
        | 'NEWS'
        | 'CUSTOM'

    title: string
    eventDate: string

    importance: 'LOW' | 'MEDIUM' | 'HIGH'

    source?: string
    url?: string

    summary?: string
}

export interface TargetNews {
    newsId: string
    title: string
    publishTime: string
    source: string
    url?: string
    sentiment?: 'POSITIVE' | 'NEUTRAL' | 'NEGATIVE'
    summary?: string
}

export interface AlertRule {
    alertId: string
    title: string

    alertType:
        | 'PRICE'
        | 'CHANGE_PERCENT'
        | 'VOLUME'
        | 'VALUATION'
        | 'TECHNICAL'
        | 'FINANCIAL'
        | 'NEWS'
        | 'CUSTOM'

    enabled: boolean

    conditionText: string

    lastTriggeredTime?: string
    triggerCount?: number

    severity: 'LOW' | 'MEDIUM' | 'HIGH'
}

export interface AnalysisRecord {
    analysisId: string
    analysisTime: string

    sceneCode:
        | 'MARKET'
        | 'VALUATION'
        | 'FINANCIAL'
        | 'CAPITAL_FLOW'
        | 'RISK'
        | 'EVENT'
        | 'CUSTOM'

    sceneName: string

    conclusion: OverallView
    conclusionText: string

    signalStrength: SignalStrength
    riskLevel: RiskLevel

    summary: string
    keyPoints: string[]

    sourceDataTime: string
    modelVersion?: string
}

export interface RelatedTarget {
    targetCode: string
    targetName: string
    targetType: TargetType | string

    relationType:
        | 'SAME_INDUSTRY'
        | 'SAME_CONCEPT'
        | 'CONSTITUENT'
        | 'TRACKING_INDEX'
        | 'RELATED_ETF'
        | 'BENCHMARK'
        | 'PEER'

    relationName: string

    lastPrice?: number
    changePercent?: number
}

export interface TargetFeatureFlags {
    quote: boolean
    kline: boolean
    valuation: boolean
    financial: boolean
    capitalFlow: boolean
    constituents: boolean
    fundInfo: boolean
    news: boolean
    alerts: boolean
    analysisHistory: boolean
}

export interface TargetPermissions {
    canWatch: boolean
    canCreateAlert: boolean
    canViewFinancial: boolean
    canViewCapitalFlow: boolean
    canViewNews: boolean
    canRunAnalysis: boolean
}

export interface ResponseMeta {
    dataSource: string
    dataVersion?: string
    generatedAt: string
    cacheExpireAt?: string
    partialData?: boolean
    warnings?: string[]
}
