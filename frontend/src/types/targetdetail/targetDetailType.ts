import type {
    TargetType,
    ValuationStatus,
    OverallView,
    RiskLevel,
    RangeCode,
    MainIndicatorCode,
    SubIndicatorCode,
    ValuationMetricCode,
} from '/@/types/marketoverview/constants'

import type {
    MetricCard,
    MarketQuote,
    ChartGroup,
    TargetPerformanceInterval,
    IndexProfile,
    IndexConstituentDetail,
    TargetWatchInfo,
    AlertRule,
    TargetEvent,
    RelatedTarget
} from '/@/types/marketoverview/view'



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

/** 财务摘要。 */
export interface FinancialSummary {
    /** 报告期。 */
    endDate?: string | null
    /** 营业收入。 */
    revenue?: number | null
    /** 营收同比。 */
    revenueYoy?: number | null
    /** 归母净利润。 */
    nIncomeAttrP?: number | null
    /** 净利润同比。 */
    netprofitYoy?: number | null
    /** 扣非净利润。 */
    profitDedt?: number | null
    /** 扣非净利润同比。 */
    dtNetprofitYoy?: number | null
    /** ROE。 */
    roe?: number | null
    /** ROA。 */
    roa?: number | null
    /** 资产负债率。 */
    debtToAssets?: number | null
    /** 经营现金流。 */
    nCashflowAct?: number | null
    /** 自由现金流。 */
    freeCashflow?: number | null
    /** EPS。 */
    eps?: number | null
    /** BPS。 */
    bps?: number | null
    /** 每股股利。 */
    dividendPerShare?: number | null
    /** 分红率。 */
    dividendPayoutRatio?: number | null
    /** 最新财报公告日期，来自 annDate，优先使用利润表或财务指标表最新报告期。 */
    annDate?: string | null
    /** 最新财报实际公告日期，来自 fAnnDate。 */
    fAnnDate?: string | null
    /** 最新财报原始报告类型。 */
    reportType?: string | null
    /** 最新财报类型展示文本，例如一季报、半年报、三季报、年报。 */
    reportTypeText?: string | null
    /** 结论。 */
    conclusion?: string | null
}

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

/** 申万行业层级。 */
export type SwIndustryLevel = 1 | 2 | 3

/** 个股所属指数分类。 */
export type StockRelatedIndexCategory = 'SIZE' | 'STRATEGY' | 'THEME' | 'INDUSTRY' | 'OTHER'

/**
 * 个股所属申万行业行。
 */
export interface StockSwIndustryRow {
    /** 股票代码全称。 */
    fullSymbol: string
    /** 股票简称。 */
    stockName?: string | null
    /** 申万行业代码。 */
    industryCode?: string | null
    /** 申万行业名称。 */
    industryName: string
    /** 申万行业层级，1=一级行业、2=二级行业、3=三级行业。 */
    industryLevel: SwIndustryLevel
    /** 父级申万行业代码。 */
    parentIndustryCode?: string | null
    /** 父级申万行业名称。 */
    parentIndustryName?: string | null
    /** 交易日期。 */
    tradeDate?: string | null
    /** 当天行业涨跌幅，单位为百分比。 */
    chgPct?: number | null
    /** 行业总市值，单位为元。 */
    totalMarketValue?: number | null
    /** 行业成交额，单位为元或后端统一口径。 */
    turnover?: number | null
    /** 行业成分数量。 */
    stockCount?: number | null
    /** 行业 PE TTM，可用于行业估值分布摘要。 */
    peTtm?: number | null
    /** 行业 PB，可用于行业估值分布摘要。 */
    pb?: number | null
    /** 行业 PS TTM，可用于行业估值分布摘要。 */
    psTtm?: number | null
}

/**
 * 个股所属指数行。
 *
 * 用于展示规模指数、策略指数、主题指数等个股所属指数。
 * indexCode 可与 index_basic_data.full_symbol 对齐；行情、市值和成交额可由 index_k_daily、
 * index_indicator_daily 或后端聚合结果补充。
 */
export interface StockBelongIndexRow {
    /** 股票代码全称。 */
    fullSymbol: string
    /** 股票简称。 */
    stockName?: string | null
    /** 指数代码全称。 */
    indexCode: string
    /** 指数名称。 */
    indexName: string
    /** 指数分类编码。 */
    indexCategory?: StockRelatedIndexCategory | null
    /** 指数分类展示名，例如规模指数、策略指数、主题指数。 */
    indexCategoryName?: string | null
    /** 交易日期。 */
    tradeDate?: string | null
    /** 指数当天涨跌幅，单位为百分比。 */
    chgPct?: number | null
    /** 指数总市值，单位为元。 */
    totalMarketValue?: number | null
    /** 指数成交额，单位为元或后端统一口径。 */
    turnover?: number | null
    /** 个股在指数中的权重，单位为百分比。 */
    weight?: number | null
}

/**
 * 行业详情页扩展数据。
 *
 * 行业详情页中：
 * - 所属行业以申万三级行业体系为主；
 * - 所属指数用于展示规模、策略、主题等指数；
 * - 同行业对比、行业估值分布、行业行情热度默认以申万三级行业为基准。
 */
export interface IndustryDetailData {
    /** 个股所属申万行业，一般包含一级、二级、三级三行。 */
    swIndustryRows?: StockSwIndustryRow[]
    /** 个股所属指数，包括规模指数、策略指数、主题指数等。 */
    belongIndexRows?: StockBelongIndexRow[]
    /** 同行业对比默认基准层级，默认取 3。 */
    benchmarkIndustryLevel?: SwIndustryLevel
    /** 同行业对比默认基准行业代码，通常为申万三级行业代码。 */
    benchmarkIndustryCode?: string | null
    /** 同行业对比默认基准行业名称，通常为申万三级行业名称。 */
    benchmarkIndustryName?: string | null
}

/** 行业摘要。 */
export interface IndustryInfo {
    /** 行业代码。 */
    industryCode?: string | null
    /** 行业名称。 */
    industryName?: string | null
    /** 行业层级。 */
    industryLevel?: SwIndustryLevel | null
    /** 股票数量。 */
    stockCount?: number | null
    /** 上涨数量。 */
    upCount?: number | null
    /** 下跌数量。 */
    downCount?: number | null
    /** 平盘数量。 */
    flatCount?: number | null
    /** 平均涨跌幅。 */
    avgChangePercent?: number | null
    /** PE 中位数。 */
    peMedian?: number | null
    /** PB 中位数。 */
    pbMedian?: number | null
}


/** 数据元信息。 */
export interface TargetDetailMeta {
    /** 数据来源。 */
    dataSource?: string | null
    /** 数据版本。 */
    dataVersion?: string | null
    /** 生成时间。 */
    generatedAt?: string | null
    /** 是否为部分数据。 */
    partialData?: boolean
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

/**
 * 股票基础信息行。
 *
 * 字段来源：type_basic_data.stock_basic_data。
 */
export interface StockProfile {
    /** 包含交易所标识的完整股票代码。 */
    fullSymbol: string
    /** 不包含交易所标识的股票代码。 */
    symbol?: string | null
    /** 证券简称。 */
    name?: string | null
    /** 地区。 */
    region?: string | null
    /** 所属行业。 */
    industry?: string | null
    /** 公司全称。 */
    fullName?: string | null
    /** 公司英文名称。 */
    enName?: string | null
    /** 中文拼音拼写。 */
    cnSpell?: string | null
    /** 市场类型，例如主板、创业板、科创板。 */
    marketType?: string | null
    /** 交易所代码。 */
    exchange?: string | null
    /** 交易货币。 */
    currencyType?: string | null
    /** 上市状态，L 上市、D 退市、G 过会未交易、P 暂停上市。 */
    listStatus?: string | null
    /** 上市日期。 */
    listDate?: string | null
    /** 退市日期。 */
    delistDate?: string | null
    /** 是否沪深港通标的，N 否、H 沪股通、S 深股通。 */
    isHs?: string | null
    /** 实控人名称。 */
    actName?: string | null
    /** 实控人企业性质。 */
    actEntType?: string | null
    
}


/**
 * 同行业可比股票行。
 *
 * 基础字段来自 stock_basic_data，行情与估值字段来自 stock_k_daily、stock_indicator_daily。
 */
export interface StockIndustryPeerRow extends StockProfile {
    /** 对比日期。 */
    tradeDate?: string | null
    /** 最新收盘价。 */
    close?: number | null
    /** 最新涨跌幅。 */
    chgPct?: number | null
    /** 总市值。 */
    totalMarketValue?: number | null
    /** 流通市值。 */
    floatMarketValue?: number | null
    /** PE TTM。 */
    peTtm?: number | null
    /** PB。 */
    pb?: number | null
    /** PS TTM。 */
    psTtm?: number | null
    /** 换手率。 */
    turnoverRate?: number | null
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

/**
 * 利润表明细行。
 *
 * 字段来源：type_finance_data.income_data。
 */
export interface IncomeStatementRow {
    /** TS 股票代码。 */
    fullSymbol: string
    /** 公告日期。 */
    annDate?: string | null
    /** 实际公告日期。 */
    fAnnDate?: string | null
    /** 报告期。 */
    endDate: string
    /** 报告类型。 */
    reportType?: string | null
    /** 公司类型。 */
    compType?: string | null
    /** 基本每股收益。 */
    basicEps?: number | null
    /** 稀释每股收益。 */
    dilutedEps?: number | null
    /** 营业总收入。 */
    totalRevenue?: number | null
    /** 营业收入。 */
    revenue?: number | null
    /** 营业总成本。 */
    totalCogs?: number | null
    /** 营业成本。 */
    operCost?: number | null
    /** 销售费用。 */
    sellExp?: number | null
    /** 管理费用。 */
    adminExp?: number | null
    /** 财务费用。 */
    finExp?: number | null
    /** 研发费用。 */
    rdExp?: number | null
    /** 投资收益。 */
    investIncome?: number | null
    /** 营业利润。 */
    operProfit?: number | null
    /** 利润总额。 */
    totalProfit?: number | null
    /** 所得税费用。 */
    incomeTax?: number | null
    /** 净利润。 */
    nIncome?: number | null
    /** 归属于母公司所有者的净利润。 */
    nIncomeAttrP?: number | null
    /** 息税前利润。 */
    ebit?: number | null
    /** 息税折旧摊销前利润。 */
    ebitda?: number | null
    /** 更新标识。 */
    updateFlag?: string | null
}

/**
 * 资产负债表明细行。
 *
 * 字段来源：type_finance_data.balance_sheet_data。
 */
export interface BalanceSheetRow {
    /** TS 股票代码。 */
    fullSymbol: string
    /** 公告日期。 */
    annDate?: string | null
    /** 实际公告日期。 */
    fAnnDate?: string | null
    /** 报告期。 */
    endDate: string
    /** 报告类型。 */
    reportType?: string | null
    /** 公司类型。 */
    compType?: string | null
    /** 期末总股本。 */
    totalShare?: number | null
    /** 货币资金。 */
    moneyCap?: number | null
    /** 交易性金融资产。 */
    tradAsset?: number | null
    /** 应收票据。 */
    notesReceiv?: number | null
    /** 应收账款。 */
    accountsReceiv?: number | null
    /** 其他应收款。 */
    othReceiv?: number | null
    /** 预付款项。 */
    prepayment?: number | null
    /** 存货。 */
    inventories?: number | null
    /** 流动资产合计。 */
    totalCurAssets?: number | null
    /** 长期股权投资。 */
    ltEqtInvest?: number | null
    /** 投资性房地产。 */
    investRealEstate?: number | null
    /** 固定资产。 */
    fixAssets?: number | null
    /** 在建工程。 */
    cip?: number | null
    /** 无形资产。 */
    intanAssets?: number | null
    /** 商誉。 */
    goodwill?: number | null
    /** 递延所得税资产。 */
    deferTaxAssets?: number | null
    /** 非流动资产合计。 */
    totalNca?: number | null
    /** 资产总计。 */
    totalAssets?: number | null
    /** 短期借款。 */
    stBorr?: number | null
    /** 应付票据。 */
    notesPayable?: number | null
    /** 应付账款。 */
    acctPayable?: number | null
    /** 预收款项。 */
    advReceipts?: number | null
    /** 应付职工薪酬。 */
    payrollPayable?: number | null
    /** 应交税费。 */
    taxesPayable?: number | null
    /** 其他应付款。 */
    othPayable?: number | null
    /** 一年内到期的非流动负债。 */
    nonCurLiabDue1y?: number | null
    /** 流动负债合计。 */
    totalCurLiab?: number | null
    /** 长期借款。 */
    ltBorr?: number | null
    /** 应付债券。 */
    bondPayable?: number | null
    /** 长期应付款。 */
    ltPayable?: number | null
    /** 递延所得税负债。 */
    deferTaxLiab?: number | null
    /** 非流动负债合计。 */
    totalNcl?: number | null
    /** 负债合计。 */
    totalLiab?: number | null
    /** 库存股。 */
    treasuryShare?: number | null
    /** 资本公积。 */
    capRese?: number | null
    /** 盈余公积。 */
    surplusRese?: number | null
    /** 未分配利润。 */
    undistrPorfit?: number | null
    /** 少数股东权益。 */
    minorityInt?: number | null
    /** 归属于母公司股东权益合计。 */
    totalHldrEqyExcMinInt?: number | null
    /** 股东权益合计。 */
    totalHldrEqyIncMinInt?: number | null
    /** 负债和股东权益总计。 */
    totalLiabHldrEqy?: number | null
    /** 更新标识。 */
    updateFlag?: string | null
}

/**
 * 现金流量表明细行。
 *
 * 字段来源：type_finance_data.cashflow_data。
 */
export interface CashflowStatementRow {
    /** TS 股票代码。 */
    fullSymbol: string
    /** 公告日期。 */
    annDate?: string | null
    /** 实际公告日期。 */
    fAnnDate?: string | null
    /** 报告期。 */
    endDate: string
    /** 报告类型。 */
    reportType?: string | null
    /** 公司类型。 */
    compType?: string | null
    /** 净利润。 */
    netProfit?: number | null
    /** 财务费用。 */
    finanExp?: number | null
    /** 销售商品、提供劳务收到的现金。 */
    cFrSaleSg?: number | null
    /** 经营活动现金流入小计。 */
    cInfFrOperateA?: number | null
    /** 购买商品、接受劳务支付的现金。 */
    cPaidGoodsS?: number | null
    /** 支付给职工以及为职工支付的现金。 */
    cPaidToForEmpl?: number | null
    /** 支付的各项税费。 */
    cPaidForTaxes?: number | null
    /** 经营活动现金流出小计。 */
    stCashOutAct?: number | null
    /** 经营活动产生的现金流量净额。 */
    nCashflowAct?: number | null
    /** 投资活动现金流入小计。 */
    stotInflowsInvAct?: number | null
    /** 投资活动现金流出小计。 */
    stotOutInvAct?: number | null
    /** 投资活动产生的现金流量净额。 */
    nCashflowInvAct?: number | null
    /** 筹资活动现金流入小计。 */
    stotCashInFncAct?: number | null
    /** 筹资活动现金流出小计。 */
    stotCashoutFncAct?: number | null
    /** 筹资活动产生的现金流量净额。 */
    nCashFlowsFncAct?: number | null
    /** 汇率变动对现金的影响。 */
    effFxFluCash?: number | null
    /** 现金及现金等价物净增加额。 */
    nIncrCashCashEqu?: number | null
    /** 期初现金及现金等价物余额。 */
    cCashEquBegPeriod?: number | null
    /** 期末现金及现金等价物余额。 */
    cCashEquEndPeriod?: number | null
    /** 企业自由现金流量。 */
    freeCashflow?: number | null
    /** 间接法经营活动现金流量净额。 */
    imNetCashflowOperAct?: number | null
    /** 更新标识。 */
    updateFlag?: string | null
}

/**
 * 财务指标明细行。
 *
 * 字段来源：type_finance_data.finance_indicator_data。
 */
export interface FinanceIndicatorRow {
    /** TS 股票代码。 */
    fullSymbol: string
    /** 公告日期。 */
    annDate?: string | null
    /** 报告期。 */
    endDate: string
    /** 基本每股收益。 */
    eps?: number | null
    /** 稀释每股收益。 */
    dtEps?: number | null
    /** 每股净资产。 */
    bps?: number | null
    /** 每股经营活动现金流量净额。 */
    ocfps?: number | null
    /** 每股现金流量净额。 */
    cfps?: number | null
    /** 每股息税前利润。 */
    ebitPs?: number | null
    /** 每股企业自由现金流量。 */
    fcffPs?: number | null
    /** 每股股东自由现金流量。 */
    fcfePs?: number | null
    /** 净资产收益率。 */
    roe?: number | null
    /** 加权平均净资产收益率。 */
    roeWaa?: number | null
    /** 扣非净资产收益率。 */
    roeDt?: number | null
    /** 总资产报酬率。 */
    roa?: number | null
    /** 投入资本回报率。 */
    roic?: number | null
    /** 毛利率。 */
    grossMargin?: number | null
    /** 净利率。 */
    netprofitMargin?: number | null
    /** 销售毛利率。 */
    grossprofitMargin?: number | null
    /** 净利润/营业总收入。 */
    profitToGr?: number | null
    /** 经营活动利润/利润总额。 */
    opToEbt?: number | null
    /** 流动比率。 */
    currentRatio?: number | null
    /** 速动比率。 */
    quickRatio?: number | null
    /** 现金比率。 */
    cashRatio?: number | null
    /** 资产负债率。 */
    debtToAssets?: number | null
    /** 权益乘数。 */
    assetsToEqt?: number | null
    /** 产权比率。 */
    debtToEqt?: number | null
    /** 息税前利润/利息费用。 */
    ebitToInterest?: number | null
    /** 应收账款周转率。 */
    arTurn?: number | null
    /** 存货周转率。 */
    invTurn?: number | null
    /** 流动资产周转率。 */
    caTurn?: number | null
    /** 固定资产周转率。 */
    faTurn?: number | null
    /** 总资产周转率。 */
    assetsTurn?: number | null
    /** 应收账款周转天数。 */
    arturnDays?: number | null
    /** 存货周转天数。 */
    invturnDays?: number | null
    /** 营业周期。 */
    turnDays?: number | null
    /** 每股营业收入。 */
    revenuePs?: number | null
    /** 每股营业总收入。 */
    totalRevenuePs?: number | null
    /** 每股资本公积。 */
    capitalResePs?: number | null
    /** 每股盈余公积。 */
    surplusResePs?: number | null
    /** 每股未分配利润。 */
    undistProfitPs?: number | null
    /** 扣除非经常性损益后的净利润。 */
    profitDedt?: number | null
    /** 非经常性损益。 */
    extraItem?: number | null
    /** 息税前利润。 */
    ebit?: number | null
    /** 息税折旧摊销前利润。 */
    ebitda?: number | null
    /** 企业自由现金流量。 */
    fcff?: number | null
    /** 股东自由现金流量。 */
    fcfe?: number | null
    /** 营运资金。 */
    workingCapital?: number | null
    /** 营业利润同比增长率。 */
    opYoy?: number | null
    /** 利润总额同比增长率。 */
    ebtYoy?: number | null
    /** 净利润同比增长率。 */
    netprofitYoy?: number | null
    /** 扣非净利润同比增长率。 */
    dtNetprofitYoy?: number | null
    /** 营业收入同比增长率。 */
    revenueYoy?: number | null
    /** 总资产同比增长率。 */
    assetsYoy?: number | null
    /** 净资产同比增长率。 */
    equityYoy?: number | null
    /** 更新标识。 */
    updateFlag?: string | null
}

/** 财报类型编码。 */
export type FinancialReportTypeCode = 'Q1' | 'HALF_YEAR' | 'Q3' | 'ANNUAL' 

/** 利润趋势 tab 指标编码。 */
export type FinancialProfitMetricCode = 'TOTAL_REVENUE' | 'N_INCOME_ATTR_P' | 'PROFIT_DEDT' | 'EPS'

/** 利润趋势柱状图展示模式。 */
export type FinancialProfitDisplayMode = 'ALL' | 'SINGLE_QUARTER'

/** 盈利、成长与负债率 tab 指标编码。 */
export type FinancialRatioMetricCode = 'ROE' | 'GROSS_MARGIN' | 'NETPROFIT_MARGIN' | 'DEBT_TO_ASSETS'

/** 财务季度编码。 */
export type FinancialQuarterCode = 'Q1' | 'Q2' | 'Q3' | 'Q4'

/**
 * 最新财报信息。
 *
 * 用于财务详情页首行展示最新财报发布日期和财报类型。
 */
export interface FinancialReportInfo {
    /** 股票代码全称。 */
    fullSymbol?: string | null
    /** 报告期。 */
    endDate?: string | null
    /** 公告日期。 */
    annDate?: string | null
    /** 实际公告日期。 */
    fAnnDate?: string | null
    /** 原始报告类型。 */
    reportType?: string | null
    /** 财报类型编码。 */
    reportTypeCode?: FinancialReportTypeCode | null
    /** 财报类型展示文本，例如一季报、半年报、三季报、年报。 */
    reportTypeText?: string | null
}

/**
 * 利润趋势基础行。
 *
 * 用于支持营业总收入、归母净利润、扣非净利润、每股收益四个 tab。
 * 数值可以是累计口径，也可以由后端提前加工成页面展示口径；页面会根据 endDate 和 quarterCode 做兼容计算。
 */
export interface FinancialProfitTrendRow {
    /** 股票代码全称。 */
    fullSymbol: string
    /** 报告期。 */
    endDate: string
    /** 公告日期。 */
    annDate?: string | null
    /** 年份。 */
    year?: number | null
    /** 季度编码。 */
    quarterCode?: FinancialQuarterCode | null
    /** 财报类型展示文本。 */
    reportTypeText?: string | null
    /** 营业总收入。 */
    totalRevenue?: number | null
    /** 营业总收入同比增长率。 */
    totalRevenueYoy?: number | null
    /** 归母净利润。 */
    nIncomeAttrP?: number | null
    /** 归母净利润同比增长率。 */
    nIncomeAttrPYoy?: number | null
    /** 扣非净利润。 */
    profitDedt?: number | null
    /** 扣非净利润同比增长率。 */
    profitDedtYoy?: number | null
    /** 每股收益。 */
    eps?: number | null
    /** 每股收益同比增长率。 */
    epsYoy?: number | null
}

/**
 * 盈利、成长与负债率趋势行。
 *
 * 用于 ROE、毛利率、净利率、资产负债率四个 tab 的柱状图。
 */
export interface FinancialRatioTrendRow {
    /** 股票代码全称。 */
    fullSymbol: string
    /** 报告期。 */
    endDate: string
    /** 财报类型展示文本。 */
    reportTypeText?: string | null
    /** ROE。 */
    roe?: number | null
    /** 毛利率。 */
    grossMargin?: number | null
    /** 净利率。 */
    netprofitMargin?: number | null
    /** 资产负债率。 */
    debtToAssets?: number | null
}

/**
 * 现金流结构行。
 *
 * 用于经营现金流、投资现金流、筹资现金流、自由现金流四项分组柱状图。
 */
export interface FinancialCashflowStructureRow {
    /** 股票代码全称。 */
    fullSymbol: string
    /** 报告期。 */
    endDate: string
    /** 年份。 */
    year?: number | null
    /** 季度编码。 */
    quarterCode?: FinancialQuarterCode | null
    /** 经营现金流。 */
    nCashflowAct?: number | null
    /** 投资现金流。 */
    nCashflowInvAct?: number | null
    /** 筹资现金流。 */
    nCashFlowsFncAct?: number | null
    /** 自由现金流。 */
    freeCashflow?: number | null
}

/**
 * 资产负债结构行。
 *
 * 用于资产、负债、归母权益三组柱状图，以及资产负债率折线图。
 */
export interface FinancialBalanceStructureRow {
    /** 股票代码全称。 */
    fullSymbol: string
    /** 报告期。 */
    endDate: string
    /** 流动资产。 */
    totalCurAssets?: number | null
    /** 非流动资产。 */
    totalNca?: number | null
    /** 总资产。 */
    totalAssets?: number | null
    /** 流动负债。 */
    totalCurLiab?: number | null
    /** 非流动负债。 */
    totalNcl?: number | null
    /** 总负债。 */
    totalLiab?: number | null
    /** 归母权益。 */
    totalHldrEqyExcMinInt?: number | null
    /** 资产负债率。 */
    debtToAssets?: number | null
}

/**
 * 财务详情页使用的四张财务表明细集合。
 */
export interface FinancialStatementDetail {
    /** 利润表明细。 */
    incomeRows?: IncomeStatementRow[]
    /** 资产负债表明细。 */
    balanceSheetRows?: BalanceSheetRow[]
    /** 现金流量表明细。 */
    cashflowRows?: CashflowStatementRow[]
    /** 财务指标明细。 */
    indicatorRows?: FinanceIndicatorRow[]
    /** 最新财报信息；不传时页面会由四表最新报告期自动推导。 */
    reportInfo?: FinancialReportInfo
    /** 利润趋势图表数据；不传时页面会由 incomeRows 与 indicatorRows 自动推导。 */
    profitTrendRows?: FinancialProfitTrendRow[]
    /** 盈利、成长与负债率图表数据；不传时页面会由 indicatorRows 自动推导。 */
    ratioTrendRows?: FinancialRatioTrendRow[]
    /** 现金流结构图表数据；不传时页面会由 cashflowRows 自动推导。 */
    cashflowStructureRows?: FinancialCashflowStructureRow[]
    /** 资产负债结构图表数据；不传时页面会由 balanceSheetRows 与 indicatorRows 自动推导。 */
    balanceStructureRows?: FinancialBalanceStructureRow[]
}

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
    /** 说明 */
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


/** 标的详情页 Tab 编码。 */
export type TargetDetailTabCode = 'QUOTE' | 'VALUATION' | 'FINANCIAL' | 'CAPITAL_FLOW' | 'INDUSTRY'

/** 通用 K 线周期。 */
export type TargetQuotePeriod = 'DAILY' | 'WEEKLY' | 'MONTHLY'


/**
 * 首屏轻量接口返回。
 *
 * 对应页面顶部：股票基本信息、最新行情、指标卡片，以及可用 Tab 等轻量数据。
 * 不返回 quoteRows / valuationRows / financialDetail / moneyFlowRows 等详情数组。
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

/** 行情 Tab 接口查询参数。 */
export interface TargetQuoteDetailQuery {
    /** 完整代码，包含交易所后缀。 */
    fullSymbol: string
    /** 复权类型。后端根据该参数返回对应复权口径的价格。 */
    adjustType: AdjustType
    /** 图表周期。 */
    period?: TargetQuotePeriod
    /** 可选：主图技术指标编码；不传时后端可返回默认或全部可展示 series。 */
    mainIndicatorCodes?: MainIndicatorCode[]
    /** 可选：副图技术指标编码；不传时后端可返回默认或全部可展示 series。 */
    subIndicatorCodes?: SubIndicatorCode[]
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
    /** 最新行情。 */
    quote?: MarketQuote
    /** 图表数据：固定以 ChartGroup 格式返回指定期限内所有可以展示的 series。 */
    charts?: ChartGroup[]
    /** 区间表现：旧摘要列表结构，保留用于兼容。 */
    performance?: TargetPerformanceInterval[]
    /** 行情详情明细，价格由后端按 adjustType 计算。 */
    quoteRows?: MarketQuote[]
    /** 技术指标综合分析。 */
    quoteTechnicalAnalysis?: QuoteTechnicalAnalysis
    /** 区间对比分析：保留旧字段，推荐同时放入 rangePerformance.comparison。 */
    quoteRangeComparison?: QuoteRangeComparison
    /** 区间表现聚合数据。 */
    rangePerformance?: RangePerformance
    /** 当前返回价格使用的复权类型。 */
    adjustType: AdjustType
    /** 当前返回图表周期。 */
    period?: TargetQuotePeriod
    /** 数据元信息。 */
    meta: TargetDetailMeta
}

/** 估值摘要接口返回。 */
export interface TargetValuationSummaryDetail {
    fullSymbol: string
    /** 估值摘要。 */
    valuation?: ValuationInfo | null
    /** 估值详情明细，来自 indicator_daily。 */
    valuationRows?: ValuationDailyRow[]
    /** 估值详情页摘要数据，主要使用 metricSummaries。 */
    valuationDetail?: ValuationMetricSummary[]
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

/** 分红走势接口返回。 */
export interface TargetDividendTrendDetail {
    fullSymbol?: string
    /** 分红走势明细。 */
    dividendRows?: DividendTrendRow[]
    /** 数据元信息。 */
    meta: TargetDetailMeta
}

/** 财务 Tab 接口查询参数。 */
export interface TargetFinancialDetailQuery {
    /** 完整代码，包含交易所后缀。 */
    fullSymbol: string
}

/** 财务 Tab 接口返回。 */
export interface TargetFinancialDetail {
    fullSymbol?: string
    /** 财务摘要。 */
    financial?: FinancialSummary | null
    /** 财务四表详情。 */
    financialDetail?: FinancialStatementDetail
    /** 数据元信息。 */
    meta: TargetDetailMeta
}

/** 资金 Tab 接口查询参数。 */
export interface TargetCapitalFlowDetailQuery {
    /** 完整代码，包含交易所后缀。 */
    fullSymbol: string
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
    close?: Array<number> | null
    /** 数据元信息。 */
    meta: TargetDetailMeta
}

/**
 * 行业 Tab 接口返回。
 *
 * 暂时保持原样不继续拆分。indexProfile 仅在该接口中保留，用于兼容现有行业 / 指数场景；
 * 首屏接口 TargetDetailShell 不返回 indexProfile。
 */
export interface TargetIndustryDetail {
    fullSymbol?: string
    /** 行业摘要，包含成分摘要。 */
    industry?: IndustryInfo | null
    /** 指数基础信息，来自 index_basic_data。 */
    indexProfile?: IndexProfile
    /** 股票基础信息，来自 stock_basic_data。 */
    stockProfile?: StockProfile
    /** 指数成分详情，来自 index_constituent_data 及关联股票数据。 */
    constituentRows?: IndexConstituentDetail[]
    /** 股票同行业可比明细，来自 stock_basic_data 及关联行情估值数据。 */
    industryPeerRows?: StockIndustryPeerRow[]
    /** 行业详情页扩展数据，包含申万三级行业归属与所属指数。 */
    industryDetail?: IndustryDetailData
    /** 数据元信息。 */
    meta: TargetDetailMeta
}

/** 标的详情页接口响应类型映射。 */
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

/**
 * 标的详情页完整页面组装态。
 *
 * 该类型用于前端在多个接口返回后合并为页面状态，不作为单个接口一次性返回。
 */
export interface TargetDetail {
    targetProfile: TargetProfile
    /** 最新行情。 */
    quote?: MarketQuote
    /** 综合概览。 */
    overview?: TargetOverview
    /** 顶部指标卡。 */
    metricCards?: MetricCard[]
    /** 图表数据。 */
    charts?: ChartGroup[]
    /** 区间表现。 */
    performance?: TargetPerformanceInterval[]
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
    /** 数据元信息。 */
    meta: TargetDetailMeta
    /** 行情详情明细，来自 k_daily 与指标表关联结果。 */
    quoteRows?: MarketQuote[]
    /** 技术指标综合分析；可由后端返回，也可由前端根据 quoteRows 计算。 */
    quoteTechnicalAnalysis?: QuoteTechnicalAnalysis
    /** 区间对比分析；推荐逐步迁移到 rangePerformance.comparison。 */
    quoteRangeComparison?: QuoteRangeComparison
    /** 区间表现聚合数据。 */
    rangePerformance?: RangePerformance
    /** 估值详情明细，来自 indicator_daily。 */
    valuationRows?: ValuationDailyRow[]
    /** 估值详情页新增的估值通道、估值走势和分红走势数据。 */
    valuationDetail?: ValuationDetailData
    /** 财务四表详情。 */
    financialDetail?: FinancialStatementDetail
    /** 股票资金流明细，来自 stock_moneyflow_daily。 */
    moneyFlowRows?: StockMoneyflowDailyRow[]
    /** 股票基础信息，来自 stock_basic_data。 */
    stockProfile?: StockProfile
    /** 指数成分详情，来自 index_constituent_data 及关联股票数据。 */
    constituentRows?: IndexConstituentDetail[]
    /** 股票同行业可比明细，来自 stock_basic_data 及关联行情估值数据。 */
    industryPeerRows?: StockIndustryPeerRow[]
    /** 行业详情页扩展数据，包含申万三级行业归属与所属指数。 */
    industryDetail?: IndustryDetailData
}