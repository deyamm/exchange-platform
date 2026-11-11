import http from '/@/utils/http';
import type { MetricQueryRequest, MetricValue } from '/@/types/metricValue';

/** 后续筛选、排行、对比页面共用的唯一指标取数入口。 */
export function queryMetricValues(data: MetricQueryRequest): Promise<MetricValue[]> | any {
    return http.post<MetricValue[]>('/api/v1/analytics/metric-values/query', data);
}
