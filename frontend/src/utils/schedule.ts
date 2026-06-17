export type ScheduleContentMode = 'text' | 'cron'

export interface ScheduleContentInput {
    mode: string
    interval: number
    second: number
    minute: number
    hour: number
    monthDay: number
    weekDays: string[]
}

export function buildScheduleContent(input: ScheduleContentInput, needContent: ScheduleContentMode): string {
    const interval = Math.max(1, input.interval || 1)
    const second = clampNumber(input.second || 0, 0, 59)
    const minute = clampNumber(input.minute || 0, 0, 59)
    const hour = clampNumber(input.hour || 0, 0, 23)
    const monthDay = clampNumber(input.monthDay || 1, 1, 31)

    const weekDays = normalizeWeekDays(input.weekDays, needContent)

    const textMap: Record<string, string> = {
        SECOND: `每隔 ${interval} 秒执行一次。`,
        MINUTE: `每隔 ${interval} 分钟，在第 ${second} 秒执行。`,
        HOUR: `每隔 ${interval} 小时，在第 ${minute} 分 ${second} 秒执行。`,
        DAY: `每隔 ${interval} 天，在 ${hour} 时 ${minute} 分 ${second} 秒执行。`,
        WEEK: `每周 ${weekDays}，在 ${hour} 时 ${minute} 分 ${second} 秒执行。`,
        MONTH: `每隔 ${interval} 个月，在第 ${monthDay} 日 ${hour} 时 ${minute} 分 ${second} 秒执行。`,
        WEEKDAY: `每个交易日，在 ${hour} 时 ${minute} 分 ${second} 秒执行。`
    }

    const cronMap: Record<string, string> = {
        SECOND: `*/${interval} * * * * ?`,
        MINUTE: `${second} */${interval} * * * ?`,
        HOUR: `${second} ${minute} */${interval} * * ?`,
        DAY: `${second} ${minute} ${hour} */${interval} * ?`,
        WEEK: `${second} ${minute} ${hour} ? * ${weekDays}`,
        MONTH: `${second} ${minute} ${hour} ${monthDay} */${interval} ?`,
        WEEKDAY: `${second} ${minute} ${hour} ? * MON-FRI`
    }

    const map = needContent === 'text' ? textMap : cronMap
    return map[input.mode] || ''
}

export function getScheduleTextFromCron(cronExpression?: string): string {
    if (!cronExpression) return '-'
    const parts = cronExpression.trim().split(/\s+/)
    if (parts.length < 6) return cronExpression

    const [second, minute, hour, dayOfMonth, month, dayOfWeek] = parts

    const secondEvery = isEvery(second)
    const minuteEvery = isEvery(minute)
    const hourEvery = isEvery(hour)
    const dayEvery = isEvery(dayOfMonth)
    const monthEvery = isEvery(month)

    if (secondEvery && minuteEvery && hourEvery && dayEvery && monthEvery && dayOfWeek === '?') {
        return `每隔 ${parseEvery(second)} 秒执行一次。`
    }

    if (isFixed(second) && minuteEvery && hourEvery && dayEvery && monthEvery && dayOfWeek === '?') {
        return `每隔 ${parseEvery(minute)} 分钟，在第 ${second} 秒执行。`
    }

    if (isFixed(second) && isFixed(minute) && hourEvery && dayEvery && monthEvery && dayOfWeek === '?') {
        return `每隔 ${parseEvery(hour)} 小时，在第 ${minute} 分 ${second} 秒执行。`
    }

    if (isFixed(second) && isFixed(minute) && isFixed(hour) && dayEvery && monthEvery && dayOfWeek === '?') {
        return `每隔 ${parseEvery(dayOfMonth)} 天，在 ${hour} 时 ${minute} 分 ${second} 秒执行。`
    }

    if (isFixed(second) && isFixed(minute) && isFixed(hour) && dayOfMonth === '?' && month === '*' && dayOfWeek) {
        const weekDays = normalizeWeekDays(dayOfWeek.split(','), 'text')
        return `每周 ${weekDays}，在 ${hour} 时 ${minute} 分 ${second} 秒执行。`
    }

    if (isFixed(second) && isFixed(minute) && isFixed(hour) && isFixed(dayOfMonth) && monthEvery && dayOfWeek === '?') {
        return `每隔 ${parseEvery(month)} 个月，在第 ${dayOfMonth} 日 ${hour} 时 ${minute} 分 ${second} 秒执行。`
    }

    if (isFixed(second) && isFixed(minute) && isFixed(hour) && dayOfMonth === '?' && month === '*' && dayOfWeek === 'MON-FRI') {
        return `每个交易日，在 ${hour} 时 ${minute} 分 ${second} 秒执行。`
    }
    return cronExpression
}

function isEvery(value: string): boolean {
    return value === '*' || value.startsWith('*/')
}

function isFixed(value: string): boolean {
    return /^\d+$/.test(value)
}

function parseEvery(value: string): number {
    if (value.startsWith('*/')) {
        const parsed = Number(value.slice(2))
        return Number.isNaN(parsed) ? 1 : parsed
    }
    return 1
}

function normalizeWeekDays(weekDays: string[] | string, needContent: ScheduleContentMode): string {
    const list = Array.isArray(weekDays) ? weekDays : weekDays.split(',')
    const normalized = list.length ? list : ['MON']
    if (needContent === 'text') {
        const map: Record<string, string> = {
            MON: '周一',
            TUE: '周二',
            WED: '周三',
            THU: '周四',
            FRI: '周五',
            SAT: '周六',
            SUN: '周日'
        }
        return normalized.map((day) => map[day] || day).join('、')
    }
    return normalized.join(',')
}

function clampNumber(value: number, min: number, max: number): number {
    if (Number.isNaN(value)) return min
    return Math.min(max, Math.max(min, value))
}
