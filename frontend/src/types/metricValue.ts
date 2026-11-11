import type { TargetType } from '/@/types/targetScope';

/** 统一指标查询：页面只提交标的和 metricCode，绝不提交 Dataset 或字段信息。 */
export interface MetricQueryRequest {
    targetType: TargetType;
    targetCodes: string[];
    metricCodes: string[];
    /** null 或不传代表每个指标的当前最新有效值。 */
    asOfTime?: string | null;
}

/** value 为 null 表示截至时间内缺失或源数据为空，业务层不得将其转换为 0。 */
export interface MetricValue {
    targetType: TargetType;
    targetCode: string;
    metricCode: string;
    value: unknown | null;
    /** 源数据实际时间，各指标间可以不同。 */
    dataTime: string | null;
}
