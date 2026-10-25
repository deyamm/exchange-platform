<template>
    <div class="constituents-tab">
        <el-empty
            v-if="!constituents"
            description="暂无成分股数据"
        />

        <template v-else>
            <section class="summary-grid">
                <el-card
                    v-for="item in summaryCards"
                    :key="item.key"
                    shadow="never"
                    class="summary-card"
                >
                    <div class="summary-label">{{ item.label }}</div>
                    <div class="summary-value" :class="item.valueClass">
                        {{ item.value }}
                    </div>
                    <div class="summary-desc">{{ item.description }}</div>
                </el-card>
            </section>

            <section class="analysis-grid">
                <el-card shadow="never" class="section-card industry-card">
                    <template #header>
                        <div class="card-header">
                            <div>
                                <div class="card-title">行业权重分布</div>
                                <div class="card-subtitle">
                                    {{
                                        isIndustryIndex
                                            ? '行业指数按流通市值占比展示申万行业层级'
                                            : '普通指数按成分权重聚合申万一级行业'
                                    }}
                                </div>
                            </div>
                            <div class="header-actions">
                                <el-tag size="small" effect="plain">
                                    {{ isIndustryIndex ? '行业指数口径' : '普通指数口径' }}
                                </el-tag>
                                <el-button
                                    v-if="isIndustryIndex && selectedIndustryNode"
                                    link
                                    type="primary"
                                    @click="resetIndustryNode"
                                >
                                    返回顶层
                                </el-button>
                            </div>
                        </div>
                    </template>
                    
                    <el-empty
                        v-if="industryChartItems.length === 0"
                        description="暂无行业权重数据"
                        :image-size="72"
                    />

                    <div
                        v-else
                        class="industry-content"
                        :class="{ 'has-tree': isIndustryIndex && industryTree.length > 0 }"
                    >
                        <div
                            v-if="isIndustryIndex && industryTree.length > 0"
                            class="industry-tree-panel"
                        >
                            <div class="tree-title">行业层级</div>
                            <el-tree
                                :data="industryTree"
                                :props="{ label: 'name', children: 'children' }"
                                :expand-on-click-node="false"
                                highlight-current
                                default-expand-all
                                @node-click="selectIndustryNode"
                            >
                                <template #default="{ data }">
                                    <div class="tree-node">
                                        <span class="tree-node-name">{{ data.name }}</span>
                                        <span class="tree-node-weight">
                                            {{ displayPercent(data.weight) }}
                                        </span>
                                    </div>
                                </template>
                            </el-tree>
                        </div>

                        <div class="industry-chart-panel">
                            <div
                                v-if="selectedIndustryNode"
                                class="chart-context"
                            >
                                当前展示：{{ selectedIndustryNode.name }}的下级构成
                            </div>
                            <div class="industry-chart-grid">
                                <TargetEcharts
                                    :option="industryWeightOption"
                                    height="340px"
                                />
                                <TargetEcharts
                                    :option="topIndustryWeightBarOption"
                                    height="340px"
                                />
                            </div>
                        </div>
                    </div>
                </el-card>

                <el-card shadow="never" class="section-card range-card">
                    <template #header>
                        <div class="card-header">
                            <div>
                                <div class="card-title">涨跌幅区间分布</div>
                                <div class="card-subtitle">
                                    上涨区间使用红色，下跌区间使用绿色
                                </div>
                            </div>
                            <span class="header-tip">共 {{ effectiveTotalCount }} 只</span>
                        </div>
                    </template>

                    <el-empty
                        v-if="effectiveTotalCount === 0"
                        description="暂无涨跌幅数据"
                        :image-size="72"
                    />

                    <div v-else class="range-list">
                        <div
                            v-for="item in changeRangeRows"
                            :key="item.rangeCode"
                            class="range-row"
                        >
                            <div class="range-label" :class="item.toneClass">
                                {{ item.label }}
                            </div>
                            <div class="range-track">
                                <div
                                    class="range-bar"
                                    :class="item.barClass"
                                    :style="{ width: `${item.width}%` }"
                                />
                            </div>
                            <div class="range-count">{{ item.count }} 只</div>
                            <div class="range-ratio">{{ item.ratio?.toFixed(1) || 0 }}%</div>
                        </div>
                    </div>
                </el-card>

                <div class="compact-stat-column">
                    <el-card shadow="never" class="section-card compact-card">
                        <template #header>
                            <div class="card-header">
                                <div>
                                    <div class="card-title">涨跌平数量占比</div>
                                    <div class="card-subtitle">仅展示紧凑堆叠条</div>
                                </div>
                            </div>
                        </template>

                        <div class="stacked-stat">
                            <div class="stacked-bar" aria-label="涨跌平数量占比">
                                <div
                                    v-if="breadthRatios.up > 0"
                                    class="stacked-segment segment-up"
                                    :style="{ width: `${breadthRatios.up}%` }"
                                    :title="`上涨 ${breadthCounts.up} 只（${breadthRatios.up.toFixed(1)}%）`"
                                />
                                <div
                                    v-if="breadthRatios.down > 0"
                                    class="stacked-segment segment-down"
                                    :style="{ width: `${breadthRatios.down}%` }"
                                    :title="`下跌 ${breadthCounts.down} 只（${breadthRatios.down.toFixed(1)}%）`"
                                />
                                <div
                                    v-if="breadthRatios.flat > 0"
                                    class="stacked-segment segment-flat"
                                    :style="{ width: `${breadthRatios.flat}%` }"
                                    :title="`平盘 ${breadthCounts.flat} 只（${breadthRatios.flat.toFixed(1)}%）`"
                                />
                            </div>

                            <div class="stacked-legend">
                                <div class="legend-item">
                                    <span class="legend-dot dot-up" />
                                    <span>上涨</span>
                                    <strong>{{ breadthCounts.up }}</strong>
                                    <span>{{ breadthRatios.up.toFixed(1) }}%</span>
                                </div>
                                <div class="legend-item">
                                    <span class="legend-dot dot-down" />
                                    <span>下跌</span>
                                    <strong>{{ breadthCounts.down }}</strong>
                                    <span>{{ breadthRatios.down.toFixed(1) }}%</span>
                                </div>
                                <div class="legend-item">
                                    <span class="legend-dot dot-flat" />
                                    <span>平盘</span>
                                    <strong>{{ breadthCounts.flat }}</strong>
                                    <span>{{ breadthRatios.flat.toFixed(1) }}%</span>
                                </div>
                            </div>
                        </div>
                    </el-card>

                    <el-card shadow="never" class="section-card compact-card">
                        <template #header>
                            <div class="card-header">
                                <div>
                                    <div class="card-title">涨跌停数量占比</div>
                                    <div class="card-subtitle">
                                        分母仅包含涨停数与跌停数
                                    </div>
                                </div>
                            </div>
                        </template>

                        <div v-if="limitTotal > 0" class="stacked-stat">
                            <div class="stacked-bar" aria-label="涨跌停数量占比">
                                <div
                                    v-if="limitRatios.up > 0"
                                    class="stacked-segment segment-up"
                                    :style="{ width: `${limitRatios.up}%` }"
                                    :title="`涨停 ${limitCounts.up} 只（${limitRatios.up.toFixed(1)}%）`"
                                />
                                <div
                                    v-if="limitRatios.down > 0"
                                    class="stacked-segment segment-down"
                                    :style="{ width: `${limitRatios.down}%` }"
                                    :title="`跌停 ${limitCounts.down} 只（${limitRatios.down.toFixed(1)}%）`"
                                />
                            </div>

                            <div class="stacked-legend two-column">
                                <div class="legend-item">
                                    <span class="legend-dot dot-up" />
                                    <span>涨停</span>
                                    <strong>{{ limitCounts.up }}</strong>
                                    <span>{{ limitRatios.up.toFixed(1) }}%</span>
                                </div>
                                <div class="legend-item">
                                    <span class="legend-dot dot-down" />
                                    <span>跌停</span>
                                    <strong>{{ limitCounts.down }}</strong>
                                    <span>{{ limitRatios.down.toFixed(1) }}%</span>
                                </div>
                            </div>
                        </div>

                        <el-empty
                            v-else
                            description="当前无涨跌停成分股"
                            :image-size="58"
                        />
                    </el-card>
                </div>
            </section>

            <el-card shadow="never" class="section-card table-card">
                <template #header>
                    <div class="card-header">
                        <div>
                            <div class="card-title">成分股列表</div>
                            <div class="card-subtitle">
                                按成分权重降序排列
                            </div>
                        </div>
                        <span class="header-tip">
                            数据日期：{{ constituents.tradeDate || '-' }}
                        </span>
                    </div>
                </template>

                <el-table
                    :data="constituents.records"
                    border
                    stripe
                    row-key="stockCode"
                    class="constituent-table"
                >
                    <el-table-column
                        prop="stockCode"
                        label="代码"
                        min-width="126"
                        fixed="left"
                    />
                    <el-table-column
                        prop="name"
                        label="名称"
                        min-width="120"
                        fixed="left"
                    >
                        <template #default="{ row }">
                            {{ row.name || '-' }}
                        </template>
                    </el-table-column>
                    <el-table-column label="行业（申万三级）" min-width="160">
                        <template #default="{ row }">
                            {{ row.industryL3 || row.industry || '-' }}
                        </template>
                    </el-table-column>
                    <el-table-column label="最新价" width="116" align="right">
                        <template #default="{ row }">
                            {{ displayNumber(row.close, 2) }}
                        </template>
                    </el-table-column>
                    <el-table-column label="涨跌幅" width="116" align="right">
                        <template #default="{ row }">
                            <span :class="changeClass(row.chgPct)">
                                {{ displayPercent(row.chgPct, 2, true) }}
                            </span>
                        </template>
                    </el-table-column>
                    <el-table-column label="权重" width="116" align="right">
                        <template #default="{ row }">
                            {{ displayPercent(row.weight, 4) }}
                        </template>
                    </el-table-column>
                </el-table>

                <div class="pagination-wrap">
                    <el-pagination
                        v-model:current-page="pagination.currentPage"
                        v-model:page-size="pagination.pageSize"
                        :page-sizes="[10, 20, 50]"
                        :total="constituents.totalCount"
                        layout="total, sizes, prev, pager, next, jumper"
                        background
                        @current-change="loadTable"
                        @size-change="loadTable"
                    />
                </div>
            </el-card>
        </template>
    </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import type { EChartsOption } from 'echarts'
import type {
    IndexConstituents,
    IndexIndustryWeightItem
} from '/@/types/indexdetail/indexDetailCenter'
import type { IndexConstituentDetail, IndexProfile } from '/@/types/marketoverview/view'
import TargetEcharts from '/@/components/common/TargetEcharts.vue'
import { queryIndexConstituentPage } from '/@/api/indexdetail/indexDetailApi'

const props = defineProps<{
    constituents?: IndexConstituents['constituents']
    profile?: IndexProfile
}>()

const pagination = reactive({
    currentPage: 1,
    pageSize: 10,
})
const selectedIndustryNode = ref<IndexIndustryWeightItem | null>(null)
const constituents = computed(() => props.constituents)
const records = computed<IndexConstituentDetail[]>(
    () => constituents.value?.records || []
)

const effectiveTotalCount = computed(() => {
    return toNumber(constituents.value?.totalCount)
})

const breadthCounts = computed(() => {
    const stats = constituents.value?.breadthStats || []

    const upFromStats = stats.find(item => item.key === 'UP')?.count
    const downFromStats = stats.find(item => item.key === 'DOWN')?.count
    const flatFromStats = stats.find(item => item.key === 'FLAT')?.count

    return {
        up: firstValidCount(
            upFromStats,
            constituents.value?.upCount,
        ),
        down: firstValidCount(
            downFromStats,
            constituents.value?.downCount,
        ),
        flat: firstValidCount(
            flatFromStats,
            constituents.value?.flatCount,
        )
    }
})

const breadthTotal = computed(
    () =>
        breadthCounts.value.up +
        breadthCounts.value.down +
        breadthCounts.value.flat
)

const breadthRatios = computed(() => ({
    up: ratioOf(breadthCounts.value.up, breadthTotal.value),
    down: ratioOf(breadthCounts.value.down, breadthTotal.value),
    flat: ratioOf(breadthCounts.value.flat, breadthTotal.value)
}))

const summaryCards = computed(() => [
    {
        key: 'total',
        label: '成分股数量',
        value: effectiveTotalCount.value,
        valueClass: '',
        description: `数据日期：${constituents.value?.tradeDate || '-'}`
    },
    {
        key: 'up',
        label: '上涨数量',
        value: breadthCounts.value.up,
        valueClass: 'is-up',
        description: `占比 ${breadthRatios.value.up.toFixed(1)}%`
    },
    {
        key: 'down',
        label: '下跌数量',
        value: breadthCounts.value.down,
        valueClass: 'is-down',
        description: `占比 ${breadthRatios.value.down.toFixed(1)}%`
    },
    {
        key: 'flat',
        label: '平盘数量',
        value: breadthCounts.value.flat,
        valueClass: 'is-flat',
        description: `占比 ${breadthRatios.value.flat.toFixed(1)}%`
    },
    {
        key: 'top-ten',
        label: '前十大权重集中度',
        value: displayPercent(props.constituents?.topTenWeightConcentration, 2),
        valueClass: 'is-primary',
        description: '前十大成分股权重之和'
    }
])

const isIndustryIndex = computed(
    () => constituents.value?.industryWeightMode === 'INDUSTRY_INDEX'
)

const industryTree = computed<IndexIndustryWeightItem[]>(
    () => constituents.value?.industryTree || []
)

const regularIndustryWeights = computed<IndexIndustryWeightItem[]>(() => {
    const provided = constituents.value?.industryWeights || []
    if (provided.length > 0) return provided

    const grouped = new Map<string, IndexIndustryWeightItem>()

    records.value.forEach(item => {
        const name = item.industryL1 || item.industry || '其他'
        const current = grouped.get(name) || {
            name,
            weight: 0,
            constituentCount: 0
        }
        current.weight += toNumber(item.weight)
        current.constituentCount += 1
        grouped.set(name, current)
    })

    const sorted = Array.from(grouped.values())
        .sort((a, b) => b.weight - a.weight)

    let cumulative = 0
    const visible: IndexIndustryWeightItem[] = []
    let otherWeight = 0
    let otherCount = 0

    sorted.forEach(item => {
        if (cumulative < 80) {
            visible.push(item)
            cumulative += item.weight
        } else {
            otherWeight += item.weight
            otherCount += item.constituentCount
        }
    })

    if (otherCount > 0) {
        visible.push({
            name: '其他',
            weight: otherWeight,
            constituentCount: otherCount
        })
    }

    return visible
})

const industryChartItems = computed<IndexIndustryWeightItem[]>(() => {
    if (!isIndustryIndex.value) return regularIndustryWeights.value

    if (
        selectedIndustryNode.value?.children &&
        selectedIndustryNode.value.children.length > 0
    ) {
        return selectedIndustryNode.value.children
    }

    return industryTree.value
})

const topIndustryWeightItems = computed<IndexIndustryWeightItem[]>(() =>
    [...industryChartItems.value]
        .sort((a, b) => toNumber(b.weight) - toNumber(a.weight))
        .slice(0, 10)
)

const industryWeightOption = computed<EChartsOption>(() => ({
    tooltip: {
        trigger: 'item',
        formatter(params: any) {
            const data = params.data || {}
            return [
                params.name,
                `权重：${displayPercent(params.value, 2)}`,
                `成分股数量：${data.constituentCount ?? 0}`
            ].join('<br/>')
        }
    },
    legend: { show: false },
    series: [
        {
            name: '行业权重',
            type: 'pie',
            radius: ['42%', '70%'],
            center: ['50%', '50%'],
            minAngle: 2,
            avoidLabelOverlap: true,
            label: {
                show: true,
                formatter(params: any) {
                    return `${params.name}\n${Number(params.percent || 0).toFixed(1)}%`
                },
                lineHeight: 18
            },
            labelLine: {
                length: 12,
                length2: 10
            },
            emphasis: {
                scale: true,
                scaleSize: 6
            },
            data: industryChartItems.value.map(item => ({
                name: item.name,
                value: toNumber(item.weight),
                constituentCount: toNumber(item.constituentCount)
            }))
        }
    ]
}))

const topIndustryWeightBarOption = computed<EChartsOption>(() => ({
    title: {
        text: '前10权重行业',
        left: 0,
        top: 0,
        textStyle: {
            fontSize: 13,
            fontWeight: 600
        }
    },
    grid: {
        top: 42,
        right: 36,
        bottom: 18,
        left: 12,
        containLabel: true
    },
    tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow' },
        formatter(params: any) {
            const param = Array.isArray(params) ? params[0] : params
            const data = param?.data || {}
            return [
                param?.name || '',
                `权重：${displayPercent(data.value, 2)}`,
                `成分股数量：${data.constituentCount ?? 0}`
            ].join('<br/>')
        }
    },
    xAxis: {
        type: 'value',
        axisLabel: {
            formatter: '{value}%'
        },
        splitLine: {
            lineStyle: {
                type: 'dashed'
            }
        }
    },
    yAxis: {
        type: 'category',
        inverse: true,
        axisTick: { show: false },
        axisLine: { show: false },
        axisLabel: {
            width: 92,
            overflow: 'truncate'
        },
        data: topIndustryWeightItems.value.map(item => item.name)
    },
    series: [
        {
            name: '权重',
            type: 'bar',
            barMaxWidth: 14,
            label: {
                show: true,
                position: 'right',
                formatter(params: any) {
                    return displayPercent(params.value, 2)
                }
            },
            data: topIndustryWeightItems.value.map(item => ({
                value: toNumber(item.weight),
                constituentCount: toNumber(item.constituentCount)
            }))
        }
    ]
}))

const changeRangeRows = computed(() => {
    const changeRanges = props.constituents?.changeRanges || []
        
    const rows = changeRanges.map(item => {
        return {
            ...item,
            barClass:
                item.direction > 0
                    ? 'bar-up'
                    : item.direction < 0
                        ? 'bar-down'
                        : 'bar-flat',
            toneClass:
                item.direction > 0
                    ? 'is-up'
                    : item.direction < 0
                        ? 'is-down'
                        : 'is-flat'
        }
    })

    const maxCount = Math.max(...rows.map(item => item.count), 1)
    return rows.map(item => ({
        ...item,
        width: item.count === 0 ? 0 : Math.max((item.count / maxCount) * 100, 2)
    }))
})

const limitCounts = computed(() => ({
    up: firstValidCount(
        constituents.value?.limitStats?.limitUpCount
    ),
    down: firstValidCount(
        constituents.value?.limitStats?.limitDownCount
    )
}))

const limitTotal = computed(() => limitCounts.value.up + limitCounts.value.down)

const limitRatios = computed(() => ({
    up: ratioOf(limitCounts.value.up, limitTotal.value),
    down: ratioOf(limitCounts.value.down, limitTotal.value)
}))

function selectIndustryNode(data: IndexIndustryWeightItem): void {
    if (data.children && data.children.length > 0) {
        selectedIndustryNode.value = data
    }
}

function resetIndustryNode(): void {
    selectedIndustryNode.value = null
}

function firstValidCount(...values: Array<number | null | undefined>): number {
    for (const value of values) {
        const numberValue = toNullableNumber(value)
        if (numberValue !== null && numberValue >= 0) {
            return Math.round(numberValue)
        }
    }
    return 0
}

function ratioOf(value: number, total: number): number {
    if (total <= 0) return 0
    return (value / total) * 100
}

function toNumber(value: unknown): number {
    const result = Number(value)
    return Number.isFinite(result) ? result : 0
}

function toNullableNumber(value: unknown): number | null {
    if (value === null || value === undefined || value === '') return null
    const result = Number(value)
    return Number.isFinite(result) ? result : null
}

function displayNumber(
    value: number | null | undefined,
    digits = 2
): string {
    const numberValue = toNullableNumber(value)
    if (numberValue === null) return '-'

    return numberValue.toLocaleString('zh-CN', {
        minimumFractionDigits: digits,
        maximumFractionDigits: digits
    })
}

function displayPercent(
    value: number | null | undefined,
    digits = 2,
    showSign = false
): string {
    const numberValue = toNullableNumber(value)
    if (numberValue === null) return '-'

    const sign = showSign && numberValue > 0 ? '+' : ''
    return `${sign}${numberValue.toFixed(digits)}%`
}

function changeClass(value: number | null | undefined): string {
    const numberValue = toNullableNumber(value)
    if (numberValue === null || numberValue === 0) return 'is-flat'
    return numberValue > 0 ? 'is-up' : 'is-down'
}

async function loadTable(): Promise<void> {
    if (!constituents.value || !props.profile) return

    try {
        const response = await queryIndexConstituentPage(
            props.profile.fullSymbol,
            props.constituents?.tradeDate || undefined,
            pagination.currentPage,
            pagination.pageSize
        )
        if (response && response.records) {
            constituents.value.records = response.records
        }
    } catch (error) {
        console.error('Failed to fetch constituent page:', error)
    }
}
</script>

<style scoped>
.constituents-tab {
    display: flex;
    flex-direction: column;
    gap: 14px;
}

.summary-grid {
    display: grid;
    grid-template-columns: repeat(5, minmax(0, 1fr));
    gap: 12px;
}

.summary-card,
.section-card {
    border-radius: 10px;
}

.summary-card :deep(.el-card__body) {
    padding: 18px;
}

.summary-label {
    color: var(--el-text-color-secondary);
    font-size: 13px;
}

.summary-value {
    margin-top: 10px;
    color: var(--el-text-color-primary);
    font-size: 25px;
    font-weight: 700;
    line-height: 1.2;
}

.summary-desc {
    min-height: 18px;
    margin-top: 8px;
    color: var(--el-text-color-secondary);
    font-size: 12px;
    line-height: 18px;
}

.analysis-grid {
    display: grid;
    grid-template-columns: minmax(0, 1.45fr) minmax(0, 1fr);
    gap: 14px;
    align-items: stretch;
}

.industry-card {
    grid-column: 1 / -1;
}

.range-card {
    min-height: 390px;
}

.compact-stat-column {
    display: grid;
    grid-template-rows: repeat(2, minmax(0, 1fr));
    gap: 14px;
}

.compact-card {
    min-height: 188px;
}

.card-header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 16px;
}

.card-title {
    color: var(--el-text-color-primary);
    font-size: 16px;
    font-weight: 600;
    line-height: 22px;
}

.card-subtitle {
    margin-top: 3px;
    color: var(--el-text-color-secondary);
    font-size: 12px;
    font-weight: 400;
    line-height: 18px;
}

.header-actions {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-shrink: 0;
}

.header-tip {
    color: var(--el-text-color-secondary);
    font-size: 12px;
    white-space: nowrap;
}

.industry-content {
    min-height: 340px;
}

.industry-content.has-tree {
    display: grid;
    grid-template-columns: minmax(220px, 0.36fr) minmax(0, 1fr);
    gap: 16px;
}

.industry-tree-panel {
    max-height: 340px;
    padding: 12px;
    overflow: auto;
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 8px;
    background: var(--el-fill-color-extra-light);
}

.tree-title {
    margin-bottom: 8px;
    color: var(--el-text-color-secondary);
    font-size: 12px;
}

.industry-tree-panel :deep(.el-tree) {
    background: transparent;
}

.tree-node {
    display: flex;
    align-items: center;
    justify-content: space-between;
    width: 100%;
    min-width: 0;
    gap: 10px;
    padding-right: 6px;
}

.tree-node-name {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.tree-node-weight {
    color: var(--el-text-color-secondary);
    font-size: 12px;
    white-space: nowrap;
}

.industry-chart-panel {
    min-width: 0;
}

.industry-chart-grid {
    display: grid;
    grid-template-columns: minmax(0, 1fr) minmax(300px, 0.82fr);
    gap: 16px;
    align-items: stretch;
}

.chart-context {
    margin-bottom: 8px;
    color: var(--el-text-color-secondary);
    font-size: 12px;
    position: relative;
    z-index: 1;
}

.range-list {
    display: flex;
    flex-direction: column;
    gap: 11px;
}

.range-row {
    display: grid;
    grid-template-columns: 102px minmax(90px, 1fr) 52px 50px;
    align-items: center;
    gap: 10px;
}

.range-label,
.range-count,
.range-ratio {
    font-size: 12px;
    white-space: nowrap;
}

.range-count,
.range-ratio {
    color: var(--el-text-color-secondary);
    text-align: right;
}

.range-track {
    height: 12px;
    overflow: hidden;
    border-radius: 999px;
    background: var(--el-fill-color-light);
}

.range-bar {
    min-width: 0;
    height: 100%;
    border-radius: inherit;
    transition: width 0.25s ease;
}

.bar-up {
    background: #f56c6c;
}

.bar-down {
    background: #67c23a;
}

.bar-flat {
    background: #909399;
}

.stacked-stat {
    display: flex;
    flex-direction: column;
    gap: 18px;
}

.stacked-bar {
    display: flex;
    width: 100%;
    height: 18px;
    overflow: hidden;
    border-radius: 999px;
    background: var(--el-fill-color-light);
}

.stacked-segment {
    min-width: 0;
    height: 100%;
    transition: width 0.25s ease;
}

.segment-up {
    background: #f56c6c;
}

.segment-down {
    background: #67c23a;
}

.segment-flat {
    background: #909399;
}

.stacked-legend {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 10px;
}

.stacked-legend.two-column {
    grid-template-columns: repeat(2, minmax(0, 1fr));
}

.legend-item {
    display: grid;
    grid-template-columns: 8px auto 1fr auto;
    align-items: center;
    gap: 6px;
    color: var(--el-text-color-secondary);
    font-size: 12px;
}

.legend-item strong {
    color: var(--el-text-color-primary);
    text-align: right;
}

.legend-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
}

.dot-up {
    background: #f56c6c;
}

.dot-down {
    background: #67c23a;
}

.dot-flat {
    background: #909399;
}

.table-card {
    overflow: hidden;
}

.constituent-table {
    width: 100%;
}

.pagination-wrap {
    display: flex;
    justify-content: flex-end;
    margin-top: 16px;
    overflow-x: auto;
}

.is-up {
    color: #f56c6c;
}

.is-down {
    color: #67c23a;
}

.is-flat {
    color: #909399;
}

.is-primary {
    color: var(--el-color-primary);
}

@media (max-width: 1200px) {
    .summary-grid {
        grid-template-columns: repeat(3, minmax(0, 1fr));
    }
}

@media (max-width: 992px) {
    .analysis-grid {
        grid-template-columns: 1fr;
    }

    .industry-card {
        grid-column: auto;
    }

    .industry-content.has-tree {
        grid-template-columns: 1fr;
    }

    .industry-chart-grid {
        grid-template-columns: 1fr;
    }

    .industry-tree-panel {
        max-height: 240px;
    }
}

@media (max-width: 768px) {
    .summary-grid {
        grid-template-columns: repeat(2, minmax(0, 1fr));
    }

    .card-header {
        flex-direction: column;
        align-items: stretch;
        gap: 8px;
    }

    .header-actions {
        justify-content: space-between;
    }

    .range-row {
        grid-template-columns: 92px minmax(70px, 1fr) 44px;
    }

    .range-ratio {
        display: none;
    }

    .stacked-legend,
    .stacked-legend.two-column {
        grid-template-columns: 1fr;
    }
}

@media (max-width: 520px) {
    .summary-grid {
        grid-template-columns: 1fr;
    }

    .range-row {
        grid-template-columns: 88px minmax(64px, 1fr) 42px;
        gap: 7px;
    }
}
</style>