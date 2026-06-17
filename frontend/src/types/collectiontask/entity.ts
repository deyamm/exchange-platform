import type {
    SyncStatus, ConfigStatus, MappingStatus, CommonStatus
} from './constants'

/** 采集任务模板（列表视图） */
export interface TaskTemplate {
    taskCode: string
    taskName: string
    currentVersionNo?: number
    syncStatus: SyncStatus
    status: CommonStatus
    latestSyncTime?: string
    dataTopicCode?: string
    dataTypeCode?: string
    dataTypeVersionNo?: number
    configStatus?: ConfigStatus
    mappingStatus?: MappingStatus
    createdAt?: string
    updatedAt?: string
}

/** 采集任务模板版本 */
export interface TaskTemplateVersion {
    taskCode: string
    versionNo: number
    taskName?: string
    taskDesc?: string
    handlerName?: string
    dataSource?: string
    assetType?: string
    bizType?: string
    paramsSchemaJson?: string
    outputFieldsJson?: string
    outputFieldsHash?: string
    ruleCategoryCodes?: string
    changeSummary?: string
    syncTime?: string
    createdAt?: string
}

/** 任务配置视图 */
export interface TaskConfig {
    taskConfigCode: string
    taskCode: string
    taskTemplateVersionNo?: number
    dataTopicCode?: string
    dataTypeCode?: string
    dataTypeVersionNo?: number
    storageMappingCode?: string
    ruleCodesJson?: string
    configStatus: ConfigStatus
    status: CommonStatus
    description?: string
    createdAt?: string
    updatedAt?: string
}

/** 字段映射视图 */
export interface StorageColumnMapping {
    sourceFieldCode: string
    sourceFieldName?: string
    sourceFieldType?: string
    dataTypeFieldCode: string
    dataTypeFieldType: string
    physicalColumnName: string
    physicalColumnType?: string
    defaultValue?: string
    required?: boolean
    uniqueKey?: boolean
}

/** 存储映射视图 */
export interface StorageMapping {
    storageMappingCode: string
    taskConfigCode?: string
    dataSourceCode?: string
    dataTypeCode?: string
    dataTypeVersionNo?: number
    physicalSchemaName?: string
    physicalTableName?: string
    writeStrategy?: string
    mappingStatus: MappingStatus
    confirmRemark?: string
    columns?: StorageColumnMapping[]
    createdAt?: string
    updatedAt?: string
}

/** 任务详情视图（聚合对象） */
export interface TaskDetail {
    taskInfo: TaskTemplate
    currentVersion?: TaskTemplateVersion
    config?: TaskConfig
    storageMapping?: StorageMapping
}

/** 工厂函数：创建默认任务配置 */
export function createDefaultTaskConfig(): TaskConfig {
    return {
        taskConfigCode: '',
        taskCode: '',
        configStatus: 'DRAFT',
        status: 'DISABLED',
    }
}
