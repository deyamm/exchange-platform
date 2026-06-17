<template>
    <div class="monitor-page">
        <el-card class="list-card" shadow="never">
            <template #header>
                <div class="card-header"><span class="card-title">异常与监控</span></div>
            </template>

            <!-- 日志 / 异常 / 问题数据 三类查询切换 -->
            <el-tabs v-model="activeTab" @tab-change="handleSearch">
                <el-tab-pane label="运行日志" name="logs" />
                <el-tab-pane label="异常记录" name="exceptions" />
                <el-tab-pane label="问题数据" name="problems" />
            </el-tabs>

            <el-form :model="queryForm" inline class="query-form">
                <el-form-item label="执行编号">
                    <el-input v-model="queryForm.runId" clearable placeholder="runId" style="width: 200px" @keyup.enter="handleSearch" />
                </el-form-item>
                <el-form-item label="任务编码">
                    <el-input v-model="queryForm.taskCode" clearable placeholder="任务编码" style="width: 170px" @keyup.enter="handleSearch" />
                </el-form-item>
                <el-form-item class="query-actions">
                    <el-button type="primary" @click="handleSearch">查询</el-button>
                    <el-button @click="handleReset">重置</el-button>
                </el-form-item>
            </el-form>

            <!-- 运行日志 -->
            <el-table v-if="activeTab === 'logs'" v-loading="tableLoading" :data="tableData" border height="calc(100vh - 350px)">
                <el-table-column prop="runId" label="执行编号" min-width="200" align="center" show-overflow-tooltip />
                <el-table-column prop="logLevel" label="级别" width="90" align="center" />
                <el-table-column prop="logContent" label="日志内容" min-width="320" show-overflow-tooltip />
                <el-table-column prop="createdAt" label="时间" min-width="170" align="center" />
            </el-table>

            <!-- 异常记录 -->
            <el-table v-else-if="activeTab === 'exceptions'" v-loading="tableLoading" :data="tableData" border height="calc(100vh - 350px)">
                <el-table-column prop="exceptionId" label="异常编号" min-width="180" align="center" show-overflow-tooltip />
                <el-table-column prop="runId" label="执行编号" min-width="180" align="center" show-overflow-tooltip />
                <el-table-column prop="exceptionType" label="异常类型" min-width="120" align="center" />
                <el-table-column prop="exceptionMessage" label="异常信息" min-width="300" show-overflow-tooltip />
                <el-table-column prop="createdAt" label="时间" min-width="170" align="center" />
            </el-table>

            <!-- 问题数据 -->
            <el-table v-else v-loading="tableLoading" :data="tableData" border height="calc(100vh - 350px)">
                <el-table-column prop="problemId" label="问题编号" min-width="180" align="center" show-overflow-tooltip />
                <el-table-column prop="runId" label="执行编号" min-width="180" align="center" show-overflow-tooltip />
                <el-table-column prop="dataTypeCode" label="数据类型" min-width="120" align="center" />
                <el-table-column prop="problemType" label="问题类型" min-width="120" align="center" />
                <el-table-column prop="problemMessage" label="问题说明" min-width="280" show-overflow-tooltip />
                <el-table-column prop="createdAt" label="时间" min-width="170" align="center" />
            </el-table>

            <div class="pagination-wrap">
                <el-pagination
                    v-model:current-page="pagination.pageNo"
                    v-model:page-size="pagination.pageSize"
                    :total="pagination.total"
                    :page-sizes="[10, 20, 50, 100]"
                    layout="total, sizes, prev, pager, next, jumper"
                    @size-change="loadTable"
                    @current-change="loadTable"
                />
            </div>
        </el-card>
    </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { queryRunLogs, queryExceptionRecords, queryProblemRecords } from '/@/api/monitor/queryApi'

type TabName = 'logs' | 'exceptions' | 'problems'

const activeTab = ref<TabName>('logs')
const tableData = ref<any[]>([])
const tableLoading = ref(false)

const queryForm = reactive({ runId: '', taskCode: '' })
const pagination = reactive({ pageNo: 1, pageSize: 20, total: 0 })

/** 按当前 Tab 调用对应查询接口 */
async function loadTable(): Promise<void> {
    tableLoading.value = true
    try {
        const params = {
            pageNo: pagination.pageNo,
            pageSize: pagination.pageSize,
            runId: queryForm.runId || undefined,
            taskCode: queryForm.taskCode || undefined
        }
        let result
        if (activeTab.value === 'logs') {
            result = await queryRunLogs(params)
        } else if (activeTab.value === 'exceptions') {
            result = await queryExceptionRecords(params)
        } else {
            result = await queryProblemRecords(params)
        }
        tableData.value = result.records || []
        pagination.total = result.total || 0
    } finally {
        tableLoading.value = false
    }
}

function handleSearch(): void {
    pagination.pageNo = 1
    loadTable()
}

function handleReset(): void {
    queryForm.runId = ''
    queryForm.taskCode = ''
    handleSearch()
}

onMounted(() => loadTable())
</script>

<style scoped>
.monitor-page { 
    height: calc(100vh - 90px); 
    padding: 8px; 
    background: #f5f7fa; 
    box-sizing: border-box; 
}

.list-card { 
    height: 100%; 
    border-radius: 10px; 
    overflow: hidden; 
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
    display: flex; 
    flex-wrap: wrap; 
    padding: 16px 16px 4px; 
    margin-bottom: 12px;
    background: #fafafa; 
    border: 1px solid #ebeef5; 
    border-radius: 8px;
}

.query-actions { 
    margin-left: auto; 
}

.pagination-wrap { 
    display: flex; 
    justify-content: center;
     padding-top: 16px; 
}

:deep(.el-table th.el-table__cell) { 
    background: #f7f8fa; 
    color: #606266; 
    font-weight: 600; 
}
</style>
