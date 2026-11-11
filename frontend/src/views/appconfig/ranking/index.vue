<template>
  <div class="analysis-page layout-pd">
    <div class="page-heading"><div><h2>指标排行</h2><p>按多个指标依次排序，缺失值统一排在有值之后。</p></div><el-radio-group v-model="targetType" @change="resetPage"><el-radio-button label="STOCK">股票</el-radio-button><el-radio-button label="INDEX">指数</el-radio-button></el-radio-group></div>
    <el-card shadow="never" class="config-card">
      <section class="scope-row" @click="scopeDialogVisible = true"><TargetScopeSummary :scope="scope" :options="scopeOptions" /><el-button link type="primary">修改</el-button></section>
      <section class="config-section"><div class="section-title">截至时间</div><AsOfTimePicker v-model="asOfTime" label="" /></section>
      <section class="config-section"><div class="section-title">排行规则</div>
        <div v-if="!rules.length" class="empty-hint">请添加至少一个数值指标</div>
        <div v-for="(rule, index) in rules" :key="rule.metricCode" class="rule-row" draggable="true" @dragstart="dragIndex = index" @dragover.prevent @drop="dropRule(index)">
          <span class="priority">{{ index + 1 }}</span><span class="drag">⠿</span><span class="metric-name">{{ metricCatalog[rule.metricCode]?.metricName || rule.metricCode }} <small>{{ rule.metricCode }}</small></span>
          <el-select v-model="rule.sortDirection" size="small" class="direction"><el-option label="从高到低" value="DESC" /><el-option label="从低到高" value="ASC" /></el-select>
          <el-button link type="danger" @click="removeRule(rule.metricCode)">删除</el-button>
        </div>
        <el-button @click="metricDialogVisible = true">+ 添加排行指标</el-button>
      </section>
      <section class="config-section topn"><span class="section-title">Top</span><el-select v-model="topN" size="small"><el-option v-for="n in [10,20,50,100,200]" :key="n" :label="String(n)" :value="n" /></el-select></section>
      <div class="actions"><el-button type="primary" :loading="loading" @click="execute">生成排行</el-button></div>
    </el-card>
    <el-card shadow="never" class="result-card"><template #header><div class="result-header"><span>排行结果 <em v-if="executed">共 {{ result.total }} 个标的</em></span><span><el-button :disabled="!executed" @click="save">保存结果</el-button><el-button type="primary" :disabled="!selectedCodes.length" @click="goCompare">进入对比</el-button></span></div></template>
      <el-alert v-if="!executed" title="设置范围和排行指标后生成结果" type="info" :closable="false" show-icon />
      <el-table v-else :data="result.rows" border stripe @selection-change="selectedCodes = $event.map((row: any) => row.targetCode)"><el-table-column type="selection" width="48" /><el-table-column prop="rank" label="排名" width="72" /><el-table-column prop="targetCode" label="代码" min-width="120" /><el-table-column prop="targetName" label="名称" min-width="120" /><el-table-column v-for="rule in rules" :key="rule.metricCode" :label="`${metricCatalog[rule.metricCode]?.metricName || rule.metricCode} ${rule.sortDirection === 'DESC' ? '↓' : '↑'}`" min-width="130"><template #default="{ row }">{{ display(row.metricValues[rule.metricCode], metricCatalog[rule.metricCode]?.unit) }}</template></el-table-column></el-table>
    </el-card>
    <TargetScopeSelector :open="scopeDialogVisible" :target-type="targetType" :value="scope" :board-options="scopeOptions.board" :watch-group-options="scopeOptions.watchGroup" :index-type-options="scopeOptions.indexType" :index-market-options="scopeOptions.market" @confirm="confirmScope" @cancel="scopeDialogVisible = false" />
    <MetricSelector :open="metricDialogVisible" :target-type="targetType" selection-mode="MULTIPLE" :allowed-value-types="['INTEGER','DECIMAL','NUMBER','NUMERIC','FLOAT','DOUBLE']" :selected-metric-codes="rules.map(r => r.metricCode)" @confirm="selectMetrics" @cancel="metricDialogVisible = false" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import AsOfTimePicker from '/@/components/common/AsOfTimePicker.vue';
import MetricSelector from '/@/components/common/MetricSelector.vue';
import TargetScopeSelector from '/@/components/common/TargetScopeSelector.vue';
import TargetScopeSummary from '/@/components/common/TargetScopeSummary.vue';
import { querySelectableMetrics, type SelectableMetric } from '/@/api/commonSelectorApi';
import { queryScreeningScopeOptions } from '/@/api/screeningApi';
import { queryRanking, saveRankingResult } from '/@/api/rankingApi';
import { emptyTargetScope, type TargetScope, type TargetScopeOptions, type TargetType } from '/@/types/targetScope';
import type { RankingQueryRequest, RankingResult, RankingRule } from '/@/types/ranking';
import { router } from '/@/router/index';

const targetType = ref<TargetType>('STOCK'); const scope = ref<TargetScope>(emptyTargetScope('STOCK')); const scopeOptions = ref<TargetScopeOptions>({ board: [], watchGroup: [], indexType: [], market: [] }); const asOfTime = ref<string | null>(null); const rules = ref<RankingRule[]>([]); const metricCatalog = ref<Record<string, SelectableMetric>>({}); const topN = ref(50); const result = ref<RankingResult>({ total: 0, rows: [] }); const selectedCodes = ref<string[]>([]); const loading = ref(false); const executed = ref(false); const scopeDialogVisible = ref(false); const metricDialogVisible = ref(false); const dragIndex = ref(-1);
onMounted(loadOptions);
async function loadOptions() { scopeOptions.value = await queryScreeningScopeOptions(targetType.value); }
async function resetPage() { scope.value = emptyTargetScope(targetType.value); rules.value = []; result.value = { total: 0, rows: [] }; executed.value = false; selectedCodes.value = []; await loadOptions(); }
function confirmScope(value: TargetScope) { scope.value = value; scopeDialogVisible.value = false; executed.value = false; }
async function selectMetrics(codes: string[]) { metricDialogVisible.value = false; const old = new Map(rules.value.map(rule => [rule.metricCode, rule])); rules.value = codes.map((code, index) => old.get(code) || ({ metricCode: code, sortDirection: 'DESC', priority: index } as RankingRule)); const page = await querySelectableMetrics({ targetType: targetType.value, allowedValueTypes: ['INTEGER','DECIMAL','NUMBER','NUMERIC','FLOAT','DOUBLE'], pageNo: 1, pageSize: 200 }); metricCatalog.value = Object.fromEntries(page.records.map(metric => [metric.metricCode, metric])); }
function removeRule(code: string) { rules.value = rules.value.filter(rule => rule.metricCode !== code); }
function dropRule(index: number) { if (dragIndex.value < 0 || dragIndex.value === index) return; const [rule] = rules.value.splice(dragIndex.value, 1); rules.value.splice(index, 0, rule); rules.value.forEach((item, i) => item.priority = i); dragIndex.value = -1; }
function request(): RankingQueryRequest { return { targetType: targetType.value, targetScope: scope.value, asOfTime: asOfTime.value, rankingRules: rules.value.map((rule, priority) => ({ ...rule, priority })), topN: topN.value }; }
async function execute() { if (!rules.value.length) return ElMessage.warning('请至少添加一个排行指标'); loading.value = true; try { result.value = await queryRanking(request()); executed.value = true; selectedCodes.value = []; } finally { loading.value = false; } }
async function save() { const snapshot = await saveRankingResult({ request: request(), result: result.value }); ElMessage.success(`已保存排行结果 ${snapshot.resultId}`); }
async function goCompare() {
  const snapshot = await saveRankingResult({ request: request(), result: result.value });
  router.push({ path: '/app/ranking-compare', query: { sourceType: 'RANKING_RESULT', sourceId: snapshot.resultId, selectedTargetCodes: selectedCodes.value.join(','), targetType: targetType.value } });
}
function display(value: unknown, unit?: string) { if (value === null || value === undefined || value === '') return '--'; return `${value}${unit || ''}`; }
</script>

<style scoped lang="scss">.analysis-page{display:grid;gap:16px}.page-heading,.result-header,.scope-row,.rule-row,.actions{display:flex;align-items:center;justify-content:space-between}.config-card,.result-card{max-width:1400px}.scope-row{cursor:pointer;border-bottom:1px solid var(--el-border-color-lighter);padding-bottom:16px}.config-section{padding:16px 0;border-bottom:1px solid var(--el-border-color-lighter)}.section-title{font-weight:600;margin-bottom:10px}.rule-row{justify-content:flex-start;gap:12px;padding:10px 8px;border:1px solid var(--el-border-color-lighter);margin-bottom:8px}.priority{width:22px}.drag{cursor:grab;color:var(--el-text-color-secondary)}.metric-name{flex:1}.metric-name small{color:var(--el-text-color-secondary);margin-left:6px}.direction{width:130px}.empty-hint{color:var(--el-text-color-secondary);padding-bottom:10px}.topn{display:flex;align-items:center;gap:10px}.topn .section-title{margin:0}.actions{justify-content:flex-end;padding-top:16px}.result-header em{font-style:normal;color:var(--el-text-color-secondary);font-size:13px;margin-left:8px}</style>
