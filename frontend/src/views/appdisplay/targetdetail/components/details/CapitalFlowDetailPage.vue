<template>
    <div class="capital-flow-detail-page">
        <!--
            资金流向详情页。
            当前物理表仅覆盖股票资金流 stock_moneyflow_daily；指数资金流、北向资金、融资融券未在当前表中定义，
            因此本页不展示未落库字段，避免前端产生不可追溯指标。
        -->
        <el-empty v-if="!resolvedCapitalFlow && !resolvedMoneyFlowRows.length" description="暂无资金流向数据" />

        <template v-else>
            <el-alert
                v-if="!resolvedMoneyFlowRows.length"
                class="source-alert"
                title="当前仅收到资金流摘要，若要展示买卖拆分和历史明细，请由后端补充 moneyFlowRows。"
                type="info"
                show-icon
                :closable="false"
            />

            <el-row :gutter="12">
                <el-col :xs="24" :sm="12" :lg="6" v-for="item in flowCards" :key="item.label">
                    <el-card class="metric-card" shadow="never">
                        <div class="metric-label">{{ item.label }}</div>
                        <div class="metric-value" :class="signedClass(item.rawValue)">{{ item.value }}</div>
                        <div class="metric-desc">{{ item.desc }}</div>
                    </el-card>
                </el-col>
            </el-row>

            <el-row :gutter="12">
                <el-col :xs="24" :lg="12">
                    <el-card class="detail-card" shadow="never">
                        <template #header>
                            <span class="card-title">当日买卖拆分</span>
                        </template>
                        <el-empty v-if="!latestMoneyFlowRow" description="暂无当日买卖拆分" />
                        <TargetEcharts v-else :option="latestSplitOption" height="340px" />
                    </el-card>
                </el-col>

                <el-col :xs="24" :lg="12">
                    <el-card class="detail-card" shadow="never">
                        <template #header>
                            <span class="card-title">分档净流入历史</span>
                        </template>
                        <el-empty v-if="!historyRows.length" description="暂无历史资金明细" />
                        <TargetEcharts v-else :option="historyFlowOption" height="340px" />
                    </el-card>
                </el-col>
            </el-row>

            <el-row :gutter="12">
                <el-col :xs="24" :lg="12">
                    <el-card class="detail-card" shadow="never">
                        <template #header>
                            <span class="card-title">总净流入趋势</span>
                        </template>
                        <el-empty v-if="!resolvedMoneyFlowRows.length" description="暂无净流入趋势" />
                        <TargetEcharts v-else :option="netFlowOption" height="340px" />
                    </el-card>
                </el-col>

                <el-col :xs="24" :lg="12">
                    <el-card class="detail-card" shadow="never">
                        <template #header>
                            <span class="card-title">资金与价格联动</span>
                        </template>
                        <el-empty v-if="!resolvedMoneyFlowRows.length || !resolvedCloseRows.length" description="暂无价格联动明细" />
                        <TargetEcharts v-else :option="flowPriceOption" height="340px" />
                    </el-card>
                </el-col>
            </el-row>

            <el-card class="detail-card" shadow="never">
                <template #header>
                    <div class="card-header">
                        <div>
                            <span class="card-title">资金流向明细</span>
                            <span class="card-tip">字段严格对应 stock_moneyflow_daily 驼峰字段</span>
                        </div>
                    </div>
                </template>

                <el-table :data="resolvedMoneyFlowRows.slice().reverse()" border height="460">
                    <el-table-column prop="tradeDate" label="交易日期" width="110" align="center" fixed />
                    <el-table-column label="netMfAmount" min-width="130" align="right">
                        <template #default="{ row }">
                            <span :class="signedClass(row.netMfAmount)">{{ formatAmount(row.netMfAmount) }}</span>
                        </template>
                    </el-table-column>
                    <el-table-column label="netMfVol" min-width="130" align="right">
                        <template #default="{ row }">{{ formatAmount(row.netMfVol) }}</template>
                    </el-table-column>
                    <el-table-column label="buyElgAmount" min-width="140" align="right">
                        <template #default="{ row }">{{ formatAmount(row.buyElgAmount) }}</template>
                    </el-table-column>
                    <el-table-column label="sellElgAmount" min-width="140" align="right">
                        <template #default="{ row }">{{ formatAmount(row.sellElgAmount) }}</template>
                    </el-table-column>
                    <el-table-column label="buyLgAmount" min-width="130" align="right">
                        <template #default="{ row }">{{ formatAmount(row.buyLgAmount) }}</template>
                    </el-table-column>
                    <el-table-column label="sellLgAmount" min-width="130" align="right">
                        <template #default="{ row }">{{ formatAmount(row.sellLgAmount) }}</template>
                    </el-table-column>
                    <el-table-column label="buyMdAmount" min-width="130" align="right">
                        <template #default="{ row }">{{ formatAmount(row.buyMdAmount) }}</template>
                    </el-table-column>
                    <el-table-column label="sellMdAmount" min-width="130" align="right">
                        <template #default="{ row }">{{ formatAmount(row.sellMdAmount) }}</template>
                    </el-table-column>
                    <el-table-column label="buySmAmount" min-width="130" align="right">
                        <template #default="{ row }">{{ formatAmount(row.buySmAmount) }}</template>
                    </el-table-column>
                    <el-table-column label="sellSmAmount" min-width="130" align="right">
                        <template #default="{ row }">{{ formatAmount(row.sellSmAmount) }}</template>
                    </el-table-column>
                    <el-table-column label="buyElgVol" min-width="130" align="right">
                        <template #default="{ row }">{{ formatAmount(row.buyElgVol) }}</template>
                    </el-table-column>
                    <el-table-column label="sellElgVol" min-width="130" align="right">
                        <template #default="{ row }">{{ formatAmount(row.sellElgVol) }}</template>
                    </el-table-column>
                    <el-table-column label="buyLgVol" min-width="130" align="right">
                        <template #default="{ row }">{{ formatAmount(row.buyLgVol) }}</template>
                    </el-table-column>
                    <el-table-column label="sellLgVol" min-width="130" align="right">
                        <template #default="{ row }">{{ formatAmount(row.sellLgVol) }}</template>
                    </el-table-column>
                    <el-table-column label="buyMdVol" min-width="130" align="right">
                        <template #default="{ row }">{{ formatAmount(row.buyMdVol) }}</template>
                    </el-table-column>
                    <el-table-column label="sellMdVol" min-width="130" align="right">
                        <template #default="{ row }">{{ formatAmount(row.sellMdVol) }}</template>
                    </el-table-column>
                    <el-table-column label="buySmVol" min-width="130" align="right">
                        <template #default="{ row }">{{ formatAmount(row.buySmVol) }}</template>
                    </el-table-column>
                    <el-table-column label="sellSmVol" min-width="130" align="right">
                        <template #default="{ row }">{{ formatAmount(row.sellSmVol) }}</template>
                    </el-table-column>
                </el-table>
            </el-card>

            <el-card v-if="resolvedCapitalFlow?.conclusion" class="detail-card" shadow="never">
                <template #header>
                    <span class="card-title">资金流向结论</span>
                </template>
                <div class="note-text">{{ resolvedCapitalFlow.conclusion }}</div>
            </el-card>
        </template>
    </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import type { EChartsOption } from 'echarts'
import type { TargetCapitalFlowDetail } from '/@/types/targetdetail/quoteDetailType'
import { queryTargetCapitalFlowDetail } from '/@/api/targetdetail/quoteDetailApi'
import TargetEcharts from '/@/components/common/TargetEcharts.vue'
import { formatAmount, formatPercent, signedClass } from '/@/utils/marketUtils'

interface CapitalFlowDetailPageProps extends TargetCapitalFlowDetail {
    /** 查询截止交易日；不传时由后端解析最近可用交易日。 */
    tradeDate?: string | null
}

const props = withDefaults(
    defineProps<CapitalFlowDetailPageProps>(), {
        moneyFlowRows: () => [],
        close: () => [],
        meta: () => ({})
    }
)

const fetched = ref<TargetCapitalFlowDetail | null>(null)
const state = computed<TargetCapitalFlowDetail>(() => fetched.value ?? props)
const resolvedCapitalFlow = computed(() => state.value.capitalFlow)
const resolvedMoneyFlowRows = computed(() => state.value.moneyFlowRows || [])
const resolvedCloseRows = computed(() => state.value.close || [])

onMounted(() => {
    if (props.fullSymbol) {
        void loadCapitalFlow()
    }
})

/** 加载资金流摘要、历史明细与价格联动数据。 */
async function loadCapitalFlow(): Promise<void> {
    try {
        fetched.value = await queryTargetCapitalFlowDetail({
            fullSymbol: props.fullSymbol as string,
            tradeDate: props.tradeDate || props.capitalFlow?.tradeDate,
            rangeCode: '3M',
            includeQuoteRows: true
        })
    } catch (error) {
        console.error('加载资金流向数据失败', error)
    }
}

const latestMoneyFlowRow = computed(() => resolvedMoneyFlowRows.value[resolvedMoneyFlowRows.value.length - 1])

const historyRows = computed(() => resolvedMoneyFlowRows.value.map(row => ({
    tradeDate: row.tradeDate,
    small: calcNet(row.buySmAmount, row.sellSmAmount),
    medium: calcNet(row.buyMdAmount, row.sellMdAmount),
    large: calcNet(row.buyLgAmount, row.sellLgAmount),
    extraLarge: calcNet(row.buyElgAmount, row.sellElgAmount)
})))

const flowCards = computed(() => {
    const latest = latestMoneyFlowRow.value
    const capitalFlowRecord = resolvedCapitalFlow.value as Record<string, any> | undefined
    const elgNet = latest ? calcNet(latest.buyElgAmount, latest.sellElgAmount) : resolvedCapitalFlow.value?.elgNetMfAmount
    const lgNet = latest ? calcNet(latest.buyLgAmount, latest.sellLgAmount) : resolvedCapitalFlow.value?.lgNetMfAmount
    const mdNet = latest ? calcNet(latest.buyMdAmount, latest.sellMdAmount) : resolvedCapitalFlow.value?.mdNetMfAmount
    const smNet = latest ? calcNet(latest.buySmAmount, latest.sellSmAmount) : resolvedCapitalFlow.value?.smNetMfAmount
    const mainNet = (elgNet || 0) + (lgNet || 0)
    const totalNet = latest?.netMfAmount ?? capitalFlowRecord?.netMfAmount ?? capitalFlowRecord?.mainNetInflow ?? mainNet

    return [
        { label: '总净流入', value: formatAmount(totalNet), rawValue: totalNet, desc: '字段：netMfAmount' },
        { label: '主力净流入', value: formatAmount(mainNet), rawValue: mainNet, desc: '特大单 + 大单' },
        { label: '中单净流入', value: formatAmount(mdNet), rawValue: mdNet, desc: 'buyMdAmount - sellMdAmount' },
        { label: '小单净流入', value: formatAmount(smNet), rawValue: smNet, desc: 'buySmAmount - sellSmAmount' }
    ]
})

const latestSplitOption = computed<EChartsOption>(() => {
    const row = latestMoneyFlowRow.value
    if (!row) return {}

    return {
        title: { text: '买入 / 卖出金额拆分', left: 8, top: 0, textStyle: { fontSize: 14, fontWeight: 600 } },
        tooltip: {
            trigger: 'axis',
            axisPointer: { type: 'shadow' },
            valueFormatter(value: any) {
                return formatAmount(Number(value))
            }
        },
        legend: { top: 28, left: 8 },
        grid: { left: 58, right: 24, top: 76, bottom: 36 },
        xAxis: { type: 'category', data: ['小单', '中单', '大单', '特大单'] },
        yAxis: {
            type: 'value',
            axisLabel: {
                formatter(value: number) {
                    return formatAmount(value)
                }
            },
            splitLine: { lineStyle: { type: 'dashed' } }
        },
        series: [
            {
                name: '买入金额',
                type: 'bar',
                data: [row.buySmAmount || 0, row.buyMdAmount || 0, row.buyLgAmount || 0, row.buyElgAmount || 0]
            },
            {
                name: '卖出金额',
                type: 'bar',
                data: [row.sellSmAmount || 0, row.sellMdAmount || 0, row.sellLgAmount || 0, row.sellElgAmount || 0]
            }
        ]
    }
})

const historyFlowOption = computed<EChartsOption>(() => ({
    title: { text: '小 / 中 / 大 / 特大单净流入', left: 8, top: 0, textStyle: { fontSize: 14, fontWeight: 600 } },
    tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow' },
        valueFormatter(value: any) {
            return formatAmount(Number(value))
        }
    },
    legend: { top: 28, left: 8 },
    grid: { left: 58, right: 24, top: 76, bottom: 46 },
    dataZoom: [
        { type: 'inside', start: historyRows.value.length > 90 ? 60 : 0, end: 100 },
        { type: 'slider', bottom: 8, height: 18, start: historyRows.value.length > 90 ? 60 : 0, end: 100 }
    ],
    xAxis: { type: 'category', data: historyRows.value.map(row => row.tradeDate), axisLabel: { hideOverlap: true } },
    yAxis: {
        type: 'value',
        axisLabel: {
            formatter(value: number) {
                return formatAmount(value)
            }
        },
        splitLine: { lineStyle: { type: 'dashed' } }
    },
    series: [
        { name: '小单', type: 'bar', stack: 'flow', data: historyRows.value.map(row => row.small) },
        { name: '中单', type: 'bar', stack: 'flow', data: historyRows.value.map(row => row.medium) },
        { name: '大单', type: 'bar', stack: 'flow', data: historyRows.value.map(row => row.large) },
        { name: '特大单', type: 'bar', stack: 'flow', data: historyRows.value.map(row => row.extraLarge) }
    ]
}))

const netFlowOption = computed(() => buildDetailLineOption(
    '总净流入额 / 净流入量',
    resolvedMoneyFlowRows.value,
    [
        { name: '净流入额', field: 'netMfAmount' },
        { name: '净流入量', field: 'netMfVol' }
    ]
))

const flowPriceOption = computed<EChartsOption>(() => {
    const rows = resolvedMoneyFlowRows.value.map((row, index) => ({
        tradeDate: row.tradeDate,
        netMfAmount: row.netMfAmount,
        close: resolvedCloseRows.value[index]
    }))
    return buildDetailLineOption(
        '净流入额与收盘价',
        rows,
        [
            { name: '净流入额', field: 'netMfAmount' },
            { name: '收盘价', field: 'close' }
        ]
    )
})

function calcNet(buy?: number | null, sell?: number | null): number {
    return (Number(buy) || 0) - (Number(sell) || 0)
}

function buildDetailLineOption<T extends Record<string, any>>(
    title: string,
    rows: T[],
    series: Array<{ name: string; field: keyof T; percent?: boolean }>,
    dateField: keyof T = 'tradeDate' as keyof T
): EChartsOption {
    const dates = rows.map(row => String(row[dateField] || ''))

    return {
        title: {
            text: title,
            left: 8,
            top: 0,
            textStyle: { fontSize: 14, fontWeight: 600 }
        },
        tooltip: {
            trigger: 'axis',
            valueFormatter(value: any) {
                return formatTooltipValue(Number(value), series.some(item => item.percent))
            }
        },
        legend: {
            top: 28,
            left: 8
        },
        grid: {
            left: 58,
            right: 24,
            top: 76,
            bottom: 46
        },
        dataZoom: [
            { type: 'inside', start: rows.length > 120 ? 60 : 0, end: 100 },
            { type: 'slider', bottom: 8, height: 18, start: rows.length > 120 ? 60 : 0, end: 100 }
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
                    return formatAmount(value)
                }
            },
            splitLine: { lineStyle: { type: 'dashed' } }
        },
        series: series.map(item => ({
            name: item.name,
            type: 'line',
            smooth: true,
            showSymbol: false,
            connectNulls: true,
            data: rows.map(row => normalizeNumber(row[item.field]))
        }))
    }
}


/**
 * 将任意值转为可用于图表展示的数值；空值保留为 null 以便 ECharts 断线。
 */
function normalizeNumber(value: unknown): number {
    if (value === undefined || value === null || value === '') return 0
    const numberValue = Number(value)
    return Number.isNaN(numberValue) ? 0 : numberValue
}

function formatTooltipValue(value: number, percent: boolean): string {
    if (Number.isNaN(value)) return '-'
    return percent ? formatPercent(value) : formatAmount(value)
}
</script>

<style scoped>
.capital-flow-detail-page {
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.source-alert {
    margin-bottom: 0;
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
    margin-right: 8px;
    font-weight: 600;
    color: var(--el-text-color-primary);
}

.note-text {
    color: var(--el-text-color-regular);
    font-size: 13px;
    line-height: 1.8;
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

@media (max-width: 768px) {
    .card-header {
        align-items: flex-start;
        flex-direction: column;
    }
}
</style>
