import { DataTopicStatus } from "./constants";


export interface DataTopic {
    dataTopicCode: string;
    dataTopicName: string;
    dataTopicLabel: string;
    parentCode?: string | null;
    nodeLevel: number;
    isLeaf: boolean;
    sortNo: number;
    status: DataTopicStatus;
    description?: string | null;
    createdAt: string; // ISO date string
    updatedAt: string; // ISO date string
    children?: DataTopic[]; // Optional property for nested data topics
}

export function createDefaultDataTopic(): DataTopic {
    return {
        dataTopicCode: '',
        dataTopicName: '',
        dataTopicLabel: '',
        parentCode: null,
        nodeLevel: 0,
        isLeaf: false,
        sortNo: 0,
        status: 'ENABLED',
        description: null,
        createdAt: '', // ISO date string
        updatedAt: '', // ISO date string
    };
}