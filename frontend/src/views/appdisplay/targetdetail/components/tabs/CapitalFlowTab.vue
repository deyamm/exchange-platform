<template>
  <div class="capital-flow-tab">
    <el-empty v-if="!capitalFlow" description="暂无资金流向数据" />

    <template v-else>
      <el-row :gutter="12">
        <!--el-col :xs="24" :sm="12" :lg="6">
          <el-card shadow="never" class="flow-card">
            <div class="flow-label">主力净流入</div>
            <div class="flow-value" :class="getValueClass(capitalFlow.mainNetInflow)">
              {{ formatAmount(capitalFlow.mainNetInflow) }}
            </div>
            <div class="flow-desc">交易日：{{ capitalFlow.tradeDate || '-' }}</div>
          </el-card>
        </el-col-->

        <el-col :xs="24" :sm="12" :lg="6">
          <el-card shadow="never" class="flow-card">
            <div class="flow-label">特大单净流入</div>
            <div class="flow-value" :class="getValueClass(capitalFlow.superLargeNetInflow)">
              {{ formatAmount(capitalFlow.superLargeNetInflow) }}
            </div>
            <div class="flow-desc">Super Large Orders</div>
          </el-card>
        </el-col>

        <el-col :xs="24" :sm="12" :lg="6">
          <el-card shadow="never" class="flow-card">
            <div class="flow-label">大单净流入</div>
            <div class="flow-value" :class="getValueClass(capitalFlow.largeNetInflow)">
              {{ formatAmount(capitalFlow.largeNetInflow) }}
            </div>
            <div class="flow-desc">Large Orders</div>
          </el-card>
        </el-col>

        <el-col :xs="24" :sm="12" :lg="6">
          <el-card shadow="never" class="flow-card">
            <div class="flow-label">中单净流入</div>
            <div class="flow-value" :class="getValueClass(capitalFlow.mediumNetInflow)">
              {{ formatAmount(capitalFlow.mediumNetInflow) }}
            </div>
            <div class="flow-desc">Medium Orders</div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :lg="6">
          <el-card shadow="never" class="flow-card">
            <div class="flow-label">小单净流入</div>
            <div class="flow-value" :class="getValueClass(capitalFlow.smallNetInflow)">
              {{ formatAmount(capitalFlow.smallNetInflow) }}
            </div>
            <div class="flow-desc">Small Orders</div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="12" class="chart-row">
        <el-col :xs="24" :lg="12">
          <el-card shadow="never">
            <TargetEcharts :option="capitalStructureOption" height="320px" />
          </el-card>
        </el-col>

        <el-col :xs="24" :lg="12">
          <el-card shadow="never">
            <TargetEcharts :option="northboundMarginOption" height="320px" />
          </el-card>
        </el-col>
      </el-row>

      <el-card shadow="never" class="history-card">
        <template #header>
          <div class="card-header">
            <span>小 / 中 / 大 / 特大资金历史流向</span>
            <span class="header-tip">点击图例可显示或隐藏对应资金</span>
          </div>
        </template>

        <el-empty
          v-if="!capitalFlow.historyFlows || capitalFlow.historyFlows.length === 0"
          description="暂无历史资金流向数据"
        />

        <TargetEcharts v-else :option="capitalFlowHistoryOption" height="420px" />
      </el-card>

      <el-card v-if="capitalFlow.conclusion" shadow="never" class="conclusion-card">
        <template #header>资金流向结论</template>
        <div class="conclusion-text">{{ capitalFlow.conclusion }}</div>
      </el-card>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { CapitalFlowInfo } from '/@/types/targetdetail/targetDetailCenter'
import TargetEcharts from '/@/views/appdisplay/targetdetail/components/charts/TargetEcharts.vue'
import {
  buildCapitalFlowHistoryStackedBarOption,
  buildCapitalStructureBarOption,
  buildNorthboundMarginBarOption
} from '/@/views/appdisplay/targetdetail/utils/targetDetailChartOptions'
import { formatAmount } from '/@/views/appdisplay/targetdetail/utils/targetDetailFormat'

const props = defineProps<{
  capitalFlow?: CapitalFlowInfo
}>()

const smallMediumNetInflow = computed(() => {
  return (props.capitalFlow?.smallNetInflow || 0) + (props.capitalFlow?.mediumNetInflow || 0)
})

const capitalStructureOption = computed(() => {
  return buildCapitalStructureBarOption(props.capitalFlow)
})

const northboundMarginOption = computed(() => {
  return buildNorthboundMarginBarOption(props.capitalFlow)
})

const capitalFlowHistoryOption = computed(() => {
  return buildCapitalFlowHistoryStackedBarOption(props.capitalFlow?.historyFlows || [])
})

const getValueClass = (value?: number) => {
  if (!value) return 'is-flat'
  return value > 0 ? 'is-up' : 'is-down'
}
</script>

<style scoped>
.capital-flow-tab {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.flow-card {
  height: 116px;
}

.flow-label {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.flow-value {
  margin-top: 10px;
  font-size: 22px;
  font-weight: 700;
  line-height: 1.2;
}

.flow-desc {
  margin-top: 8px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.is-up {
  color: var(--el-color-danger);
}

.is-down {
  color: var(--el-color-success);
}

.is-flat {
  color: var(--el-text-color-regular);
}

.chart-row {
  row-gap: 12px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.header-tip {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  font-weight: 400;
}

.conclusion-text {
  color: var(--el-text-color-regular);
  line-height: 1.8;
}

@media (max-width: 768px) {
  .card-header {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
