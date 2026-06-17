<template>
    <el-dialog :model-value="modelValue" :title="mode === 'create' ? '新增应用场景管理' : '编辑应用场景管理'" width="660px"
        destroy-on-close @close="handleClose">
        <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
            <el-form-item label="编码" prop="sceneCode">
                <el-input v-model="form.sceneCode" :disabled="mode === 'edit'" placeholder="请输入编码" />
            </el-form-item>
            <el-form-item label="名称" prop="sceneName">
                <el-input v-model="form.sceneName" placeholder="请输入名称" />
            </el-form-item>
            <el-form-item label="场景类型">
                <el-select v-model="form.sceneType" placeholder="请选择场景类型" style="width: 180px">
                    <el-option v-for="item in SCENE_TYPE_OPTIONS" :key="item.value" :label="item.label"
                        :value="item.value" />
                </el-select>
            </el-form-item>
            <el-form-item label="排序">
                <el-input-number v-model="form.sortNo" :min="1" :max="999999" controls-position="right" />
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
import { saveOrUpdateAppScene } from '/@/api/appscene/appSceneApi';
import { createDefaultAppScene, type AppScene } from '/@/types/appscene/entity';
import { COMMON_STATUS_OPTIONS, SCENE_TYPE_OPTIONS } from '/@/types/appscene/view';
import { SceneSaveRequest } from '/@/types/appscene/constants';

const props = withDefaults(defineProps<{ 
    modelValue: boolean; 
    mode?: 'create' | 'edit';
    formData?: AppScene 
}>(), {
    mode: 'create',
    formData: () => createDefaultAppScene(),
});
const emit = defineEmits<{ 
    (e: 'update:modelValue', value: boolean): void; 
    (e: 'success'): void 
}>();

const formRef = ref<FormInstance>();
const saving = ref(false);
const form = ref<SceneSaveRequest>({
    sceneCode: '',
    sceneName: '',
    sceneType: 'MARKET_OVERVIEW',
    sortNo: 0,
    status: 'DRAFT',
    description: '',
});

const rules: FormRules = {
    sceneCode: [{ required: true, message: '请输入编码', trigger: 'blur' }],
    sceneName: [{ required: true, message: '请输入名称', trigger: 'blur' }],
    status: [{ required: true, message: '请选择状态', trigger: 'change' }],
};

watch(() => props.modelValue, (visible) => {
    if (visible) Object.assign(form.value, createDefaultAppScene(), props.formData || {});
});

function handleClose() { emit('update:modelValue', false); }
async function handleSubmit() {
    await formRef.value?.validate();
    saving.value = true;
    try {
        await saveOrUpdateAppScene(form.value);
        ElMessage.success('保存成功');
        emit('success');
        handleClose();
    } finally {
        saving.value = false;
    }
}
</script>
