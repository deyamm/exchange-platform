import type { EChartsOption } from 'echarts'
import type { ChartGroup, OHLCData, WeightItem, CapitalFlowHistoryItem, CapitalFlowInfo } from '/@/types/targetdetail/targetDetailCenter'
import { formatAmount, formatPercent } from './targetDetailFormat'

function isOhlcData(value: unknown): value is OHLCData {
    return Boolean(value && typeof value === 'object' && 'date' in value && 'open' in value && 'close' in value)
}

export function getOhlcRows(chart?: ChartGroup): OHLCData[] {
    if (!chart?.series?.length) return []
    return chart.series[0].data.filter(isOhlcData)
}

export function buildCandlestickOption(chart: ChartGroup): EChartsOption {
    const rows = getOhlcRows(chart)
    const dates = rows.map(item => item.date)
    const kData = rows.map(item => [item.open, item.close, item.low, item.high])
    const volumeData = rows.map((item, index) => ({
        value: item.volume || 0,
        itemStyle: {
            color: item.close >= item.open ? '#ef5350' : '#26a69a'
        },
        dataIndex: index
    }))

    return {
        animation: false,
        tooltip: {
            trigger: 'axis',
            axisPointer: {
                type: 'cross'
            },
            formatter(params: any) {
                const list = Array.isArray(params) ? params : [params]
                const kItem = list.find((item: any) => item.seriesType === 'candlestick')
                const volumeItem = list.find((item: any) => item.seriesName === '成交量')
                if (!kItem) return ''

                const row = rows[kItem.dataIndex]
                return [
                    row.date,
                    `开盘：${row.open}`,
                    `收盘：${row.close}`,
                    `最低：${row.low}`,
                    `最高：${row.high}`,
                    `成交量：${formatAmount(row.volume)}`,
                    `成交额：${formatAmount(row.amount)}`,
                    volumeItem ? `量柱：${formatAmount(volumeItem.value)}` : ''
                ].filter(Boolean).join('<br/>')
            }
        },
        legend: {
            top: 0,
            data: ['K线', '成交量']
        },
        grid: [
            {
                left: 56,
                right: 24,
                top: 40,
                height: 230
            },
            {
                left: 56,
                right: 24,
                top: 305,
                height: 80
            }
        ],
        xAxis: [
            {
                type: 'category',
                data: dates,
                boundaryGap: true,
                axisLine: { lineStyle: { color: '#909399' } },
                axisLabel: { color: '#606266' },
                min: 'dataMin',
                max: 'dataMax'
            },
            {
                type: 'category',
                gridIndex: 1,
                data: dates,
                boundaryGap: true,
                axisLine: { lineStyle: { color: '#909399' } },
                axisLabel: { color: '#606266' },
                min: 'dataMin',
                max: 'dataMax'
            }
        ],
        yAxis: [
            {
                scale: true,
                axisLine: { lineStyle: { color: '#909399' } },
                axisLabel: { color: '#606266' },
                splitLine: { lineStyle: { color: '#ebeef5' } }
            },
            {
                scale: true,
                gridIndex: 1,
                axisLabel: {
                    color: '#606266',
                    formatter(value: number) {
                        return formatAmount(value)
                    }
                },
                splitLine: { lineStyle: { color: '#ebeef5' } }
            }
        ],
        dataZoom: [
            {
                type: 'inside',
                xAxisIndex: [0, 1],
                start: Math.max(0, 100 - Math.min(100, Math.round(30 / Math.max(rows.length, 1) * 100))),
                end: 100
            },
            {
                show: true,
                xAxisIndex: [0, 1],
                type: 'slider',
                bottom: 0,
                height: 22,
                start: Math.max(0, 100 - Math.min(100, Math.round(30 / Math.max(rows.length, 1) * 100))),
                end: 100
            }
        ],
        series: [
            {
                name: 'K线',
                type: 'candlestick',
                data: kData,
                itemStyle: {
                    color: '#ef5350',
                    color0: '#26a69a',
                    borderColor: '#ef5350',
                    borderColor0: '#26a69a'
                }
            },
            {
                name: '成交量',
                type: 'bar',
                xAxisIndex: 1,
                yAxisIndex: 1,
                data: volumeData
            }
        ]
    }
}

export function buildLineOption(chart: ChartGroup, percent = false): EChartsOption {
    const xAxis = chart.xAxis || chart.series[0]?.data.map((_, index) => String(index + 1)) || []

    return {
        tooltip: {
            trigger: 'axis',
            valueFormatter(value: any) {
                const num = Number(value)
                return percent ? formatPercent(num) : formatAmount(num)
            }
        },
        legend: {
            top: 0
        },
        grid: {
            left: 56,
            right: 24,
            top: 42,
            bottom: 46
        },
        xAxis: {
            type: 'category',
            boundaryGap: false,
            data: xAxis,
            axisLabel: { color: '#606266' },
            axisLine: { lineStyle: { color: '#909399' } }
        },
        yAxis: {
            type: 'value',
            axisLabel: {
                color: '#606266',
                formatter(value: number) {
                    return percent ? `${value}%` : formatAmount(value)
                }
            },
            splitLine: { lineStyle: { color: '#ebeef5' } }
        },
        dataZoom: [
            {
                type: 'inside'
            },
            {
                type: 'slider',
                bottom: 8,
                height: 22
            }
        ],
        series: chart.series.map(item => ({
            name: item.name,
            type: 'line',
            smooth: true,
            showSymbol: false,
            data: item.data
        }))
    }
}

export function buildBarOption(chart: ChartGroup, signed = false): EChartsOption {
    const xAxis = chart.xAxis || chart.series[0]?.data.map((_, index) => String(index + 1)) || []

    return {
        tooltip: {
            trigger: 'axis',
            valueFormatter(value: any) {
                return formatAmount(Number(value))
            }
        },
        legend: {
            top: 0
        },
        grid: {
            left: 64,
            right: 24,
            top: 42,
            bottom: 46
        },
        xAxis: {
            type: 'category',
            data: xAxis,
            axisLabel: { color: '#606266' },
            axisLine: { lineStyle: { color: '#909399' } }
        },
        yAxis: {
            type: 'value',
            axisLabel: {
                color: '#606266',
                formatter(value: number) {
                    return formatAmount(value)
                }
            },
            splitLine: { lineStyle: { color: '#ebeef5' } }
        },
        dataZoom: [
            {
                type: 'inside'
            },
            {
                type: 'slider',
                bottom: 8,
                height: 22
            }
        ],
        series: chart.series.map(item => ({
            name: item.name,
            type: 'bar',
            data: signed
                ? item.data.map(value => ({
                    value,
                    itemStyle: {
                        color: Number(value) >= 0 ? '#ef5350' : '#26a69a'
                    }
                }))
                : item.data
        }))
    }
}

export function buildPieOption(title: string, data: WeightItem[]): EChartsOption {
    return {
        tooltip: {
            trigger: 'item',
            formatter(params: any) {
                return `${params.name}<br/>权重：${formatPercent(params.value)}`
            }
        },
        legend: {
            orient: 'vertical',
            right: 8,
            top: 'middle'
        },
        series: [
            {
                name: title,
                type: 'pie',
                radius: ['42%', '68%'],
                center: ['38%', '50%'],
                avoidLabelOverlap: true,
                label: {
                    formatter: '{b}\n{d}%'
                },
                data: data.map(item => ({
                    name: item.name,
                    value: item.weight
                }))
            }
        ]
    }
}

export function buildIndustryBreadthOption(upCount = 0, downCount = 0, flatCount = 0): EChartsOption {
    return {
        tooltip: {
            trigger: 'item'
        },
        legend: {
            bottom: 0
        },
        series: [
            {
                name: '成分涨跌分布',
                type: 'pie',
                radius: ['40%', '68%'],
                data: [
                    { name: '上涨', value: upCount, itemStyle: { color: '#ef5350' } },
                    { name: '下跌', value: downCount, itemStyle: { color: '#26a69a' } },
                    { name: '平盘', value: flatCount, itemStyle: { color: '#909399' } }
                ]
            }
        ]
    }
}

const moneyFormatter = (value?: number | null): string => {
  if (value === undefined || value === null || Number.isNaN(Number(value))) {
    return '-'
  }

  const absValue = Math.abs(value)

  if (absValue >= 100000000) {
    return `${(value / 100000000).toFixed(2)}亿`
  }

  if (absValue >= 10000) {
    return `${(value / 10000).toFixed(2)}万`
  }

  return `${value.toFixed(2)}`
}

const toHundredMillion = (value?: number): number => {
  if (value === undefined || value === null || Number.isNaN(Number(value))) {
    return 0
  }

  return Number((value / 100000000).toFixed(4))
}

export const buildCapitalStructureBarOption = (capitalFlow?: CapitalFlowInfo): EChartsOption => {
  return {
    title: { text: '今日资金结构', left: 8, top: 0, textStyle: { fontSize: 14, fontWeight: 600 } },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      valueFormatter: (value) => moneyFormatter(Number(value) * 100000000)
    },
    grid: { left: 56, right: 24, top: 56, bottom: 36 },
    xAxis: { type: 'category', data: ['特大单', '大单', '中单', '小单'], axisTick: { alignWithLabel: true } },
    yAxis: { type: 'value', name: '亿', splitLine: { lineStyle: { type: 'dashed' } } },
    series: [
      {
        name: '净流入',
        type: 'bar',
        barMaxWidth: 42,
        data: [
          toHundredMillion(capitalFlow?.superLargeNetInflow),
          toHundredMillion(capitalFlow?.largeNetInflow),
          toHundredMillion(capitalFlow?.mediumNetInflow),
          toHundredMillion(capitalFlow?.smallNetInflow)
        ]
      }
    ]
  }
}

export const buildNorthboundMarginBarOption = (capitalFlow?: CapitalFlowInfo): EChartsOption => {
  return {
    title: { text: '北向与融资融券变化', left: 8, top: 0, textStyle: { fontSize: 14, fontWeight: 600 } },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      valueFormatter: (value) => moneyFormatter(Number(value) * 100000000)
    },
    grid: { left: 56, right: 24, top: 56, bottom: 36 },
    xAxis: { type: 'category', data: ['北向资金变化', '融资融券净买入'] },
    yAxis: { type: 'value', name: '亿', splitLine: { lineStyle: { type: 'dashed' } } },
    series: [
      {
        name: '变化',
        type: 'bar',
        barMaxWidth: 42,
        data: [toHundredMillion(capitalFlow?.northboundChange), toHundredMillion(capitalFlow?.marginNetBuy)]
      }
    ]
  }
}

/**
 * 小单 / 中单 / 大单 / 特大单历史资金流向堆叠柱状图。
 * ECharts legend 默认支持点击显示 / 隐藏对应系列。
 */
export const buildCapitalFlowHistoryStackedBarOption = (historyFlows: CapitalFlowHistoryItem[] = []): EChartsOption => {
  const dates = historyFlows.map((item) => item.tradeDate)

  return {
    title: { text: '历史资金流向', left: 8, top: 0, textStyle: { fontSize: 14, fontWeight: 600 } },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      valueFormatter: (value) => moneyFormatter(Number(value) * 100000000)
    },
    legend: {
      top: 28,
      left: 8,
      selectedMode: true,
      data: ['小单', '中单', '大单', '特大单']
    },
    grid: { left: 56, right: 24, top: 78, bottom: 48 },
    dataZoom: [
      { type: 'inside', start: historyFlows.length > 30 ? 50 : 0, end: 100 },
      { type: 'slider', height: 18, bottom: 12, start: historyFlows.length > 30 ? 50 : 0, end: 100 }
    ],
    xAxis: { type: 'category', data: dates, axisLabel: { hideOverlap: true } },
    yAxis: { type: 'value', name: '亿', splitLine: { lineStyle: { type: 'dashed' } }, axisLabel: { formatter: '{value}' } },
    series: [
      {
        name: '小单',
        type: 'bar',
        stack: 'capitalFlow',
        emphasis: { focus: 'series' },
        data: historyFlows.map((item) => toHundredMillion(item.smallNetInflow))
      },
      {
        name: '中单',
        type: 'bar',
        stack: 'capitalFlow',
        emphasis: { focus: 'series' },
        data: historyFlows.map((item) => toHundredMillion(item.mediumNetInflow))
      },
      {
        name: '大单',
        type: 'bar',
        stack: 'capitalFlow',
        emphasis: { focus: 'series' },
        data: historyFlows.map((item) => toHundredMillion(item.largeNetInflow))
      },
      {
        name: '特大单',
        type: 'bar',
        stack: 'capitalFlow',
        emphasis: { focus: 'series' },
        data: historyFlows.map((item) => toHundredMillion(item.superLargeNetInflow))
      }
    ]
  }
}
