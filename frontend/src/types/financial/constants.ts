/**
 * A-09 财务与基本面分析模块常量与查询参数类型。
 */

/** 财务摘要查询参数（I-22） */
export interface FinancialSummaryQueryParams {
    targetCode: string;
    sceneCode?: string;
}

/** 财务趋势查询参数（I-23） */
export interface FinancialTrendQueryParams {
    targetCode: string;
    metricCode?: string;
    pageNo?: number;
    pageSize?: number;
}
