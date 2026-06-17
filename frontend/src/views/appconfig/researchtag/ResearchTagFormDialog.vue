<template>
    <el-dialog :model-value="modelValue" :title="mode === 'create' ? '新增研究标签管理' : '编辑研究标签管理'" width="660px"
        destroy-on-close @close="handleClose">
        <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
            <el-form-item label="编码" prop="tagCode">
                <el-input v-model="form.tagCode" :disabled="mode === 'edit'" placeholder="请输入编码" />
            </el-form-item>
            <el-form-item label="名称" prop="tagName">
                <el-input v-model="form.tagName" placeholder="请输入名称" />
            </el-form-item>
            <el-form-item label="标签分类">
                <el-input v-model="form.tagCategory" placeholder="请输入标签分类" />
            </el-form-item>
            <el-form-item label="状态" prop="status">
                <el-select v-model="form.status" style="width: 180px">
                    <el-option v-for="item in COMMON_STATUS_OPTIONS" :key="item.value" :label="item.label"
                        :value="item.value" />
                </el-select>
            </el-form-item>
            <el-form-item label="说明">
                <el-input v-model="form.description" type="textarea" :rows="4" maxlength="500" show-word-limit />
            </el-form-item>
        </el-form>
        <template #footer>
            <el-button @click="handleClose">取消</el-button>
            <el-button type="primary" :loading="saving" @click="handleSubmit">保存</el-button>
        </template>
    </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import { ElMessage } from 'element-plus';
import { saveOrUpdateResearchTag } from '/@/api/researchtag/researchTagApi';
import { createDefaultResearchTag, type ResearchTag } from '/@/types/researchTag/entity';
import { COMMON_STATUS_OPTIONS } from '/@/types/appscene/view';

const props = withDefaults(
    defineProps<{ 
        modelValue: boolean; 
        mode?: 'create' | 'edit'; 
        formData?: ResearchTag 
    }>(), {
        mode: 'create',
        formData: () => createDefaultResearchTag(),
    }
);
const emit = defineEmits<{ 
    (e: 'update:modelValue', value: boolean): void; 
    (e: 'success'): void 
}>();
const formRef = ref<FormInstance>();
const saving = ref(false);
const form = reactive(createDefaultResearchTag());

const rules: FormRules = {
    tagCode: [{ required: true, message: '请输入编码', trigger: 'blur' }],
    tagName: [{ required: true, message: '请输入名称', trigger: 'blur' }],
    status: [{ required: true, message: '请选择状态', trigger: 'change' }],
};

watch(() => props.modelValue, (visible) => {
    if (visible) Object.assign(form, createDefaultResearchTag(), props.formData || {});
});

function handleClose() { 
    emit('update:modelValue', false); 
}

async function handleSubmit() {
    await formRef.value?.validate();
    saving.value = true;
    try {
        await saveOrUpdateResearchTag({ ...form });
        ElMessage.success('保存成功');
        emit('success');
        handleClose();
    } finally {
        saving.value = false;
    }
}
</script>
