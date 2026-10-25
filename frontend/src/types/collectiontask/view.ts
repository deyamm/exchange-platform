import type { SyncStatus, ConfigStatus, MappingStatus, CommonStatus } from './constants'

/** I-01 查询表单状态 */
export interface TaskQueryForm {
    taskCode: string
    taskName: string
    dataTopicCode: string
    dataTypeCode: string
    configStatus: ConfigStatus | ''
    status: CommonStatus | ''
}

export interface StorageMappingForm {
    storageMappingCode: string
    taskConfigCode: string
    dataSourceCode: string
    physicalSchemaName: string
    physicalTableName: string
    writeStrategy: string
    mappingStatus: MappingStatus
    confirmRemark: string
    columns: ColumnMappingForm[]
}

export interface ColumnMappingForm {
    // 数据类型字段
    dataTypeFieldCode: string
    dataTypeFieldName?: string
    dataTypeFieldType: string
    required?: boolean
    uniqueKey?: boolean
    // 采集任务信息
    sourceFieldCode: string
    sourceFieldName?: string
    sourceFieldType: string
    // 物理表字段信息
    physicalColumnName: string
    physicalColumnBaseType: string
    physicalColumnTypeLength?: string
    physicalColumnType: string
    defaultValue?: string
}

export interface ParamField {
    fieldCode: string
    fieldName: string
    fieldType: string
    required: boolean
    uniqueKey?: boolean
    fieldDesc?: string
}

// ── 状态文本映射 ──────────────────────────────────────────────

export const SYNC_STATUS_TEXT: Record<SyncStatus, string> = {
    SYNCED:  '已同步',
    FAILED:  '同步失败',
    PENDING: '同步中'
}

export const SYNC_STATUS_TAG_TYPE: Record<SyncStatus, string> = {
    SYNCED:  'success',
    FAILED:  'danger',
    PENDING: 'warning'
}

export const CONFIG_STATUS_TEXT: Record<ConfigStatus, string> = {
    DRAFT:     '草稿',
    CONFIRMED: '已确认',
    ENABLED:   '已启用',
    DISABLED:  '已停用',
    EXPIRED:   '已失效'
}

export const CONFIG_STATUS_TAG_TYPE: Record<ConfigStatus, string> = {
    DRAFT:     'info',
    CONFIRMED: 'success',
    ENABLED:   'primary',
    DISABLED:  'warning',
    EXPIRED:   'danger'
}

export const MAPPING_STATUS_TEXT: Record<MappingStatus, string> = {
    DRAFT:     '草稿',
    CONFIRMED: '已确认',
    ENABLED:   '已启用',
    DISABLED:  '已停用'
}

export const MAPPING_STATUS_TAG_TYPE: Record<MappingStatus, string> = {
    DRAFT:     'info',
    CONFIRMED: 'success',
    ENABLED:   'primary',
    DISABLED:  'warning'
}

export const COMMON_STATUS_TEXT: Record<string, string> = {
    ENABLED:  '启用',
    DISABLED: '停用',
    OFFLINE:  '下线'
}

export const COMMON_STATUS_TAG_TYPE: Record<string, string> = {
    ENABLED:  'success',
    DISABLED: 'info',
    OFFLINE:  'warning'
}

