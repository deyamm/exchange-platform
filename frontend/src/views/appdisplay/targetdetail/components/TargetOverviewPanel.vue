<template>
    <el-card class="overview-panel" shadow="never">
        <template #header>
            <div class="card-header">
                <span class="card-title">综合分析结论</span>
                <div class="header-tags">
                    <el-tag :type="statusTagType(overview.overallView)">
                        {{ overview.overallViewText || overallViewText(overview.overallView) }}
                    </el-tag>
                    <el-tag :type="statusTagType(overview.riskLevel)">
                        风险：{{ riskLevelText(overview.riskLevel) }}
                    </el-tag>
                    <el-tag v-if="overview.valuationStatus" type="info">
                        估值：{{ valuationStatusText(overview.valuationStatus) }}
                    </el-tag>
                </div>
            </div>
        </template>

        <div class="summary-text">{{ overview.summary }}</div>

        <el-row :gutter="16" class="overview-sections">
            <el-col :xs="24" :md="8">
                <div class="section">
                    <div class="section-title">支撑理由</div>
                    <ul class="point-list">
                        <li v-for="item in overview.reasons" :key="item">{{ item }}</li>
                    </ul>
                </div>
            </el-col>

            <el-col :xs="24" :md="8">
                <div class="section">
                    <div class="section-title">主要风险</div>
                    <ul class="point-list">
                        <li v-for="item in overview.risks" :key="item">{{ item }}</li>
                    </ul>
                </div>
            </el-col>

            <el-col :xs="24" :md="8">
                <div class="section">
                    <div class="section-title">观察建议</div>
                    <ul class="point-list">
                        <li v-for="item in overview.suggestions" :key="item">{{ item }}</li>
                    </ul>
                </div>
            </el-col>
        </el-row>

        <div v-if="overview.confidence !== undefined" class="confidence-row">
            <span>结论置信度</span>
            <el-progress :percentage="Math.round(overview.confidence * 100)" :stroke-width="10" />
        </div>
    </el-card>
</template>

<script setup lang="ts">
import type { TargetOverview } from '/@/types/targetdetail/targetDetailCenter'
import {
    overallViewText,
    riskLevelText,
    statusTagType,
    valuationStatusText
} from '../utils/targetDetailFormat'

defineProps<{
    overview: TargetOverview
}>()
</script>

<style scoped>
.overview-panel {
    border-radius: 12px;
}

.card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
}

.card-title {
    font-size: 16px;
    font-weight: 600;
    color: #303133;
}

.header-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
}

.summary-text {
    font-size: 15px;
    line-height: 1.8;
    color: #303133;
}

.overview-sections {
    margin-top: 14px;
}

.section {
    min-height: 112px;
    padding: 12px;
    background: #fafafa;
    border: 1px solid #ebeef5;
    border-radius: 10px;
}

.section-title {
    margin-bottom: 8px;
    font-size: 13px;
    font-weight: 600;
    color: #606266;
}

.point-list {
    margin: 0;
    padding-left: 18px;
    color: #606266;
    font-size: 13px;
    line-height: 1.7;
}

.confidence-row {
    display: grid;
    grid-template-columns: 96px 1fr;
    align-items: center;
    gap: 12px;
    margin-top: 16px;
    font-size: 13px;
    color: #606266;
}
</style>
