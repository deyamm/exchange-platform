<template>
    <div class="application-config-page">
        <el-card shadow="never" class="list-card">
            <template #header>
                <div class="card-header">
                    <span class="card-title">提醒规则</span>
                    <el-button type="primary" @click="openCreate">新建提醒</el-button>
                </div>
            </template>
            <el-form :model="queryForm" inline class="query-form">
                <el-form-item label="关键字">
                    <el-input v-model="queryForm.keyword" clearable placeholder="编码/名称" style="width: 220px"
                        @keyup.enter="handleSearch" />
                </el-form-item>
                <el-form-item label="状态">
                    <el-select v-model="queryForm.status" clearable placeholder="全部" style="width: 140px">
                        <el-option v-for="item in COMMON_STATUS_OPTIONS" :key="item.value" :label="item.label"
                            :value="item.value" />
                    </el-select>
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" @click="handleSearch">查询</el-button>
                    <el-button @click="handleReset">重置</el-button>
                </el-form-item>
            </el-form>
            <el-table v-loading="loading" :data="tableData" border row-key="ruleCode" height="calc(100vh - 330px)">
                <el-table-column prop="ruleName" label="规则名称" min-width="180" align="center" />
                <el-table-column prop="targetCode" label="标的" min-width="140" align="center">
                    <template #default="{ row }">{{ row.targetCode }} <small>{{ row.targetType }}</small></template>
                </el-table-column>
                <el-table-column prop="metricCode" label="指标" min-width="140" align="center" />
                <el-table-column label="条件" min-width="120" align="center">
                    <template #default="{ row }">{{ conditionText(row) }}</template>
                </el-table-column>
                <el-table-column prop="status" label="状态" width="100" align="center">
                    <template #default="{ row }"><el-tag :type="COMMON_STATUS_TAG_TYPE[row.status as CommonStatus]">{{
                        COMMON_STATUS_TEXT[row.status as CommonStatus] || row.status }}</el-tag></template>
                </el-table-column>
                <el-table-column prop="description" label="说明" min-width="220" show-overflow-tooltip align="center" />
                <el-table-column label="操作" width="160" fixed="right" align="center">
                    <template #default="{ row }">
                        <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
                        <el-button link @click="toggleStatus(row)">{{ row.status === 'ENABLED' ? '停用' : '启用' }}</el-button>
                        <el-button link type="danger" @click="remove(row)">删除</el-button>
                    </template>
                </el-table-column>
            </el-table>
            <div class="pagination-wrapper">
                <el-pagination v-model:current-page="queryForm.pageNo" v-model:page-size="queryForm.pageSize" background
                    layout="total, sizes, prev, pager, next, jumper" :total="total" @current-change="fetchData"
                    @size-change="handleSearch" />
            </div>
        </el-card>
        <AlertRuleEditorDialog v-model:open="dialogVisible" :target-type="currentRow.targetType || 'STOCK'" :target-code="dialogMode === 'edit' ? currentRow.targetCode : undefined" :target-name="currentRow.targetCode" :form-data="currentRow" @success="fetchData" />
    </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { queryAlertRulePage, deleteAlertRule, updateAlertRuleStatus } from '/@/api/alertrule/alertRuleApi';
import { createDefaultAlertRule, type AlertRule } from '/@/types/alertrule/entity';
import { COMMON_STATUS_OPTIONS, COMMON_STATUS_TEXT, COMMON_STATUS_TAG_TYPE } from '/@/types/appscene/view';
import AlertRuleEditorDialog from '/@/components/common/AlertRuleEditorDialog.vue';
import { CommonStatus } from '/@/types/appscene/constants.js';

const queryForm = reactive<any>({ 
    ruleCode: '',
    ruleName: '', 
    status: '', 
    pageNo: 1, 
    pageSize: 20 
});
const loading = ref(false);
const tableData = ref<AlertRule[]>([]);
const total = ref(0);
const dialogVisible = ref(false);
const dialogMode = ref<'create' | 'edit'>('create');
const currentRow = ref<AlertRule>(createDefaultAlertRule());

async function fetchData() {
    loading.value = true;
    try {
        const res = await queryAlertRulePage(queryForm);
        tableData.value = res.records || [];
        total.value = res.total || 0;
    } finally {
        loading.value = false;
    }
}
function handleSearch() { 
    queryForm.pageNo = 1; 
    fetchData(); 
}

function handleReset() { 
    Object.assign(queryForm, { 
        ruleCode: '', 
        ruleName: '', 
        status: '', 
        pageNo: 1, 
        pageSize: 20 
    }); 
    fetchData(); 
}

function openCreate() { 
    dialogMode.value = 'create'; 
    currentRow.value = createDefaultAlertRule(); 
    dialogVisible.value = true; 
}

function openEdit(row: AlertRule) { 
    dialogMode.value = 'edit'; 
    currentRow.value = { ...row }; 
    dialogVisible.value = true; 
}

function conditionText(row: AlertRule) {
    const condition = (row.condition || {}) as { operator?: string; threshold?: number };
    const labels: Record<string, string> = { GT: '>', GTE: '≥', LT: '<', LTE: '≤', EQ: '=', NE: '≠' };
    return condition.operator ? `${labels[condition.operator] || condition.operator} ${condition.threshold ?? ''}` : '-';
}

async function remove(row: AlertRule) {
    await ElMessageBox.confirm(`确认删除「${row.ruleName}」？`, '提示', { type: 'warning' });
    await deleteAlertRule(row.ruleCode);
    ElMessage.success('删除成功');
    fetchData();
}

async function toggleStatus(row: AlertRule) {
    const next = row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED';
    await updateAlertRuleStatus(row.ruleCode, next);
    ElMessage.success(next === 'ENABLED' ? '已启用' : '已停用');
    fetchData();
}

onMounted(fetchData);
</script>

<style scoped>
.application-config-page {
    padding: 16px;
    background: #f5f7fa;
    min-height: 100%;
}

.list-card {
    border-radius: 8px;
}

.card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.card-title {
    font-size: 16px;
    font-weight: 600;
    color: #303133;
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
