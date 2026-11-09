<template>
  <el-dialog
    :model-value="modelValue"
    :title="mode === 'create' ? '新增应用指标' : '编辑应用指标'"
    width="680px"
    destroy-on-close
    @close="handleClose"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
      <el-divider content-position="left">基本信息</el-divider>

      <el-form-item label="指标编码" prop="metricCode">
        <el-input
          v-model="form.metricCode"
          :disabled="mode === 'edit'"
          placeholder="请输入稳定业务编码"
        />
      </el-form-item>

      <el-form-item label="指标名称" prop="metricName">
        <el-input v-model="form.metricName" placeholder="请输入显示名称" />
      </el-form-item>

      <el-form-item label="指标分类" prop="metricCategory">
        <el-select
          v-model="form.metricCategory"
          placeholder="请选择指标分类"
          style="width: 100%"
        >
          <el-option
            v-for="item in METRIC_CATEGORY_OPTIONS"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="单位">
        <el-input
          v-model="form.unit"
          maxlength="32"
          placeholder="例如：%、元、倍、股"
        />
      </el-form-item>

      <el-form-item label="指标说明">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="3"
          maxlength="500"
          show-word-limit
        />
      </el-form-item>

      <el-divider content-position="left">数据来源</el-divider>

      <el-form-item label="标的类型" prop="targetType">
        <el-select v-model="form.targetType" :disabled="mode === 'edit' && Boolean(form.targetType)" placeholder="请选择标的类型" style="width: 100%" @change="handleTargetTypeChange">
          <el-option v-for="item in ASSET_TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>

      <el-alert
        v-if="datasetNotice"
        :title="datasetNotice"
        type="warning"
        :closable="false"
        class="source-alert"
      />

      <el-form-item label="来源数据集" prop="datasetCode">
        <el-select
          v-model="form.datasetCode"
          filterable
          :disabled="!form.targetType"
          placeholder="请先选择标的类型"
          style="width: 100%"
          :loading="datasetLoading"
          @change="handleDatasetChange"
        >
          <el-option
            v-for="dataset in datasetOptions"
            :key="dataset.datasetCode"
            :label="`${dataset.datasetName}（${dataset.datasetCode}）`"
            :value="dataset.datasetCode"
          >
            <span>
              {{ dataset.datasetName }}（{{ dataset.datasetCode }}）
            </span>
            <span class="option-status">
              {{ datasetStatusText(dataset.datasetStatus) }}
            </span>
          </el-option>
        </el-select>
      </el-form-item>

      <el-form-item label="来源字段" prop="fieldCode">
        <el-select
          v-model="form.fieldCode"
          filterable
          placeholder="请先选择数据集"
          style="width: 100%"
          :disabled="!form.datasetCode || fieldLoading"
          :loading="fieldLoading"
        >
          <el-option
            v-for="field in fieldOptions"
            :key="field.fieldCode"
            :label="`${field.fieldCode} · ${field.fieldName} · ${field.fieldType}`"
            :value="field.fieldCode"
          />
        </el-select>

        <div
          v-if="form.datasetCode && !fieldLoading && fieldOptions.length === 0"
          class="empty-field-tip"
        >
          当前数据集没有可用标准字段
        </div>
      </el-form-item>

      <el-form-item label="字段类型">
        <el-input
          :model-value="selectedField?.fieldType || ''"
          disabled
          placeholder="选择来源字段后自动显示"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" :loading="saving" @click="handleSubmit">
        保存
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import { ElMessage } from 'element-plus';

import {
  getDatasetOption,
  queryDatasetFields,
  queryEnabledDatasets,
  saveOrUpdateAppMetric,
  type DatasetFieldOption,
  type DatasetOption,
} from '/@/api/appmetric/appMetricApi';
import { ASSET_TYPE_OPTIONS, METRIC_CATEGORY_OPTIONS, type AssetType } from '/@/types/appmetric/constants';
import {
  createDefaultAppMetric,
  type AppMetric,
} from '/@/types/appmetric/entity';

const props = withDefaults(
  defineProps<{
    modelValue: boolean;
    mode?: 'create' | 'edit';
    formData?: AppMetric;
  }>(),
  {
    mode: 'create',
    formData: () => createDefaultAppMetric(),
  },
);

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void;
  (e: 'success'): void;
}>();

const formRef = ref<FormInstance>();
const saving = ref(false);
const datasetLoading = ref(false);
const fieldLoading = ref(false);
const datasetOptions = ref<DatasetOption[]>([]);
const fieldOptions = ref<DatasetFieldOption[]>([]);
const form = reactive<AppMetric>(createDefaultAppMetric());

const selectedDataset = computed(() =>
  datasetOptions.value.find((item) => item.datasetCode === form.datasetCode),
);

const selectedField = computed(() =>
  fieldOptions.value.find((item) => item.fieldCode === form.fieldCode),
);

const datasetNotice = computed(() =>
  selectedDataset.value && !isEnabled(selectedDataset.value.datasetStatus)
    ? '原绑定数据集当前不可用；请重新选择已启用数据集后再保存。'
    : '',
);

const rules: FormRules = {
  metricCode: [
    {
      required: true,
      message: '请输入指标编码',
      trigger: 'blur',
    },
  ],
  metricName: [
    {
      required: true,
      message: '请输入指标名称',
      trigger: 'blur',
    },
  ],
  metricCategory: [
    {
      required: true,
      message: '请选择指标分类',
      trigger: 'change',
    },
  ],
  targetType: [
    { required: true, message: '请选择标的类型', trigger: 'change' },
  ],
  datasetCode: [
    {
      required: true,
      message: '请选择来源数据集',
      trigger: 'change',
    },
  ],
  fieldCode: [
    {
      required: true,
      message: '请选择来源字段',
      trigger: 'change',
    },
  ],
};

/** 每次打开重读数据集和字段，确保字段选项来自 Dataset 当前版本而非弹窗缓存。 */
watch(
  () => props.modelValue,
  async (visible) => {
    if (!visible) {
      return;
    }

    Object.assign(form, createDefaultAppMetric(), props.formData || {});
    fieldOptions.value = [];

    if (form.targetType) {
      await loadDatasetOptions();
    }

    if (form.datasetCode) {
      await loadFields(form.datasetCode);
    }
  },
);

async function loadDatasetOptions() {
  if (!form.targetType) {
    datasetOptions.value = [];
    return;
  }
  datasetLoading.value = true;

  try {
    // 只加载与指标 targetType 相同且已启用的 Dataset，防止前端产生跨标的绑定。
    const page = await queryEnabledDatasets(form.targetType);
    datasetOptions.value = page.records || [];

    // 编辑旧记录时，停用的原 Dataset 不在“已启用”目录中；额外读取以保证值可回显。
    if (
      form.datasetCode &&
      !datasetOptions.value.some(
        (item) => item.datasetCode === form.datasetCode,
      )
    ) {
      const detail = await getDatasetOption(form.datasetCode);

      if (detail?.dataset && detail.dataset.assetType === form.targetType) {
        datasetOptions.value.push(detail.dataset);
      } else if (detail?.dataset) {
        ElMessage.warning('原绑定数据集标的类型与当前指标不一致，请重新选择。');
        form.datasetCode = '';
        form.fieldCode = '';
      }
    }
  } catch {
    datasetOptions.value = [];
    ElMessage.error('数据集查询失败，暂不能保存指标来源');
  } finally {
    datasetLoading.value = false;
  }
}

async function handleTargetTypeChange() {
  // 新建时变更类型，原 Dataset/字段的归属不再可信，必须同步清空并重新筛选。
  form.datasetCode = '';
  form.fieldCode = '';
  fieldOptions.value = [];
  await loadDatasetOptions();
}

async function handleDatasetChange(datasetCode: string) {
  // 更换 Dataset 时清空旧字段，避免提交不属于新版本字段结构的残留编码。
  form.fieldCode = '';
  fieldOptions.value = [];
  await loadFields(datasetCode);
}

async function loadFields(datasetCode: string) {
  fieldLoading.value = true;

  try {
    fieldOptions.value = (await queryDatasetFields(datasetCode)) || [];

    if (
      form.fieldCode &&
      !fieldOptions.value.some((field) => field.fieldCode === form.fieldCode)
    ) {
      ElMessage.warning('原绑定字段已不在该数据集当前版本中，请重新选择。');
      form.fieldCode = '';
    }
  } catch {
    fieldOptions.value = [];
    form.fieldCode = '';
    ElMessage.error('标准字段查询失败，暂不能保存指标来源');
  } finally {
    fieldLoading.value = false;
  }
}

function isEnabled(status: DatasetOption['datasetStatus']) {
  return status === 1 || status === '1' || status === 'ENABLED';
}

function datasetStatusText(status: DatasetOption['datasetStatus']) {
  if (isEnabled(status)) {
    return '已启用';
  }

  return status === 2 || status === '2' || status === 'DISABLED'
    ? '已停用'
    : '不可用';
}

function handleClose() {
  emit('update:modelValue', false);
}

async function handleSubmit() {
  await formRef.value?.validate();

  if (datasetNotice.value || !selectedField.value) {
    ElMessage.warning(
      datasetNotice.value || '请选择当前数据集版本中的标准字段',
    );
    return;
  }

  saving.value = true;

  try {
    // 显式挑选可持久化字段，避免 datasetName、fieldType 等展示派生值回传后端。
    await saveOrUpdateAppMetric({
      metricCode: form.metricCode,
      metricName: form.metricName,
      metricCategory: form.metricCategory,
      // 表单校验已确保该值非空；显式使用协议类型，避免将展示文本传给后端。
      targetType: form.targetType as AssetType,
      datasetCode: form.datasetCode,
      fieldCode: form.fieldCode,
      unit: form.unit,
      description: form.description,
    });

    ElMessage.success('保存成功');
    emit('success');
    handleClose();
  } finally {
    saving.value = false;
  }
}
</script>

<style scoped>
.source-alert {
  margin: 0 0 12px 110px;
}

.option-status {
  float: right;
  color: #909399;
  font-size: 12px;
}

.empty-field-tip {
  color: #909399;
  font-size: 12px;
  line-height: 20px;
}
</style>
