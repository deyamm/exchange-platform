<template>
    <div class="valuation-tab">
        <template v-if="valuation">
            <el-row :gutter="16">
                <el-col :xs="24" :lg="14">
                    <el-card class="tab-card" shadow="never">
                        <template #header>
                            <div class="card-header">
                                <span class="card-title">估值指标</span>
                                <el-tag :type="statusTagType(valuation.valuationStatus)">
                                    {{ valuationStatusText(valuation.valuationStatus) }}
                                </el-tag>
                            </div>
                        </template>

                        <el-descriptions :column="2" border>
                            <el-descriptions-item label="PE TTM">{{ valuation.peTtm ?? '-' }}</el-descriptions-item>
                            <el-descriptions-item label="PB">{{ valuation.pb ?? '-' }}</el-descriptions-item>
                            <el-descriptions-item label="PS">{{ valuation.ps ?? '-' }}</el-descriptions-item>
                            <el-descriptions-item label="PEG">{{ valuation.peg ?? '-' }}</el-descriptions-item>
                            <el-descriptions-item label="股息率">{{ formatPercent(valuation.dividendYield) }}</el-descriptions-item>
                            <el-descriptions-item label="总市值">{{ formatAmount(valuation.marketCap) }}</el-descriptions-item>
                            <el-descriptions-item label="流通市值">{{ formatAmount(valuation.floatMarketCap) }}</el-descriptions-item>
                            <el-descriptions-item label="行业PE中位数">{{ valuation.industryPeMedian ?? '-' }}</el-descriptions-item>
                            <el-descriptions-item label="行业PB中位数">{{ valuation.industryPbMedian ?? '-' }}</el-descriptions-item>
                        </el-descriptions>

                        <el-alert
                            v-if="valuation.conclusion"
                            class="conclusion"
                            :title="valuation.conclusion"
                            type="info"
                            show-icon
                            :closable="false"
                        />
                    </el-card>
                </el-col>

                <el-col :xs="24" :lg="10">
                    <el-card class="tab-card" shadow="never">
                        <template #header>
                            <span class="card-title">历史分位</span>
                        </template>

                        <div class="percentile-item">
                            <div class="percentile-label">
                                <span>PE五年分位</span>
                                <strong>{{ formatPercent(valuation.pePercentile) }}</strong>
                            </div>
                            <el-progress :percentage="valuation.pePercentile || 0" />
                        </div>

                        <div class="percentile-item">
                            <div class="percentile-label">
                                <span>PB五年分位</span>
                                <strong>{{ formatPercent(valuation.pbPercentile) }}</strong>
                            </div>
                            <el-progress :percentage="valuation.pbPercentile || 0" />
                        </div>

                        <div class="percentile-item">
                            <div class="percentile-label">
                                <span>PS五年分位</span>
                                <strong>{{ formatPercent(valuation.psPercentile) }}</strong>
                            </div>
                            <el-progress :percentage="valuation.psPercentile || 0" />
                        </div>
                    </el-card>
                </el-col>
            </el-row>
        </template>

        <el-empty v-else description="暂无估值数据" />
    </div>
</template>

<script setup lang="ts">
import type { ValuationInfo } from '/@/types/targetdetail/targetDetailCenter'
import {
    formatAmount,
    formatPercent,
    statusTagType,
    valuationStatusText
} from '../../utils/targetDetailFormat'

defineProps<{
    valuation?: ValuationInfo
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

.percentile-item + .percentile-item {
    margin-top: 28px;
}

.percentile-label {
    display: flex;
    justify-content: space-between;
    margin-bottom: 8px;
    font-size: 13px;
    color: #606266;
}
</style>
