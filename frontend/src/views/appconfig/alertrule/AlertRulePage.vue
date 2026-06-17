<template>
    <div class="application-config-page">
        <el-card shadow="never" class="list-card">
            <template #header>
                <div class="card-header">
                    <span class="card-title">提醒规则配置</span>
                    <el-button type="primary" @click="openCreate">新增提醒规则配置</el-button>
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
                <el-table-column prop="ruleCode" label="编码" min-width="160" align="center" />
                <el-table-column prop="ruleName" label="名称" min-width="160" align="center" />
                <el-table-column prop="ruleType" label="规则类型" width="130" align="center" />
                <el-table-column prop="targetCode" label="标的编码" width="130" align="center" />
                <el-table-column prop="groupCode" label="分组编码" width="130" align="center" />
                <el-table-column prop="metricCode" label="指标编码" width="130" align="center" />
                <el-table-column prop="status" label="状态" width="100" align="center">
                    <template #default="{ row }"><el-tag :type="COMMON_STATUS_TAG_TYPE[row.status as CommonStatus]">{{
                        COMMON_STATUS_TEXT[row.status as CommonStatus] || row.status }}</el-tag></template>
                </el-table-column>
                <el-table-column prop="description" label="说明" min-width="220" show-overflow-tooltip align="center" />
                <el-table-column label="操作" width="160" fixed="right" align="center">
                    <template #default="{ row }">
                        <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
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
        <AlertRuleFormDialog v-model="dialogVisible" :mode="dialogMode" :form-data="currentRow" @success="fetchData" />
    </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { queryAlertRulePage, deleteAlertRule } from '/@/api/alertrule/alertRuleApi';
import { createDefaultAlertRule, type AlertRule } from '/@/types/alertrule/entity';
import { COMMON_STATUS_OPTIONS, COMMON_STATUS_TEXT, COMMON_STATUS_TAG_TYPE } from '/@/types/appscene/view';
import AlertRuleFormDialog from './AlertRuleFormDialog.vue';
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

async function remove(row: AlertRule) {
    await ElMessageBox.confirm(`确认删除「${row.ruleName}」？`, '提示', { type: 'warning' });
    await deleteAlertRule(row.ruleCode);
    ElMessage.success('删除成功');
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
