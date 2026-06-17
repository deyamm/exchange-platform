<template>
    <div class="side-panel">
        <el-card class="side-card" shadow="never">
            <template #header>
                <span class="side-title">观察状态</span>
            </template>
            <template v-if="watch">
                <div class="watch-status">
                    <el-tag :type="watch.watchStatus === 'KEY_WATCH' ? 'warning' : 'info'">
                        {{ watch.watchStatusText }}
                    </el-tag>
                </div>
                <div class="side-field">
                    <span class="label">关注分组</span>
                    <span class="value">{{ watch.groupNames?.join(' / ') || '-' }}</span>
                </div>
                <div class="side-field">
                    <span class="label">关注理由</span>
                    <span class="value">{{ watch.watchReason || '-' }}</span>
                </div>
                <div class="side-field">
                    <span class="label">首次关注</span>
                    <span class="value">{{ formatDateTime(watch.firstWatchTime) }}</span>
                </div>
                <div class="side-field">
                    <span class="label">最近更新</span>
                    <span class="value">{{ formatDateTime(watch.lastUpdatedTime) }}</span>
                </div>
            </template>
            <el-empty v-else description="暂无观察信息" :image-size="56" />
        </el-card>

        <el-card class="side-card" shadow="never">
            <template #header>
                <div class="side-card-header">
                    <span class="side-title">提醒规则</span>
                    <el-button link type="primary" @click="$emit('create-alert')">新增</el-button>
                </div>
            </template>
            <div v-if="alerts?.length" class="alert-list">
                <div v-for="item in alerts" :key="item.alertId" class="alert-item">
                    <div class="alert-title-row">
                        <span class="alert-title">{{ item.title }}</span>
                        <el-tag size="small" :type="severityType(item.severity)">
                            {{ severityText(item.severity) }}
                        </el-tag>
                    </div>
                    <div class="alert-condition">{{ item.conditionText }}</div>
                    <div class="alert-meta">
                        {{ item.enabled ? '已启用' : '已停用' }}
                        <span v-if="item.triggerCount"> · 触发 {{ item.triggerCount }} 次</span>
                    </div>
                </div>
            </div>
            <el-empty v-else description="暂无提醒规则" :image-size="56" />
        </el-card>

        <el-card class="side-card" shadow="never">
            <template #header>
                <span class="side-title">事件日历</span>
            </template>
            <el-timeline v-if="events?.length" class="mini-timeline">
                <el-timeline-item
                    v-for="item in events.slice(0, 4)"
                    :key="item.eventId"
                    :timestamp="item.eventDate"
                    placement="top"
                >
                    <div class="timeline-title">{{ item.title }}</div>
                    <div class="timeline-summary">{{ item.summary }}</div>
                </el-timeline-item>
            </el-timeline>
            <el-empty v-else description="暂无事件" :image-size="56" />
        </el-card>

        <el-card class="side-card" shadow="never">
            <template #header>
                <span class="side-title">相关标的</span>
            </template>
            <div v-if="relatedTargets?.length" class="related-list">
                <div
                    v-for="item in relatedTargets"
                    :key="`${item.relationType}-${item.targetCode}`"
                    class="related-item"
                    @click="$emit('select-target', item.targetCode)"
                >
                    <div>
                        <div class="related-name">{{ item.targetName }}</div>
                        <div class="related-code">{{ item.targetCode }} · {{ item.relationName }}</div>
                    </div>
                    <div :class="signedClass(item.changePercent)">
                        {{ formatPercent(item.changePercent, 2, true) }}
                    </div>
                </div>
            </div>
            <el-empty v-else description="暂无相关标的" :image-size="56" />
        </el-card>
    </div>
</template>

<script setup lang="ts">
import type { AlertRule, RelatedTarget, TargetEvent, TargetWatchInfo } from '/@/types/targetdetail/targetDetailCenter'
import { formatDateTime, formatPercent, signedClass } from '../utils/targetDetailFormat'

defineEmits<{
    'create-alert': []
    'select-target': [targetCode: string]
}>()

defineProps<{
    watch?: TargetWatchInfo
    alerts: AlertRule[]
    events: TargetEvent[]
    relatedTargets: RelatedTarget[]
}>()

function severityType(severity: string): '' | 'success' | 'warning' | 'danger' | 'info' {
    const map: Record<string, '' | 'success' | 'warning' | 'danger' | 'info'> = {
        LOW: 'success',
        MEDIUM: 'warning',
        HIGH: 'danger'
    }
    return map[severity] || 'info'
}

function severityText(severity: string): string {
    const map: Record<string, string> = {
        LOW: '低',
        MEDIUM: '中',
        HIGH: '高'
    }
    return map[severity] || severity
}
</script>

<style scoped>
.side-panel {
    display: flex;
    flex-direction: column;
    gap: 14px;
}

.side-card {
    border-radius: 12px;
}

.side-card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.side-title {
    font-size: 15px;
    font-weight: 600;
    color: #303133;
}

.watch-status {
    margin-bottom: 12px;
}

.side-field {
    display: grid;
    grid-template-columns: 72px 1fr;
    gap: 8px;
    margin-top: 10px;
    font-size: 13px;
    line-height: 1.5;
}

.label {
    color: #909399;
}

.value {
    color: #303133;
    word-break: break-all;
}

.alert-list,
.related-list {
    display: flex;
    flex-direction: column;
    gap: 10px;
}

.alert-item,
.related-item {
    padding: 10px;
    background: #fafafa;
    border: 1px solid #ebeef5;
    border-radius: 8px;
}

.alert-title-row,
.related-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
}

.alert-title,
.related-name,
.timeline-title {
    font-size: 13px;
    font-weight: 600;
    color: #303133;
}

.alert-condition,
.alert-meta,
.related-code,
.timeline-summary {
    margin-top: 5px;
    font-size: 12px;
    line-height: 1.4;
    color: #909399;
}

.related-item {
    cursor: pointer;
    transition: background 0.2s;
}

.related-item:hover {
    background: #f5f7fa;
}

.mini-timeline {
    padding-left: 2px;
}

.is-up {
    color: #f56c6c;
}

.is-down {
    color: #67c23a;
}

.is-flat {
    color: #606266;
}
</style>
