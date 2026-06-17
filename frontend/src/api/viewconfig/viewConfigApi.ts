import http from '/@/utils/http';
import type { ViewConfigQueryParams, ViewConfigSaveRequest } from '/@/types/viewconfig/constants';
import type { ViewConfig } from '/@/types/viewconfig/entity';
import { PageResult } from '/@/types/common';

export function queryViewConfigPage(params: ViewConfigQueryParams): Promise<PageResult<ViewConfig>> | any {
    return http.get<PageResult<ViewConfig>>(
        '/api/v1/analytics/view-configs',
        { params }
    );
}

export function saveOrUpdateViewConfig(data: ViewConfigSaveRequest): Promise<ViewConfig> | any {
    return http.post<ViewConfig>(
        '/api/v1/analytics/view-configs', 
        data
    );
}

export function deleteViewConfig(code: string): Promise<void> | any {
    return http.delete<void>(
        `/api/v1/analytics/view-configs/${encodeURIComponent(code)}`
    );
}
