<template>
    <el-card class="main-tabs-card" shadow="never">
        <el-tabs v-model="activeTab" class="main-tabs">
            <!-- 行情详情：K线、区间表现、复权因子、行情明细。 -->
            <el-tab-pane label="行情详情" name="quote" v-if="detail.quote !== null">
                <QuoteDetailPage
                    :quote="detail.quote"
                    :charts="detail.charts || []"
                    :performance="detail.performance || []"
                    :quote-rows="detail.quoteRows || []"
                />
            </el-tab-pane>

            <!-- 估值详情：PE/PB/PS/股息率、市值股本、换手明细。 -->
            <el-tab-pane label="估值详情" name="valuation" v-if="detail.valuation !== null">
                <ValuationDetailPage
                    :valuation="detail.valuation"
                    :valuation-rows="detail.valuationRows || []"
                    :valuation-detail="detail.valuationDetail"
                />
            </el-tab-pane>

            <!-- 财务详情：利润表、资产负债表、现金流量表、财务指标四表。 -->
            <el-tab-pane label="财务详情" name="financial" v-if="detail.financial !== null">
                <FinancialDetailPage
                    :financial="detail.financial || undefined"
                    :financial-detail="detail.financialDetail"
                />
            </el-tab-pane>

            <!-- 资金流向详情：仅使用 stock_moneyflow_daily 已定义字段，不展示未落库的北向/两融数据。 -->
            <el-tab-pane label="资金流向详情" name="capitalFlow" v-if="detail.capitalFlow !== null">
                <CapitalFlowDetailPage
                    :capital-flow="detail.capitalFlow || undefined"
                    :money-flow-rows="detail.moneyFlowRows || []"
                    :quote-rows="detail.quoteRows || []"
                />
            </el-tab-pane>

            <!-- 成分/行业详情：指数展示成分，股票展示行业与同业对比。 -->
            <el-tab-pane label="行业详情" name="industry">
                <ConstituentIndustryDetailPage
                    :target="detail.target"
                    :industry="detail.industry || undefined"
                    :constituents="detail.constituents || undefined"
                    :index-profile="detail.indexProfile"
                    :stock-profile="detail.stockProfile"
                    :industry-detail="detail.industryDetail"
                    :constituent-rows="detail.constituentRows || []"
                    :industry-peer-rows="detail.industryPeerRows || []"
                />
            </el-tab-pane>
        </el-tabs>
    </el-card>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { TargetDetail } from '/@/types/targetdetail/targetDetailType'
import QuoteDetailPage from './details/QuoteDetailPage.vue'
import ValuationDetailPage from './details/ValuationDetailPage.vue'
import FinancialDetailPage from './details/FinancialDetailPage.vue'
import CapitalFlowDetailPage from './details/CapitalFlowDetailPage.vue'
import ConstituentIndustryDetailPage from './details/ConstituentIndustryDetailPage.vue'

defineProps<{
    detail: TargetDetail
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
