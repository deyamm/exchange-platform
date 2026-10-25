<template>
    <div class="task-page">
        <!-- 数据主题目录树卡片 -->
        <el-card class="tree-card" shadow="never">
            <template #header>
                <div class="card-header compact">
                    <span class="card-title">数据主题目录</span>
                </div>
            </template>

            <div class="tree-search">
                <el-input
                    v-model="queryForm.topicNameForTree"
                    clearable
                    placeholder="请输入主题名称查询"
                    @input="handleTreeSearch"
                />
            </div>

            <div class="tree-body">
                <el-tree
                    v-loading="treeLoading"
                    :data="dataTopicTree"
                    node-key="dataTopicCode"
                    :props="treeProps"
                    :default-expanded-keys="expandNodes"
                    empty-text="暂无数据主题"
                    highlight-current
                    class="topic-tree"
                    :expand-on-click-node="false"
                    @node-click="handleTreeNodeClick"
                >
                    <template #default="{ data }">
                        <span class="tree-node">
                            <span class="tree-node-name">{{ data.dataTopicName }}</span>
                            <el-tag
                                v-if="data.dataTopicCode !== NO_CONFIG_TOPIC_CODE"
                                size="small"
                                :type="DATA_TOPIC_STATUS_TAG_TYPE[data.status as DataTopicStatus]"
                                effect="plain"
                            >
                                {{ DATA_TOPIC_STATUS_TEXT[data.status as DataTopicStatus] || data.status }}
                            </el-tag>
                        </span>
                    </template>
                </el-tree>
            </div>
        </el-card>

        <!-- 采集任务列表卡片 -->
        <el-card class="list-card" shadow="never">
            <div class="content-panel">
                <!-- 查询条件 -->
                <el-form :model="queryForm" inline class="query-form">
                    <el-form-item label="任务编码">
                        <el-input
                            v-model="queryForm.taskCode"
                            clearable
                            placeholder="请输入任务编码"
                            style="width: 120px"
                        />
                    </el-form-item>
                    <el-form-item label="任务名称">
                        <el-input
                            v-model="queryForm.taskName"
                            clearable
                            placeholder="请输入任务名称"
                            style="width: 120px"
                        />
                    </el-form-item>
                    <el-form-item label="配置状态">
                        <el-select v-model="queryForm.configStatus" clearable style="width: 100px">
                            <el-option
                                v-for="(label, val) in CONFIG_STATUS_TEXT"
                                :key="val"
                                :label="label"
                                :value="val"
                            />
                        </el-select>
                    </el-form-item>
                    <el-form-item label="启停状态">
                        <el-select v-model="queryForm.status" clearable style="width: 90px">
                            <el-option label="启用" value="ENABLED" />
                            <el-option label="停用" value="DISABLED" />
                        </el-select>
                    </el-form-item>
                    <el-form-item class="query-actions">
                        <el-button type="primary" @click="handleSearch">查询</el-button>
                        <el-button @click="handleReset">重置</el-button>
                        <el-button type="primary" @click="handleSync">同步模板</el-button>
                    </el-form-item>
                </el-form>

                <!-- 任务列表 -->
                <el-table
                    v-loading="tableLoading"
                    :data="tableData"
                    border
                    height="calc(100vh - 330px)"
                    class="task-table"
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

import { DataTopic, createDefaultDataTopic } from '/@/types/datatopic/entity'
import { DataTopicStatus } from '/@/types/datatopic/constants'
import { queryDataTopicTree } from '/@/api/datatopic/dataTopicApi'
import { DATA_TOPIC_STATUS_TEXT, DATA_TOPIC_STATUS_TAG_TYPE } from '/@/types/datatopic/view'
import type { TaskTemplate, TaskConfig } from '/@/types/collectiontask/entity'
import type { SyncStatus, ConfigStatus, TaskQueryParams } from '/@/types/collectiontask/constants'
import {
    SYNC_STATUS_TEXT, SYNC_STATUS_TAG_TYPE,
    CONFIG_STATUS_TEXT, CONFIG_STATUS_TAG_TYPE,
    COMMON_STATUS_TEXT, COMMON_STATUS_TAG_TYPE
} from '/@/types/collectiontask/view'
import { queryTaskList, syncTaskTemplate, deleteTaskTemplate, updateTaskStatus } from '/@/api/collectiontask/collectionTaskApi'

import TaskConfigFormDialog from './TaskConfigFormDialog.vue'

const NO_CONFIG_TOPIC_CODE = 'noConfig'

const router = useRouter()

const treeProps = {
    label: 'dataTopicName',
    children: 'children'
}

const dataTopicTree = ref<DataTopic[]>([])
const expandNodes = ref<string[]>([])
const selectedTopic = ref<DataTopic | null>(null)
const treeLoading = ref(false)
const tableLoading = ref(false)
const tableData = ref<TaskTemplate[]>([])
const pagination = reactive({ pageNo: 1, pageSize: 20, total: 0 })

const queryForm = reactive({
    taskCode: '',
    taskName: '',
    dataTopicCode: '',
    dataTypeCode: '',
    configStatus: '',
    status: '',
    dataTopicCodePrefix: '',
    topicNameForTree: ''
})

const configDialogVisible = ref(false)
const currentTaskTemplate = ref<TaskTemplate>({} as TaskTemplate)
const currentConfig = ref<TaskConfig | null>(null)

onMounted(() => {
    loadTree()
    loadTable()
})

/** 加载数据主题树 */
async function loadTree(): Promise<void> {
    treeLoading.value = true
    try {
        const result = await queryDataTopicTree({})
        dataTopicTree.value = result || []
        const noConfigNode = createDefaultDataTopic()
        noConfigNode.dataTopicCode = NO_CONFIG_TOPIC_CODE
        noConfigNode.dataTopicName = '未配置数据主题'
        dataTopicTree.value.push(noConfigNode)
        expandNodes.value = collectLevelNodeCodes(dataTopicTree.value, 2)
    } finally {
        treeLoading.value = false
    }
}

/**
 * 收集 nodeLevel 为指定等级的节点编码
 */
function collectLevelNodeCodes(nodes: DataTopic[], level: number = 2): string[] {
    const result: string[] = []

    const walk = (list: DataTopic[]): void => {
        list.forEach(node => {
            if (Number(node.nodeLevel) === level && node.dataTopicCode) {
                result.push(node.dataTopicCode)
            }

            if (node.children && node.children.length > 0) {
                walk(node.children)
            }
        })
    }

    walk(nodes)
    return result
}

/** 加载任务列表 */
async function loadTable(): Promise<void> {
    tableLoading.value = true
    try {
        const params: TaskQueryParams = {
            taskCode: queryForm.taskCode || undefined,
            taskName: queryForm.taskName || undefined,
            dataTopicCodePrefix: queryForm.dataTopicCodePrefix || undefined,
            pageNo: pagination.pageNo,
            pageSize: pagination.pageSize
        }

        const result = await queryTaskList(params)
        tableData.value = result.records || []
        pagination.total = result.total || 0
    } finally {
        tableLoading.value = false
    }
}

/**
 * 主题树模糊搜索，目前还不需要通过接口实现后端模糊查询，前端简单实现即可
 */
function handleTreeSearch(): void {
    const keyword = queryForm.topicNameForTree.trim().toLocaleLowerCase()
    
    // 递归过滤树节点，保留匹配的节点及其父节点
    const filterTree = (nodes: DataTopic[]): DataTopic[] => {
        return nodes
            .map(node => {
                const match = node.dataTopicName.toLocaleLowerCase().includes(keyword)
                if (node.children) {
                    const filteredChildren = filterTree(node.children)
                    if (match || filteredChildren.length > 0) {
                        return { ...node, children: filteredChildren }
                    }
                } else if (match) {
                    return node
                }
                return null
            })
            .filter(node => node !== null) as DataTopic[]
    }

    if (keyword) {
        dataTopicTree.value = filterTree(dataTopicTree.value)
    } else {
        loadTree()
    }
}

function handleSearch(): void {
    pagination.pageNo = 1
    loadTable()
}

function handleReset(): void {
    queryForm.taskCode = ''
    queryForm.taskName = ''
    queryForm.configStatus = ''
    queryForm.status = ''
    queryForm.dataTypeCode = ''
    queryForm.dataTopicCode = selectedTopic.value?.dataTopicCode === NO_CONFIG_TOPIC_CODE
        ? ''
        : selectedTopic.value?.dataTopicCode || ''
    queryForm.dataTopicCodePrefix = selectedTopic.value?.dataTopicCode || ''
    pagination.pageNo = 1
    loadTable()
}

/** 选择数据主题后，加载该主题及其子主题下的采集任务 */
function handleTreeNodeClick(node: DataTopic): void {
    selectedTopic.value = node
    queryForm.dataTopicCode = node.dataTopicCode === NO_CONFIG_TOPIC_CODE ? '' : node.dataTopicCode
    queryForm.dataTopicCodePrefix = node.dataTopicCode
    pagination.pageNo = 1
    loadTable()
}


/** 跳转到任务详情页 */
function goDetail(row: TaskTemplate): void {
    router.push(`/collectionConfig/collectionTask/detail/${row.taskCode}`)
}

function goConfig(row: TaskTemplate): void {
    router.push(`/collectionConfig/collectionTask/config/${row.taskCode}`)
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
        cancelButtonText: '取消'
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
    const action = isEnable ? '启用' : '停用'
    if (isEnable && row.configStatus !== 'CONFIRMED') {
        ElMessage.warning('请先确认配置后再启用任务')
        return
    }
    await ElMessageBox.confirm(`确认${action}任务「${row.taskName}」？`, '操作确认', {
        type: 'warning',
        confirmButtonText: action,
        cancelButtonText: '取消'
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
        cancelButtonText: '取消'
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
    display: flex;
    gap: 16px;
    height: calc(100vh - 90px);
    min-height: 620px;
    padding: 8px;
    background: #f5f7fa;
    box-sizing: border-box;
}

.tree-card {
    flex: 0 0 320px;
    width: 320px;
    min-width: 280px;
    height: 100%;
    border-radius: 10px;
    overflow: hidden;
}

.list-card {
    flex: 1;
    min-width: 0;
    height: 100%;
    border-radius: 10px;
    overflow: hidden;
}

.card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
}

.card-header.compact {
    height: 24px;
}

.card-title {
    font-weight: 600;
    font-size: 16px;
    color: #303133;
}

.card-tip {
    margin: 6px 0 0;
    font-size: 12px;
    line-height: 18px;
    color: #909399;
}

.header-actions {
    display: flex;
    gap: 8px;
}

.tree-search {
    margin-bottom: 12px;
}

.tree-body {
    height: calc(100vh - 205px);
    overflow: auto;
    padding-right: 4px;
}

.topic-tree {
    --el-tree-node-hover-bg-color: #f0f6ff;
}

:deep(.topic-tree .el-tree-node__content) {
    height: 36px;
    border-radius: 6px;
    margin-bottom: 4px;
    padding-right: 8px;
}

:deep(.topic-tree .el-tree-node.is-current > .el-tree-node__content) {
    background: #ecf5ff;
    color: #409eff;
}

.tree-node {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    width: 100%;
    min-width: 0;
}

.tree-node-name {
    flex: 1;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.content-panel {
    display: flex;
    flex-direction: column;
    height: calc(100vh - 205px);
    min-height: 0;
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
    margin-right: 0;
}

.task-table {
    flex: 1;
    min-height: 360px;
}

.pagination-wrap {
    display: flex;
    justify-content: center;
    padding-top: 16px;
}

.text-gray {
    color: #909399;
    font-size: 12px;
}

:deep(.el-card__header) {
    padding: 14px 16px;
    border-bottom: 1px solid #ebeef5;
}

:deep(.el-card__body) {
    height: calc(100% - 57px);
    padding: 16px;
    box-sizing: border-box;
}

:deep(.el-table th.el-table__cell) {
    background: #f7f8fa;
    color: #606266;
    font-weight: 600;
}

@media (max-width: 1200px) {
    .task-page {
        flex-direction: column;
        height: auto;
        min-height: auto;
    }

    .tree-card {
        width: 100%;
        flex: none;
        height: 320px;
    }

    .list-card {
        height: auto;
        min-height: 620px;
    }

    .tree-body,
    .content-panel {
        height: auto;
    }

    .task-table {
        height: 420px;
    }
}
</style>