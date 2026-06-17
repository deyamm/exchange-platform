<template>
    <el-dialog :model-value="modelValue" :title="mode === 'create' ? '新增应用指标配置' : '编辑应用指标配置'" width="660px"
        destroy-on-close @close="handleClose">
        <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
            <el-form-item label="编码" prop="metricCode">
                <el-input v-model="form.metricCode" :disabled="mode === 'edit'" placeholder="请输入编码" />
            </el-form-item>
            <el-form-item label="名称" prop="metricName">
                <el-input v-model="form.metricName" placeholder="请输入名称" />
            </el-form-item>
            <el-form-item label="指标分类">
                <el-input v-model="form.metricCategory" placeholder="请输入指标分类" />
            </el-form-item>
            <el-form-item label="单位">
                <el-input v-model="form.unit" placeholder="请输入单位" />
            </el-form-item>
            <el-form-item label="展示格式">
                <el-input v-model="form.displayFormat" placeholder="请输入展示格式" />
            </el-form-item>
            <el-form-item label="数据来源">
                <el-input v-model="form.dataSourceCode" placeholder="请输入数据来源" />
            </el-form-item>
            <el-form-item label="计算周期">
                <el-input v-model="form.calcPeriod" placeholder="请输入计算周期" />
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
import { saveOrUpdateAppMetric } from '/@/api/appmetric/appMetricApi';
import { createDefaultAppMetric, type AppMetric } from '/@/types/appmetric/entity';
import { COMMON_STATUS_OPTIONS } from '/@/types/appscene/view';

const props = withDefaults(
    defineProps<{ 
        modelValue: boolean; 
        mode?: 'create' | 'edit'; 
        formData?: AppMetric 
    }>(), {
        mode: 'create',
        formData: () => createDefaultAppMetric(),
    }
);

const emit = defineEmits<{ 
    (e: 'update:modelValue', value: boolean): void; 
    (e: 'success'): void 
}>();

const formRef = ref<FormInstance>();
const saving = ref(false);
const form = reactive(createDefaultAppMetric());

const rules: FormRules = {
    metricCode: [{ required: true, message: '请输入编码', trigger: 'blur' }],
    metricName: [{ required: true, message: '请输入名称', trigger: 'blur' }],
    status: [{ required: true, message: '请选择状态', trigger: 'change' }],
};

watch(() => props.modelValue, (visible) => {
    if (visible) Object.assign(form, createDefaultAppMetric(), props.formData || {});
});

function handleClose() { 
    emit('update:modelValue', false); 
}
async function handleSubmit() {
    await formRef.value?.validate();
    saving.value = true;
    try {
        await saveOrUpdateAppMetric({ ...form });
        ElMessage.success('保存成功');
        emit('success');
        handleClose();
    } finally {
        saving.value = false;
    }
}
</script>
