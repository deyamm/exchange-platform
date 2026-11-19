<template>
    <el-drawer v-model="visibleModel" title="同步批次详情" size="620px" destroy-on-close>
        <el-empty v-if="!batch" description="暂无同步批次详情" />
        <el-tabs v-else v-model="activeTab">
            <el-tab-pane label="基本信息" name="basic">
                <el-descriptions :column="2" border>
                    <el-descriptions-item label="批次 ID">{{ batch.id }}</el-descriptions-item>
                    <el-descriptions-item label="状态">
                        <el-tag :type="statusTagType(batch.status)">{{ batch.status }}</el-tag>
                    </el-descriptions-item>
                    <el-descriptions-item label="MasterDataType">{{ valueOf(batch.masterDataType) }}</el-descriptions-item>
                    <el-descriptions-item label="来源绑定">{{ valueOf(batch.bindingCode) }}</el-descriptions-item>
                    <el-descriptions-item label="触发方式">{{ valueOf(batch.triggerType) }}</el-descriptions-item>
                    <el-descriptions-item label="资产类型">
                        {{ batch.assetType === 'STOCK' ? '股票' : batch.assetType === 'INDEX' ? '指数' : '关系' }}
                    </el-descriptions-item>
                    <el-descriptions-item label="Dataset">{{ valueOf(batch.datasetCode) }}</el-descriptions-item>
                    <el-descriptions-item label="来源">{{ valueOf(batch.sourceCode) }}</el-descriptions-item>
                    <el-descriptions-item label="任务运行 ID">{{ valueOf(batch.taskRunId) }}</el-descriptions-item>
                    <el-descriptions-item label="同步类型">{{ batch.syncType }}</el-descriptions-item>
                    <el-descriptions-item label="开始时间">{{ valueOf(batch.startedAt) }}</el-descriptions-item>
                    <el-descriptions-item label="发布时间">{{ valueOf(batch.publishedAt) }}</el-descriptions-item>
                </el-descriptions>
            </el-tab-pane>
            <el-tab-pane label="数据变化" name="changes">
                <el-descriptions :column="2" border>
                    <el-descriptions-item label="总数">{{ batch.totalCount }}</el-descriptions-item>
                    <el-descriptions-item label="新增">{{ batch.addedCount }}</el-descriptions-item>
                    <el-descriptions-item label="更新">{{ batch.updatedCount }}</el-descriptions-item>
                    <el-descriptions-item label="失效">{{ batch.removedCount }}</el-descriptions-item>
                </el-descriptions>
            </el-tab-pane>
            <el-tab-pane label="错误 / 发布结果" name="result">
                <el-alert
                    v-if="batch.status !== 'FAILED'"
                    title="该批次未返回失败错误。"
                    type="success"
                    :closable="false"
                    show-icon
                />
                <el-input
                    v-else
                    :model-value="batch.errorMessage || '-'"
                    type="textarea"
                    :rows="10"
                    readonly
                    resize="none"
                    aria-label="完整错误信息"
                />
            </el-tab-pane>
        </el-tabs>
        <template #footer>
            <el-button @click="visibleModel = false">关闭</el-button>
            <el-button v-if="canRepublish" type="primary" :loading="republishing" @click="confirmRepublish">重新发布</el-button>
        </template>
    </el-drawer>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import type { SyncBatch } from '/@/types/masterdata'
import { statusTagType } from '/@/types/masterdata'

const props = defineProps<{
    modelValue: boolean;
    batch?: SyncBatch | null;
    republishing?: boolean
}>()
const emit = defineEmits<{
    (event: 'update:modelValue', value: boolean): void;
    (event: 'republish', batch: SyncBatch): void
}>()
const visibleModel = computed({
    get: () => props.modelValue,
    set: value => emit('update:modelValue', value)
})
const activeTab = ref('basic')
const valueOf = (value: unknown) => value === null || value === undefined || value === '' ? '-' : String(value)

/** 后端未返回 republishable 时采用最保守的失败批次规则；有字段时以后端裁决为准。 */
const canRepublish = computed(() => props.batch?.republishable ?? props.batch?.status === 'FAILED')

async function confirmRepublish(): Promise<void> {
    if (!props.batch || props.republishing)
        return
    await ElMessageBox.confirm('将基于该批次已有 Dataset 重新执行主数据读取、校验和发布。不会重新执行外部数据采集。', '重新发布确认', {
        type: 'warning',
        confirmButtonText: '重新发布',
        cancelButtonText: '取消'
    })
    emit('republish', props.batch)
}
</script>