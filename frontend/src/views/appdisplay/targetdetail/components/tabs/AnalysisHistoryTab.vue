<template>
    <div class="analysis-history-tab">
        <el-form :model="query" inline class="history-query-form">
            <el-form-item label="场景">
                <el-select v-model="query.sceneCode" clearable placeholder="全部场景" style="width: 150px" @change="handleQueryChange">
                    <el-option label="行情分析" value="MARKET" />
                    <el-option label="估值分析" value="VALUATION" />
                    <el-option label="财务分析" value="FINANCIAL" />
                    <el-option label="资金流向" value="CAPITAL_FLOW" />
                    <el-option label="风险预警" value="RISK" />
                    <el-option label="事件分析" value="EVENT" />
                    <el-option label="自定义" value="CUSTOM" />
                </el-select>
            </el-form-item>

            <el-form-item label="结论">
                <el-select v-model="query.conclusion" clearable placeholder="全部结论" style="width: 150px" @change="handleQueryChange">
                    <el-option label="强烈偏多" value="STRONGLY_BULLISH" />
                    <el-option label="偏多" value="BULLISH" />
                    <el-option label="中性" value="NEUTRAL" />
                    <el-option label="偏空" value="BEARISH" />
                    <el-option label="强烈偏空" value="STRONGLY_BEARISH" />
                </el-select>
            </el-form-item>

            <el-form-item label="风险">
                <el-select v-model="query.riskLevel" clearable placeholder="全部风险" style="width: 130px" @change="handleQueryChange">
                    <el-option label="低" value="LOW" />
                    <el-option label="中" value="MEDIUM" />
                    <el-option label="高" value="HIGH" />
                    <el-option label="极高" value="EXTREME" />
                </el-select>
            </el-form-item>
        </el-form>

        <el-table :data="records" v-loading="loading" border>
            <el-table-column prop="analysisTime" label="分析时间" width="170" align="center" />
            <el-table-column prop="sceneName" label="场景" width="120" align="center" />
            <el-table-column label="结论" width="110" align="center">
                <template #default="{ row }">
                    <el-tag :type="statusTagType(row.conclusion)">
                        {{ row.conclusionText || overallViewText(row.conclusion) }}
                    </el-tag>
                </template>
            </el-table-column>
            <el-table-column label="信号强度" width="110" align="center">
                <template #default="{ row }">{{ signalStrengthText(row.signalStrength) }}</template>
            </el-table-column>
            <el-table-column label="风险等级" width="110" align="center">
                <template #default="{ row }">
                    <el-tag :type="statusTagType(row.riskLevel)">
                        {{ riskLevelText(row.riskLevel) }}
                    </el-tag>
                </template>
            </el-table-column>
            <el-table-column prop="summary" label="摘要" min-width="260" />
            <el-table-column label="关键点" min-width="260">
                <template #default="{ row }">
                    <el-tag
                        v-for="point in row.keyPoints"
                        :key="point"
                        class="point-tag"
                        size="small"
                        effect="plain"
                    >
                        {{ point }}
                    </el-tag>
                </template>
            </el-table-column>
            <el-table-column prop="modelVersion" label="版本" width="100" align="center" />
        </el-table>

        <div class="pagination-wrap">
            <el-pagination
                :current-page="pageNo"
                :page-size="pageSize"
                :total="total"
                :page-sizes="[10, 20, 50, 100]"
                layout="total, sizes, prev, pager, next, jumper"
                @size-change="handleSizeChange"
                @current-change="handlePageChange"
            />
        </div>
    </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import type { AnalysisHistoryQuery, AnalysisRecord } from '/@/types/targetdetail/targetDetailCenter'
import {
    overallViewText,
    riskLevelText,
    signalStrengthText,
    statusTagType
} from '../../utils/targetDetailFormat'

const emit = defineEmits<{
    'query-change': [query: Partial<AnalysisHistoryQuery>]
    'page-change': [pagination: { pageNo: number; pageSize: number }]
}>()

const props = defineProps<{
    records: AnalysisRecord[]
    loading: boolean
    total: number
    pageNo: number
    pageSize: number
}>()

const query = reactive<Partial<AnalysisHistoryQuery>>({
    sceneCode: '',
    conclusion: '',
    riskLevel: ''
})

function handleQueryChange(): void {
    emit('query-change', {
        sceneCode: query.sceneCode,
        conclusion: query.conclusion,
        riskLevel: query.riskLevel,
        pageNo: 1,
        pageSize: props.pageSize
    })
}

function handleSizeChange(pageSize: number): void {
    emit('page-change', { pageNo: 1, pageSize })
}

function handlePageChange(pageNo: number): void {
    emit('page-change', { pageNo, pageSize: props.pageSize })
}
</script>

<style scoped>
.analysis-history-tab {
    min-height: 360px;
}

.history-query-form {
    margin-bottom: 12px;
}

.point-tag {
    margin: 2px 4px 2px 0;
}

.pagination-wrap {
    display: flex;
    justify-content: center;
    margin-top: 16px;
}
</style>
