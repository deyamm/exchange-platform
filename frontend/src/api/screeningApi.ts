import http from '/@/utils/http';
import type { TargetType } from '/@/types/targetScope';
import type { ScreeningQueryRequest, ScreeningResult, ScreeningScopeOptions, ScreeningSnapshot } from '/@/types/screening';

/** 条件筛选专用入口：范围解析、统一指标取数与表达式计算均在分析服务完成。 */
export function queryScreeningResult(data: ScreeningQueryRequest): Promise<ScreeningResult> | any {
    return http.post<ScreeningResult>('/api/v1/analytics/screening/query', data);
}

/** 主页面将此结果传给公共 TargetScopeSelector / TargetScopeSummary，不在页面写死板块或指数枚举。 */
export function queryScreeningScopeOptions(targetType: TargetType): Promise<ScreeningScopeOptions> | any {
    return http.get<ScreeningScopeOptions>('/api/v1/analytics/screening/scope-options', { params: { targetType } });
}

/** 保存的是当前已执行的结果快照，服务端回看接口不会把它替换为今天的最新指标。 */
export function saveScreeningResult(data: { request: ScreeningQueryRequest; result: ScreeningResult }): Promise<ScreeningSnapshot> | any {
    return http.post<ScreeningSnapshot>('/api/v1/analytics/screening/results', data);
}

/** 结果回看直接读取快照，不调用筛选执行接口，确保历史值不被当前数据覆盖。 */
export function getScreeningResultSnapshot(resultId: string): Promise<ScreeningSnapshot> | any {
    return http.get<ScreeningSnapshot>(`/api/v1/analytics/screening/results/${resultId}`);
}
