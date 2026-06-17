import { CommonStatus } from "../appscene/constants";

export type WatchStatus = 'WATCHING' | 'FOCUS' | 'PAUSED' | 'CLOSED';

export type AssetType = 'STOCK' | 'FUND' | 'INDEX' | 'BOND' | 'OTHER';

export interface WatchTargetQueryParams {
    keyword?: string;
    targetCode?: string;
    targetName?: string;
    targetType?: string;
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
    targetType: string;
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