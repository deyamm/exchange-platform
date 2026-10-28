<template>
    <div class="target-detail-center-page">
        <div v-loading="detailLoading" class="page-content">
            <template v-if="shell">
                <TargetHeaderCard
                    :target="shell.targetProfile"
                    :quote="shell.quote"
                    :overview="overview?.overview"
                    @refresh="handleSearch"
                    @watch="handleWatch"
                    @create-alert="handleCreateAlert"
                />

                <TargetMetricCards :cards="shell.metricCards || []" />

                <TargetOverviewPanel :overview="overview?.overview || {}" />

                <div class="content-grid">
                    <!-- 多个tab：各 Tab 使用自己的接口按需加载 -->
                    <div class="main-content">
                        <TargetMainTabs
                            :target="shell.targetProfile"
                            :quote="shell.quote"
                            :trade-date="queryForm.tradeDate"
                        />
                    </div>

                    <aside class="side-content">
                        <TargetSidePanel
                            :watch="shell.watch || {}"
                            :alerts="shell.alerts || []"
                            :events="shell.events || []"
                            :related-targets="shell.relatedTargets || []"
                            @create-alert="handleCreateAlert"
                            @select-target="handleSelectTarget"
                        />
                    </aside>
                </div>

                <div class="meta-line">
                    数据来源：{{ shell.meta.dataSource }}
                    <span v-if="shell.meta.dataVersion"> · 版本：{{ shell.meta.dataVersion }}</span>
                    <span> · 生成时间：{{ formatDateTime(shell.meta.generatedAt) }}</span>
                    <span v-if="shell.meta.partialData"> · 部分数据</span>
                </div>
            </template>

            <el-empty v-else description="请输入标的编码查询详情" :image-size="120" />
        </div>
    </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { queryTargetDetailShell, queryTargetOverviewDetail } from '/@/api/targetdetail/targetDetailApi'
import type { TargetDetailShell, TargetOverviewDetail } from '/@/types/targetdetail/targetDetailCenter'
import type { TargetDetailQuery } from '/@/types/targetdetail/constants'
import TargetHeaderCard from './components/TargetHeaderCard.vue'
import TargetMetricCards from './components/TargetMetricCards.vue'
import TargetOverviewPanel from './components/TargetOverviewPanel.vue'
import TargetMainTabs from './components/TargetMainTabs.vue'
import TargetSidePanel from './components/TargetSidePanel.vue'
import { formatDateTime } from '/@/utils/marketUtils'


const queryForm = reactive<TargetDetailQuery>({
    targetCode: '000001.SZ',
    targetType: 'STOCK',
    tradeDate: '',
    marketCode: 'CN_A',
    includeCharts: true,
    includeFinancial: true,
    includeNews: false,
    includeHistory: false
})

const shell = ref<TargetDetailShell | null>(null)
const overview = ref<TargetOverviewDetail | null>(null)
const detailLoading = ref(false)
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
        const fullSymbol = queryForm.targetCode.trim()
        const tradeDate = queryForm.tradeDate || undefined

        // 首屏与综合分析拆为两个接口；概览失败时不影响首屏展示。
        const [shellResult, overviewResult] = await Promise.allSettled([
            queryTargetDetailShell({ fullSymbol, tradeDate }),
            queryTargetOverviewDetail({ fullSymbol, tradeDate })
        ])

        if (shellResult.status === 'rejected') {
            throw shellResult.reason
        }

        shell.value = shellResult.value

        if (overviewResult.status === 'fulfilled') {
            overview.value = overviewResult.value
        } else {
            overview.value = null
            console.warn('查询标的综合概览失败', overviewResult.reason)
        }
    } finally {
        detailLoading.value = false
    }
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
