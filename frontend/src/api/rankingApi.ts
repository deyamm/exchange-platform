import http from '/@/utils/http';
import type { RankingQueryRequest, RankingResult, RankingSnapshot } from '/@/types/ranking';

export function queryRanking(data: RankingQueryRequest): Promise<RankingResult> | any { return http.post<RankingResult>('/api/v1/analytics/ranking/query', data); }
export function saveRankingResult(data: { request: RankingQueryRequest; result: RankingResult }): Promise<RankingSnapshot> | any { return http.post<RankingSnapshot>('/api/v1/analytics/ranking/results', data); }
export function getRankingResult(resultId: string): Promise<RankingSnapshot> | any { return http.get<RankingSnapshot>(`/api/v1/analytics/ranking/results/${resultId}`); }
