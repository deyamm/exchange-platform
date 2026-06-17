import type { RunStatus, TriggerType, DatasetStatus, ScheduleStatus } from './constants'

// ── 状态文本映射 ──────────────────────────────────────────────

export const RUN_STATUS_TEXT: Record<RunStatus, string> = {
    CREATED: '已创建',
    PENDING: '待执行',
    RUNNING: '执行中',
    SUCCESS: '执行成功',
    FAILED:  '执行失败'
}

export const RUN_STATUS_TAG_TYPE: Record<RunStatus, string> = {
    CREATED: 'info',
    PENDING: 'warning',
    RUNNING: 'primary',
    SUCCESS: 'success',
    FAILED:  'danger'
}

export const TRIGGER_TYPE_TEXT: Record<TriggerType, string> = {
    MANUAL:    '手动触发',
    SCHEDULED: '定时触发'
}

export const SCHEDULE_STATUS_TEXT: Record<ScheduleStatus, string> = {
    ACTIVE: '启用',
    PAUSED: '暂停'
}

export const SCHEDULE_STATUS_TAG_TYPE: Record<ScheduleStatus, string> = {
    ACTIVE: 'success',
    PAUSED: 'info'
}

export const DATASET_STATUS_TEXT: Record<DatasetStatus, string> = {
    GENERATED: '已生成',
    AVAILABLE: '可查询',
    INVALID:   '无效',
    ARCHIVED:  '已归档'
}

export const DATASET_STATUS_TAG_TYPE: Record<DatasetStatus, string> = {
    GENERATED: 'info',
    AVAILABLE: 'success',
    INVALID:   'danger',
    ARCHIVED:  'warning'
}

export const WEEKDAY_MAP: Record<string, string> = {
    MON: '周一',
    TUE: '周二',
    WED: '周三',
    THU: '周四',
    FRI: '周五',
    SAT: '周六',
    SUN: '周日'
}
