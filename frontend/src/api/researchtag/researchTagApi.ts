import http from '/@/utils/http';
import type { ResearchTagQueryParams, ResearchTagSaveRequest } from '/@/types/researchTag/constants';
import type { ResearchTag } from '/@/types/researchTag/entity';
import { PageResult } from '/@/types/common';

export function queryResearchTagPage(params: ResearchTagQueryParams): Promise<PageResult<ResearchTag>> | any {
    return http.get<PageResult<ResearchTag>>(
        '/api/v1/analytics/research-tags',
        { params }
    );
}

export function saveOrUpdateResearchTag(data: ResearchTagSaveRequest): Promise<ResearchTag> | any{
    return http.post<ResearchTag>(
        '/api/v1/analytics/research-tags', 
        data
    );
}

export function deleteResearchTag(code: string): Promise<void> | any{
    return http.delete<void>(
        `/api/v1/analytics/research-tags/${code}`
    );
}
