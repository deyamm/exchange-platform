<template>
    <section class="index-page">
        <header class="page-header">
            <div>
                <p class="eyebrow">Tushare index_basic</p>
                <h1>指数基本信息</h1>
                <p class="subtitle">
                    按市场、指数类别、发布方多维查看指数目录，适合快速了解有哪些发布方、类别以及各维度下的指数。
                </p>
            </div>
        </header>

        <section class="stats-grid">
            <el-card shadow="never" class="stat-card">
                <span>指数总数</span>
                <strong>{{ filteredRows.length }}</strong>
            </el-card>

            <el-card shadow="never" class="stat-card">
                <span>发布方</span>
                <strong>{{ publisherOptions.length }}</strong>
            </el-card>

            <el-card shadow="never" class="stat-card">
                <span>指数类别</span>
                <strong>{{ categoryOptions.length }}</strong>
            </el-card>

            <el-card shadow="never" class="stat-card">
                <span>市场</span>
                <strong>{{ marketOptions.length }}</strong>
            </el-card>
        </section>

        <el-card shadow="never" class="filters-card">
            <el-form class="filters" label-position="top">
                <el-form-item label="市场代码">
                    <el-select v-model="selectedMarket" placeholder="全部市场" clearable>
                        <el-option label="全部市场" value="" />
                        <el-option
                            v-for="market in marketOptions"
                            :key="market"
                            :label="`${market} - ${marketNameMap[market] || '未知市场'}`"
                            :value="market"
                        />
                    </el-select>
                </el-form-item>

                <el-form-item label="指数类别">
                    <el-select v-model="selectedCategory" placeholder="全部类别" clearable>
                        <el-option label="全部类别" value="" />
                        <el-option
                            v-for="category in categoryOptions"
                            :key="category"
                            :label="category"
                            :value="category"
                        />
                    </el-select>
                </el-form-item>

                <el-form-item label="发布方">
                    <el-select v-model="selectedPublisher" placeholder="全部发布方" clearable>
                        <el-option label="全部发布方" value="" />
                        <el-option
                            v-for="publisher in publisherOptions"
                            :key="publisher"
                            :label="publisher"
                            :value="publisher"
                        />
                    </el-select>
                </el-form-item>

                <el-form-item label="关键词" class="search-box">
                    <el-input
                        v-model="keyword"
                        type="search"
                        clearable
                        placeholder="搜索代码、简称、市场、类别等..."
                    />
                </el-form-item>

                <el-form-item class="reset-item">
                    <el-button class="ghost-button" @click="resetFilters">
                        重置
                    </el-button>
                </el-form-item>
            </el-form>
        </el-card>

        <section class="layout">
            <el-card shadow="never" class="main-panel">
                <div class="panel-title">
                    <div>
                        <h2>市场 × 类别矩阵</h2>
                        <p>点击数字可快速定位到对应市场和类别。</p>
                    </div>
                </div>

                <div class="matrix-wrap">
                    <el-table :data="matrixRows" border class="matrix-el-table" style="width: 100%">
                        <el-table-column label="市场 / 类别" fixed min-width="100">
                            <template #default="{ row }">
                                <el-button
                                    type="primary"
                                    link
                                    class="link-button"
                                    @click="selectedMarket = row.market"
                                >
                                    {{ row.market }}
                                </el-button>
                                <small class="market-name">{{ row.marketName }}</small>
                            </template>
                        </el-table-column>

                        <el-table-column
                            v-for="category in topCategories"
                            :key="category"
                            :label="category"
                            min-width="90"
                            align="center"
                        >
                            <template #default="{ row }">
                                <el-button
                                    v-if="row.counts[category] > 0"
                                    text
                                    round
                                    class="count-pill"
                                    @click="pickMatrixCell(row.market, category)"
                                >
                                    {{ row.counts[category] }}
                                </el-button>
                                <span v-else class="empty-cell">—</span>
                            </template>
                        </el-table-column>
                    </el-table>
                </div>

                <div class="panel-title detail-title">
                    <div>
                        <h2>指数明细</h2>
                        <p>当前筛选结果 {{ filteredRows.length }} 条。</p>
                    </div>
                </div>

                <div class="table-wrap">
                    <el-table
                        :data="pagedRows"
                        border
                        stripe
                        class="index-el-table"
                        style="width: 100%"
                        empty-text="没有匹配的指数"
                    >
                        <el-table-column label="代码" width="120" fixed>
                            <template #default="{ row }">
                                <span class="code">{{ row.fullSymbol }}</span>
                            </template>
                        </el-table-column>

                        <el-table-column prop="name" label="简称" width="130" />

                        <el-table-column prop="market" label="市场" width="80" />

                        <el-table-column prop="publisher" label="发布方" min-width="140" show-overflow-tooltip />

                        <el-table-column label="类别" min-width="120">
                            <template #default="{ row }">
                                <el-tag class="tag" type="info" effect="plain" round>
                                    {{ row.category || '未分类' }}
                                </el-tag>
                            </template>
                        </el-table-column>

                        <el-table-column label="基期" width="110">
                            <template #default="{ row }">
                                {{ formatDate(row.baseDate) }}
                            </template>
                        </el-table-column>

                        <el-table-column label="发布日期" width="110">
                            <template #default="{ row }">
                                {{ formatDate(row.listDate) }}
                            </template>
                        </el-table-column>

                        <el-table-column label="退市日期" width="110">
                            <template #default="{ row }">
                                {{ formatDate(row.expDate) }}
                            </template>
                        </el-table-column>

                        <el-table-column label="操作" width="110" fixed="right" align="center">
                            <template #default="{ row }">
                                <el-button type="primary" link @click="openOptionalDialog(row)">
                                    加自选
                                </el-button>
                            </template>
                        </el-table-column>
                    </el-table>
                </div>

                <footer class="pager">
                    <el-pagination
                        v-model:current-page="page"
                        :page-size="pageSize"
                        :total="filteredRows.length"
                        layout="total, prev, pager, next"
                        background
                    />
                </footer>
            </el-card>

            <aside class="side-panel">
                <el-card shadow="never" class="card">
                    <h2>发布方分布</h2>
                    <p class="hint">用于快速查看有哪些指数发布方，以及各自覆盖多少指数。</p>

                    <div class="bar-list">
                        <el-button
                            v-for="item in publisherStats"
                            :key="item.name"
                            text
                            class="bar-row"
                            @click="selectedPublisher = item.name"
                        >
                            <span class="bar-row-content">
                                <span class="bar-name">{{ item.name }}</span>
                                <span class="bar-track">
                                    <span class="bar-fill" :style="{ width: `${item.percent}%` }" />
                                </span>
                                <strong>{{ item.count }}</strong>
                            </span>
                        </el-button>
                    </div>
                </el-card>

                <el-card shadow="never" class="card">
                    <h2>类别分布</h2>
                    <p class="hint">用于判断当前数据中有哪些类别指数。</p>

                    <div class="chip-list">
                        <el-button
                            v-for="item in categoryStats"
                            :key="item.name"
                            round
                            class="chip"
                            @click="selectedCategory = item.name"
                        >
                            <span>{{ item.name }}</span>
                            <strong>{{ item.count }}</strong>
                        </el-button>
                    </div>
                </el-card>

                <el-card shadow="never" class="card">
                    <h2>市场分布</h2>
                    <div class="market-list">
                        <el-button
                            v-for="item in marketStats"
                            :key="item.name"
                            text
                            class="market-item"
                            @click="selectedMarket = item.name"
                        >
                            <span>
                                <strong>{{ item.name }}</strong>
                                <small>{{ marketNameMap[item.name] || '未知市场' }}</small>
                            </span>
                            <em>{{ item.count }}</em>
                        </el-button>
                    </div>
                </el-card>
            </aside>
        </section>

        <AddTargetFormDialog
            v-model="optionalDialogVisible"
            :title="dialogTitle"
            :target-code="activeOptionalIndex.fullSymbol"
            :target-name="activeOptionalIndex.name"
            target-type="INDEX"
            @confirm="handleAddOptionalConfirm"
        />
    </section>
</template>


<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { getIndexBasic } from '/@/api/marketoverview/marketOverviewApi'
import { IndexBasic, MatrixRow } from '/@/types/marketoverview/entity'
import AddTargetFormDialog from '/@/components/common/AddTargetFormDialog.vue'
import { saveOrUpdateWatchTarget } from '/@/api/watchtarget/watchTargetApi'
import type { WatchTargetSaveRequest } from '/@/types/watchtarget/constants'
import { ElMessage } from 'element-plus'


const marketNameMap: Record<string, string> = {
    MSCI: 'MSCI指数',
    CSI: '中证指数',
    SSE: '上交所指数',
    SZSE: '深交所指数',
    CICC: '中金指数',
    SW: '申万指数',
    CNI: '国证指数',
    OTH: '其他指数'
}

const indexBasicData = ref<IndexBasic[]>([])

const selectedMarket = ref('')
const selectedCategory = ref('')
const selectedPublisher = ref('')
const keyword = ref('')
const page = ref(1)
const pageSize = 20
const dialogTitle = '添加指数自选'

const optionalDialogVisible = ref(false)
const activeOptionalIndex = ref<IndexBasic>({} as IndexBasic)

async function loadData() {
    indexBasicData.value = await getIndexBasic({})
}

// 构建市场 × 类别矩阵数据，每个单元格统计对应市场和类别的指数数量
const matrixRows = computed<MatrixRow[]>(() =>
    marketOptions.value.map((market) => ({
        market,
        marketName: marketNameMap[market] || 'OTH',
        counts: Object.fromEntries(
            topCategories.value.map((category) => [category, matrixCount(market, category)])
        )
    }))
)

const normalizedRows = computed(() =>
    indexBasicData.value.map((item) => ({
        ...item,
        market: item.market?.trim() || 'OTH',
        publisher: item.publisher?.trim() || '未知发布方',
        category: item.category?.trim() || '未分类'
    }))
)

// 过滤掉市场、发布方、类别等字段中的空值等异常数据，避免在筛选和统计时出现问题
const marketOptions = computed(() =>
    unique(normalizedRows.value.map((item) => item.market).filter(Boolean))
)

const categoryOptions = computed(() =>
    unique(normalizedRows.value.map((item) => item.category).filter(Boolean))
)

const publisherOptions = computed(() =>
    unique(normalizedRows.value.map((item) => item.publisher).filter(Boolean))
)

const filteredRows = computed(() => {
    const kw = keyword.value.trim().toLowerCase()

    return normalizedRows.value.filter((row) => {
        // 3 种筛选条件，条件为空或匹配成功则返回
        const matchMarket = !selectedMarket.value || row.market === selectedMarket.value
        const matchCategory = !selectedCategory.value || row.category === selectedCategory.value
        const matchPublisher = !selectedPublisher.value || row.publisher === selectedPublisher.value

        const haystack = [row.fullSymbol, row.name, row.market, row.publisher, row.category]
            .filter(Boolean)
            .join(' ')
            .toLowerCase()

        const matchKeyword = !kw || haystack.includes(kw)
        // 仅当 4 个条件全部满足时才返回
        return matchMarket && matchCategory && matchPublisher && matchKeyword
    })
})

const pagedRows = computed(() => {
    const start = (page.value - 1) * pageSize
    return filteredRows.value.slice(start, start + pageSize)
})

// 统计函数，统计category、market、publisher等字段的分布情况，用于侧边栏展示
const categoryStats = computed(() =>
    toStats(filteredRows.value.map((item) => item.category || '未分类'))
)

const marketStats = computed(() =>
    toStats(filteredRows.value.map((item) => item.market || 'OTH'))
)

// 发布方还提供了百分比信息，用于柱状图展示
const publisherStats = computed(() => {
    const stats = toStats(filteredRows.value.map((item) => item.publisher || '未知发布方'))
    const max = Math.max(...stats.map((item) => item.count), 1)

    return stats.slice(0, 12).map((item) => ({
        ...item,
        percent: Math.round((item.count / max) * 100)
    }))
})

const topCategories = computed(() =>
    categoryStats.value.slice(0, 12).map((item) => item.name)
)

function matrixCount(market: string, category: string) {
    return normalizedRows.value.filter(
        (item) => item.market === market && item.category === category
    ).length
}

function pickMatrixCell(market: string, category: string) {
    selectedMarket.value = market
    selectedCategory.value = category
    page.value = 1
}

function resetFilters() {
    selectedMarket.value = ''
    selectedCategory.value = ''
    selectedPublisher.value = ''
    keyword.value = ''
    page.value = 1
}

function openOptionalDialog(row: IndexBasic) {
    activeOptionalIndex.value = row
    optionalDialogVisible.value = true
}

async function handleAddOptionalConfirm(payload: {
    targetCode: string
    targetType: string
    groupCodes: string[]
}) {
    const { targetCode, targetType, groupCodes } = payload
    const request: WatchTargetSaveRequest = {
        targetCode: targetCode,
        targetName: activeOptionalIndex.value.name,
        targetType: targetType,
        marketCode: activeOptionalIndex.value.market,
        groupCodes: groupCodes,
        sortNo: 1,
        importantFlag: false,
        watchStatus: 'WATCHING',
        watchReason: undefined
    }
    try {
        await saveOrUpdateWatchTarget(request)
        if (groupCodes.length > 0) {
            ElMessage.success(`已添加指数 ${targetCode} 到自选`)
        } else {
            ElMessage.success(`已删除指数 ${targetCode} 的自选`)
        }
    } catch (error) {
        ElMessage.error('添加自选失败')
    }
}

function unique(values: string[]) {
    return Array.from(new Set(values)).sort((a, b) => a.localeCompare(b, 'zh-CN'))
}

// 将字符串数组转换为 { name, count } 形式的统计数据，并按 count 降序、name 升序排序
function toStats(values: string[]) {
    const map = new Map<string, number>()

    // 统计每个值出现的次数
    values.forEach((value) => {
        map.set(value, (map.get(value) || 0) + 1)
    })

    // 转换为数组并排序，先按 count 降序，再按 name 升序
    return Array.from(map.entries())
        .map(([name, count]) => ({ name, count }))
        .sort((a, b) => b.count - a.count || a.name.localeCompare(b.name, 'zh-CN'))
}

function formatDate(value?: string) {
    if (!value || value.length !== 8) return value || '-'
    return `${value.slice(0, 4)}-${value.slice(4, 6)}-${value.slice(6, 8)}`
}

watch([selectedMarket, selectedCategory, selectedPublisher, keyword], () => {
    page.value = 1
})

onMounted(() => {
    loadData()
})
</script>

<style scoped>
.index-page {
    min-height: 100vh;
    padding: 28px;
    background: #f6f7fb;
    color: #1f2937;
}

.page-header {
    display: flex;
    justify-content: space-between;
    gap: 24px;
    margin-bottom: 20px;
}

.eyebrow {
    margin: 0 0 6px;
    color: #64748b;
    font-size: 13px;
    letter-spacing: 0.08em;
    text-transform: uppercase;
}

.bar-row {
    width: 100%;
    height: auto;
    margin-left: 0 !important;
    padding: 8px 0 !important;
    border: 0;
    background: transparent;
    color: #1f2937;
    text-align: left;
    cursor: pointer;
}

.bar-row :deep(> span) {
    width: 100%;
}

.bar-row-content {
    display: grid;
    grid-template-columns: 86px 1fr 42px;
    gap: 10px;
    align-items: center;
    width: 100%;
}

.bar-name {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.bar-track {
    display: block;
    height: 8px;
    overflow: hidden;
    border-radius: 999px;
    background: #e5e7eb;
}

.bar-fill {
    display: block;
    height: 100%;
    border-radius: inherit;
    background: #3b82f6;
}

.subtitle,
.hint,
.panel-title p {
    margin: 8px 0 0;
    color: #64748b;
    font-size: 14px;
}

.stats-grid {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 14px;
    margin-bottom: 16px;
}

.stat-card,
.card,
.main-panel,
.filters-card {
    border: 1px solid #e5e7eb;
    border-radius: 18px;
    box-shadow: 0 12px 32px rgba(15, 23, 42, 0.04);
}

.stat-card {
    background: #fff;
}

.stat-card :deep(.el-card__body) {
    padding: 18px;
}

.card :deep(.el-card__body),
.main-panel :deep(.el-card__body) {
    padding: 18px;
}

.filters-card {
    margin-bottom: 16px;
    background: #fff;
}

.filters-card :deep(.el-card__body) {
    padding: 16px;
}

.stat-card span {
    display: block;
    color: #64748b;
    font-size: 13px;
}

.stat-card strong {
    display: block;
    margin-top: 8px;
    font-size: 28px;
}

.filters {
    display: grid;
    grid-template-columns: 180px 180px 200px minmax(220px, 1fr) 92px;
    gap: 12px;
    align-items: end;
}

.filters :deep(.el-form-item) {
    margin-bottom: 0;
}

.filters :deep(.el-form-item__label) {
    height: auto;
    margin-bottom: 6px;
    padding: 0;
    color: #475569;
    font-size: 13px;
    line-height: 1.4;
}

.filters :deep(.el-select),
.filters :deep(.el-input) {
    width: 100%;
}

.filters :deep(.el-input__wrapper),
.filters :deep(.el-select__wrapper) {
    min-height: 38px;
    border-radius: 10px;
    box-shadow: 0 0 0 1px #d1d5db inset;
}

.filters :deep(.el-input__wrapper.is-focus),
.filters :deep(.el-select__wrapper.is-focused) {
    box-shadow: 0 0 0 1px #409eff inset;
}

.reset-item :deep(.el-form-item__content) {
    align-items: flex-end;
}

.ghost-button {
    width: 100%;
    height: 38px;
    border-radius: 10px;
}

.layout {
    display: grid;
    grid-template-columns: minmax(0, 1fr) 340px;
    gap: 16px;
}

.main-panel {
    min-width: 0;
    background: #fff;
}

.side-panel {
    display: grid;
    gap: 16px;
    align-content: start;
}

.card {
    background: #fff;
}

.card h2,
.panel-title h2 {
    margin: 0;
}

.panel-title {
    display: flex;
    justify-content: space-between;
    gap: 16px;
    margin-bottom: 14px;
}

.detail-title {
    margin-top: 20px;
}

.matrix-wrap,
.table-wrap {
    overflow: auto;
}

.pager {
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 12px;
    margin-top: 16px;
    color: #64748b;
    font-size: 14px;
}

.bar-list,
.chip-list,
.market-list {
    display: grid;
    gap: 10px;
    margin-top: 14px;
}

.bar-name {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.bar-track {
    height: 8px;
    overflow: hidden;
    border-radius: 999px;
    background: #e5e7eb;
}

.bar-fill {
    display: block;
    height: 100%;
    border-radius: inherit;
    background: #3b82f6;
}

.chip {
    display: flex !important;
    justify-content: space-between;
    width: 100%;
    height: auto;
    margin-left: 0 !important;
    padding: 9px 11px !important;
    border: 1px solid #e5e7eb;
    border-radius: 999px;
    background: #fff;
    color: #1f2937;
    cursor: pointer;
}

.chip :deep(span) {
    display: flex;
    justify-content: space-between;
    width: 100%;
}

.market-item {
    display: flex !important;
    justify-content: space-between;
    width: 100%;
    height: auto;
    margin-left: 0 !important;
    padding: 12px !important;
    border: 1px solid #e5e7eb;
    border-radius: 14px;
    background: #fff;
    color: #1f2937;
    text-align: left;
    cursor: pointer;
}

.market-item :deep(span) {
    display: flex;
    justify-content: space-between;
    width: 100%;
}

.market-item small {
    display: block;
    margin-top: 3px;
    color: #94a3b8;
}

.market-item em {
    font-style: normal;
    font-weight: 800;
}

.matrix-el-table,
.index-el-table {
    font-size: 13px;
}

.matrix-el-table :deep(.el-table__header th),
.index-el-table :deep(.el-table__header th) {
    background: #f8fafc;
    color: #334155;
    font-weight: 700;
}

.matrix-el-table :deep(.el-table__cell),
.index-el-table :deep(.el-table__cell) {
    padding: 8px 0;
}

.market-name {
    display: block;
    margin-top: 3px;
    color: #94a3b8;
    font-size: 12px;
    font-weight: 400;
}

.link-button {
    height: auto;
    padding: 0 !important;
    color: #2563eb;
    font-weight: 700;
}

.count-pill {
    min-width: 36px;
    height: auto;
    padding: 4px 10px !important;
    border: 0;
    border-radius: 999px;
    background: #eef2ff !important;
    color: #3730a3 !important;
    font-weight: 700;
}

.empty-cell {
    color: #cbd5e1;
}

.code {
    font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
    color: #2563eb;
    font-weight: 700;
}

.tag {
    padding: 3px 8px;
    border: 0;
    border-radius: 999px;
    background: #f1f5f9;
    color: #334155;
}

/* 响应式保留原页面结构，同时避免 Element 组件挤压 */
@media (max-width: 1200px) {
    .layout {
        grid-template-columns: 1fr;
    }

    .side-panel {
        grid-template-columns: repeat(3, minmax(0, 1fr));
    }
}

@media (max-width: 900px) {
    .stats-grid {
        grid-template-columns: repeat(2, minmax(0, 1fr));
    }

    .filters {
        grid-template-columns: 1fr 1fr;
    }

    .side-panel {
        grid-template-columns: 1fr;
    }
}

@media (max-width: 560px) {
    .index-page {
        padding: 16px;
    }

    .stats-grid,
    .filters {
        grid-template-columns: 1fr;
    }
}
</style>