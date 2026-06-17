import http from '/@/utils/http';
import type { AlertRuleQueryParams, AlertRuleSaveRequest } from '/@/types/alertrule/constants';
import type { AlertRule } from '/@/types/alertrule/entity';
import { CommonStatus } from '/@/types/appscene/constants';
import { PageResult } from '/@/types/common';

export function queryAlertRulePage(params: AlertRuleQueryParams): Promise<PageResult<AlertRule>> | any {
    return http.get<PageResult<AlertRule>>(
        '/api/v1/analytics/alert-rules', 
        { params }
    );
}
export function saveOrUpdateAlertRule(data: AlertRuleSaveRequest): Promise<AlertRule> | any {
    return http.post<AlertRule>(
        '/api/v1/analytics/alert-rules', 
        data
    );
}
export function updateAlertRuleStatus(ruleCode: string, status: CommonStatus): Promise<AlertRule> | any {
    return http.post<AlertRule>(
        `/api/v1/analytics/alert-rules/${ruleCode}/status`, 
        { status }
    );
}
export function deleteAlertRule(code: string): Promise<void> | any {
    return http.delete<void>(
        `/api/v1/analytics/alert-rules/${code}`
    );
}
