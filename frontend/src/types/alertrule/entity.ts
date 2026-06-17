import type { RuleType } from './constants';
import type { CommonStatus } from '/@/types/appscene/constants';

export interface AlertRule {
    ruleCode: string;
    ruleName: string;
    ruleType: RuleType;
    targetCode?: string;
    groupCode?: string;
    metricCode?: string;
    condition?: unknown;
    noticeChannels?: string[];
    effectiveStartTime?: string;
    effectiveEndTime?: string;
    status: CommonStatus;
    description?: string;
    createdAt?: string;
    updatedAt?: string;
}

export function createDefaultAlertRule(): AlertRule {
    return { 
        ruleCode: '', 
        ruleName: '', 
        ruleType: 'PRICE', 
        targetCode: '', 
        groupCode: '', 
        metricCode: '', 
        condition: {}, 
        noticeChannels: ['IN_APP'], 
        status: 'ENABLED', 
        description: '' 
    };
}
