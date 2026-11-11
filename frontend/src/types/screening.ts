import type { TargetScope, TargetScopeOptions, TargetType } from '/@/types/targetScope';

/** 条件组内、组与组之间共用的有限逻辑连接符；本期不构建递归表达式树。 */
export type LogicOperator = 'AND' | 'OR';
export type MetricConditionOperator = 'EQ' | 'NE' | 'GT' | 'GTE' | 'LT' | 'LTE' | 'BETWEEN' | 'IN' | 'NOT_IN' | 'IS_NULL' | 'IS_NOT_NULL';
export type SortDirection = 'ASC' | 'DESC';

export interface MetricCondition {
    metricCode: string;
    operator: MetricConditionOperator;
    /** BETWEEN 为双元素数组，IN/NOT_IN 为数组；其余操作符是单个比较值。 */
    value?: unknown;
}

export interface MetricConditionGroup {
    operator: LogicOperator;
    conditions: MetricCondition[];
}

/** 提交给服务端的筛选协议；不包含 datasetCode、fieldCode 和物理字段。 */
export interface ScreeningQueryRequest {
    targetType: TargetType;
    targetScope: TargetScope;
    asOfTime: string | null;
    conditionGroups: MetricConditionGroup[];
    groupOperator: LogicOperator;
    sortMetricCode?: string;
    sortDirection?: SortDirection;
}

export interface ScreeningResultRow {
    targetCode: string;
    targetName: string;
    metricValues: Record<string, unknown | null>;
}

export interface ScreeningResult {
    total: number;
    rows: ScreeningResultRow[];
}

/** 选择器可直接复用的服务端主数据；当前 targetType 不支持的数组为空。 */
export type ScreeningScopeOptions = TargetScopeOptions;

/** 浏览器本地快照保存的是当时已执行的返回值，回看时不会重新以最新指标计算。 */
export interface SavedScreeningSnapshot {
    id: string;
    executedAt: string;
    request: ScreeningQueryRequest;
    result: ScreeningResult;
}

/** 服务端持久化的结果快照；读取该对象不会重新请求当前最新指标。 */
export interface ScreeningSnapshot extends SavedScreeningSnapshot {}
