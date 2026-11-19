import http from '/@/utils/http'
import type { PageResult } from '/@/types/common'
import type {
    AssetMember, MasterDataAsset, MasterDataAssetQuery, MasterDataAssetType, SyncBatch, SyncBatchQuery,
    MasterDataTypeDefinition, SourceBinding, SourceFieldMapping, TriggerType, MarketAssetType,
    SyncType,
} from '/@/types/masterdata'

const BASE_URL = '/api/v1/analytics/master-data'

/** 所有页面请求只访问已发布的主数据域接口，不触碰 Dataset 读取能力或物理存储。 */
export function queryMasterDataAssets(params: MasterDataAssetQuery): Promise<PageResult<MasterDataAsset>> | any {
    return http.get<PageResult<MasterDataAsset>>(BASE_URL, { params })
}

/** 通用 Asset 搜索仅返回已发布主数据，绝不复用关注标的业务实体。 */
export function searchMasterDataAssets(keyword: string, marketAssetType?: MarketAssetType, limit = 10): Promise<MasterDataAsset[]> | any {
    return http.get<MasterDataAsset[]>(`${BASE_URL}/search`, { params: { keyword, marketAssetType, limit } })
}

export function getMasterDataAsset(marketAssetType: MasterDataAssetType, assetCode: string): Promise<MasterDataAsset> | any {
    return http.get<MasterDataAsset>(`${BASE_URL}/${marketAssetType}/${encodeURIComponent(assetCode)}`)
}

export function queryIndexMembers(assetCode: string): Promise<AssetMember[]> | any {
    return http.get<AssetMember[]>(`${BASE_URL}/INDEX/${encodeURIComponent(assetCode)}/members`)
}

/** 复用后端已有批量详情端点，为 INDEX 成分表补充股票名称，避免任何本地猜测。 */
export function batchGetMasterDataAssets(assetCodes: string[]): Promise<MasterDataAsset[]> | any {
    return http.post<MasterDataAsset[]>(`${BASE_URL}/batch-detail`, assetCodes.map(assetCode => ({ marketAssetType: 'STOCK', assetCode })))
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
