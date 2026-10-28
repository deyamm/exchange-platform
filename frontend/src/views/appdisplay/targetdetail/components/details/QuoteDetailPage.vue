<template>
    <div class="quote-detail-page">
        <el-card v-loading="loading" class="detail-card" shadow="never">
            <template #header>
                <div class="card-header quote-toolbar">
                    <div>
                        <span class="card-title">行情走势</span>
                        <span class="card-tip">K 线、复权价格和技术指标均由后端按当前区间统一计算</span>
                    </div>

                    <div class="toolbar-controls">
                        <div class="toolbar-control">
                            <span class="control-label">复权</span>
                            <el-radio-group v-model="adjustMode" size="small" @change="handleAdjustChange">
                                <el-radio-button
                                    v-for="item in adjustTypeOptions"
                                    :key="item.value"
                                    :label="item.value"
                                >
                                    {{ item.label }}
                                </el-radio-button>
                            </el-radio-group>
                        </div>

                        <div class="toolbar-control">
                            <span class="control-label">时间范围</span>
                            <el-radio-group v-model="activeRange" size="small" @change="handleRangeChange">
                                <el-radio-button
                                    v-for="item in fixedRangeOptions"
                                    :key="item.value"
                                    :label="item.value"
                                >
                                    {{ item.label }}
                                </el-radio-button>
                            </el-radio-group>
                        </div>
                    </div>
                </div>
            </template>

            <div class="indicator-toolbar">
                <div class="indicator-control">
                    <span class="control-label">主图叠加</span>
                    <el-select v-model="activeMainIndicator" size="small" class="indicator-select">
                        <el-option
                            v-for="item in mainIndicatorOptions"
                            :key="item.value"
                            :label="item.label"
                            :value="item.value"
                        />
                    </el-select>
                    <span class="param-tip">{{ activeMainIndicatorText }}</span>
                </div>

                <div class="indicator-control">
                    <span class="control-label">副图指标</span>
                    <el-select v-model="activeSubIndicator" size="small" class="indicator-select">
                        <el-option
                            v-for="item in subIndicatorOptions"
                            :key="item.value"
                            :label="item.label"
                            :value="item.value"
                        />
                    </el-select>
                    <span class="param-tip">{{ activeSubIndicatorText }}</span>
                </div>
            </div>

            <el-empty v-if="!rawRows.length" description="暂无行情走势图" :image-size="96" />
            <TargetEcharts v-else :option="technicalChartOption" height="560px" />
        </el-card>

        <el-row :gutter="12" class="analysis-row">
            <el-col :xs="24" :sm="24" :md="15" :lg="15" :xl="15">
                <el-card class="detail-card" shadow="never">
                    <template #header>
                        <div class="card-header">
                            <span class="card-title">技术指标综合分析</span>
                            <span class="card-tip">基于当前后端行情区间与技术指标计算</span>
                        </div>
                    </template>

                    <template v-if="technicalAnalysis">
                        <section class="analysis-section conclusion-section">
                            <div class="conclusion-main">
                                <el-tag size="large" :type="conclusionTagType">
                                    {{ technicalAnalysis.conclusion.label }}
                                </el-tag>
                                <div class="conclusion-text">{{ technicalAnalysis.conclusion.summary }}</div>
                            </div>

                            <div class="conclusion-grid">
                                <div class="score-panel">
                                    <div class="score-value">{{ formatNumber(technicalAnalysis.conclusion.score, 0) }}</div>
                                    <div class="score-label">综合评分</div>
                                </div>

                                <div class="level-panel">
                                    <div
                                        v-for="item in technicalAnalysis.conclusion.levels"
                                        :key="item.key"
                                        class="level-row"
                                    >
                                        <span class="level-label">{{ item.label }}</span>
                                        <el-rate :model-value="item.level" disabled :max="5" size="small" />
                                        <span class="level-text">{{ item.text }}</span>
                                    </div>
                                </div>

                                <el-descriptions :column="1" size="small" border class="conclusion-meta">
                                    <el-descriptions-item label="信号强度">
                                        {{ signalStrengthText(technicalAnalysis.conclusion.signalStrength) }}
                                    </el-descriptions-item>
                                    <el-descriptions-item label="适用周期">
                                        {{ formatPeriod(technicalAnalysis.conclusion.period) }}
                                    </el-descriptions-item>
                                    <el-descriptions-item label="更新时间">
                                        {{ technicalAnalysis.conclusion.updatedAt || '-' }}
                                    </el-descriptions-item>
                                </el-descriptions>
                            </div>
                        </section>

                        <section class="analysis-section">
                            <div class="section-title">多维评分</div>
                            <div class="dimension-grid">
                                <div
                                    v-for="item in technicalAnalysis.dimensionScores"
                                    :key="item.dimension"
                                    class="dimension-card"
                                >
                                    <div class="dimension-title-row">
                                        <span class="dimension-title">{{ item.dimension }}</span>
                                        <span class="dimension-score">{{ formatNumber(item.score, 0) }}</span>
                                    </div>
                                    <el-progress
                                        :percentage="normalizeScore(item.score)"
                                        :show-text="false"
                                        :stroke-width="8"
                                    />
                                    <div class="dimension-desc">{{ item.explanation }}</div>
                                </div>
                            </div>
                        </section>

                        <section class="analysis-section">
                            <div class="section-title">指标信号明细</div>
                            <el-table
                                :data="technicalAnalysis.indicatorSignals"
                                border
                                size="small"
                                max-height="360"
                            >
                                <el-table-column prop="indicator" label="指标" width="120" />
                                <el-table-column prop="status" label="当前状态" min-width="170" />
                                <el-table-column label="信号方向" width="110" align="center">
                                    <template #default="{ row }">
                                        <el-tag size="small" :type="signalTagType(row.direction)">
                                            {{ row.direction }}
                                        </el-tag>
                                    </template>
                                </el-table-column>
                                <el-table-column prop="explanation" label="解释" min-width="260" />
                            </el-table>
                        </section>

                        <section class="analysis-section">
                            <div class="section-title">信号冲突与风险提示</div>
                            <div class="signal-summary">
                                <el-statistic
                                    title="多头信号"
                                    :value="technicalAnalysis.signalConflict.bullishCount"
                                />
                                <el-statistic
                                    title="空头信号"
                                    :value="technicalAnalysis.signalConflict.bearishCount"
                                />
                                <el-statistic
                                    title="中性信号"
                                    :value="technicalAnalysis.signalConflict.neutralCount"
                                />
                                <div class="consistency-box">
                                    <span class="consistency-label">信号一致性</span>
                                    <strong>{{ technicalAnalysis.signalConflict.consistency }}</strong>
                                </div>
                            </div>

                            <el-row :gutter="12">
                                <el-col :xs="24" :md="12">
                                    <div class="factor-box support-box">
                                        <div class="factor-title">主要支持因素</div>
                                        <ol>
                                            <li
                                                v-for="item in technicalAnalysis.signalConflict.supports"
                                                :key="item"
                                            >
                                                {{ item }}
                                            </li>
                                        </ol>
                                    </div>
                                </el-col>
                                <el-col :xs="24" :md="12">
                                    <div class="factor-box risk-box">
                                        <div class="factor-title">主要风险因素</div>
                                        <ol>
                                            <li
                                                v-for="item in technicalAnalysis.signalConflict.risks"
                                                :key="item"
                                            >
                                                {{ item }}
                                            </li>
                                        </ol>
                                    </div>
                                </el-col>
                            </el-row>
                        </section>

                        <section class="analysis-section explanation-section">
                            <div class="section-title">解释说明</div>
                            <div class="note-text">{{ technicalAnalysis.explanation || '-' }}</div>
                        </section>
                    </template>

                    <el-empty v-else description="暂无技术分析" :image-size="80" />
                </el-card>
            </el-col>

            <el-col :xs="24" :sm="24" :md="9" :lg="9" :xl="9">
                <el-card class="detail-card" shadow="never">
                    <template #header>
                        <div class="card-header">
                            <span class="card-title">区间表现</span>
                            <span class="card-tip">当前统计区间：{{ rangeDateText }}</span>
                        </div>
                    </template>

                    <template v-if="rangePerformance">
                        <section class="range-section">
                            <div class="section-title">收益风险</div>
                            <el-descriptions :column="1" size="small" border>
                                <el-descriptions-item label="最高价 / 最低价">
                                    {{ formatNumber(rangePerformance.returnRisk?.high) }}
                                    /
                                    {{ formatNumber(rangePerformance.returnRisk?.low) }}
                                </el-descriptions-item>
                                <el-descriptions-item label="区间收益率">
                                    <span :class="signedClass(rangePerformance.returnRisk?.changePercent)">
                                        {{ formatPercent(rangePerformance.returnRisk?.changePercent, 2, true) }}
                                    </span>
                                </el-descriptions-item>
                                <el-descriptions-item label="年化收益率">
                                    <span :class="signedClass(rangePerformance.returnRisk?.annualizedReturn)">
                                        {{ formatPercent(rangePerformance.returnRisk?.annualizedReturn, 2, true) }}
                                    </span>
                                </el-descriptions-item>
                                <el-descriptions-item label="区间振幅">
                                    {{ formatPercent(rangePerformance.returnRisk?.amplitude) }}
                                </el-descriptions-item>
                                <el-descriptions-item label="波动率">
                                    {{ formatPercent(rangePerformance.returnRisk?.volatility) }}
                                </el-descriptions-item>
                                <el-descriptions-item label="最大回撤">
                                    <span :class="signedClass(rangePerformance.returnRisk?.maxDrawdown)">
                                        {{ formatPercent(rangePerformance.returnRisk?.maxDrawdown, 2, true) }}
                                    </span>
                                </el-descriptions-item>
                                <el-descriptions-item label="夏普比率">
                                    {{ formatNumber(rangePerformance.returnRisk?.sharpeRatio, 2) }}
                                </el-descriptions-item>
                                <el-descriptions-item label="卡玛比率">
                                    {{ formatNumber(rangePerformance.returnRisk?.calmarRatio, 2) }}
                                </el-descriptions-item>
                            </el-descriptions>
                        </section>

                        <section class="range-section">
                            <div class="section-title">成交</div>
                            <el-descriptions :column="1" size="small" border>
                                <el-descriptions-item label="区间成交量">
                                    {{ formatAmount(rangePerformance.volume?.volume) }}
                                </el-descriptions-item>
                                <el-descriptions-item label="区间成交额">
                                    {{ formatAmount(rangePerformance.volume?.turnover) }}
                                </el-descriptions-item>
                                <el-descriptions-item label="日均成交量">
                                    {{ formatAmount(rangePerformance.volume?.avgVolume) }}
                                </el-descriptions-item>
                                <el-descriptions-item label="日均成交额">
                                    {{ formatAmount(rangePerformance.volume?.avgTurnover) }}
                                </el-descriptions-item>
                                <el-descriptions-item label="区间换手率">
                                    {{ formatPercent(rangePerformance.volume?.turnoverRate) }}
                                </el-descriptions-item>
                                <el-descriptions-item label="平均换手率">
                                    {{ formatPercent(rangePerformance.volume?.avgTurnoverRate) }}
                                </el-descriptions-item>
                                <el-descriptions-item label="最大成交量日">
                                    {{ rangePerformance.volume?.maxVolumeTradeDate || '-' }}
                                </el-descriptions-item>
                                <el-descriptions-item label="最大成交额日">
                                    {{ rangePerformance.volume?.maxTurnoverTradeDate || '-' }}
                                </el-descriptions-item>
                            </el-descriptions>
                        </section>

                        <section class="range-section">
                            <div class="section-title">对比分析</div>
                            <el-descriptions :column="1" size="small" border>
                                <el-descriptions-item label="跑赢/跑输大盘">
                                    {{ formatExcessReturn(rangePerformance.comparison?.marketExcessReturn) }}
                                </el-descriptions-item>
                                <el-descriptions-item label="跑赢/跑输行业">
                                    {{ formatExcessReturn(rangePerformance.comparison?.industryExcessReturn) }}
                                </el-descriptions-item>
                                <el-descriptions-item label="行业排名">
                                    {{ rangePerformance.comparison?.industryRankText || '-' }}
                                </el-descriptions-item>
                                <el-descriptions-item label="市场排名">
                                    {{ rangePerformance.comparison?.marketRankText || '-' }}
                                </el-descriptions-item>
                                <el-descriptions-item label="区间 Alpha">
                                    {{ formatPercent(rangePerformance.comparison?.alpha) }}
                                </el-descriptions-item>
                                <el-descriptions-item label="区间 Beta">
                                    {{ formatNumber(rangePerformance.comparison?.beta, 2) }}
                                </el-descriptions-item>
                                <el-descriptions-item label="相关性">
                                    {{ formatNumber(rangePerformance.comparison?.correlation, 2) }}
                                </el-descriptions-item>
                                <el-descriptions-item label="相对强弱评分">
                                    {{ formatNumber(rangePerformance.comparison?.relativeStrengthScore, 0) }}
                                </el-descriptions-item>
                            </el-descriptions>
                        </section>

                        <section class="range-section">
                            <div class="section-title">价格位置与区间结构</div>
                            <el-descriptions :column="1" size="small" border>
                                <el-descriptions-item label="当前价格分位">
                                    {{ formatPercent(rangePerformance.priceStructure?.pricePositionPercent) }}
                                </el-descriptions-item>
                                <el-descriptions-item label="距区间高点">
                                    {{ formatPercent(rangePerformance.priceStructure?.drawdownFromHigh, 2, true) }}
                                </el-descriptions-item>
                                <el-descriptions-item label="距区间低点">
                                    {{ formatPercent(rangePerformance.priceStructure?.reboundFromLow, 2, true) }}
                                </el-descriptions-item>
                                <el-descriptions-item label="区间新高次数">
                                    {{ rangePerformance.priceStructure?.newHighCount ?? '-' }}
                                </el-descriptions-item>
                                <el-descriptions-item label="区间新低次数">
                                    {{ rangePerformance.priceStructure?.newLowCount ?? '-' }}
                                </el-descriptions-item>
                                <el-descriptions-item label="价格区间">
                                    {{ rangePerformance.priceStructure?.priceRangeText || '-' }}
                                </el-descriptions-item>
                                <el-descriptions-item label="成交额区间">
                                    {{ rangePerformance.priceStructure?.turnoverRangeText || '-' }}
                                </el-descriptions-item>
                            </el-descriptions>
                        </section>
                    </template>

                    <el-empty v-else description="暂无区间表现" :image-size="80" />
                </el-card>
            </el-col>
        </el-row>
    </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import type { EChartsOption } from 'echarts'
import TargetEcharts from '/@/components/common/TargetEcharts.vue'
import { queryTargetQuoteDetail } from '/@/api/targetdetail/quoteDetailApi'
import {
    adjustTypeOptions,
    type AdjustType,
    type QuoteTechnicalAnalysis,
    type RangePerformance,
    type TargetQuoteDetail
} from '/@/types/targetdetail/quoteDetailType'
import {
    mainIndicatorOptions,
    rangeOptions,
    subIndicatorOptions,
    type MainIndicatorCode,
    type RangeCode,
    type SubIndicatorCode
} from '/@/types/marketoverview/constants'
import type { ChartGroup, OHLCData } from '/@/types/marketoverview/view'
import {
    formatAmount,
    formatNumber,
    formatPercent,
    signalStrengthText,
    signedClass,
    statusTagType
} from '/@/utils/marketUtils'

interface QuoteMarketState {
    charts: ChartGroup[]
    analysis?: QuoteTechnicalAnalysis
    rangePerformance?: RangePerformance
    adjustType: AdjustType
}

const props = withDefaults(
    defineProps<TargetQuoteDetail>(),
    {
        charts: () => [],
        adjustType: 'FORWARD',
        meta: () => ({})
    }
)

const marketState = reactive<QuoteMarketState>({
    charts: props.charts || [],
    analysis: props.quoteTechnicalAnalysis,
    rangePerformance: props.rangePerformance,
    adjustType: props.adjustType
})

const initialRange =
    props.rangePerformance?.rangeCode && props.rangePerformance.rangeCode !== 'CUSTOM'
        ? props.rangePerformance.rangeCode
        : '1Y'

const adjustMode = ref<AdjustType>(props.adjustType)
const activeRange = ref<RangeCode>(initialRange as RangeCode)
const activeMainIndicator = ref<MainIndicatorCode>('MA')
const activeSubIndicator = ref<SubIndicatorCode>('VOL')
const loading = ref(false)

const fixedRangeOptions = computed(() =>
    rangeOptions.filter(item => item.value !== 'CUSTOM')
)

const activeMainIndicatorText = computed(
    () => mainIndicatorOptions.find(item => item.value === activeMainIndicator.value)?.tip || ''
)
const activeSubIndicatorText = computed(
    () => subIndicatorOptions.find(item => item.value === activeSubIndicator.value)?.tip || ''
)

const rawRows = computed<OHLCData[]>(() => {
    const chart = marketState.charts.find(item => item.chartType === 'CANDLESTICK')
    return (chart?.series?.[0]?.data || [])
        .filter(isOhlcData)
        .slice()
        .sort((a, b) => a.tradeDate.localeCompare(b.tradeDate))
})

const technicalChartOption = computed<EChartsOption>(() => buildTechnicalChartOption(rawRows.value))
const technicalAnalysis = computed(() => marketState.analysis)
const rangePerformance = computed(() => marketState.rangePerformance)

const rangeDateText = computed(() => {
    const startDate = marketState.rangePerformance?.startDate
    const endDate = marketState.rangePerformance?.endDate
    if (startDate && endDate) return `${startDate} 至 ${endDate}`
    return fixedRangeOptions.value.find(item => item.value === activeRange.value)?.label || activeRange.value
})

const conclusionTagType = computed(() =>
    statusTagType(overallScoreToStatus(technicalAnalysis.value?.conclusion.score))
)

watch(
    () => props.charts,
    value => {
        marketState.charts = value || []
    },
    { deep: true }
)

watch(
    () => props.quoteTechnicalAnalysis,
    value => {
        marketState.analysis = value
    },
    { deep: true }
)

watch(
    () => props.rangePerformance,
    value => {
        marketState.rangePerformance = value
        if (value?.rangeCode && value.rangeCode !== 'CUSTOM') {
            activeRange.value = value.rangeCode as RangeCode
        }
    },
    { deep: true }
)

watch(
    () => props.adjustType,
    value => {
        marketState.adjustType = value
        adjustMode.value = value
    }
)

onMounted(() => {
    if (!marketState.charts.length && resolveFullSymbol()) {
        void loadMarketData()
    }
})

function handleRangeChange(): void {
    void loadMarketData()
}

function handleAdjustChange(): void {
    void loadMarketData()
}

/**
 * 重新加载当前股票的行情视图。
 * 时间范围和复权口径变化时，由后端重新生成 K 线、技术指标、技术分析和区间统计。
 */
async function loadMarketData(): Promise<void> {
    const fullSymbol = resolveFullSymbol()
    if (!fullSymbol) return

    try {
        loading.value = true
        const result = await queryTargetQuoteDetail({
            fullSymbol,
            tradeDate: props.quote?.tradeDate || '',
            rangeCode: activeRange.value,
            adjustType: adjustMode.value
        })
        console.log(result)

        if (!result) return

        marketState.charts = result.charts || []
        marketState.analysis = result.quoteTechnicalAnalysis
        marketState.rangePerformance = result.rangePerformance
        marketState.adjustType = result.adjustType
        adjustMode.value = result.adjustType
    } catch (error) {
        console.error('加载个股行情数据失败', error)
    } finally {
        loading.value = false
    }
}

function resolveFullSymbol(): string {
    return props.fullSymbol || props.quote?.fullSymbol || ''
}

function isOhlcData(value: unknown): value is OHLCData {
    return Boolean(
        value &&
        typeof value === 'object' &&
        'tradeDate' in value &&
        'open' in value &&
        'close' in value
    )
}

/**
 * 根据当前主图和副图选择组装 ECharts 配置。
 * 指标数值直接读取后端 ChartGroup，不在页面中重新计算金融指标。
 */
function buildTechnicalChartOption(rows: OHLCData[]): EChartsOption {
    const dates = rows.map(row => row.tradeDate)
    const candleData = rows.map(row => [row.open, row.close, row.low, row.high])
    const mainSeries = buildMainIndicatorSeries()
    const subSeries = buildSubIndicatorSeries()
    const legend = ['K线', ...mainSeries.map(item => String(item.name)), ...subSeries.map(item => String(item.name))]

    return {
        animation: false,
        axisPointer: {
            link: [{ xAxisIndex: [0, 1] }]
        },
        tooltip: {
            trigger: 'axis',
            axisPointer: { type: 'cross' },
            formatter(params: any) {
                const list = Array.isArray(params) ? params : [params]
                const dataIndex = list[0]?.dataIndex
                if (dataIndex === undefined || dataIndex === null) return ''

                const row = rows[dataIndex]
                if (!row) return ''

                const indicatorItems = [...mainSeries, ...subSeries]
                    .map(series => {
                        const value = getSeriesValue(series, dataIndex)
                        if (value === null || value === undefined) return null
                        return `${series.name}：${formatTooltipValue(value)}`
                    })
                    .filter((item): item is string => item !== null)

                return [
                    `<strong>${row.tradeDate}</strong>`,
                    `开盘：${formatNumber(row.open)}`,
                    `收盘：${formatNumber(row.close)}`,
                    `最低：${formatNumber(row.low)}`,
                    `最高：${formatNumber(row.high)}`,
                    `成交量：${formatAmount(row.volume)}`,
                    `成交额：${formatAmount(row.turnover)}`,
                    `换手率：${formatPercent(row.turnoverRate)}`,
                    ...indicatorItems
                ].join('<br/>')
            }
        },
        legend: {
            top: 0,
            type: 'scroll',
            data: legend
        },
        grid: [
            { left: 58, right: 28, top: 46, height: 320 },
            { left: 58, right: 28, top: 412, height: 96 }
        ],
        xAxis: [
            {
                type: 'category',
                data: dates,
                boundaryGap: true,
                min: 'dataMin',
                max: 'dataMax',
                axisLabel: { hideOverlap: true }
            },
            {
                type: 'category',
                gridIndex: 1,
                data: dates,
                boundaryGap: true,
                min: 'dataMin',
                max: 'dataMax',
                axisLabel: { hideOverlap: true }
            }
        ],
        yAxis: [
            {
                scale: true,
                splitLine: { lineStyle: { type: 'dashed' } }
            },
            {
                scale: true,
                gridIndex: 1,
                splitLine: { lineStyle: { type: 'dashed' } },
                axisLabel: {
                    formatter(value: number) {
                        return formatYAxisValue(value, activeSubIndicator.value)
                    }
                }
            }
        ],
        dataZoom: [
            { type: 'inside', xAxisIndex: [0, 1], start: 0, end: 100 },
            { type: 'slider', xAxisIndex: [0, 1], bottom: 8, height: 24, start: 0, end: 100 }
        ],
        series: [
            {
                name: 'K线',
                type: 'candlestick',
                data: candleData,
                itemStyle: {
                    color: '#ef5350',
                    color0: '#26a69a',
                    borderColor: '#ef5350',
                    borderColor0: '#26a69a'
                }
            },
            ...mainSeries,
            ...subSeries
        ]
    }
}

/** 读取后端按 chartId 拆分的主图技术指标。 */
function buildMainIndicatorSeries(): any[] {
    if (activeMainIndicator.value === 'MA') {
        return [5, 10, 20, 60, 120, 250].map(period =>
            lineSeries(`MA${period}`, findSeriesData(`MA${period}`), 0)
        )
    }

    if (activeMainIndicator.value === 'EMA') {
        return [9, 12, 26].map(period =>
            lineSeries(`EMA${period}`, findSeriesData(`EMA${period}`), 0)
        )
    }

    if (activeMainIndicator.value === 'BOLL') {
        return [
            lineSeries('BOLL上轨', findSeriesData('BOLL', 'upper'), 0),
            lineSeries('BOLL中轨', findSeriesData('BOLL', 'mid'), 0),
            lineSeries('BOLL下轨', findSeriesData('BOLL', 'lower'), 0)
        ]
    }

    if (activeMainIndicator.value === 'SAR') {
        return [{
            name: 'SAR',
            type: 'scatter',
            symbolSize: 5,
            xAxisIndex: 0,
            yAxisIndex: 0,
            data: findSeriesData('SAR', 'SAR')
        }]
    }

    if (activeMainIndicator.value === 'VWAP') {
        return [1, 5, 10, 20, 60, 120].map(period =>
            lineSeries(`VWAP${period}`, findSeriesData(`VWAP${period}`), 0)
        )
    }

    return [lineSeries('SuperTrend', findSeriesData('SUPER_TREND', 'SuperTrend'), 0)]
}

/** 读取后端按 chartId 拆分的副图技术指标。 */
function buildSubIndicatorSeries(): any[] {
    if (activeSubIndicator.value === 'VOL') {
        return [barSeries('成交量', findSeriesData('VOL', 'volume'), true)]
    }

    if (activeSubIndicator.value === 'TURNOVER') {
        return [barSeries('成交额', findSeriesData('TURNOVER', 'turnover'), true)]
    }

    if (activeSubIndicator.value === 'TURNOVER_RATE') {
        return [lineSeries('换手率', findSeriesData('TURNOVER_RATE', 'turnoverRate'), 1)]
    }

    if (activeSubIndicator.value === 'MACD') {
        return [
            lineSeries('DIF', findSeriesData('MACD', 'DIF'), 1),
            lineSeries('DEA', findSeriesData('MACD', 'DEA'), 1),
            barSeries('MACD', findSeriesData('MACD', 'MACD'))
        ]
    }

    if (activeSubIndicator.value === 'KDJ') {
        return [
            lineSeries('K', findSeriesData('KDJ', 'K'), 1),
            lineSeries('D', findSeriesData('KDJ', 'D'), 1),
            lineSeries('J', findSeriesData('KDJ', 'J'), 1)
        ]
    }

    if (activeSubIndicator.value === 'RSI') {
        return [lineSeries('RSI14', findSeriesData('RSI', 'RSI14'), 1)]
    }

    if (activeSubIndicator.value === 'CCI') {
        return [lineSeries('CCI14', findSeriesData('CCI', 'CCI14'), 1)]
    }

    if (activeSubIndicator.value === 'DMI') {
        return [
            lineSeries('+DI', findSeriesData('DMI', '+DI'), 1),
            lineSeries('-DI', findSeriesData('DMI', '-DI'), 1),
            lineSeries('ADX', findSeriesData('DMI', 'ADX'), 1)
        ]
    }

    if (activeSubIndicator.value === 'ATR') {
        return [lineSeries('ATR14', findSeriesData('ATR', 'ATR14'), 1)]
    }

    return [
        lineSeries('OBV', findSeriesData('OBV', 'OBV'), 1),
        lineSeries('OBV MA14', findSeriesData('OBV', 'OBV14'), 1)
    ]
}

/** 安全查找图表序列；后端缺少某个指标时返回空数组。 */
function findSeriesData(chartId: string, seriesName?: string): any[] {
    const chart = marketState.charts.find(item => item.chartId === chartId)
    if (!chart) return []

    if (!seriesName) {
        return chart.series?.[0]?.data || []
    }

    return chart.series?.find(item => item.name === seriesName)?.data || []
}

function lineSeries(name: string, data: any[], yAxisIndex: number): any {
    return {
        name,
        type: 'line',
        data,
        smooth: true,
        showSymbol: false,
        connectNulls: true,
        xAxisIndex: yAxisIndex,
        yAxisIndex,
        lineStyle: { width: 1.4 }
    }
}

function barSeries(name: string, data: any[], useCandleDirection = false): any {
    return {
        name,
        type: 'bar',
        xAxisIndex: 1,
        yAxisIndex: 1,
        data: data.map((value, index) => {
            const row = rawRows.value[index]
            const positive = useCandleDirection && row
                ? Number(row.close) >= Number(row.open)
                : Number(value) >= 0

            return {
                value,
                itemStyle: { color: positive ? '#ef5350' : '#26a69a' }
            }
        })
    }
}

function getSeriesValue(series: any, dataIndex: number): unknown {
    const value = series.data?.[dataIndex]
    if (value && typeof value === 'object' && 'value' in value) {
        return value.value
    }
    return value
}

function formatTooltipValue(value: unknown): string {
    if (Array.isArray(value)) {
        return value.map(item => formatNumber(Number(item))).join(', ')
    }
    if (typeof value === 'number') return formatNumber(value)
    return String(value ?? '-')
}

function formatYAxisValue(value: number, code: SubIndicatorCode): string {
    if (['VOL', 'TURNOVER', 'OBV'].includes(code)) return formatAmount(value)
    if (['TURNOVER_RATE', 'RSI', 'KDJ', 'DMI'].includes(code)) return `${formatNumber(value, 1)}%`
    return formatNumber(value, 2)
}

function formatExcessReturn(value?: number | null): string {
    if (value === undefined || value === null || Number.isNaN(Number(value))) return '-'
    return value >= 0
        ? `跑赢 ${formatPercent(value, 2, true)}`
        : `跑输 ${formatPercent(Math.abs(value), 2)}`
}

function formatPeriod(period?: string | null): string {
    const map: Record<string, string> = {
        DAILY: '日线',
        WEEKLY: '周线',
        MONTHLY: '月线'
    }
    return period ? (map[period] || period) : '-'
}

function normalizeScore(score?: number | null): number {
    if (score === undefined || score === null || Number.isNaN(Number(score))) return 0
    return Math.max(0, Math.min(100, Math.round(Number(score))))
}

function overallScoreToStatus(score?: number | null): string {
    const value = Number(score) || 0
    if (value >= 70) return 'GOOD'
    if (value >= 55) return 'NORMAL'
    if (value >= 40) return 'WARNING'
    return 'DANGER'
}

function signalTagType(direction: string): '' | 'success' | 'warning' | 'danger' | 'info' | 'primary' {
    if (['偏多', '偏强', '支持上涨', 'BULLISH', 'SUPPORT_UP'].includes(direction)) return 'success'
    if (['偏空', 'BEARISH'].includes(direction)) return 'danger'
    if (['谨慎', '风险增加', '超买', '超卖', 'CAUTION', 'RISK_UP'].includes(direction)) return 'warning'
    return 'info'
}
</script>

<style scoped>
.quote-detail-page {
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.detail-card {
    border-radius: 10px;
}

.card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
}

.quote-toolbar {
    align-items: flex-start;
}

.card-title {
    margin-right: 8px;
    font-weight: 600;
    color: var(--el-text-color-primary);
}

.card-tip,
.param-tip,
.control-label {
    color: var(--el-text-color-secondary);
    font-size: 12px;
}

.toolbar-controls,
.toolbar-control,
.indicator-toolbar,
.indicator-control {
    display: flex;
    align-items: center;
    gap: 10px;
}

.toolbar-controls {
    flex-wrap: wrap;
    justify-content: flex-end;
}

.indicator-toolbar {
    flex-wrap: wrap;
    justify-content: space-between;
    margin-bottom: 12px;
    padding: 10px 12px;
    border-radius: 8px;
    background: #f7f8fa;
}

.indicator-select {
    width: 190px;
}

.analysis-row {
    align-items: flex-start;
}

.analysis-section,
.range-section {
    margin-bottom: 18px;
}

.analysis-section:last-child,
.range-section:last-child {
    margin-bottom: 0;
}

.section-title {
    margin-bottom: 10px;
    color: var(--el-text-color-primary);
    font-size: 14px;
    font-weight: 600;
}

.conclusion-section {
    padding: 12px;
    border-radius: 10px;
    background: #f7f8fa;
}

.conclusion-main {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 12px;
}

.conclusion-text {
    color: var(--el-text-color-regular);
    line-height: 1.7;
}

.conclusion-grid {
    display: grid;
    grid-template-columns: 120px minmax(180px, 1fr) 260px;
    gap: 12px;
    align-items: stretch;
}

.score-panel {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    border-radius: 8px;
    background: #fff;
}

.score-value {
    color: var(--el-color-primary);
    font-size: 34px;
    font-weight: 700;
    line-height: 1;
}

.score-label {
    margin-top: 8px;
    color: var(--el-text-color-secondary);
    font-size: 12px;
}

.level-panel {
    padding: 10px 12px;
    border-radius: 8px;
    background: #fff;
}

.level-row {
    display: grid;
    grid-template-columns: 56px auto 1fr;
    align-items: center;
    gap: 10px;
    min-height: 30px;
}

.level-label {
    color: var(--el-text-color-regular);
    font-size: 13px;
}

.level-text {
    color: var(--el-text-color-secondary);
    font-size: 12px;
}

.conclusion-meta {
    background: #fff;
}

.dimension-grid {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 10px;
}

.dimension-card {
    padding: 12px;
    border: 1px solid #ebeef5;
    border-radius: 8px;
}

.dimension-title-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 8px;
}

.dimension-title {
    font-weight: 600;
}

.dimension-score {
    color: var(--el-color-primary);
    font-weight: 700;
}

.dimension-desc,
.note-text {
    margin-top: 8px;
    color: var(--el-text-color-regular);
    font-size: 13px;
    line-height: 1.8;
}

.signal-summary {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 10px;
    margin-bottom: 12px;
}

.consistency-box {
    display: flex;
    min-height: 70px;
    flex-direction: column;
    justify-content: center;
    padding: 0 14px;
    border: 1px solid #ebeef5;
    border-radius: 8px;
}

.consistency-label {
    color: var(--el-text-color-secondary);
    font-size: 12px;
}

.factor-box {
    min-height: 150px;
    padding: 12px 14px;
    border-radius: 8px;
    line-height: 1.8;
}

.support-box {
    background: #f0f9eb;
}

.risk-box {
    background: #fef6ec;
}

.factor-title {
    margin-bottom: 6px;
    font-weight: 600;
}

.factor-box ol {
    margin: 0;
    padding-left: 18px;
}

.explanation-section {
    padding-top: 4px;
}

.is-up {
    color: var(--el-color-danger);
}

.is-down {
    color: var(--el-color-success);
}

.is-flat {
    color: var(--el-text-color-regular);
}

@media (max-width: 1280px) {
    .conclusion-grid,
    .dimension-grid,
    .signal-summary {
        grid-template-columns: 1fr;
    }
}

@media (max-width: 768px) {
    .card-header,
    .quote-toolbar,
    .toolbar-controls,
    .toolbar-control,
    .indicator-toolbar,
    .indicator-control {
        align-items: flex-start;
        flex-direction: column;
    }
}
</style>