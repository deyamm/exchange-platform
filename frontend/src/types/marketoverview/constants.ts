/**
 * A-07 市场概览模块常量与查询参数类型。
 */

import { WatchStatus } from '/@/types/watchtarget/constants';

/** 标的类型。 */
export type TargetType = 'STOCK' | 'INDEX' | 'ETF' | 'FUND' | 'SECTOR' | 'BOND' | 'FUTURE' | 'FOREX' | 'CRYPTO' | string

/** 交易状态。 */
export type MarketStatus = 'PRE_MARKET' | 'TRADING' | 'MIDDAY_BREAK' | 'CLOSED' | 'SUSPENDED' | 'UNKNOWN' | string

/** 估值状态。 */
export type ValuationStatus = 'UNDERVALUED' | 'FAIR' | 'OVERVALUED' | 'UNKNOWN' | string

/** 综合观点。 */
export type OverallView = 'STRONGLY_BULLISH' | 'BULLISH' | 'NEUTRAL' | 'BEARISH' | 'STRONGLY_BEARISH' | string

/** 风险等级。 */
export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'EXTREME' | string

/** 信号强度。 */
export type SignalStrength = 'WEAK' | 'MEDIUM' | 'STRONG' | string

/** 指标状态。 */
export type MetricStatus = 'GOOD' | 'NORMAL' | 'WARNING' | 'DANGER' | string

/** 指标趋势。 */
export type MetricTrend = 'UP' | 'DOWN' | 'FLAT' | string

/** 应用结果类型枚举（result_type），与数据库设计 8.8 保持一致 */
export type ResultType =
    | 'MARKET_OVERVIEW'
    | 'TARGET_DETAIL'
    | 'FINANCIAL_SUMMARY'
    | 'RANKING'
    | 'COMPARISON'
    | 'ALERT';


export type RangeCode = '1M' | '3M' | '6M' | '1Y' | '2Y' | 'CUSTOM'

/** 估值详情页支持的时间范围。 */
export type ValuationTimeRangeCode = '1Y' | '3Y' | '5Y' | '10Y'

/** 估值详情页支持的估值指标。 */
export type ValuationMetricCode = 'PE_TTM' | 'PB' | 'PS_TTM'



/** 市场概览查询参数（I-18） */
export interface MarketOverviewQueryParams {
    tradeDate?: string;
}

/** 自选观察概览查询参数（I-19） */
export interface WatchOverviewQueryParams {
    targetCode?: string;
    targetName?: string;
    groupCode?: string;
    watchStatus?: WatchStatus | number | '';
    pageNo?: number;
    pageSize?: number;
}

export const rangeSizeMap: Record<Exclude<RangeCode, 'CUSTOM'>, number> = {
    '1M': 22,
    '3M': 66,
    '6M': 132,
    '1Y': 250,
    '2Y': 500
}

/** 固定估值期限到历史查询条数的映射。 */
export const valuationRangeSizeMap: Record<ValuationTimeRangeCode, number> = {
    '1Y': 250,
    '3Y': 750,
    '5Y': 1250,
    '10Y': 2500
}

export const rangeOptions: Array<{ label: string; value: RangeCode }> = [
    { label: '1月', value: '1M' },
    { label: '3月', value: '3M' },
    { label: '6月', value: '6M' },
    { label: '1年', value: '1Y' },
    { label: '2年', value: '2Y' },
    { label: '自定义', value: 'CUSTOM' }
]

export type MainIndicatorCode = 'MA' | 'EMA' | 'BOLL' | 'SAR' | 'VWAP' | 'SUPER_TREND'
export type SubIndicatorCode = 'VOL' | 'TURNOVER' | 'TURNOVER_RATE' | 'MACD' | 'KDJ' | 'RSI' | 'CCI' | 'DMI' | 'ATR' | 'OBV'

export const mainIndicatorOptions: Array<{ label: string; value: MainIndicatorCode; tip: string }> = [
    { label: 'MA 均线', value: 'MA', tip: '默认 5、10、20、60、120、250' },
    { label: 'EMA 指数均线', value: 'EMA', tip: '默认 12、26、9' },
    { label: 'BOLL 布林带', value: 'BOLL', tip: '默认中轨 20，边轨 2' },
    { label: 'SAR 抛物线转向', value: 'SAR', tip: '默认 0.02、0.2' },
    { label: 'VWAP 成交量加权均价', value: 'VWAP', tip: '默认 1、5、10、20、60、120' },
    { label: 'SuperTrend 趋势线', value: 'SUPER_TREND', tip: '默认周期 10，乘数 3' }
]

export const subIndicatorOptions: Array<{ label: string; value: SubIndicatorCode; tip: string }> = [
    { label: 'VOL 成交量', value: 'VOL', tip: '成交量柱状图' },
    { label: 'TURNOVER 成交额', value: 'TURNOVER', tip: '成交额柱状图' },
    { label: '换手率', value: 'TURNOVER_RATE', tip: '区间换手率' },
    { label: 'MACD 指数平滑异同平均线', value: 'MACD', tip: '默认 12、26、9' },
    { label: 'KDJ 随机指标', value: 'KDJ', tip: '默认 9、3、3' },
    { label: 'RSI 相对强弱指标', value: 'RSI', tip: '默认 14' },
    { label: 'CCI 顺势指标', value: 'CCI', tip: '默认 14' },
    { label: 'DMI 趋向指标', value: 'DMI', tip: '默认 14' },
    { label: 'ATR 平均真实波幅', value: 'ATR', tip: '默认 14' },
    { label: 'OBV 能量潮', value: 'OBV', tip: '默认 14' }
]