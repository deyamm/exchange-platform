<template>
    <el-dialog :model-value="modelValue" :title="mode === 'create' ? '新增数据主题' : '编辑数据主题'" width="620px" destroy-on-close
        @close="handleClose">
        <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
            <el-form-item label="主题编码" prop="dataTopicCode">
                <el-input v-model="form.dataTopicCode" disabled maxlength="64" />
            </el-form-item>

            <el-form-item label="主题名称" prop="dataTopicName">
                <el-input v-model="form.dataTopicName" maxlength="32" placeholder="请输入数据主题名称" />
            </el-form-item>

            <el-form-item label="主题标签" prop="dataTopicLabel">
                <el-input v-model="form.dataTopicLabel" maxlength="32" placeholder="请输入数据主题标签" />
            </el-form-item>

            <el-form-item label="父级主题" prop="parentCode">
                <el-input v-model="form.parentCode" maxlength="32" disabled />
            </el-form-item>

            <el-form-item label="排序号" prop="sortNo">
                <el-input-number v-model="form.sortNo" :min="0" :max="999999" controls-position="right"
                    style="width: 180px" />
            </el-form-item>

            <!--el-form-item label="状态" prop="status">
                <el-radio-group v-model="form.status">
                    <el-radio-button label="ENABLED">启用</el-radio-button>
                    <el-radio-button label="DISABLED">停用</el-radio-button>
                </el-radio-group>
            </el-form-item-->

            <el-form-item label="说明" prop="description">
                <el-input v-model="form.description" type="textarea" maxlength="255" show-word-limit :rows="4"
                    placeholder="请输入数据主题说明" />
            </el-form-item>
        </el-form>

        <template #footer>
            <el-button @click="handleClose">取消</el-button>
            <el-button type="primary" :loading="saving" @click="handleSubmit">保存</el-button>
        </template>
    </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { saveDataTopic } from '/@/api/datatopic/dataTopicApi'
import { DataTopic, createDefaultDataTopic } from '/@/types/datatopic/entity'
import type { DataTopicFormModel } from '/@/types/datatopic/view'

const props = withDefaults(
    defineProps<{
        modelValue: boolean
        mode?: 'create' | 'edit'
        formData: DataTopic,  // 必会传入一个DataTopic对象
        parentTopic: DataTopic  // 必会传入一个父级DataTopic对象，如果是新增模式则表示要新增的主题的父级，编辑模式则表示当前编辑主题的父级
    }>(),
    {
        mode: 'create',
        formData: () => createDefaultDataTopic(),
    }
)

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'success'): void
}>()

const formRef = ref<FormInstance>()
const saving = ref(false)

const form = reactive<DataTopicFormModel>({
    dataTopicCode: '',
    dataTopicName: '',
    dataTopicLabel: '',
    parentCode: '',
    sortNo: 0,
    status: 'ENABLED',
    description: ''
})

const rules: FormRules = {
    dataTopicName: [
        { required: true, message: '请输入主题名称', trigger: 'blur' },
        { max: 32, message: '主题名称不能超过 32 个字符', trigger: 'blur' }
    ],
    dataTopicLabel: [
        { required: true, message: '请输入主题标签', trigger: 'blur' },
        { pattern: /^[A-Z_]+$/, message: '主题标签只能包含大写字母和下划线', trigger: 'blur' },
        { max: 32, message: '主题标签不能超过 32 个字符', trigger: 'blur' }
    ],
    status: [{ required: true, message: '请选择状态', trigger: 'change' }],
    description: [{ max: 255, message: '说明不能超过 255 个字符', trigger: 'blur' }]
}

watch(
    // 监听 modelValue，当弹窗打开时使用传过来的topic重置表单数据
    () => props.modelValue,
    (visible) => {
        if (visible) {
            resetForm()
        }
    }
)

/**
 * 重置表单数据，如果是编辑模式则使用传过来的数据填充表单，如果是新增模式则使用默认值
 */
function resetForm(): void {
    const source = props.formData

    if (props.mode === 'create') {
        // 新增模式，dataTopicCode由系统生成，这里直接生成可以在页面中展示
        form.dataTopicCode = buildNewTopicCode()
    } else {
        // 编辑模式，dataTopicCode来自source
        form.dataTopicCode = source.dataTopicCode || ''
    }
    form.dataTopicName = source.dataTopicName
    form.dataTopicLabel = source.dataTopicLabel
    form.parentCode = source.parentCode || ''
    form.sortNo = source.sortNo
    form.status = source.status || 'ENABLED'
    form.description = source.description || ''

    setTimeout(() => {
        formRef.value?.clearValidate()
    })
}

function handleClose(): void {
    emit('update:modelValue', false)
}


/* 父节点是根节点：ROOT，获取下一个大写字母编码，对于数据主题，范围为A-N TODO: 还有改进空间*/
function getNextRootCode(children: DataTopic[]): string {
    let maxCode = 'A'
    let found = false

    for (const child of children) {
        const code = (child.dataTopicCode || '').trim().toUpperCase()
        const letter = code.charAt(0)
        if (/^[A-N]$/.test(letter)) {
            if (!found || letter > maxCode) {
                maxCode = letter
            }
            found = true
        }
    }

    if (!found) {
        return 'A'
    }

    if (maxCode === 'N') {
        throw new Error('根节点编码已达上限，无法再添加子节点')
    }

    return String.fromCharCode(maxCode.charCodeAt(0) + 1)
}

/* 父节点不是根节点，获取下一个数字编码，格式为父级编码XX，其中XX是两位数字，表示当前父级下的第几个子主题，子主题编码从00开始 */
function getNextNumericSuffix(parentCode: string, children: DataTopic[]): string {
    let maxNum = -1

    for (const child of children) {
        const code = (child.dataTopicCode || '').trim()
        if (!code.startsWith(parentCode)) {
            continue
        }
        const suffix = code.substring(parentCode.length)
        const parsed = parseInt(suffix, 10)
        if (!Number.isNaN(parsed)) {
            maxNum = Math.max(maxNum, parsed)
        }
    }

    if (maxNum >= 99) {
        throw new Error('子主题编码已达上限，无法再添加子主题')
    }

    return String(maxNum + 1).padStart(2, '0')
}

function buildNewTopicCode(): string {
    const parentCode = props.parentTopic?.dataTopicCode || form.parentCode

    if (!parentCode) return ''

    if (parentCode === 'ROOT') {
        return getNextRootCode(props.parentTopic?.children || [])
    }

    return parentCode + getNextNumericSuffix(parentCode, props.parentTopic?.children || [])
}

async function handleSubmit(): Promise<void> {
    if (!formRef.value) {
        return
    }

    await formRef.value.validate()

    saving.value = true
    try {
        await saveDataTopic({
            dataTopicCode: form.dataTopicCode.trim(),
            dataTopicName: form.dataTopicName.trim(),
            dataTopicLabel: form.dataTopicLabel.trim().toUpperCase(),
            parentCode: form.parentCode || '',
            nodeLevel: props.parentTopic ? (props.parentTopic.nodeLevel || 0) + 1 : 1,
            isLeaf: true,
            sortNo: form.sortNo,
            status: form.status,
            description: form.description?.trim() || '',
            saveOrUpdate: props.mode === 'create' ? 'SAVE' : 'UPDATE',
        })

        ElMessage.success('保存成功')
        emit('success')
    } finally {
        saving.value = false
    }
}
</script>
