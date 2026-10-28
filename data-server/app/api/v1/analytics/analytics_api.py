from typing import Optional

from fastapi import APIRouter, Depends, Query
import logging

from app.service.analytics.market_service import MarketService
from app.service.analytics.stock_service import StockService
from app.service.analytics.index_service import IndexService
from app.api.deps import get_market_service, get_stock_service, get_index_service


logger = logging.getLogger(__name__)
router = APIRouter()

# /api/v1/data-analytics/market-overview
@router.get(
    "/market-overview",
    summary="获取市场概览数据",
    response_model=dict,
    description="返回当前市场的概览数据，包括主要指数、行业表现等信息。"
)
async def get_market_overview(
    trade_date: Optional[str] = Query(None, alias="tradeDate", description="指定交易日期，格式 YYYY-MM-DD；为空时返回最新数据"),
    service: MarketService = Depends(get_market_service)
):
    """
    获取市场概览数据

    返回当前市场的概览数据，包括主要指数、行业表现等信息。
    """
    logger.info("get_market_overview called, trade_date=%s", trade_date)

    return await service.get_market_overview(trade_date=trade_date)

# /api/v1/data-analytics/stock-detail
@router.get(
    "/stock-detail",
    summary="获取标的详情首屏数据",
    response_model=dict,
    description="返回详情页首屏所需的标的基础信息、最新行情、顶部指标卡与各模块摘要，不包含 K 线、估值、财务等大数组。"
)
async def get_target_detail_shell(
    full_symbol: str = Query(..., alias="fullSymbol", description="股票完整代码，例如 '000001.SZ'"),
    trade_date: Optional[str] = Query(None, alias="tradeDate", description="指定交易日期，格式 YYYY-MM-DD；为空时返回最新数据"),
    service: StockService = Depends(get_stock_service)
):
    """
    获取标的详情首屏数据。

    对应前端 watch-targets/{targetCode}/details 接口，仅返回首屏轻量数据；
    行情、估值、财务、资金流、行业等明细由下方 /stock-detail/* 子接口按 Tab 按需加载。
    """
    logger.info("get_target_detail_shell called, full_symbol=%s, trade_date=%s", full_symbol, trade_date)

    return await service.get_target_detail_shell(full_symbol=full_symbol, trade_date=trade_date)


# /api/v1/data-analytics/stock-detail/overview
@router.get(
    "/stock-detail/overview",
    summary="获取标的综合概览",
    response_model=dict,
    description="返回详情页中部的综合分析结论，包括综合观点、风险等级、估值状态、理由、风险点和建议。"
)
async def get_target_overview_detail(
    full_symbol: str = Query(..., alias="fullSymbol", description="股票完整代码，例如 '000001.SZ'"),
    trade_date: Optional[str] = Query(None, alias="tradeDate", description="指定交易日期，格式 YYYY-MM-DD；为空时返回最新数据"),
    service: StockService = Depends(get_stock_service)
):
    """
    获取标的综合概览。

    综合观点当前仍由可解释规则生成，后续可替换为模型或更完整的规则实现。
    """
    logger.info("get_target_overview_detail called, full_symbol=%s, trade_date=%s", full_symbol, trade_date)

    return await service.get_target_overview_detail(full_symbol=full_symbol, trade_date=trade_date)


# /api/v1/data-analytics/stock-detail/quote
@router.get(
    "/stock-detail/quote",
    summary="获取行情详情",
    response_model=dict,
    description="返回复权后的 K 线、技术指标图表、技术指标综合分析与当前区间的收益风险统计。"
)
async def get_target_quote_detail(
    full_symbol: str = Query(..., alias="fullSymbol", description="股票完整代码，例如 '000001.SZ'"),
    trade_date: Optional[str] = Query(None, alias="tradeDate", description="指定交易日期，格式 YYYY-MM-DD；为空时返回最新数据"),
    range_code: Optional[str] = Query(None, alias="rangeCode", description="时间范围编码：1M/3M/6M/1Y/2Y；与 historyLimit 二选一"),
    adjust_type: str = Query("NONE", alias="adjustType", description="复权类型：NONE/FORWARD/BACKWARD，默认 NONE"),
    benchmark_code: Optional[str] = Query(None, alias="benchmarkCode", description="对比基准代码，为空时使用沪深300"),
    industry_code: Optional[str] = Query(None, alias="industryCode", description="行业代码，用于行业超额收益和排名预留"),
    history_limit: Optional[int] = Query(None, alias="historyLimit", description="历史数据条数；传入时优先于 rangeCode"),
    service: StockService = Depends(get_stock_service)
):
    """
    获取行情 Tab 数据。

    K 线先按复权口径处理，再统一计算技术指标，保证图表、技术分析和区间统计使用同一批行情。
    """
    logger.info(
        "get_target_quote_detail called, full_symbol=%s, trade_date=%s, range_code=%s, adjust_type=%s, history_limit=%s",
        full_symbol, trade_date, range_code, adjust_type, history_limit
    )

    return await service.get_target_quote_detail(
        full_symbol=full_symbol,
        trade_date=trade_date,
        history_limit=history_limit,
        adjust_type=adjust_type,
        benchmark_code=benchmark_code,
        range_code=range_code,
        industry_code=industry_code,
    )


# /api/v1/data-analytics/stock-detail/valuation-summary
@router.get(
    "/stock-detail/valuation-summary",
    summary="获取估值摘要",
    response_model=dict,
    description="返回估值 Tab 顶部 PE/PB/PS/股息率与总市值等摘要信息。"
)
async def get_target_valuation_summary_detail(
    full_symbol: str = Query(..., alias="fullSymbol", description="股票完整代码，例如 '000001.SZ'"),
    trade_date: Optional[str] = Query(None, alias="tradeDate", description="指定交易日期，格式 YYYY-MM-DD；为空时返回最新数据"),
    history_limit: Optional[int] = Query(None, alias="historyLimit", description="历史数据条数；传入时优先于 rangeCode"),
    range_code: Optional[str] = Query(None, alias="rangeCode", description="估值时间范围：1Y/3Y/5Y/10Y，默认 1Y"),
    service: StockService = Depends(get_stock_service)
):
    """
    获取估值摘要。

    只返回最新估值指标和分位状态，估值通道、估值走势、分红走势由其他子接口提供。
    """
    logger.info(
        "get_target_valuation_summary_detail called, full_symbol=%s, trade_date=%s, history_limit=%s, range_code=%s",
        full_symbol, trade_date, history_limit, range_code
    )

    return await service.get_target_valuation_summary_detail(
        full_symbol=full_symbol,
        trade_date=trade_date,
        history_limit=history_limit,
        range_code=range_code,
    )


# /api/v1/data-analytics/stock-detail/valuation-channel
@router.get(
    "/stock-detail/valuation-channel",
    summary="获取估值通道",
    response_model=dict,
    description="返回指定估值指标的收盘价与 5 条估值通道线。"
)
async def get_target_valuation_channel_detail(
    full_symbol: str = Query(..., alias="fullSymbol", description="股票完整代码，例如 '000001.SZ'"),
    trade_date: Optional[str] = Query(None, alias="tradeDate", description="指定交易日期，格式 YYYY-MM-DD；为空时返回最新数据"),
    metric_code: str = Query("PE_TTM", alias="metricCode", description="估值指标：PE_TTM/PB/PS_TTM，默认 PE_TTM"),
    history_limit: Optional[int] = Query(None, alias="historyLimit", description="历史数据条数；传入时优先于 rangeCode"),
    range_code: Optional[str] = Query(None, alias="rangeCode", description="估值时间范围：1Y/3Y/5Y/10Y，默认 1Y"),
    service: StockService = Depends(get_stock_service)
):
    """获取指定估值指标的估值通道。"""
    logger.info(
        "get_target_valuation_channel_detail called, full_symbol=%s, trade_date=%s, metric_code=%s",
        full_symbol, trade_date, metric_code
    )

    return await service.get_target_valuation_channel_detail(
        full_symbol=full_symbol,
        trade_date=trade_date,
        metric_code=metric_code,
        history_limit=history_limit,
        range_code=range_code,
    )


# /api/v1/data-analytics/stock-detail/valuation-trend
@router.get(
    "/stock-detail/valuation-trend",
    summary="获取估值走势",
    response_model=dict,
    description="返回指定估值指标的股票值、行业平均/中位数、30/70 分位与当前分位排名。"
)
async def get_target_valuation_trend_detail(
    full_symbol: str = Query(..., alias="fullSymbol", description="股票完整代码，例如 '000001.SZ'"),
    trade_date: Optional[str] = Query(None, alias="tradeDate", description="指定交易日期，格式 YYYY-MM-DD；为空时返回最新数据"),
    metric_code: str = Query("PE_TTM", alias="metricCode", description="估值指标：PE_TTM/PB/PS_TTM，默认 PE_TTM"),
    history_limit: Optional[int] = Query(None, alias="historyLimit", description="历史数据条数；传入时优先于 rangeCode"),
    range_code: Optional[str] = Query(None, alias="rangeCode", description="估值时间范围：1Y/3Y/5Y/10Y，默认 1Y"),
    service: StockService = Depends(get_stock_service)
):
    """获取指定估值指标的估值走势。"""
    logger.info(
        "get_target_valuation_trend_detail called, full_symbol=%s, trade_date=%s, metric_code=%s",
        full_symbol, trade_date, metric_code
    )

    return await service.get_target_valuation_trend_detail(
        full_symbol=full_symbol,
        trade_date=trade_date,
        metric_code=metric_code,
        history_limit=history_limit,
        range_code=range_code,
    )


# /api/v1/data-analytics/stock-detail/dividend-trend
@router.get(
    "/stock-detail/dividend-trend",
    summary="获取分红走势",
    response_model=dict,
    description="返回股票分红 TTM、申万三级行业平均与中位数分红率走势。"
)
async def get_target_dividend_trend_detail(
    full_symbol: str = Query(..., alias="fullSymbol", description="股票完整代码，例如 '000001.SZ'"),
    trade_date: Optional[str] = Query(None, alias="tradeDate", description="指定交易日期，格式 YYYY-MM-DD；为空时返回最新数据"),
    history_limit: Optional[int] = Query(None, alias="historyLimit", description="历史数据条数；传入时优先于 rangeCode"),
    range_code: Optional[str] = Query(None, alias="rangeCode", description="估值时间范围：1Y/3Y/5Y/10Y，默认 1Y"),
    service: StockService = Depends(get_stock_service)
):
    """获取分红 TTM 走势。"""
    logger.info(
        "get_target_dividend_trend_detail called, full_symbol=%s, trade_date=%s, history_limit=%s, range_code=%s",
        full_symbol, trade_date, history_limit, range_code
    )

    return await service.get_target_dividend_trend_detail(
        full_symbol=full_symbol,
        trade_date=trade_date,
        range_code=range_code,
        history_limit=history_limit,
    )


# /api/v1/data-analytics/stock-detail/financial
@router.get(
    "/stock-detail/financial",
    summary="获取财务详情",
    response_model=dict,
    description="返回财务摘要、利润表、资产负债表、现金流量表、财务指标与衍生趋势数据。"
)
async def get_target_financial_detail(
    full_symbol: str = Query(..., alias="fullSymbol", description="股票完整代码，例如 '000001.SZ'"),
    history_limit: Optional[int] = Query(None, alias="historyLimit", description="向前展示的年数，默认 5 年"),
    service: StockService = Depends(get_stock_service)
):
    """
    获取财务 Tab 数据。

    history_limit 表示向前展示的年数，服务层会换算为季度报告行数。
    """
    logger.info("get_target_financial_detail called, full_symbol=%s, history_limit=%s", full_symbol, history_limit)

    return await service.get_target_financial_detail(full_symbol=full_symbol, history_limit=history_limit)


# /api/v1/data-analytics/stock-detail/capital-flow
@router.get(
    "/stock-detail/capital-flow",
    summary="获取资金流向详情",
    response_model=dict,
    description="返回资金流摘要、stock_moneyflow_daily 历史明细，以及资金与价格联动所需的收盘价序列。"
)
async def get_target_capital_flow_detail(
    full_symbol: str = Query(..., alias="fullSymbol", description="股票完整代码，例如 '000001.SZ'"),
    trade_date: Optional[str] = Query(None, alias="tradeDate", description="指定交易日期，格式 YYYY-MM-DD；为空时返回最新数据"),
    history_limit: Optional[int] = Query(None, alias="historyLimit", description="历史数据条数；传入时优先于 rangeCode"),
    range_code: Optional[str] = Query(None, alias="rangeCode", description="时间范围编码：1M/3M/6M/1Y/2Y，默认 3M"),
    include_quote_rows: bool = Query(True, alias="includeQuoteRows", description="是否返回价格联动所需的收盘价序列，默认 True"),
    service: StockService = Depends(get_stock_service)
):
    """获取资金流 Tab 数据。"""
    logger.info(
        "get_target_capital_flow_detail called, full_symbol=%s, trade_date=%s, range_code=%s, include_quote_rows=%s",
        full_symbol, trade_date, range_code, include_quote_rows
    )

    return await service.get_target_capital_flow_detail(
        full_symbol=full_symbol,
        trade_date=trade_date,
        history_limit=history_limit,
        range_code=range_code,
        include_quote_rows=include_quote_rows,
    )


# /api/v1/data-analytics/stock-detail/industry
@router.get(
    "/stock-detail/industry",
    summary="获取行业详情",
    response_model=dict,
    description="股票返回申万三级行业归属、所属指数与同业对比；指数返回指数资料与成分股。"
)
async def get_target_industry_detail(
    full_symbol: str = Query(..., alias="fullSymbol", description="股票或指数完整代码，例如 '000001.SZ'、'000300.SH'"),
    trade_date: Optional[str] = Query(None, alias="tradeDate", description="指定交易日期，格式 YYYY-MM-DD；为空时返回最新数据"),
    service: StockService = Depends(get_stock_service)
):
    """获取行业 / 成分 Tab 数据。"""
    logger.info("get_target_industry_detail called, full_symbol=%s, trade_date=%s", full_symbol, trade_date)

    return await service.get_target_industry_detail(full_symbol=full_symbol, trade_date=trade_date)


# /api/v1/data-analytics/index-detail
@router.get(
    "/index-detail",
    summary="获取指数详细数据",
    response_model=dict,
    description="返回指定指数的详细数据，包括成分股、历史表现等信息。"
)
async def get_index_detail(
    full_symbol: str = Query(..., alias="fullSymbol", description="指数代码，例如 'S&P500'"),
    trade_date: Optional[str] = Query(None, alias="tradeDate", description="指定交易日期，格式 YYYY-MM-DD；为空时返回最新数据"),
    service: IndexService = Depends(get_index_service)
):
    """
    获取指数详细数据

    返回指定指数的详细数据，包括成分股、历史表现等信息。
    """
    logger.info("get_index_detail called, full_symbol=%s, trade_date=%s", full_symbol, trade_date)

    return await service.get_index_detail(full_symbol=full_symbol, trade_date=trade_date)

# /api/v1/data-analytics/index-detail/market
@router.get(
    "/index-detail/market",
    summary="获取指数市场数据",
    response_model=dict,
    description="返回指定指数的历史行情、图表数据、区间表现等。"
)
async def get_index_market_data(
    full_symbol: str = Query(..., alias="fullSymbol", description="指数代码，例如 'S&P500'"),
    trade_date: Optional[str] = Query(None, alias="tradeDate", description="指定交易日期，格式 YYYY-MM-DD；为空时返回最新数据"),
    history_limit: int = Query(250, alias="historyLimit", description="获取历史数据的数量，默认为 250"),
    service: IndexService = Depends(get_index_service)
):
    """
    获取指数市场数据

    返回指定指数的历史行情、图表数据、区间表现等。
    """
    logger.info("get_index_market called, full_symbol=%s, trade_date=%s, history_limit=%s", full_symbol, trade_date, history_limit)

    return await service.get_index_market(full_symbol=full_symbol, trade_date=trade_date, history_limit=history_limit)

# /api/v1/data-analytics/index-detail/constituents
@router.get(
    "/index-detail/constituents",
    summary="获取指数成分股数据",
    response_model=dict,
    description="返回指定指数的成分股数据，包括股票代码、权重等信息，以及需要的图表数据。"
)
async def get_index_constituents(
    full_symbol: str = Query(..., alias="fullSymbol", description="指数代码，例如 'S&P500'"),
    trade_date: Optional[str] = Query(None, alias="tradeDate", description="指定交易日期，格式 YYYY-MM-DD；为空时返回最新数据"),
    service: IndexService = Depends(get_index_service)
):
    """
    获取指数成分股数据

    返回指定指数的成分股数据，包括股票代码、权重等信息，以及需要的图表数据。
    """
    logger.info("get_index_constituent called, full_symbol=%s, trade_date=%s", full_symbol, trade_date)

    return await service.get_index_constituent(full_symbol=full_symbol, trade_date=trade_date)

# /api/v1/data-analytics/index-detail/constituent-page
@router.get(
    "/index-detail/constituent-page",
    summary="获取指数成分股分页数据",
    response_model=list,
    description="返回指定指数的成分股分页数据，包括股票代码、权重等信息，以及需要的图表数据。"
)
async def get_index_constituents_page(
    full_symbol: str = Query(..., alias="fullSymbol", description="指数代码，例如 'S&P500'"),
    trade_date: Optional[str] = Query(None, alias="tradeDate", description="指定交易日期，格式 YYYY-MM-DD；为空时返回最新数据"),
    page: int = Query(1, description="页码，默认为 1"),
    page_size: int = Query(10, alias="pageSize", description="每页数据量，默认为 10"),
    service: IndexService = Depends(get_index_service)
):
    """
    获取指数成分股分页数据

    返回指定指数的成分股分页数据，包括股票代码、权重等信息，以及需要的图表数据。
    """
    logger.info("get_index_constituent_page called, full_symbol=%s, trade_date=%s, page=%s, page_size=%s", full_symbol, trade_date, page, page_size)

    return await service.get_index_constituent_page(full_symbol=full_symbol, trade_date=trade_date, page=page, page_size=page_size)