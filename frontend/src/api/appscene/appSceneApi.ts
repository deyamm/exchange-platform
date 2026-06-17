import http from '/@/utils/http'
import { AppSceneQueryParams, SceneSaveRequest } from '/@/types/appscene/constants'
import { AppScene } from '/@/types/appscene/entity'
import { PageResult } from '/@/types/common';

/**
 * 获取应用场景列表，支持支持分页和按名称、分类、状态过滤
 * @param params 
 * @returns 
 */
export function queryAppScenePage(params: AppSceneQueryParams): Promise<PageResult<AppScene>> | any {
    return http.get<PageResult<AppScene>>(
        '/api/v1/analytics/app-scenes', 
        { params }
    );
}

/**
 * 获取所有已启用的应用场景列表
 * @returns
 */
export function getAllEnabledAppSceneList(): Promise<AppScene[]> | any {
    return http.get<AppScene[]>(
        '/api/v1/analytics/app-scenes/enabled'
    );
}

/**
 * 创建或更新应用场景
 * @param data 
 * @returns
 */
export function saveOrUpdateAppScene(data: SceneSaveRequest): Promise<AppScene> | any {
    return http.post<AppScene>(
        '/api/v1/analytics/app-scenes', 
        data
    );
}

/**
 * 删除应用场景
 * @param sceneCode 
 * @returns
 */
export function deleteAppScene(sceneCode: string): Promise<void> | any {
    return http.delete(
        `/api/v1/analytics/app-scenes/${sceneCode}`
    );
}
