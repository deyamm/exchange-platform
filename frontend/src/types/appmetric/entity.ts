import { CommonStatus } from "../appscene/constants";

export interface AppMetric {
    metricCode: string;
    metricName: string;
    metricCategory?: string;
    unit?: string;
    displayFormat?: string;
    dataSourceCode?: string;
    calcPeriod?: string;
    scenarioCodes?: string[];
    status: CommonStatus;
    caliberDesc?: string;
    description?: string;
    createdAt?: string;
    updatedAt?: string;
}

export function createDefaultAppMetric(): AppMetric {
    return {
        metricCode: '',
        metricName: '',
        metricCategory: 'MARKET',
        unit: '',
        displayFormat: 'NUMBER_2',
        dataSourceCode: '',
        calcPeriod: 'DAILY',
        scenarioCodes: [],
        status: 'ENABLED',
        caliberDesc: '',
        description: ''
    };
}
