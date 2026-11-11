<template>
  <div class="as-of-time-picker">
    <span v-if="label" class="as-of-label">{{ label }}</span>
    <el-select v-model="mode" class="as-of-mode" :disabled="disabled">
      <el-option label="最新" value="LATEST" />
      <el-option label="指定时间" value="SPECIFIED" />
    </el-select>
    <el-date-picker
      v-if="mode === 'SPECIFIED'"
      v-model="specifiedTime"
      type="datetime"
      class="as-of-date"
      :disabled="disabled"
      value-format="YYYY-MM-DDTHH:mm:ss"
      format="YYYY-MM-DD HH:mm:ss"
      placeholder="选择截至时间"
      @change="emitValue"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';

const props = withDefaults(defineProps<{
  /** null 表示最新；有值时必须是 ISO 8601 local datetime。 */
  modelValue?: string | null;
  label?: string;
  disabled?: boolean;
}>(), {
  modelValue: null,
  label: '截至时间',
  disabled: false,
});

const emit = defineEmits<{
  (e: 'update:modelValue', value: string | null): void;
}>();

const mode = ref<'LATEST' | 'SPECIFIED'>(props.modelValue ? 'SPECIFIED' : 'LATEST');
const specifiedTime = ref<string>(props.modelValue || '');

watch(() => props.modelValue, (value) => {
  mode.value = value ? 'SPECIFIED' : 'LATEST';
  specifiedTime.value = value || '';
});

watch(mode, (value) => {
  if (value === 'LATEST') {
    emit('update:modelValue', null);
  } else if (specifiedTime.value) {
    emit('update:modelValue', specifiedTime.value);
  }
});

function emitValue(value: string | null) {
  emit('update:modelValue', value || null);
}
</script>

<style scoped lang="scss">
.as-of-time-picker { display: inline-flex; align-items: center; gap: 8px; }
.as-of-label { color: var(--el-text-color-regular); white-space: nowrap; }
.as-of-mode { width: 110px; }
.as-of-date { width: 210px; }
</style>
