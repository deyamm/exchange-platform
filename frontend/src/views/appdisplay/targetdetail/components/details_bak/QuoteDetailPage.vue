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
                        <span class="card-tip">K 线字段来自 open、high、low、close、volume、turnover；复权基于 adjustFactor 计算</span>
                    </div>

                    <div class="toolbar-controls">
                        <el-radio-group v-model="adjustMode" size="small">
                            <el-radio-button label="FRONT_ADJUST">前复权</el-radio-button>
                            <el-radio-button label="EX_RIGHT">除权</el-radio-button>
                            <el-radio-button label="BACK_ADJUST">后复权</el-radio-button>
                        </el-radio-group>

                        <el-radio-group v-model="activeRange" size="small">
                            <el-radio-button label="1M">1月</el-radio-button>
                            <el-radio-button label="3M">3月</el-radio-button>
                            <el-radio-button label="6M">6月</el-radio-button>
                            <el-radio-button label="1Y">1年</el-radio-button>
                            <el-radio-button label="2Y">2年</el-radio-button>
                            <el-radio-button label="CUSTOM">自定义</el-radio-button>
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
                            <el-tag size="large" :type="conclusionTagType">{{ technicalAnalysis.conclusion.overallTag }}</el-tag>
                            <div class="conclusion-text">{{ technicalAnalysis.conclusion.overallJudgement }}</div>
                        </div>

                        <div class="conclusion-grid">
                            <div class="score-panel">
                                <div class="score-value">{{ formatNumber(technicalAnalysis.conclusion.overallScore, 0) }}</div>
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
                                <el-descriptions-item label="适用周期">{{ technicalAnalysis.conclusion.applicablePeriod }}</el-descriptions-item>
                                <el-descriptions-item label="更新时间">{{ technicalAnalysis.conclusion.updatedTime }}</el-descriptions-item>
                            </el-descriptions>
                        </div>
                    </section>

                    <section class="analysis-section">
                        <div class="section-title">多维评分</div>
                        <div class="dimension-grid">
                            <div
                                v-for="item in technicalAnalysis.dimensionScores"
                                :key="item.dimensionCode"
                                class="dimension-card"
                            >
                                <div class="dimension-title-row">
                                    <span class="dimension-title">{{ item.dimensionName }}</span>
                                    <span class="dimension-score">{{ formatNumber(item.score, 0) }}</span>
                                </div>
                                <el-progress :percentage="item.score" :show-text="false" :stroke-width="8" />
                                <div class="dimension-indicators">{{ item.indicators.join('、') }}</div>
                                <div class="dimension-desc">{{ item.explanation }}</div>
                            </div>
                        </div>
                    </section>

                    <section class="analysis-section">
                        <div class="section-title">指标信号明细</div>
                        <el-table :data="technicalAnalysis.indicatorSignals" border size="small" max-height="360">
                            <el-table-column prop="indicatorName" label="指标" width="120" />
                            <el-table-column prop="currentStatus" label="当前状态" min-width="170" />
                            <el-table-column label="信号方向" width="110" align="center">
                                <template #default="{ row }">
                                    <el-tag size="small" :type="signalTagType(row.signalDirection)">
                                        {{ row.signalDirectionText }}
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
                                        <li v-for="item in technicalAnalysis.signalConflict.mainSupportFactors" :key="item">{{ item }}</li>
                                    </ol>
                                </div>
                            </el-col>
                            <el-col :xs="24" :md="12">
                                <div class="factor-box risk-box">
                                    <div class="factor-title">主要风险因素</div>
                                    <ol>
                                        <li v-for="item in technicalAnalysis.signalConflict.mainRiskFactors" :key="item">{{ item }}</li>
                                    </ol>
                                </div>
                            </el-col>
                        </el-row>
                    </section>

                    <section class="analysis-section explanation-section">
                        <div class="section-title">解释说明</div>
                        <div class="note-text">{{ technicalAnalysis.conclusion.explanation }}</div>
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
                            <el-descriptions-item label="区间收益率"><span :class="signedClass(rangeStats.intervalReturn)">{{ formatPercent(rangeStats.intervalReturn, 2, true) }}</span></el-descriptions-item>
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
                            <el-descriptions-item label="日均成交额">{{ formatAmount(rangeStats.avgDailyTurnover) }}</el-descriptions-item>
                            <el-descriptions-item label="区间换手率">{{ formatPercent(rangeStats.turnoverRate) }}</el-descriptions-item>
                            <el-descriptions-item label="放量天数">{{ rangeStats.volumeExpansionDays }} 天</el-descriptions-item>
                            <el-descriptions-item label="最大成交量日">{{ rangeStats.maxVolumeDate || '-' }}</el-descriptions-item>
                            <el-descriptions-item label="成交量相对前一区间变化"><span :class="signedClass(rangeStats.volumeVsPreviousRangeChange)">{{ formatPercent(rangeStats.volumeVsPreviousRangeChange, 2, true) }}</span></el-descriptions-item>
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
                            <el-descriptions-item label="当前价格分位">{{ formatPercent(rangeStats.currentPricePercentile) }}</el-descriptions-item>
                            <el-descriptions-item label="区间新高次数">{{ rangeStats.newHighCount }} 次</el-descriptions-item>
                            <el-descriptions-item label="区间新低次数">{{ rangeStats.newLowCount }} 次</el-descriptions-item>
                            <el-descriptions-item label="价格集中区间">{{ rangeStats.priceDenseRange }}</el-descriptions-item>
                            <el-descriptions-item label="成交密集区间">{{ rangeStats.turnoverDenseRange }}</el-descriptions-item>
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
import type {
    QuoteAdjustMode,
    QuoteMainIndicatorCode,
    QuoteRangeComparison,
    QuoteSignalDirection,
    QuoteSubIndicatorCode,
    QuoteTechnicalAnalysis,
    QuoteTechnicalLevel,
    QuoteTechnicalSignal
} from '/@/types/targetdetail/targetDetailType'
import TargetEcharts from '/@/components/common/TargetEcharts.vue'
import {
    formatAmount,
    formatDateTime,
    formatNumber,
    formatPercent,
    signalStrengthText,
    signedClass,
    statusTagType
} from '/@/utils/marketUtils'
import type { MarketQuote, ChartGroup, TargetPerformanceInterval, OHLCData } from '/@/types/marketoverview/view'
import { rangeSizeMap } from '/@/types/marketoverview/constants'

const props = withDefaults(defineProps<{
    /** 当前最新行情摘要。 */
    quote?: MarketQuote
    /** 原有图表组，兼容旧接口；当 quoteRows 为空时作为降级数据源。 */
    charts?: ChartGroup[]
    /** 原有区间表现，保留兼容，不作为本页主要展示数据源。 */
    performance?: TargetPerformanceInterval[]
    /** 后端按物理表字段返回的行情明细。 */
    quoteRows?: MarketQuote[]
    /** 后端已计算的技术指标综合分析；为空时前端基于 quoteRows 自动生成。 */
    quoteTechnicalAnalysis?: QuoteTechnicalAnalysis
    /** 后端已计算的区间对比分析；当前物理表无法直接提供时允许为空。 */
    quoteRangeComparison?: QuoteRangeComparison
}>(), {
    charts: () => [],
    performance: () => [],
    quoteRows: () => []
})

interface TechnicalRow extends MarketQuote {
    /** 前端复权后的开盘价。 */
    adjustedOpen?: number | null
    /** 前端复权后的最高价。 */
    adjustedHigh?: number | null
    /** 前端复权后的最低价。 */
    adjustedLow?: number | null
    /** 前端复权后的收盘价。 */
    adjustedClose?: number | null
    /** MA5。 */
    ma5?: number | null
    /** MA10。 */
    ma10?: number | null
    /** MA20。 */
    ma20?: number | null
    /** MA60。 */
    ma60?: number | null
    /** MA120。 */
    ma120?: number | null
    /** MA250。 */
    ma250?: number | null
    /** EMA9。 */
    ema9?: number | null
    /** EMA12。 */
    ema12?: number | null
    /** EMA26。 */
    ema26?: number | null
    /** BOLL 中轨。 */
    bollMid?: number | null
    /** BOLL 上轨。 */
    bollUpper?: number | null
    /** BOLL 下轨。 */
    bollLower?: number | null
    /** SAR 抛物线点位。 */
    sar?: number | null
    /** VWAP1。 */
    vwap1?: number | null
    /** VWAP5。 */
    vwap5?: number | null
    /** VWAP10。 */
    vwap10?: number | null
    /** VWAP20。 */
    vwap20?: number | null
    /** VWAP60。 */
    vwap60?: number | null
    /** VWAP120。 */
    vwap120?: number | null
    /** SuperTrend 趋势线。 */
    superTrend?: number | null
    /** SuperTrend 方向，1 为多头，-1 为空头。 */
    superTrendDirection?: number | null
    /** MACD DIF。 */
    macdDif?: number | null
    /** MACD DEA。 */
    macdDea?: number | null
    /** MACD 柱。 */
    macdHistogram?: number | null
    /** KDJ K。 */
    kdjK?: number | null
    /** KDJ D。 */
    kdjD?: number | null
    /** KDJ J。 */
    kdjJ?: number | null
    /** RSI14。 */
    rsi14?: number | null
    /** CCI14。 */
    cci14?: number | null
    /** DMI +DI。 */
    plusDi14?: number | null
    /** DMI -DI。 */
    minusDi14?: number | null
    /** DMI ADX。 */
    adx14?: number | null
    /** ATR14。 */
    atr14?: number | null
    /** OBV。 */
    obv?: number | null
}

const adjustMode = ref<QuoteAdjustMode>('FRONT_ADJUST')
const activeRange = ref<'1M' | '3M' | '6M' | '1Y' | '2Y' | 'CUSTOM'>('1Y')
const customStartDate = ref<string>('')
const customEndDate = ref<string>('')
const activeMainIndicator = ref<QuoteMainIndicatorCode>('MA')
const activeSubIndicator = ref<QuoteSubIndicatorCode>('VOLUME')

const mainIndicatorOptions: Array<{ value: QuoteMainIndicatorCode; label: string; tip: string }> = [
    { value: 'MA', label: 'MA 均线', tip: '默认 5、10、20、60、120、250' },
    { value: 'EMA', label: 'EMA 指数均线', tip: '默认 12、26、9' },
    { value: 'BOLL', label: 'BOLL 布林带', tip: '默认中轨 20、边轨 2' },
    { value: 'SAR', label: 'SAR 抛物线转向', tip: '默认 0.02、0.2' },
    { value: 'VWAP', label: 'VWAP 成交量加权均价', tip: '默认 1、5、10、20、60、120' },
    { value: 'SUPER_TREND', label: 'SuperTrend', tip: '默认周期 10、乘数 3' }
]

const subIndicatorOptions: Array<{ value: QuoteSubIndicatorCode; label: string; tip: string }> = [
    { value: 'VOLUME', label: '成交量', tip: '成交量柱图' },
    { value: 'TURNOVER_RATE', label: '换手率', tip: '来自 turnoverRate' },
    { value: 'MACD', label: 'MACD', tip: '默认 12、26、9' },
    { value: 'KDJ', label: 'KDJ', tip: '默认 9、3、3' },
    { value: 'RSI', label: 'RSI', tip: '默认 14' },
    { value: 'CCI', label: 'CCI', tip: '默认 14' },
    { value: 'DMI', label: 'DMI', tip: '默认 14' },
    { value: 'ATR', label: 'ATR', tip: '默认 14' },
    { value: 'OBV', label: 'OBV', tip: '能量潮指标' }
]

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

    
    const rangeSize = rangeSizeMap[activeRange.value] || rows.length
    return rows.slice(Math.max(0, rows.length - rangeSize))
})

const adjustedRows = computed<TechnicalRow[]>(() => applyAdjustMode(rangeRows.value, adjustMode.value))
const technicalRows = computed<TechnicalRow[]>(() => enrichTechnicalIndicators(adjustedRows.value))
const latestRow = computed(() => technicalRows.value[technicalRows.value.length - 1])
const previousRow = computed(() => technicalRows.value[technicalRows.value.length - 2])

const rangeDateText = computed(() => {
    const first = rangeRows.value[0]?.tradeDate || '-'
    const last = rangeRows.value[rangeRows.value.length - 1]?.tradeDate || '-'
    return `${first} 至 ${last}`
})

const technicalChartOption = computed<EChartsOption>(() => buildTechnicalChartOption(technicalRows.value))
const rangeStats = computed(() => buildRangeStats(technicalRows.value, normalizedRows.value))
const technicalAnalysis = computed<QuoteTechnicalAnalysis>(() => props.quoteTechnicalAnalysis || buildAutoTechnicalAnalysis(technicalRows.value))

const conclusionLevels = computed(() => [
    { label: '趋势', value: technicalAnalysis.value.conclusion.trendLevel },
    { label: '动能', value: technicalAnalysis.value.conclusion.momentumLevel },
    { label: '量能', value: technicalAnalysis.value.conclusion.volumeLevel },
    { label: '波动', value: technicalAnalysis.value.conclusion.volatilityLevel },
    { label: '风险', value: technicalAnalysis.value.conclusion.riskLevel }
])

const conclusionTagType = computed(() => statusTagType(overallScoreToStatus(technicalAnalysis.value.conclusion.overallScore)))

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
                        return activeSubIndicator.value === 'VOLUME' || activeSubIndicator.value === 'OBV'
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

    if (activeSubIndicator.value === 'VOLUME') {
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

function applyAdjustMode(rows: MarketQuote[], mode: QuoteAdjustMode): TechnicalRow[] {
    const latestFactor = rows.slice().reverse().find(row => isValidNumber(row.adjustFactor))?.adjustFactor || 1

    return rows.map(row => {
        const factor = isValidNumber(row.adjustFactor) ? Number(row.adjustFactor) : 1
        const multiplier = mode === 'EX_RIGHT'
            ? 1
            : mode === 'FRONT_ADJUST'
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

function enrichTechnicalIndicators(rows: TechnicalRow[]): TechnicalRow[] {
    const result = rows.map(row => ({ ...row }))
    const closes = result.map(row => numberOrNull(row.adjustedClose))
    const highs = result.map(row => numberOrNull(row.adjustedHigh))
    const lows = result.map(row => numberOrNull(row.adjustedLow))
    const volumes = result.map(row => numberOrNull(row.volume))

    fillMovingAverage(result, closes, 'ma5', 5)
    fillMovingAverage(result, closes, 'ma10', 10)
    fillMovingAverage(result, closes, 'ma20', 20)
    fillMovingAverage(result, closes, 'ma60', 60)
    fillMovingAverage(result, closes, 'ma120', 120)
    fillMovingAverage(result, closes, 'ma250', 250)
    fillEma(result, closes, 'ema9', 9)
    fillEma(result, closes, 'ema12', 12)
    fillEma(result, closes, 'ema26', 26)
    fillBoll(result, closes, 20, 2)
    fillAtr(result, highs, lows, closes, 14)
    fillMacd(result, closes, 12, 26, 9)
    fillKdj(result, highs, lows, closes, 9, 3, 3)
    fillRsi(result, closes, 14)
    fillCci(result, highs, lows, closes, 14)
    fillDmi(result, highs, lows, closes, 14)
    fillObv(result, closes, volumes)
    fillVwap(result, highs, lows, closes, volumes, [1, 5, 10, 20, 60, 120])
    fillSar(result, highs, lows, 0.02, 0.2)
    fillSuperTrend(result, highs, lows, closes, 10, 3)

    return result
}

function buildRangeStats(rows: TechnicalRow[], allRows: MarketQuote[]) {
    if (!rows.length) {
        return {
            high: null,
            low: null,
            intervalReturn: null,
            annualizedReturn: null,
            amplitude: null,
            volatility: null,
            maxDrawdown: null,
            sharpeRatio: null,
            calmarRatio: null,
            volume: null,
            turnover: null,
            avgDailyTurnover: null,
            turnoverRate: null,
            volumeExpansionDays: 0,
            maxVolumeDate: '',
            volumeVsPreviousRangeChange: null,
            currentPricePercentile: null,
            newHighCount: 0,
            newLowCount: 0,
            priceDenseRange: '-',
            turnoverDenseRange: '-'
        }
    }

    const highs = rows.map(row => row.adjustedHigh).filter(isNumber)
    const lows = rows.map(row => row.adjustedLow).filter(isNumber)
    const closes = rows.map(row => row.adjustedClose).filter(isNumber)
    const volumes = rows.map(row => row.volume).filter(isNumber)
    const turnovers = rows.map(row => row.turnover).filter(isNumber)
    const firstClose = rows[0]?.preClose || rows[0]?.adjustedClose || 0
    const lastClose = rows[rows.length - 1]?.adjustedClose || 0
    const intervalReturn = firstClose ? (lastClose - firstClose) / firstClose * 100 : null
    const dailyReturns = calcDailyReturns(closes)
    const volatility = dailyReturns.length > 1 ? standardDeviation(dailyReturns) * Math.sqrt(250) * 100 : null
    const annualizedReturn = intervalReturn !== null && rows.length > 1 ? (Math.pow(1 + intervalReturn / 100, 250 / rows.length) - 1) * 100 : null
    const maxDrawdown = calcMaxDrawdown(closes)
    const sharpeRatio = volatility && annualizedReturn !== null ? annualizedReturn / volatility : null
    const calmarRatio = maxDrawdown && annualizedReturn !== null ? annualizedReturn / Math.abs(maxDrawdown) : null
    const maxVolumeRow = rows.reduce((maxRow, row) => Number(row.volume || 0) > Number(maxRow?.volume || 0) ? row : maxRow, rows[0])
    const avgVolume = average(volumes)
    const previousRows = getPreviousRangeRows(rows, allRows)
    const previousVolume = sum(previousRows.map(row => row.volume))
    const currentVolume = sum(rows.map(row => row.volume))
    const currentPricePercentile = highs.length && lows.length && lastClose
        ? (lastClose - Math.min(...lows)) / (Math.max(...highs) - Math.min(...lows) || 1) * 100
        : null

    return {
        high: highs.length ? Math.max(...highs) : null,
        low: lows.length ? Math.min(...lows) : null,
        intervalReturn,
        annualizedReturn,
        amplitude: highs.length && lows.length && firstClose ? (Math.max(...highs) - Math.min(...lows)) / firstClose * 100 : null,
        volatility,
        maxDrawdown,
        sharpeRatio,
        calmarRatio,
        volume: currentVolume,
        turnover: sum(rows.map(row => row.turnover)),
        avgDailyTurnover: turnovers.length ? average(turnovers) : null,
        turnoverRate: sum(rows.map(row => row.turnoverRate)),
        volumeExpansionDays: rows.filter(row => Number(row.volume || 0) > avgVolume * 1.2).length,
        maxVolumeDate: maxVolumeRow?.tradeDate || '',
        volumeVsPreviousRangeChange: previousVolume ? (currentVolume - previousVolume) / previousVolume * 100 : null,
        currentPricePercentile,
        newHighCount: countNewHighs(rows),
        newLowCount: countNewLows(rows),
        priceDenseRange: calcDenseRange(rows, 'adjustedClose'),
        turnoverDenseRange: calcTurnoverDenseRange(rows)
    }
}

function buildAutoTechnicalAnalysis(rows: TechnicalRow[]): QuoteTechnicalAnalysis {
    const last = rows[rows.length - 1]
    const prev = rows[rows.length - 2]

    if (!last) {
        return {
            conclusion: {
                overallTag: '暂无数据',
                overallJudgement: '当前区间没有足够行情数据，暂无法形成技术指标综合判断。',
                trendLevel: 1,
                momentumLevel: 1,
                volumeLevel: 1,
                volatilityLevel: 1,
                riskLevel: 1,
                overallScore: 0,
                signalStrength: 'WEAK',
                applicablePeriod: '当前区间',
                updatedTime: '-',
                explanation: '请扩大时间范围或检查行情数据是否已返回。'
            },
            dimensionScores: [],
            indicatorSignals: [],
            signalConflict: { bullishCount: 0, bearishCount: 0, neutralCount: 0, consistency: '暂无', mainSupportFactors: [], mainRiskFactors: [] }
        }
    }

    const signals = buildIndicatorSignals(last, prev)
    const bullishCount = signals.filter(item => ['BULLISH', 'SUPPORT_UP'].includes(item.signalDirection)).length
    const bearishCount = signals.filter(item => item.signalDirection === 'BEARISH').length
    const cautionCount = signals.filter(item => ['CAUTION', 'RISK_UP'].includes(item.signalDirection)).length
    const neutralCount = signals.length - bullishCount - bearishCount

    const trendScore = scoreByBooleans([
        Number(last.adjustedClose) > Number(last.ma20),
        Number(last.ma5) > Number(last.ma10) && Number(last.ma10) > Number(last.ma20),
        Number(last.adjustedClose) > Number(last.ema26),
        Number(last.sar) < Number(last.adjustedClose),
        Number(last.superTrendDirection) > 0,
        Number(last.plusDi14) > Number(last.minusDi14)
    ])
    const momentumScore = scoreByBooleans([
        Number(last.macdDif) > Number(last.macdDea),
        Number(last.macdHistogram) > Number(prev?.macdHistogram),
        Number(last.kdjJ) > 50 && Number(last.kdjJ) < 90,
        Number(last.rsi14) > 45 && Number(last.rsi14) < 70,
        Number(last.cci14) > 0
    ])
    const volumeScore = scoreByBooleans([
        Number(last.volume) > average(rows.slice(-20).map(row => row.volume).filter(isNumber)),
        Number(last.obv) > Number(prev?.obv),
        Number(last.adjustedClose) > Number(last.vwap20)
    ])
    const volatilityScore = clamp(100 - Math.max(0, Number(last.rsi14 || 50) - 70) * 2 - Math.max(0, Number(last.cci14 || 0) - 120) * 0.2, 35, 95)
    const pricePositionScore = clamp(Number(rangeStats.value.currentPricePercentile || 50), 0, 100)
    const consistencyScore = clamp(50 + (bullishCount - bearishCount) * 6 - cautionCount * 2, 0, 100)
    const overallScore = Math.round((trendScore * 0.24 + momentumScore * 0.2 + volumeScore * 0.16 + volatilityScore * 0.14 + pricePositionScore * 0.12 + consistencyScore * 0.14))
    const overallTag = resolveOverallTag(overallScore, last)

    const dimensionScores = [
        {
            dimensionCode: 'trend',
            dimensionName: '趋势',
            score: trendScore,
            level: scoreToLevel(trendScore),
            indicators: ['MA', 'EMA', 'SAR', 'SuperTrend', 'DMI'],
            explanation: trendScore >= 70 ? '多数趋势指标维持多头结构，价格方向偏强。' : trendScore >= 45 ? '趋势指标分歧较大，方向仍需确认。' : '趋势指标偏弱，价格仍承压。'
        },
        {
            dimensionCode: 'momentum',
            dimensionName: '动能',
            score: momentumScore,
            level: scoreToLevel(momentumScore),
            indicators: ['MACD', 'KDJ', 'RSI', 'CCI'],
            explanation: momentumScore >= 70 ? '上涨动能较强，但需留意过热指标。' : momentumScore >= 45 ? '动能温和，尚未形成单边强化。' : '动能不足，短线修复力度偏弱。'
        },
        {
            dimensionCode: 'volume',
            dimensionName: '量能',
            score: volumeScore,
            level: scoreToLevel(volumeScore),
            indicators: ['VOL', 'OBV', 'VWAP'],
            explanation: volumeScore >= 70 ? '成交量与价格方向较一致，对走势形成支持。' : volumeScore >= 45 ? '量能配合一般，持续性仍需观察。' : '量能不足或背离，走势可信度下降。'
        },
        {
            dimensionCode: 'volatility',
            dimensionName: '波动',
            score: volatilityScore,
            level: scoreToLevel(volatilityScore),
            indicators: ['BOLL', 'ATR'],
            explanation: volatilityScore >= 70 ? '波动处于可控区间，未显著偏离常态。' : '波动有所放大，接近极端区间时需要控制追涨杀跌风险。'
        },
        {
            dimensionCode: 'pricePosition',
            dimensionName: '价格位置',
            score: pricePositionScore,
            level: scoreToLevel(pricePositionScore),
            indicators: ['BOLL', 'VWAP', '区间高低点'],
            explanation: pricePositionScore >= 75 ? '当前价格接近区间高位，追高风险上升。' : pricePositionScore <= 25 ? '当前价格接近区间低位，短线可能存在修复观察价值。' : '当前价格处于区间中部，位置相对均衡。'
        },
        {
            dimensionCode: 'consistency',
            dimensionName: '信号一致性',
            score: consistencyScore,
            level: scoreToLevel(consistencyScore),
            indicators: ['多头/空头/中性信号'],
            explanation: bullishCount > bearishCount ? '多头信号数量占优，但仍需关注过热与波动风险。' : bearishCount > bullishCount ? '空头信号偏多，建议等待趋势修复。' : '多空信号接近，市场方向不够明确。'
        }
    ]

    const supportFactors = [
        Number(last.adjustedClose) > Number(last.ma20) ? '价格站上 MA20，短期趋势改善。' : '',
        Number(last.macdHistogram) > Number(prev?.macdHistogram) ? 'MACD 柱体改善，动能边际增强。' : '',
        Number(last.obv) > Number(prev?.obv) ? 'OBV 上行，成交量对价格方向有一定支持。' : '',
        Number(last.superTrendDirection) > 0 ? 'SuperTrend 维持多头状态。' : ''
    ].filter(Boolean)

    const riskFactors = [
        Number(last.rsi14) >= 70 ? 'RSI 接近或进入超买区域，短线可能偏热。' : '',
        Number(last.adjustedClose) > Number(last.bollUpper) * 0.98 ? '价格接近 BOLL 上轨，追高风险增加。' : '',
        Number(last.atr14) > Number(prev?.atr14) ? 'ATR 上升，波动扩大。' : '',
        Number(last.volume) <= average(rows.slice(-20).map(row => row.volume).filter(isNumber)) * 1.2 ? '成交量虽有变化，但未明显超过阶段均量。' : ''
    ].filter(Boolean)

    return {
        conclusion: {
            overallTag,
            overallJudgement: buildOverallJudgement(overallTag, overallScore, bullishCount, bearishCount, cautionCount),
            trendLevel: scoreToLevel(trendScore),
            momentumLevel: scoreToLevel(momentumScore),
            volumeLevel: scoreToLevel(volumeScore),
            volatilityLevel: scoreToLevel(volatilityScore),
            riskLevel: scoreToRiskLevel(volatilityScore, last),
            overallScore,
            signalStrength: overallScore >= 75 ? 'STRONG' : overallScore >= 55 ? 'MEDIUM' : 'WEAK',
            applicablePeriod: rangeDateText.value,
            updatedTime: formatDateTime(last.tradeDate),
            explanation: `从当前区间看，趋势评分为 ${formatNumber(trendScore, 0)}，动能评分为 ${formatNumber(momentumScore, 0)}，量能评分为 ${formatNumber(volumeScore, 0)}。综合判断为“${overallTag}”。若价格继续维持在 MA20 与 VWAP 上方，走势延续性较好；若 RSI、KDJ、CCI 持续过热且 ATR 上升，则需要降低追高仓位并等待回踩确认。`
        },
        dimensionScores,
        indicatorSignals: signals,
        signalConflict: {
            bullishCount,
            bearishCount,
            neutralCount: neutralCount + cautionCount,
            consistency: resolveConsistencyText(bullishCount, bearishCount, signals.length),
            mainSupportFactors: supportFactors.length ? supportFactors : ['当前支持因素不够集中，建议继续观察均线、MACD 与量能配合。'],
            mainRiskFactors: riskFactors.length ? riskFactors : ['当前风险因素不突出，但仍需关注市场整体波动与成交量持续性。']
        }
    }
}

function buildIndicatorSignals(last: TechnicalRow, prev?: TechnicalRow): QuoteTechnicalSignal[] {
    const maBullish = Number(last.ma5) > Number(last.ma10) && Number(last.ma10) > Number(last.ma20)
    const emaBullish = Number(last.adjustedClose) > Number(last.ema26)
    const bollHot = Number(last.adjustedClose) >= Number(last.bollUpper) * 0.98
    const sarBullish = Number(last.sar) < Number(last.adjustedClose)
    const vwapBullish = Number(last.adjustedClose) > Number(last.vwap20)
    const superTrendBullish = Number(last.superTrendDirection) > 0
    const volumeUp = Number(last.volume) > Number(prev?.volume)
    const macdBullish = Number(last.macdDif) > Number(last.macdDea)
    const kdjHot = Number(last.kdjJ) >= 85
    const rsiHot = Number(last.rsi14) >= 70
    const cciStrong = Number(last.cci14) > 100
    const dmiBullish = Number(last.plusDi14) > Number(last.minusDi14)
    const atrUp = Number(last.atr14) > Number(prev?.atr14)
    const obvUp = Number(last.obv) > Number(prev?.obv)

    return [
        createSignal('MA', maBullish ? '短期均线上穿中期均线' : '均线结构尚未完全多头排列', maBullish ? 'BULLISH' : 'NEUTRAL', maBullish ? 'MA5 > MA10 > MA20，短期趋势较强。' : '均线排列仍有分歧，趋势确认度一般。'),
        createSignal('EMA', emaBullish ? '价格位于 EMA26 上方' : '价格位于 EMA26 附近或下方', emaBullish ? 'BULLISH' : 'NEUTRAL', emaBullish ? '价格保持在中短期成本线上方。' : '价格尚未明显脱离中短期成本区。'),
        createSignal('BOLL', bollHot ? '价格靠近上轨' : '价格位于布林带常态区间', bollHot ? 'CAUTION' : 'NEUTRAL', bollHot ? '上行趋势较强，但短线波动风险增加。' : '价格未明显触及极端波动区间。'),
        createSignal('SAR', sarBullish ? 'SAR 点位于 K 线下方' : 'SAR 点位于 K 线上方', sarBullish ? 'BULLISH' : 'BEARISH', sarBullish ? '趋势仍处于多头跟踪状态。' : '趋势跟踪指标提示转弱。'),
        createSignal('VWAP', vwapBullish ? '价格高于 VWAP20' : '价格低于 VWAP20', vwapBullish ? 'BULLISH' : 'BEARISH', vwapBullish ? '当前价格高于区间平均成交成本。' : '当前价格低于区间平均成交成本。'),
        createSignal('SuperTrend', superTrendBullish ? '价格在趋势线上方' : '价格在趋势线下方', superTrendBullish ? 'BULLISH' : 'BEARISH', superTrendBullish ? '趋势方向仍向上。' : '趋势方向偏弱。'),
        createSignal('VOL', volumeUp ? '成交量高于上一交易日' : '成交量未明显放大', volumeUp ? 'SUPPORT_UP' : 'NEUTRAL', volumeUp ? '量能有所放大。' : '量能支持力度一般。'),
        createSignal('MACD', macdBullish ? 'DIF 高于 DEA' : 'DIF 未高于 DEA', macdBullish ? 'BULLISH' : 'BEARISH', macdBullish ? '上涨动能增强。' : '动能指标偏弱或仍在修复。'),
        createSignal('KDJ', kdjHot ? 'J 值偏高' : 'KDJ 处于常态区间', kdjHot ? 'CAUTION' : 'NEUTRAL', kdjHot ? '短线有过热迹象。' : '短线未出现明显极端信号。'),
        createSignal('RSI', rsiHot ? 'RSI 接近 70 或更高' : 'RSI 未进入超买区域', rsiHot ? 'CAUTION' : 'NEUTRAL', rsiHot ? '接近超买区域。' : '相对强弱仍处于可观察区间。'),
        createSignal('CCI', cciStrong ? 'CCI > 100' : 'CCI 未明显强势', cciStrong ? 'BULLISH' : 'NEUTRAL', cciStrong ? '价格处于强势区。' : '顺势指标暂未明显偏强。'),
        createSignal('DMI', dmiBullish ? '+DI > -DI' : '+DI 未超过 -DI', dmiBullish ? 'BULLISH' : 'BEARISH', dmiBullish ? '多方力量占优，趋势增强。' : '空方或震荡力量仍占一定优势。'),
        createSignal('ATR', atrUp ? 'ATR 上升' : 'ATR 平稳或下降', atrUp ? 'RISK_UP' : 'NEUTRAL', atrUp ? '波动扩大。' : '波动没有明显扩张。'),
        createSignal('OBV', obvUp ? 'OBV 同步上升' : 'OBV 未同步上升', obvUp ? 'SUPPORT_UP' : 'NEUTRAL', obvUp ? '成交量与价格方向一致。' : '量价确认度仍需观察。')
    ]
}

function createSignal(indicatorName: string, currentStatus: string, signalDirection: QuoteSignalDirection, explanation: string): QuoteTechnicalSignal {
    const directionMap: Record<QuoteSignalDirection, string> = {
        BULLISH: '偏多',
        BEARISH: '偏空',
        NEUTRAL: '中性',
        CAUTION: '谨慎',
        SUPPORT_UP: '支持上涨',
        RISK_UP: '风险增加'
    }

    return {
        indicatorName,
        currentStatus,
        signalDirection,
        signalDirectionText: directionMap[signalDirection],
        explanation
    }
}

function fillMovingAverage(rows: TechnicalRow[], values: Array<number | null>, field: keyof TechnicalRow, period: number): void {
    rows.forEach((row, index) => {
        setTechnicalField(row, field, index + 1 >= period ? average(values.slice(index + 1 - period, index + 1).filter(isNumber)) : null)
    })
}

function fillEma(rows: TechnicalRow[], values: Array<number | null>, field: keyof TechnicalRow, period: number): void {
    const alpha = 2 / (period + 1)
    let previous: number | null = null
    rows.forEach((row, index) => {
        const value = values[index]
        if (!isNumber(value)) {
            setTechnicalField(row, field, null)
            return
        }
        previous = previous === null ? value : value * alpha + previous * (1 - alpha)
        setTechnicalField(row, field, previous)
    })
}

function fillBoll(rows: TechnicalRow[], values: Array<number | null>, period: number, multiplier: number): void {
    rows.forEach((row, index) => {
        if (index + 1 < period) return
        const windowValues = values.slice(index + 1 - period, index + 1).filter(isNumber)
        const mid = average(windowValues)
        const std = standardDeviation(windowValues)
        row.bollMid = mid
        row.bollUpper = mid + multiplier * std
        row.bollLower = mid - multiplier * std
    })
}

function fillMacd(rows: TechnicalRow[], closes: Array<number | null>, shortPeriod: number, longPeriod: number, signalPeriod: number): void {
    const emaShort = calcEmaSeries(closes, shortPeriod)
    const emaLong = calcEmaSeries(closes, longPeriod)
    const dif = closes.map((_, index) => isNumber(emaShort[index]) && isNumber(emaLong[index]) ? Number(emaShort[index]) - Number(emaLong[index]) : null)
    const dea = calcEmaSeries(dif, signalPeriod)
    rows.forEach((row, index) => {
        row.macdDif = dif[index]
        row.macdDea = dea[index]
        row.macdHistogram = isNumber(dif[index]) && isNumber(dea[index]) ? (Number(dif[index]) - Number(dea[index])) * 2 : null
    })
}

function fillKdj(rows: TechnicalRow[], highs: Array<number | null>, lows: Array<number | null>, closes: Array<number | null>, period: number, kPeriod: number, dPeriod: number): void {
    let k = 50
    let d = 50
    rows.forEach((row, index) => {
        if (index + 1 < period || !isNumber(closes[index])) return
        const highWindow = highs.slice(index + 1 - period, index + 1).filter(isNumber)
        const lowWindow = lows.slice(index + 1 - period, index + 1).filter(isNumber)
        const highest = Math.max(...highWindow)
        const lowest = Math.min(...lowWindow)
        const rsv = (highest - lowest) === 0 ? 50 : (Number(closes[index]) - lowest) / (highest - lowest) * 100
        k = (k * (kPeriod - 1) + rsv) / kPeriod
        d = (d * (dPeriod - 1) + k) / dPeriod
        row.kdjK = k
        row.kdjD = d
        row.kdjJ = 3 * k - 2 * d
    })
}

function fillRsi(rows: TechnicalRow[], closes: Array<number | null>, period: number): void {
    rows.forEach((row, index) => {
        if (index < period) return
        const windowValues = closes.slice(index + 1 - period, index + 1)
        let gains = 0
        let losses = 0
        for (let i = 1; i < windowValues.length; i += 1) {
            const diff = Number(windowValues[i]) - Number(windowValues[i - 1])
            if (diff > 0) gains += diff
            if (diff < 0) losses += Math.abs(diff)
        }
        row.rsi14 = losses === 0 ? 100 : 100 - 100 / (1 + gains / losses)
    })
}

function fillCci(rows: TechnicalRow[], highs: Array<number | null>, lows: Array<number | null>, closes: Array<number | null>, period: number): void {
    const typicalPrices = closes.map((close, index) => isNumber(close) && isNumber(highs[index]) && isNumber(lows[index]) ? (Number(highs[index]) + Number(lows[index]) + Number(close)) / 3 : null)
    rows.forEach((row, index) => {
        if (index + 1 < period) return
        const windowValues = typicalPrices.slice(index + 1 - period, index + 1).filter(isNumber)
        const ma = average(windowValues)
        const md = average(windowValues.map(value => Math.abs(value - ma)))
        row.cci14 = md ? (Number(typicalPrices[index]) - ma) / (0.015 * md) : null
    })
}

function fillDmi(rows: TechnicalRow[], highs: Array<number | null>, lows: Array<number | null>, closes: Array<number | null>, period: number): void {
    const plusDm: number[] = [0]
    const minusDm: number[] = [0]
    const tr: number[] = [0]
    for (let i = 1; i < rows.length; i += 1) {
        const upMove = Number(highs[i]) - Number(highs[i - 1])
        const downMove = Number(lows[i - 1]) - Number(lows[i])
        plusDm[i] = upMove > downMove && upMove > 0 ? upMove : 0
        minusDm[i] = downMove > upMove && downMove > 0 ? downMove : 0
        tr[i] = trueRange(Number(highs[i]), Number(lows[i]), Number(closes[i - 1]))
    }

    rows.forEach((row, index) => {
        if (index + 1 < period) return
        const trSum = sum(tr.slice(index + 1 - period, index + 1))
        if (!trSum) return
        row.plusDi14 = sum(plusDm.slice(index + 1 - period, index + 1)) / trSum * 100
        row.minusDi14 = sum(minusDm.slice(index + 1 - period, index + 1)) / trSum * 100
        const diSum = row.plusDi14 + row.minusDi14
        row.adx14 = diSum ? Math.abs(row.plusDi14 - row.minusDi14) / diSum * 100 : null
    })
}

function fillAtr(rows: TechnicalRow[], highs: Array<number | null>, lows: Array<number | null>, closes: Array<number | null>, period: number): void {
    const trList = rows.map((_, index) => index === 0 ? Number(highs[index]) - Number(lows[index]) : trueRange(Number(highs[index]), Number(lows[index]), Number(closes[index - 1])))
    rows.forEach((row, index) => {
        if (index + 1 < period) return
        row.atr14 = average(trList.slice(index + 1 - period, index + 1))
    })
}

function fillObv(rows: TechnicalRow[], closes: Array<number | null>, volumes: Array<number | null>): void {
    let obv = 0
    rows.forEach((row, index) => {
        if (index === 0) {
            obv = Number(volumes[index] || 0)
        } else if (Number(closes[index]) > Number(closes[index - 1])) {
            obv += Number(volumes[index] || 0)
        } else if (Number(closes[index]) < Number(closes[index - 1])) {
            obv -= Number(volumes[index] || 0)
        }
        row.obv = obv
    })
}

function fillVwap(rows: TechnicalRow[], highs: Array<number | null>, lows: Array<number | null>, closes: Array<number | null>, volumes: Array<number | null>, periods: number[]): void {
    periods.forEach(period => {
        rows.forEach((row, index) => {
            if (index + 1 < period) return
            const start = index + 1 - period
            let amount = 0
            let volume = 0
            for (let i = start; i <= index; i += 1) {
                const typicalPrice = (Number(highs[i]) + Number(lows[i]) + Number(closes[i])) / 3
                amount += typicalPrice * Number(volumes[i] || 0)
                volume += Number(volumes[i] || 0)
            }
            setTechnicalField(row, `vwap${period}` as keyof TechnicalRow, volume ? amount / volume : null)
        })
    })
}

function fillSar(rows: TechnicalRow[], highs: Array<number | null>, lows: Array<number | null>, step: number, maxStep: number): void {
    if (rows.length < 2) return
    let bullish = Number(rows[1].adjustedClose) >= Number(rows[0].adjustedClose)
    let sar = bullish ? Number(lows[0]) : Number(highs[0])
    let ep = bullish ? Number(highs[0]) : Number(lows[0])
    let af = step

    rows.forEach((row, index) => {
        if (index === 0) {
            row.sar = sar
            return
        }
        sar = sar + af * (ep - sar)
        if (bullish) {
            if (Number(lows[index]) < sar) {
                bullish = false
                sar = ep
                ep = Number(lows[index])
                af = step
            } else if (Number(highs[index]) > ep) {
                ep = Number(highs[index])
                af = Math.min(maxStep, af + step)
            }
        } else if (Number(highs[index]) > sar) {
            bullish = true
            sar = ep
            ep = Number(highs[index])
            af = step
        } else if (Number(lows[index]) < ep) {
            ep = Number(lows[index])
            af = Math.min(maxStep, af + step)
        }
        row.sar = sar
    })
}

function fillSuperTrend(rows: TechnicalRow[], highs: Array<number | null>, lows: Array<number | null>, closes: Array<number | null>, period: number, multiplier: number): void {
    const trList = rows.map((_, index) => index === 0 ? Number(highs[index]) - Number(lows[index]) : trueRange(Number(highs[index]), Number(lows[index]), Number(closes[index - 1])))
    const atr = trList.map((_, index) => index + 1 >= period ? average(trList.slice(index + 1 - period, index + 1)) : null)
    let direction = 1
    let trendLine: number | null = null

    rows.forEach((row, index) => {
        if (!isNumber(atr[index])) return
        const hl2 = (Number(highs[index]) + Number(lows[index])) / 2
        const upperBand = hl2 + multiplier * Number(atr[index])
        const lowerBand = hl2 - multiplier * Number(atr[index])
        if (trendLine === null) trendLine = lowerBand
        if (Number(closes[index]) > upperBand) direction = 1
        if (Number(closes[index]) < lowerBand) direction = -1
        trendLine = direction > 0 ? Math.max(lowerBand, trendLine) : Math.min(upperBand, trendLine)
        row.superTrend = trendLine
        row.superTrendDirection = direction
    })
}

function calcEmaSeries(values: Array<number | null>, period: number): Array<number | null> {
    const alpha = 2 / (period + 1)
    let previous: number | null = null
    return values.map(value => {
        if (!isNumber(value)) return null
        previous = previous === null ? value : value * alpha + previous * (1 - alpha)
        return previous
    })
}

function getPreviousRangeRows(rows: TechnicalRow[], allRows: MarketQuote[]): MarketQuote[] {
    const firstDate = rows[0]?.tradeDate
    const startIndex = allRows.findIndex(row => row.tradeDate === firstDate)
    if (startIndex <= 0) return []
    const size = rows.length
    return allRows.slice(Math.max(0, startIndex - size), startIndex)
}

function countNewHighs(rows: TechnicalRow[]): number {
    let high = Number.NEGATIVE_INFINITY
    let count = 0
    rows.forEach(row => {
        const value = Number(row.adjustedHigh)
        if (value > high) {
            count += 1
            high = value
        }
    })
    return count
}

function countNewLows(rows: TechnicalRow[]): number {
    let low = Number.POSITIVE_INFINITY
    let count = 0
    rows.forEach(row => {
        const value = Number(row.adjustedLow)
        if (value < low) {
            count += 1
            low = value
        }
    })
    return count
}

function calcDenseRange(rows: TechnicalRow[], field: keyof TechnicalRow): string {
    const values = rows.map(row => Number(row[field])).filter(isNumber).sort((a, b) => a - b)
    if (!values.length) return '-'
    const q1 = values[Math.floor(values.length * 0.25)]
    const q3 = values[Math.floor(values.length * 0.75)]
    return `${formatNumber(q1)} ~ ${formatNumber(q3)}`
}

function calcTurnoverDenseRange(rows: TechnicalRow[]): string {
    const ranked = rows
        .filter(row => isNumber(row.turnover) && isNumber(row.adjustedClose))
        .sort((a, b) => Number(b.turnover) - Number(a.turnover))
        .slice(0, Math.max(3, Math.ceil(rows.length * 0.2)))
    if (!ranked.length) return '-'
    const prices = ranked.map(row => Number(row.adjustedClose)).sort((a, b) => a - b)
    return `${formatNumber(prices[0])} ~ ${formatNumber(prices[prices.length - 1])}`
}

function formatExcessReturn(value?: number | null): string {
    if (!isNumber(value)) return '-'
    return value >= 0 ? `跑赢 ${formatPercent(value, 2, true)}` : `跑输 ${formatPercent(Math.abs(value), 2)}`
}

function signalTagType(direction: QuoteSignalDirection): '' | 'success' | 'warning' | 'danger' | 'info' | 'primary' {
    if (['BULLISH', 'SUPPORT_UP'].includes(direction)) return 'success'
    if (direction === 'BEARISH') return 'danger'
    if (['CAUTION', 'RISK_UP'].includes(direction)) return 'warning'
    return 'info'
}

function resolveOverallTag(score: number, last: TechnicalRow): string {
    if (Number(last.rsi14) >= 75 || Number(last.kdjJ) >= 90) return '超买警戒'
    if (Number(last.rsi14) <= 30 || Number(last.kdjJ) <= 15) return '超卖反弹'
    if (score >= 80) return '强势上涨'
    if (score >= 62) return '震荡偏强'
    if (score >= 45) return '中性震荡'
    if (score >= 30) return '震荡偏弱'
    return '弱势下跌'
}

function buildOverallJudgement(tag: string, score: number, bullishCount: number, bearishCount: number, cautionCount: number): string {
    return `${tag}：当前综合评分 ${score}，多头信号 ${bullishCount} 个，空头信号 ${bearishCount} 个，谨慎或风险信号 ${cautionCount} 个。整体适合结合当前区间进行趋势跟踪，避免单独依赖单一指标。`
}

function resolveConsistencyText(bullishCount: number, bearishCount: number, total: number): string {
    const diffRatio = Math.abs(bullishCount - bearishCount) / Math.max(total, 1)
    if (diffRatio >= 0.5) return bullishCount > bearishCount ? '较强偏多' : '较强偏空'
    if (diffRatio >= 0.28) return bullishCount > bearishCount ? '中等偏强' : '中等偏弱'
    return '分歧较大'
}

function overallScoreToStatus(score: number): string {
    if (score >= 70) return 'GOOD'
    if (score >= 45) return 'NORMAL'
    return 'WARNING'
}

function scoreToLevel(score: number): QuoteTechnicalLevel {
    if (score >= 80) return 5
    if (score >= 60) return 4
    if (score >= 40) return 3
    if (score >= 20) return 2
    return 1
}

function scoreToRiskLevel(volatilityScore: number, last: TechnicalRow): QuoteTechnicalLevel {
    const hot = Number(last.rsi14) >= 70 || Number(last.kdjJ) >= 85 || Number(last.adjustedClose) >= Number(last.bollUpper) * 0.98
    if (hot && volatilityScore < 60) return 5
    if (hot) return 4
    if (volatilityScore < 45) return 4
    if (volatilityScore < 65) return 3
    return 2
}

function scoreByBooleans(values: boolean[]): number {
    if (!values.length) return 0
    return Math.round(values.filter(Boolean).length / values.length * 100)
}

function calcDailyReturns(closes: number[]): number[] {
    const returns: number[] = []
    for (let i = 1; i < closes.length; i += 1) {
        if (closes[i - 1]) returns.push((closes[i] - closes[i - 1]) / closes[i - 1])
    }
    return returns
}

function calcMaxDrawdown(closes: number[]): number | null {
    if (closes.length < 2) return null
    let peak = closes[0]
    let maxDrawdown = 0
    closes.forEach(close => {
        peak = Math.max(peak, close)
        if (peak) maxDrawdown = Math.min(maxDrawdown, (close - peak) / peak * 100)
    })
    return maxDrawdown
}

function trueRange(high: number, low: number, previousClose: number): number {
    return Math.max(high - low, Math.abs(high - previousClose), Math.abs(low - previousClose))
}

function average(values: number[]): number {
    return values.length ? sum(values) / values.length : 0
}

function isOhlcData(value: unknown): value is OHLCData {
    return Boolean(value && typeof value === 'object' && 'tradeDate' in value && 'open' in value && 'close' in value)
}

function getOhlcRows(chart?: ChartGroup): OHLCData[] {
    if (!chart?.series?.length) return []
    return chart.series[0].data.filter(isOhlcData)
}

function standardDeviation(values: number[]): number {
    if (values.length <= 1) return 0
    const avg = average(values)
    const variance = average(values.map(value => Math.pow(value - avg, 2)))
    return Math.sqrt(variance)
}

function sum(values: Array<number | null | undefined>): number {
    return values.reduce((total, value) => total || 0 + (Number(value) || 0), 0) || 0
}

function setTechnicalField(row: TechnicalRow, field: keyof TechnicalRow, value: number | null): void {
    ;(row as unknown as Record<string, number | string | null | undefined>)[String(field)] = value
}

function multiply(value: number | undefined, multiplier: number): number {
    return isValidNumber(value) ? Number(value) * multiplier : 0
}

function numberOrNull(value: number | null | undefined): number | null {
    return isValidNumber(value) ? Number(value) : null
}

function isValidNumber(value: unknown): boolean {
    return value !== undefined && value !== null && value !== '' && !Number.isNaN(Number(value))
}

function isNumber(value: unknown): value is number {
    return typeof value === 'number' && !Number.isNaN(value)
}

function clamp(value: number, min: number, max: number): number {
    return Math.min(max, Math.max(min, value))
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
