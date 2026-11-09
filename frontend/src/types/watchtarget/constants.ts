import { CommonStatus } from "../appscene/constants";

export type WatchStatus = 'WATCHING' | 'FOCUS' | 'PAUSED' | 'CLOSED';

/** 与数据集、应用指标共用的跨服务标的类型协议。 */
export type AssetType = 'STOCK' | 'INDEX' | 'FUTURE' | 'FUND' | 'BOND';

export interface WatchTargetQueryParams {
    keyword?: string;
    targetCode?: string;
    targetName?: string;
    targetType?: AssetType | '';
    marketCode?: string;
    groupCode?: string;
    watchStatus?: WatchStatus | '';
    pageNo?: number;
    pageSize?: number;
}

export interface TargetGroupQueryParams {
    keyword?: string;
    status?: CommonStatus | '';
    pageNo?: number;
    pageSize?: number;
}

export interface WatchTargetSaveRequest {
    targetCode: string;
    targetName: string;
    targetType: AssetType;
    marketCode: string;
    groupCodes?: string[];
    sortNo: number;
    importantFlag: boolean;
    watchStatus: WatchStatus;
    watchReason?: string;
}

export interface TargetGroupSaveRequest {
    groupCode: string;
    groupName: string;
    sortNo: number;
    status: CommonStatus;
    description?: string;
}
