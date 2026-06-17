<template>
    <div class="data-source-page">
        <div class="page-header">
            <div class="header-title">数据源管理</div>
            <el-button type="primary" @click="openCreateDialog">新增数据源</el-button>
        </div>

        <div v-loading="loading" class="page-content">
            <el-row :gutter="16">
                <el-col
                    v-for="source in dataSources"
                    :key="source.dataSourceCode"
                    :xs="24"
                    :sm="12"
                    :md="8"
                    :lg="6"
                >
                    <el-card shadow="hover" class="data-source-card">
                        <template #header>
                            <div class="card-header">
                                <div class="card-title">
                                    <span>{{ source.dataSourceName }}</span>
                                    <el-tag size="small" type="info">{{ source.dataSourceType }}</el-tag>
                                </div>
                                <el-tag :type="STATUS_TAG_TYPE[source.status]">
                                    {{ STATUS_TEXT[source.status] }}
                                </el-tag>
                            </div>
                        </template>
                        <div class="card-content">
                            <div class="card-row">编码：{{ source.dataSourceCode }}</div>
                            <div class="card-row">地址：{{ source.host }}:{{ source.port }}</div>
                            <div class="card-row">库：{{ source.databaseName }}</div>
                        </div>
                        <div class="card-actions">
                            <el-button size="small" @click="handleTest(source)">测试连接</el-button>
                            <el-button size="small" @click="openDetailDialog(source)">查看信息</el-button>
                            <el-button size="small" type="primary" @click="openEditDialog(source)">修改</el-button>
                            <el-button size="small" type="danger" @click="handleDelete(source)">删除</el-button>
                        </div>
                    </el-card>
                </el-col>
            </el-row>
        </div>

        <el-dialog
            v-model="formDialogVisible"
            :title="isEdit ? '修改数据源' : '新增数据源'"
            width="720px"
            destroy-on-close
        >
            <el-form
                ref="formRef"
                :model="form"
                :rules="rules"
                label-width="120px"
            >
                <el-row :gutter="16">
                    <el-col :span="12">
                        <el-form-item label="数据源编码" prop="dataSourceCode">
                            <el-input v-model="form.dataSourceCode" :disabled="isEdit" />
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="数据源名称" prop="dataSourceName">
                            <el-input v-model="form.dataSourceName" />
                        </el-form-item>
                    </el-col>
                </el-row>

                <el-row :gutter="16">
                    <el-col :span="12">
                        <el-form-item label="数据源类型" prop="dataSourceType">
                            <el-select v-model="form.dataSourceType" style="width: 100%">
                                <el-option label="MySQL" value="MYSQL" />
                            </el-select>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="状态" prop="status">
                            <el-select v-model="form.status" style="width: 100%">
                                <el-option label="启用" value="ENABLED" />
                                <el-option label="停用" value="DISABLED" />
                                <el-option label="下线" value="OFFLINE" />
                            </el-select>
                        </el-form-item>
                    </el-col>
                </el-row>

                <el-row :gutter="16">
                    <el-col :span="12">
                        <el-form-item label="主机" prop="host">
                            <el-input v-model="form.host" placeholder="127.0.0.1" />
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="端口" prop="port">
                            <el-input v-model.number="form.port" placeholder="3306" />
                        </el-form-item>
                    </el-col>
                </el-row>

                <el-row :gutter="16">
                    <el-col :span="12">
                        <el-form-item label="用户名" prop="username">
                            <el-input v-model="form.username" />
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="密码" prop="password">
                            <el-input v-model="form.password" type="password" show-password />
                        </el-form-item>
                    </el-col>
                </el-row>

                <el-row :gutter="16">
                    <el-col :span="12">
                        <el-form-item label="数据库名" prop="databaseName">
                            <el-input v-model="form.databaseName" />
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="连接参数">
                            <el-input v-model="form.connectionParams" placeholder="useSSL=false&serverTimezone=Asia/Shanghai" />
                        </el-form-item>
                    </el-col>
                </el-row>

                <el-form-item label="备注">
                    <el-input v-model="form.remark" type="textarea" :rows="2" />
                </el-form-item>

                <el-alert
                    title="提示：密码不会回显，修改时请重新输入。"
                    type="info"
                    show-icon
                    :closable="false"
                />
            </el-form>
            <template #footer>
                <el-button @click="formDialogVisible = false">取消</el-button>
                <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
            </template>
        </el-dialog>

        <el-dialog
            v-model="detailDialogVisible"
            title="数据源信息"
            width="560px"
        >
            <el-descriptions :column="2" border>
                <el-descriptions-item label="编码">{{ detail?.dataSourceCode }}</el-descriptions-item>
                <el-descriptions-item label="名称">{{ detail?.dataSourceName }}</el-descriptions-item>
                <el-descriptions-item label="类型">{{ detail?.dataSourceType }}</el-descriptions-item>
                <el-descriptions-item label="状态">{{ detail ? STATUS_TEXT[detail.status] : '' }}</el-descriptions-item>
                <el-descriptions-item label="地址">{{ detail?.host }}:{{ detail?.port }}</el-descriptions-item>
                <el-descriptions-item label="数据库">{{ detail?.databaseName }}</el-descriptions-item>
                <el-descriptions-item label="用户名">{{ detail?.username }}</el-descriptions-item>
                <el-descriptions-item label="密码">{{ detail?.password }}</el-descriptions-item>
                <el-descriptions-item label="连接参数" :span="2">{{ detail?.connectionParams || '-' }}</el-descriptions-item>
                <el-descriptions-item label="备注" :span="2">{{ detail?.remark || '-' }}</el-descriptions-item>
            </el-descriptions>
            <template #footer>
                <el-button @click="detailDialogVisible = false">关闭</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'

import type { DataSource } from '/@/types/datasource/entity'
import { listDataSources, saveDataSource, deleteDataSource, testDataSourceConnection, getDataSourceDetail } from '/@/api/datasource/dataSourceApi'

const loading = ref(false)
const saving = ref(false)
const dataSources = ref<DataSource[]>([])

const formDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const isEdit = ref(false)
const detail = ref<DataSource | null>(null)
const formRef = ref<FormInstance>()

const STATUS_TEXT: Record<string, string> = {
    ENABLED: '启用',
    DISABLED: '停用',
    OFFLINE: '下线'
}

const STATUS_TAG_TYPE: Record<string, string> = {
    ENABLED: 'success',
    DISABLED: 'info',
    OFFLINE: 'warning'
}

const form = reactive<DataSource>({
    dataSourceCode: '',
    dataSourceName: '',
    dataSourceType: 'MYSQL',
    host: '',
    port: 3306,
    username: '',
    password: '',
    databaseName: '',
    connectionParams: '',
    status: 'ENABLED',
    remark: ''
})

const rules: FormRules = {
    dataSourceCode: [{ required: true, message: '请输入数据源编码', trigger: 'blur' }],
    dataSourceName: [{ required: true, message: '请输入数据源名称', trigger: 'blur' }],
    dataSourceType: [{ required: true, message: '请选择数据源类型', trigger: 'change' }],
    host: [{ required: true, message: '请输入主机地址', trigger: 'blur' }],
    port: [{ required: true, message: '请输入端口', trigger: 'blur' }],
    username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
    password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
    databaseName: [{ required: true, message: '请输入数据库名', trigger: 'blur' }],
    status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

onMounted(async () => {
    await loadDataSources()
})

async function loadDataSources(): Promise<void> {
    loading.value = true
    try {
        dataSources.value = await listDataSources({})
    } finally {
        loading.value = false
    }
}

function openCreateDialog(): void {
    isEdit.value = false
    Object.assign(form, {
        dataSourceCode: '',
        dataSourceName: '',
        dataSourceType: 'MYSQL',
        host: '',
        port: 3306,
        username: '',
        password: '',
        databaseName: '',
        connectionParams: '',
        status: 'ENABLED',
        remark: ''
    })
    formDialogVisible.value = true
}

function openEditDialog(source: DataSource): void {
    isEdit.value = true
    Object.assign(form, {
        ...source,
        password: ''
    })
    formDialogVisible.value = true
}

async function openDetailDialog(source: DataSource): Promise<void> {
    detail.value = await getDataSourceDetail(source.dataSourceCode)
    detailDialogVisible.value = true
}

async function handleSave(): Promise<void> {
    const valid = await formRef.value?.validate().catch(() => false)
    if (!valid) return

    saving.value = true
    try {
        await saveDataSource(form)
        ElMessage.success('数据源已保存')
        formDialogVisible.value = false
        await loadDataSources()
    } finally {
        saving.value = false
    }
}

async function handleDelete(source: DataSource): Promise<void> {
    await ElMessageBox.confirm(`确认删除数据源 ${source.dataSourceName}？`, '删除数据源', {
        type: 'warning',
        confirmButtonText: '确认',
        cancelButtonText: '取消'
    })

    await deleteDataSource(source.dataSourceCode)
    ElMessage.success('数据源已删除')
    await loadDataSources()
}

async function handleTest(source: DataSource): Promise<void> {
    const ok = await testDataSourceConnection(source.dataSourceCode)
    if (ok) {
        ElMessage.success('连接成功')
    } else {
        ElMessage.error('连接失败')
    }
}
</script>

<style scoped>
.data-source-page {
    padding: 16px;
}

.page-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16px;
}

.header-title {
    font-size: 16px;
    font-weight: 600;
}

.page-content {
    min-height: 120px;
}

.data-source-card {
    margin-bottom: 16px;
}

.card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
}

.card-title {
    display: flex;
    align-items: center;
    gap: 8px;
    font-weight: 600;
}

.card-content {
    display: flex;
    flex-direction: column;
    gap: 6px;
    margin-bottom: 12px;
}

.card-row {
    color: #606266;
}

.card-actions {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
}
</style>
