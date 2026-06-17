/**
 * M-01 采集任务管理模块 - 常量与类型定义
 * 与后端枚举值保持一致
 */

import { StorageColumnMapping } from "./entity"

/** 同步状态 */
export type SyncStatus = 'SYNCED' | 'FAILED' | 'PENDING'

/** 任务配置状态 */
export type ConfigStatus = 'DRAFT' | 'CONFIRMED' | 'ENABLED' | 'DISABLED' | 'EXPIRED'

/** 存储映射状态 */
export type MappingStatus = 'DRAFT' | 'CONFIRMED' | 'ENABLED' | 'DISABLED'

/** 启停状态（沿用 common） */
export type CommonStatus = 'ENABLED' | 'DISABLED' | 'OFFLINE'

/** 查询采集任务列表请求参数 */
export interface TaskQueryParams {
    taskCode?: string
    taskName?: string
    pageNo?: number
    pageSize?: number
}

/** 新增或更新采集任务配置请求体 */
export interface TaskConfigSaveRequest {
    taskConfigCode: string
    taskCode: string
    taskTemplateVersionNo: number
    dataTopicCode: string
    dataTypeCode: string
    dataTypeVersionNo: number
    storageMappingCode?: string
    ruleCodes?: string[]
    configStatus: ConfigStatus
    status: CommonStatus
    description?: string
    saveOrUpdate: 'SAVE' | 'UPDATE'
}


/** 采集任务模板同步请求体 */
export interface TaskSyncRequest {
    taskCode?: string
    syncMode: string
}

/** 任务启停状态更新请求体 */
export interface TaskStatusUpdateRequest {
    status: CommonStatus
}

/** 保存或更新存储映射请求体 */
export interface StorageMappingDraftRequest {
    storageMappingCode: string
    taskConfigCode: string
    dataSourceCode: string
    physicalSchemaName: string
    physicalTableName: string
    writeStrategy: string
    mappingStatus: MappingStatus
    confirmRemark?: string
    columns: StorageColumnMapping[]
    saveOrUpdate: 'SAVE' | 'UPDATE'
}

