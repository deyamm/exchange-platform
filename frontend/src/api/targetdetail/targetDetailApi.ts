import http from '/@/utils/http';
import type { TargetDetailQuery, TargetDetail } from '/@/types/targetdetail/targetDetailType';

/**
 * I-20 查询标的详情
 * GET /api/v1/analytics/targets/{targetCode}/detail
 */
export function queryTargetDetail(params: TargetDetailQuery): Promise<TargetDetail> | any {
    const { targetCode, targetType, tradeDate } = params;
    return http.get<TargetDetail>(
        `/api/v1/analytics/watch-targets/${targetCode}/details`, { 
            params: { targetCode, tradeDate }
        }
    );
}


