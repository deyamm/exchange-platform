export type CommonStatus = 'DRAFT' | 'ENABLED' | 'DISABLED' | 'DELETED' | 'EXPIRED';

//0=MARKET_OVERVIEW（市场概览）、1=TARGET_RESEARCH（标的研究）、2=FINANCIAL_ANALYSIS（财务分析）、3=ANNOUNCEMENT_VIEW（公告查看）、4=SCREENING_COMPARISON（筛选对比）、5=ALERT_HANDLING（提醒处理）、6=RESEARCH_RECORD（研究记录）、7=TRACE_QUERY（追溯查询）
export type AppSceneType = 'MARKET_OVERVIEW' | 'TARGET_RESEARCH' | 'FINANCIAL_ANALYSIS' | 'ANNOUNCEMENT_VIEW' | 'SCREENING_COMPARISON' | 'ALERT_HANDLING' | 'RESEARCH_RECORD' | 'TRACE_QUERY';


export interface SceneSaveRequest {
    sceneCode: string;
    sceneName: string;
    sceneType: AppSceneType;
    sortNo: number;
    status: CommonStatus;
    description?: string;
}

export interface AppSceneQueryParams {
    sceneName?: string;
    sceneType?: AppSceneType | '';
    status?: CommonStatus | '';
    pageNo?: number;
    pageSize?: number;
}