import type {
    OverallView,
    RiskLevel,
    ValuationStatus,
    MetricTrend,
    RangeCode
} from '/@/types/marketoverview/constants'
import type { MetricCard, MarketQuote, ChartGroup, IndexProfile, IndexConstituentDetail } from '/@/types/marketoverview/view'
/**
 * 指数详情页聚合数据。
 *
 */
export interface IndexDetail {
    profile: IndexProfile
    quote?: MarketQuote
    indicator?: IndexIndicatorDaily
    overview: IndexOverview
    metricCards: MetricCard[]
    meta: ResponseMeta
}

export interface IndexMarket {
    marketCharts: ChartGroup[]
    quoteAnalysis?: QuoteTechnicalAnalysis
    /** 行情走势页区间表现，可由后端按当前区间返回；前端也可基于 K 线数据兜底计算 */
    rangePerformance?: IndexRangePerformance
}

export interface IndexConstituents {
    constituents?: IndexConstituentInfo
    constituentCharts: ChartGroup[]
    meta: ResponseMeta
}


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



export type IndexIndustryWeightMode = 'REGULAR_INDEX' | 'INDUSTRY_INDEX'

export type IndexIndustryLevelCode = 'SW_L1' | 'SW_L2' | 'SW_L3' | 'STOCK'

export interface IndexIndustryWeightItem {
    /** 行业、股票的展示名称。 */
    name: string

    /** 行业代码或股票代码。 */
    code?: string

    /** 当前节点层级。 */
    level?: IndexIndustryLevelCode

    /** 权重，单位为百分比。非行业指数来自 index_constituent_data.weight 聚合；行业指数来自流通市值占比。 */
    weight: number

    /** 当前节点包含的成分股数量。 */
    constituentCount: number

    /** 行业指数口径下的流通市值汇总，单位元。 */
    floatMarketValue?: number | null

    /** 股票叶子节点时对应的完整股票代码。 */
    stockCode?: string

    /** 下级行业或成分股节点。 */
    children?: IndexIndustryWeightItem[]
}

export interface IndexConstituentChangeRange {
    /** 区间编码，例如 flat、up-0-3、up-3-5、up-5-7、up-7-10、up-gt-10 */
    rangeCode: string

    /** 区间展示名，例如 0、0-3%、3-5%、>10%。 */
    label: string

    /** 成分股数量。 */
    count: number

    /** 占比，单位为百分比。 */
    ratio?: number

    /** 趋势方向，1 表示上涨，-1 表示下跌，0 表示平盘。 */
    direction: number
}

export interface IndexConstituentBreadthItem {
    key: 'UP' | 'DOWN' | 'FLAT'
    label: string
    count: number
    /** 占比，单位为百分比。 */
    ratio?: number
}

export interface IndexConstituentLimitStats {
    /** 涨停数量。 */
    limitUpCount: number

    /** 跌停数量。 */
    limitDownCount: number

    /** 涨停占比，单位为百分比。 */
    limitUpRatio?: number

    /** 跌停占比，单位为百分比。 */
    limitDownRatio?: number
}

export interface IndexConstituentInfo {
    /** 成分股对应交易日，来自 index_constituent_data.trade_date。 */
    tradeDate?: string

    /** 成分股总数。 */
    totalCount: number

    /** 上涨、下跌、平盘数量，基于 stock_k_daily.chg_pct 计算。 */
    upCount?: number
    downCount?: number
    flatCount?: number

    /** 前十大权重集中度，单位为百分比。 */
    topTenWeightConcentration?: number

    /** 行业权重口径：普通指数按成分权重聚合，行业指数按流通市值占比聚合。 */
    industryWeightMode?: IndexIndustryWeightMode

    /** 非行业指数默认展示的一级行业权重。 */
    industryWeights?: IndexIndustryWeightItem[]

    /** 行业指数的申万一/二/三级行业与成分股树。 */
    industryTree?: IndexIndustryWeightItem[]

    /** 涨跌幅绝对值区间分布。用于展示不同涨跌幅区间的成分股数量。当前划分0，0-3，3-5，5-7，7-10，>10，适用于上涨和下跌。 */
    changeRanges?: IndexConstituentChangeRange[]

    /** 涨、跌、平盘数量与占比。 */
    breadthStats?: IndexConstituentBreadthItem[]

    /** 涨跌停数量与占比。 */
    limitStats?: IndexConstituentLimitStats

    /** 前十大权重股。 */
    topWeights: IndexConstituentDetail[]

    /** 涨幅靠前成分股。 */
    topGainers: IndexConstituentDetail[]

    /** 跌幅靠前成分股。 */
    topLosers: IndexConstituentDetail[]

    /** 全量成分股列表，前端按权重降序分页展示。 */
    records: IndexConstituentDetail[]
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


export interface ResponseMeta {
    dataSource: string
    dataVersion?: string
    generatedAt: string
    cacheExpireAt?: string
    partialData?: boolean
    warnings?: string[]
}