import type {
    AnalysisHistoryQuery,
    AnalysisRecord,
    ChartGroup,
    PageResult,
    TargetChartQuery,
    TargetDetailDTO,
    TargetDetailQuery,
    TargetType
} from '/@/types/targetdetail/targetDetailCenter'

function sleep(ms = 240): Promise<void> {
    return new Promise(resolve => window.setTimeout(resolve, ms))
}

function clone<T>(data: T): T {
    return JSON.parse(JSON.stringify(data)) as T
}

function nowText(): string {
    const d = new Date()
    const pad = (n: number) => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

const dailyKline = [
    { date: '2026-06-01', open: 9.72, high: 9.88, low: 9.61, close: 9.82, volume: 82345678, amount: 803000000 },
    { date: '2026-06-02', open: 9.83, high: 9.96, low: 9.74, close: 9.90, volume: 74234567, amount: 732000000 },
    { date: '2026-06-03', open: 9.88, high: 10.02, low: 9.80, close: 9.97, volume: 81567890, amount: 812000000 },
    { date: '2026-06-04', open: 9.98, high: 10.08, low: 9.89, close: 10.01, volume: 93456780, amount: 931000000 },
    { date: '2026-06-05', open: 10.02, high: 10.11, low: 9.97, close: 10.06, volume: 99876543, amount: 1001000000 },
    { date: '2026-06-08', open: 10.05, high: 10.20, low: 10.01, close: 10.12, volume: 103456789, amount: 1043000000 },
    { date: '2026-06-09', open: 10.12, high: 10.18, low: 10.00, close: 10.07, volume: 91234567, amount: 920000000 },
    { date: '2026-06-10', open: 10.05, high: 10.16, low: 9.98, close: 10.07, volume: 88567890, amount: 891000000 },
    { date: '2026-06-11', open: 10.05, high: 10.31, low: 10.02, close: 10.25, volume: 123456789, amount: 1256000000 }
]

const MOCK_CHARTS: ChartGroup[] = [
    {
        chartId: 'daily_kline',
        title: '日K走势',
        chartType: 'CANDLESTICK',
        period: 'DAILY',
        series: [
            {
                name: 'K线',
                data: dailyKline
            }
        ]
    },
    {
        chartId: 'amount_trend',
        title: '成交额趋势',
        chartType: 'BAR',
        period: 'DAILY',
        xAxis: dailyKline.map(item => item.date),
        series: [
            {
                name: '成交额',
                unit: '元',
                data: dailyKline.map(item => item.amount || 0)
            }
        ]
    },
    {
        chartId: 'valuation_percentile',
        title: '估值分位走势',
        chartType: 'LINE',
        period: 'DAILY',
        xAxis: dailyKline.map(item => item.date),
        series: [
            {
                name: 'PE分位',
                unit: '%',
                data: [18, 19, 21, 22, 22, 23, 24, 24, 23]
            }
        ]
    }
]

const MOCK_HISTORY: AnalysisRecord[] = [
    {
        analysisId: 'R202606110001',
        analysisTime: '2026-06-11 15:30:00',
        sceneCode: 'MARKET',
        sceneName: '行情分析',
        conclusion: 'BULLISH',
        conclusionText: '偏强',
        signalStrength: 'MEDIUM',
        riskLevel: 'MEDIUM',
        summary: '放量上涨并站上20日均线，短期趋势改善。',
        keyPoints: ['涨跌幅强于行业平均', '成交额明显放大', '短期均线多头排列'],
        sourceDataTime: '2026-06-11 15:00:00',
        modelVersion: 'v1.2.0'
    },
    {
        analysisId: 'R202606050001',
        analysisTime: '2026-06-05 15:25:00',
        sceneCode: 'VALUATION',
        sceneName: '估值分析',
        conclusion: 'BULLISH',
        conclusionText: '偏低估',
        signalStrength: 'MEDIUM',
        riskLevel: 'LOW',
        summary: 'PE和PB均处于近五年偏低位置，估值安全边际较好。',
        keyPoints: ['PE五年分位23%', 'PB五年分位18%', '股息率处于相对较高水平'],
        sourceDataTime: '2026-06-05 15:00:00',
        modelVersion: 'v1.2.0'
    },
    {
        analysisId: 'R202605300001',
        analysisTime: '2026-05-30 20:10:00',
        sceneCode: 'RISK',
        sceneName: '风险预警',
        conclusion: 'NEUTRAL',
        conclusionText: '中性',
        signalStrength: 'WEAK',
        riskLevel: 'MEDIUM',
        summary: '短期涨幅较快，需关注回撤和量能持续性。',
        keyPoints: ['近20日涨幅领先行业', '波动率开始抬升'],
        sourceDataTime: '2026-05-30 15:00:00',
        modelVersion: 'v1.1.8'
    }
]

const MOCK_DETAIL: TargetDetailDTO = {
    target: {
        targetId: 'STOCK_000001_SZ',
        targetCode: '000001.SZ',
        targetName: '平安银行',
        targetType: 'STOCK',
        marketCode: 'CN_A',
        marketName: 'A股',
        exchangeCode: 'SZSE',
        exchangeName: '深圳证券交易所',
        currency: 'CNY',
        listingDate: '1991-04-03',
        industryCode: 'BANK',
        industryName: '银行',
        tags: ['低估值', '高股息', '沪深300'],
        description: '示例数据：用于展示通用标的详情页结构。'
    },
    quote: {
        tradeDate: '2026-06-11',
        tradeTime: '15:00:00',
        lastPrice: 10.25,
        previousClose: 10.07,
        open: 10.05,
        high: 10.31,
        low: 10.02,
        changeAmount: 0.18,
        changePercent: 1.79,
        amplitude: 2.88,
        volume: 123456789,
        amount: 2560000000,
        turnoverRate: 1.24,
        marketStatus: 'CLOSED',
        priceUnit: '元'
    },
    overview: {
        overallView: 'BULLISH',
        overallViewText: '偏强',
        trendStatus: 'UPTREND',
        valuationStatus: 'UNDERVALUED',
        riskLevel: 'MEDIUM',
        confidence: 0.76,
        summary: '短期趋势走强，估值仍处于偏低区域，但近期涨幅较快。',
        reasons: ['价格站上20日均线', '成交额较近20日均值放大', 'PE分位处于近五年低位'],
        risks: ['短期涨幅较快，存在回撤风险', '行业景气度仍需观察'],
        suggestions: ['继续观察成交量持续性', '关注下一期财报利润增速']
    },
    metricCards: [
        {
            key: 'changePercent',
            title: '今日涨跌幅',
            value: 1.79,
            unit: '%',
            displayValue: '+1.79%',
            trend: 'UP',
            status: 'GOOD',
            compareText: '强于行业平均 +0.62%'
        },
        {
            key: 'amount',
            title: '成交额',
            value: 2560000000,
            displayValue: '25.60亿',
            trend: 'UP',
            status: 'NORMAL',
            compareText: '较20日均值放大 1.8 倍'
        },
        {
            key: 'pePercentile',
            title: 'PE五年分位',
            value: 23,
            unit: '%',
            displayValue: '23%',
            status: 'GOOD',
            description: '估值处于历史偏低区域'
        },
        {
            key: 'riskLevel',
            title: '风险等级',
            value: 'MEDIUM',
            displayValue: '中等',
            status: 'WARNING',
            description: '短期涨幅较快'
        },
        {
            key: 'mainNetInflow',
            title: '主力净流入',
            value: 210000000,
            displayValue: '+2.10亿',
            trend: 'UP',
            status: 'GOOD'
        },
        {
            key: 'watchConclusion',
            title: '观察结论',
            value: '继续观察',
            displayValue: '继续观察',
            status: 'NORMAL',
            description: '关注成交额持续性'
        }
    ],
    charts: MOCK_CHARTS,
    performance: [
        { periodCode: '5D', periodName: '近5日', changePercent: 2.31, maxDrawdown: -1.05, volatility: 1.6, rankingText: '前35%' },
        { periodCode: '20D', periodName: '近20日', changePercent: 8.42, maxDrawdown: -3.12, volatility: 2.1, rankingText: '前18%' },
        { periodCode: '60D', periodName: '近60日', changePercent: 15.2, maxDrawdown: -6.8, volatility: 2.8, rankingText: '前12%' },
        { periodCode: 'YTD', periodName: '年初至今', changePercent: 12.65, maxDrawdown: -9.36, volatility: 2.4, rankingText: '前22%' }
    ],
    valuation: {
        peTtm: 8.6,
        pb: 0.72,
        ps: 1.15,
        dividendYield: 5.3,
        marketCap: 230000000000,
        floatMarketCap: 185000000000,
        pePercentile: 23,
        pbPercentile: 18,
        industryPeMedian: 11.2,
        industryPbMedian: 0.91,
        valuationStatus: 'UNDERVALUED',
        conclusion: '估值低于行业中位数，处于近五年偏低区间。'
    },
    financial: {
        reportPeriod: '2026Q1',
        reportType: 'Q1',
        revenue: 38250000000,
        revenueYoY: 6.8,
        netProfit: 12840000000,
        netProfitYoY: 4.2,
        deductedNetProfit: 12620000000,
        deductedNetProfitYoY: 3.9,
        roe: 11.6,
        roa: 0.92,
        debtAssetRatio: 91.2,
        operatingCashFlow: 21580000000,
        eps: 0.66,
        bps: 21.3,
        dividendPerShare: 0.32,
        dividendPayoutRatio: 28.5,
        conclusion: '盈利保持稳定增长，资产负债率符合银行业特征。'
    },
    capitalFlow: {
        tradeDate: '2026-06-11',
        mainNetInflow: 210000000,
        superLargeNetInflow: 85000000,
        largeNetInflow: 125000000,
        mediumNetInflow: -32000000,
        smallNetInflow: -178000000,
        northboundHoldingValue: 9980000000,
        northboundHoldingRatio: 4.32,
        northboundChange: 0.15,
        marginBalance: 8250000000,
        marginNetBuy: 120000000,
        conclusion: '主力资金与融资资金均呈小幅流入。'
    },
    industry: {
        industryCode: 'BANK',
        industryName: '银行',
        level: 1,
        industryRankings: [
            { metricCode: 'pe_ttm', metricName: 'PE估值', rank: 5, total: 42, percentile: 12, displayValue: '行业第5低' },
            { metricCode: 'roe', metricName: 'ROE', rank: 13, total: 42, percentile: 31, displayValue: '行业前31%' },
            { metricCode: 'change_20d', metricName: '近20日涨幅', rank: 8, total: 42, percentile: 19, displayValue: '行业前19%' }
        ],
        conclusion: '行业内部排名靠前，估值相对偏低。'
    },
    constituents: {
        totalCount: 0,
        upCount: 0,
        downCount: 0,
        flatCount: 0,
        topWeights: [],
        topGainers: [],
        topLosers: [],
        industryWeights: []
    },
    watch: {
        watchStatus: 'KEY_WATCH',
        watchStatusText: '重点观察',
        groupNames: ['银行', '高股息'],
        watchReason: '低估值 + 高股息 + 业绩稳定',
        firstWatchTime: '2026-03-12 10:20:00',
        lastUpdatedTime: '2026-06-11 15:31:00'
    },
    events: [
        {
            eventId: 'E001',
            eventType: 'EARNINGS',
            title: '发布2026年一季报',
            eventDate: '2026-04-29',
            importance: 'HIGH',
            source: '交易所公告',
            summary: '营收同比增长6.8%，净利润同比增长4.2%。'
        },
        {
            eventId: 'E002',
            eventType: 'DIVIDEND',
            title: '现金分红预案',
            eventDate: '2026-05-12',
            importance: 'MEDIUM',
            source: '公司公告',
            summary: '拟每股派发现金红利0.32元。'
        }
    ],
    news: [
        {
            newsId: 'N001',
            title: '银行板块成交活跃，低估值方向获得资金关注',
            publishTime: '2026-06-11 14:20:00',
            source: '示例资讯',
            sentiment: 'POSITIVE',
            summary: '银行板块午后成交放大，高股息方向表现较强。'
        },
        {
            newsId: 'N002',
            title: '市场关注后续利润增速与净息差变化',
            publishTime: '2026-06-10 18:05:00',
            source: '示例研报',
            sentiment: 'NEUTRAL',
            summary: '机构认为后续需持续跟踪净息差与资产质量变化。'
        }
    ],
    alerts: [
        {
            alertId: 'A001',
            title: '跌破20日均线',
            alertType: 'TECHNICAL',
            enabled: true,
            conditionText: '收盘价低于 MA20',
            severity: 'MEDIUM'
        },
        {
            alertId: 'A002',
            title: '成交额异常放大',
            alertType: 'VOLUME',
            enabled: true,
            conditionText: '成交额超过近20日均值2倍',
            severity: 'LOW',
            triggerCount: 2,
            lastTriggeredTime: '2026-06-11 14:30:00'
        }
    ],
    analysisHistory: MOCK_HISTORY,
    relatedTargets: [
        {
            targetCode: '600036.SH',
            targetName: '招商银行',
            targetType: 'STOCK',
            relationType: 'SAME_INDUSTRY',
            relationName: '同行业',
            lastPrice: 35.2,
            changePercent: 0.86
        },
        {
            targetCode: '601166.SH',
            targetName: '兴业银行',
            targetType: 'STOCK',
            relationType: 'SAME_INDUSTRY',
            relationName: '同行业',
            lastPrice: 18.26,
            changePercent: 1.05
        },
        {
            targetCode: '399986.SZ',
            targetName: '中证银行',
            targetType: 'INDEX',
            relationType: 'BENCHMARK',
            relationName: '行业指数',
            lastPrice: 6842.2,
            changePercent: 1.16
        }
    ],
    features: {
        quote: true,
        kline: true,
        valuation: true,
        financial: true,
        capitalFlow: true,
        constituents: false,
        fundInfo: false,
        news: true,
        alerts: true,
        analysisHistory: true
    },
    permissions: {
        canWatch: true,
        canCreateAlert: true,
        canViewFinancial: true,
        canViewCapitalFlow: true,
        canViewNews: true,
        canRunAnalysis: true
    },
    meta: {
        dataSource: 'mock-market-data',
        dataVersion: '2026-06-11',
        generatedAt: '2026-06-11 15:31:00',
        partialData: false
    }
}

function buildTargetByQuery(query: TargetDetailQuery): TargetDetailDTO {
    const data = clone(MOCK_DETAIL)
    const targetCode = query.targetCode?.trim() || '000001.SZ'
    const targetType = (query.targetType || data.target.targetType) as TargetType

    data.target.targetCode = targetCode
    data.target.targetType = targetType

    if (targetType === 'INDEX') {
        data.target.targetName = '沪深300'
        data.target.targetId = 'INDEX_000300_SH'
        data.target.marketName = 'A股指数'
        data.target.industryName = undefined
        data.quote = {
            ...data.quote,
            lastPrice: 3915.28,
            previousClose: 3888.4,
            open: 3890.14,
            high: 3926.12,
            low: 3876.3,
            changeAmount: 26.88,
            changePercent: 0.69,
            amount: 398200000000,
            turnoverRate: undefined,
            priceUnit: '点'
        }
        data.features = {
            quote: true,
            kline: true,
            valuation: true,
            financial: false,
            capitalFlow: true,
            constituents: true,
            fundInfo: false,
            news: true,
            alerts: true,
            analysisHistory: true
        }
        data.constituents = {
            totalCount: 300,
            upCount: 212,
            downCount: 74,
            flatCount: 14,
            topWeights: [
                { targetCode: '600519.SH', targetName: '贵州茅台', weight: 5.6, changePercent: 0.72, contribution: 3.2 },
                { targetCode: '300750.SZ', targetName: '宁德时代', weight: 3.4, changePercent: 1.31, contribution: 2.8 },
                { targetCode: '600036.SH', targetName: '招商银行', weight: 2.8, changePercent: 0.86, contribution: 1.4 }
            ],
            topGainers: [
                { targetCode: '300XXX.SZ', targetName: '示例成分A', changePercent: 8.2, contribution: 0.8 },
                { targetCode: '600XXX.SH', targetName: '示例成分B', changePercent: 6.5, contribution: 0.6 }
            ],
            topLosers: [
                { targetCode: '000XXX.SZ', targetName: '示例成分C', changePercent: -4.1, contribution: -0.7 }
            ],
            industryWeights: [
                { name: '金融', weight: 22.4 },
                { name: '消费', weight: 18.6 },
                { name: '科技', weight: 16.8 },
                { name: '医药', weight: 9.2 }
            ]
        }
    }

    data.meta.generatedAt = nowText()
    return data
}

export async function queryTargetDetailCenter(query: TargetDetailQuery): Promise<TargetDetailDTO> {
    await sleep()
    return buildTargetByQuery(query)
}

export async function queryTargetChartData(query: TargetChartQuery): Promise<ChartGroup[]> {
    await sleep(180)
    const charts = clone(MOCK_CHARTS)

    if (query.chartType === 'CAPITAL_FLOW') {
        return [
            {
                chartId: 'capital_flow',
                title: '主力资金净流入',
                chartType: 'BAR',
                period: 'DAILY',
                xAxis: dailyKline.map(item => item.date),
                series: [
                    {
                        name: '主力净流入',
                        unit: '元',
                        data: [30000000, -12000000, 42000000, 62000000, 48000000, 85000000, -18000000, 25000000, 210000000]
                    }
                ]
            }
        ]
    }

    return charts
}

export async function queryTargetAnalysisHistory(query: AnalysisHistoryQuery): Promise<PageResult<AnalysisRecord>> {
    await sleep(200)

    let records = clone(MOCK_HISTORY)

    if (query.sceneCode) {
        records = records.filter(item => item.sceneCode === query.sceneCode)
    }

    if (query.conclusion) {
        records = records.filter(item => item.conclusion === query.conclusion)
    }

    if (query.riskLevel) {
        records = records.filter(item => item.riskLevel === query.riskLevel)
    }

    const start = (query.pageNo - 1) * query.pageSize
    const end = start + query.pageSize

    return {
        records: records.slice(start, end),
        total: records.length,
        pageNo: query.pageNo,
        pageSize: query.pageSize
    }
}

export const capitalFlowHistoryFlowsMock = [
  { tradeDate: '2026-05-21', smallNetInflow: -42000000, mediumNetInflow: -26000000, largeNetInflow: 35000000, superLargeNetInflow: 18000000 },
  { tradeDate: '2026-05-22', smallNetInflow: -38000000, mediumNetInflow: -21000000, largeNetInflow: 42000000, superLargeNetInflow: 25000000 },
  { tradeDate: '2026-05-25', smallNetInflow: 12000000, mediumNetInflow: 18000000, largeNetInflow: 56000000, superLargeNetInflow: 31000000 },
  { tradeDate: '2026-05-26', smallNetInflow: -52000000, mediumNetInflow: -33000000, largeNetInflow: -18000000, superLargeNetInflow: -12000000 },
  { tradeDate: '2026-05-27', smallNetInflow: -25000000, mediumNetInflow: 8000000, largeNetInflow: 36000000, superLargeNetInflow: 22000000 },
  { tradeDate: '2026-05-28', smallNetInflow: -61000000, mediumNetInflow: -42000000, largeNetInflow: 68000000, superLargeNetInflow: 45000000 },
  { tradeDate: '2026-05-29', smallNetInflow: 18000000, mediumNetInflow: 16000000, largeNetInflow: 52000000, superLargeNetInflow: 33000000 },
  { tradeDate: '2026-06-01', smallNetInflow: -46000000, mediumNetInflow: -24000000, largeNetInflow: 22000000, superLargeNetInflow: 11000000 },
  { tradeDate: '2026-06-02', smallNetInflow: -35000000, mediumNetInflow: -12000000, largeNetInflow: 74000000, superLargeNetInflow: 52000000 },
  { tradeDate: '2026-06-03', smallNetInflow: 9000000, mediumNetInflow: 17000000, largeNetInflow: 81000000, superLargeNetInflow: 63000000 },
  { tradeDate: '2026-06-04', smallNetInflow: -72000000, mediumNetInflow: -39000000, largeNetInflow: -26000000, superLargeNetInflow: -18000000 },
  { tradeDate: '2026-06-05', smallNetInflow: -28000000, mediumNetInflow: 6000000, largeNetInflow: 45000000, superLargeNetInflow: 28000000 },
  { tradeDate: '2026-06-08', smallNetInflow: -50000000, mediumNetInflow: -19000000, largeNetInflow: 61000000, superLargeNetInflow: 39000000 },
  { tradeDate: '2026-06-09', smallNetInflow: 15000000, mediumNetInflow: 22000000, largeNetInflow: 92000000, superLargeNetInflow: 77000000 },
  { tradeDate: '2026-06-10', smallNetInflow: -44000000, mediumNetInflow: -28000000, largeNetInflow: 58000000, superLargeNetInflow: 41000000 },
  { tradeDate: '2026-06-11', smallNetInflow: -58000000, mediumNetInflow: -32000000, largeNetInflow: 125000000, superLargeNetInflow: 85000000 }
]
