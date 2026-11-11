<template>
  <el-dialog :model-value="open" :title="formData?.ruleCode ? '编辑指标提醒' : '新建指标提醒'" width="620px" destroy-on-close @close="close">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
      <el-form-item label="规则名称" prop="ruleName">
        <el-input v-model="form.ruleName" placeholder="例如 ROE 高于 15%" />
      </el-form-item>
      <el-form-item label="标的" prop="targetCode">
        <el-select v-if="!lockedTarget" v-model="form.targetType" style="width: 100%; margin-bottom: 8px" @change="onTargetTypeChange">
          <el-option label="股票" value="STOCK" /><el-option label="指数" value="INDEX" />
        </el-select>
        <div v-if="lockedTarget" class="locked-target">{{ lockedTarget.targetName }} {{ lockedTarget.targetCode }}</div>
        <TargetSearchBox v-else v-model="selectedTarget" :target-type="form.targetType as TargetType" placeholder="搜索标的代码或名称" @select="onTargetSelect" />
      </el-form-item>
      <el-form-item label="指标" prop="metricCode">
        <div class="metric-picker">
          <el-tag v-if="selectedMetric" closable @close="clearMetric">{{ selectedMetric.metricName }}</el-tag>
          <span v-else class="placeholder">请选择一个数值指标</span>
          <el-button link type="primary" @click="metricDialogVisible = true">选择</el-button>
        </div>
      </el-form-item>
      <el-form-item label="触发条件" prop="threshold">
        <div class="condition-row">
          <el-select v-model="form.operator" style="width: 112px">
            <el-option v-for="item in OPERATORS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <el-input-number v-model="form.threshold" :controls="false" :precision="8" style="flex: 1" />
          <span class="unit">{{ selectedMetric?.unit || '' }}</span>
        </div>
      </el-form-item>
      <el-form-item label="有效期">
        <el-date-picker v-model="form.effectiveStartTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" placeholder="长期有效可留空" />
      </el-form-item>
      <el-form-item label="状态">
        <el-radio-group v-model="form.status"><el-radio value="ENABLED">启用</el-radio><el-radio value="DISABLED">停用</el-radio></el-radio-group>
      </el-form-item>
    </el-form>
    <MetricSelector :open="metricDialogVisible" :target-type="form.targetType as TargetType" selection-mode="SINGLE" :allowed-value-types="NUMERIC_TYPES" :selected-metric-codes="form.metricCode ? [form.metricCode] : []" @confirm="selectMetric" @cancel="metricDialogVisible = false" />
    <template #footer><el-button @click="close">取消</el-button><el-button type="primary" :loading="saving" @click="submit">保存</el-button></template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import TargetSearchBox from './TargetSearchBox.vue'
import MetricSelector from './MetricSelector.vue'
import { querySelectableMetrics, type SelectableMetric } from '/@/api/commonSelectorApi'
import { saveOrUpdateAlertRule } from '/@/api/alertrule/alertRuleApi'
import { createDefaultAlertRule, type AlertRule } from '/@/types/alertrule/entity'
import type { TargetType } from '/@/types/targetScope'
import type { WatchTarget } from '/@/types/watchtarget/entity'

const NUMERIC_TYPES = ['INTEGER', 'DECIMAL', 'NUMBER', 'NUMERIC', 'FLOAT', 'DOUBLE']
const OPERATORS = [
  { label: '大于', value: 'GT' }, { label: '大于等于', value: 'GTE' },
  { label: '小于', value: 'LT' }, { label: '小于等于', value: 'LTE' },
  { label: '等于', value: 'EQ' }, { label: '不等于', value: 'NE' },
] as const

const props = withDefaults(defineProps<{
  open: boolean
  targetType?: TargetType
  targetCode?: string
  targetName?: string
  formData?: AlertRule
}>(), { targetType: 'STOCK' })
const emit = defineEmits<{ (e: 'update:open', value: boolean): void; (e: 'success'): void }>()
const formRef = ref<FormInstance>()
const saving = ref(false)
const metricDialogVisible = ref(false)
const selectedTarget = ref<WatchTarget | null>(null)
const metrics = ref<SelectableMetric[]>([])
const form = reactive<AlertRule>(createDefaultAlertRule())
const lockedTarget = computed(() => props.targetCode ? { targetCode: props.targetCode, targetName: props.targetName || props.targetCode } : null)
const selectedMetric = computed(() => metrics.value.find((item) => item.metricCode === form.metricCode))
const rules: FormRules = {
  ruleName: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  targetCode: [{ required: true, message: '请选择标的', trigger: 'change' }],
  metricCode: [{ required: true, message: '请选择指标', trigger: 'change' }],
  threshold: [{ required: true, message: '请输入阈值', trigger: 'change' }],
}

watch(() => props.open, async (open) => {
  if (!open) return
  Object.assign(form, createDefaultAlertRule(), props.formData || {})
  const condition = (props.formData?.condition || {}) as { operator?: AlertRule['operator']; threshold?: number; compareOperator?: string; thresholdValue?: number }
  if (!props.formData?.operator && (condition.operator || condition.compareOperator)) form.operator = (condition.operator || condition.compareOperator) as AlertRule['operator']
  if (props.formData?.threshold === undefined && (condition.threshold !== undefined || condition.thresholdValue !== undefined)) form.threshold = condition.threshold ?? condition.thresholdValue
  selectedTarget.value = null
  if (props.targetCode) form.targetCode = props.targetCode
  form.targetType = props.targetType
  await loadMetrics()
})

async function loadMetrics() {
  const page = await querySelectableMetrics({ targetType: form.targetType as TargetType, allowedValueTypes: NUMERIC_TYPES, pageNo: 1, pageSize: 200 })
  metrics.value = page.records || []
}
function onTargetSelect(target: WatchTarget) { selectedTarget.value = target; form.targetCode = target.targetCode }
function selectMetric(codes: string[]) { form.metricCode = codes[0] || ''; metricDialogVisible.value = false }
function clearMetric() { form.metricCode = '' }
function close() { emit('update:open', false) }
async function submit() {
  await formRef.value?.validate()
  saving.value = true
  try {
    const ruleCode = form.ruleCode || `METRIC_${Date.now()}`
    await saveOrUpdateAlertRule({ ...form, ruleCode, ruleType: 'METRIC', targetType: form.targetType, condition: { operator: form.operator, threshold: form.threshold }, operator: form.operator, threshold: form.threshold })
    ElMessage.success('保存成功'); emit('success'); close()
  } finally { saving.value = false }
}
function onTargetTypeChange() { form.targetCode = ''; form.metricCode = ''; selectedTarget.value = null; void loadMetrics() }
</script>

<style scoped>
.metric-picker, .condition-row { display: flex; align-items: center; gap: 8px; width: 100%; }
.placeholder { color: var(--el-text-color-secondary); }
.locked-target { width: 100%; padding: 8px 12px; background: var(--el-fill-color-light); border-radius: 4px; }
.unit { min-width: 28px; color: var(--el-text-color-secondary); }
</style>
