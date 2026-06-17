<template>
    <div class="financial-tab">
        <template v-if="financial">
            <el-card class="tab-card" shadow="never">
                <template #header>
                    <div class="card-header">
                        <span class="card-title">财务摘要</span>
                        <el-tag type="info">{{ financial.reportPeriod }}</el-tag>
                    </div>
                </template>

                <el-descriptions :column="3" border>
                    <el-descriptions-item label="营业收入">{{ formatAmount(financial.revenue) }}</el-descriptions-item>
                    <el-descriptions-item label="营收同比">{{ formatPercent(financial.revenueYoY, 2, true) }}</el-descriptions-item>
                    <el-descriptions-item label="净利润">{{ formatAmount(financial.netProfit) }}</el-descriptions-item>
                    <el-descriptions-item label="净利润同比">{{ formatPercent(financial.netProfitYoY, 2, true) }}</el-descriptions-item>
                    <el-descriptions-item label="扣非净利润">{{ formatAmount(financial.deductedNetProfit) }}</el-descriptions-item>
                    <el-descriptions-item label="扣非同比">{{ formatPercent(financial.deductedNetProfitYoY, 2, true) }}</el-descriptions-item>
                    <el-descriptions-item label="ROE">{{ formatPercent(financial.roe) }}</el-descriptions-item>
                    <el-descriptions-item label="ROA">{{ formatPercent(financial.roa) }}</el-descriptions-item>
                    <el-descriptions-item label="资产负债率">{{ formatPercent(financial.debtAssetRatio) }}</el-descriptions-item>
                    <el-descriptions-item label="经营现金流">{{ formatAmount(financial.operatingCashFlow) }}</el-descriptions-item>
                    <el-descriptions-item label="自由现金流">{{ formatAmount(financial.freeCashFlow) }}</el-descriptions-item>
                    <el-descriptions-item label="EPS">{{ financial.eps ?? '-' }}</el-descriptions-item>
                    <el-descriptions-item label="每股净资产">{{ financial.bps ?? '-' }}</el-descriptions-item>
                    <el-descriptions-item label="每股分红">{{ financial.dividendPerShare ?? '-' }}</el-descriptions-item>
                    <el-descriptions-item label="分红率">{{ formatPercent(financial.dividendPayoutRatio) }}</el-descriptions-item>
                </el-descriptions>

                <el-alert
                    v-if="financial.conclusion"
                    class="conclusion"
                    :title="financial.conclusion"
                    type="info"
                    show-icon
                    :closable="false"
                />
            </el-card>
        </template>

        <el-empty v-else description="暂无财务数据" />
    </div>
</template>

<script setup lang="ts">
import type { FinancialSummary } from '/@/types/targetdetail/targetDetailCenter'
import { formatAmount, formatPercent } from '../../utils/targetDetailFormat'

defineProps<{
    financial?: FinancialSummary
}>()
</script>

<style scoped>
.financial-tab {
    min-height: 360px;
}

.tab-card {
    border-radius: 10px;
}

.card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.card-title {
    font-weight: 600;
    color: #303133;
}

.conclusion {
    margin-top: 16px;
}
</style>
