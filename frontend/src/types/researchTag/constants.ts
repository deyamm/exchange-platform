import { CommonStatus } from '/@/types/appscene/constants'

export interface ResearchTagQueryParams {
    tagCode?: string;
    tagName?: string;
    tagCategory?: string;
    status?: CommonStatus | '';
    pageNo?: number;
    pageSize?: number;
}

export interface ResearchTagSaveRequest {
    tagCode: string;
    tagName: string;
    tagCategory?: string;
    status: CommonStatus;
    description?: string;
}