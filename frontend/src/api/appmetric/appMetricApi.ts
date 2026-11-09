import http from '/@/utils/http';
import type { PageResult } from '/@/types/common';
import type { AppMetricQueryParams, AppMetricSaveRequest } from '/@/types/appmetric/constants';
import type { AppMetric } from '/@/types/appmetric/entity';
import type { AssetType } from '/@/types/appmetric/constants';

/** 选择数据集时需要的最小目录信息；状态数值沿用 DatasetStatus（1 为 ENABLED）。 */
export interface DatasetOption { datasetCode: string; datasetName: string; datasetStatus: number | string; assetType?: AssetType; assetTypeName?: string; }
/** Dataset 当前 DataTypeVersion 快照中的标准字段，不是物理存储列。 */
export interface DatasetFieldOption { fieldCode: string; fieldName: string; fieldType: string; required?: boolean; sortNo?: number; }

export function queryAppMetricPage(params: AppMetricQueryParams): Promise<PageResult<AppMetric>> | any{
    return http.get<PageResult<AppMetric>>(
        '/api/v1/analytics/metrics', 
        { params }
    );
}

export function saveOrUpdateAppMetric(data: AppMetricSaveRequest): Promise<AppMetric> | any {
    return http.post<AppMetric>(
        '/api/v1/analytics/metrics', 
        data
    );
}

export function deleteAppMetric(code: string): Promise<void> | any {
    return http.delete<void>(
        `/api/v1/analytics/metrics/${code}`
    );
}

/** 独立启停接口：停用由后端在事务中校验业务引用。 */
export function updateAppMetricStatus(code: string, status: 'ENABLED' | 'DISABLED'): Promise<void> | any {
    return http.post<void>(`/api/v1/analytics/metrics/${code}/status`, { status });
}
/** 复用 Dataset 目录接口，仅查询可用于新绑定的已启用数据集。 */
export function queryEnabledDatasets(assetType: AssetType): Promise<PageResult<DatasetOption>> | any {
    return http.get<PageResult<DatasetOption>>('/api/v1/collection/monitor/datasets', { params: { assetType, datasetStatus: 1, pageNo: 1, pageSize: 200 } });
}
/** 编辑时回显已停用/归档的原绑定 Dataset，不能因默认选项过滤而丢失值。 */
export function getDatasetOption(datasetCode: string): Promise<{ dataset: DatasetOption }> | any {
    return http.get<{ dataset: DatasetOption }>(`/api/v1/collection/monitor/datasets/${datasetCode}`);
}
/** 从 Dataset 当前 DataTypeVersion 读取标准字段快照。 */
export function queryDatasetFields(datasetCode: string): Promise<DatasetFieldOption[]> | any {
    return http.get<DatasetFieldOption[]>(`/api/v1/collection/monitor/datasets/${datasetCode}/structure`);
}
