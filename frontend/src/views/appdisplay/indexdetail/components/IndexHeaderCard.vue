<template>
    <el-card class="index-header-card" shadow="never">
        <div class="header-main">
            <div class="index-identity">
                <div class="title-line">
                    <span class="index-name">{{ profile.name }}</span>
                    <span class="index-code">{{ profile.fullSymbol }}</span>

                    <el-tag size="small" type="info">指数</el-tag>

                    <el-tag
                        v-if="quote?.marketStatus"
                        size="small"
                        :type="quote.marketStatus === 'TRADING' ? 'success' : 'info'"
                    >
                        {{ marketStatusText(quote.marketStatus) }}
                    </el-tag>

                    <div class="header-actions-inline">
                        <el-button size="small" type="primary" @click="emit('refresh')">刷新</el-button>
                        <el-button size="small" @click="emit('watch')">设置关注</el-button>
                        <el-button size="small" @click="emit('create-alert')">新建提醒</el-button>
                    </div>
                </div>

                <div class="sub-line">
                    <span>{{ profile.marketName || profile.market || '-' }}</span>
                    <span v-if="profile.publisher"> / {{ profile.publisher }}</span>
                    <span v-if="profile.category"> / {{ profile.category }}</span>
                    <span v-if="profile.indexType"> / {{ profile.indexType }}</span>
                </div>

                <div v-if="profile.tags?.length" class="tag-line">
                    <el-tag v-for="tag in profile.tags" :key="tag" size="small" effect="plain">
                        {{ tag }}
                    </el-tag>
                </div>
            </div>

            <div class="quote-panel" :class="signedClass(quote?.chgPct)">
                <div class="price">
                    {{ formatNumber(quote?.close, 2) }}
                    <span class="unit">点</span>
                </div>

                <div class="change-line">
                    <span>{{ signedAmount }}</span>
                    <span>{{ formatPercent(quote?.chgPct, 2, true) }}</span>
                </div>

                <div class="time-line">
                    数据时间：{{ quoteTime }}
                </div>
            </div>
        </div>

        <div class="quote-bottom">
            <div class="quote-grid">
                <div v-for="item in quoteRows" :key="item.label" class="quote-item">
                    <span class="quote-label">{{ item.label }}</span>
                    <span class="quote-value" :class="item.className">{{ item.value }}</span>
                </div>
            </div>
        </div>
    </el-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { IndexProfile, MarketQuote } from '/@/types/marketoverview/view'
import { formatAmount, formatDateTime, formatNumber, formatPercent, marketStatusText, signedClass } from '/@/utils/marketUtils'

const emit = defineEmits<{
    (e: 'refresh'): void
    (e: 'watch'): void
    (e: 'create-alert'): void
}>()

const props = defineProps<{
    profile: IndexProfile
    quote?: MarketQuote
}>()

const signedAmount = computed(() => {
    const value = props.quote?.chgAmt
    if (value === undefined || value === null) return '-'
    return `${value > 0 ? '+' : ''}${formatNumber(value, 2)}`
})

const quoteTime = computed(() => {
    if (!props.quote) return '-'
    return formatDateTime(`${props.quote.tradeDate} ${props.quote.tradeTime || ''}`.trim())
})

const quoteRows = computed(() => {
    const previousClose = props.quote?.preClose || 0

    return [
        {
            label: '今开',
            value: formatNumber(props.quote?.open, 2),
            className: signedClass((props.quote?.open || 0) - previousClose)
        },
        {
            label: '最高',
            value: formatNumber(props.quote?.high, 2),
            className: signedClass((props.quote?.high || 0) - previousClose)
        },
        {
            label: '最低',
            value: formatNumber(props.quote?.low, 2),
            className: signedClass((props.quote?.low || 0) - previousClose)
        },
        {
            label: '昨收',
            value: formatNumber(props.quote?.preClose, 2),
            className: ''
        },
        {
            label: '振幅',
            value: formatPercent(props.quote?.amplitude),
            className: ''
        },
        {
            label: '成交量',
            value: formatAmount(props.quote?.volume),
            className: ''
        },
        {
            label: '成交额',
            value: formatAmount(props.quote?.turnover),
            className: ''
        },
        {
            label: '换手率',
            value: formatPercent(props.quote?.turnoverRate),
            className: ''
        }
    ]
})
</script>

<style scoped>
.index-header-card {
    border-radius: 12px;
}

.header-main {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 24px;
}

.index-identity {
    min-width: 0;
    flex: 1;
}

.title-line {
    display: flex;
    min-width: 0;
    flex-wrap: wrap;
    align-items: center;
    gap: 10px;
}

.index-name {
    color: #303133;
    font-size: 24px;
    font-weight: 700;
}

.index-code {
    color: #909399;
    font-size: 14px;
}

.header-actions-inline {
    display: flex;
    flex-shrink: 0;
    align-items: center;
    white-space: nowrap;
    gap: 8px;
}

/*
 * Element Plus 默认会给相邻按钮增加 margin-left，
 * 这里统一改用 gap 控制间距。
 */
.header-actions-inline :deep(.el-button + .el-button) {
    margin-left: 0;
}

.header-actions-inline :deep(.el-button) {
    padding-right: 10px;
    padding-left: 10px;
}

.sub-line {
    margin-top: 8px;
    color: #606266;
    font-size: 13px;
}

.tag-line {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    margin-top: 10px;
}

.quote-panel {
    min-width: 220px;
    text-align: right;
}

.price {
    font-size: 32px;
    font-weight: 700;
    line-height: 1.2;
}

.unit {
    margin-left: 4px;
    font-size: 13px;
    color: #909399;
}

.change-line {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
    margin-top: 8px;
    font-size: 15px;
    font-weight: 600;
}

.time-line {
    margin-top: 8px;
    color: #909399;
    font-size: 12px;
}

.quote-bottom {
    margin-top: 16px;
    padding-top: 14px;
    border-top: 1px solid #ebeef5;
}

.quote-grid {
    display: grid;
    grid-template-columns: repeat(8, minmax(96px, 1fr));
    gap: 10px 14px;
}

.quote-item {
    display: flex;
    flex-direction: column;
    gap: 4px;
}

.quote-label {
    color: #909399;
    font-size: 12px;
}

.quote-value {
    color: #303133;
    font-size: 14px;
    font-weight: 600;
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

@media (max-width: 1024px) {
    .header-main {
        flex-direction: column;
    }

    .quote-panel {
        width: 100%;
        text-align: left;
    }

    .change-line {
        justify-content: flex-start;
    }

    .quote-grid {
        grid-template-columns: repeat(4, minmax(96px, 1fr));
    }
}

@media (max-width: 640px) {
    .quote-grid {
        grid-template-columns: repeat(2, minmax(96px, 1fr));
    }
}
</style>
