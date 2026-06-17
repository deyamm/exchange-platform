import { CommonStatus } from "../appscene/constants";

export type ViewType = 'DASHBOARD' | 'LIST' | 'DETAIL' | 'DEFAULT';

export interface ViewConfigQueryParams {
    viewCode?: string;
    viewName?: string;
    sceneCode?: string;
    viewType?: string;
    status?: CommonStatus | '';
    pageNo?: number;
    pageSize?: number;
}

export interface ViewConfigSaveRequest {
    viewCode: string;
    viewName: string;
    sceneCode?: string;
    viewType?: string;
    defaultFlag?: boolean;
    fields?: unknown;
    sortRules?: unknown;
    filterConfig?: unknown;
    status: CommonStatus;
    description?: string;
}