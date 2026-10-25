<template>
    <div class="market-overview-page">
        <!-- 页面标题 -->
        <header class="page-header">
            <div>
                <h1>市场总览</h1>
                <p>
                    交易日期：{{ marketSummary.tradeDate }}
                    <span class="divider">|</span>
                    市场状态：
                    <span :class="['status-tag', marketSummary.marketStatus === '交易中' ? 'active' : 'closed']">
                        {{ marketSummary.marketStatus }}
                    </span>
                    <span class="divider">|</span>
                    数据更新时间：{{ marketSummary.updateTime }}
                    <span class="divider">|</span>
                    统计范围：{{ marketSummary.marketScope }}
                </p>
            </div>

            <div class="header-actions">
                <button class="secondary-button">数据说明</button>
                <button class="primary-button">刷新行情</button>
            </div>
        </header>

        <!-- 一、市场整体情况 -->
        <section class="section-card overview-empty-section">
            <div class="section-title">
                <div>
                    <h2>全市场核心概览</h2>
                </div>
            </div>

            <div class="overview-empty-space" aria-hidden="true"></div>
        </section>

        <!-- 二、大盘指数 2 * 4 -->
        <section class="section-card">
            <div class="section-title">
                <div>
                    <h2>大盘指数</h2>
                    <span>两行四列展示核心宽基指数，覆盖大盘、成长、蓝筹、中小盘风格</span>
                </div>
            </div>

            <div class="major-index-grid">
                <article v-for="item in majorIndexList" :key="item.indexCode" class="index-card">
                    <div class="index-card-header">
                        <div>
                            <strong>{{ item.indexName }}</strong>
                            <span>{{ item.indexCode }} · {{ item.market }}</span>
                        </div>
                        <b :class="getChangeClass(item.changePct)">
                            {{ formatSignedPercent(item.changePct) }}
                        </b>
                    </div>

                    <div class="index-point">
                        {{ formatNumber(item.latestPoint, 2) }}
                    </div>

                    <div class="index-meta">
                        <span :class="getChangeClass(item.changePoint)">
                            {{ formatSignedNumber(item.changePoint, 2) }}
                        </span>
                        <span>成交额 {{ formatAmount(item.amount) }}</span>
                        <span>成分上涨 {{ formatPercent(item.componentRiseRatio) }}</span>
                    </div>
                </article>
            </div>

            <div class="major-index-bottom-summary">
                <article class="summary-card">
                    <span>全市场成交额</span>
                    <strong>{{ formatAmount(marketSummary.totalAmount) }}</strong>
                    <p :class="getChangeClass(marketSummary.amountChangePct)">
                        较昨日 {{ formatSignedPercent(marketSummary.amountChangePct) }}
                    </p>
                </article>

                <article class="summary-card">
                    <span>短线情绪</span>
                    <strong>
                        <b class="positive-text">{{ marketSummary.limitUpCount }}</b>
                        /
                        <b class="negative-text">{{ marketSummary.limitDownCount }}</b>
                    </strong>
                    <p>涨停 / 跌停，衡量短线情绪强弱</p>
                </article>

                <article class="summary-card">
                    <span>市场方向</span>
                    <strong :class="getChangeClass(marketSummary.avgChangePct)">
                        {{ formatSignedPercent(marketSummary.avgChangePct) }}
                    </strong>
                    <p>
                        中位数
                        <b :class="getChangeClass(marketSummary.medianChangePct)">
                            {{ formatSignedPercent(marketSummary.medianChangePct) }}
                        </b>
                    </p>
                </article>
            </div>
        </section>

        <!-- 三、核心行业指数，可上下滑动 -->
        <section class="section-card">
            <div class="section-title">
                <div>
                    <h2>核心行业指数</h2>
                    <span>表格区域支持上下滑动，适合展示更多行业指数</span>
                </div>
                <span>共 {{ coreIndustryIndexList.length }} 个行业</span>
            </div>
            <div class="table-wrapper table-scroll-y">
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>行业指数</th>
                            <th>最新点位</th>
                            <th>涨跌点数</th>
                            <th>涨跌幅</th>
                            <th>成交额</th>
                            <th>成分上涨占比</th>
                            <th>领涨个股</th>
                            <th>近 5 日</th>
                            <th>近 20 日</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr v-for="item in coreIndustryIndexList" :key="item.indexCode">
                            <td>
                                <div class="main-cell">
                                    <strong>{{ item.indexName }}</strong>
                                    <span>{{ item.indexCode }} · {{ item.indexType }}</span>
                                </div>
                            </td>
                            <td>{{ formatNumber(item.latestPoint, 2) }}</td>
                            <td :class="getChangeClass(item.changePoint)">
                                {{ formatSignedNumber(item.changePoint, 2) }}
                            </td>
                            <td :class="getChangeClass(item.changePct)">
                                {{ formatSignedPercent(item.changePct) }}
                            </td>
                            <td>{{ formatAmount(item.amount) }}</td>
                            <td>{{ formatPercent(item.componentRiseRatio) }}</td>
                            <td>
                                <span>{{ item.leadingStockName }}</span>
                                <span :class="['inline-change', getChangeClass(item.leadingStockChangePct || 0)]">
                                    {{ formatSignedPercent(item.leadingStockChangePct || 0) }}
                                </span>
                            </td>
                            <td :class="getChangeClass(item.changePct5d)">
                                {{ formatSignedPercent(item.changePct5d) }}
                            </td>
                            <td :class="getChangeClass(item.changePct20d)">
                                {{ formatSignedPercent(item.changePct20d) }}
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </section>

        <!-- 四、热门个股 -->
        <section class="section-card">
            <div class="section-title">
                <div>
                    <h2>热门个股</h2>
                    <span>表格区域支持上下滑动，结合涨跌幅、成交额、换手率和所属主线综合筛选</span>
                </div>
                <span>展示 {{ hotStockList.length }} 只高关注标的</span>
            </div>

            <div class="table-wrapper table-scroll-y stock-scroll-y">
                <table class="data-table hot-stock-table">
                    <thead>
                        <tr>
                            <th>个股</th>
                            <th>所属行业</th>
                            <th>关联板块</th>
                            <th>最新价</th>
                            <th>涨跌幅</th>
                            <th>成交额</th>
                            <th>换手率</th>
                            <th>资金热度</th>
                            <th>上榜原因</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr v-for="item in hotStockList" :key="item.stockCode">
                            <td>
                                <div class="main-cell">
                                    <strong>{{ item.stockName }}</strong>
                                    <span>{{ item.stockCode }}</span>
                                </div>
                            </td>
                            <td>{{ item.industryName }}</td>
                            <td>
                                <span class="type-tag">{{ item.sectorName }}</span>
                            </td>
                            <td>{{ formatNumber(item.latestPrice, 2) }}</td>
                            <td :class="getChangeClass(item.changePct)">
                                {{ formatSignedPercent(item.changePct) }}
                            </td>
                            <td>{{ formatAmount(item.amount) }}</td>
                            <td>{{ formatPercent(item.turnoverRate) }}</td>
                            <td>
                                <div class="heat-cell">
                                    <div class="heat-track">
                                        <div class="heat-fill" :style="{ width: `${item.hotScore}%` }" />
                                    </div>
                                    <span>{{ item.hotScore }}</span>
                                </div>
                            </td>
                            <td>{{ item.reason }}</td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </section>

        <!-- 五、涨跌分布 Tab -->
        <section class="section-card">
            <div class="section-title">
                <div>
                    <h2>涨跌分布与排行</h2>
                    <span>切换查看个股、板块、概念的涨跌分布，并在 Tab 内展示交易额与主力资金流入排行</span>
                </div>
            </div>

            <div class="tab-list">
                <button
                    v-for="tab in distributionTabs"
                    :key="tab.id"
                    type="button"
                    :class="['tab-button', { active: activeDistributionTab === tab.id }]"
                    @click="activeDistributionTab = tab.id"
                >
                    {{ tab.label }}
                </button>
            </div>
            
            <div class="distribution-tab-content">
                <article class="distribution-panel">
                    <div class="distribution-panel-header">
                        <div>
                            <h3>{{ activeDistribution.label }}涨跌分布</h3>
                            <span>{{ activeDistribution.description }}</span>
                        </div>
                        <strong :class="getChangeClass(activeDistribution.avgChangePct)">
                            {{ formatSignedPercent(activeDistribution.avgChangePct) }}
                        </strong>
                    </div>

                    <div class="distribution-chart">
                        <div
                            v-for="bucket in activeDistribution.buckets"
                            :key="`${activeDistribution.id}-${bucket.rangeLabel}`"
                            class="distribution-row"
                        >
                            <div class="range-label">{{ bucket.rangeLabel }}</div>

                            <div class="bar-track">
                                <div
                                    class="bar-fill"
                                    :class="bucket.type"
                                    :style="{ width: getBucketWidth(activeDistribution.buckets, bucket.count) }"
                                />
                            </div>

                            <div class="bucket-value">
                                <strong>{{ bucket.count }}</strong>
                                <span>{{ formatPercent(bucket.ratio) }}</span>
                            </div>
                        </div>
                    </div>

                    <div class="breadth-stacked-card">
                        <div class="breadth-stacked-meta">
                            <span
                                v-for="item in activeBreadthBars"
                                :key="`${activeDistribution.id}-${item.label}-meta`"
                            >
                                <i :class="item.type" />
                                {{ item.label }}
                                <strong>{{ formatNumber(item.count, 0) }}</strong>
                                <em>{{ formatPercent(item.ratio) }}</em>
                            </span>
                        </div>

                        <div class="breadth-stacked-track">
                            <div
                                v-for="item in activeBreadthBars"
                                :key="`${activeDistribution.id}-${item.label}-bar`"
                                class="breadth-stacked-segment"
                                :class="item.type"
                                :style="{ width: `${item.ratio}%` }"
                                :title="`${item.label} ${formatNumber(item.count, 0)} / ${formatPercent(item.ratio)}`"
                            />
                        </div>
                    </div>
                </article>

                <div class="tab-rank-grid">
                    <article class="rank-panel">
                        <div class="rank-title">
                            <h3>{{ activeDistribution.label }}交易额排行</h3>
                            <span>Top {{ activeDistribution.amountRank.length }}</span>
                        </div>
                        <ol class="rank-list">
                            <li v-for="item in activeDistribution.amountRank" :key="item.name">
                                <span>{{ item.name }}</span>
                                <strong>{{ formatAmount(item.amount ?? 0) }}</strong>
                                <em>{{ item.extra }}</em>
                            </li>
                        </ol>
                    </article>

                    <article class="rank-panel">
                        <div class="rank-title">
                            <h3>{{ activeDistribution.label }}主力资金流入排行</h3>
                            <span>Top {{ activeDistribution.mainFundInflowRank.length }}</span>
                        </div>
                        <ol class="rank-list">
                            <li v-for="item in activeDistribution.mainFundInflowRank" :key="item.name">
                                <span>{{ item.name }}</span>
                                <strong :class="getChangeClass(item.mainFundInflow ?? 0)">
                                    {{ formatSignedAmount(item.mainFundInflow ?? 0) }}
                                </strong>
                                <em>{{ item.extra }}</em>
                            </li>
                        </ol>
                    </article>
                </div>
            </div>
        </section>
    </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { queryMarketOverview } from '/@/api/marketoverview/marketOverviewApi'

type DistributionTabId = 'stock' | 'sector' | 'concept'


interface MarketSummary {
    tradeDate: string
    marketStatus: '交易中' | '已收盘' | '休市' | '数据未更新'
    updateTime: string
    marketScope: string
    limitUpCount: number
    limitDownCount: number
    riseRatio: number
    totalAmount: number
    amountChangePct: number
    avgChangePct: number
    medianChangePct: number
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
    leadingStockName?: string
    leadingStockChangePct?: number
}

interface HotStock {
    stockCode: string
    stockName: string
    industryName: string
    sectorName: string
    latestPrice: number
    changePct: number
    amount: number
    turnoverRate: number
    hotScore: number
    reason: string
}

interface DistributionBucket {
    rangeLabel: string
    count: number
    ratio: number
    type: 'positive' | 'negative' | 'neutral'
}

interface RankItem {
    name: string
    amount?: number
    mainFundInflow?: number
    extra: string
}

interface DistributionTab {
    id: DistributionTabId
    label: string
    description: string
    riseCount: number
    fallCount: number
    flatCount: number
    avgChangePct: number
    buckets: DistributionBucket[]
    amountRank: RankItem[]
    mainFundInflowRank: RankItem[]
}

const marketOverview = ref<any>({})
const marketSummary = computed<MarketSummary>(() => marketOverview.value.marketSummary || {})
const majorIndexList = computed<IndexOverview[]>(() => marketOverview.value.majorIndexList || [])
const coreIndustryIndexList = computed<IndexOverview[]>(() => marketOverview.value.coreIndustryIndexList || [])
const hotStockList = computed<HotStock[]>(() => marketOverview.value.hotStockList || [])
const distributionTabs = computed<DistributionTab[]>(() => marketOverview.value.distributionTabs || [])



async function fetchMarketSummary() {
    try {
        const data = await queryMarketOverview({
            tradeDate: '2026-06-18',
        })
        console.log(data)
        marketOverview.value = data
    } catch (error) {
        console.error('获取市场总览数据失败:', error)
    }
}

const activeDistributionTab = ref<DistributionTabId>('stock')

const activeDistribution = computed<DistributionTab>(() => {
    return (
        distributionTabs.value.find((item) => item.id === activeDistributionTab.value) ??
        distributionTabs.value[0] ??
        {
            id: 'stock',
            label: '个股',
            description: '暂无涨跌分布数据',
            riseCount: 0,
            fallCount: 0,
            flatCount: 0,
            avgChangePct: 0,
            buckets: [],
            amountRank: [],
            mainFundInflowRank: []
        }
    )
})

const activeBreadthBars = computed(() => {
    const current = activeDistribution.value
    const total = current.riseCount + current.flatCount + current.fallCount

    if (total === 0) {
        return [
            { label: '上涨', count: 0, ratio: 0, type: 'positive' as const },
            { label: '平盘', count: 0, ratio: 0, type: 'neutral' as const },
            { label: '下跌', count: 0, ratio: 0, type: 'negative' as const }
        ]
    }

    return [
        { label: '上涨', count: current.riseCount, ratio: (current.riseCount / total) * 100, type: 'positive' as const },
        { label: '平盘', count: current.flatCount, ratio: (current.flatCount / total) * 100, type: 'neutral' as const },
        { label: '下跌', count: current.fallCount, ratio: (current.fallCount / total) * 100, type: 'negative' as const }
    ]
})

function formatNumber(value: number, precision = 2): string {
    if (!value) {
        value = 0
    }
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

function formatSignedAmount(value: number): string {
    const sign = value > 0 ? '+' : value < 0 ? '-' : ''
    return `${sign}${formatAmount(Math.abs(value))}`
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

function getBucketWidth(buckets: DistributionBucket[], count: number): string {
    const maxCount = Math.max(...buckets.map((item) => item.count))

    if (maxCount === 0) {
        return '0%'
    }

    return `${(count / maxCount) * 100}%`
}

onMounted(() => {
    fetchMarketSummary()
})
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
    align-items: flex-start;
    justify-content: space-between;
    gap: 12px;
    margin-bottom: 16px;
}

.section-title h2,
.subsection-title h3,
.rank-title h3,
.distribution-panel-header h3 {
    margin: 0;
}

.section-title h2 {
    font-size: 18px;
    font-weight: 700;
}

.section-title span,
.subsection-title span,
.rank-title span,
.distribution-panel-header span {
    color: #64748b;
    font-size: 13px;
}

.overview-empty-section {
    min-height: 160px;
}

.overview-empty-space {
    min-height: 96px;
}

.section-inline-summary,
.major-index-bottom-summary {
    display: grid;
    gap: 14px;
}

.section-inline-summary {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    margin-bottom: 16px;
}

.major-index-bottom-summary {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    margin-top: 16px;
}

.summary-card {
    padding: 16px;
    border-radius: 14px;
    background: #f8fafc;
    border: 1px solid #edf2f7;
}

.summary-card span {
    display: block;
    color: #64748b;
    font-size: 13px;
    margin-bottom: 8px;
}

.summary-card strong {
    display: block;
    font-size: 20px;
    margin-bottom: 8px;
}

.summary-card p {
    margin: 0;
    color: #64748b;
    font-size: 13px;
    line-height: 1.7;
}

.summary-card p.positive-text {
    color: #dc2626;
}

.summary-card p.negative-text {
    color: #16a34a;
}

.summary-card p.neutral-text {
    color: #64748b;
}

.summary-card b {
    font-weight: 700;
}

.overview-grid {
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
    font-size: 22px;
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
    background: #f0fdf4;
}

.metric-card.neutral {
    background: #f8fafc;
}

.market-pulse-grid {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 14px;
    margin-top: 16px;
}

.pulse-card {
    border-radius: 14px;
    background: #f8fafc;
    padding: 16px;
    border: 1px solid #edf2f7;
}

.pulse-card span {
    display: block;
    color: #64748b;
    font-size: 13px;
    margin-bottom: 8px;
}

.pulse-card strong {
    display: block;
    font-size: 20px;
    margin-bottom: 8px;
}

.pulse-card p {
    margin: 0;
    color: #64748b;
    font-size: 13px;
    line-height: 1.7;
}

.major-index-grid {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 14px;
    margin-bottom: 0;
}

.index-card {
    padding: 16px;
    border-radius: 14px;
    border: 1px solid #edf2f7;
    background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
}

.index-card-header {
    display: flex;
    justify-content: space-between;
    gap: 12px;
    margin-bottom: 14px;
}

.index-card-header strong {
    display: block;
    font-size: 16px;
    margin-bottom: 4px;
}

.index-card-header span,
.index-meta span {
    color: #64748b;
    font-size: 12px;
}

.index-card-header b {
    font-size: 18px;
}

.index-point {
    font-size: 26px;
    font-weight: 700;
    margin-bottom: 12px;
}

.index-meta {
    display: flex;
    flex-wrap: wrap;
    gap: 8px 14px;
}

.subsection-title {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    margin: 4px 0 12px;
}

.subsection-title h3,
.rank-title h3,
.distribution-panel-header h3 {
    font-size: 15px;
}

.table-wrapper {
    overflow-x: auto;
}
.table-scroll-y {
    max-height: 360px;
    overflow-y: auto;
    border: 1px solid #edf2f7;
    border-radius: 12px;
}

.stock-scroll-y {
    max-height: 380px;
}

.table-scroll-y .data-table th {
    position: sticky;
    top: 0;
    z-index: 2;
}


.data-table {
    width: 100%;
    min-width: 980px;
    border-collapse: collapse;
}

.hot-stock-table {
    min-width: 1080px;
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

.type-tag {
    display: inline-flex;
    align-items: center;
    border-radius: 999px;
    border: 1px solid #dbe3ef;
    background: #ffffff;
    color: #334155;
    padding: 3px 8px;
    font-size: 12px;
}

.heat-cell {
    display: flex;
    align-items: center;
    gap: 8px;
    min-width: 120px;
}

.heat-track {
    width: 80px;
    height: 8px;
    border-radius: 999px;
    background: #eef2f7;
    overflow: hidden;
}

.heat-fill {
    height: 100%;
    border-radius: 999px;
    background: #f59e0b;
}

.distribution-group-grid {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 14px;
}

.distribution-panel,
.rank-panel {
    border-radius: 14px;
    border: 1px solid #edf2f7;
    background: #f8fafc;
    padding: 16px;
}

.distribution-panel-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 12px;
    margin-bottom: 12px;
}

.distribution-panel-header strong {
    font-size: 18px;
}

.distribution-chart {
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.breadth-stacked-card {
    margin-top: 24px;
    padding: 12px;
    border-radius: 14px;
    background: #ffffff;
    border: 1px solid #edf2f7;
}

.breadth-stacked-meta {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 10px 18px;
    margin-bottom: 10px;
    color: #64748b;
    font-size: 12px;
}

.breadth-stacked-meta span {
    display: inline-flex;
    align-items: center;
    gap: 5px;
}

.breadth-stacked-meta i {
    width: 8px;
    height: 8px;
    border-radius: 999px;
}

.breadth-stacked-meta i.positive {
    background: #ef4444;
}

.breadth-stacked-meta i.neutral {
    background: #94a3b8;
}

.breadth-stacked-meta i.negative {
    background: #22c55e;
}

.breadth-stacked-meta strong {
    color: #1f2937;
}

.breadth-stacked-meta em {
    color: #94a3b8;
    font-style: normal;
}

.breadth-stacked-track {
    display: flex;
    height: 12px;
    border-radius: 999px;
    background: #eef2f7;
    overflow: hidden;
}

.breadth-stacked-segment {
    height: 100%;
    min-width: 0;
}

.breadth-stacked-segment.positive {
    background: #ef4444;
}

.breadth-stacked-segment.neutral {
    background: #94a3b8;
}

.breadth-stacked-segment.negative {
    background: #22c55e;
}

.distribution-row {
    display: grid;
    grid-template-columns: 76px minmax(0, 1fr) 86px;
    align-items: center;
    gap: 12px;
}

.range-label {
    color: #475569;
    font-size: 12px;
}

.bar-track {
    height: 10px;
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
    background: #22c55e;
}

.bar-fill.neutral {
    background: #94a3b8;
}

.bucket-value {
    display: flex;
    justify-content: space-between;
    gap: 8px;
    font-size: 12px;
}

.bucket-value span {
    color: #94a3b8;
}

.rank-grid {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 14px;
    margin-top: 16px;
}

.rank-title {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 12px;
}

.rank-list {
    display: grid;
    gap: 10px;
    padding-left: 20px;
    margin: 0;
}

.rank-list li {
    display: grid;
    grid-template-columns: minmax(0, 1fr) auto;
    gap: 4px 10px;
    align-items: center;
    color: #334155;
    font-size: 13px;
}

.rank-list strong {
    font-size: 14px;
}

.rank-list em {
    grid-column: 1 / -1;
    color: #94a3b8;
    font-size: 12px;
    font-style: normal;
}


.tab-list {
    display: inline-flex;
    gap: 6px;
    padding: 4px;
    margin-bottom: 16px;
    border: 1px solid #edf2f7;
    border-radius: 999px;
    background: #f8fafc;
}

.tab-button {
    height: 34px;
    min-width: 88px;
    padding: 0 18px;
    border: 0;
    border-radius: 999px;
    background: transparent;
    color: #64748b;
    cursor: pointer;
    font-size: 14px;
}

.tab-button.active {
    background: #2563eb;
    color: #ffffff;
    box-shadow: 0 4px 12px rgba(37, 99, 235, 0.24);
}

.distribution-tab-content {
    display: grid;
    grid-template-columns: minmax(0, 1.2fr) minmax(360px, 0.8fr);
    gap: 14px;
    align-items: stretch;
}

.tab-rank-grid {
    display: grid;
    grid-template-columns: 1fr;
    gap: 14px;
}

.positive-text {
    color: #dc2626;
}

.negative-text {
    color: #16a34a;
}

.neutral-text {
    color: #64748b;
}

@media (max-width: 1400px) {
    .overview-grid {
        grid-template-columns: repeat(2, minmax(0, 1fr));
    }

    .distribution-group-grid,
    .rank-grid,
    .tab-rank-grid {
        grid-template-columns: repeat(2, minmax(0, 1fr));
    }

    .distribution-tab-content {
        grid-template-columns: 1fr;
    }
}

@media (max-width: 1200px) {
    .major-index-grid,
    .major-index-bottom-summary,
    .market-pulse-grid {
        grid-template-columns: repeat(2, minmax(0, 1fr));
    }
}

@media (max-width: 768px) {
    .market-overview-page {
        padding: 16px;
    }

    .page-header,
    .section-title,
    .subsection-title {
        flex-direction: column;
    }

    .overview-grid,
    .major-index-grid,
    .major-index-bottom-summary,
    .section-inline-summary,
    .market-pulse-grid,
    .distribution-group-grid,
    .rank-grid {
        grid-template-columns: 1fr;
    }

    .tab-list {
        display: flex;
        width: 100%;
        border-radius: 12px;
    }

    .tab-button {
        flex: 1;
        min-width: 0;
        padding: 0 10px;
    }

    .distribution-row {
        grid-template-columns: 72px minmax(0, 1fr);
    }

    .bucket-value {
        grid-column: 2;
    }
}
</style>