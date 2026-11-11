<template>
  <el-card class="alert-message-card" shadow="never">
    <div class="message-head">
      <span class="message-title">{{ message.messageTitle }}</span>
      <span class="message-time">{{ formatDateTime(message.triggeredAt) }}</span>
    </div>
    <div class="message-content">{{ message.messageContent }}</div>
    <div class="message-footer">
      <el-button link type="primary" @click="emit('view-target', message.targetType, message.targetCode)">查看标的</el-button>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import type { AlertMessage } from '/@/types/alertmessage'
import { formatDateTime } from '/@/utils/marketUtils'
defineProps<{ message: AlertMessage }>()
const emit = defineEmits<{ (e: 'view-target', targetType: string, targetCode: string): void }>()
</script>

<style scoped>
.alert-message-card { border-radius: 10px; }
.message-head { display: flex; justify-content: space-between; gap: 12px; align-items: center; }
.message-title { font-weight: 600; color: var(--el-text-color-primary); }
.message-time { color: var(--el-text-color-secondary); font-size: 12px; white-space: nowrap; }
.message-content { margin-top: 10px; line-height: 1.7; color: var(--el-text-color-regular); }
.message-footer { display: flex; justify-content: flex-end; margin-top: 8px; }
</style>
