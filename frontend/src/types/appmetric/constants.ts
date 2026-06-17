import { CommonStatus } from "../appscene/constants";


export interface AppMetricQueryParams { 
    keyword?: string; 
    metricCategory?: string; 
    status?: CommonStatus | ''; 
    pageNo?: number; 
    pageSize?: number; 
}

export interface AppMetricSaveRequest {
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
}
