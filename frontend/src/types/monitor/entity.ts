/** 运行日志 */
export interface RunLog {
    logId?: string
    runId: string
    taskCode?: string
    logLevel?: string
    logContent?: string
    traceId?: string
    createdAt?: string
}

/** 异常记录 */
export interface ExceptionRecord {
    exceptionId: string
    runId: string
    taskCode?: string
    exceptionType?: string
    exceptionMessage?: string
    exceptionContextJson?: string
    createdAt?: string
}

/** 问题数据 */
export interface ProblemRecord {
    problemId: string
    runId: string
    taskCode?: string
    problemType?: string
    problemMessage?: string
    sampleDataJson?: string
    createdAt?: string
}

/** 历史追溯（聚合对象，I-33） */
export interface Trace {
    taskRun: import('../execution/entity').TaskRun
    snapshot?: import('../execution/entity').ExecutionConfigSnapshot
    templateVersion?: Record<string, any>
    resultSummary?: import('../execution/entity').ResultSummary
    runLogs?: RunLog[]
    exceptionRecords?: ExceptionRecord[]
    problemRecords?: ProblemRecord[]
}
