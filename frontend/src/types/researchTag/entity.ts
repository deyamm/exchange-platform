import type { CommonStatus } from '/@/types/appscene/constants';

export interface ResearchTag {
    tagCode: string;
    tagName: string;
    tagCategory?: string;
    status: CommonStatus;
    description?: string;
    createdAt?: string;
    updatedAt?: string;
}

export function createDefaultResearchTag(): ResearchTag {
    return {
        tagCode: '',
        tagName: '',
        tagCategory: '',
        status: 'ENABLED',
        description: '',
    };
}
