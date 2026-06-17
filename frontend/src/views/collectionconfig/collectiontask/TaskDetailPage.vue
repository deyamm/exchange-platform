<template>
    <div class="task-detail-page">
        <div class="page-header">
            <div class="header-left">
                <el-button @click="$router.back()">返回</el-button>
                <span class="page-title">
                    采集任务详情：{{ detail?.taskInfo?.taskName }}（{{ taskCode }}）
                </span>
            </div>
            <el-button type="primary" @click="goConfigMappingPage">
                任务配置与存储映射
            </el-button>
        </div>

        <div v-loading="loading" class="detail-content">
            <template v-if="detail">
                <!-- 任务基础信息 -->
                <el-card shadow="never" class="detail-card">
                    <template #header>
                        <span class="section-title">任务基础信息</span>
                    </template>
                    <el-descriptions :column="3" border>
                        <el-descriptions-item label="任务编码">{{ detail.taskInfo.taskCode }}</el-descriptions-item>
                        <el-descriptions-item label="任务名称">{{ detail.taskInfo.taskName }}</el-descriptions-item>
                        <el-descriptions-item label="当前版本号">V{{ detail.taskInfo.currentVersionNo }}</el-descriptions-item>
                        <el-descriptions-item label="同步状态">
                            <el-tag :type="SYNC_STATUS_TAG_TYPE[detail.taskInfo.syncStatus as SyncStatus]">
                                {{ SYNC_STATUS_TEXT[detail.taskInfo.syncStatus as SyncStatus] }}
                            </el-tag>
                        </el-descriptions-item>
                        <el-descriptions-item label="启停状态">
                            <el-tag :type="COMMON_STATUS_TAG_TYPE[detail.taskInfo.status]">
                                {{ COMMON_STATUS_TEXT[detail.taskInfo.status] }}
                            </el-tag>
                        </el-descriptions-item>
                        <el-descriptions-item label="最近同步时间">{{ detail.taskInfo.latestSyncTime }}</el-descriptions-item>
                    </el-descriptions>
                </el-card>

                <!-- 当前模板版本 -->
                <el-card shadow="never" class="detail-card" v-if="detail.currentVersion">
                    <template #header>
                        <div class="section-header">
                            <span class="section-title">当前模板版本（V{{ detail.currentVersion.versionNo }}）</span>
                            <el-button link type="primary" @click="showVersionList = true">查看历史版本</el-button>
                        </div>
                    </template>
                    <el-descriptions :column="3" border>
                        <el-descriptions-item label="处理入口">{{ detail.currentVersion.handlerName }}</el-descriptions-item>
                        <el-descriptions-item label="数据源">{{ detail.currentVersion.dataSource }}</el-descriptions-item>
                        <el-descriptions-item label="资产类型">{{ detail.currentVersion.assetType }}</el-descriptions-item>
                        <el-descriptions-item label="业务分类">{{ detail.currentVersion.bizType }}</el-descriptions-item>
                        <el-descriptions-item label="同步时间">{{ detail.currentVersion.syncTime }}</el-descriptions-item>
                        <el-descriptions-item label="字段哈希" :span="3">
                            <span class="mono-text">{{ detail.currentVersion.outputFieldsHash }}</span>
                        </el-descriptions-item>
                        <el-descriptions-item label="变更摘要" :span="3">{{ detail.currentVersion.changeSummary }}</el-descriptions-item>
                    </el-descriptions>
                    
                    <div class="json-section" v-if="detail.currentVersion.paramsSchemaJson">
                        <div class="json-label">
                            请求字段列表
                        </div>
                        <el-table
                            :data="parseParamFields(detail.currentVersion.paramsSchemaJson)"
                            border
                            class="output-fields-table"
                        >
                            <el-table-column label="序号" width="80" align="center">
                                <template #default="{ $index }">
                                    {{ $index + 1 }}
                                </template>
                            </el-table-column>
                            <el-table-column prop="fieldCode" label="字段编码" width="200" show-overflow-tooltip />
                            <el-table-column prop="fieldName" label="字段名称" width="200" show-overflow-tooltip />
                            <el-table-column prop="fieldType" label="字段类型" width="100" align="center" />
                            <el-table-column label="必填" width="100" align="center">
                                <template #default="{ row }">
                                    <el-tag v-if="row.required" type="warning">是</el-tag>
                                    <el-tag v-else type="info" >否</el-tag>
                                </template>
                            </el-table-column>
                            <el-table-column prop="fieldDesc" label="描述" min-width="100" align="center" />
                        </el-table>
                    </div>

                    <!-- 返回字段表格展示 -->
                    <div class="json-section" v-if="detail.currentVersion.outputFieldsJson">
                        <div class="json-label">
                            返回字段列表
                        </div>
                        <el-table
                            :data="parseParamFields(detail.currentVersion.outputFieldsJson)"
                            border
                            class="output-fields-table"
                        >
                            <el-table-column label="序号" width="80" align="center">
                                <template #default="{ $index }">
                                    {{ $index + 1 }}
                                </template>
                            </el-table-column>
                            <el-table-column prop="fieldCode" label="字段编码" width="200" show-overflow-tooltip />
                            <el-table-column prop="fieldName" label="字段名称" width="200" show-overflow-tooltip />
                            <el-table-column prop="fieldType" label="字段类型" width="100" align="center" />
                            <el-table-column label="必填" width="100" align="center">
                                <template #default="{ row }">
                                    <el-tag v-if="row.required" type="warning">是</el-tag>
                                    <el-tag v-else type="info" >否</el-tag>
                                </template>
                            </el-table-column>
                            <el-table-column label="唯一键" width="100" align="center">
                                <template #default="{ row }">
                                    <el-tag v-if="row.uniqueKey" type="warning">是</el-tag>
                                    <el-tag v-else type="info">否</el-tag>
                                </template>
                            </el-table-column>
                            <el-table-column prop="fieldDesc" label="描述" min-width="100" align="center" />
                        </el-table>
                    </div>
                </el-card>
            </template>
        </div>

        <!-- 历史版本对话框 -->
        <el-dialog v-model="showVersionList" title="模板版本历史" width="900px" destroy-on-close>
            <el-table :data="versionList" border>
                <el-table-column prop="versionNo" label="版本号" width="80" align="center" />
                <el-table-column prop="syncTime" label="同步时间" min-width="170" />
                <el-table-column prop="changeSummary" label="变更摘要" min-width="200" show-overflow-tooltip />
                <el-table-column prop="outputFieldsHash" label="字段哈希" min-width="220" show-overflow-tooltip />
            </el-table>
        </el-dialog>
    </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import type { TaskDetail, TaskTemplateVersion } from '/@/types/collectiontask/entity'
import type { SyncStatus } from '/@/types/collectiontask/constants'
import {
    SYNC_STATUS_TEXT, SYNC_STATUS_TAG_TYPE,
    COMMON_STATUS_TEXT, COMMON_STATUS_TAG_TYPE
} from '/@/types/collectiontask/view'
import { getTaskDetail, listTemplateVersions } from '/@/api/collectiontask/collectionTaskApi'
import { parseParamFields } from '/@/utils/utils'

const route = useRoute()
const router = useRouter()
const taskCode = route.params.taskCode as string

const loading = ref(false)
const detail = ref<TaskDetail | null>(null)
const showVersionList = ref(false)
const versionList = ref<TaskTemplateVersion[]>([])

onMounted(async () => {
    await loadDetail()
    versionList.value = await listTemplateVersions(taskCode)
})

async function loadDetail(): Promise<void> {
    loading.value = true
    try {
        detail.value = await getTaskDetail(taskCode)
    } finally {
        loading.value = false
    }
}

function goConfigMappingPage(): void {
    router.push({
        path: `/collectionTask/config/${taskCode}`
    })
}
</script>

<style scoped>
.task-detail-page {
    padding: 16px;
}

.page-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    margin-bottom: 16px;
}

.header-left {
    display: flex;
    align-items: center;
    gap: 12px;
    min-width: 0;
}

.page-title {
    font-weight: 600;
    font-size: 16px;
}

.detail-content {
    display: flex;
    flex-direction: column;
    gap: 16px;
}

.section-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.section-title {
    font-weight: 600;
}

.json-section {
    margin-top: 16px;
}

.json-label {
    font-size: 12px;
    font-weight: 600;
    color: #606266;
    margin-bottom: 8px;
}

.mono-text {
    font-family: Consolas, Monaco, monospace;
    font-size: 12px;
}

.output-fields-table {
    width: 100%;
}
</style>