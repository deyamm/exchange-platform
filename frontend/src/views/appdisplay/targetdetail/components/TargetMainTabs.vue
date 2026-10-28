<template>
    <el-card class="main-tabs-card" shadow="never">
        <el-tabs v-model="activeTab" class="main-tabs">
            <!-- 行情详情：K线、区间表现、技术指标综合分析。 -->
            <el-tab-pane label="行情详情" name="quote" lazy>
                <QuoteDetailPage
                    v-if="quote"
                    :full-symbol="target.fullSymbol"
                    :quote="quote"
                />
                <el-empty v-else description="暂无行情数据" />
            </el-tab-pane>

            <!-- 估值详情：PE/PB/PS/股息率、估值通道、估值走势、分红走势。 -->
            <el-tab-pane label="估值详情" name="valuation" lazy>
                <ValuationDetailPage
                    :full-symbol="target.fullSymbol"
                    :trade-date="resolvedTradeDate"
                />
            </el-tab-pane>

            <!-- 财务详情：利润表、资产负债表、现金流量表、财务指标四表。 -->
            <el-tab-pane label="财务详情" name="financial" lazy>
                <FinancialDetailPage :full-symbol="target.fullSymbol" />
            </el-tab-pane>

            <!-- 资金流向详情：仅使用 stock_moneyflow_daily 已定义字段，不展示未落库的北向/两融数据。 -->
            <el-tab-pane label="资金流向详情" name="capitalFlow" lazy>
                <CapitalFlowDetailPage
                    :full-symbol="target.fullSymbol"
                    :trade-date="resolvedTradeDate"
                />
            </el-tab-pane>

            <!-- 成分/行业详情：指数展示成分，股票展示行业与同业对比。 -->
            <el-tab-pane label="行业详情" name="industry" lazy>
                <ConstituentIndustryDetailPage
                    :full-symbol="target.fullSymbol"
                    :trade-date="resolvedTradeDate"
                />
            </el-tab-pane>
        </el-tabs>
    </el-card>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import type { TargetProfile } from '/@/types/targetdetail/targetDetailCenter'
import type { MarketQuote } from '/@/types/marketoverview/view'
import QuoteDetailPage from './details/QuoteDetailPage.vue'
import ValuationDetailPage from './details/ValuationDetailPage.vue'
import FinancialDetailPage from './details/FinancialDetailPage.vue'
import CapitalFlowDetailPage from './details/CapitalFlowDetailPage.vue'
import ConstituentIndustryDetailPage from './details/ConstituentIndustryDetailPage.vue'

const props = withDefaults(defineProps<{
    /** 标的基础信息，提供 fullSymbol 供各 Tab 独立请求。 */
    target: TargetProfile
    /** 首屏行情快照，用于行情 Tab 初始展示和交易日定位。 */
    quote?: MarketQuote
    /** 查询截止交易日；不传时优先使用行情快照交易日。 */
    tradeDate?: string
}>(), {
    tradeDate: ''
})

const activeTab = ref('quote')
const resolvedTradeDate = computed(() => props.tradeDate || props.quote?.tradeDate || undefined)
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
