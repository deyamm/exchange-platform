import http from '/@/utils/http';
import type { PageResult } from '/@/types/common';
import type { FinancialSummaryQueryParams, FinancialTrendQueryParams } from '/@/types/financial/constants';
import type { AppResult } from '/@/types/marketoverview/entity';

/**
 * I-22 查询财务摘要
 * GET /api/v1/application/targets/{targetCode}/financial-summary
 */
export function queryFinancialSummary(params: FinancialSummaryQueryParams): Promise<AppResult> | any {
    const { targetCode, sceneCode } = params;
    return http.get<AppResult>(
        `/api/v1/application/targets/${targetCode}/financial-summary`,
        { params: { sceneCode } }
    );
}

/**
 * I-23 查询财务趋势
 * GET /api/v1/application/targets/{targetCode}/financial-trends
 */
export function queryFinancialTrends(params: FinancialTrendQueryParams): Promise<PageResult<AppResult>> | any {
    const { targetCode, metricCode, pageNo, pageSize } = params;
    return http.get<PageResult<AppResult>>(
        `/api/v1/application/targets/${targetCode}/financial-trends`,
        { params: { metricCode, pageNo, pageSize } }
    );
}
