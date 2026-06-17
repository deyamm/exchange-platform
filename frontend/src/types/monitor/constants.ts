/**
 * M-07/M-08/M-09 查询与追溯 - 查询参数类型
 */

/** 运行日志查询参数（I-31） */
export interface RunLogQueryParams {
    runId?: string
    taskCode?: string
    logLevel?: string
    startTime?: string
    endTime?: string
    pageNo?: number
    pageSize?: number
}

/** 异常记录查询参数（I-32） */
export interface ExceptionQueryParams {
    runId?: string
    taskCode?: string
    // exceptionType?: string
    startTime?: string
    endTime?: string
    pageNo?: number
    pageSize?: number
}

/** 问题数据查询参数（I-33） */
export interface ProblemQueryParams {
    runId?: string
    taskCode?: string
    // problemType?: string
    startTime?: string
    endTime?: string
    pageNo?: number
    pageSize?: number
}
