

/** 前端查询参数。 */
export interface TargetDetailQuery {
    /** 标的代码，例如 000001.SZ。 */
    targetCode: string
    /** 标的类型，可为空，由后端识别。 */
    targetType?: string
    /** 查询交易日。 */
    tradeDate?: string
    /** 市场代码，例如 CN_A。 */
    marketCode?: string
    /** 是否返回图表数据。 */
    includeCharts?: boolean
    /** 是否返回财务数据。 */
    includeFinancial?: boolean
    /** 是否返回新闻事件。 */
    includeNews?: boolean
    /** 是否返回历史分析。 */
    includeHistory?: boolean
}