import type { IndexConstituentDetail, IndexProfile } from '/@/types/marketoverview/view'
import type { TargetDetailMeta } from './common'

/* -------------------------------------------------------------------------- */
/*                                   行业类                                    */
/*             所属申万行业、所属指数、同行业对比、指数成分与行业摘要             */
/* -------------------------------------------------------------------------- */

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

/** 行业 Tab 接口查询参数。 */
export interface TargetIndustryDetailQuery {
    /** 完整代码，包含交易所后缀。 */
    fullSymbol: string
    /** 查询截止交易日；不传时由后端解析最近可用交易日。 */
    tradeDate?: string | null
}

/**
 * 行业 Tab 接口返回。
 *
 * indexProfile 仅在该接口中保留，用于兼容现有行业 / 指数场景；
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
