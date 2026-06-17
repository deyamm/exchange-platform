import http from '/@/utils/http';
import type { PageResult } from '/@/types/common';
import type { DataType, DataTypeDetail, DataTypeField, DataTypeVersion } from '/@/types/datatype/entity';
import { DataTypeQueryParams, DataTypeSaveRequest } from '/@/types/datatype/constants';


export function queryDataTypeTree(params: DataTypeQueryParams): Promise<DataType[]> | any {
    return http.get<DataType[]>(
        '/api/v1/collection/data-types', { 
            params: {
                ...params,
                tree: true // 强制后端返回树形结构
            } 
        } 
    );
}

export function queryDataTypePage(params: DataTypeQueryParams): Promise<PageResult<DataType>> | any {
    return http.get<PageResult<DataType>>(
        '/api/v1/collection/data-types', { 
            params: {
                ...params,
                tree: false // 强制后端返回分页列表
            } 
        } 
    );
}

export function getDataTypeDetail(dataTypeCode: string): Promise<DataTypeDetail> | any {
    return http.get<DataTypeDetail>(
        `/api/v1/collection/data-types/${dataTypeCode}`
    );
}

export function saveDataType(data: DataTypeSaveRequest): Promise<DataType> | any {
    return http.post<DataType>(
        '/api/v1/collection/data-types', 
        data
    );
}

export function deleteDataType(dataTypeCode: string): Promise<void> | any {
    return http.delete<void>(
        `/api/v1/collection/data-types/${dataTypeCode}`
    );
}

export function queryDataTypeFields(dataTypeCode: string): Promise<DataTypeField[]> | any {
    return http.get<DataTypeField[]>(
        `/api/v1/collection/data-types/${dataTypeCode}/fields`
    );
}

export function saveDataTypeFields(dataTypeCode: string, fields: DataTypeField[]): Promise<DataTypeField[]> | any {
    return http.post<DataTypeField[]>(
        `/api/v1/collection/data-types/${dataTypeCode}/fields`, 
        {
            fields
        }
    );
}

export function publishDataTypeVersion(
    dataTypeCode: string,
    data: {
        version: number;
        versionName?: string;
        changeSummary?: string;
    }
): Promise<DataTypeVersion> | any {
    return http.post<DataTypeVersion>(
        `/api/v1/collection/data-types/${dataTypeCode}/versions`, 
        data
    );
}

export function queryDataTypeVersions(dataTypeCode: string): Promise<DataTypeVersion[]> | any {
    return http.get<DataTypeVersion[]>(
        `/api/v1/collection/data-types/${dataTypeCode}/versions`
    );
}
