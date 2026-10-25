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
                        <IndexMainTabs 
                            v-if="detail && market && constituents"
                            :detail="detail" 
                            :market="market" 
                            :constituents="constituents" 
                        />
                    </div>

                    <aside class="side-content">
                        <IndexSidePanel
                            v-if="detail && constituents"
                            :profile="detail.profile"
                            :constituents="constituents.constituents"
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

            <el-empty v-else description="请输入指数代码查询详情" :image-size="120" />
        </div>
    </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { queryIndexDetail, queryIndexMarket, queryIndexConstituents } from '/@/api/indexdetail/indexDetailApi'
import type { IndexDetail, IndexMarket, IndexConstituents } from '/@/types/indexdetail/indexDetailCenter'
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

const detail = ref<IndexDetail | null>(null)
const market = ref<IndexMarket | null>(null)
const constituents = ref<IndexConstituents | null>(null)
const detailLoading = ref(false)

onMounted(() => {
    handleSearch()
})

async function handleSearch(): Promise<void> {
    if (!queryForm.fullSymbol.trim()) {
        ElMessage.warning('请输入指数代码')
        return
    }

    detailLoading.value = true

    try {
        const [detailData, marketData, constituentsData] = await Promise.all([
            queryIndexDetail(
                queryForm.fullSymbol.trim(),
                queryForm.tradeDate
            ),
            queryIndexMarket(
                queryForm.fullSymbol.trim(),
                queryForm.tradeDate,
                250
            ),
            queryIndexConstituents(
                queryForm.fullSymbol.trim(),
                queryForm.tradeDate,
            ),
        ])

        detail.value = detailData
        market.value = marketData
        constituents.value = constituentsData
    } finally {
        detailLoading.value = false
        console.log(market.value)
    }
}

function handleWatch(): void {
    ElMessage.info('这里可以接入指数关注功能')
}

function handleCreateAlert(): void {
    ElMessage.info('这里可以接入指数提醒功能')
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
