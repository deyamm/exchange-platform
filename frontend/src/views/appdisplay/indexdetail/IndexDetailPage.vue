<template>
    <div class="index-detail-center-page">
        <div v-loading="detailLoading" class="page-content">
            <template v-if="detail">
                <IndexHeaderCard
                    :profile="detail.profile"
                    :quote="detail.quote"
                    @refresh="handleSearch"
                    @watch="handleWatch"
                    @create-alert="handleCreateAlert"
                />

                <IndexMetricCards :cards="detail.metricCards" />

                <IndexOverviewPanel :overview="detail.overview" />

                <div class="content-grid">
                    <div class="main-content">
                        <!-- 各 Tab 按自身功能域接口加载数据，首屏只负责指数基础信息 -->
                        <IndexMainTabs
                            v-if="detail"
                            :detail="detail"
                            :trade-date="resolvedTradeDate"
                        />
                    </div>

                    <aside class="side-content">
                        <!-- 侧栏自行加载前十大权重 -->
                        <IndexSidePanel
                            v-if="detail"
                            :profile="detail.profile"
                            :trade-date="resolvedTradeDate"
                        />
                    </aside>
                </div>

                <div class="meta-line">
                    数据来源：{{ detail.meta.dataSource }}
                    <span v-if="detail.meta.dataVersion"> · 版本：{{ detail.meta.dataVersion }}</span>
                    <span> · 生成时间：{{ formatDateTime(detail.meta.generatedAt) }}</span>
                    <span v-if="detail.meta.partialData"> · 部分数据</span>
                </div>
                <RecentAlertMessages target-type="INDEX" :target-code="detail.profile.fullSymbol" />
            </template>

            <el-empty v-else description="请输入指数代码查询详情" :image-size="120" />
        </div>

        <AddTargetFormDialog
            v-model="watchDialogVisible"
            title="添加指数自选"
            :target-code="activeWatchIndex?.fullSymbol || ''"
            :target-name="activeWatchIndex?.name"
            target-type="INDEX"
            @confirm="handleAddWatchConfirm"
        />
        <AlertRuleEditorDialog v-model:open="alertDialogVisible" target-type="INDEX"
            :target-code="detail?.profile.fullSymbol" :target-name="detail?.profile.name" @success="handleSearch" />
    </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute } from 'vue-router'
import { queryIndexDetail } from '/@/api/indexdetail/indexDetailApi'
import type { IndexDetail } from '/@/types/indexdetail/indexDetailCenter'
import type { IndexProfile } from '/@/types/marketoverview/view'
import AddTargetFormDialog from '/@/components/common/AddTargetFormDialog.vue'
import AlertRuleEditorDialog from '/@/components/common/AlertRuleEditorDialog.vue'
import RecentAlertMessages from '/@/components/common/RecentAlertMessages.vue'
import { saveOrUpdateWatchTarget } from '/@/api/watchtarget/watchTargetApi'
import type { WatchTargetSaveRequest } from '/@/types/watchtarget/constants'
import IndexHeaderCard from './components/IndexHeaderCard.vue'
import IndexMetricCards from './components/IndexMetricCards.vue'
import IndexOverviewPanel from './components/IndexOverviewPanel.vue'
import IndexMainTabs from './components/IndexMainTabs.vue'
import IndexSidePanel from './components/IndexSidePanel.vue'
import { formatDateTime } from '/@/utils/marketUtils'

const queryForm = reactive({
    fullSymbol: '000001.SH',
    tradeDate: '',
})

const route = useRoute()

const detail = ref<IndexDetail | null>(null)
const detailLoading = ref(false)
const watchDialogVisible = ref(false)
const alertDialogVisible = ref(false)
const activeWatchIndex = ref<IndexProfile | null>(null)

/** 首屏返回的行情日期即后端解析后的实际交易日，供各 Tab 复用。 */
const resolvedTradeDate = computed(
    () => queryForm.tradeDate || detail.value?.quote?.tradeDate || undefined
)

function routeFullSymbol(): string {
    const routeValue = route.params.fullSymbol ?? route.query.fullSymbol ?? route.query.targetCode
    const value = Array.isArray(routeValue) ? routeValue[0] : routeValue
    return typeof value === 'string' && value.trim() ? value.trim() : '000001.SH'
}

watch(
    () => [route.params.fullSymbol, route.query.fullSymbol, route.query.targetCode],
    () => {
        queryForm.fullSymbol = routeFullSymbol()
        void handleSearch()
    },
    { immediate: true },
)

async function handleSearch(): Promise<void> {
    if (!queryForm.fullSymbol.trim()) {
        ElMessage.warning('请输入指数代码')
        return
    }

    detailLoading.value = true

    try {
        // 首屏只拉取指数详情壳数据；行情、成分股由对应 Tab 自行加载。
        detail.value = await queryIndexDetail(
            queryForm.fullSymbol.trim(),
            queryForm.tradeDate
        )
    } finally {
        detailLoading.value = false
    }
}

function handleWatch(): void {
    if (!detail.value) return
    activeWatchIndex.value = detail.value.profile
    watchDialogVisible.value = true
}

async function handleAddWatchConfirm(payload: {
    targetCode: string
    targetType: string
    groupCodes: string[]
}): Promise<void> {
    const index = activeWatchIndex.value
    if (!index) return

    const request: WatchTargetSaveRequest = {
        targetCode: payload.targetCode,
        targetName: index.name,
        targetType: payload.targetType,
        marketCode: index.market || '',
        groupCodes: payload.groupCodes,
        sortNo: 1,
        importantFlag: false,
        watchStatus: 'WATCHING',
        watchReason: undefined
    }

    try {
        await saveOrUpdateWatchTarget(request)
        ElMessage.success(
            payload.groupCodes.length > 0
                ? `已添加指数 ${payload.targetCode} 到自选`
                : `已删除指数 ${payload.targetCode} 的自选`
        )
    } catch (error) {
        ElMessage.error('添加自选失败')
    }
}

function handleCreateAlert(): void {
    alertDialogVisible.value = true
}
</script>

<style scoped>
.index-detail-center-page {
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

.query-bar {
    padding: 12px 12px 0;
    background: #fff;
    border: 1px solid #ebeef5;
    border-radius: 12px;
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
</style>
