<template>
    <div ref="chartRef" class="target-echarts" :style="{ height }" />
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, shallowRef, watch } from 'vue'
import * as echarts from 'echarts'
import type { ECharts, EChartsOption } from 'echarts'

const props = withDefaults(
    defineProps<{
        option: EChartsOption
        height?: string
        loading?: boolean
        autoresize?: boolean
    }>(),
    {
        height: '360px',
        loading: false,
        autoresize: true
    }
)

const chartRef = ref<HTMLDivElement | null>(null)
const chartInstance = shallowRef<ECharts | null>(null)
let resizeObserver: ResizeObserver | null = null

function initChart(): void {
    if (!chartRef.value) return

    if (!chartInstance.value) {
        chartInstance.value = echarts.init(chartRef.value)
    }

    renderChart()
}

function renderChart(): void {
    const chart = chartInstance.value
    if (!chart) return

    if (props.loading) {
        chart.showLoading('default', {
            text: '加载中...'
        })
    } else {
        chart.hideLoading()
    }

    chart.setOption(props.option, true)
}

function resizeChart(): void {
    chartInstance.value?.resize()
}

onMounted(async () => {
    await nextTick()
    initChart()

    if (props.autoresize && chartRef.value && 'ResizeObserver' in window) {
        resizeObserver = new ResizeObserver(() => resizeChart())
        resizeObserver.observe(chartRef.value)
    }

    window.addEventListener('resize', resizeChart)
})

onBeforeUnmount(() => {
    window.removeEventListener('resize', resizeChart)

    if (resizeObserver && chartRef.value) {
        resizeObserver.unobserve(chartRef.value)
        resizeObserver.disconnect()
        resizeObserver = null
    }

    chartInstance.value?.dispose()
    chartInstance.value = null
})

watch(
    () => props.option,
    async () => {
        await nextTick()
        if (!chartInstance.value) {
            initChart()
        } else {
            renderChart()
        }
    },
    { deep: true }
)

watch(
    () => props.loading,
    () => renderChart()
)

defineExpose({
    resize: resizeChart,
    getInstance: () => chartInstance.value
})
</script>

<style scoped>
.target-echarts {
    width: 100%;
    min-height: 240px;
}
</style>
