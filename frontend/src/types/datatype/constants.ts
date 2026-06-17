export type CommonStatus = 'ENABLED' | 'DISABLED' | 'OFFLINE';

export type DataTypeNodeType = 'CATEGORY' | 'CONCRETE';

export type PublishStatus = 'DRAFT' | 'PUBLISHED' | 'OFFLINE';

export type FieldType = 'STRING' | 'TEXT' | 'INTEGER' | 'DECIMAL' | 'DATE' | 'DATETIME' | 'BOOLEAN' | 'ENUM'

/* 用于查询数据类型 */
export interface DataTypeQueryParams {
    parentCode?: string;
    dataTypeName?: string;
    dataTypeLabel?: string;
    nodeType?: DataTypeNodeType | '';
    status?: CommonStatus | '';
    tree?: boolean;
    pageNo?: number;
    pageSize?: number;
}

/* 保存数据类型的请求参数接口*/
export interface DataTypeSaveRequest {
    dataTypeCode: string;
    dataTypeName: string;
    dataTypeLabel?: string;
    parentCode?: string;
    nodeType: DataTypeNodeType;
    sortNo?: number;
    nodeLevel?: number;
    isLeaf?: boolean;
    status: CommonStatus;
    description?: string;
    saveOrUpdate: 'SAVE' | 'UPDATE';
}