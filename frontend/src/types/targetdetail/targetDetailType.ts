/**
 * 标的详情类型统一出口（兼容旧引用）。
 *
 * 类型已按功能拆分，新代码请直接从对应模块导入：
 * - 页面骨架 / 首屏 / 综合分析：`./targetDetailCenter`
 * - 行情类（行情走势 + 资金流向）：`./quoteDetailType`
 * - 估值类：`./valuationDetailType`
 * - 财务类：`./financialDetailType`
 * - 行业类：`./industryDetailType`
 * - 通用元信息：`./common`
 */
export * from './common'
export * from './targetDetailCenter'
export * from './quoteDetailType'
export * from './valuationDetailType'
export * from './financialDetailType'
export * from './industryDetailType'
