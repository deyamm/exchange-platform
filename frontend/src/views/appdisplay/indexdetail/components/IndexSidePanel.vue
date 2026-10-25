<template>
    <div class="side-panel">
        <el-card class="side-card" shadow="never">
            <template #header>
                <span class="side-title">指数资料</span>
            </template>

            <div class="side-field">
                <span class="label">发布方</span>
                <span class="value">{{ profile.publisher || '-' }}</span>
            </div>
            <div class="side-field">
                <span class="label">发布日期</span>
                <span class="value">{{ formatDate(profile.listDate) }}</span>
            </div>
            <div class="side-field">
                <span class="label">基期</span>
                <span class="value">{{ formatDate(profile.baseDate) }}</span>
            </div>
            <div class="side-field">
                <span class="label">基点</span>
                <span class="value">{{ formatNumber(profile.basePoint, 2) }}</span>
            </div>
            <div class="side-field">
                <span class="label">加权方式</span>
                <span class="value">{{ profile.weightRule || '-' }}</span>
            </div>
        </el-card>

        <el-card class="side-card" shadow="never">
            <template #header>
                <span class="side-title">前十大权重</span>
            </template>

            <div v-if="constituents?.topWeights?.length" class="related-list">
                <div v-for="item in constituents.topWeights.slice(0, 10)" :key="item.stockCode" class="related-item">
                    <div>
                        <div class="related-name">{{ item.name || item.stockCode }}</div>
                        <div class="related-code">{{ item.stockCode }} · {{ formatPercent(item.weight) }}</div>
                    </div>
                    <div :class="signedClass(item.chgPct)">
                        {{ formatPercent(item.chgPct, 2, true) }}
                    </div>
                </div>
            </div>

            <el-empty v-else description="暂无权重数据" :image-size="56" />
        </el-card>

        <el-card class="side-card" shadow="never">
            <template #header>
                <span class="side-title">数据口径</span>
            </template>

            <div class="data-note">
                <p>基础资料：index_basic_data</p>
                <p>行情：index_k_daily</p>
                <p>估值规模：index_indicator_daily</p>
                <p>成分股：index_constituent_data</p>
            </div>
        </el-card>
    </div>
</template>

<script setup lang="ts">
import type { IndexConstituentInfo } from '/@/types/indexdetail/indexDetailCenter'
import type { IndexProfile } from '/@/types/marketoverview/view'
import { formatDate, formatNumber, formatPercent, signedClass } from '/@/utils/marketUtils'

defineProps<{
    profile: IndexProfile
    constituents?: IndexConstituentInfo
}>()
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

.side-title {
    font-size: 15px;
    font-weight: 600;
    color: #303133;
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

.related-list {
    display: flex;
    flex-direction: column;
    gap: 10px;
}

.related-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
    padding: 10px;
    background: #fafafa;
    border: 1px solid #ebeef5;
    border-radius: 8px;
}

.related-name {
    font-size: 13px;
    font-weight: 600;
    color: #303133;
}

.related-code {
    margin-top: 5px;
    font-size: 12px;
    line-height: 1.4;
    color: #909399;
}

.data-note {
    font-size: 12px;
    line-height: 1.7;
    color: #606266;
}

.data-note p {
    margin: 0 0 6px;
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
