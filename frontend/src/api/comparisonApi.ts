import http from '/@/utils/http';
import type { ComparisonQueryRequest, ComparisonResult, ComparisonSnapshot } from '/@/types/comparison';

export function queryComparison(data: ComparisonQueryRequest): Promise<ComparisonResult> | any { return http.post<ComparisonResult>('/api/v1/analytics/comparison/query', data); }
export function saveComparisonResult(data: { request: ComparisonQueryRequest; result: ComparisonResult }): Promise<ComparisonSnapshot> | any { return http.post<ComparisonSnapshot>('/api/v1/analytics/comparison/results', data); }
export function getComparisonResult(resultId: string): Promise<ComparisonSnapshot> | any { return http.get<ComparisonSnapshot>(`/api/v1/analytics/comparison/results/${resultId}`); }
