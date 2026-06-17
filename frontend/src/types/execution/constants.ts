/**
 * M-05/M-06 采集执行控制 - 常量与类型定义
 * 与后端枚举值保持一致
 */

/** 执行状态 */
export type RunStatus = 'CREATED' | 'PENDING' | 'RUNNING' | 'SUCCESS' | 'FAILED'

/** 触发方式 */
export type TriggerType = 'MANUAL' | 'SCHEDULED'

/** 调度状态 */
export type ScheduleStatus = 'ACTIVE' | 'PAUSED'

/** 数据集状态 */
export type DatasetStatus = 'GENERATED' | 'AVAILABLE' | 'INVALID' | 'ARCHIVED'

/** 创建执行记录请求体（I-24） */
export interface TaskRunCreateRequest {
    taskConfigCode: string
    taskCode: string
    triggerType?: TriggerType
    cronExpression?: string
    requestId: string
    params?: Record<string, any>
    saveOrUpdate: 'SAVE' | 'UPDATE'
}

/** 查询执行记录列表请求参数（I-26） */
export interface TaskRunQueryParams {
    taskCode?: string
    taskConfigCode?: string
    runStatus?: RunStatus | ''
    triggerType?: TriggerType | ''
    startTime?: string
    endTime?: string
    pageNo?: number
    pageSize?: number
}

/** 调度任务查询参数 */
export interface ExecutionSchedulerQueryParams {
    taskCode?: string
    status?: ScheduleStatus | ''
    pageNo?: number
    pageSize?: number
}
