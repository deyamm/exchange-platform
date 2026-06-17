<template>
    <el-dialog
        :model-value="modelValue"
        :title="isCreate ? '新增任务配置' : '修改任务配置'"
        width="680px"
        destroy-on-close
        @update:model-value="$emit('update:modelValue', $event)"
    >
        <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            label-width="130px"
            class="config-form"
        >   
            <!-- 配置编码 -->
            <el-form-item label="配置编码" prop="taskConfigCode">
                <el-input
                    v-model="form.taskConfigCode"
                    :disabled="!isCreate"
                    placeholder="如 TC_MARKET_DAILY_01"
                />
            </el-form-item>

            <el-form-item label="任务模板编码" prop="taskCode">
                <el-input
                    v-model="form.taskCode"
                    :disabled=true
                />
            </el-form-item>

            <!-- 模板版本号 -->
            <el-form-item label="模板版本" prop="taskTemplateVersionNo">
                <el-input
                    v-model="form.taskTemplateVersionNo"
                    :disabled=true
                />
            </el-form-item>

            <!-- 数据主题 -->
            <el-form-item label="数据主题" prop="dataTopicCode">
                <el-tree-select
                    v-model="form.dataTopicCode"
                    :data="dataTopicOptions"
                    :props="treeSelectProps"
                    :loading="optionLoading"
                    node-key="value"
                    value-key="value"
                    check-strictly
                    filterable
                    clearable
                    :render-after-expand="false"
                    placeholder="请选择数据主题（仅叶子节点可选）"
                    style="width: 100%"
                    @change="handleDataTopicChange"
                    :disabled="form.configStatus === 'CONFIRMED'"
                />
            </el-form-item>

            <!-- 数据类型 -->
            <el-form-item label="数据类型" prop="dataTypeCode">
                <el-tree-select
                    v-model="form.dataTypeCode"
                    :data="dataTypeOptions"
                    :props="treeSelectProps"
                    :loading="optionLoading"
                    node-key="value"
                    value-key="value"
                    check-strictly
                    filterable
                    clearable
                    :render-after-expand="false"
                    placeholder="请选择数据类型（仅叶子节点可选）"
                    style="width: 100%"
                    @change="handleDataTypeChange"
                    :disabled="form.configStatus === 'CONFIRMED'"
                />
            </el-form-item>

            <!-- 绑定规则 -->
            <el-form-item label="绑定规则">
                <el-input
                    v-model="ruleCodesInput"
                    placeholder="规则编码，多个以英文逗号分隔"
                />
            </el-form-item>

            <!-- 配置状态 -->
            <el-form-item label="配置状态" prop="configStatus">
                <el-select v-model="form.configStatus" style="width: 100%">
                    <el-option label="草稿" value="DRAFT" />
                    <el-option label="已确认" value="CONFIRMED" />
                    <el-option label="已启用" value="ENABLED" />
                </el-select>
            </el-form-item>

            <!-- 配置说明 -->
            <el-form-item label="配置说明">
                <el-input
                    v-model="form.description"
                    type="textarea"
                    :rows="3"
                    placeholder="请输入配置说明"
                />
            </el-form-item>
        </el-form>

        <template #footer>
            <el-button @click="$emit('update:modelValue', false)">取消</el-button>
            <el-button type="primary" :loading="saving" @click="handleSubmit">保存</el-button>
        </template>
    </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import type { FormInstance } from 'element-plus'
import { ElMessage } from 'element-plus'

import type { TaskConfig, TaskTemplate } from '/@/types/collectiontask/entity'
import type { TaskConfigSaveRequest } from '/@/types/collectiontask/constants'
import type { DataTopic } from '/@/types/datatopic/entity'
import type { DataType } from '/@/types/datatype/entity'
import { saveTaskConfig } from '/@/api/collectiontask/collectionTaskApi'
import { queryDataTopicTree } from '/@/api/datatopic/dataTopicApi'
import { queryDataTypeTree } from '/@/api/datatype/dataTypeApi'

interface TreeSelectOption {
    value: string
    label: string
    disabled: boolean
    version?: number
    children?: TreeSelectOption[]
}

const props = defineProps<{
    modelValue: boolean
    taskTemplate: TaskTemplate
    existingConfig: TaskConfig | null
}>()

const emit = defineEmits<{
    (e: 'update:modelValue', v: boolean): void
    (e: 'success'): void
}>()

const formRef = ref<FormInstance>()
const saving  = ref(false)
const optionLoading = ref(false)

const dataTopicOptions = ref<TreeSelectOption[]>([])
const dataTypeOptions = ref<TreeSelectOption[]>([])

const treeSelectProps = {
    value: 'value',
    label: 'label',
    children: 'children',
    disabled: 'disabled'
}

const isCreate = computed(() => !props.existingConfig?.taskConfigCode)

const form = reactive<TaskConfigSaveRequest>({
    taskConfigCode: '',
    taskCode: props.taskTemplate.taskCode,
    taskTemplateVersionNo: 1,
    dataTopicCode: '',
    dataTypeCode: '',
    dataTypeVersionNo: 1,
    storageMappingCode: '',
    ruleCodes: [],
    configStatus: 'DRAFT',
    status: 'DISABLED',
    description: '',
    saveOrUpdate: isCreate.value ? 'SAVE' : 'UPDATE'
})

/** 规则编码输入（逗号分隔的字符串） */
const ruleCodesInput = ref('')

const rules = {
    taskConfigCode: [{ required: true, message: '请填写配置编码' }],
    taskTemplateVersionNo: [{ required: true, message: '请选择模板版本' }],
    dataTopicCode: [{ required: true, message: '请选择数据主题' }],
    dataTypeCode: [{ required: true, message: '请选择数据类型' }],
    dataTypeVersionNo: [{ required: true, message: '数据类型版本将由数据类型自动带出' }],
    configStatus: [{ required: true, message: '请选择配置状态' }]
}

watch(() => props.taskTemplate.taskCode, (taskCode) => {
    form.taskCode = taskCode
}, { immediate: true })

// 弹窗打开时加载下拉数据，并回填现有配置
watch(
    () => props.modelValue, 
    async (visible) => {
        if (!visible) return

        if (props.existingConfig) {
            fillForm(props.existingConfig)
        } else {
            resetForm()
        }

        await loadDialogOptions()
    }, { 
        immediate: false 
    }
)

function resetForm(): void {
    Object.assign(form, {
        taskConfigCode: '',
        taskCode: props.taskTemplate.taskCode,
        taskTemplateVersionNo: props.taskTemplate.currentVersionNo || 1,
        dataTopicCode: '',
        dataTypeCode: '',
        dataTypeVersionNo: 1,
        storageMappingCode: '',
        ruleCodes: [],
        configStatus: 'DRAFT',
        status: 'DISABLED',
        description: '',
        saveOrUpdate: isCreate.value ? 'SAVE' : 'UPDATE'
    })
    ruleCodesInput.value = ''
    formRef.value?.clearValidate()
}

function fillForm(config: TaskConfig): void {
    Object.assign(form, {
        taskConfigCode: config.taskConfigCode,
        taskCode: config.taskCode,
        taskTemplateVersionNo: config.taskTemplateVersionNo || props.taskTemplate.currentVersionNo || 1,
        dataTopicCode: config.dataTopicCode || '',
        dataTypeCode: config.dataTypeCode || '',
        dataTypeVersionNo: config.dataTypeVersionNo || 1,
        storageMappingCode: config.storageMappingCode || '',
        configStatus: config.configStatus,
        status: config.status,
        description: config.description || '',
        saveOrUpdate: isCreate.value ? 'SAVE' : 'UPDATE'
    })

    // 解析规则编码列表
    try {
        const codes = JSON.parse(config.ruleCodesJson || '[]')
        ruleCodesInput.value = Array.isArray(codes) ? codes.join(', ') : ''
    } catch {
        ruleCodesInput.value = ''
    }

    formRef.value?.clearValidate()
}

async function loadDialogOptions(): Promise<void> {
    if (!props.taskTemplate.taskCode) return

    optionLoading.value = true
    try {
        const [topics, dataTypes] = await Promise.all([
            queryDataTopicTree({}),
            queryDataTypeTree({})
        ])

        dataTopicOptions.value = buildDataTopicOptions(Array.isArray(topics) ? topics : [])
        dataTypeOptions.value = buildDataTypeOptions(Array.isArray(dataTypes) ? dataTypes : [])
    } finally {
        optionLoading.value = false
    }
}

function buildDataTopicOptions(list: DataTopic[]): TreeSelectOption[] {
    return list.map(item => {
        const children = buildDataTopicOptions(item.children || [])

        return {
            value: item.dataTopicCode,
            label: `${item.dataTopicName || item.dataTopicLabel || item.dataTopicCode}（${item.dataTopicCode}）`,
            disabled: !item.isLeaf,
            children: children.length ? children : undefined
        }
    })
}

function buildDataTypeOptions(list: DataType[]): TreeSelectOption[] {
    return list.map(item => {
        const children = buildDataTypeOptions(item.children || [])

        return {
            value: item.dataTypeCode,
            label: `${item.dataTypeName || item.dataTypeLabel || item.dataTypeCode}（${item.dataTypeCode} - V${item.version}）`,
            disabled: !item.isLeaf,
            version: item.version,
            children: children.length ? children : undefined
        }
    })
}

function findTreeOption(list: TreeSelectOption[], value: string): TreeSelectOption | undefined {
    for (const item of list) {
        if (item.value === value) return item

        const matched = item.children?.length ? findTreeOption(item.children, value) : undefined
        if (matched) return matched
    }

    return undefined
}

function isSelectableLeaf(list: TreeSelectOption[], value: string): boolean {
    const option = findTreeOption(list, value)
    return Boolean(option && !option.disabled)
}

function handleDataTopicChange(value: string | number | boolean | undefined): void {
    const dataTopicCode = value === undefined || value === null ? '' : String(value)
    if (!dataTopicCode) return

    if (!isSelectableLeaf(dataTopicOptions.value, dataTopicCode)) {
        form.dataTopicCode = ''
        ElMessage.warning('数据主题只能选择叶子节点')
    }
}

function handleDataTypeChange(value: string | number | boolean | undefined): void {
    const dataTypeCode = value === undefined || value === null ? '' : String(value)

    if (!dataTypeCode) {
        form.dataTypeVersionNo = 1
        return
    }

    const selected = findTreeOption(dataTypeOptions.value, dataTypeCode)
    if (!selected || selected.disabled) {
        form.dataTypeCode = ''
        form.dataTypeVersionNo = 1
        ElMessage.warning('数据类型只能选择叶子节点')
        return
    }

    form.dataTypeVersionNo = selected.version || 1
}

async function handleSubmit(): Promise<void> {
    const valid = await formRef.value?.validate().catch(() => false)
    if (!valid) return

    if (!isSelectableLeaf(dataTopicOptions.value, form.dataTopicCode)) {
        ElMessage.warning('请选择叶子节点数据主题')
        return
    }

    const selectedDataType = findTreeOption(dataTypeOptions.value, form.dataTypeCode)
    if (!selectedDataType || selectedDataType.disabled) {
        ElMessage.warning('请选择叶子节点数据类型')
        return
    }
    form.dataTypeVersionNo = selectedDataType.version || 1

    // 将规则编码字符串解析为数组
    form.ruleCodes = ruleCodesInput.value
        ? ruleCodesInput.value.split(',').map(s => s.trim()).filter(Boolean)
        : []
    form.taskCode = props.taskTemplate.taskCode

    saving.value = true
    try {
        await saveTaskConfig(form)
        ElMessage.success('配置保存成功')
        emit('success')
    } finally {
        saving.value = false
    }
}
</script>

<style scoped>
.config-form {
    padding: 0 20px;
}
</style>