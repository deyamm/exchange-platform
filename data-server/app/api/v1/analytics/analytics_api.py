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

# /api/v1/data-analytics/stock_detail
@router.get(
    "/stock-detail",
    summary="获取股票详细数据",
    response_model=dict,
    description="返回指定股票的详细数据，包括财务指标、估值指标等信息。"
)
async def get_stock_detail(
    full_symbol: str = Query(..., alias="fullSymbol", description="股票代码，例如 'AAPL'"),
    trade_date: Optional[str] = Query(None, alias="tradeDate", description="指定交易日期，格式 YYYY-MM-DD；为空时返回最新数据"),
    include_charts: bool = Query(True, alias="includeCharts", description="是否包含图表数据，默认为 False"),
    include_financial: bool = Query(True, alias="includeFinancials", description="是否包含财务数据，默认为 False"),
    include_history: bool = Query(True, alias="includeHistory", description="是否包含历史数据，默认为 False"),
    service: StockService = Depends(get_stock_service)
):
    """
    获取股票详细数据

    返回指定股票的详细数据，包括财务指标、估值指标等信息。
    """
    logger.info("get_stock_detail called, full_symbol=%s, trade_date=%s, include_charts=%s, include_financial=%s, include_history=%s", full_symbol, trade_date, include_charts, include_financial, include_history)

    return await service.get_stock_detail(full_symbol=full_symbol, trade_date=trade_date, include_charts=include_charts, include_financial=include_financial, include_history=include_history)


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