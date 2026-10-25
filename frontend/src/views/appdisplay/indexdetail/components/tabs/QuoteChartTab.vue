<template>
    <div class="quote-detail-page quote-chart-tab">
        <el-card class="detail-card" shadow="never">
            <template #header>
                <div class="card-header quote-toolbar">
                    <div class="header-title-wrap">
                        <span class="card-title">行情走势</span>
                        <span class="card-tip">当前：{{ mainIndicatorLabel }} / {{ subIndicatorLabel }} · {{
                            activeRangeText }}</span>
                    </div>

                    <div class="toolbar-controls">

                        <div class="toolbar-control">
                            <span class="control-label">时间范围</span>
                            <el-radio-group v-model="selectedRange" size="small" @change="handleRangeChange">
                                <el-radio-button v-for="item in rangeOptions" :key="item.value" :label="item.value">
                                    {{ item.label }}
                                </el-radio-button>
                            </el-radio-group>
                        </div>
                    </div>
                </div>

                <div v-if="selectedRange === 'CUSTOM'" class="custom-date-row">
                    <span class="custom-date-label">自定义区间</span>
                    <el-date-picker v-model="customStartDate" type="date" size="small" value-format="YYYY-MM-DD"
                        placeholder="开始日期" />
                    <span class="date-separator">至</span>
                    <el-date-picker v-model="customEndDate" type="date" size="small" value-format="YYYY-MM-DD"
                        placeholder="结束日期" />
                </div>
            </template>

            <div class="indicator-toolbar">
                <div class="indicator-control">
                    <span class="control-label">主图叠加</span>
                    <el-select v-model="mainIndicator" size="small" class="indicator-select">
                        <el-option v-for="item in mainIndicatorOptions" :key="item.value" :label="item.label"
                            :value="item.value" />
                    </el-select>
                    <span class="param-tip">{{ mainIndicatorParamText }}</span>
                </div>

                <div class="indicator-control">
                    <span class="control-label">副图指标</span>
                    <el-select v-model="subIndicator" size="small" class="indicator-select">
                        <el-option v-for="item in subIndicatorOptions" :key="item.value" :label="item.label"
                            :value="item.value" />
                    </el-select>
                    <span class="param-tip">{{ subIndicatorParamText }}</span>
                </div>
            </div>

            <el-empty v-if="!rawRows.length" description="暂无行情走势图" :image-size="96" />
            <TargetEcharts v-else :option="klineOption" height="560px" />
        </el-card>

        <el-row v-if="rawRows.length" :gutter="12" class="analysis-row">
            <el-col :xs="24" :sm="24" :md="15" :lg="15" :xl="15">
                <el-card class="detail-card" shadow="never">
                    <template #header>
                        <div class="card-header">
                            <span class="card-title">技术指标综合分析</span>
                            <span class="card-tip">基于当前 K 线区间与所列技术指标自动计算</span>
                        </div>
                    </template>

                    <section class="analysis-section conclusion-section">
                        <div class="conclusion-main">
                            <el-tag size="large" :type="conclusionTagType">{{ localProps.analysis.conclusion.label
                                }}</el-tag>
                            <div class="conclusion-text">{{ localProps.analysis.conclusion.summary }}</div>
                        </div>

                        <div class="conclusion-grid">
                            <div class="score-panel">
                                <div class="score-value">{{ formatNumber(localProps.analysis.conclusion.score, 0) }}
                                </div>
                                <div class="score-label">综合评分</div>
                            </div>

                            <div class="level-panel">
                                <div v-for="item in localProps.analysis.conclusion.levels" :key="item.key"
                                    class="level-row">
                                    <span class="level-label">{{ item.label }}</span>
                                    <el-rate :model-value="item.level" disabled :max="5" size="small" />
                                    <span class="level-text">{{ item.text }}</span>
                                </div>
                            </div>

                            <el-descriptions :column="1" size="small" border class="conclusion-meta">
                                <el-descriptions-item label="信号强度">{{ localProps.analysis.conclusion.signalStrength
                                    }}</el-descriptions-item>
                                <el-descriptions-item label="适用周期">{{ localProps.analysis.conclusion.period
                                    }}</el-descriptions-item>
                                <el-descriptions-item label="更新时间">{{
                                    formatDateTime(localProps.analysis.conclusion.updatedAt)
                                    }}</el-descriptions-item>
                            </el-descriptions>
                        </div>
                    </section>

                    <section class="analysis-section">
                        <div class="section-title">多维评分</div>
                        <div class="dimension-grid">
                            <div v-for="item in localProps.analysis.dimensionScores" :key="item.dimension"
                                class="dimension-card">
                                <div class="dimension-title-row">
                                    <span class="dimension-title">{{ item.dimension }}</span>
                                    <el-tag size="small" :type="scoreTagType(item.score)">{{ formatNumber(item.score, 0)
                                        }}</el-tag>
                                </div>
                                <el-progress :percentage="normalizeScore(item.score)" :show-text="false"
                                    :stroke-width="8" />
                                <div class="dimension-desc">{{ item.explanation }}</div>
                            </div>
                        </div>
                    </section>

                    <section class="analysis-section">
                        <div class="section-title">指标信号明细</div>
                        <el-table :data="localProps.analysis.indicatorSignals" border size="small" max-height="360">
                            <el-table-column prop="indicator" label="指标" width="120" />
                            <el-table-column prop="status" label="当前状态" min-width="170" />
                            <el-table-column label="信号方向" width="110" align="center">
                                <template #default="{ row }">
                                    <el-tag size="small" :type="indicatorTagType(row.direction)">
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
                            <el-statistic title="多头信号" :value="localProps.analysis.signalConflict.bullishCount" />
                            <el-statistic title="空头信号" :value="localProps.analysis.signalConflict.bearishCount" />
                            <el-statistic title="中性信号" :value="localProps.analysis.signalConflict.neutralCount" />
                            <div class="consistency-box">
                                <span class="consistency-label">信号一致性</span>
                                <strong>{{ localProps.analysis.signalConflict.consistency }}</strong>
                            </div>
                        </div>

                        <el-row :gutter="12">
                            <el-col :xs="24" :md="12">
                                <div class="factor-box support-box">
                                    <div class="factor-title">主要支持因素</div>
                                    <ol>
                                        <li v-for="item in localProps.analysis.signalConflict.supports" :key="item">{{
                                            item }}
                                        </li>
                                    </ol>
                                </div>
                            </el-col>
                            <el-col :xs="24" :md="12">
                                <div class="factor-box risk-box">
                                    <div class="factor-title">主要风险因素</div>
                                    <ol>
                                        <li v-for="item in localProps.analysis.signalConflict.risks" :key="item">{{ item
                                            }}</li>
                                    </ol>
                                </div>
                            </el-col>
                        </el-row>
                    </section>

                    <section class="analysis-section explanation-section">
                        <div class="section-title">解释说明</div>
                        <div class="note-text">{{ localProps.analysis.explanation }}</div>
                    </section>
                </el-card>
            </el-col>

            <el-col :xs="24" :sm="24" :md="9" :lg="9" :xl="9">
                <el-card class="detail-card" shadow="never">
                    <template #header>
                        <div class="card-header">
                            <span class="card-title">区间表现</span>
                            <span class="card-tip">随 K 线时间范围联动：{{ rangeDateText }}</span>
                        </div>
                    </template>

                    <section v-for="section in rangeSections" :key="section.title" class="range-section">
                        <div class="section-title">{{ section.title }}</div>
                        <el-descriptions :column="1" size="small" border>
                            <el-descriptions-item v-for="item in section.items" :key="item.label" :label="item.label">
                                <span :class="item.className">{{ item.value }}</span>
                            </el-descriptions-item>
                        </el-descriptions>
                    </section>
                </el-card>
            </el-col>
        </el-row>
    </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import type { EChartsOption } from 'echarts'
import type { IndexRangePerformance, QuoteTechnicalAnalysis } from '/@/types/indexdetail/indexDetailCenter'
import TargetEcharts from '/@/components/common/TargetEcharts.vue'
import { formatAmount, formatDateTime, formatNumber, formatPercent, signedClass } from '/@/utils/marketUtils'
import { ChartGroup, ChartSeries, MarketQuote, OHLCData } from '/@/types/marketoverview/view'
import { queryIndexMarket } from '/@/api/indexdetail/indexDetailApi'
import { rangeSizeMap, rangeOptions, RangeCode, mainIndicatorOptions, subIndicatorOptions, MainIndicatorCode, SubIndicatorCode } from '/@/types/marketoverview/constants'
import { ElMessage } from 'element-plus'



interface RangeMetricItem {
    label: string
    value: string
    className?: string
}

const props = withDefaults(
    defineProps<{
        quote?: MarketQuote
        charts?: ChartGroup[]
        analysis?: QuoteTechnicalAnalysis
        rangePerformance?: IndexRangePerformance
    }>(),
    {
        charts: () => [],
        analysis: () => ({} as QuoteTechnicalAnalysis),
        rangePerformance: () => ({} as IndexRangePerformance)
    }
)

const localProps = reactive({
    charts: props.charts,
    analysis: props.analysis,
    rangePerformance: props.rangePerformance
})

const selectedRange = ref<RangeCode>('6M')
const customStartDate = ref('')
const customEndDate = ref('')
const mainIndicator = ref<MainIndicatorCode>('MA')
const subIndicator = ref<SubIndicatorCode>('VOL')


const rawRows = computed<OHLCData[]>(() => {
    const klineChart = localProps.charts.find(chart => chart.chartType === 'CANDLESTICK')
    const series = klineChart?.series?.[0]
    return (series?.data || []).filter(isOhlcData).sort((a, b) => a.tradeDate.localeCompare(b.tradeDate))
})

const mainIndicatorLabel = computed(() => mainIndicatorOptions.find(item => item.value === mainIndicator.value)?.label || '-')
const subIndicatorLabel = computed(() => subIndicatorOptions.find(item => item.value === subIndicator.value)?.label || '-')
const mainIndicatorParamText = computed(() => {
    const map: Record<MainIndicatorCode, string> = {
        MA: '默认 5、10、20、60、120、250',
        EMA: '默认 12、26、9',
        BOLL: '默认中轨 20，边轨 2',
        SAR: '默认 0.02、0.2',
        VWAP: '默认 1、5、10、20、60、120',
        SUPER_TREND: '默认周期 10，乘数 3'
    }
    return map[mainIndicator.value]
})

const subIndicatorParamText = computed(() => {
    const map: Record<SubIndicatorCode, string> = {
        VOL: '成交量柱状图',
        TURNOVER: '成交额柱状图',
        TURNOVER_RATE: '区间换手率',
        MACD: '默认 12、26、9',
        KDJ: '默认 9、3、3',
        RSI: '默认 14',
        CCI: '默认 14',
        DMI: '默认 14',
        ATR: '默认 14',
        OBV: '默认 14'
    }
    return map[subIndicator.value]
})

const activeRangeText = computed(() => {
    if (selectedRange.value !== 'CUSTOM') return rangeOptions.find(item => item.value === selectedRange.value)?.label || '-'
    return `${customStartDate.value || '开始'} 至 ${customEndDate.value || '结束'}`
})

const klineOption = computed<EChartsOption>(() => buildKlineOption(rawRows.value))

const conclusionTagType = computed(() => {
    const label = localProps.analysis.conclusion?.label || ''
    if (label.includes('强势') || label.includes('偏强') || label.includes('反弹')) return 'success'
    if (label.includes('偏弱') || label.includes('下跌')) return 'danger'
    if (label.includes('警戒') || label.includes('观察')) return 'warning'
    return 'info'
})

const rangeDateText = computed(() => {
    const { startDate, endDate } = localProps.rangePerformance
    if (startDate && endDate) return `${startDate} 至 ${endDate}`
    return activeRangeText.value
})

const rangeSections = computed<Array<{ title: string; items: RangeMetricItem[] }>>(() => {
    const data = localProps.rangePerformance
    return [
        {
            title: '收益风险',
            items: [
                { label: '最高价 / 最低价', value: `${formatNumber(data.riskReturn.highPrice)} / ${formatNumber(data.riskReturn.lowPrice)}` },
                { label: '区间收益率', value: formatPercent(data.riskReturn.periodReturn, 2, true), className: signedClass(data.riskReturn.periodReturn) },
                { label: '年化收益率', value: formatPercent(data.riskReturn.annualizedReturn, 2, true), className: signedClass(data.riskReturn.annualizedReturn) },
                { label: '区间振幅', value: formatPercent(data.riskReturn.amplitude) },
                { label: '波动率', value: formatPercent(data.riskReturn.volatility) },
                { label: '最大回撤', value: formatPercent(data.riskReturn.maxDrawdown), className: 'is-down' },
                { label: '夏普比率', value: formatNumber(data.riskReturn.sharpeRatio) },
                { label: '卡玛比率', value: formatNumber(data.riskReturn.calmarRatio) }
            ]
        },
        {
            title: '成交',
            items: [
                { label: '区间成交额', value: formatAmount(data.trading.periodTurnover) },
                { label: '日均成交额', value: formatAmount(data.trading.averageTurnover) },
                { label: '区间换手率', value: formatPercent(data.trading.periodTurnoverRate) },
                { label: '放量天数', value: `${data.trading.volumeExpansionDays || 0} 天` },
                { label: '最大成交量日', value: data.trading.maxVolumeDate || '-' },
                { label: '成交量相对前一区间变化', value: formatPercent(data.trading.turnoverChangeVsPrevious, 2, true), className: signedClass(data.trading.turnoverChangeVsPrevious) }
            ]
        },
        {
            title: '价格位置与区间结构',
            items: [
                { label: '当前价格分位', value: formatPercent(data.structure.currentPricePercentile) },
                { label: '区间新高次数', value: `${data.structure.newHighCount || 0} 次` },
                { label: '区间新低次数', value: `${data.structure.newLowCount || 0} 次` },
                { label: '价格集中区间', value: data.structure.priceConcentrationRange || '-' },
                { label: '成交密集区间', value: data.structure.turnoverConcentrationRange || '-' }
            ]
        }
    ]
})

function handleRangeChange(value: RangeCode): void {
    if (value === 'CUSTOM') {
        ElMessage.info('暂不支持自定义区间查询，默认使用最近 6 个月数据')
        selectedRange.value = '6M'
    }

    // const latest = parseDate(rawRows.value[rawRows.value.length - 1].tradeDate)
    // if (!customEndDate.value) customEndDate.value = formatDateValue(latest)
    // if (!customStartDate.value) customStartDate.value = formatDateValue(shiftMonths(latest, -6))
    loadMarketData()
}

async function loadMarketData(): Promise<void> {
    if (!props.quote?.fullSymbol) return

    try {
        const marketData = await queryIndexMarket(
            props.quote.fullSymbol,
            props.quote.tradeDate,
            rangeSizeMap[selectedRange.value] || 250
        )
        if (marketData) {
            localProps.charts = marketData.marketCharts
            localProps.analysis = marketData.quoteAnalysis
            localProps.rangePerformance = marketData.rangePerformance
            console.log('行情数据加载成功', marketData)
        }
    } catch (error) {
        console.error('加载行情数据失败', error)
    }
}

function isOhlcData(value: unknown): value is OHLCData {
    return Boolean(value && typeof value === 'object' && 'tradeDate' in value && 'open' in value && 'close' in value)
}

function buildKlineOption(rows: OHLCData[]): EChartsOption {
    // TODO: 显示的指标需要根据 mainIndicator.value 和 subIndicator.value 来动态生成，而不是固定显示所有指标
    const dates = rows.map(item => item.tradeDate)
    // k线图数据格式为 [开盘价, 收盘价, 最低价, 最高价]
    const candleData = rows.map(item => [item.open, item.close, item.low, item.high])
    const mainSeries = buildMainIndicatorSeries()
    const subSeries = buildSubIndicatorSeries()
    const legendData = ['K线', ...mainSeries.map(item => String(item.name)), ...subSeries.map(item => String(item.name))]

    return {
        animation: false,
        axisPointer: {
            link: [
                {
                    xAxisIndex: [0, 1]
                }
            ]
        },
        tooltip: {
            trigger: 'axis',
            axisPointer: {
                type: 'cross'
            },
            formatter(params: any) {
                const list = Array.isArray(params)
                    ? params
                    : [params]

                const dataIndex = list[0]?.dataIndex

                if (
                    dataIndex === undefined ||
                    dataIndex === null
                ) {
                    return ''
                }

                const row = rows[dataIndex]

                if (!row) {
                    return ''
                }

                const mainTooltipItems = mainSeries
                    .map(series => {
                        const value = getSeriesValue(
                            series,
                            dataIndex
                        )

                        if (
                            value === null ||
                            value === undefined
                        ) {
                            return null
                        }

                        return [
                            series.name,
                            formatTooltipValue(value)
                        ].join('：')
                    })
                    .filter(
                        (item): item is string =>
                            item !== null
                    )

                const subTooltipItems = subSeries
                    .map(series => {
                        const value = getSeriesValue(
                            series,
                            dataIndex
                        )

                        if (
                            value === null ||
                            value === undefined
                        ) {
                            return null
                        }

                        return [
                            series.name,
                            formatTooltipValue(value)
                        ].join('：')
                    })
                    .filter(
                        (item): item is string =>
                            item !== null
                    )

                const content: string[] = [
                    `<strong>${row.tradeDate}</strong>`,
                    `开盘：${formatNumber(row.open)}`,
                    `收盘：${formatNumber(row.close)}`,
                    `最低：${formatNumber(row.low)}`,
                    `最高：${formatNumber(row.high)}`
                ]

                if (mainTooltipItems.length) {
                    content.push(
                        '<br/><strong>主图指标</strong>',
                        ...mainTooltipItems
                    )
                }

                if (subTooltipItems.length) {
                    content.push(
                        '<br/><strong>副图指标</strong>',
                        ...subTooltipItems
                    )
                }

                return content.join('<br/>')
            }
        },
        legend: {
            top: 0,
            type: 'scroll',
            data: legendData
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
                axisLine: { lineStyle: { color: '#909399' } },
                axisLabel: { color: '#606266' }
            },
            {
                type: 'category',
                gridIndex: 1,
                data: dates,
                boundaryGap: true,
                min: 'dataMin',
                max: 'dataMax',
                axisLine: { lineStyle: { color: '#909399' } },
                axisLabel: { color: '#606266' }
            }
        ],
        yAxis: [
            {
                scale: true,
                axisLine: { lineStyle: { color: '#909399' } },
                axisLabel: { color: '#606266' },
                splitLine: { lineStyle: { color: '#ebeef5' } }
            },
            {
                scale: true,
                gridIndex: 1,
                axisLabel: {
                    color: '#606266',
                    formatter(value: number) {
                        return formatYAxisValue(value, subIndicator.value)
                    }
                },
                splitLine: { lineStyle: { color: '#ebeef5' } }
            }
        ],
        dataZoom: [
            { type: 'inside', xAxisIndex: [0, 1], start: 0, end: 100 },
            { show: true, type: 'slider', xAxisIndex: [0, 1], bottom: 8, height: 24, start: 0, end: 100 }
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

function buildMainIndicatorSeries(): any[] {

    if (mainIndicator.value === 'MA') {
        return [5, 10, 20, 60, 120, 250].map(
            period => lineSeries(
                `MA${period}`,
                localProps.charts.filter(
                    (chart: ChartGroup) => chart.chartId === `MA${period}`
                )[0]?.series[0]?.data || [],
                0
            )
        )
    }

    if (mainIndicator.value === 'EMA') {
        return [12, 26, 9].map(
            period => lineSeries(
                `EMA${period}`,
                localProps.charts.filter(
                    (chart: ChartGroup) => chart.chartId === `EMA${period}`
                )[0]?.series[0]?.data || [],
                0
            )
        )
    }

    if (mainIndicator.value === 'BOLL') {
        const boll = localProps.charts.filter((chart: ChartGroup) => chart.chartId === 'BOLL')[0]?.series
        return [
            lineSeries('BOLL上轨', boll.filter((series: ChartSeries) => series.name === 'upper')[0]?.data || [], 0),
            lineSeries('BOLL中轨', boll.filter((series: ChartSeries) => series.name === 'mid')[0]?.data || [], 0),
            lineSeries('BOLL下轨', boll.filter((series: ChartSeries) => series.name === 'lower')[0]?.data || [], 0)
        ]
    }

    if (mainIndicator.value === 'SAR') {
        return [
            {
                name: 'SAR(0.02,0.2)',
                type: 'scatter',
                symbolSize: 5,
                data: localProps.charts.filter((chart: ChartGroup) => chart.chartId === 'SAR')[0]?.series[0]?.data || [],
                yAxisIndex: 0,
                xAxisIndex: 0
            }
        ]
    }

    if (mainIndicator.value === 'VWAP') {
        return [1, 5, 10, 20, 60, 120].map(
            period => lineSeries(
                `VWAP${period}`,
                localProps.charts.filter(
                    (chart: ChartGroup) => chart.chartId === `VWAP${period}`
                )[0]?.series[0]?.data || [],
                0
            )
        )
    }

    const superTrend = localProps.charts.filter((chart: ChartGroup) => chart.chartId === 'SUPER_TREND')[0]?.series
    return [
        lineSeries('SuperTrend(10,3)', superTrend[0]?.data || [], 0)
    ]
}

function buildSubIndicatorSeries(): any[] {

    if (subIndicator.value === 'VOL') {
        return [
            barSeries('成交量', localProps.charts.filter((chart: ChartGroup) => chart.chartId === 'VOL')[0]?.series[0]?.data || [])
        ]
    }

    if (subIndicator.value === 'TURNOVER') {
        return [
            barSeries('成交额', localProps.charts.filter((chart: ChartGroup) => chart.chartId === 'TURNOVER')[0]?.series[0]?.data || [])
        ]
    }

    if (subIndicator.value === 'TURNOVER_RATE') {
        return [
            lineSeries('换手率', localProps.charts.filter((chart: ChartGroup) => chart.chartId === 'TURNOVER_RATE')[0]?.series[0]?.data || [], 1)
        ]
    }

    if (subIndicator.value === 'MACD') {
        const macd = localProps.charts.filter((chart: ChartGroup) => chart.chartId === 'MACD')[0]?.series
        return [
            lineSeries('DIF', macd.filter((s: ChartSeries) => s.name === 'DIF')[0]?.data || [], 1),
            lineSeries('DEA', macd.filter((s: ChartSeries) => s.name === 'DEA')[0]?.data || [], 1),
            barSeries('MACD', macd.filter((s: ChartSeries) => s.name === 'MACD')[0]?.data || [])
        ]
    }

    if (subIndicator.value === 'KDJ') {
        const kdj = localProps.charts.filter((chart: ChartGroup) => chart.chartId === 'KDJ')[0]?.series
        return [
            lineSeries('K', kdj.filter((s: ChartSeries) => s.name === 'K')[0]?.data || [], 1),
            lineSeries('D', kdj.filter((s: ChartSeries) => s.name === 'D')[0]?.data || [], 1),
            lineSeries('J', kdj.filter((s: ChartSeries) => s.name === 'J')[0]?.data || [], 1)
        ]
    }

    if (subIndicator.value === 'RSI') {
        return [
            lineSeries('RSI14', localProps.charts.filter((chart: ChartGroup) => chart.chartId === 'RSI')[0]?.series[0]?.data || [], 1)
        ]
    }
    if (subIndicator.value === 'CCI') {
        return [
            lineSeries('CCI14', localProps.charts.filter((chart: ChartGroup) => chart.chartId === 'CCI')[0]?.series[0]?.data || [], 1)
        ]
    }
    if (subIndicator.value === 'ATR') {
        return [
            lineSeries('ATR14', localProps.charts.filter((chart: ChartGroup) => chart.chartId === 'ATR')[0]?.series[0]?.data || [], 1)
        ]
    }
    if (subIndicator.value === 'OBV') {
        const obv = localProps.charts.filter((chart: ChartGroup) => chart.chartId === 'OBV')[0]?.series
        return [
            lineSeries('OBV', obv.filter((series: ChartSeries) => series.name === 'OBV')[0]?.data || [], 1),
            lineSeries('OBV MA14', obv.filter((series: ChartSeries) => series.name === 'OBV14')[0]?.data || [], 1)
        ]
    }

    const dmi = localProps.charts.filter((chart: ChartGroup) => chart.chartId === 'DMI')[0]?.series
    return [
        lineSeries('+DI', dmi.filter((series: ChartSeries) => series.name === '+DI')[0]?.data || [], 1),
        lineSeries('-DI', dmi.filter((series: ChartSeries) => series.name === '-DI')[0]?.data || [], 1),
        lineSeries('ADX', dmi.filter((series: ChartSeries) => series.name === 'ADX')[0]?.data || [], 1)
    ]
}

function lineSeries(name: string, data: Array<any>, yAxisIndex: number): any {
    return {
        name,
        type: 'line',
        data,
        smooth: true,
        showSymbol: false,
        xAxisIndex: yAxisIndex,
        yAxisIndex,
        connectNulls: true,
        lineStyle: { width: 1.4 }
    }
}

function barSeries(name: string, data: Array<any>): any {
    return {
        name,
        type: 'bar',
        xAxisIndex: 1,
        yAxisIndex: 1,
        data: data.map(value => ({
            value,
            itemStyle: { color: Number(value) >= 0 ? '#ef5350' : '#26a69a' }
        }))
    }
}

function formatTooltipValue(value: unknown): string {
    if (Array.isArray(value)) return value.map(item => formatNumber(Number(item))).join(', ')
    if (typeof value === 'number') return formatNumber(value)
    return String(value ?? '-')
}

function formatYAxisValue(value: number, code: SubIndicatorCode): string {
    if (['VOL', 'OBV'].includes(code)) return formatAmount(value)
    if (['TURNOVER_RATE', 'RSI', 'KDJ', 'DMI'].includes(code)) return `${formatNumber(value, 0)}%`
    return formatNumber(value, 1)
}

function normalizeScore(score?: number): number {
    if (score === undefined || score === null || Number.isNaN(Number(score))) return 0
    return Math.max(0, Math.min(100, Math.round(score)))
}

function scoreTagType(score?: number): 'success' | 'warning' | 'danger' | 'info' {
    const value = Number(score || 0)
    if (value >= 70) return 'success'
    if (value >= 50) return 'warning'
    if (value > 0) return 'danger'
    return 'info'
}

function indicatorTagType(direction: string): 'success' | 'warning' | 'danger' | 'info' {
    if (['偏多', '偏强', '支持上涨'].includes(direction)) return 'success'
    if (['偏空'].includes(direction)) return 'danger'
    if (['谨慎', '风险增加', '超卖'].includes(direction)) return 'warning'
    return 'info'
}

function getSeriesValue(
    series: any,
    dataIndex: number
): unknown {
    const value = series.data?.[dataIndex]

    if (
        value !== null &&
        typeof value === 'object' &&
        !Array.isArray(value) &&
        'value' in value
    ) {
        return value.value
    }

    return value
}

</script>

<style scoped>
.quote-detail-page {
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.quote-chart-tab {
    min-height: 520px;
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

.header-title-wrap {
    min-width: 220px;
}

.card-title {
    margin-right: 8px;
    font-weight: 600;
    color: var(--el-text-color-primary);
}

.card-tip,
.param-tip,
.custom-date-label,
.control-label {
    color: var(--el-text-color-secondary);
    font-size: 12px;
}

.toolbar-controls,
.indicator-toolbar,
.indicator-control,
.custom-date-row,
.toolbar-control {
    display: flex;
    align-items: center;
    gap: 10px;
}

.toolbar-controls {
    flex-wrap: wrap;
    justify-content: flex-end;
}

.toolbar-control {
    flex-wrap: wrap;
}

.custom-date-row {
    margin-top: 12px;
}

.date-separator {
    color: var(--el-text-color-secondary);
}

.indicator-toolbar {
    flex-wrap: wrap;
    justify-content: space-between;
    margin-bottom: 12px;
    padding: 10px 12px;
    border-radius: 8px;
    background: #f7f8fa;
}

.indicator-control {
    flex-wrap: wrap;
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
    grid-template-columns: 120px minmax(220px, 1fr) 260px;
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
    grid-template-columns: 44px minmax(110px, 1fr) 48px;
    align-items: center;
    gap: 10px;
    min-height: 26px;
}

.level-label {
    color: var(--el-text-color-regular);
    font-size: 13px;
}

.level-text {
    color: var(--el-text-color-secondary);
    font-size: 12px;
    text-align: right;
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
    .indicator-toolbar,
    .indicator-control,
    .custom-date-row,
    .toolbar-control {
        align-items: flex-start;
        flex-direction: column;
    }

    .indicator-select {
        width: 100%;
    }
}
</style>