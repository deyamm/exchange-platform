/**
 * A-07 市场概览模块常量与查询参数类型。
 */

import { WatchStatus } from '/@/types/watchtarget/constants';

/** 应用结果类型枚举（result_type），与数据库设计 8.8 保持一致 */
export type ResultType =
    | 'MARKET_OVERVIEW'
    | 'TARGET_DETAIL'
    | 'FINANCIAL_SUMMARY'
    | 'RANKING'
    | 'COMPARISON'
    | 'ALERT';


/** 市场概览查询参数（I-18） */
export interface MarketOverviewQueryParams {
    sceneCode?: string;
}

/** 自选观察概览查询参数（I-19） */
export interface WatchOverviewQueryParams {
    targetCode?: string;
    targetName?: string;
    groupCode?: string;
    watchStatus?: WatchStatus | number | '';
    pageNo?: number;
    pageSize?: number;
}

