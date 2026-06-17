import type { CommonStatus, DataTypeNodeType, PublishStatus, FieldType } from './constants'
import type { SelectOption } from '/@/types/common'


/**
 * OPTIONS用于下拉框，TEXT用于显示文本，TAG_TYPE用于标签颜色
 */
export const COMMON_STATUS_OPTIONS: SelectOption<CommonStatus>[] = [
    { label: '启用', value: 'ENABLED' },
    { label: '停用', value: 'DISABLED' },
    { label: '下线', value: 'OFFLINE' }
]

export const COMMON_STATUS_TEXT: Record<CommonStatus, string> = {
    ENABLED: '启用',
    DISABLED: '停用',
    OFFLINE: '下线'
}

export const COMMON_STATUS_TAG_TYPE: Record<CommonStatus, 'success' | 'info' | 'warning'> = {
    ENABLED: 'success',
    DISABLED: 'info',
    OFFLINE: 'warning'
}

export const DATA_TYPE_NODE_TYPE_OPTIONS: SelectOption<DataTypeNodeType>[] = [
    { label: '分类节点', value: 'CATEGORY' },
    { label: '具体节点', value: 'CONCRETE' }
]

export const DATA_TYPE_NODE_TYPE_TEXT: Record<DataTypeNodeType, string> = {
    CATEGORY: '分类节点',
    CONCRETE: '具体节点'
}

export const FIELD_TYPE_OPTIONS: SelectOption<FieldType>[] = [
    { label: '字符串', value: 'STRING' },
    { label: '长文本', value: 'TEXT' },
    { label: '整数', value: 'INTEGER' },
    { label: '小数', value: 'DECIMAL' },
    { label: '日期', value: 'DATE' },
    { label: '日期时间', value: 'DATETIME' },
    { label: '布尔', value: 'BOOLEAN' },
    { label: '枚举', value: 'ENUM' }
]

export const FIELD_TYPE_TEXT: Record<FieldType, string> = {
    STRING: '字符串',
    TEXT: '长文本',
    INTEGER: '整数',
    DECIMAL: '小数',
    DATE: '日期',
    DATETIME: '日期时间',
    BOOLEAN: '布尔',
    ENUM: '枚举'
}

export const PUBLISH_STATUS_OPTIONS: SelectOption<PublishStatus>[] = [
    { label: '草稿', value: 'DRAFT' },
    { label: '已发布', value: 'PUBLISHED' },
    { label: '已下线', value: 'OFFLINE' }
]

export const PUBLISH_STATUS_TEXT: Record<PublishStatus, string> = {
    DRAFT: '草稿',
    PUBLISHED: '已发布',
    OFFLINE: '已下线'
}

export const PUBLISH_STATUS_TAG_TYPE: Record<PublishStatus, 'success' | 'info' | 'warning'> = {
    DRAFT: 'info',
    PUBLISHED: 'success',
    OFFLINE: 'warning'
}

export interface QueryForm {
    typeNameForTree: string
    parentCode: string
    dataTypeName: string
    dataTypeLabel: string
}

export interface DataTypeFormModel {
    dataTypeCode: string
    dataTypeName: string
    dataTypeLabel: string
    parentCode: string
    nodeType: DataTypeNodeType
    sortNo: number
    status: CommonStatus
    description: string
}