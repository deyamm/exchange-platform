<template>
    <div class="execution-page">
        <el-card class="list-card" shadow="never">
            <template #header>
                <div class="card-header">
                    <span class="card-title">任务执行</span>
                    <el-button type="primary" @click="openCreateDialog">新建执行</el-button>
                </div>
            </template>

            <!-- 条件查询 -->
            <el-form :model="queryForm" inline class="query-form">
                <el-form-item label="任务编码">
                    <el-input v-model="queryForm.taskCode" clearable placeholder="任务编码" style="width: 180px" @keyup.enter="handleSearch" />
                </el-form-item>
                <el-form-item label="执行状态">
                    <el-select v-model="queryForm.runStatus" clearable placeholder="全部" style="width: 140px">
                        <el-option v-for="(text, value) in RUN_STATUS_TEXT" :key="value" :label="text" :value="value" />
                    </el-select>
                </el-form-item>
                <el-form-item class="query-actions">
                    <el-button type="primary" @click="handleSearch">查询</el-button>
                    <el-button @click="handleReset">重置</el-button>
                    <el-button @click="loadTable">刷新</el-button>
                </el-form-item>
            </el-form>

            <el-table v-loading="tableLoading" :data="tableData" border height="calc(100vh - 300px)">
                <el-table-column prop="runId" label="执行编号" min-width="200" align="center" show-overflow-tooltip />
                <el-table-column prop="taskCode" label="任务编码" min-width="160" align="center" />
                <el-table-column label="触发方式" width="100" align="center">
                    <template #default="{ row }">{{ row.triggerType ? TRIGGER_TYPE_TEXT[row.triggerType as TriggerType] : '-' }}</template>
                </el-table-column>
                <el-table-column label="执行状态" width="110" align="center">
                    <template #default="{ row }">
                        <el-tag :type="RUN_STATUS_TAG_TYPE[row.runStatus as RunStatus]">{{ RUN_STATUS_TEXT[row.runStatus as RunStatus] || row.runStatus }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="startTime" label="开始时间" min-width="170" align="center" />
                <el-table-column prop="endTime" label="结束时间" min-width="170" align="center" />
                <el-table-column prop="runInfo" label="执行信息" min-width="200" align="center" show-overflow-tooltip />
                <el-table-column prop="schedulerId" label="调度编号" min-width="160" align="center" show-overflow-tooltip />
                <el-table-column prop="scheduledFireTime" label="调度触发时间" min-width="170" align="center" />
                <el-table-column label="操作" width="230" fixed="right" align="center">
                    <template #default="{ row }">
                        <el-button v-if="row.runStatus === 'CREATED'" link type="success"  @click="handleExecute(row)">触发执行</el-button>
                        <el-button v-if="row.runStatus === 'FAILED'" link type="warning" @click="handleExecute(row)">重新执行</el-button>
                        <el-button link type="primary" @click="openDetail(row)">详情</el-button>
                        <el-button link type="danger" :disabled="row.runStatus === 'RUNNING'" @click="handleDelete(row)">删除</el-button>
                    </template>
                </el-table-column>
            </el-table>

            <div class="pagination-wrap">
                <el-pagination
                    v-model:current-page="pagination.pageNo"
                    v-model:page-size="pagination.pageSize"
                    :total="pagination.total"
                    :page-sizes="[10, 20, 50, 100]"
                    layout="total, sizes, prev, pager, next, jumper"
                    @size-change="loadTable"
                    @current-change="loadTable"
                />
            </div>
        </el-card>

        <!-- 执行详情抽屉 -->
        <el-drawer v-model="detailVisible" title="执行记录详情" size="46%">
            <el-descriptions v-if="detail" :column="1" border>
                <el-descriptions-item label="执行编号">{{ detail.taskRun.runId }}</el-descriptions-item>
                <el-descriptions-item label="任务编码">{{ detail.taskRun.taskCode }}</el-descriptions-item>
                <el-descriptions-item label="调度编号">{{ detail.taskRun.schedulerId || '-' }}</el-descriptions-item>
                <el-descriptions-item label="执行状态">
                    <el-tag :type="RUN_STATUS_TAG_TYPE[detail.taskRun.runStatus as RunStatus]">{{ RUN_STATUS_TEXT[detail.taskRun.runStatus as RunStatus] }}</el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="调度触发时间">{{ detail.taskRun.scheduledFireTime || '-' }}</el-descriptions-item>
                <el-descriptions-item v-if="detail.resultSummary" label="处理/成功/异常">
                    {{ detail.resultSummary.totalCount }} / {{ detail.resultSummary.successCount }} / {{ detail.resultSummary.failureCount }}
                </el-descriptions-item>
                <el-descriptions-item v-if="detail.resultSummary" label="结果定位">{{ detail.resultSummary.resultLocation || '-' }}</el-descriptions-item>
                <el-descriptions-item v-if="detail.taskRun.runInfo" label="执行信息">{{ detail.taskRun.runInfo }}</el-descriptions-item>
            </el-descriptions>
        </el-drawer>

        <TaskRunCreateDialog v-model:visible="createDialogVisible" @created="handleCreated" />
    </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { queryTaskRuns, executeTaskRun, getTaskRunDetail, deleteTaskRun } from '/@/api/execution/executionApi'
import { RUN_STATUS_TEXT, RUN_STATUS_TAG_TYPE, TRIGGER_TYPE_TEXT } from '/@/types/execution/view'
import type { TaskRun, TaskRunDetail } from '/@/types/execution/entity'
import type { RunStatus, TriggerType } from '/@/types/execution/constants'
import TaskRunCreateDialog from './TaskRunCreateDialog.vue'

const tableData = ref<TaskRun[]>([])
const tableLoading = ref(false)
const detailVisible = ref(false)
const detail = ref<TaskRunDetail | null>(null)
const createDialogVisible = ref(false)

const queryForm = reactive({
    taskCode: '',
    runStatus: '' as RunStatus | ''
})

const pagination = reactive({ pageNo: 1, pageSize: 20, total: 0 })

/** 加载执行记录列表 */
async function loadTable(): Promise<void> {
    tableLoading.value = true
    try {
        const result = await queryTaskRuns({
            pageNo: pagination.pageNo,
            pageSize: pagination.pageSize,
            taskCode: queryForm.taskCode || undefined,
            runStatus: queryForm.runStatus || undefined,
        })
        tableData.value = result.records || []
        pagination.total = result.total || 0
    } finally {
        tableLoading.value = false
    }
}

function openCreateDialog(): void {
    createDialogVisible.value = true
}

function handleSearch(): void {
    pagination.pageNo = 1
    loadTable()
}

function handleReset(): void {
    queryForm.taskCode = ''
    queryForm.runStatus = ''
    handleSearch()
}

function handleCreated(): void {
    loadTable()
}

/** 触发执行 */
async function handleExecute(row: TaskRun): Promise<void> {
    await ElMessageBox.confirm('确定要触发执行吗？', '执行提示', {
        type: 'warning',
        confirmButtonText: '执行',
        cancelButtonText: '取消'
    }).then(async () => {
        await executeTaskRun(row.runId)
        ElMessage.success('已触发执行')
        loadTable()
    }).catch(() => {
        // 取消执行
    })
}

/** 删除任务 */
async function handleDelete(row: TaskRun): Promise<void> {
    await ElMessageBox.confirm('确定删除该执行记录吗？此操作不可恢复。', '删除提示', {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText:  '取消'
    }).then(async () => {
        await deleteTaskRun(row.runId)
        ElMessage.success('已删除执行记录')
        loadTable()
    }).catch(() => {
        // 取消删除
    })
}

/** 查看详情 */
async function openDetail(row: TaskRun): Promise<void> {
    detail.value = await getTaskRunDetail(row.runId)
    detailVisible.value = true
}

onMounted(() => loadTable())
</script>

<style scoped>
.execution-page {
    height: calc(100vh - 90px);
    padding: 8px;
    background: #f5f7fa;
    box-sizing: border-box;
}

.list-card { 
    height: 100%; 
    border-radius: 10px; 
    overflow: hidden; 
}

.card-header { 
    display: flex; 
    align-items: center; 
    
    justify-content: space-between; 
}
.card-title { 
    font-size: 16px; 
    font-weight: 600; 
    color: #303133; 
}

.query-form {
    display: flex; 
    flex-wrap: wrap; 
    padding: 16px 16px 4px; 
    margin-bottom: 12px;
    background: #fafafa; 
    border: 1px solid #ebeef5; 
    border-radius: 8px;
}

.query-actions { 
    margin-left: auto; 
}

.pagination-wrap { 
    display: flex; 
    justify-content: center; 
    padding-top: 16px; 
}

:deep(.el-table th.el-table__cell) { 
    background: #f7f8fa; 
    color: #606266; 
    font-weight: 600; 
}
</style>
