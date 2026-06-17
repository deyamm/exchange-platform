<template>
    <div class="target-detail-center-page">
        <div v-loading="detailLoading" class="page-content">
            <template v-if="detail">
                <TargetHeaderCard
                    :target="detail.target"
                    :quote="detail.quote"
                    :overview="detail.overview"
                    @refresh="handleSearch"
                    @watch="handleWatch"
                    @create-alert="handleCreateAlert"
                />

                <TargetMetricCards :cards="detail.metricCards" />

                <TargetOverviewPanel :overview="detail.overview" />

                <div class="content-grid">
                    <!-- 多个tab -->
                    <div class="main-content">
                        <TargetMainTabs
                            :detail="detail"
                            :history-records="historyRecords"
                            :history-loading="historyLoading"
                            :history-total="historyTotal"
                            :history-page-no="historyQuery.pageNo"
                            :history-page-size="historyQuery.pageSize"
                            @history-query-change="handleHistoryQueryChange"
                            @history-page-change="handleHistoryPageChange"
                        />
                    </div>

                    <aside class="side-content">
                        <TargetSidePanel
                            :watch="detail.watch"
                            :alerts="detail.alerts"
                            :events="detail.events"
                            :related-targets="detail.relatedTargets"
                            @create-alert="handleCreateAlert"
                            @select-target="handleSelectTarget"
                        />
                    </aside>
                </div>

                <div class="meta-line">
                    数据来源：{{ detail.meta.dataSource }}
                    <span v-if="detail.meta.dataVersion"> · 版本：{{ detail.meta.dataVersion }}</span>
                    <span> · 生成时间：{{ formatDateTime(detail.meta.generatedAt) }}</span>
                    <span v-if="detail.meta.partialData"> · 部分数据</span>
                </div>
            </template>

            <el-empty v-else description="请输入标的编码查询详情" :image-size="120" />
        </div>
    </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
    queryTargetAnalysisHistory,
    queryTargetDetailCenter
} from '/@/api/targetdetail/targetDetailCenterApi'
import type {
    AnalysisHistoryQuery,
    AnalysisRecord,
    TargetDetailDTO,
    TargetDetailQuery,
    TargetType
} from '/@/types/targetdetail/targetDetailCenter'
import TargetHeaderCard from './components/TargetHeaderCard.vue'
import TargetMetricCards from './components/TargetMetricCards.vue'
import TargetOverviewPanel from './components/TargetOverviewPanel.vue'
import TargetMainTabs from './components/TargetMainTabs.vue'
import TargetSidePanel from './components/TargetSidePanel.vue'
import { formatDateTime } from './utils/targetDetailFormat'

const queryForm = reactive<TargetDetailQuery>({
    targetCode: '000001.SZ',
    targetType: '',
    marketCode: 'CN_A',
    includeCharts: true,
    includeFinancial: true,
    includeNews: true,
    includeHistory: true
})

const detail = ref<TargetDetailDTO | null>(null)
const detailLoading = ref(false)

const historyRecords = ref<AnalysisRecord[]>([])
const historyLoading = ref(false)
const historyTotal = ref(0)

const historyQuery = reactive<AnalysisHistoryQuery>({
    targetCode: queryForm.targetCode,
    sceneCode: '',
    conclusion: '',
    riskLevel: '',
    pageNo: 1,
    pageSize: 20
})

onMounted(() => {
    handleSearch()
})

async function handleSearch(): Promise<void> {
    if (!queryForm.targetCode.trim()) {
        ElMessage.warning('请输入标的编码')
        return
    }

    detailLoading.value = true

    try {
        detail.value = await queryTargetDetailCenter({
            ...queryForm,
            targetCode: queryForm.targetCode.trim(),
            targetType: (queryForm.targetType || undefined) as TargetType | undefined
        })

        historyQuery.targetCode = queryForm.targetCode.trim()
        historyQuery.pageNo = 1
        await loadHistory()
    } finally {
        detailLoading.value = false
    }
}

function handleReset(): void {
    queryForm.targetCode = ''
    queryForm.targetType = ''
    queryForm.marketCode = ''
    detail.value = null
    historyRecords.value = []
    historyTotal.value = 0
}

async function loadHistory(): Promise<void> {
    if (!historyQuery.targetCode) return

    historyLoading.value = true
    try {
        const result = await queryTargetAnalysisHistory({ ...historyQuery })
        historyRecords.value = result.records
        historyTotal.value = result.total
    } finally {
        historyLoading.value = false
    }
}

async function handleHistoryQueryChange(query: Partial<AnalysisHistoryQuery>): Promise<void> {
    historyQuery.sceneCode = query.sceneCode || ''
    historyQuery.conclusion = query.conclusion || ''
    historyQuery.riskLevel = query.riskLevel || ''
    historyQuery.pageNo = query.pageNo || 1
    historyQuery.pageSize = query.pageSize || historyQuery.pageSize
    await loadHistory()
}

async function handleHistoryPageChange(pagination: { pageNo: number; pageSize: number }): Promise<void> {
    historyQuery.pageNo = pagination.pageNo
    historyQuery.pageSize = pagination.pageSize
    await loadHistory()
}

function handleSelectTarget(targetCode: string): void {
    queryForm.targetCode = targetCode
    handleSearch()
}

function handleWatch(): void {
    ElMessage.info('这里可以打开关注设置弹窗')
}

function handleCreateAlert(): void {
    ElMessage.info('这里可以打开提醒规则弹窗')
}
</script>

<style scoped>
.target-detail-center-page {
    min-height: calc(100vh - 90px);
    padding: 8px;
    background: #f5f7fa;
    box-sizing: border-box;
}


.page-content {
    display: flex;
    flex-direction: column;
    gap: 14px;
    min-height: 480px;
}

.content-grid {
    display: grid;
    grid-template-columns: minmax(0, 1fr) 320px;
    gap: 14px;
    align-items: flex-start;
}

.main-content {
    min-width: 0;
}

.side-content {
    min-width: 0;
}

.meta-line {
    padding: 0 4px 12px;
    font-size: 12px;
    color: #909399;
}

:deep(.el-card__header) {
    padding: 14px 16px;
}

:deep(.el-card__body) {
    padding: 16px;
}

:deep(.el-table th.el-table__cell) {
    background: #f7f8fa;
    color: #606266;
    font-weight: 600;
}

@media (max-width: 1280px) {
    .content-grid {
        grid-template-columns: 1fr;
    }

    .side-content {
        order: -1;
    }
}

@media (max-width: 768px) {
    .query-actions {
        margin-left: 0;
    }
}
</style>
