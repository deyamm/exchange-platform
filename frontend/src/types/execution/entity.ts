import type { RunStatus, TriggerType, DatasetStatus, ScheduleStatus } from './constants'

/** 执行记录（列表视图） */
export interface TaskRun {
    runId: string
    taskConfigCode?: string
    taskCode?: string
    taskTemplateVersionNo?: number
    dataTopicCode?: string
    dataTypeCode?: string
    dataTypeVersionNo?: number
    datasetCode?: string
    triggerType?: TriggerType
    schedulerId?: string
    runStatus: RunStatus
    startTime?: string
    scheduledFireTime?: string
    endTime?: string
    runInfo?: string
    createdAt?: string
}

/** 调度配置 */
export interface ExecutionScheduler {
    schedulerId: string
    taskConfigCode?: string
    taskCode?: string
    cronExpression: string
    timeZone?: string
    status: ScheduleStatus
    nextFireTime?: string
    lastFireTime?: string
    lastRunId?: string
    requestId?: string
    createdAt?: string
    updatedAt?: string
}

/** 执行配置快照 */
export interface ExecutionConfigSnapshot {
    snapshotId: string
    runId: string
    taskConfigCode?: string
    taskCode?: string
    taskTemplateVersionNo?: number
    dataTopicCode?: string
    dataTypeCode?: string
    dataTypeVersionNo?: number
    handlerName: string
    snapshotContent?: string
    createdAt?: string
}

/** 结果摘要 */
export interface ResultSummary {
    summaryId: string
    runId: string
    totalCount?: number
    successCount?: number
    failureCount?: number
    resultLocation?: string
    summaryContent?: string
    createdAt?: string
}

/** 数据集 */
export interface Dataset {
    datasetCode: string
    datasetName?: string
    runId?: string
    summaryId?: string
    taskCode?: string
    dataTopicCode?: string
    dataTypeCode?: string
    dataTypeVersionNo?: number
    storageMappingCode?: string
    resultLocation?: string
    datasetStatus: DatasetStatus
    generatedTime?: string
    description?: string
    createdAt?: string
}

/** 执行记录详情（聚合对象） */
export interface TaskRunDetail {
    taskRun: TaskRun
    snapshot?: ExecutionConfigSnapshot
    resultSummary?: ResultSummary
}

/** 数据集详情（聚合对象） */
export interface DatasetDetail {
    dataset: Dataset
    taskRun?: TaskRun
    resultSummary?: ResultSummary
}
