import http from '/@/utils/http';
import type { PageResult } from '/@/types/common';
import type { TargetDetailQueryParams, TargetHistoryQueryParams } from '/@/types/targetdetail/constants';
import type { TargetDetail } from '/@/types/targetdetail/entity';
import type { ApplicationResult } from '/@/types/marketoverview/entity';

/**
 * I-20 查询标的详情
 * GET /api/v1/analytics/targets/{targetCode}/detail
 */
export function queryTargetDetail(params: TargetDetailQueryParams): Promise<TargetDetail> | any {
    const { targetCode, sceneCode } = params;
    return http.get<TargetDetail>(
        `/api/v1/analytics/targets/${targetCode}/detail`, { 
            params: { sceneCode }
        }
    );
}

/**
 * I-21 查询标的历史数据
 * GET /api/v1/analytics/targets/{targetCode}/history
 */
export function queryTargetHistory(params: TargetHistoryQueryParams): Promise<PageResult<ApplicationResult>> | any {
    const { targetCode, metricCode, pageNo, pageSize } = params;
    return http.get<PageResult<ApplicationResult>>(
        `/api/v1/analytics/targets/${targetCode}/history`, { 
            params: { 
                metricCode, 
                pageNo, 
                pageSize 
            } 
        }
    );
}
