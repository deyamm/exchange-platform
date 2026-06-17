<template>
    <el-dialog :model-value="modelValue" :title="mode === 'create' ? '新增提醒规则' : '编辑提醒规则'" width="760px" destroy-on-close
        @close="handleClose">
        <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
            <el-form-item label="规则编码" prop="ruleCode"><el-input v-model="form.ruleCode" :disabled="mode === 'edit'"
                    placeholder="如 ALERT_PRICE_000001" /></el-form-item>
            <el-form-item label="规则名称" prop="ruleName"><el-input v-model="form.ruleName" /></el-form-item>
            <el-form-item label="规则类型"><el-select v-model="form.ruleType" style="width:180px"><el-option
                        v-for="item in RULE_TYPE_OPTIONS" :key="item.value" :label="item.label"
                        :value="item.value" /></el-select></el-form-item>
            <el-form-item label="标的编码"><el-input v-model="form.targetCode" /></el-form-item>
            <el-form-item label="分组编码"><el-input v-model="form.groupCode" /></el-form-item>
            <el-form-item label="指标编码"><el-input v-model="form.metricCode" /></el-form-item>
            <el-form-item label="触发条件JSON"><el-input v-model="conditionText" type="textarea" :rows="4"
                    placeholder='{"compareOperator":"GREATER_EQUAL","thresholdValue":12.5}' /></el-form-item>
            <el-form-item label="通知渠道"><el-select v-model="form.noticeChannels" multiple filterable allow-create
                    default-first-option style="width:100%"><el-option label="站内" value="IN_APP" /><el-option label="邮件"
                        value="EMAIL" /><el-option label="企业微信" value="WECHAT_WORK" /></el-select></el-form-item>
            <el-form-item label="有效开始"><el-date-picker v-model="form.effectiveStartTime" type="datetime"
                    value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item>
            <el-form-item label="有效结束"><el-date-picker v-model="form.effectiveEndTime" type="datetime"
                    value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item>
            <el-form-item label="状态"><el-select v-model="form.status" style="width:180px"><el-option
                        v-for="item in COMMON_STATUS_OPTIONS" :key="item.value" :label="item.label"
                        :value="item.value" /></el-select></el-form-item>
            <el-form-item label="说明"><el-input v-model="form.description" type="textarea" :rows="3" maxlength="500"
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
import { saveOrUpdateAlertRule } from '/@/api/alertrule/alertRuleApi';
import { createDefaultAlertRule, type AlertRule } from '/@/types/alertrule/entity';
import { RULE_TYPE_OPTIONS } from '/@/types/alertrule/view';
import { COMMON_STATUS_OPTIONS } from '/@/types/appscene/view';

const props = withDefaults(
    defineProps<{ 
        modelValue: boolean; 
        mode?: 'create' | 'edit'; 
        formData?: AlertRule 
    }>(), { 
        mode: 'create', 
        formData: () => createDefaultAlertRule() 
    }
);

const emit = defineEmits<{ 
    (e: 'update:modelValue', value: boolean): void; 
    (e: 'success'): void 
}>();

const formRef = ref<FormInstance>(); 
const saving = ref(false); 
const form = reactive(createDefaultAlertRule()); 
const conditionText = ref('{}');
const rules: FormRules = { 
    ruleCode: [{ required: true, message: '请输入规则编码', trigger: 'blur' }], 
    ruleName: [{ required: true, message: '请输入规则名称', trigger: 'blur' }] 
};

watch(
    () => props.modelValue, 
    (visible) => { 
        if (visible) { 
            Object.assign(form, createDefaultAlertRule(), props.formData || {}); 
            conditionText.value = JSON.stringify(form.condition || {}, null, 2); 
        } 
    }
);

function parseCondition() { 
    try { 
        return conditionText.value ? JSON.parse(conditionText.value) : {}; 
    } catch { 
        return {}; 
    } 
}

function handleClose() {
    emit('update:modelValue', false); 
}

async function handleSubmit() { 
    await formRef.value?.validate(); 
    saving.value = true; 
    try { 
        await saveOrUpdateAlertRule({ 
            ...form, 
            condition: parseCondition() 
        }); 
        ElMessage.success('保存成功'); 
        emit('success'); 
        handleClose(); 
    } finally { 
        saving.value = false; 
    } 
}
</script>
