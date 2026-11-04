import type { ChartGroup, IndexConstituentDetail } from '/@/types/marketoverview/view'
import type { ResponseMeta } from './indexCommon'

/* -------------------------------------------------------------------------- */
/*                                   成分股类                                  */
/*                     成分股统计、行业权重与涨跌分布                          */
/* -------------------------------------------------------------------------- */

/**
 * 成分股 Tab 接口返回。
 *
 * 对应 index-constituents 接口：成分股统计、行业权重、涨跌幅分布与图表。
 */
export interface IndexConstituents {
    constituents?: IndexConstituentInfo
    constituentCharts: ChartGroup[]
    meta: ResponseMeta
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
