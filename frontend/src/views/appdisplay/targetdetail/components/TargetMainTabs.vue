<template>
    <el-card class="main-tabs-card" shadow="never">
        <el-tabs v-model="activeTab" class="main-tabs">
            <el-tab-pane label="行情走势" name="quote" v-if="detail.features?.quote !== false">
                <QuoteChartTab :quote="detail.quote" :charts="detail.charts" :performance="detail.performance || []" />
            </el-tab-pane>

            <el-tab-pane label="估值分析" name="valuation" v-if="detail.features?.valuation !== false">
                <ValuationTab :valuation="detail.valuation" />
            </el-tab-pane>

            <el-tab-pane label="财务摘要" name="financial" v-if="detail.features?.financial">
                <FinancialTab :financial="detail.financial" />
            </el-tab-pane>

            <el-tab-pane label="资金流向" name="capitalFlow" v-if="detail.features?.capitalFlow !== false">
                <CapitalFlowTab :capital-flow="detail.capitalFlow" />
            </el-tab-pane>

            <el-tab-pane label="成分/行业" name="industry">
                <IndustryConstituentTab
                    :target="detail.target"
                    :industry="detail.industry"
                    :constituents="detail.constituents"
                    :fund-info="detail.fundInfo"
                    :related-targets="detail.relatedTargets"
                />
            </el-tab-pane>

            <el-tab-pane label="新闻公告" name="news" v-if="detail.features?.news !== false">
                <NewsEventTab :events="detail.events" :news="detail.news" />
            </el-tab-pane>

            <el-tab-pane label="历史分析" name="history" v-if="detail.features?.analysisHistory !== false">
                <AnalysisHistoryTab
                    :records="historyRecords"
                    :loading="historyLoading"
                    :total="historyTotal"
                    :page-no="historyPageNo"
                    :page-size="historyPageSize"
                    @query-change="$emit('history-query-change', $event)"
                    @page-change="$emit('history-page-change', $event)"
                />
            </el-tab-pane>
        </el-tabs>
    </el-card>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { AnalysisHistoryQuery, AnalysisRecord, TargetDetailDTO } from '/@/types/targetdetail/targetDetailCenter'
import QuoteChartTab from './tabs/QuoteChartTab.vue'
import ValuationTab from './tabs/ValuationTab.vue'
import FinancialTab from './tabs/FinancialTab.vue'
import CapitalFlowTab from './tabs/CapitalFlowTab.vue'
import IndustryConstituentTab from './tabs/IndustryConstituentTab.vue'
import NewsEventTab from './tabs/NewsEventTab.vue'
import AnalysisHistoryTab from './tabs/AnalysisHistoryTab.vue'


const emits = defineEmits<{
    (e: 'history-query-change', query: Partial<AnalysisHistoryQuery>): void
    (e: 'history-page-change', pagination: { pageNo: number; pageSize: number }): void
}>()

defineProps<{
    detail: TargetDetailDTO
    historyRecords: AnalysisRecord[]
    historyLoading: boolean
    historyTotal: number
    historyPageNo: number
    historyPageSize: number
}>()

const activeTab = ref('quote')
</script>

<style scoped>
.main-tabs-card {
    border-radius: 12px;
    min-height: 520px;
}

.main-tabs {
    min-height: 480px;
}

:deep(.el-tabs__header) {
    margin-bottom: 18px;
}
</style>
