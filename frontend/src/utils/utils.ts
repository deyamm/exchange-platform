import { ParamField } from "../types/collectiontask/view";
import type { SelectOption } from "/@/types/common";
import type { DataTopic } from "/@/types/datatopic/entity";

/**
 * 将后端返回的数据主题树转换成 SelectOption 数组，适用于下拉选择组件,label 显示为 "  "（根据层级） + dataTopicName + "(" + dataTopicCode + ")", value 为 dataTopicCode。
 * 如果 disabledCode 不为 null，则对应 dataTopicCode 的选项会被禁用,如果为 null，则所有选项都不禁用。
 * @param tree 
 * @param disabledCode 
 * @returns 
 */
export function buildTopicSelectOptions(tree: DataTopic[] = [], disabledCode: string | null = null): SelectOption[] {
    const result: SelectOption[] = [];

    function walk(nodes: DataTopic[], level = 0): void {
        nodes.forEach(node => {
            result.push({
                label: `${'  '.repeat(level)}${node.dataTopicName}(${node.dataTopicCode})`,
                value: node.dataTopicCode,
                disabled: Boolean(disabledCode && node.dataTopicCode === disabledCode),
            });
            if (node.children && node.children.length > 0) {
                walk(node.children, level + 1);
            }
        });
    }
    walk(tree);
    return result;
}

export function parseParamFields(json?: string): ParamField[] {
    if (!json) return []
    try {
        const fields = JSON.parse(json)
        if (!Array.isArray(fields)) return []
        return fields
            .map((item) => ({
                fieldCode: item.fieldCode || '',
                fieldName: item.fieldName || '',
                fieldType: item.fieldType || '',
                required: Boolean(item.required) || false,
                uniqueKey: Boolean(item.uniqueKey) || false,
                fieldDesc: item.fieldDesc || ''
            }))
            .filter((item) => item.fieldCode)
    } catch {
        return []
    }
}

export function generateRequestId(): string {
    const prefix = 'REQ'
    const random = Math.random().toString(36).replace(/[^a-z0-9]+/gi, '').toUpperCase()
    return `${prefix}-${Date.now()}-${random.slice(0, 10)}`
}

export function resolveTokenValue(dataSource: string, triggerType: 'SCHEDULED' | 'MANUAL', token: 'today' | 'tomorrow' | 'fireDate' | 'now' | 'fireTime'): string {
    // 获取采集任务的数据源，默认为default，供token解析使用
    if (triggerType === 'SCHEDULED') {
        return `{${dataSource}:${token}}`
    }
    // Manual creation: return concrete formatted value according to datasource
    const now = new Date()
    const baseDate = token === 'tomorrow' ? addDays(now, 1) : now

    // now 和 fileTime 都是datetime类型，不区分数据源
    if (token === 'now' || token === 'fireTime') {
        return formatDateTime(now)
    }

    // today/tomorrow 需要区分数据源，部分数据源（如tushare）使用不同的日期格式
    return formatDateByDataSource(dataSource, baseDate)
}

function formatDateByDataSource(dataSource: string, date: Date): string {
    // 当前仅tushare数据源使用yyyyMMdd格式，其他数据源（包括默认和akshare）使用yyyy-MM-dd格式
    if (String(dataSource).toLowerCase() === 'tushare') {
        const year = date.getFullYear()
        const month = String(date.getMonth() + 1).padStart(2, '0')
        const day = String(date.getDate()).padStart(2, '0')
        return `${year}${month}${day}`
    }
    // default and akshare: use yyyy-MM-dd
    return formatDate(date)
}

function addDays(date: Date, days: number): Date {
    const next = new Date(date)
    next.setDate(next.getDate() + days)
    return next
}

function formatDate(date: Date): string {
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
}

function formatDateTime(date: Date): string {
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const hours = String(date.getHours()).padStart(2, '0')
    const minutes = String(date.getMinutes()).padStart(2, '0')
    const seconds = String(date.getSeconds()).padStart(2, '0')
    return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}