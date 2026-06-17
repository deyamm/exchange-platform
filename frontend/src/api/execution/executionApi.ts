import http from '/@/utils/http'
import type { PageResult } from '/@/types/common'
import type { TaskRun, TaskRunDetail, ExecutionScheduler } from '/@/types/execution/entity'
import type { TaskRunCreateRequest, TaskRunQueryParams, ExecutionSchedulerQueryParams } from '/@/types/execution/constants'

/**
 * I-24 创建执行记录（生成执行配置快照）
 */
export function createTaskRun(data: TaskRunCreateRequest): Promise<TaskRun> | any {
    return http.post<TaskRun>('/api/v1/collection/task-runs', data)
}

/**
 * I-25 触发任务执行
 */
export function executeTaskRun(runId: string): Promise<TaskRun> | any {
    return http.post<TaskRun>(`/api/v1/collection/task-runs/${runId}/execute`)
}

/**
 * I-26 查询执行记录列表（分页）
 */
export function queryTaskRuns(params: TaskRunQueryParams): Promise<PageResult<TaskRun>> | any {
    return http.get<PageResult<TaskRun>>('/api/v1/collection/task-runs', { params })
}

/**
 * I-27 查询执行记录详情
 */
export function getTaskRunDetail(runId: string): Promise<TaskRunDetail> | any {
    return http.get<TaskRunDetail>(`/api/v1/collection/task-runs/${runId}/detail`)
}

/**
 * I-27 删除执行记录
 */
export function deleteTaskRun(runId: string): Promise<boolean> | any {
    return http.delete<boolean>(`/api/v1/collection/task-runs/${runId}`)
}

/**
 * I-28 查询调度任务列表
 */
export function queryExecutionSchedulers(params: ExecutionSchedulerQueryParams): Promise<PageResult<ExecutionScheduler>> | any {
    return http.get<PageResult<ExecutionScheduler>>('/api/v1/collection/execution-schedulers', { params })
}

/**
 * I-29 暂停调度
 */
export function pauseExecutionScheduler(schedulerId: string): Promise<boolean> | any {
    return http.post<boolean>(`/api/v1/collection/execution-schedulers/${schedulerId}/pause`)
}

/**
 * I-30 恢复调度
 */
export function resumeExecutionScheduler(schedulerId: string): Promise<boolean> | any {
    return http.post<boolean>(`/api/v1/collection/execution-schedulers/${schedulerId}/resume`)
}

/**
 * I-31 删除调度
 */
export function deleteExecutionScheduler(schedulerId: string): Promise<boolean> | any {
    return http.delete<boolean>(`/api/v1/collection/execution-schedulers/${schedulerId}`)
}
