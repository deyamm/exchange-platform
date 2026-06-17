import http from '/@/utils/http';
import type { PageResult } from '/@/types/common';
import type { DataTopicQueryParams, DataTopicSaveRequest } from '/@/types/datatopic/constants';
import type { DataTopic } from '/@/types/datatopic/entity';

export function queryDataTopicTree(params: DataTopicQueryParams): Promise<DataTopic[]> | any {
    return http.get<DataTopic[]>('/api/v1/collection/data-topics', {
        params: {
            ...params,
            tree: true
        }
    });
}

export function queryDataTopicPage(params: DataTopicQueryParams): Promise<PageResult<DataTopic>> | any {
    return http.get<PageResult<DataTopic>>('/api/v1/collection/data-topics', {
        params: {
            ...params,
            tree: false
        }
    });
}

export function saveDataTopic(data: DataTopicSaveRequest) {
    return http.post<DataTopic>('/api/v1/collection/data-topics', data);
}

export function deleteDataTopic(dataTopicCode: string) {
    return http.delete<void>('/api/v1/collection/data-topics', {
        params: {
            dataTopicCode
        }
    });
}