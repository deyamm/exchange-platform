<template>
    <el-dialog
        :model-value="modelValue"
        title="标的概览详情"
        width="560px"
        destroy-on-close
        @close="handleClose"
    >
        <el-descriptions v-if="target" :column="1" border>
            <el-descriptions-item label="标的编码">{{ target.targetCode }}</el-descriptions-item>
            <el-descriptions-item label="标的名称">{{ target.targetName }}</el-descriptions-item>
            <el-descriptions-item label="标的类型">{{ target.targetType || '-' }}</el-descriptions-item>
            <el-descriptions-item label="所属市场">{{ target.marketCode || '-' }}</el-descriptions-item>
            <el-descriptions-item label="关注分组">{{ target.targetGroups || '-' }}</el-descriptions-item>
            <el-descriptions-item label="重点标记">
                <el-tag v-if="target.importantFlag" type="warning" effect="plain">重点</el-tag>
                <span v-else>否</span>
            </el-descriptions-item>
            <el-descriptions-item label="观察状态">
                <el-tag :type="WATCH_STATUS_TAG_TYPE[target.watchStatus as WatchStatus] || 'info'">
                    {{ WATCH_STATUS_TEXT[target.watchStatus as WatchStatus] || target.watchStatus }}
                </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="关注理由">{{ target.watchReason || '-' }}</el-descriptions-item>
        </el-descriptions>
        <el-empty v-else description="暂无数据" :image-size="80" />

        <template #footer>
            <el-button type="primary" @click="handleClose">关闭</el-button>
        </template>
    </el-dialog>
</template>

<script setup lang="ts">
import { WATCH_STATUS_TEXT, WATCH_STATUS_TAG_TYPE } from '/@/types/watchtarget/view'
import type { WatchTarget } from '/@/types/watchtarget/entity'
import { WatchStatus } from '/@/types/watchtarget/constants';

defineProps<{
    modelValue: boolean
    target: WatchTarget | null
}>()

const emit = defineEmits<{
    (e: 'update:modelValue', value: boolean): void
}>()

function handleClose(): void {
    emit('update:modelValue', false)
}
</script>
