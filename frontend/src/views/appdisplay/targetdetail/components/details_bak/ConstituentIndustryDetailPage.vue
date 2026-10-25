<template>
    <div class="constituent-industry-detail-page">
        <el-card class="detail-card" shadow="never">
            <template #header>
                <div class="card-header">
                    <div>
                        <span class="card-title">所属行业 / 指数</span>
                        <span class="card-tip">行业口径以申万三级行业体系为主</span>
                    </div>
                    <el-tag type="info" effect="plain">同行业基准：{{ benchmarkIndustryName }}</el-tag>
                </div>
            </template>

            <el-row :gutter="12">
                <el-col :xs="24" :lg="12">
                    <div class="section-subtitle">申万行业</div>
                    <el-empty v-if="!swIndustryRows.length" description="暂无申万行业归属数据" />
                    <template v-else>
                        <div class="industry-path-panel">
                            <el-breadcrumb separator="/">
                                <el-breadcrumb-item v-for="row in swIndustryRows"
                                    :key="`${row.industryLevel}-${row.industryCode || row.industryName}`">
                                    <span class="breadcrumb-level">{{ industryLevelText(row.industryLevel) }}</span>
                                    <strong>{{ row.industryName }}</strong>
                                </el-breadcrumb-item>
                            </el-breadcrumb>
                        </div>

                        <el-row :gutter="12" class="sw-industry-card-row">
                            <el-col v-for="row in swIndustryRows"
                                :key="`card-${row.industryLevel}-${row.industryCode || row.industryName}`" :xs="24"
                                :md="8">
                                <el-card class="sw-industry-card" shadow="never">
                                    <div class="sw-industry-card-header">
                                        <el-tag size="small" effect="plain">
                                            {{ industryLevelText(row.industryLevel) }}
                                        </el-tag>
                                        <span class="sw-industry-date">{{ row.tradeDate || '-' }}</span>
                                    </div>

                                    <div class="sw-industry-name">{{ row.industryName }}</div>

                                    <div class="sw-industry-metrics">
                                        <div class="sw-industry-metric">
                                            <span>涨跌幅</span>
                                            <strong :class="signedClass(row.chgPct)">
                                                {{ formatPercent(row.chgPct, 2, true) }}
                                            </strong>
                                        </div>
                                        <div class="sw-industry-metric">
                                            <span>总市值</span>
                                            <strong>{{ formatAmount(row.totalMarketValue) }}</strong>
                                        </div>
                                        <div class="sw-industry-metric">
                                            <span>成交额</span>
                                            <strong>{{ formatAmount(row.turnover) }}</strong>
                                        </div>
                                        <div class="sw-industry-metric">
                                            <span>成分数量</span>
                                            <strong>{{ formatNumber(row.stockCount, 0) }}</strong>
                                        </div>
                                    </div>
                                </el-card>
                            </el-col>
                        </el-row>

                        <el-collapse class="industry-detail-collapse">
                            <el-collapse-item title="查看申万行业明细表" name="swIndustryTable">
                                <el-table :data="swIndustryRows" border>
                                    <el-table-column label="层级" width="90">
                                        <template #default="{ row }">{{ industryLevelText(row.industryLevel)
                                            }}</template>
                                    </el-table-column>
                                    <el-table-column prop="industryName" label="行业名称" min-width="130" />
                                    <el-table-column prop="industryCode" label="行业代码" min-width="120" />
                                    <el-table-column prop="parentIndustryName" label="上级行业" min-width="120" />
                                    <el-table-column prop="tradeDate" label="日期" width="110" align="center" />
                                    <el-table-column label="涨跌幅" width="100" align="right">
                                        <template #default="{ row }">
                                            <span :class="signedClass(row.chgPct)">{{ formatPercent(row.chgPct, 2, true)
                                                }}</span>
                                        </template>
                                    </el-table-column>
                                    <el-table-column label="行业总市值" min-width="140" align="right">
                                        <template #default="{ row }">{{ formatAmount(row.totalMarketValue) }}</template>
                                    </el-table-column>
                                    <el-table-column label="行业成交额" min-width="140" align="right">
                                        <template #default="{ row }">{{ formatAmount(row.turnover) }}</template>
                                    </el-table-column>
                                </el-table>
                            </el-collapse-item>
                        </el-collapse>
                    </template>
                </el-col>

                <el-col :xs="24" :lg="12">
                    <div class="section-subtitle">其他指数</div>
                    <el-empty v-if="!belongIndexRows.length" description="暂无所属指数数据" />
                    <template v-else>
                        <div class="index-category-summary">
                            <div v-for="item in belongIndexCategorySummary" :key="item.categoryName"
                                class="index-category-summary-item">
                                <span>{{ item.categoryName }} <strong>{{ item.count }}</strong></span>

                            </div>
                        </div>

                        <div class="belong-index-group-list">
                            <div v-for="group in belongIndexGroups" :key="group.categoryName"
                                class="belong-index-group">
                                <div class="belong-index-group-title">
                                    <span>{{ group.categoryName }}</span>
                                    <el-tag size="small" type="info" effect="plain">{{ group.rows.length }} 个</el-tag>
                                </div>

                                <div class="belong-index-card-list">
                                    <el-card v-for="row in group.rows" :key="row.indexCode" class="belong-index-card"
                                        shadow="never">
                                        <div class="belong-index-card-header">
                                            <div>
                                                <div class="belong-index-name">{{ row.indexName }}</div>
                                                <div class="belong-index-code">{{ row.indexCode }}</div>
                                            </div>
                                            <span :class="signedClass(row.chgPct)" class="belong-index-change">
                                                {{ formatPercent(row.chgPct, 2, true) }}
                                            </span>
                                        </div>

                                        <div class="belong-index-metrics">
                                            <div class="belong-index-metric">
                                                <span>总市值</span>
                                                <strong>{{ formatAmount(row.totalMarketValue) }}</strong>
                                            </div>
                                            <div class="belong-index-metric">
                                                <span>成交额</span>
                                                <strong>{{ formatAmount(row.turnover) }}</strong>
                                            </div>
                                            <div class="belong-index-metric">
                                                <span>权重</span>
                                                <strong>{{ formatPercent(row.weight) }}</strong>
                                            </div>
                                        </div>
                                    </el-card>
                                </div>
                            </div>
                        </div>

                        <el-collapse class="industry-detail-collapse">
                            <el-collapse-item title="查看所属指数明细表" name="belongIndexTable">
                                <el-table :data="belongIndexRows" border height="260">
                                    <el-table-column prop="indexName" label="指数名称" min-width="130" fixed />
                                    <el-table-column prop="indexCode" label="指数代码" min-width="120" />
                                    <el-table-column label="指数类型" width="110">
                                        <template #default="{ row }">{{ normalizeIndexCategoryName(row) }}</template>
                                    </el-table-column>
                                    <el-table-column label="涨跌幅" width="100" align="right">
                                        <template #default="{ row }">
                                            <span :class="signedClass(row.chgPct)">{{ formatPercent(row.chgPct, 2, true)
                                                }}</span>
                                        </template>
                                    </el-table-column>
                                    <el-table-column label="指数总市值" min-width="140" align="right">
                                        <template #default="{ row }">{{ formatAmount(row.totalMarketValue) }}</template>
                                    </el-table-column>
                                    <el-table-column label="指数成交额" min-width="140" align="right">
                                        <template #default="{ row }">{{ formatAmount(row.turnover) }}</template>
                                    </el-table-column>
                                    <el-table-column label="个股权重" width="110" align="right">
                                        <template #default="{ row }">{{ formatPercent(row.weight) }}</template>
                                    </el-table-column>
                                </el-table>
                            </el-collapse-item>
                        </el-collapse>
                    </template>
                </el-col>
            </el-row>
        </el-card>

        <el-card class="detail-card" shadow="never">
            <template #header>
                <div class="card-header">
                    <div>
                        <span class="card-title">同行业对比</span>
                        <span class="card-tip">以申万三级行业“{{ benchmarkIndustryName }}”为基准</span>
                    </div>
                </div>
            </template>
            <el-empty v-if="!benchmarkPeerRows.length" description="暂无同行业可比数据" />
            <el-table v-else :data="benchmarkPeerRows" border height="380">
                <el-table-column prop="fullSymbol" label="股票代码" width="120" fixed />
                <el-table-column prop="name" label="股票名称" min-width="120" />
                <el-table-column prop="close" label="最新价" width="90" align="right" />
                <el-table-column label="涨跌幅" width="100" align="right">
                    <template #default="{ row }">
                        <span :class="signedClass(row.chgPct)">{{ formatPercent(row.chgPct, 2, true) }}</span>
                    </template>
                </el-table-column>
                <el-table-column label="总市值" min-width="150" align="right">
                    <template #default="{ row }">{{ formatAmount(row.totalMarketValue) }}</template>
                </el-table-column>
                <el-table-column prop="peTtm" label="PE(TTM)" width="100" align="right" />
                <el-table-column prop="pb" label="PB" width="90" align="right" />
                <el-table-column prop="psTtm" label="PS(TTM)" width="100" align="right" />
                <el-table-column label="换手率" width="120" align="right">
                    <template #default="{ row }">{{ formatPercent(row.turnoverRate) }}</template>
                </el-table-column>
            </el-table>
        </el-card>

        <el-row :gutter="12">
            <el-col :xs="24" :lg="12">
                <el-card class="detail-card" shadow="never">
                    <template #header>
                        <span class="card-title">行业估值分布</span>
                    </template>
                    <el-empty v-if="!benchmarkPeerRows.length" description="暂无估值分布" />
                    <TargetEcharts v-else :option="peerValuationOption" height="340px" />
                </el-card>
            </el-col>

            <el-col :xs="24" :lg="12">
                <el-card class="detail-card" shadow="never">
                    <template #header>
                        <span class="card-title">行业行情热度</span>
                    </template>
                    <el-empty v-if="!benchmarkPeerRows.length" description="暂无行业行情" />
                    <TargetEcharts v-else :option="peerChangeOption" height="340px" />
                </el-card>
            </el-col>
        </el-row>
    </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { EChartsOption } from 'echarts'
import type {
    ConstituentInfo,
    IndustryDetailData,
    IndustryInfo,
    StockProfile,
    StockBelongIndexRow,
    StockIndustryPeerRow,
    StockSwIndustryRow,
    SwIndustryLevel,
    TargetProfile
} from '/@/types/targetdetail/targetDetailType'
import TargetEcharts from '/@/components/common/TargetEcharts.vue'
import { formatAmount, formatNumber, formatPercent, signedClass } from '/@/utils/marketUtils'
import type { IndexProfile, IndexConstituentDetail } from '/@/types/marketoverview/view'

const props = withDefaults(defineProps<{
    /** 当前标的基础展示信息，沿用现有 TargetProfile 类型。 */
    target: TargetProfile
    /** 现有行业摘要。 */
    industry?: IndustryInfo
    /** 现有指数成分摘要。 */
    constituents?: ConstituentInfo
    /** 指数基础信息，来自 index_basic_data。 */
    indexProfile?: IndexProfile
    /** 股票基础信息，来自 stock_basic_data；本页不再展示股票基础信息，仅作为兜底行业来源。 */
    stockProfile?: StockProfile
    /** 行业详情页扩展数据：申万三级行业归属与所属指数。 */
    industryDetail?: IndustryDetailData
    /** 指数成分明细，来自 index_constituent_data 及关联股票数据。 */
    constituentRows?: IndexConstituentDetail[]
    /** 同行业可比股票明细；股票页默认以申万三级行业过滤。 */
    industryPeerRows?: StockIndustryPeerRow[]
}>(), {
    constituentRows: () => [],
    industryPeerRows: () => []
})

const swIndustryRows = computed<StockSwIndustryRow[]>(() => {
    const rows = (props.industryDetail?.swIndustryRows || []).slice().sort((a, b) => Number(a.industryLevel) - Number(b.industryLevel))

    if (rows.length) return rows

    const fallbackIndustryName = props.industry?.industryName || props.stockProfile?.industry || props.target.industryName

    if (!fallbackIndustryName) return []

    return [
        {
            fullSymbol: props.target.fullSymbol,
            industryName: fallbackIndustryName,
            industryLevel: 3,
            chgPct: props.industry?.avgChangePercent ?? null,
            stockCount: props.industry?.stockCount ?? null
        }
    ]
})

const belongIndexRows = computed<StockBelongIndexRow[]>(() => props.industryDetail?.belongIndexRows || [])

const belongIndexCategorySummary = computed(() => belongIndexGroups.value.map(group => ({
    categoryName: group.categoryName,
    count: group.rows.length
})))

const belongIndexGroups = computed(() => {
    const groupMap = new Map<string, StockBelongIndexRow[]>()

    belongIndexRows.value.forEach(row => {
        const categoryName = normalizeIndexCategoryName(row)
        if (!groupMap.has(categoryName)) {
            groupMap.set(categoryName, [])
        }
        groupMap.get(categoryName)!.push(row)
    })

    return Array.from(groupMap.entries())
        .map(([categoryName, rows]) => ({
            categoryName,
            rows: rows.slice().sort((a, b) => (Number(b.weight) || 0) - (Number(a.weight) || 0))
        }))
        .sort((a, b) => indexCategorySortWeight(a.categoryName) - indexCategorySortWeight(b.categoryName))
})


const benchmarkIndustry = computed(() => {
    const benchmarkLevel = props.industryDetail?.benchmarkIndustryLevel || 3
    const explicitBenchmark = swIndustryRows.value.find(row => row.industryLevel === benchmarkLevel)
    const level3 = swIndustryRows.value.find(row => row.industryLevel === 3)
    const last = swIndustryRows.value[swIndustryRows.value.length - 1]

    return explicitBenchmark || level3 || last
})

const benchmarkIndustryName = computed(() => (
    props.industryDetail?.benchmarkIndustryName ||
    benchmarkIndustry.value?.industryName ||
    props.industry?.industryName ||
    props.stockProfile?.industry ||
    '-'
))

const benchmarkIndustryCode = computed(() => (
    props.industryDetail?.benchmarkIndustryCode ||
    benchmarkIndustry.value?.industryCode ||
    ''
))

const benchmarkPeerRows = computed(() => {
    const benchmarkName = benchmarkIndustryName.value
    const benchmarkCode = benchmarkIndustryCode.value

    const filteredRows = props.industryPeerRows.filter(row => {
        if (benchmarkCode && row.swIndustryLevel3Code) {
            return row.swIndustryLevel3Code === benchmarkCode
        }

        if (benchmarkName && benchmarkName !== '-' && row.swIndustryLevel3Name) {
            return row.swIndustryLevel3Name === benchmarkName
        }

        if (benchmarkName && benchmarkName !== '-' && row.industry) {
            return row.industry === benchmarkName
        }

        return true
    })

    return filteredRows.length ? filteredRows : props.industryPeerRows
})

const peerValuationOption = computed<EChartsOption>(() => buildMultiBarOption(
    '申万三级行业估值分布',
    benchmarkPeerRows.value
        .slice()
        .sort((a, b) => (Number(b.peTtm) || 0) - (Number(a.peTtm) || 0))
        .slice(0, 20)
        .reverse(),
    'name',
    [
        { name: 'PE TTM', key: 'peTtm' },
        { name: 'PB', key: 'pb' },
        { name: 'PS TTM', key: 'psTtm' }
    ],
    true
))

const peerChangeOption = computed<EChartsOption>(() => {
    const rows = [
        { name: '上涨', value: benchmarkPeerRows.value.filter(row => Number(row.chgPct) > 0).length },
        { name: '下跌', value: benchmarkPeerRows.value.filter(row => Number(row.chgPct) < 0).length },
        { name: '平盘', value: benchmarkPeerRows.value.filter(row => Number(row.chgPct) === 0).length }
    ]

    return buildPieOption('申万三级行业行情热度', rows)
})


function normalizeIndexCategoryName(row: StockBelongIndexRow): string {
    const categoryCode = String(row.indexCategory || '').toUpperCase()
    const categoryName = String(row.indexCategoryName || '').trim()

    if (categoryName === '宽基指数') return '规模指数'
    if (categoryName) return categoryName

    if (categoryCode === 'BROAD_BASED' || categoryCode === 'SCALE') return '规模指数'
    if (categoryCode === 'STRATEGY') return '策略指数'
    if (categoryCode === 'THEME') return '主题指数'
    if (categoryCode === 'INDUSTRY') return '行业指数'

    return '其他指数'
}

function indexCategorySortWeight(categoryName: string): number {
    const sortMap: Record<string, number> = {
        规模指数: 1,
        策略指数: 2,
        主题指数: 3,
        行业指数: 4,
        其他指数: 99
    }

    return sortMap[categoryName] || 50
}

function buildPieOption(title: string, rows: Array<{ name: string; value: number }>, percent = false): EChartsOption {
    return {
        tooltip: {
            trigger: 'item',
            formatter(params: any) {
                const value = Number(params.value) || 0
                const displayValue = percent ? formatPercent(value) : formatNumber(value, 0)
                return `${params.marker}${params.name}：${displayValue}（${params.percent}%）`
            }
        },
        legend: {
            type: 'scroll',
            orient: 'vertical',
            right: 4,
            top: 20,
            bottom: 20
        },
        series: [
            {
                name: title,
                type: 'pie',
                radius: ['42%', '68%'],
                center: ['38%', '52%'],
                data: rows,
                label: {
                    formatter(params: any) {
                        const value = Number(params.value) || 0
                        return `${params.name}\n${percent ? formatPercent(value) : formatNumber(value, 0)}`
                    }
                }
            }
        ]
    }
}

function buildMultiBarOption<T extends Record<string, any>>(
    title: string,
    rows: T[],
    nameKey: keyof T,
    seriesConfig: Array<{ name: string; key: keyof T }>,
    horizontal = false
): EChartsOption {
    const names = rows.map(row => String(row[nameKey] || '-'))

    return {
        tooltip: {
            trigger: 'axis',
            axisPointer: { type: 'shadow' },
            formatter(params: any) {
                const items = Array.isArray(params) ? params : [params]
                const name = items[0]?.axisValue || ''
                const lines = items.map((item: any) => `${item.marker}${item.seriesName}：${formatNumber(item.value, 2)}`)
                return [name, ...lines].join('<br/>')
            }
        },
        legend: { top: 0, left: 8 },
        grid: horizontal
            ? { left: 96, right: 24, top: 52, bottom: 24 }
            : { left: 58, right: 24, top: 52, bottom: 42 },
        xAxis: horizontal
            ? {
                type: 'value',
                splitLine: { lineStyle: { type: 'dashed' } }
            }
            : {
                type: 'category',
                data: names,
                axisLabel: { hideOverlap: true }
            },
        yAxis: horizontal
            ? {
                type: 'category',
                data: names
            }
            : {
                type: 'value',
                splitLine: { lineStyle: { type: 'dashed' } }
            },
        series: seriesConfig.map(item => ({
            name: item.name,
            type: 'bar',
            barMaxWidth: 18,
            data: rows.map(row => Number(row[item.key]) || 0)
        }))
    }
}

function industryLevelText(level?: SwIndustryLevel | null): string {
    if (level === 1) return '一级行业'
    if (level === 2) return '二级行业'
    if (level === 3) return '三级行业'
    return '-'
}

</script>

<style scoped>
.constituent-industry-detail-page {
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

.chart-row {
    margin-top: 12px;
    row-gap: 12px;
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

.section-subtitle {
    margin-bottom: 10px;
    font-weight: 600;
    color: var(--el-text-color-primary);
}


.industry-path-panel {
    padding: 12px 14px;
    border: 1px solid var(--el-border-color-light);
    border-radius: 10px;
    background: var(--el-fill-color-light);
}

.industry-path-title {
    margin-bottom: 8px;
    color: var(--el-text-color-secondary);
    font-size: 12px;
}

.breadcrumb-level {
    margin-right: 6px;
    color: var(--el-text-color-secondary);
    font-size: 12px;
}

.sw-industry-card-row {
    margin-top: 12px;
    row-gap: 12px;
}

.sw-industry-card {
    height: 100%;
    border-radius: 10px;
}

.sw-industry-card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
}

.sw-industry-date {
    color: var(--el-text-color-secondary);
    font-size: 12px;
}

.sw-industry-name {
    margin-top: 12px;
    font-size: 18px;
    font-weight: 700;
    color: var(--el-text-color-primary);
}

.sw-industry-metrics {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
    margin-top: 14px;
}

.sw-industry-metric {
    padding: 8px;
    border-radius: 8px;
    background: var(--el-fill-color-lighter);
}

.sw-industry-metric span {
    display: block;
    margin-bottom: 4px;
    color: var(--el-text-color-secondary);
    font-size: 12px;
}

.sw-industry-metric strong {
    font-size: 14px;
    color: var(--el-text-color-primary);
}


.index-category-summary {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 8px;
    margin-bottom: 12px;
}

.index-category-summary-item {
    padding: 10px;
    border: 1px solid var(--el-border-color-light);
    border-radius: 10px;
    background: var(--el-fill-color-light);
}

.index-category-summary-item span {
    display: block;
    color: var(--el-text-color-secondary);
    font-size: 12px;
}

.index-category-summary-item strong {
    display: block;
    margin-top: 4px;
    font-size: 20px;
    color: var(--el-text-color-primary);
}

.belong-index-group-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.belong-index-group {
    padding: 12px;
    border: 1px solid var(--el-border-color-light);
    border-radius: 10px;
    background: var(--el-fill-color-blank);
}

.belong-index-group-title {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 10px;
    font-weight: 600;
    color: var(--el-text-color-primary);
}

.belong-index-card-list {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
}

.belong-index-card {
    border-radius: 10px;
}

.belong-index-card-header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 8px;
}

.belong-index-name {
    font-weight: 700;
    color: var(--el-text-color-primary);
}

.belong-index-code {
    margin-top: 4px;
    color: var(--el-text-color-secondary);
    font-size: 12px;
}

.belong-index-change {
    white-space: nowrap;
    font-weight: 700;
}

.belong-index-metrics {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 8px;
    margin-top: 12px;
}

.belong-index-metric {
    padding: 8px;
    border-radius: 8px;
    background: var(--el-fill-color-lighter);
}

.belong-index-metric span {
    display: block;
    margin-bottom: 4px;
    color: var(--el-text-color-secondary);
    font-size: 12px;
}

.belong-index-metric strong {
    font-size: 13px;
    color: var(--el-text-color-primary);
}

.industry-detail-collapse {
    margin-top: 12px;
}

.note-text {
    margin-top: 12px;
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


@media (max-width: 1200px) {

    .index-category-summary,
    .belong-index-card-list {
        grid-template-columns: repeat(2, minmax(0, 1fr));
    }
}

@media (max-width: 768px) {

    .index-category-summary,
    .belong-index-card-list,
    .belong-index-metrics {
        grid-template-columns: 1fr;
    }

    .card-header {
        align-items: flex-start;
        flex-direction: column;
    }
}
</style>