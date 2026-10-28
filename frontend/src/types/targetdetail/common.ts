/**
 * 标的详情各分接口共用的数据元信息。
 *
 * 该类型不依赖任何业务模块，供 targetDetailCenter、quoteDetailType、
 * valuationDetailType、financialDetailType、industryDetailType 共同引用。
 */
export interface TargetDetailMeta {
    /** 数据来源。 */
    dataSource?: string | null
    /** 数据版本。 */
    dataVersion?: string | null
    /** 生成时间。 */
    generatedAt?: string | null
    /** 是否为部分数据。 */
    partialData?: boolean
    /** 数据缺失或降级提示。 */
    warnings?: string[]
}
