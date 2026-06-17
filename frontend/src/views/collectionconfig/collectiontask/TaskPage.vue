<template>
    <div class="task-page">
        <el-card shadow="never">
            <template #header>
                <div class="card-header">
                    <span class="card-title">采集任务管理</span>
                    <div class="header-actions">
                        <el-button type="primary" @click="handleSync">手动同步模板</el-button>
                    </div>
                </div>
            </template>

            <!-- 查询条件 -->
            <el-form :model="queryForm" inline class="query-form">
                <el-form-item label="任务编码">
                    <el-input
                        v-model="queryForm.taskCode"
                        clearable
                        placeholder="请输入任务编码"
                        style="width: 180px"
                    />
                </el-form-item>
                <el-form-item label="任务名称">
                    <el-input
                        v-model="queryForm.taskName"
                        clearable
                        placeholder="请输入任务名称"
                        style="width: 180px"
                    />
                </el-form-item>
                <el-form-item label="配置状态">
                    <el-select v-model="queryForm.configStatus" clearable style="width: 130px">
                        <el-option
                            v-for="(label, val) in CONFIG_STATUS_TEXT"
                            :key="val"
                            :label="label"
                            :value="val"
                        />
                    </el-select>
                </el-form-item>
                <el-form-item label="启停状态">
                    <el-select v-model="queryForm.status" clearable style="width: 110px">
                        <el-option label="启用" value="ENABLED" />
                        <el-option label="停用" value="DISABLED" />
                    </el-select>
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" @click="handleSearch">查询</el-button>
                    <el-button @click="handleReset">重置</el-button>
                </el-form-item>
            </el-form>

            <!-- 任务列表 -->
            <el-table
                v-loading="tableLoading"
                :data="tableData"
                border
                height="calc(100vh - 300px)"
            >
                <el-table-column prop="taskCode"        label="任务编码"   min-width="160" />
                <el-table-column prop="taskName"        label="任务名称"   min-width="160" />
                <el-table-column prop="currentVersionNo" label="当前版本"  width="90" align="center" />
                <el-table-column prop="syncStatus"      label="同步状态"   width="110" align="center">
                    <template #default="{ row }">
                        <el-tag :type="SYNC_STATUS_TAG_TYPE[row.syncStatus as SyncStatus]">
                            {{ SYNC_STATUS_TEXT[row.syncStatus as SyncStatus] || row.syncStatus }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="configStatus"    label="配置状态"   width="100" align="center">
                    <template #default="{ row }">
                        <el-tag v-if="row.configStatus" :type="CONFIG_STATUS_TAG_TYPE[row.configStatus as ConfigStatus]">
                            {{ CONFIG_STATUS_TEXT[row.configStatus as ConfigStatus] || row.configStatus }}
                        </el-tag>
                        <span v-else class="text-gray">未配置</span>
                    </template>
                </el-table-column>
                <el-table-column prop="status"          label="启停状态"   width="90" align="center">
                    <template #default="{ row }">
                        <el-tag :type="COMMON_STATUS_TAG_TYPE[row.status]">
                            {{ COMMON_STATUS_TEXT[row.status] || row.status }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="latestSyncTime"  label="最近同步"   min-width="170" />
                <el-table-column label="操作" width="270" fixed="right" align="center">
                    <template #default="{ row }">
                        <el-button link type="primary" @click="goDetail(row)">详情</el-button>
                        <el-button link type="primary" @click="goConfig(row)">配置</el-button>
                        <el-button v-if="row.syncStatus !== 'FAILED'" link :type="row.status === 'ENABLED' ? 'warning' : 'success'" @click="handleToggleStatus(row)">
                            {{ row.status === 'ENABLED' ? '停用' : '启用' }}
                        </el-button>
                        <el-button v-if="row.syncStatus === 'FAILED'" link type="danger" @click="deleteTask(row)">删除</el-button>
                    </template>
                </el-table-column>
            </el-table>

            <!-- 分页 -->
            <div class="pagination-wrap">
                <el-pagination
                    v-model:current-page="pagination.pageNo"
                    v-model:page-size="pagination.pageSize"
                    :total="pagination.total"
                    :page-sizes="[10, 20, 50]"
                    layout="total, sizes, prev, pager, next, jumper"
                    @size-change="loadTable"
                    @current-change="loadTable"
                />
            </div>
        </el-card>

        <!-- 任务配置弹窗 -->
        <TaskConfigFormDialog
            v-model="configDialogVisible"
            :task-template="currentTaskTemplate"
            :existing-config="currentConfig"
            @success="handleSaveSuccess"
        />
    </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'

import type { TaskTemplate, TaskConfig } from '/@/types/collectiontask/entity'
import type { SyncStatus, ConfigStatus } from '/@/types/collectiontask/constants'
import {
    SYNC_STATUS_TEXT, SYNC_STATUS_TAG_TYPE,
    CONFIG_STATUS_TEXT, CONFIG_STATUS_TAG_TYPE,
    COMMON_STATUS_TEXT, COMMON_STATUS_TAG_TYPE
} from '/@/types/collectiontask/view'
import type { TaskQueryForm } from '/@/types/collectiontask/view'
import { queryTaskList, syncTaskTemplate, deleteTaskTemplate, updateTaskStatus } from '/@/api/collectiontask/collectionTaskApi'

import TaskConfigFormDialog from './TaskConfigFormDialog.vue'

const router = useRouter()

const tableLoading = ref(false)
const tableData    = ref<TaskTemplate[]>([])
const pagination   = reactive({ pageNo: 1, pageSize: 20, total: 0 })

const queryForm = reactive<TaskQueryForm>({
    taskCode:    '',
    taskName:    '',
    dataTopicCode: '',
    dataTypeCode: '',
    configStatus: '',
    status:      ''
})

const configDialogVisible = ref(false)
const currentTaskTemplate = ref<TaskTemplate>({} as TaskTemplate)
const currentConfig = ref<TaskConfig | null>(null)

onMounted(() => {
    loadTable()
})

/** 加载任务列表 */
async function loadTable(): Promise<void> {
    tableLoading.value = true
    try {
        const result = await queryTaskList({
            taskCode:      queryForm.taskCode || undefined,
            taskName:      queryForm.taskName || undefined,
            pageNo:        pagination.pageNo,
            pageSize:      pagination.pageSize
        })
        tableData.value    = result.records || []
        pagination.total   = result.total   || 0
    } finally {
        tableLoading.value = false
    }
}

function handleSearch(): void {
    pagination.pageNo = 1
    loadTable()
}

function handleReset(): void {
    queryForm.taskCode     = ''
    queryForm.taskName     = ''
    queryForm.configStatus = ''
    queryForm.status       = ''
    pagination.pageNo      = 1
    loadTable()
}

/** 跳转到任务详情页 */
function goDetail(row: TaskTemplate): void {
    router.push(`/collectionTask/detail/${row.taskCode}`)
}

function goConfig(row: TaskTemplate): void {
    router.push(`/collectionTask/config/${row.taskCode}`)
}

async function deleteTask(row: TaskTemplate): Promise<void> {
    // 需要先判断是否存在配置信息，如果存在则提示需要先删除配置信息才能删除任务
    if (row.configStatus) {
        ElMessage.warning('请先删除任务配置，才能删除任务')
        return
    }
    await ElMessageBox.confirm(`确认删除任务「${row.taskName}」？`, '删除确认', {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText:  '取消'
    }).then(async () => {
        await deleteTaskTemplate(row.taskCode)
        ElMessage.success('删除成功')
        await loadTable()
    }).catch(() => {
        // 取消删除
    })
}

/** 变更启停状态 */
async function handleToggleStatus(row: TaskTemplate): Promise<void> {
    const isEnable = row.status !== 'ENABLED'
    const action   = isEnable ? '启用' : '停用'
    if (isEnable && row.configStatus !== 'CONFIRMED') {
        ElMessage.warning('请先确认配置后再启用任务')
        return
    }
    await ElMessageBox.confirm(`确认${action}任务「${row.taskName}」？`, '操作确认', {
        type: 'warning',
        confirmButtonText: action,
        cancelButtonText:  '取消'
    })

    await updateTaskStatus(row.taskCode, { status: isEnable ? 'ENABLED' : 'DISABLED' })
    ElMessage.success(`${action}成功`)
    await loadTable()
}

/** 手动同步任务模板 */
async function handleSync(): Promise<void> {
    await ElMessageBox.confirm('确认同步所有任务模板？', '同步确认', {
        type: 'info',
        confirmButtonText: '同步',
        cancelButtonText:  '取消'
    }).then(async () => {
        await syncTaskTemplate({ syncMode: 'MANUAL' })
        ElMessage.success('同步完成')
        await loadTable()
    }).catch(() => {
        return
    })
}

async function handleSaveSuccess(): Promise<void> {
    configDialogVisible.value = false
    await loadTable()
}
</script>

<style scoped>
.task-page {
    padding: 16px;
}

.card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.card-title {
    font-weight: 600;
    font-size: 16px;
}

.header-actions {
    display: flex;
    gap: 8px;
}

.query-form {
    margin-bottom: 12px;
}

.pagination-wrap {
    display: flex;
    justify-content: center;
    margin-top: 12px;
}

.text-gray {
    color: #909399;
    font-size: 12px;
}
</style>
