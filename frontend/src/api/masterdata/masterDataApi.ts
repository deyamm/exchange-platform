import http from '/@/utils/http'
import type { PageResult } from '/@/types/common'
import type {
    MasterDataAsset, SyncBatch, SyncBatchQuery,
    MasterDataTypeDefinition, SourceBinding, SourceFieldMapping, TriggerType, MarketAssetType,
    SyncType, MasterDataTypeCode, PublishedMasterDataQuery,
    PublishedMasterDataType, PublishedMasterDataTypeDetail,
} from '/@/types/masterdata'

const BASE_URL = '/api/v1/analytics/master-data'


/** 仅查询服务端已发布的主数据；筛选字段使用对应 typeCode 的逻辑字段名。 */
export function queryPublishedMasterData(params: PublishedMasterDataQuery): Promise<PageResult<Record<string, any>>> | any {
    return http.post<PageResult<Record<string, any>>>(`${BASE_URL}/search`, params)
}

/** 关注标的选择器的兼容包装；其他资产类型尚无已发布的主数据类型时返回空结果。 */
export async function searchMasterDataAssets(keyword: string, marketAssetType?: MarketAssetType, limit = 10): Promise<MasterDataAsset[]> {
    const typeCode = marketAssetType === 'INDEX' ? 'MARKET_ASSET_INDEX'
        : marketAssetType === 'STOCK' ? 'MARKET_ASSET_STOCK' : undefined
    if (!typeCode) return []
    const result = await queryPublishedMasterData({
        typeCode,
        keywords: keyword,
        keywordFields: ['assetCode', 'assetName'],
        orderBy: 'assetCode',
        ascending: true,
        pageNo: 1,
        pageSize: limit,
    })
    return result.records as MasterDataAsset[]
}

export function querySyncBatches(params: SyncBatchQuery): Promise<PageResult<SyncBatch>> | any {
    return http.get<PageResult<SyncBatch>>(`${BASE_URL}/sync-batches`, { params })
}

export function republishSyncBatch(batchId: number): Promise<SyncBatch> | any {
    return http.post<SyncBatch>(`${BASE_URL}/sync-batches/${batchId}/republish`)
}

export function queryMasterDataTypes(): Promise<MasterDataTypeDefinition[]> | any {
    return http.get<MasterDataTypeDefinition[]>(`${BASE_URL}/registry/types`)
}

/** 查询已成功发布的主数据类型；浏览器不直接读取 Dataset 或物理表。 */
export function queryPublishedMasterDataTypes(keyword?: string): Promise<PublishedMasterDataType[]> | any {
    return http.get<PublishedMasterDataType[]>(`${BASE_URL}/published-types`, { params: { keyword } })
}

export function getPublishedMasterDataType(typeCode: MasterDataTypeCode): Promise<PublishedMasterDataTypeDetail> | any {
    return http.get<PublishedMasterDataTypeDetail>(`${BASE_URL}/published-types/${typeCode}`)
}

export function queryPublishedMasterDataRows(typeCode: MasterDataTypeCode,
    params: Omit<PublishedMasterDataQuery, 'typeCode'>): Promise<PageResult<Record<string, any>>> | any {
    return queryPublishedMasterData({ typeCode, ...params })
}

export function querySourceBindings(params: { keyword?: string; datasetCode?: string; masterDataType?: string; triggerMode?: string; enabled?: boolean; readiness?: string; pageNo: number; pageSize: number }): Promise<PageResult<SourceBinding>> | any {
    return http.get<PageResult<SourceBinding>>(`${BASE_URL}/sources`, { params })
}

export function getSourceBinding(id: number): Promise<SourceBinding> | any {
    return http.get<SourceBinding>(`${BASE_URL}/sources/${id}`)
}

export function saveSourceBinding(payload: { id?: number; bindingCode: string; bindingName: string; masterDataType: string; datasetCode: string; triggerMode: TriggerType; enabled: boolean; description?: string }): Promise<SourceBinding> | any {
    const { id, ...body } = payload
    return http.post<SourceBinding>(`${BASE_URL}/sources`, body, { params: id ? { id } : undefined })
}

export function querySourceMappings(id: number): Promise<SourceFieldMapping[]> | any {
    return http.get<SourceFieldMapping[]>(`${BASE_URL}/sources/${id}/mappings`)
}

export function saveSourceMappings(id: number, mappings: SourceFieldMapping[]): Promise<SourceFieldMapping[]> | any {
    return http.put<SourceFieldMapping[]>(`${BASE_URL}/sources/${id}/mappings`, { mappings })
}

export function querySourceReadiness(id: number): Promise<{ status: string; errors: string[] }> | any {
    return http.get<{ status: string; errors: string[] }>(`${BASE_URL}/sources/${id}/readiness`)
}

export function setSourceBindingEnabled(id: number, enabled: boolean): Promise<boolean> | any {
    return http.put<boolean>(`${BASE_URL}/sources/${id}/enabled`, null, { params: { enabled } })
}

export function publishSourceBinding(id: number, payload: { taskRunId?: string; syncType: SyncType }): Promise<SyncBatch> | any {
    return http.post<SyncBatch>(`${BASE_URL}/sources/${id}/publish`, payload)
}
