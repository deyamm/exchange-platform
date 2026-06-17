import type { CommonStatus } from '/@/types/appscene/constants';

export interface ViewConfig {
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
    createdAt?: string;
    updatedAt?: string;
}

export function createDefaultViewConfig(): ViewConfig {
    return {
        viewCode: '',
        viewName: '',
        sceneCode: '',
        viewType: 'DASHBOARD',
        defaultFlag: false,
        fields: [],
        sortRules: [],
        filterConfig: {},
        status: 'DRAFT',
        description: ''
    };
}
