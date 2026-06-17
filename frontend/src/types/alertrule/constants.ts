import { CommonStatus } from '/@/types/appscene/constants';

export type RuleType = 'PRICE' | 'METRIC' | 'ANNOUNCEMENT';

export interface AlertRuleQueryParams { 
    ruleCode?: string; 
    ruleName?: string;
    targetCode?: string; 
    ruleType?: RuleType | ''; 
    status?: CommonStatus | ''; 
    pageNo?: number; 
    pageSize?: number; 
}

export interface AlertRuleSaveRequest {
    ruleCode: string;
    ruleName: string;
    ruleType: RuleType;
    targetCode?: string;
    groupCode?: string;
    metricCode?: string;
    condition?: unknown;
    compareOperator?: string;
    thresholdValue?: number;
    noticeChannels?: string[];
    effectiveStartTime?: string;
    effectiveEndTime?: string;
    status: CommonStatus;
    description?: string;
}
