import { PageResult } from '/@/types/common';
import {
    TargetGroupQueryParams,
    TargetGroupSaveRequest,
    WatchTargetQueryParams,
    WatchTargetSaveRequest,
} from '/@/types/watchtarget/constants';
import { TargetGroup, WatchTarget } from '/@/types/watchtarget/entity';
import http from '/@/utils/http';

// 根据查询条件分页获取关注标的列表
export function queryWatchTargetPage(params: WatchTargetQueryParams): Promise<PageResult<WatchTarget>> | any {
    return http.get<PageResult<WatchTarget>>(
        '/api/v1/analytics/watch-targets',
        { params },
    );
}

// 根据查询条件分页获取标的分组列表
export function queryTargetGroupPage(params: TargetGroupQueryParams): Promise<PageResult<TargetGroup>> | any {
    return http.get<PageResult<TargetGroup>>(
        '/api/v1/analytics/target-groups',
        { params },
    );
}

// 获取关注标的详情，主要包括标的所属的分组信息， TODO: 每个标的都包含各自信息，这一部分还没有体现
export function getWatchTargetGroups(targetCode: string, targetType: string): Promise<WatchTarget> | any {
    return http.get<WatchTarget>(
        `/api/v1/analytics/watch-targets/${targetCode}/groups`,
        { params: { targetType } },
    );
}

// 新增或更新关注标的，
export function saveOrUpdateWatchTarget(data: WatchTargetSaveRequest): Promise<WatchTarget> | any {
    return http.post<WatchTarget>(
        '/api/v1/analytics/watch-targets',
        data,
    );
}

export function deleteWatchTarget(
    targetCode: string,
    params?: Pick<WatchTargetQueryParams, 'targetType' | 'groupCode'>,
): Promise<void> | any {
    return http.delete(
        `/api/v1/analytics/watch-targets/${targetCode}`,
        { params },
    );
}

export function saveOrUpdateTargetGroup(data: TargetGroupSaveRequest): Promise<TargetGroup> | any {
    return http.post<TargetGroup>(
        '/api/v1/analytics/target-groups',
        data,
    );
}

export function deleteTargetGroup(groupCode: string): Promise<void> | any {
    return http.delete(
        `/api/v1/analytics/target-groups/${groupCode}`,
    );
}