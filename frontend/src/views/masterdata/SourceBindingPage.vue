<template>
    <div class="page">
        <el-card shadow="never">
            <template #header>
                <div>
                    <b>主数据来源管理</b>
                    <div class="hint">配置 Dataset 如何映射为系统注册的主数据类型；页面不暴露物理表和物理列。</div>
                </div>
            </template>
            <el-form :model="filters" inline>
                <el-form-item label="类型">
                    <el-select
                        v-model="filters.masterDataType"
                        clearable
                        placeholder="全部"
                        style="width: 150px"
                    >
                        <el-option
                            v-for="item in typeOptions"
                            :key="item.typeCode"
                            :label="item.displayName"
                            :value="item.typeCode"
                        />
                    </el-select>
                </el-form-item>
                <el-form-item label="Dataset/关键字">
                    <el-input v-model="filters.keyword" clearable />
                </el-form-item>
                <el-form-item label="触发">
                    <el-select v-model="filters.triggerMode" clearable style="width: 110px">
                        <el-option label="手工" value="MANUAL" />
                        <el-option label="自动" value="AUTO" />
                    </el-select>
                </el-form-item>
                <el-form-item label="启用">
                    <el-select v-model="filters.enabled" clearable style="width: 100px">
                        <el-option label="启用" :value="true" />
                        <el-option label="停用" :value="false" />
                    </el-select>
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" @click="search">查询</el-button>
                    <el-button @click="reset">重置</el-button>
                    <el-button @click="openCreate">新增来源</el-button>
                </el-form-item>
            </el-form>
            <el-table v-loading="loading" :data="records" border empty-text="暂无来源绑定">
                <el-table-column prop="bindingName" label="来源名称" min-width="150" />
                <el-table-column prop="bindingCode" label="编码" min-width="130" />
                <el-table-column label="MasterDataType" min-width="150">
                    <template #default="{ row }">{{ typeLabel(row.masterDataType) }}</template>
                </el-table-column>
                <el-table-column prop="datasetCode" label="Dataset" min-width="150" />
                <el-table-column prop="triggerMode" label="触发方式" width="100" />
                <el-table-column label="启用" width="85">
                    <template #default="{ row }">
                        <el-switch v-model="row.enabled" @change="toggle(row)" />
                    </template>
                </el-table-column>
                <el-table-column label="Readiness" width="110">
                    <template #default="{ row }">
                        <el-tag :type="readinessTag(row.readiness)">{{ row.readiness }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="260" fixed="right">
                    <template #default="{ row }">
                        <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
                        <el-button link type="primary" @click="openMapping(row)">字段映射</el-button>
                        <el-button
                            link
                            type="success"
                            :disabled="row.readiness !== 'READY' || !row.enabled"
                            @click="publish(row)"
                        >
                            发布
                        </el-button>
                    </template>
                </el-table-column>
            </el-table>
            <div class="pagination">
                <el-pagination
                    v-model:current-page="filters.pageNo"
                    v-model:page-size="filters.pageSize"
                    :total="total"
                    :page-sizes="[10, 20, 50]"
                    layout="total, sizes, prev, pager, next, jumper"
                    @size-change="search"
                    @current-change="load"
                />
            </div>
        </el-card>
        <el-drawer v-model="editorOpen" :title="editingId ? '编辑来源' : '新增来源'" size="620px" destroy-on-close>
            <el-form ref="editorForm" :model="editor" label-width="110px">
                <el-form-item label="来源编码" required>
                    <el-input v-model="editor.bindingCode" :disabled="!!editingId" />
                </el-form-item>
                <el-form-item label="来源名称" required>
                    <el-input v-model="editor.bindingName" />
                </el-form-item>
                <el-form-item label="MasterDataType" required>
                    <el-select
                        v-model="editor.masterDataType"
                        :disabled="!!editingId"
                        placeholder="从系统注册表选择"
                        style="width: 100%"
                    >
                        <el-option
                            v-for="item in typeOptions"
                            :key="item.typeCode"
                            :label="item.displayName"
                            :value="item.typeCode"
                        />
                    </el-select>
                </el-form-item>
                <el-form-item label="Dataset" required>
                    <el-select
                        v-model="editor.datasetCode"
                        filterable
                        placeholder="选择已存在的 Dataset"
                        style="width: 100%"
                    >
                        <el-option
                            v-for="item in datasets"
                            :key="item.datasetCode"
                            :label="`${item.datasetName || item.datasetCode}（${item.datasetCode}）`"
                            :value="item.datasetCode"
                        />
                    </el-select>
                </el-form-item>
                <el-form-item label="触发方式">
                    <el-radio-group v-model="editor.triggerMode">
                        <el-radio label="MANUAL">手工</el-radio>
                        <el-radio label="AUTO">自动</el-radio>
                    </el-radio-group>
                </el-form-item>
                <el-form-item label="启用">
                    <el-switch v-model="editor.enabled" />
                </el-form-item>
                <el-form-item label="说明">
                    <el-input v-model="editor.description" type="textarea" />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="editorOpen = false">取消</el-button>
                <el-button type="primary" :loading="saving" @click="saveEditor">保存</el-button>
            </template>
        </el-drawer>
        <el-drawer v-model="mappingOpen" title="字段映射与可发布性" size="820px" destroy-on-close>
            <template v-if="mappingBinding && definition">
                <el-alert
                    v-if="readinessErrors.length"
                    :title="`不可发布：${readinessErrors.join('；')}`"
                    type="error"
                    :closable="false"
                    show-icon
                />
                <el-alert
                    v-else
                    title="READY：必填字段、Dataset 字段和映射配置均通过检查。"
                    type="success"
                    :closable="false"
                    show-icon
                />
                <el-table :data="mappingRows" border class="mapping-table">
                    <el-table-column prop="fieldName" label="主数据字段" min-width="160">
                        <template #default="{ row }">{{ row.fieldName }}（{{ row.fieldCode }}）</template>
                    </el-table-column>
                    <el-table-column label="必填" width="70">
                        <template #default="{ row }">{{ row.required ? '是' : '否' }}</template>
                    </el-table-column>
                    <el-table-column prop="valueType" label="类型" width="90" />
                    <el-table-column label="映射方式" width="130">
                        <template #default="{ row }">
                            <el-select v-model="row.mappingMode" style="width: 110px">
                                <el-option label="Dataset 字段" value="FIELD" />
                                <el-option label="固定值" value="CONSTANT" />
                            </el-select>
                        </template>
                    </el-table-column>
                    <el-table-column label="Dataset 字段 / 固定值" min-width="260">
                        <template #default="{ row }">
                            <el-select
                                v-if="row.mappingMode === 'FIELD'"
                                v-model="row.sourceFieldCode"
                                filterable
                                clearable
                                style="width: 100%"
                            >
                                <el-option
                                    v-for="field in datasetFields"
                                    :key="field.fieldCode"
                                    :label="`${field.fieldName || field.fieldCode}（${field.fieldCode}）`"
                                    :value="field.fieldCode"
                                />
                            </el-select>
                            <el-input v-else v-model="row.constantValue" placeholder="例如 SW / INDUSTRY" />
                        </template>
                    </el-table-column>
                </el-table>
                <div class="mapping-actions">
                    <el-button @click="refreshReadiness">重新校验</el-button>
                    <el-button type="primary" :loading="mappingSaving" @click="saveMapping">保存映射</el-button>
                </div>
            </template>
        </el-drawer>
    </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { queryEnabledDatasets, queryDatasetFields } from '/@/api/appmetric/appMetricApi'
import {
    publishSourceBinding,
    queryMasterDataTypes,
    querySourceBindings,
    querySourceMappings,
    querySourceReadiness,
    saveSourceBinding,
    saveSourceMappings,
    setSourceBindingEnabled
} from '/@/api/masterdata/masterDataApi'
import type { DatasetFieldOption } from '/@/api/appmetric/appMetricApi'
import type { MasterDataTypeDefinition, SourceBinding, SourceFieldMapping } from '/@/types/masterdata'

const loading = ref(false), saving = ref(false), mappingSaving = ref(false), editorOpen = ref(false), mappingOpen = ref(false), total = ref(0)
const records = ref<SourceBinding[]>([]), typeOptions = ref<MasterDataTypeDefinition[]>([]), datasets = ref<{
    datasetCode: string;
    datasetName?: string
}[]>([]), datasetFields = ref<DatasetFieldOption[]>([]), readinessErrors = ref<string[]>([])
const mappingBinding = ref<SourceBinding | null>(null), definition = ref<MasterDataTypeDefinition | null>(null), editingId = ref<number | undefined>()
const filters = reactive({
    keyword: '',
    masterDataType: '',
    triggerMode: '',
    enabled: undefined as boolean | undefined,
    pageNo: 1,
    pageSize: 20
})
const editor = reactive({
    bindingCode: '',
    bindingName: '',
    masterDataType: '' as string,
    datasetCode: '',
    triggerMode: 'MANUAL' as 'MANUAL' | 'AUTO',
    enabled: true,
    description: ''
})
const mappingRows = ref<(SourceFieldMapping & {
    fieldCode: string;
    fieldName: string;
    valueType: string;
    required: boolean
})[]>([])
const typeLabel = (code: string) => typeOptions.value.find(item => item.typeCode === code)?.displayName || code
const readinessTag = (value: string) => value === 'READY' ? 'success' : value === 'DISABLED' ? 'info' : 'danger'

async function load(): Promise<void> {
    loading.value = true;
    try {
        const result = await querySourceBindings({
            ...filters
        });
        records.value = result.records || [];
        total.value = result.total || 0
    }
    finally {
        loading.value = false
    }
}

function search(): void {
    filters.pageNo = 1;
    load()
}

function reset(): void {
    Object.assign(filters, {
        keyword: '',
        masterDataType: '',
        triggerMode: '',
        enabled: undefined,
        pageNo: 1
    });
    load()
}

function openCreate(): void {
    editingId.value = undefined;
    Object.assign(editor, {
        bindingCode: '',
        bindingName: '',
        masterDataType: '',
        datasetCode: '',
        triggerMode: 'MANUAL',
        enabled: true,
        description: ''
    });
    editorOpen.value = true;
    loadDatasets()
}

function openEdit(row: SourceBinding): void {
    editingId.value = row.id;
    Object.assign(editor, {
        bindingCode: row.bindingCode,
        bindingName: row.bindingName,
        masterDataType: row.masterDataType,
        datasetCode: row.datasetCode,
        triggerMode: row.triggerMode === 'AUTO' ? 'AUTO' : 'MANUAL',
        enabled: row.enabled,
        description: row.description || ''
    });
    editorOpen.value = true;
    loadDatasets()
}

async function loadDatasets(): Promise<void> {
    const result = await queryEnabledDatasets();
    datasets.value = result.records || []
}

async function saveEditor(): Promise<void> {
    if (!editor.bindingCode || !editor.bindingName || !editor.masterDataType || !editor.datasetCode) {
        ElMessage.warning('请完整填写来源信息');
        return
    }
    saving.value = true;
    try {
        const row = await saveSourceBinding({
            ...editor,
            masterDataType: editor.masterDataType
        });
        editorOpen.value = false;
        await load();
        ElMessage.success(`来源已保存，请配置字段映射（${row.bindingCode}）`)
    }
    finally {
        saving.value = false
    }
}

async function openMapping(row: SourceBinding): Promise<void> {
    mappingBinding.value = row;
    definition.value = typeOptions.value.find(item => item.typeCode === row.masterDataType) || await queryMasterDataTypes()
        .then(
            items => items.find(item => item.typeCode === row.masterDataType) || null
        );
    datasetFields.value = await queryDatasetFields(row.datasetCode);
    const saved = await querySourceMappings(row.id);
    const savedMap = new Map(saved.map(item => [item.targetField, item]));
    mappingRows.value = (definition.value?.fields || []).map(field => ({
        fieldCode: field.fieldCode,
        fieldName: field.fieldName,
        valueType: field.valueType,
        required: field.required,
        targetField: field.fieldCode,
        mappingMode: savedMap.get(field.fieldCode)?.mappingMode || 'FIELD',
        sourceFieldCode: savedMap.get(field.fieldCode)?.sourceFieldCode || '',
        constantValue: savedMap.get(field.fieldCode)?.constantValue || ''
    }));
    await refreshReadiness();
    mappingOpen.value = true
}

async function saveMapping(): Promise<void> {
    if (!mappingBinding.value)
        return;
    mappingSaving.value = true;
    try {
        const mappings = mappingRows.value.filter(row => row.mappingMode === 'FIELD' ? !!row.sourceFieldCode : !!row.constantValue).map(row => ({
            targetField: row.targetField,
            mappingMode: row.mappingMode,
            sourceFieldCode: row.mappingMode === 'FIELD' ? row.sourceFieldCode : null,
            constantValue: row.mappingMode === 'CONSTANT' ? row.constantValue : null
        }));
        await saveSourceMappings(mappingBinding.value.id, mappings);
        await refreshReadiness();
        await load();
        ElMessage.success('字段映射已保存')
    }
    finally {
        mappingSaving.value = false
    }
}

async function refreshReadiness(): Promise<void> {
    if (!mappingBinding.value)
        return;
    const result = await querySourceReadiness(mappingBinding.value.id);
    readinessErrors.value = result.errors || []
}

async function toggle(row: SourceBinding): Promise<void> {
    try {
        await setSourceBindingEnabled(row.id, row.enabled);
        await load()
    }
    catch {
        row.enabled = !row.enabled
    }
}

async function publish(row: SourceBinding): Promise<void> {
    await ElMessageBox.confirm(`将读取 ${row.datasetCode} 并发布为${typeLabel(row.masterDataType)}，是否继续？`, '发布确认', {
        type: 'warning',
        confirmButtonText: '发布',
        cancelButtonText: '取消'
    });
    const batch = await publishSourceBinding(row.id, {
        syncType: 'FULL'
    });
    ElMessage.success(`发布批次已创建：${batch.id}`);
    await load()
}

onMounted(async () => {
    typeOptions.value = await queryMasterDataTypes();
    await load()
})
</script>

<style scoped>
.page {
    padding: 16px;
}

.hint {
    margin-top: 6px;
    color: var(--el-text-color-secondary);
    font-size: 13px;
}

.pagination {
    display: flex;
    justify-content: flex-end;
    margin-top: 16px;
}

.mapping-table {
    margin-top: 16px;
}

.mapping-actions {
    display: flex;
    justify-content: flex-end;
    gap: 8px;
    margin-top: 16px;
}
</style>