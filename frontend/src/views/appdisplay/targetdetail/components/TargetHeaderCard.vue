<template>
    <el-card class="target-header-card" shadow="never">
        <!-- 标的基本信息与当前价格 -->
        <div class="header-main">
            <!-- 左侧：证券名称、代码、类型、状态、市场信息、标签及顶部操作按钮 -->
            <div class="target-identity">
                <div class="title-line">
                    <span class="target-name">{{ target.targetName }}</span>
                    <span class="target-code">{{ target.targetCode }}</span>

                    <el-tag size="small" type="info">
                        {{ targetTypeText(target.targetType) }}
                    </el-tag>

                    <el-tag
                        v-if="quote?.marketStatus"
                        size="small"
                        :type="quote.marketStatus === 'TRADING' ? 'success' : 'info'"
                    >
                        {{ marketStatusText(quote.marketStatus) }}
                    </el-tag>

                    <!-- 顶部操作按钮：放在 marketStatus 右侧 -->
                    <div class="header-actions-inline">
                        <el-button
                            size="small"
                            type="primary"
                            @click="emits('refresh')"
                        >
                            刷新
                        </el-button>

                        <el-button
                            size="small"
                            @click="emits('watch')"
                        >
                            设置关注
                        </el-button>

                        <el-button
                            size="small"
                            @click="emits('create-alert')"
                        >
                            新建提醒
                        </el-button>
                    </div>
                </div>

                <div class="sub-line">
                    <span>{{ target.marketName }}</span>
                    <span v-if="target.exchangeName"> / {{ target.exchangeName }}</span>
                    <span v-if="target.industryName"> / {{ target.industryName }}</span>
                    <span v-if="target.currency"> / {{ target.currency }}</span>
                </div>

                <div v-if="target.tags?.length" class="tag-line">
                    <el-tag
                        v-for="tag in target.tags"
                        :key="tag"
                        size="small"
                        effect="plain"
                    >
                        {{ tag }}
                    </el-tag>
                </div>
            </div>

            <!-- 右侧：价格、涨跌、行情时间 -->
            <div class="quote-panel" :class="signedClass(quote?.changePercent)">
                <div class="price">
                    {{ quote?.lastPrice ?? '-' }}
                    <span class="unit">{{ quote?.priceUnit || '' }}</span>
                </div>

                <div class="change-line">
                    <span>{{ signedAmount }}</span>
                    <span>{{ formatPercent(quote?.changePercent, 2, true) }}</span>
                </div>

                <div class="time-line">
                    数据时间：{{ quoteTime }}
                </div>
            </div>
        </div>

        <!-- 行情指标区 -->
        <div class="quote-bottom">
            <div class="quote-grid">
                <div
                    v-for="item in quoteRows"
                    :key="item.label"
                    class="quote-item"
                >
                    <span class="quote-label">{{ item.label }}</span>
                    <span class="quote-value" :class="item.className">
                        {{ item.value }}
                    </span>
                </div>
            </div>
        </div>
    </el-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type {
    MarketQuote,
    TargetOverview,
    TargetProfile
} from '/@/types/targetdetail/targetDetailCenter'
import {
    formatAmount,
    formatDateTime,
    formatNumber,
    formatPercent,
    marketStatusText,
    signedClass,
    targetTypeText
} from '../utils/targetDetailFormat'

const emits = defineEmits<{
    (e: 'refresh'): void
    (e: 'watch'): void
    (e: 'create-alert'): void
}>()

const props = defineProps<{
    target: TargetProfile
    quote?: MarketQuote
    overview?: TargetOverview
}>()

/**
 * 涨跌金额：
 * 正数添加 + 号，负数保留 - 号，没有行情时展示 -
 */
const signedAmount = computed(() => {
    const value = props.quote?.changeAmount

    if (value === undefined || value === null) {
        return '-'
    }

    return `${value > 0 ? '+' : ''}${formatNumber(value, 2)}`
})

/**
 * 行情更新时间
 */
const quoteTime = computed(() => {
    if (!props.quote) {
        return '-'
    }

    return formatDateTime(
        `${props.quote.tradeDate} ${props.quote.tradeTime}`
    )
})

/**
 * 当天行情指标
 */
const quoteRows = computed(() => {
    const previousClose = props.quote?.previousClose || 0

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
            value: formatNumber(props.quote?.previousClose, 2),
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
            value: formatAmount(props.quote?.amount),
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
.target-header-card {
    border-radius: 12px;
}

/* 顶部：左侧标的信息，右侧当前价格 */
.header-main {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 24px;
}

.target-identity {
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

.target-name {
    color: #303133;
    font-size: 24px;
    font-weight: 700;
}

.target-code {
    color: #909399;
    font-size: 14px;
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
    margin-top: 12px;
}

/* 顶部操作区：紧跟 marketStatus 标签右侧 */
.header-actions-inline {
    display: flex;
    flex-shrink: 0;
    align-items: center;
    gap: 8px;
    white-space: nowrap;
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

/* 当前行情价格 */
.quote-panel {
    min-width: 220px;
    flex-shrink: 0;
    text-align: right;
}

.price {
    font-size: 34px;
    font-weight: 700;
    line-height: 1.1;
}

.unit {
    color: #909399;
    font-size: 14px;
    font-weight: 400;
}

.change-line {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
    margin-top: 8px;
    font-size: 16px;
    font-weight: 600;
}

.time-line {
    margin-top: 8px;
    color: #909399;
    font-size: 12px;
}

/*
 * 下方区域：
 * 仅保留行情指标；
 * 操作按钮已移动到 title-line 内。
 */
.quote-bottom {
    margin-top: 20px;
    padding-top: 16px;
    border-top: 1px solid #ebeef5;
}

/* 当天行情指标 */
.quote-grid {
    display: grid;
    min-width: 0;
    grid-template-columns: repeat(8, minmax(72px, 1fr));
    gap: 12px;
}

.quote-item {
    display: flex;
    min-width: 0;
    flex-direction: column;
    gap: 6px;
}

.quote-label {
    color: #909399;
    font-size: 12px;
}

.quote-value {
    overflow: hidden;
    color: #303133;
    font-size: 14px;
    font-weight: 600;
    text-overflow: ellipsis;
    white-space: nowrap;
}

/* 涨跌状态 */
.is-up {
    color: #f56c6c;
}

.is-down {
    color: #67c23a;
}

.is-flat {
    color: #606266;
}

/* 中等屏幕：指标改为四列，顶部按钮可换行 */
@media (max-width: 1280px) {
    .quote-grid {
        grid-template-columns: repeat(4, minmax(92px, 1fr));
    }

    .header-actions-inline {
        flex-wrap: wrap;
    }
}

/* 小屏幕：价格区下移，按钮在标题信息下方自然换行 */
@media (max-width: 768px) {
    .header-main {
        flex-direction: column;
        gap: 16px;
    }

    .quote-panel {
        min-width: 0;
        text-align: left;
    }

    .change-line {
        justify-content: flex-start;
    }

    .header-actions-inline {
        width: 100%;
        margin-top: 2px;
        flex-wrap: wrap;
        justify-content: flex-start;
    }

    .quote-grid {
        width: 100%;
        grid-template-columns: repeat(2, minmax(92px, 1fr));
    }
}
</style>