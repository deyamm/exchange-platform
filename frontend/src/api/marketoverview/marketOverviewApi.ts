import http from '/@/utils/http';
import type { MarketOverviewQueryParams, WatchOverviewQueryParams } from '/@/types/marketoverview/constants';
import type { AppResult } from '/@/types/marketoverview/entity';
import type { WatchTarget } from '/@/types/watchtarget/entity';

/**
 * I-18 查询市场概览
 * GET /api/v1/analytics/market/overview
 */
export function queryMarketOverview(params: MarketOverviewQueryParams): Promise<AppResult> | any {
    return http.get<AppResult>(
        '/api/v1/analytics/market/overview', 
        { params }
    );
}

/**
 * I-19 查询自选观察概览
 * GET /api/v1/analytics/watch-targets/overview
 */
export function queryWatchOverview(params: WatchOverviewQueryParams): Promise<WatchTarget[]> | any {
    return http.get<WatchTarget[]>(
        '/api/v1/analytics/watch-targets/overview', 
        { params }
    );
}

/**
 * 查询指数基本信息
 */
export function getIndexBasic(params: MarketOverviewQueryParams): Promise<any[]> | any {
    return http.get<any[]>(
        '/api/v1/analytics/market/index-basic', 
        { params }
    );
}
