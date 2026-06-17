import http from '/@/utils/http';
import type { PageResult } from '/@/types/common';
import type { AppMetricQueryParams, AppMetricSaveRequest } from '/@/types/appmetric/constants';
import type { AppMetric } from '/@/types/appmetric/entity';

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
