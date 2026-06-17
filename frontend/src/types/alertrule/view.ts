import { RuleType } from './constants';

export const RULE_TYPE_OPTIONS = [
    { label: '价格提醒', value: 'PRICE' },
    { label: '指标提醒', value: 'METRIC' },
    { label: '公告提醒', value: 'ANNOUNCEMENT' },
] as const;

export const RULE_TYPE_TEXT: Record<RuleType, string> = { 
    PRICE: '价格提醒', 
    METRIC: '指标提醒', 
    ANNOUNCEMENT: '公告提醒' 
};