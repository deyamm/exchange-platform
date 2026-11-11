<template>
  <div class="analysis-page layout-pd">
    <div class="page-heading"><div><h2>标的对比</h2><p>多个同类型标的的多指标客观横向比较。</p></div><el-radio-group v-model="targetType" @change="resetTargets"><el-radio-button label="STOCK">股票</el-radio-button><el-radio-button label="INDEX">指数</el-radio-button></el-radio-group></div>
    <el-card shadow="never" class="config-card"><section><div class="section-title">对比标的</div><div class="search-row"><TargetSearchBox :target-type="targetType" v-model="searchTarget" @select="addTarget" /><el-tag v-for="target in targets" :key="target.targetCode" closable @close="removeTarget(target.targetCode)">{{ target.targetCode }} {{ target.targetName }}</el-tag></div></section><section class="config-section"><div class="section-title">截至时间</div><AsOfTimePicker v-model="asOfTime" label="" /></section><section class="config-section"><div class="section-title">对比指标</div><div class="metric-tags"><el-tag v-for="metric in selectedMetrics" :key="metric.metricCode" closable @close="removeMetric(metric.metricCode)">{{ metric.metricName }}</el-tag><el-button link type="primary" @click="metricDialogVisible = true">修改指标</el-button></div></section><div class="actions"><el-button type="primary" :loading="loading" @click="execute">生成对比</el-button></div></el-card>
    <el-card shadow="never" class="result-card"><template #header><div class="result-header"><span>对比矩阵</span><el-button :disabled="!executed" @click="save">保存结果</el-button></div></template><el-alert v-if="!executed" title="选择至少两个同类型标的和一个指标" type="info" :closable="false" show-icon /><el-table v-else :data="result.rows" border stripe><el-table-column prop="metricName" label="指标" min-width="160"><template #default="{ row }">{{ row.metricName }} <small>{{ row.metricCode }}</small></template></el-table-column><el-table-column v-for="target in targets" :key="target.targetCode" :label="target.targetName || target.targetCode" min-width="140"><template #default="{ row }">{{ display(row.values[target.targetCode], row.unit) }}</template></el-table-column></el-table></el-card>
    <MetricSelector :open="metricDialogVisible" :target-type="targetType" selection-mode="MULTIPLE" :selected-metric-codes="selectedMetrics.map(metric => metric.metricCode)" @confirm="selectMetrics" @cancel="metricDialogVisible = false" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import AsOfTimePicker from '/@/components/common/AsOfTimePicker.vue';
import MetricSelector from '/@/components/common/MetricSelector.vue';
import TargetSearchBox from '/@/components/common/TargetSearchBox.vue';
import { querySelectableMetrics, type SelectableMetric } from '/@/api/commonSelectorApi';
import { queryComparison, saveComparisonResult } from '/@/api/comparisonApi';
import type { ComparisonQueryRequest, ComparisonResult, ComparisonTarget } from '/@/types/comparison';
import type { TargetType } from '/@/types/targetScope';
import type { WatchTarget } from '/@/types/watchtarget/entity';

const route = useRoute(); const targetType = ref<TargetType>((route.query.targetType as TargetType) || 'STOCK'); const targets = ref<ComparisonTarget[]>([]); const searchTarget = ref<WatchTarget | null>(null); const asOfTime = ref<string | null>(null); const selectedMetrics = ref<SelectableMetric[]>([]); const metricCatalog = ref<Record<string, SelectableMetric>>({}); const metricDialogVisible = ref(false); const result = ref<ComparisonResult>({ targetType: targetType.value, targets: [], rows: [] }); const loading = ref(false); const executed = ref(false);
onMounted(() => { const codes = String(route.query.selectedTargetCodes || '').split(',').filter(Boolean); codes.forEach(code => targets.value.push({ targetCode: code, targetName: code })); loadMetrics(); });
const metricCodes = computed(() => selectedMetrics.value.map(metric => metric.metricCode));
async function loadMetrics() { const page = await querySelectableMetrics({ targetType: targetType.value, pageNo: 1, pageSize: 200 }); metricCatalog.value = Object.fromEntries(page.records.map(metric => [metric.metricCode, metric])); }
function resetTargets() { targets.value = []; executed.value = false; }
function addTarget(target: WatchTarget) { if (target.targetType !== targetType.value) return ElMessage.warning('只能添加同一标的类型'); if (!targets.value.some(item => item.targetCode === target.targetCode)) targets.value.push({ targetCode: target.targetCode, targetName: target.targetName }); searchTarget.value = null; }
function removeTarget(code: string) { targets.value = targets.value.filter(target => target.targetCode !== code); executed.value = false; }
async function selectMetrics(codes: string[]) { metricDialogVisible.value = false; await loadMetrics(); selectedMetrics.value = codes.map(code => metricCatalog.value[code]).filter(Boolean); executed.value = false; }
function removeMetric(code: string) { selectedMetrics.value = selectedMetrics.value.filter(metric => metric.metricCode !== code); executed.value = false; }
function request(): ComparisonQueryRequest { return { targetType: targetType.value, targetCodes: targets.value.map(target => target.targetCode), metricCodes: metricCodes.value, asOfTime: asOfTime.value }; }
async function execute() { if (targets.value.length < 2) return ElMessage.warning('请至少选择两个标的'); if (!metricCodes.value.length) return ElMessage.warning('请至少选择一个指标'); loading.value = true; try { result.value = await queryComparison(request()); if (result.value.targets?.length) targets.value = result.value.targets; executed.value = true; } finally { loading.value = false; } }
async function save() { const snapshot = await saveComparisonResult({ request: request(), result: result.value }); ElMessage.success(`已保存对比结果 ${snapshot.resultId}`); }
function display(value: unknown, unit?: string) { if (value === null || value === undefined || value === '') return '--'; return `${value}${unit || ''}`; }
</script>

<style scoped lang="scss">.analysis-page{display:grid;gap:16px}.page-heading,.result-header,.actions{display:flex;align-items:center;justify-content:space-between}.config-card,.result-card{max-width:1400px}.section-title{font-weight:600;margin-bottom:10px}.config-section{padding:16px 0;border-top:1px solid var(--el-border-color-lighter)}.search-row{display:flex;align-items:center;gap:8px;flex-wrap:wrap}.search-row .stock-search-box{width:360px}.metric-tags{display:flex;align-items:center;gap:8px;flex-wrap:wrap}.actions{padding-top:16px;justify-content:flex-end}small{color:var(--el-text-color-secondary)}</style>
