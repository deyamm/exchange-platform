<template>
    <el-dialog :model-value="modelValue" :title="mode === 'create' ? '新增数据类型' : '编辑数据类型'" width="560px" destroy-on-close @close="handleClose">
        <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
            <el-form-item label="类型编码" prop="dataTypeCode">
                <el-input v-model="form.dataTypeCode" disabled />
            </el-form-item>
            <el-form-item label="类型名称" prop="dataTypeName">
                <el-input v-model="form.dataTypeName" placeholder="如 行情日线数据" />
            </el-form-item>
            <el-form-item label="类型标签" prop="dataTypeLabel">
                <el-input v-model="form.dataTypeLabel" placeholder="如 MARKET_DAILY_QUOTE" />
            </el-form-item>
            <el-form-item label="父级编码">
                <el-input v-model="form.parentCode" clearable placeholder="为空表示根节点"  disabled/>
            </el-form-item>
            <el-form-item label="节点类型" prop="nodeType">
                <el-select v-model="form.nodeType" style="width: 100%" :disabled="props.formData.versionCount > 0">
                    <el-option v-for="item in DATA_TYPE_NODE_TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
            </el-form-item>
            <!--el-form-item label="节点层级">
                <el-input-number v-model="form.nodeLevel" :min="1" :max="10" style="width: 100%" />
            </el-form-item-->
            <el-form-item label="排序号">
                <el-input-number v-model="form.sortNo" :min="1" style="width: 100%" />
            </el-form-item>
            <!--el-form-item label="状态" prop="status">
                <el-select v-model="form.status" style="width: 100%">
                    <el-option v-for="item in COMMON_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
            </el-form-item-->
            <el-form-item label="说明">
                <el-input v-model="form.description" type="textarea" :rows="3" maxlength="255" show-word-limit />
            </el-form-item>
        </el-form>
        <template #footer>
            <el-button @click="handleClose">取消</el-button>
            <el-button type="primary" :loading="saving" @click="handleSubmit">保存</el-button>
        </template>
    </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { saveDataType } from '/@/api/datatype/dataTypeApi'
import { DATA_TYPE_NODE_TYPE_OPTIONS, DataTypeFormModel } from '/@/types/datatype/view'
import { createDefaultDataType, DataType } from '/@/types/datatype/entity'

const props = withDefaults(
    defineProps<{ 
        modelValue: boolean; 
        mode?: 'create' | 'edit'; 
        formData: DataType; 
        parentType: DataType 
    }>(),
    {
        mode: 'create',
        formData: () => createDefaultDataType(),
        parentType: () => createDefaultDataType()
    }
)

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'success'): void
}>()

const formRef = ref<FormInstance>()
const saving = ref(false)
const form = ref<DataTypeFormModel>({
    dataTypeCode: '',
    dataTypeName: '',
    dataTypeLabel: '',
    parentCode: '',
    nodeType: 'CONCRETE',
    sortNo: 0,
    status: 'ENABLED',
    description: ''
})

const rules: FormRules = {
    dataTypeName: [
        { required: true, message: '请输入类型名称', trigger: 'blur' },
        { max: 32, message: '类型名称不能超过32个字符', trigger: 'blur' }
    ],
    dataTypeLabel: [
        { required: true, message: '请输入类型标签', trigger: 'blur' },
        { pattern: /^[A-Z_]+$/, message: '类型标签只能包含大写字母和下划线', trigger: 'blur' },
        { max: 32, message: '类型标签不能超过 32 个字符', trigger: 'blur' }
    ],
    nodeType: [{ required: true, message: '请选择节点类型', trigger: 'change' }],
    // status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

watch(() => props.modelValue, (visible) => {
    if (visible) resetForm()
})

/** 根据传入对象重置表单。 */
function resetForm(): void {
    const source = props.formData

    if (props.mode === 'create') {
        // 新增模式，dataTypeCode由系统生成，这里直接生成可以在页面中展示
        form.value.dataTypeCode = buildNewDataTypeCode()
    } else {
        // 编辑模式，dataTypeCode来自source
        form.value.dataTypeCode = source.dataTypeCode || ''
    }
    form.value.dataTypeName = source.dataTypeName || ''
    form.value.dataTypeLabel = source.dataTypeLabel || ''
    form.value.parentCode = source.parentCode || ''
    form.value.nodeType = source.nodeType || 'CONCRETE'
    form.value.sortNo = source.sortNo || 0
    form.value.status = source.status || 'ENABLED'

    setTimeout(() => {
        formRef.value?.clearValidate()
    })
}

function handleClose(): void {
    emit('update:modelValue', false)
}

/* 父节点是根节点：ROOT，获取下一个大写字母编码，对于数据主题，范围为O-Z TODO: 还有改进空间*/
function getNextRootCode(children: DataType[]): string {
    let maxCode = 'O'
    let found = false

    for (const child of children) {
        const code = (child.dataTypeCode || '').trim().toUpperCase()
        const letter = code.charAt(0)
        if (/^[A-Z]$/.test(letter)) {
            if (!found || letter > maxCode) {
                maxCode = letter
            }
            found = true
        }
    }

    if (!found) {
        return 'O'
    }

    if (maxCode === 'Z') {
        throw new Error('根节点编码已达上限，无法再添加子节点')
    }

    return String.fromCharCode(maxCode.charCodeAt(0) + 1)
}

/* 父节点不是根节点，获取下一个数字编码，格式为父级编码XX，其中XX是两位数字，表示当前父级下的第几个子主题，子主题编码从00开始 */
function getNextNumericSuffix(parentCode: string, children: DataType[]): string {
    let maxNum = -1

    for (const child of children) {
        const code = (child.dataTypeCode || '').trim()
        if (!code.startsWith(parentCode)) continue
        const suffix = code.substring(parentCode.length)
        const parsed = parseInt(suffix, 10)
        if (!Number.isNaN(parsed)) {
            maxNum = Math.max(maxNum, parsed)
        }
    }

    if (maxNum >= 99) {
        throw new Error('子节点编码已达上限，无法再添加子节点')
    }

    return String(maxNum + 1).padStart(2, '0')
}

function buildNewDataTypeCode(): string {
    const parentCode = props.parentType?.dataTypeCode || form.value.parentCode
    if (!parentCode) return ''

    if (parentCode === 'ROOT') {
        return getNextRootCode(props.parentType?.children || [])
    }

    return parentCode + getNextNumericSuffix(parentCode, props.parentType?.children || [])
}

/** 提交数据类型基础信息。 */
async function handleSubmit(): Promise<void> {
    if (!formRef.value) return
    await formRef.value.validate()
    saving.value = true
    try {
        await saveDataType({
            dataTypeCode: form.value.dataTypeCode.trim(),
            dataTypeName: form.value.dataTypeName.trim(),
            dataTypeLabel: form.value.dataTypeLabel.trim().toUpperCase(),
            parentCode: form.value.parentCode || '',
            nodeType: form.value.nodeType,
            sortNo: form.value.sortNo,
            nodeLevel: props.parentType ? (props.parentType.nodeLevel || 0) + 1 : 1,
            isLeaf: true, // 目前只支持叶子节点，后续如果需要支持非叶子节点再修改
            status: form.value.status,
            description: form.value.description.trim(),
            saveOrUpdate: props.mode === 'create' ? 'SAVE' : 'UPDATE'
        })
        ElMessage.success('保存成功')
        emit('success')
    } finally {
        saving.value = false
    }
}
</script>
