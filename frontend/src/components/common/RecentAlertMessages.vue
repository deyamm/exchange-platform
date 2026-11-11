<template>
  <el-card class="recent-alerts" shadow="never">
    <template #header><span class="section-title">最近提醒</span></template>
    <div v-loading="loading" class="message-list">
      <AlertMessageCard v-for="message in messages" :key="message.id" :message="message" @view-target="viewTarget" />
      <el-empty v-if="!loading && !messages.length" description="暂无提醒消息" :image-size="56" />
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { queryRecentAlertMessages } from '/@/api/alertmessage/alertMessageApi'
import type { AlertMessage } from '/@/types/alertmessage'
import AlertMessageCard from './AlertMessageCard.vue'
import { DETAIL_RECENT_ALERT_COUNT } from '/@/config/alert'

const props = withDefaults(defineProps<{ targetType: string; targetCode: string; limit?: number }>(), { limit: DETAIL_RECENT_ALERT_COUNT })
const messages = ref<AlertMessage[]>([])
const loading = ref(false)
const router = useRouter()
async function load() {
  if (!props.targetCode) return
  loading.value = true
  try { messages.value = await queryRecentAlertMessages(props.targetType, props.targetCode, props.limit) || [] } finally { loading.value = false }
}
function viewTarget(targetType: string, targetCode: string) {
  const name = targetType === 'INDEX' ? 'indexDetailByCode' : 'targetDetailByCode'
  const params = targetType === 'INDEX' ? { fullSymbol: targetCode } : { targetCode }
  void router.push({ name, params })
}
watch(() => [props.targetType, props.targetCode, props.limit], load)
onMounted(load)
</script>

<style scoped>
.recent-alerts { border-radius: 12px; }
.message-list { display: flex; flex-direction: column; gap: 10px; }
.section-title { font-weight: 600; }
</style>
