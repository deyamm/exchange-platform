<template>
  <div class="application-config-page">
    <div class="metric-layout">
      <el-card shadow="never" class="category-card">
        <template #header>
          <span class="category-title">指标分类</span>
        </template>

        <div class="category-list">
          <el-button
            v-for="category in categoryFilters"
            :key="category.value"
            :type="
              selectedCategory === category.value ? 'primary' : 'default'
            "
            text
            class="category-button"
            @click="handleCategoryChange(category.value)"
          >
            {{ category.label }}
          </el-button>
        </div>
      </el-card>

      <el-card shadow="never" class="list-card">
        <template #header>
          <div class="card-header">
            <span class="card-title">应用指标配置</span>
            <el-button type="primary" @click="openCreate">
              新增指标
            </el-button>
          </div>
        </template>

        <el-form :model="queryForm" inline class="query-form">
          <el-form-item label="关键字">
            <el-input
              v-model="queryForm.keyword"
              clearable
              placeholder="编码/名称"
              style="width: 220px"
              @keyup.enter="handleSearch"
            />
          </el-form-item>

          <el-form-item label="状态">
            <el-select
              v-model="queryForm.status"
              clearable
              placeholder="全部"
              style="width: 140px"
            >
              <el-option
                v-for="item in metricStatusOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="标的类型">
            <el-select
              v-model="queryForm.targetType"
              clearable
              placeholder="全部"
              style="width: 130px"
            >
              <el-option
                v-for="item in ASSET_TYPE_OPTIONS"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" @click="handleSearch">
              查询
            </el-button>
            <el-button @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>

        <el-table
          v-loading="loading"
          :data="tableData"
          border
          row-key="metricCode"
          height="calc(100vh - 330px)"
        >
          <el-table-column
            prop="metricCode"
            label="指标编码"
            min-width="145"
            align="center"
          />
          <el-table-column
            prop="metricName"
            label="指标名称"
            min-width="140"
            align="center"
          />
          <el-table-column label="分类" width="90" align="center">
            <template #default="{ row }">
              {{
                METRIC_CATEGORY_TEXT[
                  row.metricCategory as MetricCategoryFilter
                ] || row.metricCategory
              }}
            </template>
          </el-table-column>
          <el-table-column label="标的类型" width="100" align="center">
            <template #default="{ row }">
              {{
                row.targetTypeName ||
                ASSET_TYPE_TEXT[row.targetType as AssetType] ||
                row.targetType ||
                '待处理旧记录'
              }}
            </template>
          </el-table-column>
          <el-table-column
            label="来源数据集"
            min-width="165"
            show-overflow-tooltip
            align="center"
          >
            <template #default="{ row }">
              {{ row.datasetName || row.datasetCode || '待处理旧记录' }}
            </template>
          </el-table-column>
          <el-table-column
            label="来源字段"
            min-width="150"
            show-overflow-tooltip
            align="center"
          >
            <template #default="{ row }">
              {{
                row.fieldName
                  ? `${row.fieldCode} · ${row.fieldName}`
                  : row.fieldCode || '-'
              }}
            </template>
          </el-table-column>
          <el-table-column
            prop="fieldType"
            label="字段类型"
            width="110"
            align="center"
          />
          <el-table-column
            prop="unit"
            label="单位"
            width="80"
            align="center"
          />
          <el-table-column
            prop="status"
            label="状态"
            width="90"
            align="center"
          >
            <template #default="{ row }">
              <el-tag
                :type="
                  COMMON_STATUS_TAG_TYPE[row.status as CommonStatus]
                "
              >
                {{
                  COMMON_STATUS_TEXT[row.status as CommonStatus] ||
                  row.status
                }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column
            prop="updatedAt"
            label="更新时间"
            width="165"
            align="center"
          />
          <el-table-column
            label="操作"
            width="200"
            fixed="right"
            align="center"
          >
            <template #default="{ row }">
              <el-button link type="primary" @click="openEdit(row)">
                编辑
              </el-button>
              <el-button
                link
                :type="row.status === 'ENABLED' ? 'warning' : 'success'"
                @click="toggleStatus(row)"
              >
                {{ row.status === 'ENABLED' ? '停用' : '启用' }}
              </el-button>
              <el-button link type="danger" @click="remove(row)">
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-wrapper">
          <el-pagination
            v-model:current-page="queryForm.pageNo"
            v-model:page-size="queryForm.pageSize"
            background
            layout="total, sizes, prev, pager, next, jumper"
            :total="total"
            @current-change="fetchData"
            @size-change="handleSearch"
          />
        </div>
      </el-card>
    </div>

    <AppMetricFormDialog
      v-model="dialogVisible"
      :mode="dialogMode"
      :form-data="currentRow"
      @success="fetchData"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';

import {
  deleteAppMetric,
  queryAppMetricPage,
  updateAppMetricStatus,
} from '/@/api/appmetric/appMetricApi';
import {
  ASSET_TYPE_OPTIONS,
  ASSET_TYPE_TEXT,
  METRIC_CATEGORY_TEXT,
  type AssetType,
  type MetricCategoryFilter,
} from '/@/types/appmetric/constants';
import {
  createDefaultAppMetric,
  type AppMetric,
} from '/@/types/appmetric/entity';
import {
  COMMON_STATUS_TAG_TYPE,
  COMMON_STATUS_TEXT,
} from '/@/types/appscene/view';
import type { CommonStatus } from '/@/types/appscene/constants';

import AppMetricFormDialog from './AppMetricFormDialog.vue';

const categoryFilters: {
  label: string;
  value: MetricCategoryFilter;
}[] = [
  { label: '全部', value: 'ALL' },
  { label: '行情', value: 'MARKET' },
  { label: '估值', value: 'VALUATION' },
  { label: '财务', value: 'FINANCE' },
  { label: '成长', value: 'GROWTH' },
  { label: '风险', value: 'RISK' },
  { label: '其他', value: 'OTHER' },
];

const metricStatusOptions = [
  { label: '启用', value: 'ENABLED' },
  { label: '停用', value: 'DISABLED' },
];

const selectedCategory = ref<MetricCategoryFilter>('ALL');

const queryForm = reactive({
  keyword: '',
  metricCategory: '',
  status: '' as CommonStatus | '',
  targetType: '' as AssetType | '',
  pageNo: 1,
  pageSize: 20,
});

const loading = ref(false);
const tableData = ref<AppMetric[]>([]);
const total = ref(0);
const dialogVisible = ref(false);
const dialogMode = ref<'create' | 'edit'>('create');
const currentRow = ref<AppMetric>(createDefaultAppMetric());

async function fetchData() {
  loading.value = true;

  try {
    const res = await queryAppMetricPage(queryForm);
    tableData.value = res.records || [];
    total.value = res.total || 0;
  } finally {
    loading.value = false;
  }
}

/** 分类导航是唯一分类筛选入口；ALL 只转为空查询参数，绝不传给后端保存。 */
function handleCategoryChange(category: MetricCategoryFilter) {
  selectedCategory.value = category;
  queryForm.metricCategory = category === 'ALL' ? '' : category;
  queryForm.pageNo = 1;
  fetchData();
}

function handleSearch() {
  queryForm.pageNo = 1;
  fetchData();
}

/** 重置条件时有意保留左侧分类，避免用户失去当前浏览上下文。 */
function handleReset() {
  Object.assign(queryForm, {
    keyword: '',
    status: '',
    targetType: '',
    pageNo: 1,
  });
  fetchData();
}

function openCreate() {
  dialogMode.value = 'create';
  currentRow.value = {
    ...createDefaultAppMetric(),
    metricCategory:
      selectedCategory.value === 'ALL' ? '' : selectedCategory.value,
  };
  dialogVisible.value = true;
}

function openEdit(row: AppMetric) {
  dialogMode.value = 'edit';
  currentRow.value = { ...row };
  dialogVisible.value = true;
}

async function toggleStatus(row: AppMetric) {
  const nextStatus =
    row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED';
  const action = nextStatus === 'DISABLED' ? '停用' : '启用';

  await ElMessageBox.confirm(
    `确认${action}「${row.metricName}」？`,
    '提示',
    {
      type: 'warning',
    },
  );

  await updateAppMetricStatus(row.metricCode, nextStatus);
  ElMessage.success(`${action}成功`);
  fetchData();
}

async function remove(row: AppMetric) {
  await ElMessageBox.confirm(
    `确认删除「${row.metricName}」？`,
    '提示',
    {
      type: 'warning',
    },
  );

  await deleteAppMetric(row.metricCode);
  ElMessage.success('删除成功');
  fetchData();
}

onMounted(fetchData);
</script>

<style scoped>
.application-config-page {
  min-height: 100%;
  padding: 16px;
  background: #f5f7fa;
  box-sizing: border-box;
}

.metric-layout {
  display: flex;
  align-items: stretch;
  gap: 16px;
}

.category-card {
  width: 220px;
  flex: 0 0 220px;
  border-radius: 8px;
}

.list-card {
  min-width: 0;
  flex: 1;
  border-radius: 8px;
}

.category-title,
.card-title {
  color: #303133;
  font-size: 16px;
  font-weight: 600;
}

.category-list {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.category-button {
  width: 100%;
  margin: 0;
  justify-content: flex-start;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.query-form {
  margin-bottom: 12px;
  padding: 12px 12px 0;
  background: #fafafa;
  border-radius: 6px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
</style>
