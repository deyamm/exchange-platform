<template>
    <div class="market-overview-page">
        <!-- 页面标题 -->
        <header class="page-header">
            <div>
                <h1>市场概览</h1>
                <p>
                    交易日期：{{ marketSummary.tradeDate }}
                    <span class="divider">|</span>
                    市场状态：
                    <span :class="['status-tag', marketSummary.marketStatus === '交易中' ? 'active' : 'closed']">
                        {{ marketSummary.marketStatus }}
                    </span>
                    <span class="divider">|</span>
                    数据更新时间：{{ marketSummary.updateTime }}
                </p>
            </div>

            <div class="header-actions">
                <button class="secondary-button">数据来源</button>
                <button class="primary-button">刷新</button>
            </div>
        </header>

        <!-- 一、市场行情概览 -->
        <section class="section-card">
            <div class="section-title">
                <h2>市场行情概览</h2>
                <span>统计范围：{{ marketSummary.marketScope }}</span>
            </div>

            <div class="summary-grid">
                <div class="metric-card">
                    <span class="metric-label">有效标的数量</span>
                    <strong>{{ marketSummary.validTargetCount }}</strong>
                    <small>纳入统计标的</small>
                </div>

                <div class="metric-card positive">
                    <span class="metric-label">上涨家数</span>
                    <strong>{{ marketSummary.riseCount }}</strong>
                    <small>占比 {{ formatPercent(marketSummary.riseRatio) }}</small>
                </div>

                <div class="metric-card negative">
                    <span class="metric-label">下跌家数</span>
                    <strong>{{ marketSummary.fallCount }}</strong>
                    <small>占比 {{ formatPercent(marketSummary.fallRatio) }}</small>
                </div>

                <div class="metric-card neutral">
                    <span class="metric-label">平盘家数</span>
                    <strong>{{ marketSummary.flatCount }}</strong>
                    <small>涨跌幅为 0</small>
                </div>

                <div class="metric-card positive">
                    <span class="metric-label">涨停家数</span>
                    <strong>{{ marketSummary.limitUpCount }}</strong>
                    <small>达到涨停价</small>
                </div>

                <div class="metric-card negative">
                    <span class="metric-label">跌停家数</span>
                    <strong>{{ marketSummary.limitDownCount }}</strong>
                    <small>达到跌停价</small>
                </div>

                <div class="metric-card">
                    <span class="metric-label">总成交额</span>
                    <strong>{{ formatAmount(marketSummary.totalAmount) }}</strong>
                    <small :class="getChangeClass(marketSummary.amountChangePct)">
                        较昨日 {{ formatSignedPercent(marketSummary.amountChangePct) }}
                    </small>
                </div>

                <div class="metric-card">
                    <span class="metric-label">平均涨跌幅</span>
                    <strong :class="getChangeClass(marketSummary.avgChangePct)">
                        {{ formatSignedPercent(marketSummary.avgChangePct) }}
                    </strong>
                    <small>
                        中位数 {{ formatSignedPercent(marketSummary.medianChangePct) }}
                    </small>
                </div>
            </div>

            <div class="hotspot-grid">
                <div class="hotspot-panel">
                    <h3>热点行业</h3>
                    <div class="tag-list">
                        <button v-for="item in marketSummary.hotIndustries" :key="item" class="hot-tag">
                            {{ item }}
                        </button>
                    </div>
                </div>

                <div class="hotspot-panel">
                    <h3>热点概念</h3>
                    <div class="tag-list">
                        <button v-for="item in marketSummary.hotConcepts" :key="item" class="hot-tag">
                            {{ item }}
                        </button>
                    </div>
                </div>
            </div>
        </section>

        <!-- 二、指数概览 -->
        <section class="section-card">
            <div class="section-title">
                <h2>指数概览</h2>
                <span>核心指数表现</span>
            </div>

            <div class="table-wrapper">
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>指数名称</th>
                            <th>最新点位</th>
                            <th>涨跌点数</th>
                            <th>涨跌幅</th>
                            <th>振幅</th>
                            <th>成交额</th>
                            <th>近 5 日</th>
                            <th>近 20 日</th>
                            <th>成分上涨占比</th>
                            <th>更新时间</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr v-for="item in indexList" :key="item.indexCode">
                            <td>
                                <div class="main-cell">
                                    <strong>{{ item.indexName }}</strong>
                                    <span>{{ item.indexCode }}</span>
                                </div>
                            </td>
                            <td>{{ formatNumber(item.latestPoint, 2) }}</td>
                            <td :class="getChangeClass(item.changePoint)">
                                {{ formatSignedNumber(item.changePoint, 2) }}
                            </td>
                            <td :class="getChangeClass(item.changePct)">
                                {{ formatSignedPercent(item.changePct) }}
                            </td>
                            <td>{{ formatPercent(item.amplitude) }}</td>
                            <td>{{ formatAmount(item.amount) }}</td>
                            <td :class="getChangeClass(item.changePct5d)">
                                {{ formatSignedPercent(item.changePct5d) }}
                            </td>
                            <td :class="getChangeClass(item.changePct20d)">
                                {{ formatSignedPercent(item.changePct20d) }}
                            </td>
                            <td>{{ formatPercent(item.componentRiseRatio) }}</td>
                            <td>{{ item.updateTime }}</td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </section>

        <!-- 三、板块概览 -->
        <section class="section-card">
            <div class="section-title">
                <h2>板块概览</h2>
                <span>行业板块与概念板块表现</span>
            </div>

            <div class="table-wrapper">
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>板块名称</th>
                            <th>类型</th>
                            <th>成分数量</th>
                            <th>涨跌幅</th>
                            <th>成交额</th>
                            <th>成交额占比</th>
                            <th>上涨占比</th>
                            <th>涨停家数</th>
                            <th>领涨标的</th>
                            <th>热度分数</th>
                            <th>排名</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr v-for="item in sectorList" :key="item.sectorCode">
                            <td>
                                <div class="main-cell">
                                    <strong>{{ item.sectorName }}</strong>
                                    <span>{{ item.sectorCode }}</span>
                                </div>
                            </td>
                            <td>
                                <span class="type-tag">{{ item.sectorType }}</span>
                            </td>
                            <td>{{ item.componentCount }}</td>
                            <td :class="getChangeClass(item.changePct)">
                                {{ formatSignedPercent(item.changePct) }}
                            </td>
                            <td>{{ formatAmount(item.amount) }}</td>
                            <td>{{ formatPercent(item.amountRatio) }}</td>
                            <td>{{ formatPercent(item.riseRatio) }}</td>
                            <td>{{ item.limitUpCount }}</td>
                            <td>
                                <span>{{ item.leadingStockName }}</span>
                                <span :class="['inline-change', getChangeClass(item.leadingStockChangePct)]">
                                    {{ formatSignedPercent(item.leadingStockChangePct) }}
                                </span>
                            </td>
                            <td>{{ item.hotScore }}</td>
                            <td>No.{{ item.rank }}</td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </section>

        <!-- 四、涨跌分布概览 -->
        <section class="section-card">
            <div class="section-title">
                <h2>涨跌分布概览</h2>
                <span>
                    上涨 {{ distributionSummary.riseCount }} /
                    下跌 {{ distributionSummary.fallCount }} /
                    平盘 {{ distributionSummary.flatCount }} /
                    涨跌比 {{ riseFallRatio }}
                </span>
            </div>

            <div class="distribution-layout">
                <div class="distribution-chart">
                    <div v-for="bucket in distributionBuckets" :key="bucket.rangeLabel" class="distribution-row">
                        <div class="range-label">{{ bucket.rangeLabel }}</div>

                        <div class="bar-track">
                            <div class="bar-fill" :class="bucket.type"
                                :style="{ width: getBucketWidth(bucket.count) }" />
                        </div>

                        <div class="bucket-value">
                            <strong>{{ bucket.count }}</strong>
                            <span>{{ formatPercent(bucket.ratio) }}</span>
                        </div>
                    </div>
                </div>

                <aside class="distribution-side">
                    <div class="side-item">
                        <span>有效标的</span>
                        <strong>{{ distributionSummary.validTargetCount }}</strong>
                    </div>
                    <div class="side-item positive">
                        <span>最大涨幅</span>
                        <strong>{{ formatSignedPercent(distributionSummary.maxRisePct) }}</strong>
                    </div>
                    <div class="side-item negative">
                        <span>最大跌幅</span>
                        <strong>{{ formatSignedPercent(distributionSummary.maxFallPct) }}</strong>
                    </div>
                    <div class="side-item">
                        <span>平均涨跌幅</span>
                        <strong :class="getChangeClass(distributionSummary.avgChangePct)">
                            {{ formatSignedPercent(distributionSummary.avgChangePct) }}
                        </strong>
                    </div>
                    <div class="side-item">
                        <span>涨跌幅中位数</span>
                        <strong :class="getChangeClass(distributionSummary.medianChangePct)">
                            {{ formatSignedPercent(distributionSummary.medianChangePct) }}
                        </strong>
                    </div>
                </aside>
            </div>
        </section>
    </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface MarketSummary {
    tradeDate: string
    marketStatus: '交易中' | '已收盘' | '休市' | '数据未更新'
    updateTime: string
    marketScope: string
    validTargetCount: number
    riseCount: number
    fallCount: number
    flatCount: number
    limitUpCount: number
    limitDownCount: number
    riseRatio: number
    fallRatio: number
    totalAmount: number
    totalVolume: number
    amountChangePct: number
    avgChangePct: number
    medianChangePct: number
    hotIndustries: string[]
    hotConcepts: string[]
}

interface IndexOverview {
    indexCode: string
    indexName: string
    indexType: string
    market: string
    latestPoint: number
    preClosePoint: number
    changePoint: number
    changePct: number
    amplitude: number
    amount: number
    changePct5d: number
    changePct20d: number
    componentRiseRatio: number
    updateTime: string
}

interface SectorOverview {
    sectorCode: string
    sectorName: string
    sectorType: '行业' | '概念'
    componentCount: number
    changePct: number
    amount: number
    amountRatio: number
    riseRatio: number
    limitUpCount: number
    leadingStockName: string
    leadingStockChangePct: number
    hotScore: number
    rank: number
}

interface DistributionBucket {
    rangeLabel: string
    count: number
    ratio: number
    type: 'positive' | 'negative' | 'neutral'
}

interface DistributionSummary {
    validTargetCount: number
    riseCount: number
    fallCount: number
    flatCount: number
    maxRisePct: number
    maxFallPct: number
    avgChangePct: number
    medianChangePct: number
}

const marketSummary: MarketSummary = {
    tradeDate: '2026-06-07',
    marketStatus: '已收盘',
    updateTime: '15:30:00',
    marketScope: 'A 股全市场',
    validTargetCount: 5326,
    riseCount: 3280,
    fallCount: 1786,
    flatCount: 260,
    limitUpCount: 86,
    limitDownCount: 14,
    riseRatio: 61.58,
    fallRatio: 33.53,
    totalAmount: 982356000000,
    totalVolume: 73560000000,
    amountChangePct: 8.42,
    avgChangePct: 0.86,
    medianChangePct: 0.54,
    hotIndustries: ['半导体', '证券', '电力设备', '汽车零部件', '通信设备'],
    hotConcepts: ['人工智能', '机器人', '算力', '低空经济', '存储芯片']
}

const indexList: IndexOverview[] = [
    {
        indexCode: '000001.SH',
        indexName: '上证指数',
        indexType: '宽基指数',
        market: '上交所',
        latestPoint: 3286.42,
        preClosePoint: 3258.73,
        changePoint: 27.69,
        changePct: 0.85,
        amplitude: 1.26,
        amount: 426800000000,
        changePct5d: 1.92,
        changePct20d: 3.64,
        componentRiseRatio: 64.35,
        updateTime: '15:30:00'
    },
    {
        indexCode: '399001.SZ',
        indexName: '深证成指',
        indexType: '宽基指数',
        market: '深交所',
        latestPoint: 10582.17,
        preClosePoint: 10441.29,
        changePoint: 140.88,
        changePct: 1.35,
        amplitude: 1.72,
        amount: 555600000000,
        changePct5d: 2.84,
        changePct20d: 4.12,
        componentRiseRatio: 67.48,
        updateTime: '15:30:00'
    },
    {
        indexCode: '399006.SZ',
        indexName: '创业板指',
        indexType: '宽基指数',
        market: '深交所',
        latestPoint: 2189.58,
        preClosePoint: 2144.95,
        changePoint: 44.63,
        changePct: 2.08,
        amplitude: 2.34,
        amount: 228900000000,
        changePct5d: 4.35,
        changePct20d: 6.18,
        componentRiseRatio: 71.52,
        updateTime: '15:30:00'
    }
]

const sectorList: SectorOverview[] = [
    {
        sectorCode: 'IND_SEMI',
        sectorName: '半导体',
        sectorType: '行业',
        componentCount: 186,
        changePct: 4.26,
        amount: 98200000000,
        amountRatio: 10.0,
        riseRatio: 82.31,
        limitUpCount: 12,
        leadingStockName: '示例科技',
        leadingStockChangePct: 20.0,
        hotScore: 92,
        rank: 1
    },
    {
        sectorCode: 'IND_BROKER',
        sectorName: '证券',
        sectorType: '行业',
        componentCount: 51,
        changePct: 3.18,
        amount: 65400000000,
        amountRatio: 6.66,
        riseRatio: 76.47,
        limitUpCount: 4,
        leadingStockName: '示例证券',
        leadingStockChangePct: 10.02,
        hotScore: 86,
        rank: 2
    },
    {
        sectorCode: 'CONCEPT_AI',
        sectorName: '人工智能',
        sectorType: '概念',
        componentCount: 238,
        changePct: 3.84,
        amount: 112300000000,
        amountRatio: 11.43,
        riseRatio: 79.41,
        limitUpCount: 15,
        leadingStockName: '示例智能',
        leadingStockChangePct: 19.98,
        hotScore: 95,
        rank: 1
    }
]

const distributionSummary: DistributionSummary = {
    validTargetCount: 5326,
    riseCount: 3280,
    fallCount: 1786,
    flatCount: 260,
    maxRisePct: 20.0,
    maxFallPct: -10.01,
    avgChangePct: 0.86,
    medianChangePct: 0.54
}

const distributionBuckets: DistributionBucket[] = [
    { rangeLabel: '涨停', count: 86, ratio: 1.61, type: 'positive' },
    { rangeLabel: '≥ 7%', count: 168, ratio: 3.15, type: 'positive' },
    { rangeLabel: '5% ~ 7%', count: 286, ratio: 5.37, type: 'positive' },
    { rangeLabel: '3% ~ 5%', count: 612, ratio: 11.49, type: 'positive' },
    { rangeLabel: '1% ~ 3%', count: 1248, ratio: 23.43, type: 'positive' },
    { rangeLabel: '0% ~ 1%', count: 880, ratio: 16.52, type: 'positive' },
    { rangeLabel: '0%', count: 260, ratio: 4.88, type: 'neutral' },
    { rangeLabel: '-1% ~ 0%', count: 760, ratio: 14.27, type: 'negative' },
    { rangeLabel: '-3% ~ -1%', count: 586, ratio: 11.0, type: 'negative' },
    { rangeLabel: '-5% ~ -3%', count: 288, ratio: 5.41, type: 'negative' },
    { rangeLabel: '≤ -5%', count: 138, ratio: 2.59, type: 'negative' },
    { rangeLabel: '跌停', count: 14, ratio: 0.26, type: 'negative' }
]

const maxBucketCount = computed(() => {
    return Math.max(...distributionBuckets.map((item) => item.count))
})

const riseFallRatio = computed(() => {
    if (distributionSummary.fallCount === 0) {
        return '--'
    }

    return (distributionSummary.riseCount / distributionSummary.fallCount).toFixed(2)
})

function formatNumber(value: number, precision = 2): string {
    return value.toLocaleString('zh-CN', {
        minimumFractionDigits: precision,
        maximumFractionDigits: precision
    })
}

function formatSignedNumber(value: number, precision = 2): string {
    const sign = value > 0 ? '+' : ''
    return `${sign}${formatNumber(value, precision)}`
}

function formatPercent(value: number): string {
    return `${formatNumber(value, 2)}%`
}

function formatSignedPercent(value: number): string {
    const sign = value > 0 ? '+' : ''
    return `${sign}${formatNumber(value, 2)}%`
}

function formatAmount(value: number): string {
    if (value >= 100000000) {
        return `${formatNumber(value / 100000000, 2)} 亿`
    }

    if (value >= 10000) {
        return `${formatNumber(value / 10000, 2)} 万`
    }

    return formatNumber(value, 2)
}

function getChangeClass(value: number): string {
    if (value > 0) {
        return 'positive-text'
    }

    if (value < 0) {
        return 'negative-text'
    }

    return 'neutral-text'
}

function getBucketWidth(count: number): string {
    if (maxBucketCount.value === 0) {
        return '0%'
    }

    return `${(count / maxBucketCount.value) * 100}%`
}
</script>

<style scoped>
.market-overview-page {
    min-height: 100vh;
    padding: 24px;
    background: #f5f7fb;
    color: #1f2937;
    box-sizing: border-box;
}

.page-header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 16px;
    margin-bottom: 20px;
}

.page-header h1 {
    margin: 0 0 8px;
    font-size: 28px;
    font-weight: 700;
}

.page-header p {
    margin: 0;
    color: #64748b;
    font-size: 14px;
}

.divider {
    margin: 0 8px;
    color: #cbd5e1;
}

.header-actions {
    display: flex;
    gap: 10px;
}

.primary-button,
.secondary-button {
    height: 36px;
    padding: 0 16px;
    border-radius: 8px;
    border: 1px solid transparent;
    cursor: pointer;
    font-size: 14px;
}

.primary-button {
    background: #2563eb;
    color: #ffffff;
}

.secondary-button {
    background: #ffffff;
    color: #334155;
    border-color: #dbe3ef;
}

.status-tag {
    display: inline-flex;
    align-items: center;
    padding: 2px 8px;
    border-radius: 999px;
    font-size: 12px;
}

.status-tag.active {
    color: #047857;
    background: #d1fae5;
}

.status-tag.closed {
    color: #475569;
    background: #e2e8f0;
}

.section-card {
    background: #ffffff;
    border: 1px solid #e5edf6;
    border-radius: 16px;
    padding: 20px;
    margin-bottom: 20px;
    box-shadow: 0 8px 24px rgba(15, 23, 42, 0.04);
}

.section-title {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    margin-bottom: 16px;
}

.section-title h2 {
    margin: 0;
    font-size: 18px;
    font-weight: 700;
}

.section-title span {
    color: #64748b;
    font-size: 13px;
}

.summary-grid {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 14px;
}

.metric-card {
    padding: 16px;
    border-radius: 14px;
    background: #f8fafc;
    border: 1px solid #edf2f7;
}

.metric-card .metric-label {
    display: block;
    margin-bottom: 10px;
    color: #64748b;
    font-size: 13px;
}

.metric-card strong {
    display: block;
    margin-bottom: 8px;
    font-size: 24px;
    line-height: 1;
}

.metric-card small {
    font-size: 12px;
    color: #94a3b8;
}

.metric-card.positive {
    background: #fff7f7;
}

.metric-card.negative {
    background: #f7fbff;
}

.metric-card.neutral {
    background: #f8fafc;
}

.hotspot-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 14px;
    margin-top: 16px;
}

.hotspot-panel {
    border-radius: 14px;
    background: #f8fafc;
    padding: 16px;
    border: 1px solid #edf2f7;
}

.hotspot-panel h3 {
    margin: 0 0 12px;
    font-size: 15px;
}

.tag-list {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
}

.hot-tag,
.type-tag {
    display: inline-flex;
    align-items: center;
    border-radius: 999px;
    border: 1px solid #dbe3ef;
    background: #ffffff;
    color: #334155;
}

.hot-tag {
    height: 28px;
    padding: 0 10px;
    cursor: pointer;
}

.type-tag {
    padding: 3px 8px;
    font-size: 12px;
}

.table-wrapper {
    overflow-x: auto;
}

.data-table {
    width: 100%;
    min-width: 980px;
    border-collapse: collapse;
}

.data-table th,
.data-table td {
    padding: 12px 10px;
    border-bottom: 1px solid #edf2f7;
    text-align: left;
    font-size: 13px;
    white-space: nowrap;
}

.data-table th {
    color: #64748b;
    background: #f8fafc;
    font-weight: 600;
}

.data-table tbody tr:hover {
    background: #f8fafc;
}

.main-cell {
    display: flex;
    flex-direction: column;
    gap: 4px;
}

.main-cell strong {
    color: #1f2937;
}

.main-cell span {
    color: #94a3b8;
    font-size: 12px;
}

.inline-change {
    margin-left: 8px;
}

.distribution-layout {
    display: grid;
    grid-template-columns: minmax(0, 1fr) 240px;
    gap: 20px;
}

.distribution-chart {
    display: flex;
    flex-direction: column;
    gap: 10px;
}

.distribution-row {
    display: grid;
    grid-template-columns: 90px minmax(0, 1fr) 96px;
    align-items: center;
    gap: 12px;
}

.range-label {
    color: #475569;
    font-size: 13px;
}

.bar-track {
    height: 12px;
    border-radius: 999px;
    background: #eef2f7;
    overflow: hidden;
}

.bar-fill {
    height: 100%;
    border-radius: 999px;
}

.bar-fill.positive {
    background: #ef4444;
}

.bar-fill.negative {
    background: #3b82f6;
}

.bar-fill.neutral {
    background: #94a3b8;
}

.bucket-value {
    display: flex;
    justify-content: space-between;
    gap: 8px;
    font-size: 13px;
}

.bucket-value span {
    color: #94a3b8;
}

.distribution-side {
    display: grid;
    gap: 10px;
}

.side-item {
    padding: 14px;
    border-radius: 12px;
    background: #f8fafc;
    border: 1px solid #edf2f7;
}

.side-item span {
    display: block;
    margin-bottom: 8px;
    color: #64748b;
    font-size: 12px;
}

.side-item strong {
    font-size: 20px;
}

.positive-text {
    color: #dc2626;
}

.negative-text {
    color: #2563eb;
}

.neutral-text {
    color: #64748b;
}

@media (max-width: 1200px) {
    .summary-grid {
        grid-template-columns: repeat(2, minmax(0, 1fr));
    }

    .distribution-layout {
        grid-template-columns: 1fr;
    }
}

@media (max-width: 768px) {
    .market-overview-page {
        padding: 16px;
    }

    .page-header {
        flex-direction: column;
    }

    .summary-grid,
    .hotspot-grid {
        grid-template-columns: 1fr;
    }

    .distribution-row {
        grid-template-columns: 80px minmax(0, 1fr);
    }

    .bucket-value {
        grid-column: 2;
    }
}
</style>
