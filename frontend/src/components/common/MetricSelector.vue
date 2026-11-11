<template>
  <el-dialog :model-value="open" title="选择指标" width="760px" destroy-on-close @close="handleCancel">
    <div class="metric-groups" aria-label="指标分类">
      <el-button
        v-for="group in metricGroups"
        :key="group.value"
        :type="currentMetricGroup === group.value ? 'primary' : 'default'"
        @click="currentMetricGroup = group.value"
      >{{ group.label }}</el-button>
    </div>
    <el-input v-model="keyword" class="metric-search" clearable placeholder="搜索指标名称或编码" />

    <section class="selected-metrics" aria-label="已选指标">
      <div class="selected-label">已选指标</div>
      <div class="selected-tags">
        <el-tag
          v-for="metric in selectedMetrics"
          :key="metric.metricCode"
          closable
          @close="removeMetric(metric.metricCode)"
        >{{ metric.metricName }}</el-tag>
        <span v-if="selectedMetrics.length === 0" class="empty-selected">暂未选择指标</span>
      </div>
    </section>

    <div v-loading="loading" class="metric-list">
      <el-radio-group v-if="selectionMode === 'SINGLE'" v-model="singleSelectedCode" class="metric-options">
        <el-radio v-for="metric in visibleMetrics" :key="metric.metricCode" :label="metric.metricCode">
          {{ metric.metricName }}
        </el-radio>
      </el-radio-group>
      <el-checkbox-group v-else v-model="multipleSelectedCodes" class="metric-options">
        <el-checkbox v-for="metric in visibleMetrics" :key="metric.metricCode" :label="metric.metricCode">
          {{ metric.metricName }}
        </el-checkbox>
      </el-checkbox-group>
      <el-empty v-if="!loading && visibleMetrics.length === 0" :image-size="64" description="暂无可选指标" />
    </div>
    <template #footer>
      <el-button @click="handleCancel">取消</el-button>
      <el-button type="primary" @click="handleConfirm">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { querySelectableMetrics, type SelectableMetric } from '/@/api/commonSelectorApi';
import { METRIC_CATEGORY_TEXT, type MetricCategoryFilter } from '/@/types/appmetric/constants';
import type { TargetType } from '/@/types/targetScope';

const props = withDefaults(defineProps<{
  open: boolean;
  targetType: TargetType;
  selectionMode: 'SINGLE' | 'MULTIPLE';
  selectedMetricCodes?: string[];
  allowedValueTypes?: string[];
}>(), {
  selectedMetricCodes: () => [],
  allowedValueTypes: () => [],
});

const emit = defineEmits<{
  (e: 'confirm', metricCodes: string[]): void;
  (e: 'cancel'): void;
}>();

const metricGroups: Array<{ label: string; value: MetricCategoryFilter }> = (Object.keys(METRIC_CATEGORY_TEXT) as MetricCategoryFilter[])
  .map((value) => ({ value, label: METRIC_CATEGORY_TEXT[value] }));
const currentMetricGroup = ref<MetricCategoryFilter>('ALL');
const keyword = ref('');
const loading = ref(false);
const metrics = ref<SelectableMetric[]>([]);
const selectedCodes = ref(new Set<string>());

const visibleMetrics = computed(() => metrics.value.filter((metric) => {
  const matchesGroup = currentMetricGroup.value === 'ALL' || metric.metricCategory === currentMetricGroup.value;
  const normalizedKeyword = keyword.value.trim().toLowerCase();
  const matchesKeyword = !normalizedKeyword || metric.metricName.toLowerCase().includes(normalizedKeyword)
    || metric.metricCode.toLowerCase().includes(normalizedKeyword);
  return matchesGroup && matchesKeyword;
}));
const selectedMetrics = computed(() => metrics.value.filter((metric) => selectedCodes.value.has(metric.metricCode)));
const multipleSelectedCodes = computed<string[]>({
  get: () => [...selectedCodes.value],
  set: (codes) => { selectedCodes.value = new Set(codes); },
});
const singleSelectedCode = computed<string>({
  get: () => [...selectedCodes.value][0] || '',
  set: (code) => { selectedCodes.value = new Set(code ? [code] : []); },
});

watch(() => [props.open, props.targetType] as const, async ([visible]) => {
  if (!visible) return;
  currentMetricGroup.value = 'ALL';
  keyword.value = '';
  selectedCodes.value = new Set(props.selectionMode === 'SINGLE'
    ? props.selectedMetricCodes.slice(0, 1)
    : props.selectedMetricCodes);
  await loadMetrics();
}, { immediate: true });

async function loadMetrics() {
  loading.value = true;
  try {
    // 组件始终传 targetType + ENABLED；分类和关键字在已加载集合中切换，因此不会丢失跨分类选择。
    const page = await querySelectableMetrics({
      targetType: props.targetType,
      allowedValueTypes: props.allowedValueTypes,
      pageNo: 1,
      pageSize: 200,
    });
    metrics.value = page.records;
    const availableCodes = new Set(metrics.value.map((metric) => metric.metricCode));
    selectedCodes.value = new Set([...selectedCodes.value].filter((code) => availableCodes.has(code)));
  } finally {
    loading.value = false;
  }
}

function removeMetric(metricCode: string) {
  const next = new Set(selectedCodes.value);
  next.delete(metricCode);
  selectedCodes.value = next;
}

function handleConfirm() {
  emit('confirm', [...selectedCodes.value]);
}

function handleCancel() {
  emit('cancel');
}
</script>

<style scoped lang="scss">
.metric-groups, .selected-tags, .metric-options { display: flex; flex-wrap: wrap; gap: 8px; }
.metric-search { margin-top: 14px; }
.selected-metrics { margin: 16px 0; }
.selected-label { margin-bottom: 8px; color: var(--el-text-color-regular); }
.empty-selected { color: var(--el-text-color-secondary); font-size: 13px; }
.metric-list { min-height: 120px; max-height: 320px; overflow: auto; }
.metric-options { flex-direction: column; align-items: flex-start; }
</style>
