import type { AssetType } from '/@/types/asset';

/**
 * 标的范围使用与跨服务 AssetType 完全一致的协议值。
 * 当前公共选择器仅提供 STOCK、INDEX 的范围语义，保留其他类型以避免重新定义同义枚举。
 */
export type TargetType = AssetType;

export type StockScopeType = 'ALL' | 'BOARD' | 'WATCH_GROUP';
export type IndexScopeType = 'ALL' | 'INDEX_TYPE' | 'MARKET' | 'WATCH_GROUP';
export type TargetScopeType = StockScopeType | IndexScopeType;

/** 可持久化、可跨页面传递的范围模型；展示名称不写入 scopeValues。 */
export interface TargetScope {
    targetType: TargetType;
    scopeType: TargetScopeType;
    scopeValues: string[];
}

/** 主数据查询结果到选择器的最小展示模型。 */
export interface TargetScopeOption {
    code: string;
    name: string;
}

export interface TargetScopeOptions {
    board: TargetScopeOption[];
    watchGroup: TargetScopeOption[];
    indexType: TargetScopeOption[];
    market: TargetScopeOption[];
}

export const STOCK_SCOPE_TYPE_OPTIONS: Array<{ label: string; value: StockScopeType }> = [
    { label: '全部股票', value: 'ALL' },
    { label: '板块', value: 'BOARD' },
    { label: '关注分组', value: 'WATCH_GROUP' },
];

export const INDEX_SCOPE_TYPE_OPTIONS: Array<{ label: string; value: IndexScopeType }> = [
    { label: '全部指数', value: 'ALL' },
    { label: '指数类型', value: 'INDEX_TYPE' },
    { label: '所属市场', value: 'MARKET' },
    { label: '关注分组', value: 'WATCH_GROUP' },
];

export function scopeTypeOptions(targetType: TargetType) {
    if (targetType === 'STOCK') return STOCK_SCOPE_TYPE_OPTIONS;
    if (targetType === 'INDEX') return INDEX_SCOPE_TYPE_OPTIONS;
    return [];
}

export function isScopeTypeSupported(targetType: TargetType, scopeType: string): scopeType is TargetScopeType {
    return scopeTypeOptions(targetType).some((item) => item.value === scopeType);
}

export function emptyTargetScope(targetType: TargetType): TargetScope {
    return { targetType, scopeType: 'ALL', scopeValues: [] };
}
