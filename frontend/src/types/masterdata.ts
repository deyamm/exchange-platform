import type { SelectOption } from '/@/types/common'

/** 本期主数据页面只展示后端已正式发布的股票和指数。 */
export type MarketAssetType = 'STOCK' | 'INDEX' | 'FUND' | 'BOND' | 'FUTURE'
export type MasterDataAssetType = Extract<MarketAssetType, 'STOCK' | 'INDEX'>
export type MasterDataStatus = 'ACTIVE' | 'INACTIVE'
export type SyncBatchStatus = 'CREATED' | 'VALIDATING' | 'PUBLISHED' | 'FAILED'
export type SyncType = 'INIT' | 'FULL' | 'INCREMENTAL'
export type MappingMode = 'FIELD' | 'CONSTANT'
export type TriggerType = 'MANUAL' | 'AUTO' | 'REPUBLISH'
export type ReadinessStatus = 'READY' | 'INVALID' | 'DISABLED'

export interface TargetFieldDefinition {
    fieldCode: string
    fieldName: string
    valueType: string
    required: boolean
    enumType?: string | null
    allowedMappingModes: MappingMode[]
}
export interface MasterDataTypeDefinition {
    typeCode: 'MARKET_ASSET_STOCK' | 'MARKET_ASSET_INDEX' | 'MARKET_ASSET_REL_INDEX_MEMBER'
    displayName: string
    domain: string
    category: string
    fields: TargetFieldDefinition[]
}
export interface SourceFieldMapping {
    id?: number
    targetField: string
    mappingMode: MappingMode
    sourceFieldCode?: string | null
    constantValue?: string | null
}
export interface SourceBinding {
    id: number
    bindingCode: string
    bindingName: string
    masterDataType: MasterDataTypeDefinition['typeCode']
    datasetCode: string
    triggerMode: TriggerType
    enabled: boolean
    description?: string | null
    readiness: ReadinessStatus
    readinessErrors: string[]
    lastBatch?: SyncBatch | null
}

/** 与 Java MarketAssetView 一一对应；时间字段保留 optional，以兼容后端逐步扩展。 */
export interface MasterDataAsset {
    id: number
    marketAssetType: MarketAssetType | null
    assetCode: string
    assetName: string
    status: MasterDataStatus
    marketType?: string | null
    exchangeCode?: string | null
    currencyCode?: string | null
    listDate?: string | null
    delistDate?: string | null
    publisherCode?: string | null
    indexCategory?: string | null
    schemeCode?: string | null
    level?: number | null
    launchDate?: string | null
    endDate?: string | null
    createdAt?: string | null
    updatedAt?: string | null
}

/** 页面仅转发过滤条件，绝不在浏览器中合并或求交集主数据结果。 */
export interface MasterDataAssetQuery {
    marketAssetType?: MasterDataAssetType
    keyword?: string
    status?: MasterDataStatus
    marketType?: string
    exchangeCode?: string
    publisherCode?: string
    indexCategory?: string
    schemeCode?: string
    level?: number
    pageNo: number
    pageSize: number
}

export interface AssetMember {
    parentAssetCode: string
    memberAssetCode: string
    validFrom?: string | null
    validTo?: string | null
    batchId?: number | null
}

/** 成分关系接口只返回证券代码；页面按已存在的 batch-detail 正式 API 补充显示名称。 */
export interface AssetMemberDisplay extends AssetMember {
    memberAssetName?: string
    memberMarketType?: string | null
}

export interface SyncBatch {
    id: number
    assetType?: MasterDataAssetType
    masterDataType?: 'MARKET_ASSET_STOCK' | 'MARKET_ASSET_INDEX' | 'MARKET_ASSET_REL_INDEX_MEMBER' | null
    bindingId?: number | null
    bindingCode?: string | null
    bindingName?: string | null
    triggerType?: TriggerType | null
    sourceCode?: string | null
    taskRunId?: string | null
    datasetCode: string
    syncType: SyncType
    status: SyncBatchStatus
    totalCount: number
    addedCount: number
    updatedCount: number
    removedCount: number
    startedAt?: string | null
    publishedAt?: string | null
    errorMessage?: string | null
    /** 后端将来返回此字段时，页面优先服从它，而不是推导复杂状态机。 */
    republishable?: boolean
}

export interface SyncBatchQuery {
    masterDataType?: string
    sourceCode?: string
    datasetCode?: string
    syncType?: SyncType
    status?: SyncBatchStatus
    startTime?: string
    endTime?: string
    pageNo: number
    pageSize: number
}

export const MASTER_DATA_ASSET_TYPE_OPTIONS: SelectOption<MasterDataAssetType>[] = [
    { label: '股票', value: 'STOCK' },
    { label: '指数', value: 'INDEX' },
]

export const MASTER_DATA_STATUS_OPTIONS: SelectOption<MasterDataStatus>[] = [
    { label: '生效', value: 'ACTIVE' },
    { label: '失效', value: 'INACTIVE' },
]

/** 以下值与后端稳定枚举一致，只承担下拉展示和协议值传递，不在前端解释业务含义。 */
export const STOCK_MARKET_TYPE_OPTIONS: SelectOption[] = [
    { label: 'A 股', value: 'A_SHARE' }, { label: 'B 股', value: 'B_SHARE' },
    { label: 'H 股', value: 'H_SHARE' }, { label: '美股', value: 'US' }, { label: '其他', value: 'OTHER' },
]
export const PUBLISHER_CODE_OPTIONS: SelectOption[] = [
    { label: '中证', value: 'CSI' }, { label: '申万', value: 'SW' }, { label: '同花顺', value: 'THS' },
    { label: '东方财富', value: 'EM' }, { label: '其他', value: 'OTHER' },
]
export const INDEX_CATEGORY_OPTIONS: SelectOption[] = [
    { label: '宽基', value: 'BROAD' }, { label: '行业', value: 'INDUSTRY' }, { label: '概念', value: 'CONCEPT' },
    { label: '主题', value: 'THEME' }, { label: '风格', value: 'STYLE' }, { label: '策略', value: 'STRATEGY' }, { label: '其他', value: 'OTHER' },
]
export const SYNC_TYPE_OPTIONS: SelectOption<SyncType>[] = [
    { label: '初始化', value: 'INIT' }, { label: '全量', value: 'FULL' }, { label: '增量', value: 'INCREMENTAL' },
]
export const SYNC_BATCH_STATUS_OPTIONS: SelectOption<SyncBatchStatus>[] = [
    { label: '已创建', value: 'CREATED' }, { label: '校验中', value: 'VALIDATING' },
    { label: '已发布', value: 'PUBLISHED' }, { label: '失败', value: 'FAILED' },
]


type StatusTagType = 'success' | 'info' | 'warning' | 'danger'

const STATUS_TAG_TYPE_MAP: Record<MasterDataStatus | SyncBatchStatus, StatusTagType> = {
    ACTIVE: 'success',
    INACTIVE: 'info',
    CREATED: 'info',
    VALIDATING: 'warning',
    PUBLISHED: 'success',
    FAILED: 'danger',
}

export const statusTagType = (
    status?: MasterDataStatus | SyncBatchStatus
): StatusTagType => STATUS_TAG_TYPE_MAP[status ?? 'INACTIVE']
