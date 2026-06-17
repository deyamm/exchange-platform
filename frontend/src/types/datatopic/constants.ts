
export type DataTopicStatus = 'ENABLED' | 'DISABLED';
    
export interface DataTopicQueryParams {
    parentCode?: string;
    dataTopicName?: string;
    dataTopicLabel?: string;
    status?: DataTopicStatus | '';
    tree?: boolean;
    pageNo?: number;
    pageSize?: number;
}

/**
 * 保存数据主题的请求参数接口，需要严格遵守后端接口的验证规则：
 */
export interface DataTopicSaveRequest {
    dataTopicCode: string;
    dataTopicName: string;
    dataTopicLabel: string;
    parentCode?: string;
    nodeLevel?: number;
    isLeaf?: boolean;
    sortNo: number;
    status: DataTopicStatus;
    description?: string;
    saveOrUpdate: 'SAVE' | 'UPDATE';
}



