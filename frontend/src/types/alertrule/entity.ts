import type { RuleType } from './constants';
import type { CommonStatus } from '/@/types/appscene/constants';
import type { AssetType } from '/@/types/asset';

export interface AlertRule {
    ruleCode: string;
    ruleName: string;
    ruleType: RuleType;
    targetCode?: string;
    targetType?: AssetType | '';
    groupCode?: string;
    metricCode?: string;
    operator?: 'GT' | 'GTE' | 'LT' | 'LTE' | 'EQ' | 'NE';
    threshold?: number;
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
        ruleType: 'METRIC',
        targetCode: '', 
        targetType: '',
        groupCode: '', 
        metricCode: '', 
        condition: {},
        operator: 'GTE',
        threshold: undefined,
        noticeChannels: ['IN_APP'], 
        status: 'ENABLED', 
        description: '' 
    };
}
