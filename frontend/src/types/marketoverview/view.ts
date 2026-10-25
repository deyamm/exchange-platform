import { MarketStatus, MetricStatus, MetricTrend, RiskLevel } from "./constants"

/** 指标卡片。 */
export interface MetricCard {
    /** 唯一键。 */
    key: string
    /** 标题。 */
    title: string
    /** 原始值。 */
    value?: number | string
    /** 格式化展示值。 */
    displayValue?: string
    /** 单位。 */
    unit?: string | null
    /** 趋势。 */
    trend?: MetricTrend
    /** 对比描述。 */
    compareText?: string
    /** 补充说明。 */
    description?: string
    /** 状态。 */
    status?: MetricStatus
    /** 排序。 */
    sortOrder?: number
}

/** 最新行情摘要。 */
export interface MarketQuote {
    /** 完整代码。 */
    fullSymbol: string
    /** 交易日期。 */
    tradeDate: string
    /** 交易时间。 */
    tradeTime?: string
    /** 交易状态。 */
    marketStatus?: MarketStatus
    /** 开盘价。 */
    open: number
    /** 最高价。 */
    high: number
    /** 最低价。 */
    low: number
    /** 收盘价或最新价。 */
    close: number
    /** 昨收价。 */
    preClose: number
    /** 涨跌额。 */
    chgAmt?: number
    /** 涨跌幅，单位为百分比。 */
    chgPct?: number
    /** 振幅，单位为百分比。 */
    amplitude?: number | null
    /** 成交量。 */
    volume?: number
    /** 成交额。 */
    turnover?: number
    /** 换手率，单位为百分比。 */
    turnoverRate?: number | null
    /** 复权因子。 */
    adjustFactor?: number | null
    /** 价格单位，例如 元、点。 */
    priceUnit?: string | null
}

/** 权重数据。 */
export interface WeightItem {
    /** 名称。 */
    name: string
    /** 权重，单位为百分比。 */
    weight: number
}

/** OHLC 图表数据。 */
export interface OHLCData {
    /** 交易日期。 */
    tradeDate: string
    /** 开盘价。 */
    open: number
    /** 最高价。 */
    high: number
    /** 最低价。 */
    low: number
    /** 收盘价。 */
    close: number
    /** 昨收价。 */
    preClose: number
    /** 涨跌额。 */
    chgAmt?: number
    /** 涨跌幅。 */
    chgPct?: number
    /** 成交量。 */
    volume?: number
    /** 成交额。 */
    turnover?: number
    /** 换手率，单位为百分比。 */
    turnoverRate?: number
}

/** 图表序列。 */
export interface ChartSeries {
    /** 序列名称。 */
    name: string
    /** 序列数据。 */
    data: Array<number | null | string | OHLCData | Record<string, any>>
    /** 单位。 */
    unit?: string
}

/** 图表组。 */
export interface ChartGroup {
    /** 图表 ID。 */
    chartId: string
    /** 图表标题。 */
    title?: string | null
    /** 图表类型。 */
    chartType: 'LINE' | 'BAR' | 'PIE' | 'CANDLESTICK' | 'TABLE' | string
    /** 图表周期。 */
    period?: 'DAILY' | 'WEEKLY' | 'MONTHLY'
    /** X 轴数据。 */
    xAxis?: string[]
    /** 序列。 */
    series: ChartSeries[],
    /** 图表选项。 */
    options?: Record<string, unknown>
}

/** 区间表现。 */
export interface TargetPerformanceInterval {
    /** 区间编码。 */
    periodCode: string
    /** 区间名称。 */
    periodName: string
    /** 区间收益率。 */
    changePercent?: number | null
    /** 最大回撤。 */
    maxDrawdown?: number | null
    /** 波动率。 */
    volatility?: number | null
    /** 排名文本。 */
    rankingText?: string | null
}

/**
 * 指数基础信息。
 *
 * 来源表：type_basic_data.index_basic_data
 */
export interface IndexProfile {
    /** type_basic_data.index_basic_data.full_symbol：指数代码全称，包含交易所后缀 */
    fullSymbol: string

    /** type_basic_data.index_basic_data.name：指数简称 */
    name: string

    /** type_basic_data.index_basic_data.fullname：指数全称 */
    fullname?: string

    /** type_basic_data.index_basic_data.market：市场，例如 MSCI / CSI / SSE / SZSE / SW */
    market?: string

    /** market 的中文展示名，前端根据字典转换得到，不要求物理表存储 */
    marketName?: string

    /** type_basic_data.index_basic_data.publisher：发布方 */
    publisher?: string

    /** type_basic_data.index_basic_data.index_type：指数风格 */
    indexType?: string

    /** type_basic_data.index_basic_data.category：指数类别 */
    category?: string

    /** type_basic_data.index_basic_data.base_date：基期 */
    baseDate?: string

    /** type_basic_data.index_basic_data.base_point：基点 */
    basePoint?: number

    /** type_basic_data.index_basic_data.list_date：发布日期 */
    listDate?: string

    /** type_basic_data.index_basic_data.weight_rule：加权方式 */
    weightRule?: string

    /** type_basic_data.index_basic_data.description：描述 */
    description?: string

    /** type_basic_data.index_basic_data.exp_date：终止日期 */
    expDate?: string

    /** 页面展示标签，由 market / category / indexType / weightRule 组合生成 */
    tags?: string[]
}

/**
 * 指数成分股详情行。
 */
export interface IndexConstituentDetail {
    /** type_basic_data.index_constituent_data.index_code */
    indexCode: string

    /** type_basic_data.index_constituent_data.stock_code */
    stockCode: string

    /** type_basic_data.index_constituent_data.trade_date */
    tradeDate: string

    /** type_basic_data.index_constituent_data.weight，单位为百分比。 */
    weight?: number | null

    /** type_basic_data.stock_basic_data.name */
    name?: string | null

    /** 申万三级行业；兼容既有字段。 */
    industry?: string | null

    /** 申万一级行业。 */
    industryL1?: string | null

    /** 申万二级行业。 */
    industryL2?: string | null

    /** 申万三级行业。 */
    industryL3?: string | null

    /** type_exchange_data.stock_k_daily.close */
    close?: number | null

    /** type_exchange_data.stock_k_daily.chg_pct，单位为百分比。 */
    chgPct?: number | null

    /** type_exchange_data.stock_indicator_daily.total_market_value，单位元。 */
    totalMarketValue?: number | null

    /** type_exchange_data.stock_indicator_daily.float_market_value，单位元。 */
    floatMarketValue?: number | null

    /** type_exchange_data.stock_indicator_daily.pe_ttm */
    peTtm?: number | null

    /** type_exchange_data.stock_indicator_daily.pb */
    pb?: number | null

    /** type_exchange_data.stock_indicator_daily.turnover_rate */
    turnoverRate?: number | null
}

/** 关注信息。 */
export interface TargetWatchInfo {
    /** 关注状态。 */
    watchStatus?: string | null
    /** 关注状态展示文本。 */
    watchStatusText?: string | null
    /** 分组名称。 */
    groupNames?: string[]
    /** 关注理由。 */
    watchReason?: string | null
    /** 首次关注时间。 */
    firstWatchTime?: string | null
    /** 最后更新时间。 */
    lastUpdatedTime?: string | null
}

/** 提醒规则。 */
export interface AlertRule {
    /** 提醒 ID。 */
    alertId: string
    /** 标题。 */
    title: string
    /** 条件描述。 */
    conditionText?: string | null
    /** 严重程度。 */
    severity?: RiskLevel | string | null
    /** 是否启用。 */
    enabled?: boolean
    /** 触发次数。 */
    triggerCount?: number | null
}

/** 标的事件。 */
export interface TargetEvent {
    /** 事件 ID。 */
    eventId: string
    /** 事件日期。 */
    eventDate?: string | null
    /** 标题。 */
    title: string
    /** 摘要。 */
    summary?: string | null
}

/** 相关标的。 */
export interface RelatedTarget {
    /** 完整代码。 */
    fullSymbol: string
    /** 名称。 */
    name: string
    /** 关系类型。 */
    relationType?: string | null
    /** 关系名称。 */
    relationName?: string | null
    /** 涨跌幅。 */
    chgPct?: number | null
}
