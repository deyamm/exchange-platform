import { CommonStatus, DataTypeNodeType, PublishStatus } from './constants';

export interface DataType {
    dataTypeCode: string;
    dataTypeName: string;
    dataTypeLabel: string;
    parentCode?: string | null;
    nodeType: DataTypeNodeType;
    sortNo: number;
    nodeLevel: number;
    isLeaf: boolean;
    status: CommonStatus;
    version: number;
    description?: string | null;
    fieldCount: number;
    versionCount: number;
    createdAt?: string; // ISO date string
    updatedAt?: string; // ISO date string
    children?: DataType[]; // Optional property for nested data types
}

export interface DataTypeField {
    dataTypeCode: string;
    fieldCode: string;
    fieldName: string;
    fieldType: string; 
    defaultValue?: string | null;
    required: boolean;
    uniqueKey: boolean;
    sortNo: number;
    description?: string | null;
    createdAt?: string; // ISO date string
    updatedAt?: string; // ISO date string
}

export interface DataTypeVersion {
    dataTypeCode: string;
    version: number;
    versionName: string;
    fieldSchemaContent: string; // JSON string representing the field schema
    publishStatus: PublishStatus;
    publishTime?: string; // ISO date string
    changeSummary?: string | null;
    createdAt?: string; // ISO date string
    updatedAt?: string; // ISO date string
}

export interface DataTypeDetail {
    dataTypeInfo: DataType;
    fields: DataTypeField[];
    versions: DataTypeVersion[];
}

export function createDefaultDataType(): DataType {
    return {
        dataTypeCode: '',
        dataTypeName: '',
        dataTypeLabel: '',
        parentCode: '',
        nodeType: 'CATEGORY',
        sortNo: 0,
        nodeLevel: 1,
        isLeaf: true,
        status: 'ENABLED',
        version: 0,
        fieldCount: 0,
        versionCount: 0,
    };
}

export function createDefaultDataTypeField(dataTypeCode: string, sortNo: number): DataTypeField {
    return {
        dataTypeCode,
        fieldCode: '',
        fieldName: '',
        fieldType: 'STRING',
        defaultValue: '',
        required: false,
        uniqueKey: false,
        sortNo,
        description: '',
    };
}


