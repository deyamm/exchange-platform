import type { ResultType } from './constants';

/** 应用结果实体，对应后端 AppResultView */
export interface AppResult {
    resultId: string;
    resultType: ResultType | string;
    sceneCode?: string | null;
    targetCode?: string | null;
    metricCode?: string | null;
    relatedObjectType?: string | null;
    relatedObjectId?: string | null;
    resultSummary?: Record<string, unknown> | null;
    sourceDataTime?: string | null;
    createdAt?: string | null;
}

export function createDefaultAppResult(): AppResult {
    return {
        resultId: '',
        resultType: 'MARKET_OVERVIEW',
        sceneCode: null,
        targetCode: null,
        metricCode: null,
        relatedObjectType: null,
        relatedObjectId: null,
        resultSummary: null,
        sourceDataTime: null,
        createdAt: null
    };
}

export interface IndexBasic {
    fullSymbol: string
    name: string
    fullname: string
    market: string
    publisher: string
    indexType: string
    category: string
    baseDate: string
    basePoint: number
    listDate: string
    weightRule: string
    description: string
    expDate: string
}

export interface MatrixRow {
    market: string
    marketName: string
    counts: Record<string, number>
}