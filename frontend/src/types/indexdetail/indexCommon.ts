/**
 * 指数详情各功能域共用的响应元信息。
 *
 * 放在独立文件中，避免 indexDetailCenter / indexMarketType / indexConstituentsType
 * 之间为了 ResponseMeta 产生循环引用。
 */
export interface ResponseMeta {
    dataSource: string
    dataVersion?: string
    generatedAt: string
    cacheExpireAt?: string
    partialData?: boolean
    warnings?: string[]
}
