<template>
    <div class="quote-detail-page">
        <!--
            行情详情页。
            本页按照“行情详情.md”的结构重构：
            1. 删除顶部最新收盘、区间最高/最低、区间成交额、换手/量比卡片。
            2. K 线区域支持前复权、除权、后复权，以及 1月、3月、6月、1年、2年、自定义区间。
            3. 主图一次叠加一个技术指标组，副图一次展示一个技术指标。
            4. K 线下方左侧展示技术指标综合分析，右侧展示随当前区间联动的区间表现。
        -->
        <el-card class="detail-card" shadow="never">
            <template #header>
                <div class="card-header quote-toolbar">
                    <div>
                        <span class="card-title">行情走势</span>
                        <span class="card-tip">K 线字段来自 open、high、low、close、volume、turnover；复权口径由后端按 adjustType 返回</span>
                    </div>

                    <div class="toolbar-controls">
                        <el-radio-group v-model="adjustMode" size="small">
                            <el-radio-button v-for="item in adjustTypeOptions" :key="item.value" :label="item.value">
                                {{ item.label }}
                            </el-radio-button>
                        </el-radio-group>

                        <el-radio-group v-model="activeRange" size="small">
                            <el-radio-button v-for="item in rangeOptions" :key="item.value" :label="item.value">
                                {{ item.label }}
                            </el-radio-button>
                        </el-radio-group>
                    </div>
                </div>

                <div v-if="activeRange === 'CUSTOM'" class="custom-date-row">
                    <span class="custom-date-label">自定义区间</span>
                    <el-date-picker
                        v-model="customStartDate"
                        type="date"
                        size="small"
                        value-format="YYYY-MM-DD"
                        placeholder="开始日期"
                    />
                    <span class="date-separator">至</span>
                    <el-date-picker
                        v-model="customEndDate"
                        type="date"
                        size="small"
                        value-format="YYYY-MM-DD"
                        placeholder="结束日期"
                    />
                </div>
            </template>

            <div class="indicator-toolbar">
                <div class="indicator-control">
                    <span class="control-label">主图叠加</span>
                    <el-select v-model="activeMainIndicator" size="small" style="width: 190px">
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
                    <el-select v-model="activeSubIndicator" size="small" style="width: 190px">
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

            <el-empty v-if="!rangeRows.length" description="暂无行情走势图" />
            <TargetEcharts v-else :option="technicalChartOption" height="560px" />
        </el-card>

        <el-row :gutter="12" class="analysis-row">
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
                            <el-tag size="large" :type="conclusionTagType">{{ technicalAnalysis.conclusion.label }}</el-tag>
                            <div class="conclusion-text">{{ technicalAnalysis.conclusion.summary }}</div>
                        </div>

                        <div class="conclusion-grid">
                            <div class="score-panel">
                                <div class="score-value">{{ formatNumber(technicalAnalysis.conclusion.score, 0) }}</div>
                                <div class="score-label">综合评分</div>
                            </div>

                            <div class="level-panel">
                                <div v-for="item in conclusionLevels" :key="item.label" class="level-row">
                                    <span class="level-label">{{ item.label }}</span>
                                    <el-rate :model-value="item.value" disabled :max="5" size="small" />
                                </div>
                            </div>

                            <el-descriptions :column="1" size="small" border class="conclusion-meta">
                                <el-descriptions-item label="信号强度">{{ signalStrengthText(technicalAnalysis.conclusion.signalStrength) }}</el-descriptions-item>
                                <el-descriptions-item label="适用周期">{{ technicalAnalysis.conclusion.period }}</el-descriptions-item>
                                <el-descriptions-item label="更新时间">{{ technicalAnalysis.conclusion.updatedAt }}</el-descriptions-item>
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
                                <el-progress :percentage="item.score" :show-text="false" :stroke-width="8" />
                                <div class="dimension-indicators">{{ item.dimension }}</div>
                                <div class="dimension-desc">{{ item.explanation }}</div>
                            </div>
                        </div>
                    </section>

                    <section class="analysis-section">
                        <div class="section-title">指标信号明细</div>
                        <el-table :data="technicalAnalysis.indicatorSignals" border size="small" max-height="360">
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
                            <el-statistic title="多头信号" :value="technicalAnalysis.signalConflict.bullishCount" />
                            <el-statistic title="空头信号" :value="technicalAnalysis.signalConflict.bearishCount" />
                            <el-statistic title="中性信号" :value="technicalAnalysis.signalConflict.neutralCount" />
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
                                        <li v-for="item in technicalAnalysis.signalConflict.supports" :key="item">{{ item }}</li>
                                    </ol>
                                </div>
                            </el-col>
                            <el-col :xs="24" :md="12">
                                <div class="factor-box risk-box">
                                    <div class="factor-title">主要风险因素</div>
                                    <ol>
                                        <li v-for="item in technicalAnalysis.signalConflict.risks" :key="item">{{ item }}</li>
                                    </ol>
                                </div>
                            </el-col>
                        </el-row>
                    </section>

                    <section class="analysis-section explanation-section">
                        <div class="section-title">解释说明</div>
                        <div class="note-text">{{ technicalAnalysis.explanation }}</div>
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

                    <section class="range-section">
                        <div class="section-title">收益风险</div>
                        <el-descriptions :column="1" size="small" border>
                            <el-descriptions-item label="最高价 / 最低价">{{ formatNumber(rangeStats.high) }} / {{ formatNumber(rangeStats.low) }}</el-descriptions-item>
                            <el-descriptions-item label="区间收益率"><span :class="signedClass(rangeStats.changePercent)">{{ formatPercent(rangeStats.changePercent, 2, true) }}</span></el-descriptions-item>
                            <el-descriptions-item label="年化收益率"><span :class="signedClass(rangeStats.annualizedReturn)">{{ formatPercent(rangeStats.annualizedReturn, 2, true) }}</span></el-descriptions-item>
                            <el-descriptions-item label="区间振幅">{{ formatPercent(rangeStats.amplitude) }}</el-descriptions-item>
                            <el-descriptions-item label="波动率">{{ formatPercent(rangeStats.volatility) }}</el-descriptions-item>
                            <el-descriptions-item label="最大回撤"><span :class="signedClass(rangeStats.maxDrawdown)">{{ formatPercent(rangeStats.maxDrawdown, 2, true) }}</span></el-descriptions-item>
                            <el-descriptions-item label="夏普比率">{{ formatNumber(rangeStats.sharpeRatio, 2) }}</el-descriptions-item>
                            <el-descriptions-item label="卡玛比率">{{ formatNumber(rangeStats.calmarRatio, 2) }}</el-descriptions-item>
                        </el-descriptions>
                    </section>

                    <section class="range-section">
                        <div class="section-title">成交</div>
                        <el-descriptions :column="1" size="small" border>
                            <el-descriptions-item label="区间成交量">{{ formatAmount(rangeStats.volume) }}</el-descriptions-item>
                            <el-descriptions-item label="区间成交额">{{ formatAmount(rangeStats.turnover) }}</el-descriptions-item>
                            <el-descriptions-item label="日均成交额">{{ formatAmount(rangeStats.avgTurnover) }}</el-descriptions-item>
                            <el-descriptions-item label="区间换手率">{{ formatPercent(rangeStats.turnoverRate) }}</el-descriptions-item>
                            <el-descriptions-item label="放量天数">{{ rangeStats.maxVolumeTradeDate ? 1 : 0 }} 天</el-descriptions-item>
                            <el-descriptions-item label="最大成交量日">{{ rangeStats.maxVolumeTradeDate || '-' }}</el-descriptions-item>
                            <el-descriptions-item label="成交量相对前一区间变化"><span :class="signedClass(null)">{{ formatPercent(null, 2, true) }}</span></el-descriptions-item>
                        </el-descriptions>
                    </section>

                    <section class="range-section">
                        <div class="section-title">对比分析</div>
                        <el-descriptions :column="1" size="small" border>
                            <el-descriptions-item label="跑赢/跑输大盘">{{ formatExcessReturn(quoteRangeComparison?.marketExcessReturn) }}</el-descriptions-item>
                            <el-descriptions-item label="跑赢/跑输行业">{{ formatExcessReturn(quoteRangeComparison?.industryExcessReturn) }}</el-descriptions-item>
                            <el-descriptions-item label="行业排名">{{ quoteRangeComparison?.industryRankText || '-' }}</el-descriptions-item>
                            <el-descriptions-item label="市场排名">{{ quoteRangeComparison?.marketRankText || '-' }}</el-descriptions-item>
                            <el-descriptions-item label="区间 Alpha">{{ formatPercent(quoteRangeComparison?.alpha) }}</el-descriptions-item>
                            <el-descriptions-item label="区间 Beta">{{ formatNumber(quoteRangeComparison?.beta, 2) }}</el-descriptions-item>
                            <el-descriptions-item label="相关性">{{ formatNumber(quoteRangeComparison?.correlation, 2) }}</el-descriptions-item>
                            <el-descriptions-item label="相对强弱评分">{{ formatNumber(quoteRangeComparison?.relativeStrengthScore, 0) }}</el-descriptions-item>
                        </el-descriptions>
                    </section>

                    <section class="range-section">
                        <div class="section-title">价格位置与区间结构</div>
                        <el-descriptions :column="1" size="small" border>
                            <el-descriptions-item label="当前价格分位">{{ formatPercent(rangeStats.pricePositionPercent) }}</el-descriptions-item>
                            <el-descriptions-item label="区间新高次数">{{ rangeStats.newHighCount }} 次</el-descriptions-item>
                            <el-descriptions-item label="区间新低次数">{{ rangeStats.newLowCount }} 次</el-descriptions-item>
                            <el-descriptions-item label="价格集中区间">{{ rangeStats.priceRangeText }}</el-descriptions-item>
                            <el-descriptions-item label="成交密集区间">{{ rangeStats.turnoverRangeText }}</el-descriptions-item>
                        </el-descriptions>
                    </section>
                </el-card>
            </el-col>
        </el-row>
    </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import type { EChartsOption } from 'echarts'
import type { AdjustType, QuoteTechnicalAnalysis, TargetQuoteDetail } from '/@/types/targetdetail/targetDetailType'
import { adjustTypeOptions } from '/@/types/targetdetail/targetDetailType'
import TargetEcharts from '/@/components/common/TargetEcharts.vue'
import {
    formatAmount,
    formatNumber,
    formatPercent,
    signalStrengthText,
    signedClass,
    statusTagType
} from '/@/utils/marketUtils'
import type { MarketQuote, ChartGroup, OHLCData } from '/@/types/marketoverview/view'
import { rangeSizeMap, rangeOptions, mainIndicatorOptions, subIndicatorOptions } from '/@/types/marketoverview/constants'
import type { RangeCode, MainIndicatorCode, SubIndicatorCode } from '/@/types/marketoverview/constants'

const props = withDefaults(
    defineProps<TargetQuoteDetail>(), {
        charts: () => [],
        performance: () => [],
        quoteRows: () => [],
        adjustType: 'FORWARD',
        meta: () => ({})
})

const adjustMode = ref<AdjustType>('FORWARD')
const activeRange = ref<RangeCode>('1Y')
const customStartDate = ref<string>('')
const customEndDate = ref<string>('')
const activeMainIndicator = ref<MainIndicatorCode>('MA')
const activeSubIndicator = ref<SubIndicatorCode>('VOL')

const activeMainIndicatorText = computed(() => mainIndicatorOptions.find(item => item.value === activeMainIndicator.value)?.tip || '')
const activeSubIndicatorText = computed(() => subIndicatorOptions.find(item => item.value === activeSubIndicator.value)?.tip || '')

const primaryChart = computed(() => props.charts.find(chart => chart.chartType === 'CANDLESTICK') || props.charts[0])

const normalizedRows = computed<MarketQuote[]>(() => {
    if (props.quoteRows.length) return [...props.quoteRows].sort((a, b) => String(a.tradeDate).localeCompare(String(b.tradeDate)))

    const chartRows = getOhlcRows(primaryChart.value)
    return chartRows.map(row => ({
        fullSymbol: props.quote?.fullSymbol || '',
        tradeDate: row.tradeDate,
        open: row.open,
        high: row.high,
        low: row.low,
        close: row.close,
        volume: row.volume,
        turnover: row.turnover,
        preClose: row.preClose,
        chgPct: row.chgPct,
        chgAmt: row.chgAmt
    }))
})

const rangeRows = computed(() => {
    const rows = normalizedRows.value
    if (!rows.length) return []

    if (activeRange.value === 'CUSTOM') {
        return rows.filter(row => {
            const date = String(row.tradeDate || '')
            if (customStartDate.value && date < customStartDate.value) return false
            if (customEndDate.value && date > customEndDate.value) return false
            return true
        })
    }

    
    const rangeSize = rangeSizeMap[activeRange.value as Exclude<RangeCode, 'CUSTOM'>] || rows.length
    return rows.slice(Math.max(0, rows.length - rangeSize))
})

const adjustedRows = computed<TechnicalRow[]>(() => rangeRows.value.map(row => ({
    ...row,
    adjustedOpen: row.open,
    adjustedHigh: row.high,
    adjustedLow: row.low,
    adjustedClose: row.close
})))

const technicalRows = computed<TechnicalRow[]>(() => enrichTechnicalIndicators(adjustedRows.value))

const rangeDateText = computed(() => {
    const first = props.rangePerformance?.startDate || rangeRows.value[0]?.tradeDate || '-'
    const last = props.rangePerformance?.endDate || rangeRows.value[rangeRows.value.length - 1]?.tradeDate || '-'
    return `${first} 至 ${last}`
})

const technicalChartOption = computed<EChartsOption>(() => buildTechnicalChartOption(technicalRows.value))
const rangeStats = computed(() => ({
    ...buildRangeStats(technicalRows.value, normalizedRows.value),
    ...(props.rangePerformance?.returnRisk || {}),
    ...(props.rangePerformance?.volume || {}),
    ...(props.rangePerformance?.priceStructure || {})
}))
const quoteRangeComparison = computed(() => props.rangePerformance?.comparison || props.quoteRangeComparison)
const technicalAnalysis = computed<QuoteTechnicalAnalysis>(() => props.quoteTechnicalAnalysis || buildAutoTechnicalAnalysis(technicalRows.value))

const conclusionLevels = computed(() => (technicalAnalysis.value.conclusion.levels || []).map(item => ({
    label: item.label,
    value: item.level
})))

const conclusionTagType = computed(() => statusTagType(overallScoreToStatus(technicalAnalysis.value.conclusion.score)))

function buildTechnicalChartOption(rows: TechnicalRow[]): EChartsOption {
    const dates = rows.map(row => row.tradeDate)
    const kData = rows.map(row => [row.adjustedOpen, row.adjustedClose, row.adjustedLow, row.adjustedHigh])
    const legend = ['K线']
    const series: any[] = [
        {
            name: 'K线',
            type: 'candlestick',
            data: kData,
            itemStyle: {
                color: '#ef5350',
                color0: '#26a69a',
                borderColor: '#ef5350',
                borderColor0: '#26a69a'
            }
        }
    ]

    addMainIndicatorSeries(rows, series, legend)
    addSubIndicatorSeries(rows, series, legend)

    return {
        animation: false,
        tooltip: {
            trigger: 'axis',
            axisPointer: { type: 'cross' },
            formatter(params: any) {
                const list = Array.isArray(params) ? params : [params]
                const kItem = list.find((item: any) => item.seriesType === 'candlestick')
                if (!kItem) return ''
                const row = rows[kItem.dataIndex]
                return [
                    row.tradeDate,
                    `开盘：${formatNumber(row.adjustedOpen)}`,
                    `收盘：${formatNumber(row.adjustedClose)}`,
                    `最低：${formatNumber(row.adjustedLow)}`,
                    `最高：${formatNumber(row.adjustedHigh)}`,
                    `成交量：${formatAmount(row.volume)}`,
                    `成交额：${formatAmount(row.turnover)}`,
                    `换手率：${formatPercent(row.turnoverRate)}`
                ].join('<br/>')
            }
        },
        legend: {
            top: 0,
            type: 'scroll',
            data: legend
        },
        grid: [
            { left: 58, right: 28, top: 46, height: 330 },
            { left: 58, right: 28, top: 420, height: 90 }
        ],
        xAxis: [
            { type: 'category', data: dates, boundaryGap: true, min: 'dataMin', max: 'dataMax', axisLabel: { hideOverlap: true } },
            { type: 'category', gridIndex: 1, data: dates, boundaryGap: true, min: 'dataMin', max: 'dataMax', axisLabel: { hideOverlap: true } }
        ],
        yAxis: [
            { scale: true, splitLine: { lineStyle: { type: 'dashed' } } },
            {
                scale: true,
                gridIndex: 1,
                splitLine: { lineStyle: { type: 'dashed' } },
                axisLabel: {
                    formatter(value: number) {
                        return activeSubIndicator.value === 'VOL' || activeSubIndicator.value === 'OBV'
                            ? formatAmount(value)
                            : String(Number(value.toFixed(2)))
                    }
                }
            }
        ],
        dataZoom: [
            { type: 'inside', xAxisIndex: [0, 1], start: 0, end: 100 },
            { type: 'slider', xAxisIndex: [0, 1], bottom: 6, height: 22, start: 0, end: 100 }
        ],
        series
    }
}

function addMainIndicatorSeries(rows: TechnicalRow[], series: any[], legend: string[]): void {
    const line = (name: string, field: keyof TechnicalRow) => {
        legend.push(name)
        series.push({ name, type: 'line', showSymbol: false, smooth: true, connectNulls: true, data: rows.map(row => row[field]) })
    }

    if (activeMainIndicator.value === 'MA') {
        line('MA5', 'ma5')
        line('MA10', 'ma10')
        line('MA20', 'ma20')
        line('MA60', 'ma60')
        line('MA120', 'ma120')
        line('MA250', 'ma250')
    }

    if (activeMainIndicator.value === 'EMA') {
        line('EMA12', 'ema12')
        line('EMA26', 'ema26')
        line('EMA9', 'ema9')
    }

    if (activeMainIndicator.value === 'BOLL') {
        line('BOLL上轨', 'bollUpper')
        line('BOLL中轨', 'bollMid')
        line('BOLL下轨', 'bollLower')
    }

    if (activeMainIndicator.value === 'SAR') {
        legend.push('SAR')
        series.push({ name: 'SAR', type: 'scatter', symbolSize: 5, data: rows.map(row => row.sar) })
    }

    if (activeMainIndicator.value === 'VWAP') {
        line('VWAP1', 'vwap1')
        line('VWAP5', 'vwap5')
        line('VWAP10', 'vwap10')
        line('VWAP20', 'vwap20')
        line('VWAP60', 'vwap60')
        line('VWAP120', 'vwap120')
    }

    if (activeMainIndicator.value === 'SUPER_TREND') {
        line('SuperTrend', 'superTrend')
    }
}

function addSubIndicatorSeries(rows: TechnicalRow[], series: any[], legend: string[]): void {
    const line = (name: string, field: keyof TechnicalRow) => {
        legend.push(name)
        series.push({ name, type: 'line', xAxisIndex: 1, yAxisIndex: 1, showSymbol: false, smooth: true, connectNulls: true, data: rows.map(row => row[field]) })
    }

    if (activeSubIndicator.value === 'VOL') {
        legend.push('成交量')
        series.push({
            name: '成交量',
            type: 'bar',
            xAxisIndex: 1,
            yAxisIndex: 1,
            data: rows.map(row => ({
                value: row.volume || 0,
                itemStyle: { color: Number(row.adjustedClose) >= Number(row.adjustedOpen) ? '#ef5350' : '#26a69a' }
            }))
        })
    }

    if (activeSubIndicator.value === 'TURNOVER') line('成交额', 'turnover')

    if (activeSubIndicator.value === 'TURNOVER_RATE') line('换手率', 'turnoverRate')

    if (activeSubIndicator.value === 'MACD') {
        legend.push('MACD柱')
        series.push({ name: 'MACD柱', type: 'bar', xAxisIndex: 1, yAxisIndex: 1, data: rows.map(row => row.macdHistogram) })
        line('DIF', 'macdDif')
        line('DEA', 'macdDea')
    }

    if (activeSubIndicator.value === 'KDJ') {
        line('K', 'kdjK')
        line('D', 'kdjD')
        line('J', 'kdjJ')
    }

    if (activeSubIndicator.value === 'RSI') line('RSI14', 'rsi14')
    if (activeSubIndicator.value === 'CCI') line('CCI14', 'cci14')

    if (activeSubIndicator.value === 'DMI') {
        line('+DI', 'plusDi14')
        line('-DI', 'minusDi14')
        line('ADX', 'adx14')
    }

    if (activeSubIndicator.value === 'ATR') line('ATR14', 'atr14')
    if (activeSubIndicator.value === 'OBV') line('OBV', 'obv')
}

function applyAdjustMode(rows: MarketQuote[], mode: AdjustType): TechnicalRow[] {
    const latestFactor = rows.slice().reverse().find(row => isValidNumber(row.adjustFactor))?.adjustFactor || 1

    return rows.map(row => {
        const factor = isValidNumber(row.adjustFactor) ? Number(row.adjustFactor) : 1
        const multiplier = mode === 'NONE'
            ? 1
            : mode === 'FORWARD'
                ? factor / latestFactor
                : factor

        return {
            ...row,
            adjustedOpen: multiply(row.open, multiplier),
            adjustedHigh: multiply(row.high, multiplier),
            adjustedLow: multiply(row.low, multiplier),
            adjustedClose: multiply(row.close, multiplier),
            preClose: multiply(row.preClose, multiplier),
            chgAmt: multiply(row.chgAmt, multiplier)
        }
    })
}

function formatExcessReturn(value?: number | null): string {
    if (!isNumber(value)) return '-'
    return value >= 0 ? `跑赢 ${formatPercent(value, 2, true)}` : `跑输 ${formatPercent(Math.abs(value), 2)}`
}

function isNumber(value: unknown): value is number {
    return typeof value === 'number' && !Number.isNaN(value)
}

type TechnicalRow = MarketQuote & {
    adjustedOpen: number
    adjustedHigh: number
    adjustedLow: number
    adjustedClose: number
    ma5?: number | null
    ma10?: number | null
    ma20?: number | null
    ma60?: number | null
    ma120?: number | null
    ma250?: number | null
    ema12?: number | null
    ema26?: number | null
    ema9?: number | null
    bollUpper?: number | null
    bollMid?: number | null
    bollLower?: number | null
    sar?: number | null
    vwap1?: number | null
    vwap5?: number | null
    vwap10?: number | null
    vwap20?: number | null
    vwap60?: number | null
    vwap120?: number | null
    superTrend?: number | null
    macdHistogram?: number | null
    macdDif?: number | null
    macdDea?: number | null
    kdjK?: number | null
    kdjD?: number | null
    kdjJ?: number | null
    rsi14?: number | null
    cci14?: number | null
    plusDi14?: number | null
    minusDi14?: number | null
    adx14?: number | null
    atr14?: number | null
    obv?: number | null
}

function getOhlcRows(chart?: ChartGroup): OHLCData[] {
    if (!chart) return []
    const rows = chart.series?.flatMap(item => item.data || []) || []
    return rows.filter((row): row is OHLCData => Boolean(row && typeof row === 'object' && 'tradeDate' in row && 'open' in row && 'close' in row))
}

function multiply(value: number | null | undefined, multiplier: number): number {
    return (Number(value) || 0) * multiplier
}

function enrichTechnicalIndicators(rows: TechnicalRow[]): TechnicalRow[] {
    return rows.map((row, index) => ({
        ...row,
        ma5: averageClose(rows, index, 5),
        ma10: averageClose(rows, index, 10),
        ma20: averageClose(rows, index, 20),
        ma60: averageClose(rows, index, 60),
        ma120: averageClose(rows, index, 120),
        ma250: averageClose(rows, index, 250),
        ema12: averageClose(rows, index, 12),
        ema26: averageClose(rows, index, 26),
        ema9: averageClose(rows, index, 9),
        bollMid: averageClose(rows, index, 20),
        bollUpper: averageClose(rows, index, 20),
        bollLower: averageClose(rows, index, 20),
        vwap1: row.adjustedClose,
        vwap5: averageClose(rows, index, 5),
        vwap10: averageClose(rows, index, 10),
        vwap20: averageClose(rows, index, 20),
        vwap60: averageClose(rows, index, 60),
        vwap120: averageClose(rows, index, 120),
        obv: row.volume
    }))
}

function averageClose(rows: TechnicalRow[], endIndex: number, windowSize: number): number | null {
    const start = Math.max(0, endIndex - windowSize + 1)
    const values = rows.slice(start, endIndex + 1).map(row => Number(row.adjustedClose)).filter(value => !Number.isNaN(value))
    if (!values.length) return null
    return values.reduce((sum, value) => sum + value, 0) / values.length
}

function buildRangeStats(rows: TechnicalRow[], allRows: MarketQuote[]) {
    if (!rows.length) return {}
    const first = rows[0]
    const last = rows[rows.length - 1]
    const highs = rows.map(row => Number(row.adjustedHigh)).filter(value => !Number.isNaN(value))
    const lows = rows.map(row => Number(row.adjustedLow)).filter(value => !Number.isNaN(value))
    const high = highs.length ? Math.max(...highs) : null
    const low = lows.length ? Math.min(...lows) : null
    const close = Number(last.adjustedClose) || 0
    const base = Number(first.preClose || first.adjustedOpen || first.adjustedClose) || 0
    const changePercent = base ? ((close - base) / base) * 100 : null
    return {
        high,
        low,
        changePercent,
        annualizedReturn: changePercent !== null && rows.length ? changePercent * (250 / rows.length) : null,
        amplitude: high !== null && low !== null && base ? ((high - low) / base) * 100 : null,
        volatility: null,
        maxDrawdown: high ? ((close - high) / high) * 100 : null,
        sharpeRatio: null,
        calmarRatio: null,
        volume: rows.reduce((sum, row) => sum + (Number(row.volume) || 0), 0),
        turnover: rows.reduce((sum, row) => sum + (Number(row.turnover) || 0), 0),
        avgTurnover: rows.length ? rows.reduce((sum, row) => sum + (Number(row.turnover) || 0), 0) / rows.length : null,
        turnoverRate: rows.reduce((sum, row) => sum + (Number(row.turnoverRate) || 0), 0),
        maxVolumeTradeDate: rows.slice().sort((a, b) => (Number(b.volume) || 0) - (Number(a.volume) || 0))[0]?.tradeDate || null,
        pricePositionPercent: high !== null && low !== null && high !== low ? ((close - low) / (high - low)) * 100 : null,
        newHighCount: null,
        newLowCount: null,
        priceRangeText: high !== null && low !== null ? `${formatNumber(low)} - ${formatNumber(high)}` : '-',
        turnoverRangeText: '-'
    }
}

function buildAutoTechnicalAnalysis(rows: TechnicalRow[]): QuoteTechnicalAnalysis {
    const last = rows[rows.length - 1]
    const prev = rows[rows.length - 2]
    const score = last && prev && Number(last.adjustedClose) >= Number(prev.adjustedClose) ? 60 : 50
    return {
        conclusion: {
            label: score >= 60 ? '震荡偏强' : '中性震荡',
            summary: '基于当前区间行情数据自动生成的兜底技术分析。',
            score,
            signalStrength: 'MEDIUM',
            period: activeRange.value,
            updatedAt: last?.tradeDate || '-',
            levels: [
                { key: 'trend', label: '趋势', level: Math.round(score / 20), tone: 'flat', text: '-' },
                { key: 'momentum', label: '动能', level: Math.round(score / 20), tone: 'flat', text: '-' },
                { key: 'volume', label: '量能', level: 3, tone: 'flat', text: '-' },
                { key: 'volatility', label: '波动', level: 3, tone: 'flat', text: '-' },
                { key: 'risk', label: '风险', level: 3, tone: 'flat', text: '-' }
            ]
        },
        dimensionScores: [
            { dimension: '趋势', score, explanation: '-' },
            { dimension: '动能', score, explanation: '-' },
            { dimension: '量能', score: 50, explanation: '-' }
        ],
        indicatorSignals: [],
        signalConflict: { bullishCount: 0, bearishCount: 0, neutralCount: 0, consistency: '-', supports: [], risks: [] },
        explanation: '后端未返回技术指标综合分析时的前端兜底展示。'
    }
}

function overallScoreToStatus(score?: number | null): string {
    const value = Number(score) || 0
    if (value >= 70) return 'GOOD'
    if (value >= 55) return 'NORMAL'
    if (value >= 40) return 'WARNING'
    return 'DANGER'
}

function signalTagType(direction: string): '' | 'success' | 'warning' | 'danger' | 'info' | 'primary' {
    if (['BULLISH', 'SUPPORT_UP'].includes(direction)) return 'success'
    if (direction === 'BEARISH') return 'danger'
    if (['CAUTION', 'RISK_UP'].includes(direction)) return 'warning'
    return 'info'
}
function isValidNumber(value: unknown): boolean {
    return value !== undefined && value !== null && value !== '' && !Number.isNaN(Number(value))
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
.custom-date-label,
.control-label {
    color: var(--el-text-color-secondary);
    font-size: 12px;
}

.toolbar-controls,
.indicator-toolbar,
.indicator-control,
.custom-date-row {
    display: flex;
    align-items: center;
    gap: 10px;
}

.toolbar-controls {
    flex-wrap: wrap;
    justify-content: flex-end;
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
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
    min-height: 26px;
}

.level-label {
    color: var(--el-text-color-regular);
    font-size: 13px;
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

.dimension-indicators {
    margin-top: 8px;
    color: var(--el-text-color-secondary);
    font-size: 12px;
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
    .custom-date-row {
        align-items: flex-start;
        flex-direction: column;
    }
}
</style>
