
<template>
    <div class="data-topic-page">
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
                    :data="topicTree"
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

        <!-- 数据主题列表卡片 -->
        <el-card class="list-card" shadow="never">
            <template #header>
                <div class="card-header">
                    <div>
                        <span class="card-title">数据主题列表</span>
                    </div>
                    <el-button type="primary" @click="openCreateDialog">新增数据主题</el-button>
                </div>
            </template>

            <div class="content-panel">
                <el-form :model="queryForm" inline class="query-form">
                    <el-form-item label="主题名称">
                        <el-input
                            v-model="queryForm.dataTopicName"
                            clearable
                            placeholder="如 行情信息"
                            style="width: 180px"
                            @keyup.enter="handleSearch"
                        />
                    </el-form-item>

                    <el-form-item label="主题标签">
                        <el-input
                            v-model="queryForm.dataTopicLabel"
                            clearable
                            placeholder="如 PRICE_INFO"
                            style="width: 180px"
                            @keyup.enter="handleSearch"
                        />
                    </el-form-item>

                    <el-form-item class="query-actions">
                        <el-button type="primary" @click="handleSearch">查询</el-button>
                        <el-button @click="handleReset">重置</el-button>
                    </el-form-item>
                </el-form>

                <el-table
                    v-loading="tableLoading"
                    :data="tableData"
                    border
                    row-key="dataTopicCode"
                    class="topic-table"
                    height="calc(100vh - 330px)"
                >
                    <el-table-column prop="dataTopicCode" label="主题编码" min-width="150" align="center"/>
                    <el-table-column prop="dataTopicName" label="主题名称" min-width="150" align="center"/>
                    <el-table-column prop="dataTopicLabel" label="主题标签" min-width="200" align="center"/>
                    <el-table-column prop="sortNo" label="排序" width="90" align="center" />
                    <el-table-column prop="status" label="状态" width="100" align="center">
                        <template #default="{ row }">
                            <el-tag :type="DATA_TOPIC_STATUS_TAG_TYPE[row.status as DataTopicStatus]">
                                {{ DATA_TOPIC_STATUS_TEXT[row.status as DataTopicStatus] || row.status }}
                            </el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column prop="description" label="说明" min-width="220" show-overflow-tooltip align="center"/>
                    <el-table-column label="操作" width="210" fixed="right" align="center">
                        <template #default="{ row }">
                            <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
                            <!--el-button
                                link
                                :type="row.status === 'ENABLED' ? 'warning' : 'success'"
                                @click="toggleStatus(row)"
                            >
                                {{ row.status === 'ENABLED' ? '停用' : '启用' }}
                            </el-button-->
                            <el-button link type="danger" @click="handleDeleteTopic(row)">删除</el-button>
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
            </div>
        </el-card>

        <!-- 数据主题表单弹窗 -->
        <DataTopicFormDialog
            v-model="dialogVisible"
            :mode="dialogMode"
            :form-data="currentTopic"
            :parent-topic="clickedTopic"
            @success="handleSaveSuccess"
        />
    </div>
</template>



<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteDataTopic, queryDataTopicPage, queryDataTopicTree } from '/@/api/datatopic/dataTopicApi'
import { DATA_TOPIC_STATUS_TEXT, DATA_TOPIC_STATUS_TAG_TYPE } from '/@/types/datatopic/view'
import DataTopicFormDialog from './DataTopicFormDialog.vue'
import { DataTopicStatus } from '/@/types/datatopic/constants'
import { DataTopic, createDefaultDataTopic } from '/@/types/datatopic/entity'

/**
 * 查询表单类型
 */
interface QueryForm {
    topicNameForTree: string
    dataTopicName: string
    dataTopicLabel: string
    parentCode: string
}

/**
 * 分页状态类型
 */
interface PaginationState {
    pageNo: number
    pageSize: number
    total: number
}

const treeProps = {
    label: 'dataTopicName',
    children: 'children'
}

const topicTree = ref<DataTopic[]>([])
const tableData = ref<DataTopic[]>([])
const treeLoading = ref(false)
const tableLoading = ref(false)
const expandNodes = ref<string[]>([]) // 需要展开的树节点列表

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
// 构建一个默认的DataTopic
const currentTopic = ref<DataTopic>(createDefaultDataTopic())
const clickedTopic = ref<DataTopic>(createDefaultDataTopic())

// 条件查询表单
const queryForm = reactive<QueryForm>({
    parentCode: '', // 所点击的树节点作为父节点，查询子节点列表
    dataTopicName: '',
    dataTopicLabel: '',
    topicNameForTree: '' // 用于根据主题名称过滤树节点，前端实现简单的模糊搜索功能
})

const pagination = reactive<PaginationState>({
    pageNo: 1,
    pageSize: 20,
    total: 0
})

/**
 * 加载主题树
 */
async function loadTree(): Promise<void> {
    treeLoading.value = true
    try {
        topicTree.value = await queryDataTopicTree({})
        expandNodes.value = collectLevelNodeCodes(topicTree.value, 2)
    } finally {
        treeLoading.value = false
    }
}

/**
 * 加载主题表格
 */
async function loadTable(): Promise<void> {
    tableLoading.value = true
    try {
        const result = await queryDataTopicPage({
            pageNo: pagination.pageNo,
            pageSize: pagination.pageSize,
            parentCode: queryForm.parentCode || undefined,
            dataTopicName: queryForm.dataTopicName || undefined,
            dataTopicLabel: queryForm.dataTopicLabel || undefined,
        })

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
        topicTree.value = filterTree(topicTree.value)
    } else {
        loadTree()
    }
}

/**
 * 查询按钮点击
 */
function handleSearch(): void {
    if (!queryForm.parentCode) {
        ElMessage.warning('请先选择左侧数据主题节点')
        return
    }
    pagination.pageNo = 1
    loadTable()
}

/**
 * 重置按钮点击
 */
function handleReset(): void {
    queryForm.dataTopicName = ''
    queryForm.dataTopicLabel = ''
    queryForm.parentCode = clickedTopic.value.dataTopicCode || ''
    pagination.pageNo = 1
    loadTable()
}

/**
 * 树节点点击
 */
function handleTreeNodeClick(node: DataTopic): void {
    queryForm.parentCode = node.dataTopicCode
    pagination.pageNo = 1
    clickedTopic.value = node
    loadTable()
}

/**
 * 打开新增弹窗
 */
function openCreateDialog(): void {
    if (!clickedTopic.value.dataTopicCode) {
        ElMessage.warning('请先选择左侧数据主题节点，确定新增数据主题的父节点')
        return
    }
    dialogMode.value = 'create'
    currentTopic.value = createDefaultDataTopic()
    currentTopic.value.parentCode = clickedTopic.value.dataTopicCode
    dialogVisible.value = true
}

/**
 * 打开编辑弹窗
 */
function openEditDialog(row: DataTopic): void {
    dialogMode.value = 'edit'
    currentTopic.value = { ...row }
    dialogVisible.value = true
}

/**
 * 删除主题
 */
async function handleDeleteTopic(row: DataTopic): Promise<void> {
    try {
        await ElMessageBox.confirm(
            `确认删除数据主题「${row.dataTopicName}」？`,
            '删除确认',
            {
                type: 'warning',
                confirmButtonText: '删除',
                cancelButtonText: '取消'
            }
        )

        await deleteDataTopic(row.dataTopicCode)

        ElMessage.success('删除成功')
        await handleSaveSuccess()
    } catch (error) {
        // 取消删除或后端校验失败时直接结束，错误提示由全局拦截器展示
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

/**
 * 保存成功后刷新
 */
async function handleSaveSuccess(): Promise<void> {
    dialogVisible.value = false
    await Promise.all([loadTree(), loadTable()])
}

onMounted(() => {
    loadTree()
    loadTable()
})
</script>

<style scoped>
.data-topic-page {
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
    font-size: 16px;
    font-weight: 600;
    color: #303133;
}

.card-tip {
    margin: 6px 0 0;
    font-size: 12px;
    line-height: 18px;
    color: #909399;
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

.topic-table {
    flex: 1;
    min-height: 360px;
}

.pagination-wrap {
    display: flex;
    justify-content:center;
    padding-top: 16px;
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
    .data-topic-page {
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

    .topic-table {
        height: 420px;
    }
}
</style>