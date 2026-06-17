<template>
    <div class="financial-page">
        <!-- 查询条件卡片 -->
        <el-card class="search-card" shadow="never">
            <el-form :model="queryForm" inline class="query-form">
                <el-form-item label="标的编码">
                    <el-input
                        v-model="queryForm.targetCode"
                        clearable
                        placeholder="如 000001.SZ"
                        style="width: 200px"
                        @keyup.enter="handleSearch"
                    />
                </el-form-item>
                <el-form-item label="场景编码">
                    <el-input
                        v-model="queryForm.sceneCode"
                        clearable
                        placeholder="如 FINANCIAL"
                        style="width: 200px"
                        @keyup.enter="handleSearch"
                    />
                </el-form-item>
                <el-form-item class="query-actions">
                    <el-button type="primary" @click="handleSearch">查询</el-button>
                    <el-button @click="handleReset">重置</el-button>
                </el-form-item>
            </el-form>
        </el-card>

        <!-- 财务摘要卡片 -->
        <el-card class="summary-card" shadow="never">
            <template #header>
                <div class="card-header">
                    <span class="card-title">财务摘要</span>
                    <p class="card-tip" v-if="summary.sourceDataTime">
                        数据时间：{{ formatTime(summary.sourceDataTime) }}
                    </p>
                </div>
            </template>

            <div v-loading="summaryLoading" class="summary-grid">
                <template v-if="summaryEntries.length">
                    <div v-for="item in summaryEntries" :key="item.key" class="summary-item">
                        <span class="summary-label">{{ item.key }}</span>
                        <span class="summary-value">{{ item.value }}</span>
                    </div>
                </template>
                <el-empty v-else description="请输入标的编码查询财务摘要" :image-size="80" />
            </div>
        </el-card>

        <!-- 财务趋势列表卡片 -->
        <el-card class="list-card" shadow="never">
            <template #header>
                <div class="card-header">
                    <span class="card-title">财务趋势与同行对比</span>
                    <el-input
                        v-model="trendMetricCode"
                        clearable
                        placeholder="按指标编码过滤"
                        style="width: 200px"
                        @keyup.enter="loadTrends"
                    />
                </div>
            </template>

            <div class="content-panel">
                <el-table
                    v-loading="trendLoading"
                    :data="trendData"
                    border
                    row-key="resultId"
                    class="trend-table"
                    height="calc(100vh - 540px)"
                >
                    <el-table-column prop="resultId" label="结果编号" min-width="170" align="center" />
                    <el-table-column prop="metricCode" label="财务指标" min-width="140" align="center" />
                    <el-table-column label="趋势摘要" min-width="280" align="center">
                        <template #default="{ row }">
                            <span class="summary-cell">{{ stringifySummary(row.resultSummary) }}</span>
                        </template>
                    </el-table-column>
                    <el-table-column label="数据时间" width="180" align="center">
                        <template #default="{ row }">{{ formatTime(row.sourceDataTime) }}</template>
                    </el-table-column>
                    <el-table-column label="操作" width="100" fixed="right" align="center">
                        <template #default="{ row }">
                            <el-button link type="primary" @click="openTrendDetail(row)">查看</el-button>
                        </template>
                    </el-table-column>
                </el-table>

                <div class="pagination-wrap">
                    <el-pagination
                        v-model:current-page="pagination.pageNo"
                        v-model:page-size="pagination.pageSize"
                        :total="pagination.total"
                        :page-sizes="[10, 20, 50, 100]"
                        layout="total, sizes, prev, pager, next, jumper"
                        @size-change="loadTrends"
                        @current-change="loadTrends"
                    />
                </div>
            </div>
        </el-card>

        <!-- 财务趋势详情弹窗 -->
        <FinancialTrendDialog v-model="dialogVisible" :result="currentResult" />
    </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { queryFinancialSummary, queryFinancialTrends } from '/@/api/financial/financialApi'
import { toSummaryEntries, stringifySummary } from '/@/utils/utils'
import type { AppResult } from '/@/types/marketoverview/entity'
import FinancialTrendDialog from './FinancialTrendDialog.vue'

interface QueryForm {
    targetCode: string
    sceneCode: string
}

interface PaginationState {
    pageNo: number
    pageSize: number
    total: number
}

const queryForm = reactive<QueryForm>({ targetCode: '', sceneCode: '' })
const summary = ref<Partial<AppResult>>({})
const summaryLoading = ref(false)

const trendData = ref<AppResult[]>([])
const trendLoading = ref(false)
const trendMetricCode = ref('')

const dialogVisible = ref(false)
const currentResult = ref<AppResult | null>(null)

const pagination = reactive<PaginationState>({ pageNo: 1, pageSize: 20, total: 0 })

const summaryEntries = computed(() => toSummaryEntries(summary.value.resultSummary))

function openTrendDetail(row: AppResult): void {
    currentResult.value = row
    dialogVisible.value = true
}

function formatTime(time?: string | null): string {
    return time ? time.replace('T', ' ') : '-'
}

async function loadSummary(): Promise<void> {
    summaryLoading.value = true
    try {
        summary.value = (await queryFinancialSummary({
            targetCode: queryForm.targetCode.trim(),
            sceneCode: queryForm.sceneCode || undefined
        })) || {}
    } finally {
        summaryLoading.value = false
    }
}

async function loadTrends(): Promise<void> {
    if (!queryForm.targetCode.trim()) {
        return
    }
    trendLoading.value = true
    try {
        const result = await queryFinancialTrends({
            targetCode: queryForm.targetCode.trim(),
            metricCode: trendMetricCode.value || undefined,
            pageNo: pagination.pageNo,
            pageSize: pagination.pageSize
        })
        trendData.value = result.records || []
        pagination.total = result.total || 0
    } finally {
        trendLoading.value = false
    }
}

function handleSearch(): void {
    if (!queryForm.targetCode.trim()) {
        ElMessage.warning('请输入标的编码')
        return
    }
    pagination.pageNo = 1
    loadSummary()
    loadTrends()
}

function handleReset(): void {
    queryForm.targetCode = ''
    queryForm.sceneCode = ''
    trendMetricCode.value = ''
    summary.value = {}
    trendData.value = []
    pagination.total = 0
}
</script>

<style scoped>
.financial-page {
    display: flex;
    flex-direction: column;
    gap: 16px;
    height: calc(100vh - 90px);
    min-height: 620px;
    padding: 8px;
    background: #f5f7fa;
    box-sizing: border-box;
}

.search-card,
.summary-card {
    flex: 0 0 auto;
    border-radius: 10px;
    overflow: hidden;
}

.list-card {
    flex: 1;
    min-height: 0;
    border-radius: 10px;
    overflow: hidden;
}

.card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
}

.card-title {
    font-size: 16px;
    font-weight: 600;
    color: #303133;
}

.card-tip {
    margin: 0;
    font-size: 12px;
    color: #909399;
}

.query-form {
    display: flex;
    flex-wrap: wrap;
}

.query-actions {
    margin-left: auto;
    margin-right: 0;
}

.summary-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
    gap: 12px;
    min-height: 80px;
}

.summary-item {
    display: flex;
    flex-direction: column;
    gap: 6px;
    padding: 12px 14px;
    background: #fafafa;
    border: 1px solid #ebeef5;
    border-radius: 8px;
}

.summary-label {
    font-size: 12px;
    color: #909399;
}

.summary-value {
    font-size: 16px;
    font-weight: 600;
    color: #303133;
    word-break: break-all;
}

.summary-cell {
    display: inline-block;
    max-width: 100%;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.content-panel {
    display: flex;
    flex-direction: column;
    height: 100%;
    min-height: 0;
}

.trend-table {
    flex: 1;
    min-height: 280px;
}

.pagination-wrap {
    display: flex;
    justify-content: center;
    padding-top: 16px;
}

:deep(.el-card__header) {
    padding: 14px 16px;
    border-bottom: 1px solid #ebeef5;
}

:deep(.el-table th.el-table__cell) {
    background: #f7f8fa;
    color: #606266;
    font-weight: 600;
}

@media (max-width: 1200px) {
    .financial-page {
        height: auto;
        min-height: auto;
    }

    .list-card {
        min-height: 520px;
    }

    .content-panel {
        height: auto;
    }

    .trend-table {
        height: 420px;
    }
}
</style>
