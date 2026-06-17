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

export const MYSQL_BASE_TYPE_OPTIONS_BY_FIELD_TYPE: Record<string, any[]> = {
    STRING: [
        { label: 'VARCHAR（变长字符串）', value: 'VARCHAR' },
        { label: 'CHAR（固定长度字符串）', value: 'CHAR' }
    ],
    TEXT: [
        { label: 'TEXT（长文本，约 64KB）', value: 'TEXT' },
        { label: 'MEDIUMTEXT（较大文本，约 16MB）', value: 'MEDIUMTEXT' },
        { label: 'LONGTEXT（超大文本）', value: 'LONGTEXT' }
    ],
    INTEGER: [
        { label: 'INT（常规整数）', value: 'INT' },
        { label: 'BIGINT（大整数、ID、计数）', value: 'BIGINT' },
        { label: 'SMALLINT（小范围整数）', value: 'SMALLINT' },
        { label: 'TINYINT（很小范围整数）', value: 'TINYINT' }
    ],
    DECIMAL: [
        { label: 'DECIMAL（精确小数）', value: 'DECIMAL' }
    ],
    DATE: [
        { label: 'DATE（日期）', value: 'DATE' }
    ],
    DATETIME: [
        { label: 'DATETIME（日期时间）', value: 'DATETIME' },
        { label: 'TIMESTAMP（时间戳）', value: 'TIMESTAMP' }
    ],
    BOOLEAN: [
        { label: 'TINYINT（布尔推荐）', value: 'TINYINT' },
        { label: 'BOOLEAN（MySQL 中等价于 TINYINT(1)）', value: 'BOOLEAN' }
    ],
    ENUM: [
        { label: 'VARCHAR（枚举编码，推荐）', value: 'VARCHAR' },
        { label: 'ENUM（需配置枚举值）', value: 'ENUM' }
    ]
}

export const MYSQL_LENGTH_OPTIONS_BY_BASE_TYPE: Record<string, any[]> = {
    VARCHAR: [
        { label: '255（通用字符串）', value: '255' },
        { label: '128（名称、短文本）', value: '128' },
        { label: '64（编码、状态值、枚举值）', value: '64' },
        { label: '32（短编码）', value: '32' },
        { label: '512（较长字符串）', value: '512' },
        { label: '1024（URL、较长描述）', value: '1024' }
    ],
    CHAR: [
        { label: '32（固定长度编码）', value: '32' },
        { label: '36（UUID）', value: '36' },
        { label: '64（固定长度编码）', value: '64' }
    ],
    DECIMAL: [
        { label: '18,6（通用小数）', value: '18,6' },
        { label: '18,2（常用金额）', value: '18,2' },
        { label: '20,2（大金额）', value: '20,2' },
        { label: '20,4（金额含更多小数位）', value: '20,4' },
        { label: '10,6（比例/比率）', value: '10,6' },
        { label: '8,4（百分比或较短比例）', value: '8,4' },
        { label: '20,8（高精度小数）', value: '20,8' }
    ],
    TINYINT: [
        { label: '1（布尔值）', value: '1' }
    ],
    ENUM: [
        { label: "'A','B'（示例，按实际枚举值调整）", value: "'A','B'" }
    ]
}