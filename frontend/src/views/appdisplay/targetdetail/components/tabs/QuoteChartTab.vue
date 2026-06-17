<template>
    <div class="quote-chart-tab">
        <el-row :gutter="16">
            <el-col :xs="24" :lg="16">
                <div class="chart-list">
                    <!-- 各个图在左侧上下排列 -->
                    <el-card v-for="chart in charts" :key="chart.chartId" class="chart-card" shadow="never">
                        <template #header>
                            <div class="chart-header">
                                <span>{{ chart.title }}</span>
                                <div class="chart-actions">
                                    <el-tag size="small" type="info">{{ chart.chartType }}</el-tag>
                                    <el-button link type="primary" @click="activeRawChartId = activeRawChartId === chart.chartId ? '' : chart.chartId">
                                        {{ activeRawChartId === chart.chartId ? '隐藏数据' : '查看数据' }}
                                    </el-button>
                                </div>
                            </div>
                        </template>

                        <TargetEcharts
                            :option="buildOption(chart)"
                            :height="chart.chartType === 'CANDLESTICK' ? '430px' : '340px'"
                        />

                        <el-table
                            v-if="activeRawChartId === chart.chartId && chart.chartType === 'CANDLESTICK'"
                            :data="ohlcRows(chart).slice().reverse()"
                            border
                            height="260"
                            class="raw-table"
                        >
                            <el-table-column prop="date" label="日期" min-width="110" align="center" />
                            <el-table-column prop="open" label="开盘" width="90" align="right" />
                            <el-table-column prop="high" label="最高" width="90" align="right" />
                            <el-table-column prop="low" label="最低" width="90" align="right" />
                            <el-table-column prop="close" label="收盘" width="90" align="right" />
                            <el-table-column label="成交量" min-width="120" align="right">
                                <template #default="{ row }">{{ formatAmount(row.volume) }}</template>
                            </el-table-column>
                            <el-table-column label="成交额" min-width="120" align="right">
                                <template #default="{ row }">{{ formatAmount(row.amount) }}</template>
                            </el-table-column>
                        </el-table>
                    </el-card>
                </div>
            </el-col>

            <el-col :xs="24" :lg="8">
                <el-card class="quote-summary-card" shadow="never">
                    <template #header>
                        <span class="card-title">区间表现</span>
                    </template>
                    <el-table :data="performance" border>
                        <el-table-column prop="periodName" label="区间" min-width="90" />
                        <el-table-column label="涨跌幅" min-width="90" align="right">
                            <template #default="{ row }">
                                <span :class="signedClass(row.changePercent)">
                                    {{ formatPercent(row.changePercent, 2, true) }}
                                </span>
                            </template>
                        </el-table-column>
                        <el-table-column label="最大回撤" min-width="90" align="right">
                            <template #default="{ row }">{{ formatPercent(row.maxDrawdown, 2, true) }}</template>
                        </el-table-column>
                        <el-table-column prop="rankingText" label="排名" min-width="80" />
                    </el-table>
                </el-card>

                <el-card class="quote-summary-card" shadow="never">
                    <template #header>
                        <span class="card-title">行情摘要</span>
                    </template>
                    <el-descriptions :column="1" border size="small">
                        <el-descriptions-item label="最新">{{ quote?.lastPrice ?? '-' }}</el-descriptions-item>
                        <el-descriptions-item label="涨跌幅">
                            <span :class="signedClass(quote?.changePercent)">
                                {{ formatPercent(quote?.changePercent, 2, true) }}
                            </span>
                        </el-descriptions-item>
                        <el-descriptions-item label="成交额">{{ formatAmount(quote?.amount) }}</el-descriptions-item>
                        <el-descriptions-item label="换手率">{{ formatPercent(quote?.turnoverRate) }}</el-descriptions-item>
                    </el-descriptions>
                </el-card>

                <el-card class="quote-summary-card" shadow="never">
                    <template #header>
                        <span class="card-title">图表说明</span>
                    </template>
                    <div class="chart-note">
                        K线图包含价格与成交量两个网格，支持鼠标滚轮缩放、拖拽区间、十字光标查看明细。
                    </div>
                </el-card>
            </el-col>
        </el-row>
    </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { EChartsOption } from 'echarts'
import type { ChartGroup, MarketQuote, TargetPerformanceInterval } from '/@/types/targetdetail/targetDetailCenter'
import TargetEcharts from '../charts/TargetEcharts.vue'
import {
    buildBarOption,
    buildCandlestickOption,
    buildLineOption,
    getOhlcRows
} from '../../utils/targetDetailChartOptions'
import { formatAmount, formatPercent, signedClass } from '../../utils/targetDetailFormat'

defineProps<{
    quote?: MarketQuote
    charts: ChartGroup[]
    performance: TargetPerformanceInterval[]
}>()

const activeRawChartId = ref('')

function buildOption(chart: ChartGroup): EChartsOption {
    if (chart.chartType === 'CANDLESTICK') return buildCandlestickOption(chart)
    if (chart.chartType === 'BAR') return buildBarOption(chart)
    if (chart.chartType === 'LINE') return buildLineOption(chart, chart.chartId.includes('percentile'))
    return buildLineOption(chart)
}

function ohlcRows(chart: ChartGroup) {
    return getOhlcRows(chart)
}
</script>

<style scoped>
.quote-chart-tab {
    min-height: 420px;
}

.chart-list {
    display: flex;
    flex-direction: column;
    gap: 16px;
}

.chart-card,
.quote-summary-card {
    border-radius: 10px;
}

.quote-summary-card + .quote-summary-card {
    margin-top: 16px;
}

.chart-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.chart-actions {
    display: flex;
    align-items: center;
    gap: 8px;
}

.card-title {
    font-weight: 600;
    color: #303133;
}

.raw-table {
    margin-top: 12px;
}

.chart-note {
    font-size: 13px;
    line-height: 1.7;
    color: #606266;
}

.is-up {
    color: #f56c6c;
}

.is-down {
    color: #67c23a;
}

.is-flat {
    color: #606266;
}
</style>
