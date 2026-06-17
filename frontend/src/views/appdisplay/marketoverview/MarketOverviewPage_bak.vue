<template>
    <div class="market-overview-page">
        <!-- 市场概览卡片 -->
        <el-card class="overview-card" shadow="never">
            <template #header>
                <div class="card-header">
                    <div>
                        <span class="card-title">市场概览</span>
                        <p class="card-tip" v-if="overview.sourceDataTime">
                            数据时间：{{ formatTime(overview.sourceDataTime) }}
                        </p>
                    </div>
                    <el-button :loading="overviewLoading" @click="loadOverview">刷新</el-button>
                </div>
            </template>

            <div v-if="summaryEntries.length" v-loading="overviewLoading" class="summary-grid">
                <template>
                    <div v-for="item in summaryEntries" :key="item.key" class="summary-item">
                        <span class="summary-label">{{ item.label }}</span>
                        <span class="summary-value">{{ item.value }}</span>
                    </div>
                </template>
            </div>
            <el-empty v-else description="暂无市场概览数据" :image-size="80" />
        </el-card>

        <!-- 自选观察概览列表卡片 -->
        <el-card class="list-card" shadow="never">
            <template #header>
                <div class="card-header">
                    <span class="card-title">自选观察概览</span>
                </div>
            </template>

            <div class="content-panel">
                <el-form :model="queryForm" inline class="query-form">
                    <el-form-item label="标的编码">
                        <el-input
                            v-model="queryForm.targetCode"
                            clearable
                            placeholder="标的编码"
                            style="width: 180px"
                            @keyup.enter="handleSearch"
                        />
                    </el-form-item>
                    <el-form-item label="标的名称">
                        <el-input
                            v-model="queryForm.targetName"
                            clearable
                            placeholder="标的编码或名称"
                            style="width: 180px"
                            @keyup.enter="handleSearch"
                        />
                    </el-form-item>

                    <el-form-item label="分组编码">
                        <el-input
                            v-model="queryForm.groupCode"
                            clearable
                            placeholder="如 BANK_GROUP"
                            style="width: 180px"
                            @keyup.enter="handleSearch"
                        />
                    </el-form-item>

                    <el-form-item label="观察状态">
                        <el-select v-model="queryForm.watchStatus" clearable placeholder="全部" style="width: 150px">
                            <el-option
                                v-for="opt in WATCH_STATUS_OPTIONS"
                                :key="opt.value"
                                :label="opt.label"
                                :value="opt.value"
                            />
                        </el-select>
                    </el-form-item>

                    <el-form-item class="query-actions">
                        <el-button type="primary" @click="handleSearch">查询</el-button>
                        <el-button @click="handleReset">重置</el-button>
                    </el-form-item>
                </el-form>

                <el-table
                    v-loading="tableLoading"
                    :data="tableData"
                    border
                    row-key="targetCode"
                    class="overview-table"
                    height="calc(100vh - 430px)"
                >
                    <el-table-column prop="targetCode" label="标的编码" min-width="140" align="center" />
                    <el-table-column prop="targetName" label="标的名称" min-width="140" align="center" />
                    <el-table-column prop="targetType" label="类型" width="100" align="center" />
                    <el-table-column prop="marketCode" label="市场" width="90" align="center" />
                    <el-table-column prop="groupCode" label="分组" min-width="140" align="center" />
                    <el-table-column label="重点" width="80" align="center">
                        <template #default="{ row }">
                            <el-tag v-if="row.importantFlag" type="warning" effect="plain">重点</el-tag>
                            <span v-else>-</span>
                        </template>
                    </el-table-column>
                    <el-table-column label="观察状态" width="120" align="center">
                        <template #default="{ row }">
                            <el-tag :type="WATCH_STATUS_TAG_TYPE[row.watchStatus as WatchStatus] || 'info'">
                                {{ WATCH_STATUS_TEXT[row.watchStatus as WatchStatus] || row.watchStatus }}
                            </el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column prop="watchReason" label="关注理由" min-width="200" show-overflow-tooltip align="center" />
                    <el-table-column label="操作" width="120" fixed="right" align="center">
                        <template #default="{ row }">
                            <el-button link type="primary" @click="openDetail(row)">查看</el-button>
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
                        @size-change="loadTable"
                        @current-change="loadTable"
                    />
                </div>
            </div>
        </el-card>

        <!-- 标的概览详情弹窗 -->
        <WatchTargetDetailDialog v-model="dialogVisible" :target="currentTarget" />
    </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { queryMarketOverview, queryWatchOverview } from '/@/api/marketoverview/marketOverviewApi'
import { WATCH_STATUS_OPTIONS, WATCH_STATUS_TEXT, WATCH_STATUS_TAG_TYPE } from '/@/types/watchtarget/view'
import { AppResult, createDefaultAppResult } from '/@/types/marketoverview/entity'
import type { WatchTarget } from '/@/types/watchtarget/entity'
import type { WatchStatus } from '/@/types/watchtarget/constants'
import WatchTargetDetailDialog from './WatchTargetDetailDialog.vue'

interface QueryForm {
    targetCode: string
    targetName: string
    groupCode: string
    watchStatus: WatchStatus | ''
}

interface PaginationState {
    pageNo: number
    pageSize: number
    total: number
}

const overview = ref<AppResult>(createDefaultAppResult())
const overviewLoading = ref(false)
const tableData = ref<WatchTarget[]>([])
const tableLoading = ref(false)

const dialogVisible = ref(false)
const currentTarget = ref<WatchTarget | null>(null)

const queryForm = reactive<QueryForm>({
    targetCode: '',
    targetName: '',
    groupCode: '',
    watchStatus: ''
})

const pagination = reactive<PaginationState>({
    pageNo: 1,
    pageSize: 20,
    total: 0
})

// 将结果摘要 JSON 拆解为可展示的键值对
const summaryEntries = computed(() => {
    const summary = overview.value.resultSummary
    if (!summary || typeof summary !== 'object') {
        return [] as { key: string; label: string; value: string }[]
    }
    return Object.entries(summary).map(([key, value]) => ({
        key,
        label: key,
        value: typeof value === 'object' ? JSON.stringify(value) : String(value)
    }))
})

function formatTime(time?: string | null): string {
    return time ? time.replace('T', ' ') : '-'
}

async function loadOverview(): Promise<void> {
    overviewLoading.value = true
    try {
        overview.value = (await queryMarketOverview({})) || createDefaultAppResult()
    } finally {
        overviewLoading.value = false
    }
}

async function loadTable(): Promise<void> {
    tableLoading.value = true
    try {
        const result = await queryWatchOverview({
            pageNo: pagination.pageNo,
            pageSize: pagination.pageSize,
            targetCode: queryForm.targetCode || undefined,
            targetName: queryForm.targetName || undefined,
            groupCode: queryForm.groupCode || undefined,
            watchStatus: queryForm.watchStatus || undefined
        })
        tableData.value = result.records || []
        pagination.total = result.total || 0
    } finally {
        tableLoading.value = false
    }
}

function handleSearch(): void {
    pagination.pageNo = 1
    loadTable()
}

function handleReset(): void {
    queryForm.targetCode = ''
    queryForm.targetName = ''
    queryForm.groupCode = ''
    queryForm.watchStatus = ''
    pagination.pageNo = 1
    loadTable()
}

function openDetail(row: WatchTarget): void {
    currentTarget.value = row
    dialogVisible.value = true
}

onMounted(() => {
    loadOverview()
    loadTable()
})
</script>

<style scoped>
.market-overview-page {
    display: flex;
    flex-direction: column;
    gap: 16px;
    height: calc(100vh - 90px);
    min-height: 620px;
    padding: 8px;
    background: #f5f7fa;
    box-sizing: border-box;
}

.overview-card {
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
    margin: 6px 0 0;
    font-size: 12px;
    line-height: 18px;
    color: #909399;
}

.summary-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
    gap: 12px;
    min-height: 80px;
}

.summary-item {
    display: flex;
    flex-direction: column;
    gap: 6px;
    padding: 14px 16px;
    background: #fafafa;
    border: 1px solid #ebeef5;
    border-radius: 8px;
}

.summary-label {
    font-size: 12px;
    color: #909399;
}

.summary-value {
    font-size: 18px;
    font-weight: 600;
    color: #303133;
    word-break: break-all;
}

.content-panel {
    display: flex;
    flex-direction: column;
    height: 100%;
    min-height: 0;
}

.query-form {
    display: flex;
    flex-wrap: wrap;
    padding: 16px 16px 4px;
    margin-bottom: 12px;
    background: #fafafa;
    border: 1px solid #ebeef5;
    border-radius: 8px;
}

.query-actions {
    margin-left: auto;
    margin-right: 0;
}

.overview-table {
    flex: 1;
    min-height: 320px;
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
    .market-overview-page {
        height: auto;
        min-height: auto;
    }

    .list-card {
        min-height: 620px;
    }

    .content-panel {
        height: auto;
    }

    .overview-table {
        height: 420px;
    }
}
</style>
