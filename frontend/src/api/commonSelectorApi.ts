import { queryAppMetricPage } from '/@/api/appmetric/appMetricApi';
import { getIndexBasic } from '/@/api/marketoverview/marketOverviewApi';
import { queryTargetGroupPage } from '/@/api/watchtarget/watchTargetApi';
import type { AppMetric } from '/@/types/appmetric/entity';
import type { MetricCategoryFilter } from '/@/types/appmetric/constants';
import type { PageResult } from '/@/types/common';
import type { IndexBasic } from '/@/types/marketoverview/entity';
import type { TargetGroup } from '/@/types/watchtarget/entity';
import type { TargetScopeOption, TargetScopeOptions, TargetType } from '/@/types/targetScope';

/** MetricSelector 可见的最小指标信息，刻意不暴露 Dataset 或物理来源字段。 */
export interface SelectableMetric {
    metricCode: string;
    metricName: string;
    metricCategory: string;
    valueType?: string;
    /** 单位来自 AppMetric，只作只读展示，筛选页面绝不允许用户改写或换算。 */
    unit?: string;
}

export interface SelectableMetricQuery {
    targetType: TargetType;
    metricCategory?: MetricCategoryFilter;
    keyword?: string;
    allowedValueTypes?: string[];
    pageNo?: number;
    pageSize?: number;
}

/**
 * 复用 AppMetric 的分页能力，并在公共边界剥离数据来源信息。
 * valueType 是当前 Dataset 字段类型的展示快照，现有指标接口尚未将其作为独立查询条件；
 * 因此在当前页结果上作兼容过滤。
 */
export async function querySelectableMetrics(query: SelectableMetricQuery): Promise<PageResult<SelectableMetric>> {
    const page = await queryAppMetricPage({
        targetType: query.targetType,
        metricCategory: query.metricCategory && query.metricCategory !== 'ALL' ? query.metricCategory : '',
        keyword: query.keyword,
        status: 'ENABLED',
        pageNo: query.pageNo || 1,
        pageSize: query.pageSize || 200,
    });
    const allowedValueTypes = new Set(query.allowedValueTypes || []);
    const records = ((page.records || []) as AppMetric[])
        .filter((metric) => !allowedValueTypes.size || Boolean(metric.fieldType)
            && allowedValueTypes.has(metric.fieldType as string))
        .map((metric) => ({
            metricCode: metric.metricCode,
            metricName: metric.metricName,
            metricCategory: metric.metricCategory,
            valueType: metric.fieldType,
            unit: metric.unit,
        }));
    return { ...page, records };
}

/** 选择范围时仅加载启用的关注分组。 */
export async function queryEnabledTargetScopeGroups(): Promise<TargetScopeOption[]> {
    const page = await queryTargetGroupPage({ status: 'ENABLED', pageNo: 1, pageSize: 200 });
    return ((page.records || []) as TargetGroup[]).map((group) => ({
        code: group.groupCode,
        name: group.groupName,
    }));
}

function uniqueOptions(rows: IndexBasic[], key: 'category' | 'market'): TargetScopeOption[] {
    const seen = new Set<string>();
    return rows.reduce<TargetScopeOption[]>((options, row) => {
        const code = row[key]?.trim();
        if (code && !seen.has(code)) {
            seen.add(code);
            // 编码和值来自 index_basic_data 主数据；此处不创建第二套指数类型/市场字典。
            options.push({ code, name: code });
        }
        return options;
    }, []);
}

/** 从已有指数基础主数据动态派生指数类型和所属市场选项。 */
export async function queryIndexTargetScopeOptions(): Promise<Pick<TargetScopeOptions, 'indexType' | 'market'>> {
    const rows = await getIndexBasic({});
    const indexRows = (rows || []) as IndexBasic[];
    return {
        indexType: uniqueOptions(indexRows, 'category'),
        market: uniqueOptions(indexRows, 'market'),
    };
}
