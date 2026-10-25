<template>
    <div class="valuation-tab">
        <template v-if="indicator">
            <el-row :gutter="16">
                <el-col :xs="24" :lg="14">
                    <el-card class="tab-card" shadow="never">
                        <template #header>
                            <div class="card-header">
                                <span class="card-title">估值与规模指标</span>
                                <el-tag :type="statusTagType(indicator.valuationStatus)">
                                    {{ valuationStatusText(indicator.valuationStatus) }}
                                </el-tag>
                            </div>
                        </template>

                        <el-descriptions :column="2" border>
                            <el-descriptions-item label="交易日期">{{ indicator.tradeDate }}</el-descriptions-item>
                            <el-descriptions-item label="PE">{{ formatNumber(indicator.pe, 2) }}</el-descriptions-item>
                            <el-descriptions-item label="PE TTM">{{ formatNumber(indicator.peTtm, 2) }}</el-descriptions-item>
                            <el-descriptions-item label="PB">{{ formatNumber(indicator.pb, 2) }}</el-descriptions-item>
                            <el-descriptions-item label="总市值">{{ formatAmount(indicator.totalMarketValue) }}</el-descriptions-item>
                            <el-descriptions-item label="流通市值">{{ formatAmount(indicator.floatMarketValue) }}</el-descriptions-item>
                            <el-descriptions-item label="总股本">{{ formatAmount(indicator.totalShare) }}</el-descriptions-item>
                            <el-descriptions-item label="流通股本">{{ formatAmount(indicator.floatShare) }}</el-descriptions-item>
                            <el-descriptions-item label="自由流通股本">{{ formatAmount(indicator.freeShare) }}</el-descriptions-item>
                            <el-descriptions-item label="换手率">{{ formatPercent(indicator.turnoverRate) }}</el-descriptions-item>
                            <el-descriptions-item label="自由流通换手率">{{ formatPercent(indicator.turnoverRateFree) }}</el-descriptions-item>
                        </el-descriptions>

                        <el-alert
                            v-if="indicator.conclusion"
                            class="conclusion"
                            :title="indicator.conclusion"
                            type="info"
                            show-icon
                            :closable="false"
                        />
                    </el-card>
                </el-col>

                <el-col :xs="24" :lg="10">
                    <el-card class="tab-card" shadow="never">
                        <template #header>
                            <span class="card-title">字段口径</span>
                        </template>

                        <div class="field-source-list">
                            <div>总市值：index_indicator_daily.total_market_value</div>
                            <div>流通市值：index_indicator_daily.float_market_value</div>
                            <div>PE：index_indicator_daily.pe</div>
                            <div>PE TTM：index_indicator_daily.pe_ttm</div>
                            <div>PB：index_indicator_daily.pb</div>
                            <div>换手率：index_indicator_daily.turnover_rate</div>
                            <div>自由流通换手率：index_indicator_daily.turnover_rate_free</div>
                        </div>
                    </el-card>
                </el-col>
            </el-row>
        </template>

        <el-empty v-else description="暂无估值与规模数据" />
    </div>
</template>

<script setup lang="ts">
import type { IndexIndicatorDaily } from '/@/types/indexdetail/indexDetailCenter'
import { formatAmount, formatNumber, formatPercent, statusTagType, valuationStatusText } from '/@/utils/marketUtils'

defineProps<{
    indicator?: IndexIndicatorDaily
}>()
</script>

<style scoped>
.valuation-tab {
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

.field-source-list {
    display: flex;
    flex-direction: column;
    gap: 10px;
    color: #606266;
    font-size: 13px;
    line-height: 1.6;
}
</style>
