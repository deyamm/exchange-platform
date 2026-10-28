import type { TargetDetailMeta } from './common'

/* -------------------------------------------------------------------------- */
/*                                   财务类                                    */
/*               财务摘要、三大报表、财务指标、财务图表衍生行                     */
/* -------------------------------------------------------------------------- */

/** 财务摘要。 */
export interface FinancialSummary {
    /** 报告期。 */
    endDate?: string | null
    /** 营业收入。 */
    revenue?: number | null
    /** 营收同比。 */
    revenueYoy?: number | null
    /** 归母净利润。 */
    nIncomeAttrP?: number | null
    /** 净利润同比。 */
    netprofitYoy?: number | null
    /** 扣非净利润。 */
    profitDedt?: number | null
    /** 扣非净利润同比。 */
    dtNetprofitYoy?: number | null
    /** ROE。 */
    roe?: number | null
    /** ROA。 */
    roa?: number | null
    /** 资产负债率。 */
    debtToAssets?: number | null
    /** 经营现金流。 */
    nCashflowAct?: number | null
    /** 自由现金流。 */
    freeCashflow?: number | null
    /** EPS。 */
    eps?: number | null
    /** BPS。 */
    bps?: number | null
    /** 每股股利。 */
    dividendPerShare?: number | null
    /** 分红率。 */
    dividendPayoutRatio?: number | null
    /** 最新财报公告日期，来自 annDate，优先使用利润表或财务指标表最新报告期。 */
    annDate?: string | null
    /** 最新财报实际公告日期，来自 fAnnDate。 */
    fAnnDate?: string | null
    /** 最新财报原始报告类型。 */
    reportType?: string | null
    /** 最新财报类型展示文本，例如一季报、半年报、三季报、年报。 */
    reportTypeText?: string | null
    /** 结论。 */
    conclusion?: string | null
}

/**
 * 利润表明细行。
 *
 * 字段来源：type_finance_data.income_data。
 */
export interface IncomeStatementRow {
    /** TS 股票代码。 */
    fullSymbol: string
    /** 公告日期。 */
    annDate?: string | null
    /** 实际公告日期。 */
    fAnnDate?: string | null
    /** 报告期。 */
    endDate: string
    /** 报告类型。 */
    reportType?: string | null
    /** 公司类型。 */
    compType?: string | null
    /** 基本每股收益。 */
    basicEps?: number | null
    /** 稀释每股收益。 */
    dilutedEps?: number | null
    /** 营业总收入。 */
    totalRevenue?: number | null
    /** 营业收入。 */
    revenue?: number | null
    /** 营业总成本。 */
    totalCogs?: number | null
    /** 营业成本。 */
    operCost?: number | null
    /** 销售费用。 */
    sellExp?: number | null
    /** 管理费用。 */
    adminExp?: number | null
    /** 财务费用。 */
    finExp?: number | null
    /** 研发费用。 */
    rdExp?: number | null
    /** 投资收益。 */
    investIncome?: number | null
    /** 营业利润。 */
    operProfit?: number | null
    /** 利润总额。 */
    totalProfit?: number | null
    /** 所得税费用。 */
    incomeTax?: number | null
    /** 净利润。 */
    nIncome?: number | null
    /** 归属于母公司所有者的净利润。 */
    nIncomeAttrP?: number | null
    /** 息税前利润。 */
    ebit?: number | null
    /** 息税折旧摊销前利润。 */
    ebitda?: number | null
    /** 更新标识。 */
    updateFlag?: string | null
}

/**
 * 资产负债表明细行。
 *
 * 字段来源：type_finance_data.balance_sheet_data。
 */
export interface BalanceSheetRow {
    /** TS 股票代码。 */
    fullSymbol: string
    /** 公告日期。 */
    annDate?: string | null
    /** 实际公告日期。 */
    fAnnDate?: string | null
    /** 报告期。 */
    endDate: string
    /** 报告类型。 */
    reportType?: string | null
    /** 公司类型。 */
    compType?: string | null
    /** 期末总股本。 */
    totalShare?: number | null
    /** 货币资金。 */
    moneyCap?: number | null
    /** 交易性金融资产。 */
    tradAsset?: number | null
    /** 应收票据。 */
    notesReceiv?: number | null
    /** 应收账款。 */
    accountsReceiv?: number | null
    /** 其他应收款。 */
    othReceiv?: number | null
    /** 预付款项。 */
    prepayment?: number | null
    /** 存货。 */
    inventories?: number | null
    /** 流动资产合计。 */
    totalCurAssets?: number | null
    /** 长期股权投资。 */
    ltEqtInvest?: number | null
    /** 投资性房地产。 */
    investRealEstate?: number | null
    /** 固定资产。 */
    fixAssets?: number | null
    /** 在建工程。 */
    cip?: number | null
    /** 无形资产。 */
    intanAssets?: number | null
    /** 商誉。 */
    goodwill?: number | null
    /** 递延所得税资产。 */
    deferTaxAssets?: number | null
    /** 非流动资产合计。 */
    totalNca?: number | null
    /** 资产总计。 */
    totalAssets?: number | null
    /** 短期借款。 */
    stBorr?: number | null
    /** 应付票据。 */
    notesPayable?: number | null
    /** 应付账款。 */
    acctPayable?: number | null
    /** 预收款项。 */
    advReceipts?: number | null
    /** 应付职工薪酬。 */
    payrollPayable?: number | null
    /** 应交税费。 */
    taxesPayable?: number | null
    /** 其他应付款。 */
    othPayable?: number | null
    /** 一年内到期的非流动负债。 */
    nonCurLiabDue1y?: number | null
    /** 流动负债合计。 */
    totalCurLiab?: number | null
    /** 长期借款。 */
    ltBorr?: number | null
    /** 应付债券。 */
    bondPayable?: number | null
    /** 长期应付款。 */
    ltPayable?: number | null
    /** 递延所得税负债。 */
    deferTaxLiab?: number | null
    /** 非流动负债合计。 */
    totalNcl?: number | null
    /** 负债合计。 */
    totalLiab?: number | null
    /** 库存股。 */
    treasuryShare?: number | null
    /** 资本公积。 */
    capRese?: number | null
    /** 盈余公积。 */
    surplusRese?: number | null
    /** 未分配利润。 */
    undistrPorfit?: number | null
    /** 少数股东权益。 */
    minorityInt?: number | null
    /** 归属于母公司股东权益合计。 */
    totalHldrEqyExcMinInt?: number | null
    /** 股东权益合计。 */
    totalHldrEqyIncMinInt?: number | null
    /** 负债和股东权益总计。 */
    totalLiabHldrEqy?: number | null
    /** 更新标识。 */
    updateFlag?: string | null
}

/**
 * 现金流量表明细行。
 *
 * 字段来源：type_finance_data.cashflow_data。
 */
export interface CashflowStatementRow {
    /** TS 股票代码。 */
    fullSymbol: string
    /** 公告日期。 */
    annDate?: string | null
    /** 实际公告日期。 */
    fAnnDate?: string | null
    /** 报告期。 */
    endDate: string
    /** 报告类型。 */
    reportType?: string | null
    /** 公司类型。 */
    compType?: string | null
    /** 净利润。 */
    netProfit?: number | null
    /** 财务费用。 */
    finanExp?: number | null
    /** 销售商品、提供劳务收到的现金。 */
    cFrSaleSg?: number | null
    /** 经营活动现金流入小计。 */
    cInfFrOperateA?: number | null
    /** 购买商品、接受劳务支付的现金。 */
    cPaidGoodsS?: number | null
    /** 支付给职工以及为职工支付的现金。 */
    cPaidToForEmpl?: number | null
    /** 支付的各项税费。 */
    cPaidForTaxes?: number | null
    /** 经营活动现金流出小计。 */
    stCashOutAct?: number | null
    /** 经营活动产生的现金流量净额。 */
    nCashflowAct?: number | null
    /** 投资活动现金流入小计。 */
    stotInflowsInvAct?: number | null
    /** 投资活动现金流出小计。 */
    stotOutInvAct?: number | null
    /** 投资活动产生的现金流量净额。 */
    nCashflowInvAct?: number | null
    /** 筹资活动现金流入小计。 */
    stotCashInFncAct?: number | null
    /** 筹资活动现金流出小计。 */
    stotCashoutFncAct?: number | null
    /** 筹资活动产生的现金流量净额。 */
    nCashFlowsFncAct?: number | null
    /** 汇率变动对现金的影响。 */
    effFxFluCash?: number | null
    /** 现金及现金等价物净增加额。 */
    nIncrCashCashEqu?: number | null
    /** 期初现金及现金等价物余额。 */
    cCashEquBegPeriod?: number | null
    /** 期末现金及现金等价物余额。 */
    cCashEquEndPeriod?: number | null
    /** 企业自由现金流量。 */
    freeCashflow?: number | null
    /** 间接法经营活动现金流量净额。 */
    imNetCashflowOperAct?: number | null
    /** 更新标识。 */
    updateFlag?: string | null
}

/**
 * 财务指标明细行。
 *
 * 字段来源：type_finance_data.finance_indicator_data。
 */
export interface FinanceIndicatorRow {
    /** TS 股票代码。 */
    fullSymbol: string
    /** 公告日期。 */
    annDate?: string | null
    /** 报告期。 */
    endDate: string
    /** 基本每股收益。 */
    eps?: number | null
    /** 稀释每股收益。 */
    dtEps?: number | null
    /** 每股净资产。 */
    bps?: number | null
    /** 每股经营活动现金流量净额。 */
    ocfps?: number | null
    /** 每股现金流量净额。 */
    cfps?: number | null
    /** 每股息税前利润。 */
    ebitPs?: number | null
    /** 每股企业自由现金流量。 */
    fcffPs?: number | null
    /** 每股股东自由现金流量。 */
    fcfePs?: number | null
    /** 净资产收益率。 */
    roe?: number | null
    /** 加权平均净资产收益率。 */
    roeWaa?: number | null
    /** 扣非净资产收益率。 */
    roeDt?: number | null
    /** 总资产报酬率。 */
    roa?: number | null
    /** 投入资本回报率。 */
    roic?: number | null
    /** 毛利率。 */
    grossMargin?: number | null
    /** 净利率。 */
    netprofitMargin?: number | null
    /** 销售毛利率。 */
    grossprofitMargin?: number | null
    /** 净利润/营业总收入。 */
    profitToGr?: number | null
    /** 经营活动利润/利润总额。 */
    opToEbt?: number | null
    /** 流动比率。 */
    currentRatio?: number | null
    /** 速动比率。 */
    quickRatio?: number | null
    /** 现金比率。 */
    cashRatio?: number | null
    /** 资产负债率。 */
    debtToAssets?: number | null
    /** 权益乘数。 */
    assetsToEqt?: number | null
    /** 产权比率。 */
    debtToEqt?: number | null
    /** 息税前利润/利息费用。 */
    ebitToInterest?: number | null
    /** 应收账款周转率。 */
    arTurn?: number | null
    /** 存货周转率。 */
    invTurn?: number | null
    /** 流动资产周转率。 */
    caTurn?: number | null
    /** 固定资产周转率。 */
    faTurn?: number | null
    /** 总资产周转率。 */
    assetsTurn?: number | null
    /** 应收账款周转天数。 */
    arturnDays?: number | null
    /** 存货周转天数。 */
    invturnDays?: number | null
    /** 营业周期。 */
    turnDays?: number | null
    /** 每股营业收入。 */
    revenuePs?: number | null
    /** 每股营业总收入。 */
    totalRevenuePs?: number | null
    /** 每股资本公积。 */
    capitalResePs?: number | null
    /** 每股盈余公积。 */
    surplusResePs?: number | null
    /** 每股未分配利润。 */
    undistProfitPs?: number | null
    /** 扣除非经常性损益后的净利润。 */
    profitDedt?: number | null
    /** 非经常性损益。 */
    extraItem?: number | null
    /** 息税前利润。 */
    ebit?: number | null
    /** 息税折旧摊销前利润。 */
    ebitda?: number | null
    /** 企业自由现金流量。 */
    fcff?: number | null
    /** 股东自由现金流量。 */
    fcfe?: number | null
    /** 营运资金。 */
    workingCapital?: number | null
    /** 营业利润同比增长率。 */
    opYoy?: number | null
    /** 利润总额同比增长率。 */
    ebtYoy?: number | null
    /** 净利润同比增长率。 */
    netprofitYoy?: number | null
    /** 扣非净利润同比增长率。 */
    dtNetprofitYoy?: number | null
    /** 营业收入同比增长率。 */
    revenueYoy?: number | null
    /** 总资产同比增长率。 */
    assetsYoy?: number | null
    /** 净资产同比增长率。 */
    equityYoy?: number | null
    /** 更新标识。 */
    updateFlag?: string | null
}

/** 财报类型编码。 */
export type FinancialReportTypeCode = 'Q1' | 'HALF_YEAR' | 'Q3' | 'ANNUAL'

/** 利润趋势 tab 指标编码。 */
export type FinancialProfitMetricCode = 'TOTAL_REVENUE' | 'N_INCOME_ATTR_P' | 'PROFIT_DEDT' | 'EPS'

/** 利润趋势柱状图展示模式。 */
export type FinancialProfitDisplayMode = 'ALL' | 'SINGLE_QUARTER'

/** 盈利、成长与负债率 tab 指标编码。 */
export type FinancialRatioMetricCode = 'ROE' | 'GROSS_MARGIN' | 'NETPROFIT_MARGIN' | 'DEBT_TO_ASSETS'

/** 财务季度编码。 */
export type FinancialQuarterCode = 'Q1' | 'Q2' | 'Q3' | 'Q4'

/**
 * 最新财报信息。
 *
 * 用于财务详情页首行展示最新财报发布日期和财报类型。
 */
export interface FinancialReportInfo {
    /** 股票代码全称。 */
    fullSymbol?: string | null
    /** 报告期。 */
    endDate?: string | null
    /** 公告日期。 */
    annDate?: string | null
    /** 实际公告日期。 */
    fAnnDate?: string | null
    /** 原始报告类型。 */
    reportType?: string | null
    /** 财报类型编码。 */
    reportTypeCode?: FinancialReportTypeCode | null
    /** 财报类型展示文本，例如一季报、半年报、三季报、年报。 */
    reportTypeText?: string | null
}

/**
 * 利润趋势基础行。
 *
 * 用于支持营业总收入、归母净利润、扣非净利润、每股收益四个 tab。
 * 数值可以是累计口径，也可以由后端提前加工成页面展示口径；页面会根据 endDate 和 quarterCode 做兼容计算。
 */
export interface FinancialProfitTrendRow {
    /** 股票代码全称。 */
    fullSymbol: string
    /** 报告期。 */
    endDate: string
    /** 公告日期。 */
    annDate?: string | null
    /** 年份。 */
    year?: number | null
    /** 季度编码。 */
    quarterCode?: FinancialQuarterCode | null
    /** 财报类型展示文本。 */
    reportTypeText?: string | null
    /** 营业总收入。 */
    totalRevenue?: number | null
    /** 营业总收入同比增长率。 */
    totalRevenueYoy?: number | null
    /** 归母净利润。 */
    nIncomeAttrP?: number | null
    /** 归母净利润同比增长率。 */
    nIncomeAttrPYoy?: number | null
    /** 扣非净利润。 */
    profitDedt?: number | null
    /** 扣非净利润同比增长率。 */
    profitDedtYoy?: number | null
    /** 每股收益。 */
    eps?: number | null
    /** 每股收益同比增长率。 */
    epsYoy?: number | null
}

/**
 * 盈利、成长与负债率趋势行。
 *
 * 用于 ROE、毛利率、净利率、资产负债率四个 tab 的柱状图。
 */
export interface FinancialRatioTrendRow {
    /** 股票代码全称。 */
    fullSymbol: string
    /** 报告期。 */
    endDate: string
    /** 财报类型展示文本。 */
    reportTypeText?: string | null
    /** ROE。 */
    roe?: number | null
    /** 毛利率。 */
    grossMargin?: number | null
    /** 净利率。 */
    netprofitMargin?: number | null
    /** 资产负债率。 */
    debtToAssets?: number | null
}

/**
 * 现金流结构行。
 *
 * 用于经营现金流、投资现金流、筹资现金流、自由现金流四项分组柱状图。
 */
export interface FinancialCashflowStructureRow {
    /** 股票代码全称。 */
    fullSymbol: string
    /** 报告期。 */
    endDate: string
    /** 年份。 */
    year?: number | null
    /** 季度编码。 */
    quarterCode?: FinancialQuarterCode | null
    /** 经营现金流。 */
    nCashflowAct?: number | null
    /** 投资现金流。 */
    nCashflowInvAct?: number | null
    /** 筹资现金流。 */
    nCashFlowsFncAct?: number | null
    /** 自由现金流。 */
    freeCashflow?: number | null
}

/**
 * 资产负债结构行。
 *
 * 用于资产、负债、归母权益三组柱状图，以及资产负债率折线图。
 */
export interface FinancialBalanceStructureRow {
    /** 股票代码全称。 */
    fullSymbol: string
    /** 报告期。 */
    endDate: string
    /** 流动资产。 */
    totalCurAssets?: number | null
    /** 非流动资产。 */
    totalNca?: number | null
    /** 总资产。 */
    totalAssets?: number | null
    /** 流动负债。 */
    totalCurLiab?: number | null
    /** 非流动负债。 */
    totalNcl?: number | null
    /** 总负债。 */
    totalLiab?: number | null
    /** 归母权益。 */
    totalHldrEqyExcMinInt?: number | null
    /** 资产负债率。 */
    debtToAssets?: number | null
}

/**
 * 财务详情页使用的四张财务表明细集合。
 */
export interface FinancialStatementDetail {
    /** 利润表明细。 */
    incomeRows?: IncomeStatementRow[]
    /** 资产负债表明细。 */
    balanceSheetRows?: BalanceSheetRow[]
    /** 现金流量表明细。 */
    cashflowRows?: CashflowStatementRow[]
    /** 财务指标明细。 */
    indicatorRows?: FinanceIndicatorRow[]
    /** 最新财报信息；不传时页面会由四表最新报告期自动推导。 */
    reportInfo?: FinancialReportInfo
    /** 利润趋势图表数据；不传时页面会由 incomeRows 与 indicatorRows 自动推导。 */
    profitTrendRows?: FinancialProfitTrendRow[]
    /** 盈利、成长与负债率图表数据；不传时页面会由 indicatorRows 自动推导。 */
    ratioTrendRows?: FinancialRatioTrendRow[]
    /** 现金流结构图表数据；不传时页面会由 cashflowRows 自动推导。 */
    cashflowStructureRows?: FinancialCashflowStructureRow[]
    /** 资产负债结构图表数据；不传时页面会由 balanceSheetRows 与 indicatorRows 自动推导。 */
    balanceStructureRows?: FinancialBalanceStructureRow[]
}

/** 财务 Tab 接口查询参数。 */
export interface TargetFinancialDetailQuery {
    /** 完整代码，包含交易所后缀。 */
    fullSymbol: string
    /** 向前展示的年数；不传时由后端使用默认值。 */
    historyLimit?: number | null
}

/** 财务 Tab 接口返回。 */
export interface TargetFinancialDetail {
    fullSymbol?: string
    /** 财务摘要。 */
    financial?: FinancialSummary | null
    /** 财务四表详情。 */
    financialDetail?: FinancialStatementDetail
    /** 数据元信息。 */
    meta: TargetDetailMeta
}
