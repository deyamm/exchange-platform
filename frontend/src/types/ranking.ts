import type { TargetScope, TargetType } from './targetScope';

export type RankingDirection = 'ASC' | 'DESC';
export interface RankingRule { metricCode: string; sortDirection: RankingDirection; priority: number; }
export interface RankingQueryRequest { targetType: TargetType; targetScope: TargetScope; asOfTime: string | null; rankingRules: RankingRule[]; topN: number; }
export interface RankingResultRow { rank: number; targetCode: string; targetName: string; metricValues: Record<string, unknown | null>; }
export interface RankingResult { total: number; rows: RankingResultRow[]; }
export interface RankingSnapshot { resultId: string; executedAt: string; request: RankingQueryRequest; result: RankingResult; }
