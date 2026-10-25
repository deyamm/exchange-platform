<template>
    <div class="valuation-detail-page">
        <template v-if="valuation || valuationRows.length || valuationDetail.length || channelRows.length || trendRows.length || dividendRows.length">
            <el-row :gutter="12">
                <el-col :xs="24" :sm="12" :lg="6" v-for="item in valuationCards" :key="item.key">
                    <el-card class="metric-card" shadow="never">
                        <div class="metric-label">{{ item.label }}</div>
                        <div class="metric-value">{{ item.value }}</div>
                        <div class="metric-desc">{{ item.desc }}</div>
                    </el-card>
                </el-col>
            </el-row>

            <el-card class="detail-card" shadow="never">
                <template #header>
                    <div class="card-header">
                        <div>
                            <span class="card-title">估值通道</span>
                            <span class="card-tip">收盘价曲线 + 5 条估值通道线，图例展示通道倍数</span>
                        </div>
                        <el-radio-group v-model="channelRange" size="small">
                            <el-radio-button
                                v-for="item in rangeOptions"
                                :key="item.value"
                                :label="item.value"
                            >
                                {{ item.label }}
                            </el-radio-button>
                        </el-radio-group>
                    </div>
                </template>

                <el-tabs v-model="activeChannelMetric" class="metric-tabs">
                    <el-tab-pane
                        v-for="item in metricOptions"
                        :key="item.value"
                        :name="item.value"
                        :label="item.label"
                    />
                </el-tabs>

                <el-alert
                    v-if="!hasBackendChannelRows && channelRowsForDisplay.length"
                    class="data-tip-alert"
                    title="当前通道线由 valuationRows 做兼容展示；正式接入时建议由后端直接返回 channelRows。"
                    type="info"
                    show-icon
                    :closable="false"
                />

                <el-empty v-if="!channelRowsForDisplay.length" description="暂无估值通道数据" />
                <TargetEcharts v-else :option="valuationChannelOption" height="430px" />
            </el-card>

            <el-card class="detail-card" shadow="never">
                <template #header>
                    <div class="card-header">
                        <div>
                            <span class="card-title">估值走势</span>
                            <span class="card-tip">股票估值、行业平均、行业中位数，并叠加 30/70 分位虚线</span>
                        </div>
                        <el-radio-group v-model="trendRange" size="small">
                            <el-radio-button
                                v-for="item in rangeOptions"
                                :key="item.value"
                                :label="item.value"
                            >
                                {{ item.label }}
                            </el-radio-button>
                        </el-radio-group>
                    </div>
                </template>

                <el-tabs v-model="activeTrendMetric" class="metric-tabs">
                    <el-tab-pane
                        v-for="item in metricOptions"
                        :key="item.value"
                        :name="item.value"
                        :label="item.label"
                    />
                </el-tabs>

                <el-row :gutter="12">
                    <el-col :xs="24" :lg="18">
                        <el-empty v-if="!trendRowsForDisplay.length" description="暂无估值走势数据" />
                        <TargetEcharts v-else :option="valuationTrendOption" height="430px" />
                    </el-col>

                    <el-col :xs="24" :lg="6">
                        <div class="valuation-side-panel">
                            <div class="side-title">{{ activeTrendMetricLabel }} 摘要</div>
                            <el-descriptions :column="1" size="small" border>
                                <el-descriptions-item label="股票估值">
                                    {{ formatMultipleValue(trendSnapshot.stockValue) }}
                                </el-descriptions-item>
                                <el-descriptions-item label="当前分位">
                                    {{ formatPercent(trendSnapshot.percentileValue) }}
                                </el-descriptions-item>
                                <el-descriptions-item label="30分位估值">
                                    {{ formatMultipleValue(trendSnapshot.percentile30Value) }}
                                </el-descriptions-item>
                                <el-descriptions-item label="70分位估值">
                                    {{ formatMultipleValue(trendSnapshot.percentile70Value) }}
                                </el-descriptions-item>
                                <el-descriptions-item label="行业平均">
                                    {{ formatMultipleValue(trendSnapshot.industryAverageValue) }}
                                </el-descriptions-item>
                                <el-descriptions-item label="行业中位数">
                                    {{ formatMultipleValue(trendSnapshot.industryMedianValue) }}
                                </el-descriptions-item>
                                <el-descriptions-item label="最新日期">
                                    {{ trendSnapshot.latestTradeDate || '-' }}
                                </el-descriptions-item>
                            </el-descriptions>

                            <div class="percentile-block">
                                <div class="percentile-label">
                                    <span>当前估值分位</span>
                                    <strong>{{ formatPercent(trendSnapshot.percentileValue) }}</strong>
                                </div>
                                <el-progress :percentage="safeProgress(trendSnapshot.percentileValue)" :stroke-width="8" />
                            </div>
                        </div>
                    </el-col>
                </el-row>
            </el-card>

            <el-card class="detail-card" shadow="never">
                <template #header>
                    <div class="card-header">
                        <div>
                            <span class="card-title">分红走势</span>
                            <span class="card-tip">分红 TTM、行业平均分红率、行业中位数分红率</span>
                        </div>
                        <el-radio-group v-model="dividendRange" size="small">
                            <el-radio-button
                                v-for="item in rangeOptions"
                                :key="item.value"
                                :label="item.value"
                            >
                                {{ item.label }}
                            </el-radio-button>
                        </el-radio-group>
                    </div>
                </template>

                <el-empty v-if="!dividendRowsForDisplay.length" description="暂无分红走势数据" />
                <TargetEcharts v-else :option="dividendTrendOption" height="400px" />
            </el-card>
        </template>

        <el-empty v-else description="暂无估值数据" />
    </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import type { EChartsOption } from 'echarts'
import type {
    DividendTrendRow,
    ValuationChannelRow,
    ValuationDailyRow,
    ValuationMetricSummary,
    ValuationTrendRow,
    TargetValuationSummaryDetail,
    TargetValuationChannelDetail,
    TargetValuationTrendDetail,
    TargetDividendTrendDetail
} from '/@/types/targetdetail/targetDetailType'
import type { ValuationMetricCode, ValuationTimeRangeCode } from '/@/types/marketoverview/constants'
import TargetEcharts from '/@/components/common/TargetEcharts.vue'
import { formatAmount, formatNumber, formatPercent } from '/@/utils/marketUtils'

type ValuationDetailPageProps = TargetValuationSummaryDetail
    & Partial<TargetValuationChannelDetail>
    & Partial<TargetValuationTrendDetail>
    & Partial<TargetDividendTrendDetail>

const props = withDefaults(
    defineProps<ValuationDetailPageProps>(), {
        valuationRows: () => [],
        valuationDetail: () => [],
        channelRows: () => [],
        trendRows: () => [],
        dividendRows: () => [],
        meta: () => ({})
})

const metricOptions: Array<{ label: string; value: ValuationMetricCode; field: keyof ValuationDailyRow }> = [
    { label: 'PE TTM', value: 'PE_TTM', field: 'peTtm' },
    { label: 'PB', value: 'PB', field: 'pb' },
    { label: 'PS TTM', value: 'PS_TTM', field: 'psTtm' },
]

const rangeOptions: Array<{ label: string; value: ValuationTimeRangeCode; days: number }> = [
    { label: '1年', value: '1Y', days: 365 },
    { label: '3年', value: '3Y', days: 365 * 3 },
    { label: '5年', value: '5Y', days: 365 * 5 },
    { label: '10年', value: '10Y', days: 365 * 10 }
]

const activeChannelMetric = ref<ValuationMetricCode>('PE_TTM')
const activeTrendMetric = ref<ValuationMetricCode>('PE_TTM')
const channelRange = ref<ValuationTimeRangeCode>('3Y')
const trendRange = ref<ValuationTimeRangeCode>('3Y')
const dividendRange = ref<ValuationTimeRangeCode>('3Y')

const latestRow = computed(() => {
    const rows = sortByTradeDate(props.valuationRows)
    return rows[rows.length - 1]
})

const valuationCards = computed(() => [
    {
        key: 'peTtm',
        label: 'PE TTM',
        value: formatNumber(latestRow.value?.peTtm ?? props.valuation?.peTtm, 2),
        desc: '市盈率 TTM'
    },
    {
        key: 'pb',
        label: 'PB',
        value: formatNumber(latestRow.value?.pb ?? props.valuation?.pb, 2),
        desc: '市净率'
    },
    {
        key: 'psTtm',
        label: 'PS TTM',
        value: formatNumber(latestRow.value?.psTtm ?? props.valuation?.psTtm ?? props.valuation?.ps, 2),
        desc: '市销率 TTM'
    },
    {
        key: 'totalMarketValue',
        label: '总市值',
        value: formatAmount(latestRow.value?.totalMarketValue ?? props.valuation?.totalMarketValue),
        desc: '字段：totalMarketValue'
    }
])

const activeTrendMetricLabel = computed(() => metricLabel(activeTrendMetric.value))

const hasBackendChannelRows = computed(() => Boolean(props.channelRows?.length))

const channelRowsForDisplay = computed(() => {
    const backendRows = sortByTradeDate((props.channelRows || [])
        .filter(row => row.metricCode === activeChannelMetric.value))

    if (backendRows.length) {
        return filterRowsByRange(backendRows, channelRange.value)
    }

    return buildFallbackChannelRows(activeChannelMetric.value, filterRowsByRange(props.valuationRows, channelRange.value))
})

const trendRowsForDisplay = computed(() => {
    const backendRows = sortByTradeDate((props.trendRows || [])
        .filter(row => row.metricCode === activeTrendMetric.value))

    if (backendRows.length) {
        return filterRowsByRange(backendRows, trendRange.value)
    }

    return buildFallbackTrendRows(activeTrendMetric.value, filterRowsByRange(props.valuationRows, trendRange.value))
})

const dividendRowsForDisplay = computed(() => {
    const backendRows = sortByTradeDate(props.dividendRows || [])

    if (backendRows.length) {
        return filterRowsByRange(backendRows, dividendRange.value)
    }

    return buildFallbackDividendRows(filterRowsByRange(props.valuationRows, dividendRange.value))
})

const trendSnapshot = computed(() => {
    const rows = trendRowsForDisplay.value
    const latest = rows[rows.length - 1]
    const stockValues = rows.map(row => normalizeNumber(row.stockValue)).filter(isNumber)
    const stockValue = normalizeNumber(latest?.stockValue)
    const summary = props.valuationDetail?.find((item: ValuationMetricSummary) => item.metricCode === activeTrendMetric.value)

    return {
        stockValue: stockValue ?? summary?.currentValue ?? null,
        percentileValue: normalizeNumber(latest?.percentileValue) ?? normalizeNumber(summary?.percentileValue) ?? calcPercentile(stockValues, stockValue),
        percentile30Value: normalizeNumber(latest?.percentile30Value) ?? normalizeNumber(summary?.percentile30Value) ?? quantile(stockValues, 0.3),
        percentile70Value: normalizeNumber(latest?.percentile70Value) ?? normalizeNumber(summary?.percentile70Value) ?? quantile(stockValues, 0.7),
        industryAverageValue: normalizeNumber(latest?.industryAverageValue) ?? normalizeNumber(summary?.industryAverageValue),
        industryMedianValue: normalizeNumber(latest?.industryMedianValue) ?? normalizeNumber(summary?.industryMedianValue),
        latestTradeDate: latest?.tradeDate || summary?.latestTradeDate || '-'
    }
})

const valuationChannelOption = computed<EChartsOption>(() => {
    const rows = channelRowsForDisplay.value
    const dates = rows.map(row => row.tradeDate)
    const latest = rows[rows.length - 1]

    return {
        tooltip: {
            trigger: 'axis',
            formatter(params: any) {
                const items = Array.isArray(params) ? params : [params]
                const date = items[0]?.axisValue || ''
                const lines = items.map((item: any) => {
                    const raw = item.data || {}
                    if (item.seriesName === '收盘价') {
                        return `${item.marker}${item.seriesName}：${formatNumber(raw.value ?? raw, 2)}`
                    }

                    return `${item.marker}${item.seriesName}：${formatNumber(raw.value, 2)}（${formatMultiple(raw.multiple)}）`
                })
                return [date, ...lines].join('<br/>')
            }
        },
        legend: {
            top: 0,
            left: 8
        },
        grid: {
            left: 58,
            right: 24,
            top: 48,
            bottom: 46
        },
        dataZoom: [
            { type: 'inside' as const, start: 0, end: 100 },
            { type: 'slider' as const, bottom: 8, height: 18, start: 0, end: 100 }
        ],
        xAxis: {
            type: 'category',
            boundaryGap: false,
            data: dates,
            axisLabel: { hideOverlap: true }
        },
        yAxis: {
            type: 'value',
            scale: true,
            axisLabel: {
                formatter(value: number) {
                    return formatNumber(value, 0)
                }
            },
            splitLine: { lineStyle: { type: 'dashed' } }
        },
        series: [
            {
                name: '收盘价',
                type: 'line' as const,
                smooth: true,
                showSymbol: false,
                data: rows.map(row => ({ value: normalizeNumber(row.close) || 0 }))
            },
            ...([1, 2, 3, 4, 5] as const).map(index => ({
                name: formatMultiple(latest?.[`channelMultiple${index}`]),
                type: 'line' as const,
                smooth: true,
                showSymbol: false,
                connectNulls: true,
                data: rows.map(row => ({
                    value: normalizeNumber(row[`channelPrice${index}`]) || 0,
                    multiple: normalizeNumber(row[`channelMultiple${index}`]) || 0
                }))
            }))
        ]
    }
})

const valuationTrendOption = computed<EChartsOption>(() => {
    const rows = trendRowsForDisplay.value
    const dates = rows.map(row => row.tradeDate)

    return {
        tooltip: {
            trigger: 'axis',
            formatter(params: any) {
                const items = Array.isArray(params) ? params : [params]
                const date = items[0]?.axisValue || ''
                const lines = items.map((item: any) => {
                    const value = item.data?.value ?? item.data
                    return `${item.marker}${item.seriesName}：${formatMultipleValue(value)}`
                })
                return [date, ...lines].join('<br/>')
            }
        },
        legend: {
            top: 0,
            left: 8
        },
        grid: {
            left: 58,
            right: 24,
            top: 48,
            bottom: 46
        },
        dataZoom: [
            { type: 'inside' as const, start: 0, end: 100 },
            { type: 'slider' as const, bottom: 8, height: 18, start: 0, end: 100 }
        ],
        xAxis: {
            type: 'category',
            boundaryGap: false,
            data: dates,
            axisLabel: { hideOverlap: true }
        },
        yAxis: {
            type: 'value',
            scale: true,
            axisLabel: {
                formatter(value: number) {
                    return formatNumber(value, 1)
                }
            },
            splitLine: { lineStyle: { type: 'dashed' } }
        },
        series: [
            buildLineSeries('股票估值', rows.map(row => row.stockValue)),
            buildLineSeries('行业平均估值', rows.map(row => row.industryAverageValue)),
            buildLineSeries('行业中位数估值', rows.map(row => row.industryMedianValue)),
            buildLineSeries('30分位估值', rows.map(row => row.percentile30Value), true),
            buildLineSeries('70分位估值', rows.map(row => row.percentile70Value), true)
        ]
    }
})

const dividendTrendOption = computed<EChartsOption>(() => {
    const rows = dividendRowsForDisplay.value
    const dates = rows.map(row => row.tradeDate)

    return {
        tooltip: {
            trigger: 'axis',
            formatter(params: any) {
                const items = Array.isArray(params) ? params : [params]
                const date = items[0]?.axisValue || ''
                const lines = items.map((item: any) => {
                    const value = item.data?.value ?? item.data
                    return `${item.marker}${item.seriesName}：${formatPercent(normalizeNumber(value))}`
                })
                return [date, ...lines].join('<br/>')
            }
        },
        legend: {
            top: 0,
            left: 8
        },
        grid: {
            left: 58,
            right: 24,
            top: 48,
            bottom: 46
        },
        dataZoom: [
            { type: 'inside' as const, start: 0, end: 100 },
            { type: 'slider' as const, bottom: 8, height: 18, start: 0, end: 100 }
        ],
        xAxis: {
            type: 'category',
            boundaryGap: false,
            data: dates,
            axisLabel: { hideOverlap: true }
        },
        yAxis: {
            type: 'value',
            scale: true,
            axisLabel: {
                formatter(value: number) {
                    return `${value}%`
                }
            },
            splitLine: { lineStyle: { type: 'dashed' } }
        },
        series: [
            buildLineSeries('分红TTM', rows.map(row => row.dvTtm)),
            buildLineSeries('行业平均分红率', rows.map(row => row.industryAverageDvTtm)),
            buildLineSeries('行业中位数分红率', rows.map(row => row.industryMedianDvTtm))
        ]
    }
})

function buildLineSeries(name: string, values: Array<number | null | undefined>, dashed = false) {
    return {
        name,
        type: 'line' as const,
        smooth: true,
        showSymbol: false,
        connectNulls: true,
        lineStyle: dashed ? { type: 'dashed' as const } : undefined,
        data: values.map(value => ({ value: normalizeNumber(value) || 0 }))
    }
}

function buildFallbackChannelRows(metricCode: ValuationMetricCode, sourceRows: ValuationDailyRow[]): ValuationChannelRow[] {
    const field = metricField(metricCode)
    const rows = sortByTradeDate(sourceRows)
        .map(row => ({
            row,
            metricValue: normalizeNumber(row[field]),
            close: normalizeNumber(row.close)
        }))
        .filter(item => isNumber(item.metricValue) && isNumber(item.close) && Number(item.metricValue) > 0)

    if (!rows.length) return []

    const multiples = buildFiveMultiples(rows.map(item => item.metricValue as number))

    return rows.map(({ row, metricValue, close }) => {
        const basePrice = Number(close) / Number(metricValue)

        return {
            fullSymbol: row.fullSymbol,
            tradeDate: row.tradeDate,
            metricCode,
            close,
            channelMultiple1: multiples[0],
            channelPrice1: round(basePrice * multiples[0]),
            channelMultiple2: multiples[1],
            channelPrice2: round(basePrice * multiples[1]),
            channelMultiple3: multiples[2],
            channelPrice3: round(basePrice * multiples[2]),
            channelMultiple4: multiples[3],
            channelPrice4: round(basePrice * multiples[3]),
            channelMultiple5: multiples[4],
            channelPrice5: round(basePrice * multiples[4])
        }
    })
}

function buildFallbackTrendRows(metricCode: ValuationMetricCode, sourceRows: ValuationDailyRow[]): ValuationTrendRow[] {
    const field = metricField(metricCode)
    const averageField = industryAverageField(metricCode)
    const medianField = industryMedianField(metricCode)
    const rows = sortByTradeDate(sourceRows)
    const stockValues = rows.map(row => normalizeNumber(row[field])).filter(isNumber)
    const p30 = quantile(stockValues, 0.3)
    const p70 = quantile(stockValues, 0.7)

    return rows
        .map(row => {
            const stockValue = normalizeNumber(row[field])

            return {
                fullSymbol: row.fullSymbol,
                tradeDate: row.tradeDate,
                metricCode,
                stockValue,
                industryAverageValue: normalizeNumber(row[averageField]),
                industryMedianValue: normalizeNumber(row[medianField]),
                percentile30Value: p30,
                percentile70Value: p70,
                percentileValue: calcPercentile(stockValues, stockValue)
            }
        })
        .filter(row => isNumber(row.stockValue))
}

function buildFallbackDividendRows(sourceRows: ValuationDailyRow[]): DividendTrendRow[] {
    return sortByTradeDate(sourceRows)
        .map(row => ({
            fullSymbol: row.fullSymbol,
            tradeDate: row.tradeDate,
            dvTtm: normalizeNumber(row.dvTtm),
            industryAverageDvTtm: normalizeNumber(row.industryAverageDvTtm),
            industryMedianDvTtm: normalizeNumber(row.industryMedianDvTtm)
        }))
        .filter(row => isNumber(row.dvTtm))
}

function buildFiveMultiples(values: number[]): number[] {
    const sortedValues = values.filter(isNumber).sort((a, b) => a - b)

    if (!sortedValues.length) return [0, 0, 0, 0, 0]

    return [
        quantile(sortedValues, 0.1),
        quantile(sortedValues, 0.3),
        quantile(sortedValues, 0.5),
        quantile(sortedValues, 0.7),
        quantile(sortedValues, 0.9)
    ].map(value => round(value ?? 0))
}

function filterRowsByRange<T extends { tradeDate?: string | null }>(rows: T[], rangeCode: ValuationTimeRangeCode): T[] {
    const sortedRows = sortByTradeDate(rows)
    const latestDateText = sortedRows[sortedRows.length - 1]?.tradeDate

    if (!latestDateText) return sortedRows

    const latestTime = new Date(latestDateText).getTime()
    const rangeDays = rangeOptions.find(item => item.value === rangeCode)?.days || 365 * 3
    const minTime = latestTime - rangeDays * 24 * 60 * 60 * 1000

    return sortedRows.filter(row => {
        if (!row.tradeDate) return false
        return new Date(row.tradeDate).getTime() >= minTime
    })
}

function sortByTradeDate<T extends { tradeDate?: string | null }>(rows: T[] = []): T[] {
    return rows.slice().sort((a, b) => String(a.tradeDate || '').localeCompare(String(b.tradeDate || '')))
}

function metricField(metricCode: ValuationMetricCode): keyof ValuationDailyRow {
    const map: Record<ValuationMetricCode, keyof ValuationDailyRow> = {
        PE_TTM: 'peTtm',
        PB: 'pb',
        PS_TTM: 'psTtm',
    }

    return map[metricCode]
}

function industryAverageField(metricCode: ValuationMetricCode): keyof ValuationDailyRow {
    const map: Record<ValuationMetricCode, keyof ValuationDailyRow> = {
        PE_TTM: 'industryAveragePeTtm',
        PB: 'industryAveragePb',
        PS_TTM: 'industryAveragePsTtm',
    }

    return map[metricCode]
}

function industryMedianField(metricCode: ValuationMetricCode): keyof ValuationDailyRow {
    const map: Record<ValuationMetricCode, keyof ValuationDailyRow> = {
        PE_TTM: 'industryMedianPeTtm',
        PB: 'industryMedianPb',
        PS_TTM: 'industryMedianPsTtm',
    }

    return map[metricCode]
}

function metricLabel(metricCode: ValuationMetricCode): string {
    return metricOptions.find(item => item.value === metricCode)?.label || metricCode
}

function formatMultiple(value?: number | null): string {
    if (value === undefined || value === null || Number.isNaN(Number(value))) return '-'
    return `${formatNumber(Number(value), 2)}x`
}

function formatMultipleValue(value?: number | null): string {
    if (value === undefined || value === null || Number.isNaN(Number(value))) return '-'
    return formatNumber(Number(value), 2)
}

function normalizeNumber(value: unknown): number | null {
    if (value === undefined || value === null || value === '') return null
    const numberValue = Number(value)
    return Number.isNaN(numberValue) ? null : numberValue
}

function isNumber(value: unknown): value is number {
    return typeof value === 'number' && !Number.isNaN(value)
}

function quantile(values: number[], probability: number): number | null {
    const sortedValues = values.filter(isNumber).sort((a, b) => a - b)

    if (!sortedValues.length) return null

    const index = (sortedValues.length - 1) * probability
    const lower = Math.floor(index)
    const upper = Math.ceil(index)
    const weight = index - lower

    if (upper >= sortedValues.length) return sortedValues[lower]
    return sortedValues[lower] * (1 - weight) + sortedValues[upper] * weight
}

function calcPercentile(values: number[], value?: number | null): number | null {
    if (!isNumber(value) || !values.length) return null
    const sortedValues = values.filter(isNumber).sort((a, b) => a - b)
    const lowerOrEqualCount = sortedValues.filter(item => item <= value).length

    return round((lowerOrEqualCount / sortedValues.length) * 100)
}

function round(value: number, digits = 2): number {
    const factor = 10 ** digits
    return Math.round(value * factor) / factor
}

function safeProgress(value?: number | null): number {
    if (value === undefined || value === null || Number.isNaN(Number(value))) return 0
    return Math.max(0, Math.min(100, Number(value)))
}
</script>

<style scoped>
.valuation-detail-page {
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.metric-card,
.detail-card {
    border-radius: 10px;
}

.metric-card {
    height: 118px;
}

.metric-label,
.metric-desc,
.card-tip {
    color: var(--el-text-color-secondary);
    font-size: 12px;
}

.metric-value {
    margin-top: 10px;
    font-size: 22px;
    font-weight: 700;
    line-height: 1.2;
}

.metric-desc {
    margin-top: 8px;
}

.card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
}

.card-title {
    font-weight: 700;
    margin-right: 8px;
}

.metric-tabs {
    margin-bottom: 8px;
}

.data-tip-alert {
    margin-bottom: 12px;
}

.valuation-side-panel {
    height: 100%;
    min-height: 360px;
    padding: 14px;
    border: 1px solid var(--el-border-color-light);
    border-radius: 10px;
    background: var(--el-fill-color-blank);
}

.side-title {
    margin-bottom: 12px;
    font-weight: 700;
}

.percentile-block {
    margin-top: 16px;
}

.percentile-label {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 8px;
    color: var(--el-text-color-secondary);
    font-size: 12px;
}

@media (max-width: 992px) {
    .card-header {
        align-items: flex-start;
        flex-direction: column;
    }

    .valuation-side-panel {
        min-height: auto;
        margin-top: 12px;
    }
}
</style>