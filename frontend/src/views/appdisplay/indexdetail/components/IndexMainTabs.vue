<template>
    <el-card class="main-tabs-card" shadow="never">
        <el-tabs v-model="activeTab" class="main-tabs">
            <el-tab-pane label="行情走势" name="quote">
                <QuoteChartTab
                    :quote="detail.quote"
                    :charts="market.marketCharts"
                    :analysis="market.quoteAnalysis"
                    :range-performance="market.rangePerformance"
                />
            </el-tab-pane>

            <el-tab-pane label="估值与规模" name="valuation">
                <ValuationTab :indicator="detail.indicator" />
            </el-tab-pane>

            <el-tab-pane label="成分股" name="constituents">
                <ConstituentsTab
                    :constituents="constituents.constituents"
                    :profile="detail.profile"
                />
            </el-tab-pane>

            <el-tab-pane label="指数档案" name="basic">
                <IndexBasicInfoTab :profile="detail.profile" />
            </el-tab-pane>
        </el-tabs>
    </el-card>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { IndexConstituents, IndexDetail, IndexMarket } from '/@/types/indexdetail/indexDetailCenter'
import QuoteChartTab from './tabs/QuoteChartTab.vue'
import ValuationTab from './tabs/ValuationTab.vue'
import ConstituentsTab from './tabs/ConstituentsTab.vue'
import IndexBasicInfoTab from './tabs/IndexBasicInfoTab.vue'

defineProps<{
    detail: IndexDetail
    market: IndexMarket
    constituents: IndexConstituents
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
