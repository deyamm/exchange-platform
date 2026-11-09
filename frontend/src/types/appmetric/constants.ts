import type { SelectOption } from '/@/types/common';
import type { CommonStatus } from '../appscene/constants';
import type { AssetType } from '/@/types/asset';

export type { AssetType } from '/@/types/asset';
export { ASSET_TYPE_OPTIONS, ASSET_TYPE_TEXT } from '/@/types/asset';

/** ALL 仅是页面筛选条件，不能作为 AppMetric.metricCategory 保存。 */
export type MetricCategoryFilter = 'ALL' | 'MARKET' | 'VALUATION' | 'FINANCE' | 'GROWTH' | 'RISK' | 'OTHER';
export type MetricCategory = Exclude<MetricCategoryFilter, 'ALL'>;

/** 固定业务分类：本期不建设分类表、树或 CRUD。 */
export const METRIC_CATEGORY_OPTIONS: SelectOption<MetricCategory>[] = [
    { label: '行情', value: 'MARKET' }, { label: '估值', value: 'VALUATION' },
    { label: '财务', value: 'FINANCE' }, { label: '成长', value: 'GROWTH' },
    { label: '风险', value: 'RISK' }, { label: '其他', value: 'OTHER' },
];
export const METRIC_CATEGORY_TEXT: Record<MetricCategoryFilter, string> = {
    ALL: '全部', MARKET: '行情', VALUATION: '估值', FINANCE: '财务',
    GROWTH: '成长', RISK: '风险', OTHER: '其他',
};

export interface AppMetricQueryParams { 
    keyword?: string; 
    metricCategory?: string; 
    targetType?: AssetType | '';
    status?: CommonStatus | ''; 
    pageNo?: number; 
    pageSize?: number; 
}

export interface AppMetricSaveRequest {
    metricCode: string;
    metricName: string;
    metricCategory: string;
    targetType: AssetType;
    datasetCode: string;
    fieldCode: string;
    unit?: string;
    description?: string;
}
