import type {
    MarketStatus,
    MetricStatus,
    OverallView,
    RiskLevel,
    SignalStrength,
    TargetType,
    ValuationStatus
} from '/@/types/targetdetail/targetDetailCenter'

export function formatDateTime(input?: string | null): string {
    if (!input) return '-'
    return String(input).replace('T', ' ')
}

export function formatNumber(value?: number | null, digits = 2): string {
    if (value === undefined || value === null || Number.isNaN(value)) return '-'
    return Number(value).toLocaleString('zh-CN', {
        minimumFractionDigits: digits,
        maximumFractionDigits: digits
    })
}

export function formatPercent(value?: number | null, digits = 2, showSign = false): string {
    if (value === undefined || value === null || Number.isNaN(value)) return '-'
    const sign = showSign && value > 0 ? '+' : ''
    return `${sign}${formatNumber(value, digits)}%`
}

export function formatAmount(value?: number | null, currency = ''): string {
    if (value === undefined || value === null || Number.isNaN(value)) return '-'

    const abs = Math.abs(value)
    const sign = value < 0 ? '-' : ''

    if (abs >= 1_0000_0000_0000) return `${sign}${formatNumber(abs / 1_0000_0000_0000, 2)}万亿${currency}`
    if (abs >= 1_0000_0000) return `${sign}${formatNumber(abs / 1_0000_0000, 2)}亿${currency}`
    if (abs >= 1_0000) return `${sign}${formatNumber(abs / 1_0000, 2)}万${currency}`

    return `${sign}${formatNumber(abs, 2)}${currency}`
}

export function signedClass(value?: number | null): string {
    if (value === undefined || value === null) return 'is-flat'
    if (value > 0) return 'is-up'
    if (value < 0) return 'is-down'
    return 'is-flat'
}

export function targetTypeText(type?: TargetType | string): string {
    const map: Record<string, string> = {
        STOCK: '股票',
        INDEX: '指数',
        ETF: 'ETF',
        FUND: '基金',
        SECTOR: '行业/板块',
        BOND: '债券',
        FUTURE: '期货',
        FOREX: '外汇',
        CRYPTO: '加密资产'
    }
    return type ? map[String(type)] || String(type) : '-'
}

export function marketStatusText(status?: MarketStatus | string): string {
    const map: Record<string, string> = {
        PRE_MARKET: '盘前',
        TRADING: '交易中',
        MIDDAY_BREAK: '午间休市',
        CLOSED: '已收盘',
        SUSPENDED: '停牌',
        UNKNOWN: '未知'
    }
    return status ? map[String(status)] || String(status) : '-'
}

export function valuationStatusText(status?: ValuationStatus | string): string {
    const map: Record<string, string> = {
        UNDERVALUED: '偏低估',
        FAIR: '合理',
        OVERVALUED: '偏高估',
        UNKNOWN: '未知'
    }
    return status ? map[String(status)] || String(status) : '-'
}

export function overallViewText(view?: OverallView | string): string {
    const map: Record<string, string> = {
        STRONGLY_BULLISH: '强烈偏多',
        BULLISH: '偏多',
        NEUTRAL: '中性',
        BEARISH: '偏空',
        STRONGLY_BEARISH: '强烈偏空'
    }
    return view ? map[String(view)] || String(view) : '-'
}

export function riskLevelText(level?: RiskLevel | string): string {
    const map: Record<string, string> = {
        LOW: '低',
        MEDIUM: '中',
        HIGH: '高',
        EXTREME: '极高'
    }
    return level ? map[String(level)] || String(level) : '-'
}

export function signalStrengthText(strength?: SignalStrength | string): string {
    const map: Record<string, string> = {
        WEAK: '弱',
        MEDIUM: '中',
        STRONG: '强'
    }
    return strength ? map[String(strength)] || String(strength) : '-'
}

export function statusTagType(status?: MetricStatus | RiskLevel | OverallView | string): '' | 'success' | 'warning' | 'danger' | 'info' | 'primary' {
    const value = String(status || '')
    if (['GOOD', 'LOW', 'BULLISH', 'STRONGLY_BULLISH', 'UNDERVALUED'].includes(value)) return 'success'
    if (['WARNING', 'MEDIUM', 'NEUTRAL', 'FAIR'].includes(value)) return 'warning'
    if (['DANGER', 'HIGH', 'EXTREME', 'BEARISH', 'STRONGLY_BEARISH', 'OVERVALUED'].includes(value)) return 'danger'
    if (['NORMAL'].includes(value)) return 'primary'
    return 'info'
}
