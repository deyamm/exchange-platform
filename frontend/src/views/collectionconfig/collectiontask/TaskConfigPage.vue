<template>
    <div class="task-config-mapping-page">
        <div class="page-header">
            <div class="header-left">
                <el-button @click="$router.back()">返回</el-button>
                <span class="page-title">
                    任务配置与存储映射：{{ taskDetail?.taskInfo?.taskName }}（{{ taskCode }}）
                </span>
            </div>
            <el-button type="primary" :loading="savingMapping" :disabled="mappingLocked" @click="handleSaveMappingDraft">
                保存映射草稿
            </el-button>
        </div>

        <div v-loading="loading" class="page-content">
            <template v-if="taskDetail">
                <!-- 任务配置 -->
                <el-card shadow="never" class="detail-card">
                    <template #header>
                        <div class="section-header">
                            <span class="section-title">任务配置</span>

                            <div class="section-actions">
                                <el-button type="primary" size="small" @click="openConfigDialog">
                                    {{ taskDetail.config ? '修改配置' : '新增配置' }}
                                </el-button>
                                <el-button v-if="taskDetail.config" type="danger" size="small" @click="deleteConfig">
                                    {{ '删除配置' }}
                                </el-button>
                            </div>
                        </div>
                    </template>

                    <el-empty v-if="!taskDetail.config" description="尚未配置，请点击「新增配置」" />
                    <el-descriptions v-else :column="3" border>
                        <el-descriptions-item label="配置编码">{{ taskDetail.config.taskConfigCode }}</el-descriptions-item>
                        <el-descriptions-item label="数据主题">{{ taskDetail.config.dataTopicCode }}</el-descriptions-item>
                        <el-descriptions-item label="数据类型">{{ taskDetail.config.dataTypeCode }}</el-descriptions-item>
                        <el-descriptions-item label="数据类型版本">V{{ taskDetail.config.dataTypeVersionNo }}</el-descriptions-item>
                        <el-descriptions-item label="配置状态">
                            <el-tag :type="CONFIG_STATUS_TAG_TYPE[taskDetail.config.configStatus as ConfigStatus]">
                                {{ CONFIG_STATUS_TEXT[taskDetail.config.configStatus as ConfigStatus] }}
                            </el-tag>
                        </el-descriptions-item>
                        <el-descriptions-item label="存储映射编码">{{ taskDetail.config.storageMappingCode }}</el-descriptions-item>
                        <el-descriptions-item label="配置说明" :span="3">{{ taskDetail.config.description }}</el-descriptions-item>
                    </el-descriptions>
                </el-card>

                <!-- 物理存储映射配置 -->
                <el-card shadow="never" class="detail-card">
                    <template #header>
                        <div class="section-header">
                            <span class="section-title">物理存储映射配置</span>
                            <div v-if="taskDetail.config" class="section-actions">
                                <el-button size="small" :disabled="mappingLocked" @click="handleResetMappingFromFields">
                                    根据返回字段重新生成
                                </el-button>
                                <el-button
                                    v-if="mappingForm.mappingStatus === 'DRAFT' && hasDraft"
                                    type="success"
                                    size="small"
                                    @click="handleConfirmMapping"
                                >
                                    确认映射
                                </el-button>
                            </div>
                        </div>
                    </template>

                    <el-alert
                        v-if="!taskDetail.config?.dataTypeCode"
                        title="当前任务暂未配置数据类型，无法生成字段映射。"
                        type="warning"
                        show-icon
                        :closable="false"
                    />

                    <template v-else>
                        <el-form
                            ref="mappingFormRef"
                            :model="mappingForm"
                            :rules="mappingRules"
                            label-width="120px"
                            class="mapping-form"
                        >
                            <el-row :gutter="24">
                                <el-col :span="6">
                                    <el-form-item label="数据源" prop="dataSourceCode">
                                        <el-select
                                            v-model="mappingForm.dataSourceCode"
                                            placeholder="请选择数据源"
                                            filterable
                                            clearable
                                            style="width: 100%"
                                            :disabled="mappingLocked"
                                            @change="handleDataSourceChange"
                                        >
                                            <el-option
                                                v-for="source in dataSources"
                                                :key="source.dataSourceCode"
                                                :label="source.dataSourceName + '（' + source.databaseName + '）'"
                                                :value="source.dataSourceCode"
                                            />
                                        </el-select>
                                    </el-form-item>
                                </el-col>
                                <el-col :span="6">
                                    <el-form-item label="物理表 Schema" prop="physicalSchemaName">
                                        <el-input
                                            v-model="mappingForm.physicalSchemaName"
                                            placeholder="由数据源自动带出"
                                            disabled
                                        />
                                    </el-form-item>
                                </el-col>
                                <el-col :span="6">
                                    <el-form-item label="物理表 Name" prop="physicalTableName">
                                        <el-select
                                            v-model="mappingForm.physicalTableName"
                                            filterable
                                            allow-create
                                            default-first-option
                                            placeholder="输入或选择已有表"
                                            style="width: 100%"
                                            :disabled="mappingLocked"
                                            @change="handlePhysicalTableNameChange"
                                        >
                                            <el-option
                                                v-for="table in dataSourceTables"
                                                :key="table.tableName"
                                                :label="table.tableName"
                                                :value="table.tableName"
                                            />
                                        </el-select>
                                    </el-form-item>
                                </el-col>
                                <el-col :span="6">
                                    <el-form-item label="写入策略">
                                        <el-select v-model="mappingForm.writeStrategy" style="width: 100%" :disabled="mappingLocked" @change="markNeedSave">
                                            <el-option label="追加写入" value="APPEND" />
                                            <el-option label="覆盖写入" value="OVERWRITE" />
                                            <el-option label="主键更新" value="UPSERT" />
                                        </el-select>
                                    </el-form-item>
                                </el-col>
                                <el-col :span="6">
                                    <el-form-item label="映射状态">
                                        <el-tag :type="MAPPING_STATUS_TAG_TYPE[mappingForm.mappingStatus as MappingStatus]">
                                            {{ MAPPING_STATUS_TEXT[mappingForm.mappingStatus as MappingStatus] || mappingForm.mappingStatus }}
                                        </el-tag>
                                    </el-form-item>
                                </el-col>
                            </el-row>

                            <el-form-item label="确认备注">
                                <el-input
                                    v-model="mappingForm.confirmRemark"
                                    type="textarea"
                                    :rows="2"
                                    placeholder="确认映射时的备注"
                                    style="max-width: 600px;"
                                    :disabled="mappingLocked"
                                    @input="markNeedSave"
                                />
                            </el-form-item>
                        </el-form>

                        <div class="mapping-columns">
                            <div class="mapping-columns-title">字段映射</div>
                            <el-table
                                :data="mappingForm.columns"
                                class="mapping-table"
                            >
                                <el-table-column label="序号" width="60" align="center">
                                    <template #default="{ $index }">
                                        {{ $index + 1 }}
                                    </template>
                                </el-table-column>

                                <el-table-column label="数据类型字段" align="center">
                                    <el-table-column label="字段" min-width="100" align="center" show-overflow-tooltip>
                                        <template #default="{ row }">
                                            <span>{{ row.dataTypeFieldCode }}/{{ row.dataTypeFieldName }}/{{ row.dataTypeFieldType }}</span>
                                        </template>
                                    </el-table-column>
                                    <el-table-column label="必填" width="80" align="center">
                                        <template #default="{ row }">
                                            <el-tag v-if="row.required" type="warning">是</el-tag>
                                            <el-tag v-else type="info">否</el-tag>
                                        </template>
                                    </el-table-column>
                                    <el-table-column label="唯一键" width="80" align="center">
                                        <template #default="{ row }">
                                            <el-tag v-if="row.uniqueKey" type="warning">是</el-tag>
                                            <el-tag v-else type="info">否</el-tag>
                                        </template>
                                    </el-table-column> 
                                </el-table-column>

                                <el-table-column label="任务返回字段" min-width="120" align="center">
                                    <template #default="{ row }">
                                        <el-select
                                            v-model="row.sourceFieldCode"
                                            placeholder="请选择任务返回字段"
                                            filterable
                                            clearable
                                            style="width: 100%"
                                            :disabled="mappingLocked"
                                            @change="(value: any) => handleSelectChange(value, row)"
                                        >
                                            <template
                                                v-for="field in taskOutputFields"
                                                :key="field.fieldCode"
                                            >
                                                <el-option
                                                    v-if="isFieldOptionVisible(field, row)"
                                                    :label="field.fieldCode + '/' + field.fieldName + '/' + field.fieldType"
                                                    :value="field.fieldCode"
                                                >
                                                    <span>{{ field.fieldCode + '/' + field.fieldName + '/' + field.fieldType }}</span>
                                                </el-option>
                                            </template>
                                        </el-select>
                                    </template>
                                </el-table-column>

                                <el-table-column label="物理表字段信息" align="center">
                                    <el-table-column label="物理字段名称" min-width="80" align="center">
                                        <template #default="{ row }">
                                            <el-input
                                                v-model="row.physicalColumnName"
                                                placeholder="默认继承字段编码"
                                                :disabled="mappingLocked"
                                                @input="markNeedSave"
                                            />
                                        </template>
                                    </el-table-column>
                                    <el-table-column label="物理字段类型" min-width="180" align="center">
                                        <template #default="{ row }">
                                            <div class="mysql-column-type-editor">
                                                <el-select
                                                    v-model="row.physicalColumnBaseType"
                                                    placeholder="类型"
                                                    filterable
                                                    style="width: 45%"
                                                    :disabled="mappingLocked"
                                                    @change="() => handlePhysicalColumnBaseTypeChange(row)"
                                                >
                                                    <el-option
                                                        v-for="option in getMysqlBaseTypeOptions(row)"
                                                        :key="option.value"
                                                        :label="option.label"
                                                        :value="option.value"
                                                    />
                                                </el-select>

                                                <el-select
                                                    v-model="row.physicalColumnTypeLength"
                                                    placeholder="长度/精度"
                                                    filterable
                                                    allow-create
                                                    default-first-option
                                                    style="width: 55%"
                                                    :disabled="mappingLocked || !hasMysqlColumnTypeLength(row)"
                                                    @change="() => syncPhysicalColumnType(row)"
                                                >
                                                    <el-option
                                                        v-for="option in getMysqlColumnTypeLengthOptions(row)"
                                                        :key="option.value"
                                                        :label="option.label"
                                                        :value="option.value"
                                                    />
                                                </el-select>
                                            </div>
                                        </template>
                                    </el-table-column>
                                    <el-table-column label="默认值" min-width="80">
                                        <template #default="{ row }">
                                            <el-input
                                                v-model="row.defaultValue"
                                                placeholder="默认值（可选）"
                                                :disabled="mappingLocked"
                                                @input="markNeedSave"
                                            />
                                        </template>
                                    </el-table-column>
                                </el-table-column>
                            </el-table>
                        </div>
                    </template>
                </el-card>
            </template>
        </div>

        <!-- 任务配置弹窗 -->
        <TaskConfigFormDialog
            v-model="configDialogVisible"
            :task-template="taskDetail?.taskInfo || {} as TaskTemplate"
            :existing-config="taskDetail?.config || null"
            @success="handleConfigSaveSuccess"
        />
    </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'

import type { StorageColumnMapping, TaskDetail, TaskTemplate } from '/@/types/collectiontask/entity'
import type { DataSource, DataSourceTable } from '/@/types/datasource/entity'
import type { ConfigStatus, MappingStatus, StorageMappingDraftRequest } from '/@/types/collectiontask/constants'
import {
    CONFIG_STATUS_TEXT, CONFIG_STATUS_TAG_TYPE, MYSQL_BASE_TYPE_OPTIONS_BY_FIELD_TYPE,
    MYSQL_LENGTH_OPTIONS_BY_BASE_TYPE, MAPPING_STATUS_TEXT, MAPPING_STATUS_TAG_TYPE, StorageMappingForm, ColumnMappingForm,
    ParamField
} from '/@/types/collectiontask/view'
import { getTaskDetail, saveStorageMappingDraft, deleteTaskConfig, confirmStorageMapping, getStorageMapping } from '/@/api/collectiontask/collectionTaskApi'
import { queryDataTypeFields } from '/@/api/datatype/dataTypeApi'
import { listDataSources, listDataSourceTables } from '/@/api/datasource/dataSourceApi'
import TaskConfigFormDialog from './TaskConfigFormDialog.vue'
import { DataTypeField } from '/@/types/datatype/entity'
import { parseParamFields } from '/@/utils/utils'

const route = useRoute()
const taskCode = route.params.taskCode as string

const loading = ref(false)
const savingMapping = ref(false)
const taskDetail = ref<TaskDetail>({} as TaskDetail)
const datatypeFields = ref<DataTypeField[]>([])
const configDialogVisible = ref(false)
const mappingFormRef = ref<FormInstance>()
const taskOutputFields = ref<ParamField[]>([])
const dataSources = ref<DataSource[]>([])
const dataSourceTables = ref<DataSourceTable[]>([])
// 当前是否有已保存的映射草稿，只有保存过草稿后才允许确认映射，避免直接确认未保存的映射配置
const hasDraft = ref(false)

const mappingForm = reactive<StorageMappingForm>({
    storageMappingCode: '',
    taskConfigCode: '',
    dataSourceCode: '',
    physicalSchemaName: '',
    physicalTableName: '',
    writeStrategy: 'UPSERT', // APPEND / UPSERT / OVERWRITE
    mappingStatus: 'DRAFT', 
    confirmRemark: '',
    columns: []
})

const mappingLocked = computed(() => mappingForm.mappingStatus === 'CONFIRMED')

// 已选择的任务返回字段列表，用于控制下拉选项的显示和禁用，避免重复选择
const selectedSourceFieldCodes = computed(() =>
    new Set(
        mappingForm.columns
            .map((column) => column.sourceFieldCode)
            .filter((code) => code && code !== '-')
    )
)

const mappingRules: FormRules = {
    dataSourceCode: [{ required: true, message: '请选择数据源', trigger: 'change' }],
    physicalSchemaName: [{ required: true, message: '请先选择数据源', trigger: 'change' }],
    physicalTableName: [{ required: true, message: '请填写物理表 Name', trigger: 'blur' }]
}

onMounted(async () => {
    await loadDetail()
})


async function loadDetail(): Promise<void> {
    loading.value = true
    try {
        // Task详细信息
        taskDetail.value = await getTaskDetail(taskCode)
        // 已有的数据源列表（用于映射选择）
        dataSources.value = await listDataSources()
        // 解析任务返回字段供映射选择(与form中的返回字段不同)
        taskOutputFields.value = parseParamFields(taskDetail.value?.currentVersion?.outputFieldsJson)
        taskOutputFields.value.unshift({
            fieldCode: '-',
            fieldName: '不映射',
            fieldType: '-',
            required: false,
            uniqueKey: false,
        })
        // 查询数据类型返回字段并填充映射表单
        if (taskDetail.value?.config?.dataTypeCode) {
            datatypeFields.value = await queryDataTypeFields(taskDetail.value.config.dataTypeCode)
            fillMappingForm()
        }
    } finally {
        loading.value = false
    }
}

function openConfigDialog(): void {
    configDialogVisible.value = true
}

function deleteConfig(): void {
    ElMessageBox.confirm('确认删除当前任务配置？删除后相关存储映射也将被删除。', '删除配置', {
        type: 'warning',
        confirmButtonText: '确认',
        cancelButtonText: '取消'
    }).then(async () => {
        await deleteTaskConfig(taskDetail.value?.config?.taskConfigCode || '')
        await loadDetail()
        ElMessage.success('任务配置删除成功')
    })
}

async function handleConfigSaveSuccess(): Promise<void> {
    configDialogVisible.value = false
    await loadDetail()
}

/**
 * 自动填充映射表单数据：包括映射基础信息和字段级映射
 * 用于加载页面和重置映射时调用
 * 包含2种填充逻辑：
 * （1）使用已保存的映射草稿填充，没有草稿则会使用默认值来填充
 * （2）使用已确认的存储映射数据来覆盖草稿数据中的物理表字段映射信息，简化配置流程
 */
function fillMappingForm(mapping = taskDetail.value?.storageMapping): void {
    // 根据configCode自动生成mappingCode
    mappingForm.storageMappingCode = mapping?.storageMappingCode || taskDetail.value?.config?.taskConfigCode + '_mapping' || ''
    mappingForm.taskConfigCode = taskDetail.value?.config?.taskConfigCode || ''
    mappingForm.dataSourceCode = mapping?.dataSourceCode || ''
    mappingForm.physicalSchemaName = mapping?.physicalSchemaName || ''
    mappingForm.physicalTableName = mapping?.physicalTableName || ''
    mappingForm.writeStrategy = mapping?.writeStrategy || 'UPSERT'
    mappingForm.mappingStatus = mapping?.mappingStatus || 'DRAFT'
    mappingForm.confirmRemark = mapping?.confirmRemark || ''

    hasDraft.value = Boolean(mapping?.storageMappingCode)
    syncDataSourceInfo()

    // 构建数据类型字段到存储映射列的映射，方便后续生成表单数据时查找匹配的存储映射列
    const mappedColumnMap = new Map(
        mapping?.columns?.map((col) => [col.dataTypeFieldCode, col]) || []
    )

    // mappingForm的columns根据数据类型字段生成，所以根据数据类型字段来初始化物理表区域的form
    // 如果有已确认的映射则用映射数据覆盖默认值，没有则使用默认值，由于此时还未选择物理表，所以都为默认值或草稿值
    mappingForm.columns = datatypeFields.value.map((field) => {
        return buildMappingColumn(field, mappedColumnMap.get(field.fieldCode))
    })
}

/**
 * 根据选择的数据源编码同步物理表Schema名称和加载表列表，供物理表选择使用
 */
function syncDataSourceInfo(): void {
    if (!mappingForm.dataSourceCode) {
        dataSourceTables.value = []
        return
    }
    const selected = dataSources.value.find((item) => item.dataSourceCode === mappingForm.dataSourceCode)
    if (selected) {
        mappingForm.physicalSchemaName = selected.databaseName
    }
    loadTableOptions()
}

/**
 * 加载数据源对应的表列表，供物理表选择使用，由于是异步，得套层async函数
 */
async function loadTableOptions(): Promise<void> {
    try {
        dataSourceTables.value = await listDataSourceTables(mappingForm.dataSourceCode)
    } catch {
        dataSourceTables.value = []
    }
}

// 数据源变化：标记待保存、同步数据源名称、加载数据表列表
function handleDataSourceChange(): void {
    markNeedSave()
    syncDataSourceInfo()
}

// 物理表变化：标记待保存、同步物理表名称、尝试自动填充映射字段（如果有匹配的已确认映射）
async function handlePhysicalTableNameChange(): Promise<void> {

    if (!mappingForm.physicalSchemaName || !mappingForm.physicalTableName) {
        return
    }

    await autoFillFromStorageMapping()
}

// 根据数据类型字段类型生成mysql字段类型选项，默认字符串类型为VARCHAR，用于下拉框
function getMysqlBaseTypeOptions(row: any): any[] {
    return MYSQL_BASE_TYPE_OPTIONS_BY_FIELD_TYPE[row.dataTypeFieldType] || [
        { label: 'VARCHAR（默认字符串）', value: 'VARCHAR' }
    ]
}

// 根据mysql字段基础类型生成长度/精度选项，默认字符串长度为255，decimal类型长度为18,6，用于下拉框
function getMysqlColumnTypeLengthOptions(row: any): any[] {
    return MYSQL_LENGTH_OPTIONS_BY_BASE_TYPE[row.physicalColumnBaseType] || []
}

// 判断当前mysql字段类型是否有长度/精度选项
function hasMysqlColumnTypeLength(row: any): boolean {
    return getMysqlColumnTypeLengthOptions(row).length > 0
}

// 根据mysql字段基础类型获取默认长度/精度，如字符串默认255，decimal默认18,6，如果没有则返回空字符串
function getDefaultMysqlColumnTypeLength(baseType: string): string {
    const lengthOptions = MYSQL_LENGTH_OPTIONS_BY_BASE_TYPE[baseType]
    if (!lengthOptions?.length) {
        return ''
    }
    return lengthOptions[0].value
}

// 根据基础类型和长度生成完整的mysql字段类型，如VARCHAR(255)或DECIMAL(18,6)，如果没有长度则返回基础类型
function buildMysqlColumnType(baseType: string, length: string): string {
    if (!baseType) {
        return ''
    }
    if (!length) {
        return baseType
    }
    return `${baseType}(${length})`
}

/**
 * 根据提供的columnType字符串解析出基础类型和长度，并构建完整的mysql字段类型字符串。
 * 如果columnType无效或未提供，则使用fallbackBaseType和默认长度生成完整类型
 * @param columnType 
 * @param fallbackBaseType 
 */
function parseMysqlColumnType(columnType: string | undefined, fallbackBaseType: string): {
    baseType: string
    length: string
    fullType: string
} {
    if (!columnType) {
        const fallbackLength = getDefaultMysqlColumnTypeLength(fallbackBaseType)
        return {
            baseType: fallbackBaseType,
            length: fallbackLength,
            fullType: buildMysqlColumnType(fallbackBaseType, fallbackLength)
        }
    }

    const normalized = columnType.trim()
    const matched = normalized.match(/^([A-Za-z]+)\s*(?:\((.*)\))?$/)
    const parsedBaseType = matched?.[1]?.toUpperCase() || fallbackBaseType
    const parsedLength = matched?.[2]?.trim() || getDefaultMysqlColumnTypeLength(parsedBaseType)

    return {
        baseType: parsedBaseType,
        length: parsedLength,
        fullType: buildMysqlColumnType(parsedBaseType, parsedLength)
    }
}

/**
 * 根据schema名和table名查询是否有已确认的存储映射，如果有则用映射字段自动填充当前映射表单的物理表字段信息，简化配置流程
 */
async function autoFillFromStorageMapping(): Promise<void> {
    try {
        const mapping = await getStorageMapping(
            mappingForm.physicalSchemaName,
            mappingForm.physicalTableName
        )
        // 使用已确认的存储映射数据来覆盖当前表单数据中的物理表字段映射信息，简化配置流程
        if (mapping) {
            if (mapping?.columns?.length !== datatypeFields.value.length) {
                mappingForm.physicalTableName = ''
                ElMessage.warning('所选物理表的存储映射字段数量与当前数据类型字段数量不匹配，无法自动填充映射字段，请手动配置')
                return
            }
            fillMappingForm(mapping)
            markNeedSave()
            ElMessage.success('已根据物理表自动填充映射字段')
        }
    } catch (error) {
        ElMessage.warning('未查到可复用的存储映射，保留当前映射配置')
        console.warn(error)
    }
}

/**
 * 根据已选择的物理字段基础类型和长度，生成完整的物理字段类型字符串，并同步到当前行数据中
 * @param row 
 */
function syncPhysicalColumnType(row: any): void {
    row.physicalColumnType = buildMysqlColumnType(
        row.physicalColumnBaseType,
        row.physicalColumnTypeLength
    )
    markNeedSave()
}

/**
 * 根据已选择的物理字段基础类型，获取默认的长度/精度值，并同步到当前行数据中，同时更新完整的物理字段类型字符串
 * @param row 
 */
function handlePhysicalColumnBaseTypeChange(row: any): void {
    row.physicalColumnTypeLength = getDefaultMysqlColumnTypeLength(
        row.physicalColumnBaseType
    )
    syncPhysicalColumnType(row)
}

/**
 * 重置映射表单的字段映射数据，重新根据数据类型字段生成映射表单初始数据
 */
async function handleResetMappingFromFields(): Promise<void> {
    await ElMessageBox.confirm(
        '确认根据当前模板返回字段重新生成映射？未保存的字段映射修改会被覆盖。',
        '重新生成映射',
        {
            type: 'warning',
            confirmButtonText: '确认',
            cancelButtonText: '取消'
        }
    )
    fillMappingForm()
    ElMessage.success('已根据返回字段重新生成映射')
}

async function handleSaveMappingDraft(): Promise<void> {
    const valid = await mappingFormRef.value?.validate().catch(() => false)
    if (!valid) return

    const invalidColumn = mappingForm.columns.find((item) =>
        !item.physicalColumnName || !item.physicalColumnType
    )
    if (invalidColumn) {
        ElMessage.warning(`请完善字段「${invalidColumn.dataTypeFieldCode}」的物理字段名称和物理字段类型`)
        return
    }

    savingMapping.value = true
    try {
        const payload = buildStorageMappingPayload('SAVE')

        taskDetail.value.storageMapping = await saveStorageMappingDraft(taskCode, payload)
        hasDraft.value = true

        ElMessage.success('映射草稿保存成功')
    } catch (error) {
        ElMessage.error('映射草稿保存失败：' + (error as Error).message)
    } finally {
        savingMapping.value = false
    }
}

async function handleConfirmMapping(): Promise<void> {
    await ElMessageBox.confirm('确认该存储映射？确认后不可直接覆盖。', '确认映射', {
        type: 'warning',
        confirmButtonText: '确认',
        cancelButtonText: '取消'
    })

    if (!hasDraft.value) {
        ElMessage.warning('请先保存映射草稿')
        return
    }

    const payload = {
        ...buildStorageMappingPayload('UPDATE'),
        mappingStatus: 'CONFIRMED' as MappingStatus
    }

    try {
        taskDetail.value.storageMapping = await confirmStorageMapping(taskCode, payload)
        mappingForm.mappingStatus = 'CONFIRMED'
        ElMessage.success('映射确认成功')
        await loadDetail()
    } catch (error) {
        ElMessage.error('映射确认失败：' + (error as Error).message)
        return
    }
}

// 将表单数据组装为StorageMapping对象，准备发送给后端接口
function buildStorageMappingPayload(saveOrUpdate: 'SAVE' | 'UPDATE'): StorageMappingDraftRequest {
    return {
        storageMappingCode: mappingForm.storageMappingCode,
        taskConfigCode: mappingForm.taskConfigCode,
        dataSourceCode: mappingForm.dataSourceCode,
        physicalSchemaName: mappingForm.physicalSchemaName,
        physicalTableName: mappingForm.physicalTableName,
        writeStrategy: mappingForm.writeStrategy,
        mappingStatus: mappingForm.mappingStatus,
        confirmRemark: mappingForm.confirmRemark,
        columns: mappingForm.columns.map((item) => ({
            sourceFieldCode: item.sourceFieldCode,
            sourceFieldName: item.sourceFieldName,
            sourceFieldType: item.sourceFieldType,
            dataTypeFieldCode: item.dataTypeFieldCode,
            dataTypeFieldType: item.dataTypeFieldType,
            physicalColumnName: item.physicalColumnName,
            physicalColumnType: item.physicalColumnType,
            defaultValue: item.defaultValue,
            required: item.required,
            uniqueKey: item.uniqueKey,
        })),
        saveOrUpdate: saveOrUpdate
    }
}

// 将返回字段列表中已选择的字段设置为已选状态
function handleSelectChange(sourceFieldCode: string, row: ColumnMappingForm): void {
    const currentField = taskOutputFields.value.find(
        (field) => field.fieldCode === sourceFieldCode
    )
    if (!currentField || sourceFieldCode === '-') {
        row.sourceFieldCode = ''
        row.sourceFieldName = ''
        row.sourceFieldType = ''
    } else {
        row.sourceFieldCode = currentField.fieldCode
        row.sourceFieldName = currentField.fieldName
        row.sourceFieldType = currentField.fieldType
    }

    markNeedSave()

}

/**
 * 根据DataType字段，以及已有的已经映射的StorageColumnMapping，生成用于表单展示的映射列数据
 * 如果没有storageColumnMapping则用DataType字段的默认值生成映射列数据
 * @param field 
 * @param mappedColumn 
 */
function buildMappingColumn(field: DataTypeField, mappedColumn?: StorageColumnMapping) {
    const baseTypeOptions = MYSQL_BASE_TYPE_OPTIONS_BY_FIELD_TYPE[field.fieldType]

    
    const parsedType = parseMysqlColumnType(
        mappedColumn?.physicalColumnType,
        baseTypeOptions?.[0]?.value || 'VARCHAR'
    )

    return {
        dataTypeFieldCode: field.fieldCode || '',
        dataTypeFieldName: field.fieldName || '',
        dataTypeFieldType: field.fieldType || '',
        required: mappedColumn?.required ?? field.required ?? false,
        uniqueKey: mappedColumn?.uniqueKey ?? field.uniqueKey ?? false,
        sourceFieldCode: mappedColumn?.sourceFieldCode || '',
        sourceFieldName: mappedColumn?.sourceFieldName || '',
        sourceFieldType: mappedColumn?.sourceFieldType || '',
        physicalColumnName: mappedColumn?.physicalColumnName || field.fieldCode || '',
        physicalColumnBaseType: parsedType.baseType,
        physicalColumnTypeLength: parsedType.length,
        physicalColumnType: parsedType.fullType,
        defaultValue: mappedColumn?.defaultValue ?? field.defaultValue ?? 'null',
    }
}

// 标记当前映射配置有未保存的修改，只有在保存过草稿后才允许确认映射，避免直接确认未保存的映射配置
function markNeedSave(): void {
    hasDraft.value = false
}

// 控制每行下拉选项显示：已选字段和当前行字段始终可见，其他字段根据selected状态控制
function isFieldOptionVisible(field: ParamField, row: ColumnMappingForm): boolean {
    // 不映射选项始终可见
    if (field.fieldCode === '-') {
        return true
    }
    // 已选字段和当前行字段始终可见
    if (field.fieldCode === row.sourceFieldCode) {
        return true
    }
    // 字段类型必须匹配才能选择
    if (field.fieldType.toUpperCase() !== row.dataTypeFieldType.toUpperCase()) {
        return false
    }
    // 对于必填字段，已选字段只能选择必填字段；对于非必填字段，已选字段只能选择非必填字段
    if (row.required && !field.required) {
        return false
    }
    // 对于唯一键字段，已选字段只能选择唯一键字段；
    if (row.uniqueKey && !field.uniqueKey) {
        return false
    }
    // 已选择的字段不可见，避免重复选择
    return !selectedSourceFieldCodes.value.has(field.fieldCode)
}

</script>

<style scoped>
.task-config-mapping-page {
    padding: 16px;
}

.page-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    margin-bottom: 16px;
}

.header-left {
    display: flex;
    align-items: center;
    gap: 12px;
    min-width: 0;
}

.page-title {
    font-weight: 600;
    font-size: 16px;
}

.page-content {
    display: flex;
    flex-direction: column;
    gap: 16px;
}

.section-header {
    display: flex; 
    align-items: center;
    justify-content: space-between;
    width: 100%;
}

.section-header .section-title {
    display: flex;
    align-items: flex-start;
}

.section-title {
    font-weight: 600;
}

.section-actions {
    display: flex;
    align-items: center;
    gap: 8px;
}

.mapping-form {
    margin-bottom: 16px;
}

.mapping-form ::v-deep .el-form-item {
    padding: 8px;
}

.mapping-columns-title {
    font-size: 16px;
    margin-bottom: 8px;
}

.mapping-table {
    width: 100%;
}

.mysql-column-type-editor ::v-deep .el-select {
    padding: 10px;
}
</style>