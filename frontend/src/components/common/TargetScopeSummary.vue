<template>
  <section class="target-scope-summary" aria-label="标的范围">
    <div class="summary-label">标的范围</div>
    <div class="summary-target-type">{{ targetTypeName }}</div>
    <div class="summary-detail">{{ detailText }}</div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { ASSET_TYPE_TEXT } from '/@/types/asset';
import type { TargetScope, TargetScopeOptions } from '/@/types/targetScope';

const props = withDefaults(defineProps<{
  scope: TargetScope;
  /** 传入选择器使用过的主数据即可展示名称，组件不会展示内部 code。 */
  options?: Partial<TargetScopeOptions>;
  maxNames?: number;
}>(), {
  options: () => ({}),
  maxNames: 2,
});

const targetTypeName = computed(() => ASSET_TYPE_TEXT[props.scope.targetType]);
const scopeLabel = computed(() => ({
  BOARD: '板块', WATCH_GROUP: '关注分组', INDEX_TYPE: '类型', MARKET: '市场',
}[props.scope.scopeType] || '范围'));
const currentOptions = computed(() => {
  if (props.scope.scopeType === 'BOARD') return props.options.board || [];
  if (props.scope.scopeType === 'WATCH_GROUP') return props.options.watchGroup || [];
  if (props.scope.scopeType === 'INDEX_TYPE') return props.options.indexType || [];
  if (props.scope.scopeType === 'MARKET') return props.options.market || [];
  return [];
});
const detailText = computed(() => {
  if (props.scope.scopeType === 'ALL') return props.scope.targetType === 'STOCK' ? '全部股票' : '全部指数';
  const names = props.scope.scopeValues
    .map((code) => currentOptions.value.find((item) => item.code === code)?.name)
    .filter((name): name is string => Boolean(name));
  if (!names.length) return `已选择 ${props.scope.scopeValues.length} 项`;
  const visibleNames = names.slice(0, props.maxNames);
  const rest = names.length - visibleNames.length;
  return `${scopeLabel.value}：${visibleNames.join('、')}${rest > 0 ? `等 ${names.length} 个` : ''}`;
});
</script>

<style scoped lang="scss">
.target-scope-summary { display: grid; gap: 4px; }
.summary-label { color: var(--el-text-color-secondary); font-size: 13px; }
.summary-target-type { color: var(--el-text-color-primary); font-size: 16px; font-weight: 600; }
.summary-detail { color: var(--el-text-color-regular); line-height: 1.5; }
</style>
