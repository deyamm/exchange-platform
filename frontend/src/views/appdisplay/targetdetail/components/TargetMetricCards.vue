<template>
    <div class="metric-card-grid">
        <el-card v-for="item in sortedCards" :key="item.key" class="metric-card" shadow="never">
            <div class="metric-title-row">
                <span class="metric-title">{{ item.title }}</span>
                <el-tag v-if="item.status" size="small" :type="statusTagType(item.status)">
                    {{ statusText(item.status) }}
                </el-tag>
            </div>

            <div class="metric-value" :class="trendClass(item.trend)">
                {{ item.displayValue || item.value }}
                <span v-if="item.unit && !item.displayValue" class="metric-unit">{{ item.unit }}</span>
            </div>

            <div v-if="item.compareText" class="metric-compare">{{ item.compareText }}</div>
            <div v-if="item.description" class="metric-description">{{ item.description }}</div>
        </el-card>
    </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { MetricCard, MetricTrend } from '/@/types/targetdetail/targetDetailCenter'
import { statusTagType } from '../utils/targetDetailFormat'

const props = defineProps<{
    cards: MetricCard[]
}>()

const sortedCards = computed(() => {
    return [...(props.cards || [])].sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
})

function trendClass(trend?: MetricTrend): string {
    if (trend === 'UP') return 'is-up'
    if (trend === 'DOWN') return 'is-down'
    return ''
}

function statusText(status: string): string {
    const map: Record<string, string> = {
        GOOD: '良好',
        NORMAL: '正常',
        WARNING: '关注',
        DANGER: '风险'
    }
    return map[status] || status
}
</script>

<style scoped>
.metric-card-grid {
    display: grid;
    grid-template-columns: repeat(6, minmax(150px, 1fr));
    gap: 12px;
}

.metric-card {
    border-radius: 12px;
}

.metric-title-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
}

.metric-title {
    font-size: 13px;
    color: #909399;
}

.metric-value {
    margin-top: 12px;
    font-size: 24px;
    font-weight: 700;
    color: #303133;
    word-break: break-all;
}

.metric-unit {
    margin-left: 4px;
    font-size: 13px;
    font-weight: 400;
    color: #909399;
}

.metric-compare,
.metric-description {
    margin-top: 8px;
    font-size: 12px;
    line-height: 1.4;
    color: #606266;
}

.metric-description {
    color: #909399;
}

.is-up {
    color: #f56c6c;
}

.is-down {
    color: #67c23a;
}

@media (max-width: 1440px) {
    .metric-card-grid {
        grid-template-columns: repeat(3, minmax(150px, 1fr));
    }
}

@media (max-width: 768px) {
    .metric-card-grid {
        grid-template-columns: 1fr;
    }
}
</style>
