import http from '/@/utils/http'
import type { PageResult } from '/@/types/common'
import type { Dataset, DatasetDetail, ResultSummary } from '/@/types/execution/entity'
import type { TaskRunQueryParams } from '/@/types/execution/constants'
import type { RunLog, ExceptionRecord, ProblemRecord, Trace } from '/@/types/monitor/entity'
import type { RunLogQueryParams, ExceptionQueryParams, ProblemQueryParams } from '/@/types/monitor/constants'

/**
 * I-28 查询数据集列表（分页）
 */
export function queryDatasets(params: TaskRunQueryParams): Promise<PageResult<Dataset>> | any {
    return http.get<PageResult<Dataset>>('/api/v1/collection/monitor/datasets', { params })
}

/**
 * I-29 查询数据集详情
 */
export function getDatasetDetail(datasetCode: string): Promise<DatasetDetail> | any {
    return http.get<DatasetDetail>(`/api/v1/collection/monitor/datasets/${datasetCode}`)
}

/**
 * I-30 查询结果概览/明细
 */
export function queryResults(params: TaskRunQueryParams): Promise<ResultSummary[]> | any {
    return http.get<ResultSummary[]>('/api/v1/collection/monitor/results', { params })
}

/**
 * I-31 查询运行日志（分页）
 */
export function queryRunLogs(params: RunLogQueryParams): Promise<PageResult<RunLog>> | any {
    return http.get<PageResult<RunLog>>('/api/v1/collection/monitor/run-logs', { params })
}

/**
 * I-32 查询异常记录（分页）
 */
export function queryExceptionRecords(params: ExceptionQueryParams): Promise<PageResult<ExceptionRecord>> | any {
    return http.get<PageResult<ExceptionRecord>>('/api/v1/collection/monitor/exception-records', { params })
}

/**
 * I-33 查询问题数据（分页）
 */
export function queryProblemRecords(params: ProblemQueryParams): Promise<PageResult<ProblemRecord>> | any {
    return http.get<PageResult<ProblemRecord>>('/api/v1/collection/monitor/problem-records', { params })
}

/**
 * I-34 查询历史追溯信息
 */
export function getTrace(runId: string): Promise<Trace> | any {
    return http.get<Trace>(`/api/v1/collection/monitor/task-runs/${runId}/trace`)
}
