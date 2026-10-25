# TargetDetail 拆分接口 Python 实现文档

> 本文档基于本次上传的 `targetDetailType.ts`、`constants.ts` 与 `physical_storage_tables.md` 生成。  
> Python 实现建议使用 Pydantic / dataclass 对齐 TypeScript 类型，接口返回字段名保持 TypeScript 的 camelCase。

## 0. 通用约定

### 0.1 期限参数

行情、资金、财务等使用 `RangeCode`：

```ts
RangeCode = '1M' | '3M' | '6M' | '1Y' | '2Y' | 'CUSTOM'
```

固定期限映射来自 `constants.ts`：

```ts
rangeSizeMap = {
  '1M': 22,
  '3M': 66,
  '6M': 132,
  '1Y': 250,
  '2Y': 500
}
```

估值使用 `ValuationTimeRangeCode`：

```ts
ValuationTimeRangeCode = '1Y' | '3Y' | '5Y' | '10Y'
valuationRangeSizeMap = {
  '1Y': 250,
  '3Y': 750,
  '5Y': 1250,
  '10Y': 2500
}
```

请求侧传 `historyLimit`，Python 服务端查询指定 `historyLimit` + 额外250条，避免计算指标缺少数据。  
`CUSTOM` 当前仅保留点位，不展开自定义日期查询。

### 0.2 通用返回元信息

```yaml
TargetDetailMeta:
  dataSource: string | null
  dataVersion: string | null
  generatedAt: string | null
  partialData: boolean | null
```

### 0.3 通用 Python 处理约定

- 数据库字段使用 snake_case，接口返回统一转换为 camelCase。
- 涉及行业相关内容，统一使用申万行业三级分类。其中 `type_basic_data.index_basic_data` 包含申万行业指数的基本信息，`type_basic_data.index_constituent_data` 包含申万三级行业指数的成分股信息，`type_basic_data.index_industry_level` 包含申万行业指数的层级关系。在进行同业对比时，统一使用第三级行业分类。
- 对于前端传入的 `tradeDate`，需要调用 `resolve_trade_date` 来获取有效交易日，该函数可以同时处理None日期。

---

# 1. 接口清单

| Python 函数名 | 建议路径 | 返回类型 | 对应页面 / 区域 |
|---|---|---|---|
| `get_target_detail_shell` | `GET /api/market/targets/{fullSymbol}/detail/shell` | `TargetDetailShell` | 详情页顶部 |
| `get_target_overview_detail` | `GET /api/market/targets/{fullSymbol}/detail/overview` | `TargetOverviewDetail` | 综合分析结论 |
| `get_target_quote_detail` | `GET /api/market/targets/{fullSymbol}/detail/quote` | `TargetQuoteDetail` | `QuoteDetailPage.vue` |
| `get_target_valuation_summary_detail` | `GET /api/market/targets/{fullSymbol}/detail/valuation/summary` | `TargetValuationSummaryDetail` | `ValuationDetailPage.vue` |
| `get_target_valuation_channel_detail` | `GET /api/market/targets/{fullSymbol}/detail/valuation/channel` | `TargetValuationChannelDetail` | `ValuationDetailPage.vue` |
| `get_target_valuation_trend_detail` | `GET /api/market/targets/{fullSymbol}/detail/valuation/trend` | `TargetValuationTrendDetail` | `ValuationDetailPage.vue` |
| `get_target_dividend_trend_detail` | `GET /api/market/targets/{fullSymbol}/detail/valuation/dividend` | `TargetDividendTrendDetail` | `ValuationDetailPage.vue` |
| `get_target_financial_detail` | `GET /api/market/targets/{fullSymbol}/detail/financial` | `TargetFinancialDetail` | `FinancialDetailPage.vue` |
| `get_target_capital_flow_detail` | `GET /api/market/targets/{fullSymbol}/detail/capital-flow` | `TargetCapitalFlowDetail` | `CapitalFlowDetailPage.vue` |
| `get_target_industry_detail` | `GET /api/market/targets/{fullSymbol}/detail/industry` | `TargetIndustryDetail` | `ConstituentIndustryDetailPage.vue` |

---

# 2. 接口明细

## 2.1 `get_target_detail_shell`

### 功能描述

返回详情页顶部首屏轻量数据，包括标的基础信息、最新行情、顶部指标卡、估值摘要、财务摘要、资金流摘要、行业摘要与可用 Tab。

### 对应页面

详情页顶部区域，不对应单独 Tab。

### 请求参数

```yaml
fullSymbol: string
tradeDate: string | null
```

### 返回参数：`TargetDetailShell`

```yaml
TargetDetailShell:
  targetProfile:
    fullSymbol: string
    symbol: string | null
    name: string
    targetType: TargetType
    marketCode: string | null
    marketTypeName: string | null
    exchangeName: string | null
    industryName: string | null
    currencyType: string | null
    tags: string[]
  quote:
    fullSymbol: string
    tradeDate: string
    tradeTime: string | null
    marketStatus: MarketStatus | null
    open: number
    high: number
    low: number
    close: number
    preClose: number
    chgAmt: number | null
    chgPct: number | null
    amplitude: number | null
    volume: number | null
    turnover: number | null
    turnoverRate: number | null
    adjustFactor: number | null
    priceUnit: string | null
  metricCards:
    - key: string
      title: string
      value: number | string | null
      displayValue: string | null
      unit: string | null
      trend: MetricTrend | null
      compareText: string | null
      description: string | null
      status: MetricStatus | null
      sortOrder: number | null
  valuation:
    pe: number | null
    peTtm: number | null
    pb: number | null
    ps: number | null
    psTtm: number | null
    dvTtm: number | null
    totalMarketValue: number | null
  financial:
    endDate: string | null
    revenue: number | null
    revenueYoy: number | null
    nIncomeAttrP: number | null
    netprofitYoy: number | null
    profitDedt: number | null
    dtNetprofitYoy: number | null
    roe: number | null
    roa: number | null
    debtToAssets: number | null
    nCashflowAct: number | null
    freeCashflow: number | null
    eps: number | null
    bps: number | null
    dividendPerShare: number | null
    dividendPayoutRatio: number | null
    annDate: string | null
    fAnnDate: string | null
    reportType: string | null
    reportTypeText: string | null
    conclusion: string | null
  capitalFlow:
    tradeDate: string | null
    netMfAmount: number | null
    elgNetMfAmount: number | null
    lgNetMfAmount: number | null
    mdNetMfAmount: number | null
    smNetMfAmount: number | null
    conclusion: string | null
  industry:
    industryCode: string | null
    industryName: string | null
    industryLevel: 1 | 2 | 3 | null
    stockCount: number | null
    upCount: number | null
    downCount: number | null
    flatCount: number | null
    avgChangePercent: number | null
    peMedian: number | null
    pbMedian: number | null
  watch: TargetWatchInfo | null
  alerts: AlertRule[]
  events: TargetEvent[]
  relatedTargets: RelatedTarget[]
  stockProfile: StockProfile | null
  availableTabs: TargetDetailTabCode[]
  meta: TargetDetailMeta
```

### 数据来源与处理过程

| 结果部分 | 物理表 | 简要处理 |
|---|---|---|
| `targetProfile` / `stockProfile` | `type_basic_data.stock_basic_data`；指数场景可用 `type_basic_data.index_basic_data` | 根据 `full_symbol` 查询基础信息，映射名称、交易所、市场类型、货币、行业等字段。 |
| `quote` | 股票：`type_exchange_data.stock_k_daily`；股票可关联 `type_exchange_data.stock_adjust_factor` | 查询最新交易日行情；计算 `amplitude=(high-low)/preClose*100`；股票填充 `adjustFactor`。 |
| `valuation` | 股票：`type_exchange_data.stock_indicator_daily` | 查询最新指标行，映射 PE、PB、PS、股息率、市值等。 |
| `financial` | `type_finance_data.income_data`、`finance_indicator_data`、`cashflow_data`、`balance_sheet_data` | 按最新报告期取财务摘要，优先使用最新有效 `end_date`。 |
| `capitalFlow` | `type_exchange_data.stock_moneyflow_daily` | 查询最新资金流，拆分特大单、大单、中单、小单净流入。 |
| `industry` | `type_basic_data.stock_basic_data`、`type_basic_data.index_industry_level`、行情与指标表 | 股票按所属行业聚合；指数按行业层级表补充。 |
| `metricCards` | 上述行情、估值、资金、财务结果 | Python 组合顶部指标卡，生成状态、说明、排序。 |

### 生成结果

生成轻量首屏对象，不包含 `quoteRows`、`valuationRows`、`financialDetail`、`moneyFlowRows` 等大数组。

---

## 2.2 `get_target_overview_detail`

### 功能描述

返回中部综合分析结论，包括综合观点、风险等级、估值状态、摘要、支撑理由、风险点、观察建议和置信度。
**注：该部分尚未形成有效的能力，暂时生成固定的结果。**

### 对应页面

详情页中部“综合分析结论”。

### 请求参数

```yaml
fullSymbol: string
tradeDate: string | null
```

### 返回参数：`TargetOverviewDetail`

```yaml
TargetOverviewDetail:
  fullSymbol: string
  overview:
    overallView: OverallView | null
    overallViewText: string | null
    riskLevel: RiskLevel | null
    valuationStatus: ValuationStatus | null
    summary: string | null
    reasons: string[]
    risks: string[]
    suggestions: string[]
    confidence: number | null
  meta: TargetDetailMeta
```

### 数据来源与处理过程

| 结果部分 | 物理表 | 简要处理 |
|---|---|---|
| `overview` | 行情表、估值指标表、资金流表、财务四表、行业基础表 | 当前物理表没有 `TargetOverview` 专表；Python 根据行情趋势、估值分位、资金流、财务质量和行业对比规则生成，暂时生成固定结果。 |
| `valuationStatus` | `stock_indicator_daily` / `index_indicator_daily` | 使用 PE TTM、PB、PS TTM 与历史分位或行业中位数比较生成。 |
| `riskLevel` | 行情、财务、资金衍生数据 | 结合波动、回撤、资产负债率、现金流与资金流持续性生成。 |

### 生成结果

生成 `TargetOverviewDetail`，供综合分析模块独立刷新，不影响顶部和各 Tab 数据。

---

## 2.3 `get_target_quote_detail`

### 功能描述

返回行情 Tab 所需数据，包括后端按复权类型计算后的行情明细、技术图表、技术分析、区间表现与对比分析。

### 对应页面

`QuoteDetailPage.vue`

### 请求参数：`TargetQuoteDetailQuery`

```yaml
TargetQuoteDetailQuery:
  fullSymbol: string
  tradeDate: string | null
  historyLimit: number              
  adjustType: 'NONE' | 'FORWARD' | 'BACKWARD'
  benchmarkCode: string | null  # 默认为沪深300指数
```

### 返回参数：`TargetQuoteDetail`

```yaml
TargetQuoteDetail:
  fullSymbol: string | null
  quote: MarketQuote | null
  charts:
    - chartId: string
      title: string | null
      chartType: string
      period: 'DAILY' | 'WEEKLY' | 'MONTHLY' | null
      xAxis: string[]
      series:
        - name: string
          data: array
          unit: string | null
      options: object | null
  performance:
    - periodCode: string
      periodName: string
      changePercent: number | null
      maxDrawdown: number | null
      volatility: number | null
      rankingText: string | null
  quoteRows:
    - MarketQuote
  quoteTechnicalAnalysis:
    conclusion:
      label: string
      summary: string
      score: number
      signalStrength: string
      period: string
      updatedAt: string
      levels:
        - key: string
          label: string
          level: number
          tone: 'up' | 'down' | 'flat' | 'risk'
          text: string
    dimensionScores:
      - dimension: string
        score: number
        explanation: string
    indicatorSignals:
      - indicator: string
        status: string
        direction: string
        explanation: string
    signalConflict:
      bullishCount: number
      bearishCount: number
      neutralCount: number
      consistency: string
      supports: string[]
      risks: string[]
    explanation: string | null
  quoteRangeComparison:
    marketExcessReturn: number | null
    industryExcessReturn: number | null
    industryRankText: string | null
    marketRankText: string | null
    alpha: number | null
    beta: number | null
    correlation: number | null
    relativeStrengthScore: number | null
  rangePerformance:
    rangeCode: RangeCode | null
    startDate: string | null
    endDate: string | null
    returnRisk:
      changePercent: number | null
      maxDrawdown: number | null
      volatility: number | null
      rankingText: string | null
      high: number | null
      low: number | null
      amplitude: number | null
      annualizedReturn: number | null
      sharpeRatio: number | null
      calmarRatio: number | null
    volume:
      volume: number | null
      turnover: number | null
      turnoverRate: number | null
      avgVolume: number | null
      avgTurnover: number | null
      avgTurnoverRate: number | null
      maxVolume: number | null
      maxVolumeTradeDate: string | null
      maxTurnover: number | null
      maxTurnoverTradeDate: string | null
    comparison: QuoteRangeComparison
    priceStructure:
      open: number | null
      high: number | null
      low: number | null
      close: number | null
      preClose: number | null
      pricePositionPercent: number | null
      drawdownFromHigh: number | null
      reboundFromLow: number | null
      newHighCount: number | null
      newLowCount: number | null
      priceRangeText: string | null
      turnoverRangeText: string | null
  adjustType: AdjustType
  period: TargetQuotePeriod | null
  meta: TargetDetailMeta
```

### 数据来源与处理过程

| 结果部分 | 物理表 | 简要处理 |
|---|---|---|
| `quoteRows` | 股票：`type_exchange_data.stock_k_daily`；指数：`type_exchange_data.index_k_daily` | 按 `full_symbol` 和 `historyLimit` 倒序取最近 N 条，再升序返回。 |
| 复权价格 | `type_exchange_data.stock_adjust_factor` + `stock_k_daily` | 股票根据 `adjustType` 计算 `open/high/low/close/preClose`；指数不复权。 **复权价格计算方法：stock_adjust_factor中对每个复权因子生成一条记录，每条记录的trade_date代表该复权因子有效的最后一天，比如000001.SZ, 2026-06-12, 103 \n 000001.SZ, 2026-07-08, 104，2026-06-13至2026-07-08的复权因子为104，2026-06-12（含）以前的复权因子为103，复权价的计算方法为：前复权，从最近一天往前算，前复权价格=除权价格 * 当天的复权因子 / 最新的复权因子，后复权价格，从最早一天往后算，后复权价格= 除权价格 * 最新复权因子 / 当天复权因子**|
| `turnoverRate` | 股票：`stock_indicator_daily`；指数：`index_indicator_daily` | 与 K 线按 `full_symbol + trade_date` 左连接。 |
| `charts` | 行情明细 + 技术指标计算结果 | Python 使用 pandas 计算 MA/EMA/BOLL/MACD/KDJ/RSI 等，统一输出 `ChartGroup[]`。 |
| `rangePerformance.returnRisk` | `quoteRows` | 计算区间收益、最大回撤、波动率、振幅、年化收益等。 |
| `rangePerformance.volume` | `quoteRows` + 指标表 | 计算区间成交量、成交额、换手率、均值和极值日期。 |
| `rangePerformance.comparison` | 基准指数行情、行业同行行情 | 计算相对大盘收益、相对行业收益、Alpha、Beta、相关性；排名字段无足够数据时返回 null。 |
| `rangePerformance.priceStructure` | `quoteRows` | 计算价格位置、新高新低次数、区间结构。 |

### 生成结果

生成行情 Tab 完整数据。前端不再本地计算复权价，直接使用后端返回的 `quoteRows` 与 `charts`。

---

## 2.4 `get_target_valuation_summary_detail`

### 功能描述

返回估值 Tab 顶部摘要，包括当前估值、历史指标行和估值指标摘要。

### 对应页面

`ValuationDetailPage.vue`

### 请求参数

```yaml
fullSymbol: string
tradeDate: string | null
```

### 返回参数：`TargetValuationSummaryDetail`

```yaml
TargetValuationSummaryDetail:
  fullSymbol: string
  valuation:
    pe: number | null
    peTtm: number | null
    pb: number | null
    ps: number | null
    psTtm: number | null
    dvTtm: number | null
    totalMarketValue: number | null
  valuationRows:
    - fullSymbol: string
      tradeDate: string
      close: number | null
      totalMarketValue: number | null
      floatMarketValue: number | null
      totalShare: number | null
      floatShare: number | null
      freeShare: number | null
      turnoverRate: number | null
      turnoverRateFree: number | null
      pe: number | null
      peTtm: number | null
      pb: number | null
      ps: number | null
      psTtm: number | null
      dvRatio: number | null
      dvTtm: number | null
      industryAveragePeTtm: number | null
      industryMedianPeTtm: number | null
      industryAveragePb: number | null
      industryMedianPb: number | null
      industryAveragePsTtm: number | null
      industryMedianPsTtm: number | null
      industryAverageDvTtm: number | null
      industryMedianDvTtm: number | null
  valuationDetail:
    - metricCode: ValuationMetricCode
      metricName: string | null
      currentValue: number | null
      percentileValue: number | null
      percentile30Value: number | null
      percentile70Value: number | null
      industryAverageValue: number | null
      industryMedianValue: number | null
      latestTradeDate: string | null
      description: string | null
  meta: TargetDetailMeta
```

### 数据来源与处理过程

| 结果部分 | 物理表 | 简要处理 |
|---|---|---|
| `valuation` | 股票：`stock_indicator_daily`；指数：`index_indicator_daily` | 取最新交易日估值指标。 |
| `valuationRows` | 股票：`stock_indicator_daily`；指数：`index_indicator_daily` | 取历史估值行；股票包含 PS、股息率，指数需要根据其成分股来计算得到相应的指标，通过流通市值加权平均，`index_indicator_daily` 表中数据缺失。 |
| `valuationDetail` | 指标历史 + 行业聚合 | 对 PE TTM、PB、PS TTM 计算当前值、历史分位、30/70 分位、行业均值与中位数。 |

### 生成结果

生成估值摘要与可复用的历史估值基础数据。

---

## 2.5 `get_target_valuation_channel_detail`

### 功能描述

返回某个估值指标下的估值通道图数据。

### 对应页面

`ValuationDetailPage.vue`

### 请求参数

```yaml
fullSymbol: string
tradeDate: string | null
metricCode: ValuationMetricCode     # PE_TTM | PB | PS_TTM
historyLimit: number                
```

### 返回参数：`TargetValuationChannelDetail`

```yaml
TargetValuationChannelDetail:
  fullSymbol: string
  metricCode: ValuationMetricCode
  channelRows:
    - fullSymbol: string
      tradeDate: string
      metricCode: ValuationMetricCode
      close: number | null
      channelMultiple1: number | null
      channelPrice1: number | null
      channelMultiple2: number | null
      channelPrice2: number | null
      channelMultiple3: number | null
      channelPrice3: number | null
      channelMultiple4: number | null
      channelPrice4: number | null
      channelMultiple5: number | null
      channelPrice5: number | null
  meta: TargetDetailMeta
```

### 数据来源与处理过程

| 结果部分 | 物理表 | 简要处理 |
|---|---|---|
| `channelRows.close` | `stock_k_daily` / `index_k_daily` | 取指定期限内收盘价。 |
| 估值倍数 | `stock_indicator_daily` | 依据 `metricCode` 选择 PE TTM、PB、PS TTM 的历史序列。 |
| 通道价格 | 行情 + 估值指标 | Python 计算 5 条通道倍数与对应价格线；物理表无现成通道字段。计算方法为：选择指定期限范围内指定指标的最小值作为最下方的通道，中间轨道为该期限的中位值，两者差值的0.5倍为相邻通道 |

### 生成结果

生成估值通道图明细 `channelRows`。

---

## 2.6 `get_target_valuation_trend_detail`

### 功能描述

返回某个估值指标下，股票自身估值、行业平均估值、行业中位数估值与分位线。

### 对应页面

`ValuationDetailPage.vue`

### 请求参数

```yaml
fullSymbol: string
metricCode: ValuationMetricCode
tradeDate: string | null
historyLimit: number
```

### 返回参数：`TargetValuationTrendDetail`

```yaml
TargetValuationTrendDetail:
  fullSymbol: string
  metricCode: ValuationMetricCode
  metricSummaries:
    - ValuationMetricSummary
  trendRows:
    - fullSymbol: string
      tradeDate: string
      metricCode: ValuationMetricCode
      stockValue: number | null
      industryAverageValue: number | null
      industryMedianValue: number | null
      percentile30Value: number | null
      percentile70Value: number | null
      percentileValue: number | null
  meta: TargetDetailMeta
```

### 数据来源与处理过程

| 结果部分 | 物理表 | 简要处理 |
|---|---|---|
| `stockValue` | `stock_indicator_daily` | 按 `metricCode` 映射 PE TTM、PB、PS TTM。 |
| 行业均值 / 中位数 | `stock_basic_data` ；必要时使用 `index_industry_level` | 找到同业样本后按交易日聚合均值和中位数。行业均值由该申万三级行业的成分股根据流通市值加权平均得到，中位数不需要加权。 |
| 分位线 | 历史估值序列 | 计算当前期限内 30 分位、70 分位与每日分位。 |

### 生成结果

生成估值趋势图所需 `trendRows` 和摘要 `metricSummaries`。

---

## 2.7 `get_target_dividend_trend_detail`

### 功能描述

返回分红 TTM、行业平均分红率、行业中位数分红率走势。

### 对应页面

`ValuationDetailPage.vue`

### 请求参数

```yaml
fullSymbol: string
tradeDate: string | null
rangeCode: ValuationTimeRangeCode
historyLimit: number
```

### 返回参数：`TargetDividendTrendDetail`

```yaml
TargetDividendTrendDetail:
  fullSymbol: string | null
  dividendRows:
    - fullSymbol: string
      tradeDate: string
      dvTtm: number | null
      industryAverageDvTtm: number | null
      industryMedianDvTtm: number | null
  meta: TargetDetailMeta
```

### 数据来源与处理过程

| 结果部分 | 物理表 | 简要处理 |
|---|---|---|
| `dvTtm` | `stock_indicator_daily` | 股票场景取股息率 TTM；指数表没有股息率字段时返回空。 |
| 行业分红率 | `stock_basic_data` | 按股票所在的申万三级行业成分股根据流通市值加权平均得到行业平均数，中位数不需要加权。 |

### 生成结果

生成分红趋势图数据 `dividendRows`。

---

## 2.8 `get_target_financial_detail`

### 功能描述

返回财务 Tab 所需的财务摘要、四表明细和图表衍生行。

### 对应页面

`FinancialDetailPage.vue`

### 请求参数：`TargetFinancialDetailQuery`

```yaml
TargetFinancialDetailQuery:
  fullSymbol: string
  historyLimit: number | null        # 由前端传具体的年份数，Python 从当前年份往前推算，查询对应的报告期
```

### 返回参数：`TargetFinancialDetail`

```yaml
TargetFinancialDetail:
  fullSymbol: string | null
  financial:
    FinancialSummary
  financialDetail:
    incomeRows:
      - IncomeStatementRow
    balanceSheetRows:
      - BalanceSheetRow
    cashflowRows:
      - CashflowStatementRow
    indicatorRows:
      - FinanceIndicatorRow
    reportInfo:
      fullSymbol: string | null
      endDate: string | null
      annDate: string | null
      fAnnDate: string | null
      reportType: string | null
      reportTypeCode: FinancialReportTypeCode | null
      reportTypeText: string | null
    profitTrendRows:
      - fullSymbol: string
        endDate: string
        annDate: string | null
        year: number | null
        quarterCode: FinancialQuarterCode | null
        reportTypeText: string | null
        totalRevenue: number | null
        totalRevenueYoy: number | null
        nIncomeAttrP: number | null
        nIncomeAttrPYoy: number | null
        profitDedt: number | null
        profitDedtYoy: number | null
        eps: number | null
        epsYoy: number | null
    ratioTrendRows:
      - fullSymbol: string
        endDate: string
        reportTypeText: string | null
        roe: number | null
        grossMargin: number | null
        netprofitMargin: number | null
        debtToAssets: number | null
    cashflowStructureRows:
      - fullSymbol: string
        endDate: string
        year: number | null
        quarterCode: FinancialQuarterCode | null
        nCashflowAct: number | null
        nCashflowInvAct: number | null
        nCashFlowsFncAct: number | null
        freeCashflow: number | null
    balanceStructureRows:
      - fullSymbol: string
        endDate: string
        totalCurAssets: number | null
        totalNca: number | null
        totalAssets: number | null
        totalCurLiab: number | null
        totalNcl: number | null
        totalLiab: number | null
        totalHldrEqyExcMinInt: number | null
        debtToAssets: number | null
  meta: TargetDetailMeta
```

### 数据来源与处理过程

| 结果部分 | 物理表 | 简要处理 |
|---|---|---|
| `incomeRows` | `type_finance_data.income_data` | 按 `full_symbol` 查询报告期，映射利润表字段。 |
| `balanceSheetRows` | `type_finance_data.balance_sheet_data` | 查询资产负债表字段，按 `end_date` 排序。 |
| `cashflowRows` | `type_finance_data.cashflow_data` | 查询现金流量表字段，生成现金流结构。 |
| `indicatorRows` | `type_finance_data.finance_indicator_data` | 查询 ROE、毛利率、净利率、资产负债率、同比指标等。 |
| `financial` | 四表最新期 | 汇总最新收入、利润、ROE、资产负债率、现金流、EPS、BPS 等。 |
| `reportInfo` | 四表最新公告字段 | 按最新报告期推导财报类型和公告日期。 |
| 趋势行 | 四表明细 | Python 对齐 `end_date`，生成利润趋势、盈利能力、现金流结构、资产负债结构。 |

### 生成结果

生成财务 Tab 可直接渲染的数据对象。

---

## 2.9 `get_target_capital_flow_detail`

### 功能描述

返回资金流 Tab 所需的资金流摘要、资金流历史明细和价格联动数据。

### 对应页面

`CapitalFlowDetailPage.vue`

### 请求参数：`TargetCapitalFlowDetailQuery`

```yaml
TargetCapitalFlowDetailQuery:
  fullSymbol: string
  tradeDate: string | null
  historyLimit: number              # 由 rangeSizeMap 得到
```

### 返回参数：`TargetCapitalFlowDetail`

```yaml
TargetCapitalFlowDetail:
  fullSymbol: string | null
  capitalFlow:
    tradeDate: string | null
    netMfAmount: number | null
    elgNetMfAmount: number | null
    lgNetMfAmount: number | null
    mdNetMfAmount: number | null
    smNetMfAmount: number | null
    conclusion: string | null
  moneyFlowRows:
    - fullSymbol: string
      tradeDate: string
      buySmVol: number | null
      buySmAmount: number | null
      sellSmVol: number | null
      sellSmAmount: number | null
      buyMdVol: number | null
      buyMdAmount: number | null
      sellMdVol: number | null
      sellMdAmount: number | null
      buyLgVol: number | null
      buyLgAmount: number | null
      sellLgVol: number | null
      sellLgAmount: number | null
      buyElgVol: number | null
      buyElgAmount: number | null
      sellElgVol: number | null
      sellElgAmount: number | null
      netMfVol: number | null
      netMfAmount: number | null
  close: number[] | null
  meta: TargetDetailMeta
```

### 数据来源与处理过程

| 结果部分 | 物理表 | 简要处理 |
|---|---|---|
| `moneyFlowRows` | `type_exchange_data.stock_moneyflow_daily` | 按 `full_symbol` 和 `historyLimit` 查询资金流历史。 |
| `capitalFlow` | `stock_moneyflow_daily` 最新行 | 取最新交易日净流入额，并计算各单类型净额：买入金额 - 卖出金额。 |
| `close` | `type_exchange_data.stock_k_daily` | 按相同日期序列返回收盘价数组，用于图表联动。 |

### 生成结果

生成资金流摘要、资金流历史明细和价格联动数组。

---

## 2.10 `get_target_industry_detail`

### 功能描述

返回行业 Tab 的整体数据。该 Tab 暂时不拆分，包含行业摘要、指数基础信息、股票基础信息、指数成分、同行业可比和行业扩展信息。
注意，行业统一使用申万分级行业，同行业、行业摘要、行业成分对比仅使用申万三级行业，涉及行业层级的归属使用申万行业层级表。

### 对应页面

`ConstituentIndustryDetailPage.vue`

### 请求参数

```yaml
fullSymbol: string
tradeDate: string | null
```

### 返回参数：`TargetIndustryDetail`

```yaml
TargetIndustryDetail:
  fullSymbol: string | null
  industry:
    industryCode: string | null
    industryName: string | null
    industryLevel: 1 | 2 | 3 | null
    stockCount: number | null
    upCount: number | null
    downCount: number | null
    flatCount: number | null
    avgChangePercent: number | null
    peMedian: number | null
    pbMedian: number | null
  indexProfile:
    fullSymbol: string
    name: string
    fullname: string | null
    market: string | null
    marketName: string | null
    publisher: string | null
    indexType: string | null
    category: string | null
    baseDate: string | null
    basePoint: number | null
    listDate: string | null
    weightRule: string | null
    description: string | null
    expDate: string | null
    tags: string[]
  stockProfile:
    fullSymbol: string
    symbol: string | null
    name: string | null
    region: string | null
    industry: string | null
    fullName: string | null
    enName: string | null
    cnSpell: string | null
    marketType: string | null
    exchange: string | null
    currencyType: string | null
    listStatus: string | null
    listDate: string | null
    delistDate: string | null
    isHs: string | null
    actName: string | null
    actEntType: string | null
  constituentRows:
    - indexCode: string
      stockCode: string
      tradeDate: string
      weight: number | null
      name: string | null
      industry: string | null
      industryL1: string | null
      industryL2: string | null
      industryL3: string | null
      close: number | null
      chgPct: number | null
      totalMarketValue: number | null
      floatMarketValue: number | null
      peTtm: number | null
      pb: number | null
      turnoverRate: number | null
  industryPeerRows:
    - fullSymbol: string
      symbol: string | null
      name: string | null
      region: string | null
      industry: string | null
      close: number | null
      chgPct: number | null
      totalMarketValue: number | null
      floatMarketValue: number | null
      peTtm: number | null
      pb: number | null
      psTtm: number | null
      turnoverRate: number | null
  industryDetail:
    swIndustryRows:
      - fullSymbol: string
        stockName: string | null
        industryCode: string | null
        industryName: string
        industryLevel: 1 | 2 | 3
        parentIndustryCode: string | null
        parentIndustryName: string | null
        tradeDate: string | null
        chgPct: number | null
        totalMarketValue: number | null
        turnover: number | null
        stockCount: number | null
        peTtm: number | null
        pb: number | null
        psTtm: number | null
    belongIndexRows:
      - fullSymbol: string
        stockName: string | null
        indexCode: string
        indexName: string
        indexCategory: string | null
        indexCategoryName: string | null
        tradeDate: string | null
        chgPct: number | null
        totalMarketValue: number | null
        turnover: number | null
        weight: number | null
    benchmarkIndustryLevel: 1 | 2 | 3 | null
    benchmarkIndustryCode: string | null
    benchmarkIndustryName: string | null
  meta: TargetDetailMeta
```

### 数据来源与处理过程

| 结果部分 | 物理表 | 简要处理 |
|---|---|---|
| `indexProfile` | `type_basic_data.index_basic_data` | 指数标的按 `full_symbol` 查询基础信息。 |
| `stockProfile` | `type_basic_data.stock_basic_data` | 股票标的按 `full_symbol` 查询基础信息。 |
| `constituentRows` | `index_constituent_data` + `stock_basic_data` + `stock_k_daily` + `stock_indicator_daily` | 指数场景查询最新成分，关联股票名称、行业、行情、估值、换手率。 |
| `industryPeerRows` | `stock_basic_data` + `stock_k_daily` + `stock_indicator_daily` | 股票场景按行业筛选同行股票，关联最新行情和估值。 |
| `industryDetail.swIndustryRows` | `index_industry_level`、`stock_basic_data` | 生成申万行业层级归属。 |
| `industryDetail.belongIndexRows` | `index_constituent_data` + `index_basic_data` + `index_k_daily` + `index_indicator_daily` | 查询股票所属指数及权重，并补充指数行情和市值。 |
| `industry` | 同行业样本聚合 | 统计股票数量、上涨/下跌/平盘数量、平均涨跌幅、PE/PB 中位数。 |

### 生成结果

生成行业 Tab 整体对象，暂不拆分分页接口。

---

# 3. Python 实现建议结构

```python
class StockService:
    def get_target_detail_shell(self, full_symbol: str) -> dict: ...
    def get_target_overview_detail(self, full_symbol: str) -> dict: ...
    def get_target_quote_detail(
        self,
        full_symbol: str,
        range_code: str,
        adjust_type: str,
        period: str = "DAILY",
        benchmark_code: str | None = None,
        industry_code: str | None = None,
    ) -> dict: ...
    def get_target_valuation_summary_detail(self, full_symbol: str) -> dict: ...
    def get_target_valuation_channel_detail(self, full_symbol: str, metric_code: str, range_code: str) -> dict: ...
    def get_target_valuation_trend_detail(self, full_symbol: str, metric_code: str, range_code: str) -> dict: ...
    def get_target_dividend_trend_detail(self, full_symbol: str, range_code: str) -> dict: ...
    def get_target_financial_detail(self, full_symbol: str, range_code: str = "2Y") -> dict: ...
    def get_target_capital_flow_detail(self, full_symbol: str, range_code: str = "3M", include_quote_rows: bool = True) -> dict: ...
    def get_target_industry_detail(self, full_symbol: str) -> dict: ...
```

## 3.1 字段映射原则

| 数据库字段 | 接口字段 |
|---|---|
| `full_symbol` | `fullSymbol` |
| `trade_date` | `tradeDate` |
| `pre_close` | `preClose` |
| `chg_amt` | `chgAmt` |
| `chg_pct` | `chgPct` |
| `turnover_rate` | `turnoverRate` |
| `total_market_value` | `totalMarketValue` |
| `float_market_value` | `floatMarketValue` |
| `pe_ttm` | `peTtm` |
| `ps_ttm` | `psTtm` |
| `dv_ttm` | `dvTtm` |
| `net_mf_amount` | `netMfAmount` |
| `n_income_attr_p` | `nIncomeAttrP` |
| `n_cashflow_act` | `nCashflowAct` |
| `debt_to_assets` | `debtToAssets` |
| `gross_margin` | `grossMargin` |
| `netprofit_margin` | `netprofitMargin` |

## 3.2 物理表覆盖范围

当前接口可直接或间接使用的物理表：

```yaml
type_basic_data:
  - index_basic_data
  - index_constituent_data
  - index_industry_level
  - stock_basic_data
type_exchange_data:
  - index_indicator_daily
  - index_k_daily
  - stock_adjust_factor
  - stock_indicator_daily
  - stock_k_daily
  - stock_moneyflow_daily
type_finance_data:
  - balance_sheet_data
  - cashflow_data
  - finance_indicator_data
  - income_data
```

三个物理库分别对应3个repo文件，每个文件负责查询需要的数据，查询到的数据统一由stock_service进行整合和衍生计算，返回给前端。

---

# 4. 推荐调用顺序

```yaml
页面初始化:
  - get_target_detail_shell
  - get_target_overview_detail
  - get_target_quote_detail

切换到行情 Tab:
  - get_target_quote_detail

切换到估值 Tab:
  - get_target_valuation_summary_detail
  - get_target_valuation_channel_detail
  - get_target_valuation_trend_detail
  - get_target_dividend_trend_detail

切换到财务 Tab:
  - get_target_financial_detail

切换到资金 Tab:
  - get_target_capital_flow_detail

切换到行业 Tab:
  - get_target_industry_detail
```

---
