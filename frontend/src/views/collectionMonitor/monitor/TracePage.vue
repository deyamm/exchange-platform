<template>
    <div class="trace-page">
        <el-card class="list-card" shadow="never">
            <template #header>
                <div class="card-header"><span class="card-title">历史追溯</span></div>
            </template>

            <el-form :model="queryForm" inline class="query-form">
                <el-form-item label="执行编号">
                    <el-input v-model="queryForm.runId" clearable placeholder="请输入 runId" style="width: 260px" @keyup.enter="handleQuery" />
                </el-form-item>
                <el-form-item class="query-actions">
                    <el-button type="primary" @click="handleQuery">追溯</el-button>
                </el-form-item>
            </el-form>

            <div v-loading="loading" class="trace-body">
                <template v-if="trace">
                    <!-- 执行记录 -->
                    <el-descriptions title="执行记录" :column="2" border class="trace-block">
                        <el-descriptions-item label="执行编号">{{ trace.taskRun.runId }}</el-descriptions-item>
                        <el-descriptions-item label="任务编码">{{ trace.taskRun.taskCode }}</el-descriptions-item>
                        <el-descriptions-item label="执行状态">
                            <el-tag :type="RUN_STATUS_TAG_TYPE[trace.taskRun.runStatus]">{{ RUN_STATUS_TEXT[trace.taskRun.runStatus] }}</el-tag>
                        </el-descriptions-item>
                        <el-descriptions-item label="数据集编码">{{ trace.taskRun.datasetCode || '-' }}</el-descriptions-item>
                    </el-descriptions>

                    <!-- 执行配置快照 -->
                    <el-descriptions v-if="trace.snapshot" title="执行配置快照" :column="2" border class="trace-block">
                        <el-descriptions-item label="任务模板版本">{{ trace.snapshot.taskTemplateVersionNo }}</el-descriptions-item>
                        <el-descriptions-item label="数据类型版本">{{ trace.snapshot.dataTypeCode }} / v{{ trace.snapshot.dataTypeVersionNo }}</el-descriptions-item>
                        <el-descriptions-item label="存储映射">{{ trace.snapshot.storageMappingCode || '-' }}</el-descriptions-item>
                        <el-descriptions-item label="执行参数">{{ trace.snapshot.executionParamsJson || '-' }}</el-descriptions-item>
                    </el-descriptions>

                    <!-- 结果摘要 -->
                    <el-descriptions v-if="trace.resultSummary" title="结果摘要" :column="2" border class="trace-block">
                        <el-descriptions-item label="处理条数">{{ trace.resultSummary.totalCount }}</el-descriptions-item>
                        <el-descriptions-item label="成功条数">{{ trace.resultSummary.successCount }}</el-descriptions-item>
                        <el-descriptions-item label="异常条数">{{ trace.resultSummary.failureCount }}</el-descriptions-item>
                        <el-descriptions-item label="结果定位">{{ trace.resultSummary.resultLocation || '-' }}</el-descriptions-item>
                    </el-descriptions>

                    <!-- 运行留痕索引 -->
                    <div class="trace-block">
                        <div class="block-title">运行留痕</div>
                        <el-tag class="count-tag" type="info">日志 {{ trace.runLogs?.length || 0 }}</el-tag>
                        <el-tag class="count-tag" type="danger">异常 {{ trace.exceptionRecords?.length || 0 }}</el-tag>
                        <el-tag class="count-tag" type="warning">问题数据 {{ trace.problemRecords?.length || 0 }}</el-tag>
                    </div>
                </template>
                <el-empty v-else description="请输入执行编号进行追溯" />
            </div>
        </el-card>
    </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getTrace } from '/@/api/monitor/queryApi'
import { RUN_STATUS_TEXT, RUN_STATUS_TAG_TYPE } from '/@/types/execution/view'
import type { Trace } from '/@/types/monitor/entity'

const loading = ref(false)
const trace = ref<Trace | null>(null)
const queryForm = reactive({ runId: '' })

/** 按 runId 聚合查询历史执行主线 */
async function handleQuery(): Promise<void> {
    if (!queryForm.runId.trim()) {
        ElMessage.warning('请输入执行编号')
        return
    }
    loading.value = true
    try {
        trace.value = await getTrace(queryForm.runId.trim())
    } finally {
        loading.value = false
    }
}
</script>

<style scoped>
.trace-page { height: calc(100vh - 90px); padding: 8px; background: #f5f7fa; box-sizing: border-box; }
.list-card { height: 100%; border-radius: 10px; overflow: hidden; }
.card-header { display: flex; align-items: center; justify-content: space-between; }
.card-title { font-size: 16px; font-weight: 600; color: #303133; }
.query-form {
    display: flex; flex-wrap: wrap; padding: 16px 16px 4px; margin-bottom: 12px;
    background: #fafafa; border: 1px solid #ebeef5; border-radius: 8px;
}
.query-actions { margin-left: auto; }
.trace-body { height: calc(100vh - 260px); overflow: auto; }
.trace-block { margin-bottom: 20px; }
.block-title { font-size: 15px; font-weight: 600; color: #303133; margin-bottom: 10px; }
.count-tag { margin-right: 10px; }
:deep(.el-table th.el-table__cell) { background: #f7f8fa; color: #606266; font-weight: 600; }
</style>
