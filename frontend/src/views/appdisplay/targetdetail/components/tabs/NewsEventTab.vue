<template>
    <div class="news-event-tab">
        <el-row :gutter="16">
            <el-col :xs="24" :lg="12">
                <el-card class="tab-card" shadow="never">
                    <template #header>
                        <span class="card-title">新闻资讯</span>
                    </template>

                    <div v-if="news?.length" class="news-list">
                        <div v-for="item in news" :key="item.newsId" class="news-item">
                            <div class="news-title-row">
                                <span class="news-title">{{ item.title }}</span>
                                <el-tag v-if="item.sentiment" size="small" :type="sentimentType(item.sentiment)">
                                    {{ sentimentText(item.sentiment) }}
                                </el-tag>
                            </div>
                            <div class="news-meta">{{ item.source }} · {{ formatDateTime(item.publishTime) }}</div>
                            <div v-if="item.summary" class="news-summary">{{ item.summary }}</div>
                        </div>
                    </div>

                    <el-empty v-else description="暂无新闻资讯" />
                </el-card>
            </el-col>

            <el-col :xs="24" :lg="12">
                <el-card class="tab-card" shadow="never">
                    <template #header>
                        <span class="card-title">公告 / 事件时间线</span>
                    </template>

                    <el-timeline v-if="events?.length">
                        <el-timeline-item
                            v-for="item in events"
                            :key="item.eventId"
                            :timestamp="item.eventDate"
                            placement="top"
                            :type="eventType(item.importance)"
                        >
                            <div class="event-title-row">
                                <span class="event-title">{{ item.title }}</span>
                                <el-tag size="small" :type="eventType(item.importance)">
                                    {{ importanceText(item.importance) }}
                                </el-tag>
                            </div>
                            <div class="event-meta">{{ item.source || '-' }} · {{ eventTypeText(item.eventType) }}</div>
                            <div v-if="item.summary" class="event-summary">{{ item.summary }}</div>
                        </el-timeline-item>
                    </el-timeline>

                    <el-empty v-else description="暂无公告事件" />
                </el-card>
            </el-col>
        </el-row>
    </div>
</template>

<script setup lang="ts">
import type { TargetEvent, TargetNews } from '/@/types/targetdetail/targetDetailCenter'
import { formatDateTime } from '../../utils/targetDetailFormat'

defineProps<{
    events: TargetEvent[]
    news: TargetNews[]
}>()

function sentimentType(sentiment: string): '' | 'success' | 'warning' | 'danger' | 'info' {
    const map: Record<string, '' | 'success' | 'warning' | 'danger' | 'info'> = {
        POSITIVE: 'success',
        NEUTRAL: 'info',
        NEGATIVE: 'danger'
    }
    return map[sentiment] || 'info'
}

function sentimentText(sentiment: string): string {
    const map: Record<string, string> = {
        POSITIVE: '正面',
        NEUTRAL: '中性',
        NEGATIVE: '负面'
    }
    return map[sentiment] || sentiment
}

function eventType(importance: string): 'primary' | 'success' | 'warning' | 'danger' | 'info' {
    const map: Record<string, 'primary' | 'success' | 'warning' | 'danger' | 'info'> = {
        LOW: 'info',
        MEDIUM: 'warning',
        HIGH: 'danger'
    }
    return map[importance] || 'info'
}

function importanceText(importance: string): string {
    const map: Record<string, string> = {
        LOW: '低',
        MEDIUM: '中',
        HIGH: '高'
    }
    return map[importance] || importance
}

function eventTypeText(type: string): string {
    const map: Record<string, string> = {
        ANNOUNCEMENT: '公告',
        EARNINGS: '财报',
        DIVIDEND: '分红',
        INDEX_REBALANCE: '指数调样',
        SHAREHOLDER_MEETING: '股东大会',
        RISK: '风险事件',
        NEWS: '新闻',
        CUSTOM: '自定义'
    }
    return map[type] || type
}
</script>

<style scoped>
.news-event-tab {
    min-height: 360px;
}

.tab-card {
    border-radius: 10px;
}

.card-title {
    font-weight: 600;
    color: #303133;
}

.news-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.news-item {
    padding: 12px;
    background: #fafafa;
    border: 1px solid #ebeef5;
    border-radius: 10px;
}

.news-title-row,
.event-title-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
}

.news-title,
.event-title {
    font-size: 14px;
    font-weight: 600;
    color: #303133;
}

.news-meta,
.event-meta {
    margin-top: 6px;
    font-size: 12px;
    color: #909399;
}

.news-summary,
.event-summary {
    margin-top: 8px;
    font-size: 13px;
    line-height: 1.6;
    color: #606266;
}
</style>
