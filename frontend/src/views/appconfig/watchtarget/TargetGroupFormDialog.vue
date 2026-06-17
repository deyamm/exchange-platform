<template>
    <el-dialog :model-value="modelValue" :title="mode === 'create' ? '新增标的分组' : '编辑标的分组'" width="600px" destroy-on-close
        @close="handleClose">
        <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
            <el-form-item label="分组编码" prop="groupCode"><el-input v-model="form.groupCode"
                    :disabled="mode === 'edit'" /></el-form-item>
            <el-form-item label="分组名称" prop="groupName"><el-input v-model="form.groupName" /></el-form-item>
            <el-form-item label="排序号"><el-input-number v-model="form.sortNo" :min="1" :max="999999"
                    controls-position="right" /></el-form-item>
            <el-form-item label="状态"><el-select v-model="form.status" style="width:180px"><el-option
                        v-for="item in COMMON_STATUS_OPTIONS" :key="item.value" :label="item.label"
                        :value="item.value" /></el-select></el-form-item>
            <el-form-item label="说明"><el-input v-model="form.description" type="textarea" :rows="4" maxlength="500"
                    show-word-limit /></el-form-item>
        </el-form>
        <template #footer><el-button @click="handleClose">取消</el-button><el-button type="primary" :loading="saving"
                @click="handleSubmit">保存</el-button></template>
    </el-dialog>
</template>
<script setup lang="ts">
import { reactive, ref, watch } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import { ElMessage } from 'element-plus';
import { saveOrUpdateTargetGroup } from '/@/api/watchtarget/watchTargetApi';
import { createDefaultTargetGroup, type TargetGroup } from '/@/types/watchtarget/entity';
import { COMMON_STATUS_OPTIONS } from '/@/types/appscene/view';

const props = withDefaults(
    defineProps<{ 
        modelValue: boolean; 
        mode?: 'create' | 'edit'; 
        formData?: TargetGroup 
    }>(), { 
        mode: 'create', 
        formData: () => createDefaultTargetGroup() 
    }
);

const emit = defineEmits<{ 
    (e: 'update:modelValue', value: boolean): void; 
    (e: 'success'): void 
}>();

const formRef = ref<FormInstance>(); 
const saving = ref(false); 
const form = reactive(createDefaultTargetGroup());

const rules: FormRules = { 
    groupCode: [{ required: true, message: '请输入分组编码', trigger: 'blur' }], 
    groupName: [{ required: true, message: '请输入分组名称', trigger: 'blur' }] 
};

watch(
    () => props.modelValue, 
    (visible) => { 
        if (visible) 
            Object.assign(form, createDefaultTargetGroup(), props.formData || {}); 
    }
);

function handleClose() { 
    emit('update:modelValue', false); 
}

async function handleSubmit() { 
    await formRef.value?.validate(); 
    saving.value = true; 
    try { 
        await saveOrUpdateTargetGroup({ ...form }); 
        ElMessage.success('保存成功'); 
        emit('success'); 
        handleClose(); 
    } finally { 
        saving.value = false; 
    } 
}

</script>
