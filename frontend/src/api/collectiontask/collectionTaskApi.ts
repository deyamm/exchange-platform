import http from '/@/utils/http'
import type { PageResult } from '/@/types/common'
import type {
    TaskTemplate, TaskDetail, TaskTemplateVersion, TaskConfig, StorageMapping
} from '/@/types/collectiontask/entity'
import type {
    TaskQueryParams, TaskConfigSaveRequest,
    TaskSyncRequest, StorageMappingDraftRequest, TaskStatusUpdateRequest
} from '/@/types/collectiontask/constants'


/**
 * I-01 查询采集任务列表（分页）
 */
export function queryTaskList(params: TaskQueryParams): Promise<PageResult<TaskTemplate>> | any {
    return http.get<PageResult<TaskTemplate>>(
        '/api/v1/collection/tasks',
        { params }
    )
}

/**
 * 删除采集任务模板
 * @param taskCode 
 * @returns 
 */
export function deleteTaskTemplate(taskCode: string): Promise<void> | any {
    return http.delete<void>(
        `/api/v1/collection/tasks/${taskCode}`
    )
}

/**
 * I-02 查询采集任务详情
 */
export function getTaskDetail(taskCode: string): Promise<TaskDetail> | any {
    return http.get<TaskDetail>(
        `/api/v1/collection/tasks/${taskCode}`
    )
}

/**
 * I-03 保存采集任务配置（新增或修改）
 */
export function saveTaskConfig(data: TaskConfigSaveRequest): Promise<TaskConfig> | any {
    return http.post<TaskConfig>(
        '/api/v1/collection/tasks/configs',
        data
    )
}

/**
 * 删除采集任务配置
 */
export function deleteTaskConfig(taskConfigCode: string): Promise<void> | any {
    return http.delete<void>(
        `/api/v1/collection/tasks/configs/${taskConfigCode}`
    )
}

/**
 * I-05 手动同步任务模板
 */
export function syncTaskTemplate(data: TaskSyncRequest): Promise<void> | any {
    return http.post<void>(
        '/api/v1/collection/tasks/sync',
        data
    )
}

/**
 * I-06 查询任务模板版本列表
 */
export function listTemplateVersions(taskCode: string): Promise<TaskTemplateVersion[]> | any {
    return http.get<TaskTemplateVersion[]>(
        `/api/v1/collection/tasks/${taskCode}/versions`
    )
}

/**
 * I-07 查询任务模板版本详情
 */
export function getTemplateVersionDetail(
    taskCode: string,
    versionNo: number
): Promise<TaskTemplateVersion> | any {
    return http.get<TaskTemplateVersion>(
        `/api/v1/collection/tasks/${taskCode}/versions/${versionNo}`
    )
}

/**
 * I-08 维护任务存储映射草稿
 */
export function saveStorageMappingDraft(
    taskCode: string,
    data: StorageMappingDraftRequest
): Promise<StorageMapping> | any {
    return http.post<StorageMapping>(
        `/api/v1/collection/tasks/${taskCode}/storage-mapping`,
        data
    )
}

/**
 * I-09 确认任务存储映射
 */
export function confirmStorageMapping(
    taskCode: string,
    data: StorageMappingDraftRequest
): Promise<StorageMapping> | any {
    return http.post<StorageMapping>(
        `/api/v1/collection/tasks/${taskCode}/storage-mapping/confirm`,
        data
    )
}

/**
 * I-10 更新任务启停状态
 */
export function updateTaskStatus(
    taskCode: string,
    data: TaskStatusUpdateRequest
): Promise<void> | any {
    return http.post<void>(
        `/api/v1/collection/tasks/${taskCode}/status`,
        data
    )
}

export function getStorageMapping(physicalSchemaName: string, physicalTableName: string): Promise<StorageMapping> | any {
    return http.get<StorageMapping>(
        '/api/v1/collection/tasks/storage-mapping',
        {
            params: {
                physicalSchemaName,
                physicalTableName
            }
        }
    )
}