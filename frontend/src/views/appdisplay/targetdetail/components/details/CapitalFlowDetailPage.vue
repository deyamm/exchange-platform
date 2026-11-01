<template>
    <div class="capital-flow-detail-page">
        <!--
            资金流向详情页。
            当前物理表仅覆盖股票资金流 stock_moneyflow_daily；指数资金流、北向资金、融资融券未在当前表中定义，
            因此本页不展示未落库字段，避免前端产生不可追溯指标。
        -->
        <el-empty v-if="!resolvedCapitalFlow && !resolvedMoneyFlowRows.length" description="暂无资金流向数据" />

        <template v-else>
            <el-alert v-if="!resolvedMoneyFlowRows.length" class="source-alert"
                title="当前仅收到资金流摘要，若要展示买卖拆分和历史明细，请由后端补充 moneyFlowRows。" type="info" show-icon :closable="false" />

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
                        <el-empty v-if="!resolvedMoneyFlowRows.length || !resolvedCloseRows.length"
                            description="暂无价格联动明细" />
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
                    <el-table-column label="净流入额（万元）" min-width="140" align="right">
                        <template #default="{ row }">
                            <span :class="signedClass(row.netMfAmount)">
                                {{ formatAmount(row.netMfAmount) }}
                            </span>
                        </template>
                    </el-table-column>
                    <el-table-column label="净流入量（手）" min-width="130" align="right">
                        <template #default="{ row }">
                            <span :class="signedClass(row.netMfVol)">
                                {{ formatAmount(row.netMfVol) }}
                            </span>
                        </template>
                    </el-table-column>
                    <el-table-column label="特大单买入金额（万元）" min-width="180" align="right">
                        <template #default="{ row }">
                            {{ formatAmount(row.buyElgAmount) }}
                        </template>
                    </el-table-column>
                    <el-table-column label="特大单卖出金额（万元）" min-width="180" align="right">
                        <template #default="{ row }">
                            {{ formatAmount(row.sellElgAmount) }}
                        </template>
                    </el-table-column>
                    <el-table-column label="大单买入金额（万元）" min-width="170" align="right">
                        <template #default="{ row }">
                            {{ formatAmount(row.buyLgAmount) }}
                        </template>
                    </el-table-column>

                    <el-table-column label="大单卖出金额（万元）" min-width="170" align="right">
                        <template #default="{ row }">
                            {{ formatAmount(row.sellLgAmount) }}
                        </template>
                    </el-table-column>
                    <el-table-column label="中单买入金额（万元）" min-width="170" align="right">
                        <template #default="{ row }">
                            {{ formatAmount(row.buyMdAmount) }}
                        </template>
                    </el-table-column>
                    <el-table-column label="中单卖出金额（万元）" min-width="170" align="right">
                        <template #default="{ row }">
                            {{ formatAmount(row.sellMdAmount) }}
                        </template>
                    </el-table-column>
                    <el-table-column label="小单买入金额（万元）" min-width="170" align="right">
                        <template #default="{ row }">
                            {{ formatAmount(row.buySmAmount) }}
                        </template>
                    </el-table-column>
                    <el-table-column label="小单卖出金额（万元）" min-width="170" align="right">
                        <template #default="{ row }">
                            {{ formatAmount(row.sellSmAmount) }}
                        </template>
                    </el-table-column>
                    <el-table-column label="特大单买入量（手）" min-width="160" align="right">
                        <template #default="{ row }">
                            {{ formatAmount(row.buyElgVol) }}
                        </template>
                    </el-table-column>
                    <el-table-column label="特大单卖出量（手）" min-width="160" align="right">
                        <template #default="{ row }">
                            {{ formatAmount(row.sellElgVol) }}
                        </template>
                    </el-table-column>
                    <el-table-column label="大单买入量（手）" min-width="150" align="right">
                        <template #default="{ row }">
                            {{ formatAmount(row.buyLgVol) }}
                        </template>
                    </el-table-column>
                    <el-table-column label="大单卖出量（手）" min-width="150" align="right">
                        <template #default="{ row }">
                            {{ formatAmount(row.sellLgVol) }}
                        </template>
                    </el-table-column>
                    <el-table-column label="中单买入量（手）" min-width="150" align="right">
                        <template #default="{ row }">
                            {{ formatAmount(row.buyMdVol) }}
                        </template>
                    </el-table-column>
                    <el-table-column label="中单卖出量（手）" min-width="150" align="right">
                        <template #default="{ row }">
                            {{ formatAmount(row.sellMdVol) }}
                        </template>
                    </el-table-column>
                    <el-table-column label="小单买入量（手）" min-width="150" align="right">
                        <template #default="{ row }">
                            {{ formatAmount(row.buySmVol) }}
                        </template>
                    </el-table-column>
                    <el-table-column label="小单卖出量（手）" min-width="150" align="right">
                        <template #default="{ row }">
                            {{ formatAmount(row.sellSmVol) }}
                        </template>
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
    extraLarge: calcNet(row.buyElgAmount, row.sellElgAmount),
    large: calcNet(row.buyLgAmount, row.sellLgAmount),
    medium: calcNet(row.buyMdAmount, row.sellMdAmount),
    small: calcNet(row.buySmAmount, row.sellSmAmount)
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

    const buyAmounts = {
        extraLarge: normalizeNumber(row.buyElgAmount),
        large: normalizeNumber(row.buyLgAmount),
        medium: normalizeNumber(row.buyMdAmount),
        small: normalizeNumber(row.buySmAmount)
    }

    const sellAmounts = {
        extraLarge: normalizeNumber(row.sellElgAmount),
        large: normalizeNumber(row.sellLgAmount),
        medium: normalizeNumber(row.sellMdAmount),
        small: normalizeNumber(row.sellSmAmount)
    }

    const buyTotal = Object.values(buyAmounts).reduce((sum, value) => sum + value, 0)
    const sellTotal = Object.values(sellAmounts).reduce((sum, value) => sum + value, 0)

    // 买入、卖出各固定占整个饼图的 50%。
    // 每个档位在各自半圆内部按实际金额比例进行拆分。
    const halfValue = (value: number, total: number) => {
        if (total <= 0) return 0
        return value / total * 50
    }

    const sidePercent = (value: number, total: number) => {
        if (total <= 0) return 0
        return value / total * 100
    }

    /*
     * ECharts 从 90°（正上方）开始顺时针绘制。
     *
     * 右半圆（买入）：
     *   特大单 -> 大单 -> 中单 -> 小单
     *
     * 左半圆在数组中必须反向：
     *   小单 -> 中单 -> 大单 -> 特大单
     *
     * 这样实际页面上左侧从上到下看到的仍然是：
     *   特大单 -> 大单 -> 中单 -> 小单
     */
    const pieData = [
        // 右侧：买入，视觉上从上到下 特大 -> 大 -> 中 -> 小
        {
            name: '特大单买入',
            shortName: '特',
            labelColor: '#922B21',
            side: '买入',
            actualValue: buyAmounts.extraLarge,
            sidePercent: sidePercent(buyAmounts.extraLarge, buyTotal),
            value: halfValue(buyAmounts.extraLarge, buyTotal),
            itemStyle: { color: '#922B21' }
        },
        {
            name: '大单买入',
            shortName: '大',
            labelColor: '#C0392B',
            side: '买入',
            actualValue: buyAmounts.large,
            sidePercent: sidePercent(buyAmounts.large, buyTotal),
            value: halfValue(buyAmounts.large, buyTotal),
            itemStyle: { color: '#C0392B' }
        },
        {
            name: '中单买入',
            shortName: '中',
            labelColor: '#E74C3C',
            side: '买入',
            actualValue: buyAmounts.medium,
            sidePercent: sidePercent(buyAmounts.medium, buyTotal),
            value: halfValue(buyAmounts.medium, buyTotal),
            itemStyle: { color: '#E74C3C' }
        },
        {
            name: '小单买入',
            shortName: '小',
            labelColor: '#F5B7B1',
            side: '买入',
            actualValue: buyAmounts.small,
            sidePercent: sidePercent(buyAmounts.small, buyTotal),
            value: halfValue(buyAmounts.small, buyTotal),
            itemStyle: { color: '#F5B7B1' }
        },

        // 左侧：数组反向排列，视觉上从上到下仍为 特大 -> 大 -> 中 -> 小
        {
            name: '小单卖出',
            shortName: '小',
            labelColor: '#ABEBC6',
            side: '卖出',
            actualValue: sellAmounts.small,
            sidePercent: sidePercent(sellAmounts.small, sellTotal),
            value: halfValue(sellAmounts.small, sellTotal),
            itemStyle: { color: '#ABEBC6' }
        },
        {
            name: '中单卖出',
            shortName: '中',
            labelColor: '#52BE80',
            side: '卖出',
            actualValue: sellAmounts.medium,
            sidePercent: sidePercent(sellAmounts.medium, sellTotal),
            value: halfValue(sellAmounts.medium, sellTotal),
            itemStyle: { color: '#52BE80' }
        },
        {
            name: '大单卖出',
            shortName: '大',
            labelColor: '#1E8449',
            side: '卖出',
            actualValue: sellAmounts.large,
            sidePercent: sidePercent(sellAmounts.large, sellTotal),
            value: halfValue(sellAmounts.large, sellTotal),
            itemStyle: { color: '#1E8449' }
        },
        {
            name: '特大单卖出',
            shortName: '特',
            labelColor: '#145A32',
            side: '卖出',
            actualValue: sellAmounts.extraLarge,
            sidePercent: sidePercent(sellAmounts.extraLarge, sellTotal),
            value: halfValue(sellAmounts.extraLarge, sellTotal),
            itemStyle: { color: '#145A32' }
        }
    ]

    return {
        title: {
            text: '买入 / 卖出金额占比',
            left: 8,
            top: 0,
            textStyle: {
                fontSize: 14,
                fontWeight: 600
            }
        },

        tooltip: {
            trigger: 'item',
            formatter(params: any) {
                const data = params.data
                return [
                    `${params.marker}${data.name}`,
                    `金额：${formatAmount(Number(data.actualValue))}`,
                    `${data.side}侧占比：${Number(data.sidePercent).toFixed(2)}%`
                ].join('<br/>')
            }
        },

        graphic: [
            {
                type: 'text',
                left: '18%',
                top: 36,
                style: {
                    text: '卖出',
                    fill: '#196F3D',
                    fontSize: 13,
                    fontWeight: 600
                }
            },
            {
                type: 'text',
                right: '18%',
                top: 36,
                style: {
                    text: '买入',
                    fill: '#922B21',
                    fontSize: 13,
                    fontWeight: 600
                }
            }
        ],

        series: [
            {
                name: '当日买卖拆分',
                type: 'pie',

                // 从正上方开始顺时针：
                // 前 50% 为右半圆买入，后 50% 为左半圆卖出。
                startAngle: 90,
                clockwise: true,

                radius: '66%',
                center: ['50%', '57%'],
                avoidLabelOverlap: true,

                itemStyle: {
                    borderColor: '#fff',
                    borderWidth: 2
                },

                label: {
                    show: true,
                    position: 'outside',
                    alignTo: 'edge',
                    edgeDistance: 10,
                    bleedMargin: 4,
                    distanceToLabelLine: 4,
                    formatter(params: any) {
                        const data = params.data
                        const levelStyleMap: Record<string, string> = {
                            '特大单买入': 'buyExtraLarge',
                            '大单买入': 'buyLarge',
                            '中单买入': 'buyMedium',
                            '小单买入': 'buySmall',
                            '小单卖出': 'sellSmall',
                            '中单卖出': 'sellMedium',
                            '大单卖出': 'sellLarge',
                            '特大单卖出': 'sellExtraLarge'
                        }
                        const levelStyle = levelStyleMap[data.name] || 'level'
                        return [
                            `{${levelStyle}|${data.shortName}}`,
                            `{amount|${formatAmount(Number(data.actualValue))}}`,
                            `{percent|${Number(data.sidePercent).toFixed(1)}%}`
                        ].join(' ')
                    },
                    fontSize: 14,
                    lineHeight: 24,
                    rich: {
                        buyExtraLarge: {
                            color: '#fff',
                            backgroundColor: '#922B21',
                            borderRadius: 11,
                            width: 22,
                            height: 22,
                            align: 'center',
                            verticalAlign: 'middle',
                            fontSize: 14,
                            fontWeight: 700
                        },
                        buyLarge: {
                            color: '#fff',
                            backgroundColor: '#C0392B',
                            borderRadius: 11,
                            width: 22,
                            height: 22,
                            align: 'center',
                            verticalAlign: 'middle',
                            fontSize: 14,
                            fontWeight: 700
                        },
                        buyMedium: {
                            color: '#fff',
                            backgroundColor: '#E74C3C',
                            borderRadius: 11,
                            width: 22,
                            height: 22,
                            align: 'center',
                            verticalAlign: 'middle',
                            fontSize: 14,
                            fontWeight: 700
                        },
                        buySmall: {
                            color: '#7B241C',
                            backgroundColor: '#F5B7B1',
                            borderRadius: 11,
                            width: 22,
                            height: 22,
                            align: 'center',
                            verticalAlign: 'middle',
                            fontSize: 14,
                            fontWeight: 700
                        },
                        sellExtraLarge: {
                            color: '#fff',
                            backgroundColor: '#145A32',
                            borderRadius: 11,
                            width: 22,
                            height: 22,
                            align: 'center',
                            verticalAlign: 'middle',
                            fontSize: 14,
                            fontWeight: 700
                        },
                        sellLarge: {
                            color: '#fff',
                            backgroundColor: '#1E8449',
                            borderRadius: 11,
                            width: 22,
                            height: 22,
                            align: 'center',
                            verticalAlign: 'middle',
                            fontSize: 14,
                            fontWeight: 700
                        },
                        sellMedium: {
                            color: '#fff',
                            backgroundColor: '#52BE80',
                            borderRadius: 11,
                            width: 22,
                            height: 22,
                            align: 'center',
                            verticalAlign: 'middle',
                            fontSize: 14,
                            fontWeight: 700
                        },
                        sellSmall: {
                            color: '#145A32',
                            backgroundColor: '#ABEBC6',
                            borderRadius: 11,
                            width: 22,
                            height: 22,
                            align: 'center',
                            verticalAlign: 'middle',
                            fontSize: 14,
                            fontWeight: 700
                        },
                        level: {
                            color: '#fff',
                            backgroundColor: '#34495E',
                            borderRadius: 11,
                            width: 22,
                            height: 22,
                            align: 'center',
                            verticalAlign: 'middle',
                            fontSize: 14,
                            fontWeight: 700
                        },
                        amount: {
                            color: '#303133',
                            fontSize: 14,
                            fontWeight: 600
                        },
                        percent: {
                            color: '#606266',
                            fontSize: 14,
                            fontWeight: 500
                        }
                    }
                },

                labelLine: {
                    show: true,
                    length: 12,
                    length2: 8
                },

                emphasis: {
                    scale: true,
                    scaleSize: 6,
                    label: {
                        fontWeight: 'bold'
                    }
                },

                data: pieData
            }
        ]
    }
})

const historyFlowOption = computed<EChartsOption>(() => ({
    title: { text: '特大 / 大 / 中 / 小单净流入', left: 8, top: 0, textStyle: { fontSize: 14, fontWeight: 600 } },
    tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow' },
        valueFormatter(value: any) {
            return formatAmount(Number(value))
        }
    },
    legend: {
        orient: 'vertical',
        top: 54,
        right: 8,
        data: ['特大单', '大单', '中单', '小单']
    },
    grid: { left: 58, right: 92, top: 76, bottom: 46 },
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
        { name: '特大单', type: 'bar', stack: 'flow', data: historyRows.value.map(row => row.extraLarge) },
        { name: '大单', type: 'bar', stack: 'flow', data: historyRows.value.map(row => row.large) },
        { name: '中单', type: 'bar', stack: 'flow', data: historyRows.value.map(row => row.medium) },
        { name: '小单', type: 'bar', stack: 'flow', data: historyRows.value.map(row => row.small) }
    ]
}))

const netFlowRows = computed(() => {
    let cumulativeNetMfAmount = 0

    return resolvedMoneyFlowRows.value.map(row => {
        const netMfAmount = normalizeNumber(row.netMfAmount)
        cumulativeNetMfAmount += netMfAmount

        return {
            tradeDate: row.tradeDate,
            netMfAmount,
            cumulativeNetMfAmount
        }
    })
})

const netFlowOption = computed(() => buildDetailLineOption(
    '总净流入额 / 累计净流入额',
    netFlowRows.value,
    [
        { name: '总净流入额', field: 'netMfAmount', yAxisIndex: 0 },
        { name: '累计净流入额', field: 'cumulativeNetMfAmount', yAxisIndex: 1 }
    ],
    undefined,
    [
        { name: '总净流入额', formatter: formatAmount },
        { name: '累计净流入额', formatter: formatAmount }
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
            { name: '净流入额', field: 'netMfAmount', yAxisIndex: 0 },
            { name: '收盘价', field: 'close', yAxisIndex: 1 }
        ],
        undefined,
        [
            { name: '净流入额', formatter: formatAmount },
            { name: '收盘价', formatter: (value: number) => value.toFixed(2) }
        ]
    )
})

function calcNet(buy?: number | null, sell?: number | null): number {
    return (Number(buy) || 0) - (Number(sell) || 0)
}

function buildDetailLineOption<T extends Record<string, any>>(
    title: string,
    rows: T[],
    series: Array<{ name: string; field: keyof T; percent?: boolean; yAxisIndex?: number }>,
    dateField: keyof T = 'tradeDate' as keyof T,
    yAxes?: Array<{ name?: string; formatter?: (value: number) => string }>
): EChartsOption {
    const dates = rows.map(row => String(row[dateField] || ''))

    return {
        title: {
            text: title,
            left: 8,
            top: 0,
            textStyle: {
                fontSize: 14,
                fontWeight: 600
            }
        },
        tooltip: {
            trigger: 'axis'
        },
        legend: {
            top: 28,
            left: 8
        },
        grid: {
            left: 70,
            right: 70,
            top: 76,
            bottom: 46
        },
        dataZoom: [
            {
                type: 'inside',
                start: rows.length > 120 ? 60 : 0,
                end: 100
            },
            {
                type: 'slider',
                bottom: 8,
                height: 18,
                start: rows.length > 120 ? 60 : 0,
                end: 100
            }
        ],
        xAxis: {
            type: 'category',
            boundaryGap: false,
            data: dates,
            axisLabel: {
                hideOverlap: true
            }
        },
        yAxis: yAxes?.length
            ? yAxes.map((axis, index) => ({
                type: 'value',
                name: axis.name,
                scale: true,
                position: index === 0 ? 'left' : 'right',
                axisLabel: {
                    formatter(value: number) {
                        return axis.formatter
                            ? axis.formatter(value)
                            : formatAmount(value)
                    }
                },
                splitLine: {
                    show: index === 0,
                    lineStyle: {
                        type: 'dashed'
                    }
                }
            }))
            : {
                type: 'value',
                scale: true,
                axisLabel: {
                    formatter(value: number) {
                        return formatAmount(value)
                    }
                },
                splitLine: {
                    lineStyle: {
                        type: 'dashed'
                    }
                }
            },
        series: series.map(item => ({
            name: item.name,
            type: 'line',
            smooth: true,
            showSymbol: false,
            connectNulls: true,
            yAxisIndex: item.yAxisIndex ?? 0,
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