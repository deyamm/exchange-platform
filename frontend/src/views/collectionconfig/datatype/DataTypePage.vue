<template>
    <div class="data-type-page">
        <el-card class="tree-card" shadow="never">
            <template #header><div class="card-header compact"><span class="card-title">数据类型目录</span></div></template>
            <div class="tree-search">
                <el-input v-model="queryForm.typeNameForTree" clearable placeholder="请输入类型名称查询" @input="loadTree" />
            </div>
            <div class="tree-body">
                <el-tree
                    v-loading="treeLoading"
                    :data="typeTree"
                    node-key="dataTypeCode"
                    default-expand-all
                    :props="treeProps"
                    empty-text="暂无数据类型"
                    highlight-current
                    class="type-tree"
                    :expand-on-click-node="false"
                    @node-click="handleTreeNodeClick"
                >
                    <template #default="{ data }">
                        <span class="tree-node">
                            <span class="tree-node-name">{{ data.dataTypeName }}</span>
                            <el-tag size="small" effect="plain">{{ DATA_TYPE_NODE_TYPE_TEXT[data.nodeType as DataTypeNodeType] || data.nodeType }}</el-tag>
                        </span>
                    </template>
                </el-tree>
            </div>
        </el-card>

        <el-card class="list-card" shadow="never">
            <template #header >
                <div class="card-header">
                    <span class="card-title">数据类型管理</span>
                    <el-button type="primary" @click="openCreateDialog" :disabled="contentMode !== 'list' || selectedType.nodeType === 'CONCRETE'">新增数据类型</el-button>
                </div>
            </template>

            <template v-if="contentMode === 'list'">
                <el-form :model="queryForm" inline class="query-form">
                    <el-form-item label="类型名称">
                        <el-input v-model="queryForm.dataTypeName" clearable placeholder="如 行情日线" style="width: 180px" @keyup.enter="handleSearch" />
                    </el-form-item>
                    <el-form-item label="类型标签">
                        <el-input v-model="queryForm.dataTypeLabel" clearable placeholder="如 股票日线" style="width: 180px" @keyup.enter="handleSearch" />
                    </el-form-item>
                    <el-form-item>
                        <el-button type="primary" @click="handleSearch">查询</el-button>
                        <el-button @click="handleReset">重置</el-button>
                    </el-form-item>
                </el-form>

                <el-table v-loading="tableLoading" :data="tableData" border row-key="dataTypeCode" height="calc(100vh - 330px)">
                    <el-table-column prop="dataTypeCode" label="类型编码" min-width="130" align="center" />
                    <el-table-column prop="dataTypeName" label="类型名称" min-width="150" align="center" />
                    <el-table-column prop="dataTypeLabel" label="类型标签" min-width="170" align="center" />
                    <el-table-column prop="version" label="当前版本" min-width="100" align="center" show-overflow-tooltip />
                    <el-table-column prop="fieldCount" label="字段数" width="90" align="center" />
                    <el-table-column prop="status" label="状态" width="90" align="center">
                        <template #default="{ row }">
                            <el-tag :type="COMMON_STATUS_TAG_TYPE[row.status as CommonStatus]">{{ COMMON_STATUS_TEXT[row.status as CommonStatus] || row.status }}</el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column label="操作" width="310" fixed="right" align="center">
                        <template #default="{ row }">
                            <el-button link type="primary" @click="openFieldPanel(row)" :disabled="row.nodeType !== 'CONCRETE'">结构</el-button>
                            <!-- <el-button link type="primary" @click="openVersionPanel(row)" :disabled="row.nodeType !== 'CONCRETE'">历史</el-button> -->
                            <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
                            <el-button link type="primary" @click="handleDeleteType(row)">删除</el-button>
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
            </template>

            <template v-else>
                <div v-if="selectedType.dataTypeCode" class="detail-header">
                    <div class="detail-title">
                        {{ contentMode === 'field' ? '字段结构维护' : '历史版本' }}：
                        {{ selectedType.dataTypeName }}（{{ selectedType.dataTypeCode }}）
                    </div>
                    <div class="detail-actions">
                        <el-button v-if="contentMode === 'field'" type="success" :disabled="!selectedType.dataTypeCode" @click="handlePublish">发布版本</el-button>
                        <el-button v-if="contentMode === 'field'" type="primary" plain @click="goToVersionPanel">查看历史版本</el-button>
                        <el-button v-if="contentMode === 'version'" type="primary" plain @click="goToFieldPanel">维护字段结构</el-button>
                        <el-button @click="backToList">返回列表</el-button>
                    </div>
                </div>
                <el-empty v-else description="请先选择一个具体数据类型" />

                <DataTypeFieldPanel
                    v-if="selectedType.dataTypeCode && contentMode === 'field'"
                    :data-type-code="selectedType.dataTypeCode"
                />

                <div v-if="selectedType.dataTypeCode && contentMode === 'version'" class="version-panel">
                    <el-table :data="versions" border height="calc(100vh - 260px)">
                        <el-table-column prop="version" label="版本号" min-width="100" />
                        <el-table-column prop="versionName" label="版本名称" min-width="100" />
                        <el-table-column prop="publishStatus" label="发布状态" width="100">
                            <template #default="{ row }">
                                <el-tag :type="PUBLISH_STATUS_TAG_TYPE[row.publishStatus as PublishStatus]">
                                    {{ PUBLISH_STATUS_TEXT[row.publishStatus as PublishStatus] || row.publishStatus }}
                                </el-tag>
                            </template>
                        </el-table-column>
                        <el-table-column prop="publishTime" label="发布时间" min-width="170" />
                        <el-table-column prop="changeSummary" label="变更说明" min-width="220" show-overflow-tooltip />
                        <el-table-column label="操作" width="120" fixed="right" align="center">
                            <template #default="{ row }">
                                <el-button link type="primary" @click="openVersionSchemaDialog(row)">
                                    查看结构
                                </el-button>
                            </template>
                        </el-table-column>
                    </el-table>
                </div>
            </template>
        </el-card>

        <DataTypeFormDialog
            v-model="typeDialogVisible"
            :mode="dialogMode"
            :form-data="currentType"
            :parent-type="selectedType"
            @success="handleSaveSuccess"
        />

        <el-dialog
            v-model="versionSchemaDialogVisible"
            title="版本字段结构"
            width="800px"
            destroy-on-close
        >
            <div class="schema-dialog-header">
                <span>版本：{{ currentVersionSchema?.versionName || `V${currentVersionSchema?.version || ''}` }}</span>
                <span v-if="currentVersionSchema?.publishTime">发布时间：{{ currentVersionSchema.publishTime }}</span>
            </div>

            <el-input
                :model-value="currentVersionSchemaContent"
                type="textarea"
                :rows="24"
                readonly
                resize="none"
                class="schema-content"
            />
        </el-dialog>
    </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { DataType, DataTypeVersion, createDefaultDataType } from '/@/types/datatype/entity'
import { getDataTypeDetail, queryDataTypePage, queryDataTypeTree, queryDataTypeVersions, deleteDataType, publishDataTypeVersion } from '/@/api/datatype/dataTypeApi'
import { COMMON_STATUS_TAG_TYPE, COMMON_STATUS_TEXT, DATA_TYPE_NODE_TYPE_TEXT, QueryForm, PUBLISH_STATUS_TEXT,  PUBLISH_STATUS_TAG_TYPE} from '/@/types/datatype/view'
import DataTypeFormDialog from './DataTypeFormDialog.vue'
import DataTypeFieldPanel from './DataTypeFieldPanel.vue'
import { DataTypeNodeType, PublishStatus } from '/@/types/datatype/constants.js'
import { CommonStatus } from '/@/types/collectiontask/constants.js'

const treeProps = { 
    label: 'dataTypeName', 
    children: 'children' 
}

const queryForm = reactive<QueryForm>({ 
    typeNameForTree: '', 
    parentCode: '', 
    dataTypeName: '', 
    dataTypeLabel: '' 
})

const pagination = reactive({ 
    pageNo: 1, 
    pageSize: 20, 
    total: 0 
})

const typeTree = ref<DataType[]>([])

const tableData = ref<DataType[]>([])

const versions = ref<DataTypeVersion[]>([])

const selectedType = ref<DataType>(createDefaultDataType())

const currentType = ref<DataType>(createDefaultDataType())

const treeLoading = ref(false)

const tableLoading = ref(false)

const typeDialogVisible = ref(false)

const versionSchemaDialogVisible = ref(false)

const currentVersionSchema = ref<DataTypeVersion | null>(null)

const currentVersionSchemaContent = ref('')

const dialogMode = ref<'create' | 'edit'>('create')

const contentMode = ref<'list' | 'field' | 'version'>('list')

onMounted(() => {
    loadTree()
    loadTable()
})

/** 加载左侧数据类型树。 */
async function loadTree(): Promise<void> {
    treeLoading.value = true
    try {
        typeTree.value = await queryDataTypeTree({ dataTypeName: queryForm.typeNameForTree })
    } finally {
        treeLoading.value = false
    }
}

/** 加载右侧数据类型列表。 */
async function loadTable(): Promise<void> {
    tableLoading.value = true
    try {
        const result = await queryDataTypePage({
            parentCode: queryForm.parentCode,
            dataTypeName: queryForm.dataTypeName,
            dataTypeLabel: queryForm.dataTypeLabel,
            pageNo: pagination.pageNo,
            pageSize: pagination.pageSize
        })
        tableData.value = result.records || []
        pagination.total = result.total || 0
    } finally {
        tableLoading.value = false
    }
}

function handleTreeNodeClick(row: DataType): void {
    queryForm.parentCode = row.dataTypeCode
    pagination.pageNo = 1
    contentMode.value = 'list'
    selectDataType(row)
    loadTable()
}

function handleSearch(): void {
    if (!queryForm.parentCode) {
        ElMessage.warning('请先选择左侧数据类型节点')
        return
    }
    pagination.pageNo = 1
    loadTable()
}

function handleReset(): void {
    queryForm.dataTypeName = ''
    queryForm.dataTypeLabel = ''
    queryForm.parentCode = selectedType.value.dataTypeCode || ''
    pagination.pageNo = 1
    loadTable()
}

/**
 * 删除类型
 */
async function handleDeleteType(row: DataType): Promise<void> {
    try {
        await ElMessageBox.confirm(
            `确认删除数据类型「${row.dataTypeName}」？`,
            '删除确认',
            {
                type: 'warning',
                confirmButtonText: '删除',
                cancelButtonText: '取消'
            }
        )

        await deleteDataType(row.dataTypeCode)

        ElMessage.success('删除成功')
        await handleSaveSuccess()
    } catch (error) {
        // 取消删除或后端校验失败时直接结束，错误提示由全局拦截器展示
    }
}

function openCreateDialog(): void {
    if (!selectedType.value.dataTypeCode) {
        ElMessage.warning('请先选择左侧数据类型节点，确定新增数据类型的父节点')
        return
    }
    dialogMode.value = 'create'
    currentType.value = createDefaultDataType()
    currentType.value.parentCode = selectedType.value.dataTypeCode
    typeDialogVisible.value = true
}

function openEditDialog(row: DataType): void {
    dialogMode.value = 'edit'
    currentType.value = { ...row }
    typeDialogVisible.value = true
}

async function openFieldPanel(row: DataType): Promise<void> {
    await selectDataType(row)
    contentMode.value = 'field'
}

function goToFieldPanel(): void {
    contentMode.value = 'field'
}

function goToVersionPanel(): void {
    contentMode.value = 'version'
}

function backToList(): void {
    contentMode.value = 'list'
}

function openVersionSchemaDialog(row: DataTypeVersion): void {
    currentVersionSchema.value = row
    currentVersionSchemaContent.value = formatFieldSchemaContent(row.fieldSchemaContent)
    versionSchemaDialogVisible.value = true
}

function formatFieldSchemaContent(content: string): string {
    if (!content) {
        return ''
    }

    try {
        return JSON.stringify(JSON.parse(content), null, 2)
    } catch (error) {
        return content
    }
}

/** 选择数据类型后加载字段、版本、数据集和存储映射概要。 */
async function selectDataType(row: DataType): Promise<void> {
    selectedType.value = { ...row }
    const detail = await getDataTypeDetail(row.dataTypeCode)
    versions.value = detail.versions || []
}

/** 发布数据类型版本，发布后形成字段快照。 */
async function handlePublish(): Promise<void> {
    if (!selectedType.value.dataTypeCode) {
        ElMessage.warning('请先选择数据类型')
        return
    }
    const { value } = await ElMessageBox.prompt('请输入本次版本变更说明', `发布数据类型版本: V${versions.value.length + 1}`, {
        confirmButtonText: '发布',
        cancelButtonText: '取消',
        inputType: 'textarea',
        inputPlaceholder: '如：初始化字段结构'
    })
    await publishDataTypeVersion(selectedType.value.dataTypeCode, { version: versions.value.length + 1, versionName: `版本V${versions.value.length + 1}`, changeSummary: value })
    ElMessage.success('版本发布成功')
    versions.value = await queryDataTypeVersions(selectedType.value.dataTypeCode)
    await loadTable()
}

async function handleSaveSuccess(): Promise<void> {
    typeDialogVisible.value = false
    contentMode.value = 'list'
    await loadTree()
    await loadTable()
}

</script>

<style scoped>

.data-type-page {
    display: grid;
    grid-template-columns: 300px 1fr;
    gap: 16px;
    padding: 16px;
}

.tree-card, .list-card {
    min-height: calc(100vh - 120px);
}

.card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.card-header.compact {
    justify-content: flex-start;
}

.card-title {
    font-weight: 600;
    font-size: 16px;
}

.tree-search {
    margin-bottom: 12px;
}


.tree-body {
    height: calc(100vh - 205px);
    overflow: auto;
    padding-right: 4px;
}

.type-tree {
    --el-tree-node-hover-bg-color: #f0f6ff;
}

:deep(.type-tree .el-tree-node__content) {
    height: 36px;
    border-radius: 6px;
    margin-bottom: 4px;
    padding-right: 8px;
}

:deep(.type-tree .el-tree-node.is-current > .el-tree-node__content) {
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

.query-form {
    margin-bottom: 12px;
}

.pagination-wrap {
    display: flex;
    justify-content: center;
    margin-top: 12px;
}

.detail-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    margin-bottom: 12px;
}

.detail-title {
    font-weight: 600;
}

.detail-actions {
    display: flex;
    align-items: center;
    gap: 8px;
}

.version-panel {
    margin-top: 12px;
}

.panel-toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 12px;
}

.panel-title {
    font-weight: 600;
}

.schema-dialog-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    margin-bottom: 12px;
    color: #606266;
    font-size: 13px;
}

.schema-content {
    font-family: Consolas, Monaco, Menlo, monospace;
}

:deep(.schema-content textarea) {
    font-family: Consolas, Monaco, Menlo, monospace;
    line-height: 1.5;
}
</style>