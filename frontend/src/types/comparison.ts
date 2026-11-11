import type { TargetType } from './targetScope';

export interface ComparisonQueryRequest { targetType: TargetType; targetCodes: string[]; metricCodes: string[]; asOfTime: string | null; }
export interface ComparisonTarget { targetCode: string; targetName: string; }
export interface ComparisonMetricRow { metricCode: string; metricName: string; unit?: string; values: Record<string, unknown | null>; dataTimes?: Record<string, string | null>; }
export interface ComparisonResult { targetType: TargetType; targets: ComparisonTarget[]; rows: ComparisonMetricRow[]; }
export interface ComparisonSnapshot { resultId: string; executedAt: string; request: ComparisonQueryRequest; result: ComparisonResult; }
