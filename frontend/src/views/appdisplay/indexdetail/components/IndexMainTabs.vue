<template>
    <el-card class="main-tabs-card" shadow="never">
        <el-tabs v-model="activeTab" class="main-tabs">
            <!-- lazy：Tab 首次激活时才挂载，由子组件按自己的接口加载数据 -->
            <el-tab-pane label="行情走势" name="quote" lazy>
                <QuoteChartTab
                    :quote="detail.quote"
                    :trade-date="tradeDate"
                />
            </el-tab-pane>

            <el-tab-pane label="估值与规模" name="valuation" lazy>
                <ValuationTab :indicator="detail.indicator" />
            </el-tab-pane>

            <el-tab-pane label="成分股" name="constituents" lazy>
                <ConstituentsTab
                    :full-symbol="detail.profile.fullSymbol"
                    :trade-date="tradeDate"
                />
            </el-tab-pane>

            <el-tab-pane label="指数档案" name="basic" lazy>
                <IndexBasicInfoTab :profile="detail.profile" />
            </el-tab-pane>
        </el-tabs>
    </el-card>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { IndexDetail } from '/@/types/indexdetail/indexDetailCenter'
import QuoteChartTab from './tabs/QuoteChartTab.vue'
import ValuationTab from './tabs/ValuationTab.vue'
import ConstituentsTab from './tabs/ConstituentsTab.vue'
import IndexBasicInfoTab from './tabs/IndexBasicInfoTab.vue'

defineProps<{
    /** 指数首屏详情，提供 quote / indicator / profile 等基础数据 */
    detail: IndexDetail
    /** 首屏解析后的实际交易日，供各子接口统一使用 */
    tradeDate?: string
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
