import { CommonStatus } from '/@/types/appscene/constants';
import type { AssetType } from '/@/types/asset';

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
    /** 与 targetCode 一起构成具体提醒标的的唯一业务身份。 */
    targetType?: AssetType | '';
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
