<template>
    <div class="industry-tab">
        <el-row :gutter="16">
            <el-col :xs="24" :lg="12">
                <el-card class="tab-card" shadow="never">
                    <template #header>
                        <span class="card-title">行业 / 关系画像</span>
                    </template>

                    <template v-if="industry">
                        <el-descriptions :column="1" border>
                            <el-descriptions-item label="行业名称">{{ industry.industryName }}</el-descriptions-item>
                            <el-descriptions-item label="行业代码">{{ industry.industryCode }}</el-descriptions-item>
                            <el-descriptions-item label="行业层级">{{ industry.level ?? '-' }}</el-descriptions-item>
                            <el-descriptions-item label="上级行业">{{ industry.parentIndustryName || '-' }}</el-descriptions-item>
                        </el-descriptions>

                        <el-alert
                            v-if="industry.conclusion"
                            class="conclusion"
                            :title="industry.conclusion"
                            type="info"
                            show-icon
                            :closable="false"
                        />

                        <el-table
                            v-if="industry.industryRankings?.length"
                            :data="industry.industryRankings"
                            border
                            class="ranking-table"
                        >
                            <el-table-column prop="metricName" label="指标" min-width="120" />
                            <el-table-column prop="displayValue" label="排名" min-width="120" />
                            <el-table-column label="分位" min-width="100" align="right">
                                <template #default="{ row }">{{ formatPercent(row.percentile) }}</template>
                            </el-table-column>
                        </el-table>
                    </template>

                    <el-empty v-else description="暂无行业信息" />
                </el-card>
            </el-col>

            <el-col :xs="24" :lg="12">
                <el-card class="tab-card" shadow="never">
                    <template #header>
                        <span class="card-title">{{ target.targetType === 'INDEX' ? '指数成分' : '相关标的' }}</span>
                    </template>

                    <template v-if="target.targetType === 'INDEX' && constituents?.totalCount">
                        <div class="constituent-summary">
                            <div class="count-item">
                                <strong>{{ constituents.totalCount }}</strong>
                                <span>成分股</span>
                            </div>
                            <div class="count-item is-up">
                                <strong>{{ constituents.upCount || 0 }}</strong>
                                <span>上涨</span>
                            </div>
                            <div class="count-item is-down">
                                <strong>{{ constituents.downCount || 0 }}</strong>
                                <span>下跌</span>
                            </div>
                            <div class="count-item">
                                <strong>{{ constituents.flatCount || 0 }}</strong>
                                <span>平盘</span>
                            </div>
                        </div>

                        <el-table :data="constituents.topWeights || []" border class="ranking-table">
                            <el-table-column prop="targetName" label="前十大权重" min-width="120" />
                            <el-table-column prop="targetCode" label="代码" min-width="110" />
                            <el-table-column label="权重" width="90" align="right">
                                <template #default="{ row }">{{ formatPercent(row.weight) }}</template>
                            </el-table-column>
                            <el-table-column label="涨跌幅" width="100" align="right">
                                <template #default="{ row }">
                                    <span :class="signedClass(row.changePercent)">
                                        {{ formatPercent(row.changePercent, 2, true) }}
                                    </span>
                                </template>
                            </el-table-column>
                        </el-table>
                    </template>

                    <template v-else>
                        <el-table :data="relatedTargets" border>
                            <el-table-column prop="targetName" label="名称" min-width="120" />
                            <el-table-column prop="targetCode" label="代码" min-width="110" />
                            <el-table-column prop="relationName" label="关系" min-width="100" />
                            <el-table-column label="涨跌幅" width="100" align="right">
                                <template #default="{ row }">
                                    <span :class="signedClass(row.changePercent)">
                                        {{ formatPercent(row.changePercent, 2, true) }}
                                    </span>
                                </template>
                            </el-table-column>
                        </el-table>
                    </template>
                </el-card>
            </el-col>
        </el-row>

        <el-card v-if="fundInfo" class="tab-card fund-card" shadow="never">
            <template #header>
                <span class="card-title">基金 / ETF 信息</span>
            </template>
            <el-descriptions :column="3" border>
                <el-descriptions-item label="基金公司">{{ fundInfo.fundCompany || '-' }}</el-descriptions-item>
                <el-descriptions-item label="基金经理">{{ fundInfo.fundManager || '-' }}</el-descriptions-item>
                <el-descriptions-item label="成立日期">{{ fundInfo.inceptionDate || '-' }}</el-descriptions-item>
                <el-descriptions-item label="跟踪指数">{{ fundInfo.trackingIndexName || '-' }}</el-descriptions-item>
                <el-descriptions-item label="基金规模">{{ formatAmount(fundInfo.fundSize) }}</el-descriptions-item>
                <el-descriptions-item label="折溢价率">{{ formatPercent(fundInfo.premiumRate) }}</el-descriptions-item>
                <el-descriptions-item label="单位净值">{{ fundInfo.nav ?? '-' }}</el-descriptions-item>
                <el-descriptions-item label="累计净值">{{ fundInfo.accumulatedNav ?? '-' }}</el-descriptions-item>
                <el-descriptions-item label="跟踪误差">{{ formatPercent(fundInfo.trackingError) }}</el-descriptions-item>
            </el-descriptions>
        </el-card>
    </div>
</template>

<script setup lang="ts">
import type {
    ConstituentInfo,
    FundInfo,
    IndustryInfo,
    RelatedTarget,
    TargetProfile
} from '/@/types/targetdetail/targetDetailCenter'
import { formatAmount, formatPercent, signedClass } from '../../utils/targetDetailFormat'

defineProps<{
    target: TargetProfile
    industry?: IndustryInfo
    constituents?: ConstituentInfo
    fundInfo?: FundInfo
    relatedTargets: RelatedTarget[]
}>()
</script>

<style scoped>
.industry-tab {
    min-height: 360px;
}

.tab-card {
    border-radius: 10px;
}

.fund-card {
    margin-top: 16px;
}

.card-title {
    font-weight: 600;
    color: #303133;
}

.conclusion {
    margin-top: 16px;
}

.ranking-table {
    margin-top: 16px;
}

.constituent-summary {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 10px;
    margin-bottom: 16px;
}

.count-item {
    display: flex;
    flex-direction: column;
    gap: 5px;
    padding: 12px;
    text-align: center;
    background: #fafafa;
    border: 1px solid #ebeef5;
    border-radius: 8px;
}

.count-item strong {
    font-size: 20px;
}

.count-item span {
    font-size: 12px;
    color: #909399;
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
