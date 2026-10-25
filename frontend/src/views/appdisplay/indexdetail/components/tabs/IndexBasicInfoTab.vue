<template>
    <div class="basic-info-tab">
        <el-row :gutter="16">
            <el-col :xs="24" :lg="14">
                <el-card class="tab-card" shadow="never">
                    <template #header>
                        <span class="card-title">指数基本资料</span>
                    </template>

                    <el-descriptions :column="2" border>
                        <el-descriptions-item label="指数代码">{{ profile.fullSymbol }}</el-descriptions-item>
                        <el-descriptions-item label="简称">{{ profile.name }}</el-descriptions-item>
                        <el-descriptions-item label="全称">{{ profile.fullname || '-' }}</el-descriptions-item>
                        <el-descriptions-item label="市场">{{ profile.marketName || profile.market || '-' }}</el-descriptions-item>
                        <el-descriptions-item label="发布方">{{ profile.publisher || '-' }}</el-descriptions-item>
                        <el-descriptions-item label="指数风格">{{ profile.indexType || '-' }}</el-descriptions-item>
                        <el-descriptions-item label="指数类别">{{ profile.category || '-' }}</el-descriptions-item>
                        <el-descriptions-item label="加权方式">{{ profile.weightRule || '-' }}</el-descriptions-item>
                        <el-descriptions-item label="基期">{{ formatDate(profile.baseDate) }}</el-descriptions-item>
                        <el-descriptions-item label="基点">{{ formatNumber(profile.basePoint, 2) }}</el-descriptions-item>
                        <el-descriptions-item label="发布日期">{{ formatDate(profile.listDate) }}</el-descriptions-item>
                        <el-descriptions-item label="终止日期">{{ formatDate(profile.expDate) }}</el-descriptions-item>
                    </el-descriptions>
                </el-card>
            </el-col>

            <el-col :xs="24" :lg="10">
                <el-card class="tab-card" shadow="never">
                    <template #header>
                        <span class="card-title">指数描述</span>
                    </template>

                    <div class="description-text">
                        {{ profile.description || '暂无指数描述' }}
                    </div>

                    <div class="source-note">
                        字段来源：type_basic_data.index_basic_data
                    </div>
                </el-card>
            </el-col>
        </el-row>
    </div>
</template>

<script setup lang="ts">
import type { IndexProfile } from '/@/types/marketoverview/view'
import { formatDate, formatNumber } from '/@/utils/marketUtils'

defineProps<{
    profile: IndexProfile
}>()
</script>

<style scoped>
.basic-info-tab {
    min-height: 360px;
}

.tab-card {
    border-radius: 10px;
}

.card-title {
    font-weight: 600;
    color: #303133;
}

.description-text {
    min-height: 160px;
    color: #303133;
    font-size: 14px;
    line-height: 1.8;
}

.source-note {
    margin-top: 16px;
    color: #909399;
    font-size: 12px;
}
</style>
