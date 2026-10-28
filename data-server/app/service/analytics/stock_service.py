from __future__ import annotations

import logging
import statistics
from datetime import date, datetime
from decimal import Decimal
from typing import Any, Dict, List, Mapping, Optional, Sequence, Tuple

from sqlalchemy.ext.asyncio import AsyncConnection

from app.repository.basic_data_repo import BasicDataRepo
from app.repository.exchange_data_repo import ExchangeDataRepo
from app.repository.finance_data_repo import FinanceDataRepo
from app.utils.datetime_utils import get_current_datetime, resolve_trade_date
from app.utils.indicator_utils import (
    annualized_volatility_from_pct,
    calculate_alpha_beta_placeholder,
    calculate_technical_indicator_rows,
    estimate_risk_level_placeholder,
    max_drawdown_from_rows,
    percentile_rank,
    percentile_value,
    round_or_none,
    to_float_or_none,
)
from app.utils.target_detail_utils import build_default_watch_info, compact_dict, metric_card, net_flow, strip_exchange
from app.utils.typing_utils import to_str_date

logger = logging.getLogger(__name__)

EXCHANGE_NAME_MAP = {
    "SSE": "上海证券交易所",
    "SZSE": "深圳证券交易所",
    "BSE": "北京证券交易所",
    "SH": "上海证券交易所",
    "SZ": "深圳证券交易所",
    "BJ": "北京证券交易所",
}

REPORT_TYPE_TEXT_MAP = {
    "Q1": "一季报",
    "HALF_YEAR": "半年报",
    "Q3": "三季报",
    "ANNUAL": "年报",
}

VALUATION_METRICS = {
    "PE_TTM": {"field": "pe_ttm", "camel": "peTtm", "name": "PE TTM"},
    "PB": {"field": "pb", "camel": "pb", "name": "PB"},
    "PS_TTM": {"field": "ps_ttm", "camel": "psTtm", "name": "PS TTM"},
}

RANGE_SIZE_MAP = {
    "1M": 22,
    "3M": 66,
    "6M": 132,
    "1Y": 250,
    "2Y": 500,
}

VALUATION_RANGE_SIZE_MAP = {
    "1Y": 250,
    "3Y": 750,
    "5Y": 1250,
    "10Y": 2500,
}

TECHNICAL_LOOKBACK_EXTRA = 250
DEFAULT_BENCHMARK_CODE = "000300.SH"
PRICE_FIELDS = ("open", "high", "low", "close", "preClose")
VALID_ADJUST_TYPES = {"NONE", "FORWARD", "BACKWARD"}


class StockService:
    """股票 TargetDetail 服务。

    分层边界：
    - BasicDataRepo 只读 type_basic_data；
    - ExchangeDataRepo 只读 type_exchange_data；
    - FinanceDataRepo 只读 type_finance_data；
    - 本 service 负责跨库组合、字段转 camelCase、申万三级行业口径加工、
      技术指标与估值衍生数据计算，最终返回 targetDetailType.TargetDetail 结构。
    """

    build_default_watch_info = staticmethod(build_default_watch_info)
    compact_dict = staticmethod(compact_dict)
    metric_card = staticmethod(metric_card)
    net_flow = staticmethod(net_flow)
    strip_exchange = staticmethod(strip_exchange)

    def __init__(
        self,
        basic_conn: AsyncConnection,
        exchange_conn: AsyncConnection,
        finance_conn: AsyncConnection,
    ):
        self.basic_conn = basic_conn
        self.exchange_conn = exchange_conn
        self.finance_conn = finance_conn
        
        self.basic_repo = BasicDataRepo(basic_conn)
        self.exchange_repo = ExchangeDataRepo(exchange_conn)
        self.finance_repo = FinanceDataRepo(finance_conn)

    async def get_target_detail_shell(
        self,
        full_symbol: str,
        trade_date: str | None = None,
    ) -> Dict[str, Any]:
        """详情页首屏轻量接口。

        只返回顶部展示所需的基础信息、最新行情、指标卡、摘要和可用 Tab，
        不返回 quoteRows / valuationRows / financialDetail / moneyFlowRows 等大数组。
        """
        requested_trade_date = resolve_trade_date(trade_date)
        warnings: List[str] = []

        basic = await self.basic_repo.get_stock_basic_detail(full_symbol)
        resolved_full_symbol = (basic or {}).get("full_symbol") or full_symbol
        quote = await self.exchange_repo.get_stock_latest_snapshot(resolved_full_symbol, requested_trade_date)
        resolved_trade_date = to_str_date((quote or {}).get("trade_date")) or requested_trade_date

        if not basic:
            warnings.append("stock_basic_data 未找到股票基础信息")
        if not quote:
            warnings.append("stock_k_daily / stock_indicator_daily 未找到最新行情")

        indicator_history = await self.exchange_repo.get_stock_indicator_history(resolved_full_symbol, resolved_trade_date, limit=260)
        moneyflow_history = await self.exchange_repo.get_stock_moneyflow_history(resolved_full_symbol, resolved_trade_date, limit=260)
        financial_bundle = await self.finance_repo.get_latest_financial_bundle(resolved_full_symbol)
        industry_context = await self.load_industry_context(basic, resolved_full_symbol, resolved_trade_date)

        market_quote = self.build_market_quote(quote)
        valuation = self.build_valuation_info(quote, indicator_history)
        financial = self.build_financial_summary(financial_bundle)
        capital_flow = self.build_capital_flow_info(quote, moneyflow_history)
        industry = self.build_industry_info(industry_context)
        profile = self.build_target_profile(basic, resolved_full_symbol, industry_context)
        stock_profile = self.build_stock_profile(basic, resolved_full_symbol) if basic else None

        return self.drop_none_top_level({
            "targetProfile": profile,
            "quote": market_quote,
            "metricCards": self.build_metric_cards(quote, valuation, financial, capital_flow),
            "valuation": valuation,
            "financial": financial,
            "capitalFlow": capital_flow,
            "industry": industry,
            "watch": self.build_default_watch_info(),
            "alerts": [],
            "events": [],
            "relatedTargets": self.build_related_targets(industry_context),
            "stockProfile": stock_profile,
            "availableTabs": self.build_available_tabs(valuation, financial, capital_flow, industry),
            "meta": self.build_meta(resolved_trade_date, warnings),
        })

    async def get_target_overview_detail(
        self,
        full_symbol: str,
        trade_date: str | None = None,
    ) -> Dict[str, Any]:
        """标的详情的综合分析结论部分。当前仍以可解释规则和预留入口生成。"""
        requested_trade_date = resolve_trade_date(trade_date)
        basic = await self.basic_repo.get_stock_basic_detail(full_symbol)
        resolved_full_symbol = (basic or {}).get("full_symbol") or full_symbol
        quote = await self.exchange_repo.get_stock_latest_snapshot(resolved_full_symbol, requested_trade_date)
        resolved_trade_date = to_str_date((quote or {}).get("trade_date")) or requested_trade_date
        history = await self.exchange_repo.get_stock_history(resolved_full_symbol, resolved_trade_date, limit=260)
        indicator_history = await self.exchange_repo.get_stock_indicator_history(resolved_full_symbol, resolved_trade_date, limit=260)
        financial_bundle = await self.finance_repo.get_latest_financial_bundle(resolved_full_symbol)
        valuation = self.build_valuation_info(quote, indicator_history)
        financial = self.build_financial_summary(financial_bundle)
        warnings = [] if quote else ["stock_k_daily / stock_indicator_daily 未找到行情快照"]
        return {
            "fullSymbol": resolved_full_symbol,
            "overview": self.build_overview(quote, history, valuation, financial),
            "meta": self.build_meta(resolved_trade_date, warnings),
        }

    async def get_target_quote_detail(
        self,
        full_symbol: str,
        trade_date: str | None = None,
        history_limit: int | None = None,
        adjust_type: str = "NONE",
        benchmark_code: str | None = None,
        period: str = "DAILY",
        range_code: str | None = None,
        main_indicator_codes: Optional[Sequence[str]] = None,
        sub_indicator_codes: Optional[Sequence[str]] = None,
        industry_code: str | None = None,
    ) -> Dict[str, Any]:
        """获取行情 Tab 数据。

        行情历史先按复权口径处理，再统一计算技术指标。接口返回图表、技术分析和
        当前区间统计；页面不再依赖原始 quoteRows 重新计算金融指标。
        """
        requested_trade_date = resolve_trade_date(trade_date)
        limit = self.normalize_history_limit(history_limit, range_code, default=RANGE_SIZE_MAP["1Y"])
        fetch_limit = limit + TECHNICAL_LOOKBACK_EXTRA

        adjust_type = (adjust_type or "NONE").upper()
        if adjust_type not in VALID_ADJUST_TYPES:
            adjust_type = "NONE"

        basic = await self.basic_repo.get_stock_basic_detail(full_symbol)
        resolved_full_symbol = (basic or {}).get("full_symbol") or full_symbol
        quote = await self.exchange_repo.get_stock_latest_snapshot(resolved_full_symbol, requested_trade_date)
        resolved_trade_date = to_str_date((quote or {}).get("trade_date")) or requested_trade_date

        history = await self.exchange_repo.get_stock_history(
            resolved_full_symbol, resolved_trade_date, limit=fetch_limit
        )
        indicator_history = await self.exchange_repo.get_stock_indicator_history(
            resolved_full_symbol, resolved_trade_date, limit=fetch_limit
        )

        if adjust_type != "NONE":
            factors = await self.exchange_repo.get_stock_adjust_factors(resolved_full_symbol)
            history = self.apply_adjust_type_to_k_rows(history, factors, adjust_type)

        quote_rows_full = self.build_quote_rows(history, indicator_history)
        technical_rows_full = calculate_technical_indicator_rows(quote_rows_full) if quote_rows_full else []
        technical_rows = technical_rows_full[-limit:]

        quote_rows = [self.extract_market_quote_row(row) for row in technical_rows]
        latest_quote = quote_rows[-1] if quote_rows else self.build_market_quote(quote)

        charts = self.build_quote_market_charts(technical_rows)
        comparison = await self.build_quote_range_comparison_for_rows(
            quote_rows,
            benchmark_code or DEFAULT_BENCHMARK_CODE,
            resolved_trade_date,
            industry_code=industry_code,
        )
        range_performance = self.build_range_performance(quote_rows, comparison, range_code)

        warnings: List[str] = []
        if not quote_rows:
            warnings.append("stock_k_daily 未找到行情历史")
        if adjust_type != "NONE" and not history:
            warnings.append("stock_adjust_factor 未找到可用于复权的历史行情")

        return self.drop_none_top_level({
            "fullSymbol": resolved_full_symbol,
            "quote": latest_quote,
            "charts": charts,
            "quoteTechnicalAnalysis": self.build_quote_technical_analysis(
                technical_rows, resolved_trade_date
            ),
            "rangePerformance": range_performance,
            "adjustType": adjust_type,
            "meta": self.build_meta(resolved_trade_date, warnings),
        })

    async def get_target_valuation_summary_detail(
        self,
        full_symbol: str,
        trade_date: str | None = None,
        history_limit: int | None = None,
        range_code: str | None = None,
    ) -> Dict[str, Any]:
        """估值 Tab 顶部摘要接口。"""
        requested_trade_date = resolve_trade_date(trade_date)
        limit = self.normalize_valuation_history_limit(history_limit, range_code, default=VALUATION_RANGE_SIZE_MAP["1Y"])
        basic = await self.basic_repo.get_stock_basic_detail(full_symbol)
        resolved_full_symbol = (basic or {}).get("full_symbol") or full_symbol
        quote = await self.exchange_repo.get_stock_latest_snapshot(resolved_full_symbol, requested_trade_date)
        resolved_trade_date = to_str_date((quote or {}).get("trade_date")) or requested_trade_date
        indicator_history = await self.exchange_repo.get_stock_indicator_history(resolved_full_symbol, resolved_trade_date, limit=limit)

        return self.drop_none_top_level({
            "fullSymbol": resolved_full_symbol,
            "valuation": self.build_valuation_info(quote, indicator_history),
            "meta": self.build_meta(resolved_trade_date, [] if indicator_history else ["stock_indicator_daily 未找到估值历史"]),
        })

    async def get_target_valuation_channel_detail(
        self,
        full_symbol: str,
        trade_date: str | None = None,
        metric_code: str = "PE_TTM",
        history_limit: int | None = None,
        range_code: str | None = None,
    ) -> Dict[str, Any]:
        """估值通道接口。"""
        requested_trade_date = resolve_trade_date(trade_date)
        limit = self.normalize_valuation_history_limit(history_limit, range_code, default=VALUATION_RANGE_SIZE_MAP["1Y"])
        metric_code = self.normalize_metric_code(metric_code)
        basic = await self.basic_repo.get_stock_basic_detail(full_symbol)
        resolved_full_symbol = (basic or {}).get("full_symbol") or full_symbol
        quote = await self.exchange_repo.get_stock_latest_snapshot(resolved_full_symbol, requested_trade_date)
        resolved_trade_date = to_str_date((quote or {}).get("trade_date")) or requested_trade_date
        indicator_history = await self.exchange_repo.get_stock_indicator_history(resolved_full_symbol, resolved_trade_date, limit=limit)

        rows = self.build_valuation_channel_rows(
            indicator_history,
            metric_code,
            history_limit=limit,
        )
        return {
            "fullSymbol": resolved_full_symbol,
            "metricCode": metric_code,
            "channelRows": rows,
            "meta": self.build_meta(resolved_trade_date, [] if rows else ["未生成估值通道数据"]),
        }

    async def get_target_valuation_trend_detail(
        self,
        full_symbol: str,
        trade_date: str | None = None,
        metric_code: str = "PE_TTM",
        history_limit: int | None = None,
        range_code: str | None = None,
    ) -> Dict[str, Any]:
        """估值趋势接口。

        对外参数和返回结构保持不变。内部只计算指定 metric_code 的摘要和趋势，
        行业统计也只聚合该指标所需字段。
        """
        requested_trade_date = resolve_trade_date(trade_date)
        limit = self.normalize_valuation_history_limit(history_limit, range_code, default=VALUATION_RANGE_SIZE_MAP["1Y"])
        metric_code = self.normalize_metric_code(metric_code)
        basic = await self.basic_repo.get_stock_basic_detail(full_symbol)
        resolved_full_symbol = (basic or {}).get("full_symbol") or full_symbol
        quote = await self.exchange_repo.get_stock_latest_snapshot(resolved_full_symbol, requested_trade_date)
        resolved_trade_date = to_str_date((quote or {}).get("trade_date")) or requested_trade_date
        indicator_history = await self.exchange_repo.get_stock_indicator_history(resolved_full_symbol, resolved_trade_date, limit=limit)

        metric_field = VALUATION_METRICS[metric_code]["field"]
        industry_stats = await self.load_industry_valuation_stats(
            basic,
            resolved_full_symbol,
            resolved_trade_date,
            indicator_history,
            limit,
            fields=(metric_field,),
        )

        metric_summaries = self.build_valuation_metric_summaries(
            indicator_history,
            industry_stats,
            history_limit=limit,
            metric_codes=(metric_code,),
        )
        trend_rows = self.build_valuation_trend_rows(
            indicator_history,
            industry_stats,
            metric_code
        )

        return {
            "fullSymbol": resolved_full_symbol,
            "metricCode": metric_code,
            "metricSummaries": metric_summaries,
            "trendRows": trend_rows,
            "meta": self.build_meta(resolved_trade_date, [] if indicator_history else ["stock_indicator_daily 未找到估值历史"]),
        }


    async def get_target_dividend_trend_detail(
        self,
        full_symbol: str,
        trade_date: str | None = None,
        range_code: str | None = "1Y",
        history_limit: int | None = None,
    ) -> Dict[str, Any]:
        """分红 TTM 趋势接口。

        对外参数和返回结构保持不变。内部只加载 dividendRows 所需的 dv_ttm
        行业统计，避免额外聚合 PE/PB/PS。
        """
        requested_trade_date = resolve_trade_date(trade_date)
        limit = self.normalize_valuation_history_limit(history_limit, range_code, default=VALUATION_RANGE_SIZE_MAP["1Y"])
        basic = await self.basic_repo.get_stock_basic_detail(full_symbol)
        resolved_full_symbol = (basic or {}).get("full_symbol") or full_symbol
        quote = await self.exchange_repo.get_stock_latest_snapshot(resolved_full_symbol, requested_trade_date)
        resolved_trade_date = to_str_date((quote or {}).get("trade_date")) or requested_trade_date
        indicator_history = await self.exchange_repo.get_stock_indicator_history(resolved_full_symbol, resolved_trade_date, limit=limit)

        industry_stats = await self.load_industry_valuation_stats(
            basic,
            resolved_full_symbol,
            resolved_trade_date,
            indicator_history,
            limit,
            fields=("dv_ttm",),
        )
        rows = self.build_dividend_trend_rows(
            indicator_history,
            industry_stats,
        )
        return {
            "fullSymbol": resolved_full_symbol,
            "dividendRows": rows,
            "meta": self.build_meta(resolved_trade_date, [] if rows else ["stock_indicator_daily 未找到分红历史"]),
        }

    async def get_target_financial_detail(
        self,
        full_symbol: str,
        history_limit: int | None = None,
    ) -> Dict[str, Any]:
        """财务 Tab 接口。history_limit 表示向前展示的年数。"""
        row_limit = self.normalize_financial_row_limit(history_limit)
        basic = await self.basic_repo.get_stock_basic_detail(full_symbol)
        resolved_full_symbol = (basic or {}).get("full_symbol") or full_symbol
        financial_bundle = await self.finance_repo.get_latest_financial_bundle(resolved_full_symbol)
        detail_bundle = await self.finance_repo.get_financial_statement_detail(resolved_full_symbol, limit=row_limit)
        financial = self.build_financial_summary(financial_bundle)
        financial_detail = self.build_financial_detail(detail_bundle)
        data_version = (financial or {}).get("endDate")
        return self.drop_none_top_level({
            "fullSymbol": resolved_full_symbol,
            "financial": financial,
            "financialDetail": financial_detail,
            "meta": self.build_meta(data_version, [] if financial_detail else ["type_finance_data 未找到财务详情"]),
        })

    async def get_target_capital_flow_detail(
        self,
        full_symbol: str,
        trade_date: str | None = None,
        history_limit: int | None = None,
        range_code: str | None = "3M",
        include_quote_rows: bool = True,
    ) -> Dict[str, Any]:
        """资金流 Tab 接口。"""
        requested_trade_date = resolve_trade_date(trade_date)
        limit = self.normalize_history_limit(history_limit, range_code, default=RANGE_SIZE_MAP["3M"])
        basic = await self.basic_repo.get_stock_basic_detail(full_symbol)
        resolved_full_symbol = (basic or {}).get("full_symbol") or full_symbol
        quote = await self.exchange_repo.get_stock_latest_snapshot(resolved_full_symbol, requested_trade_date)
        resolved_trade_date = to_str_date((quote or {}).get("trade_date")) or requested_trade_date
        moneyflow_history = await self.exchange_repo.get_stock_moneyflow_history(resolved_full_symbol, resolved_trade_date, limit=limit)
        money_flow_rows = self.build_money_flow_rows(moneyflow_history)
        close_rows = await self.exchange_repo.get_stock_history(resolved_full_symbol, resolved_trade_date, limit=limit) if include_quote_rows else []
        close_map = {to_str_date(row.get("trade_date")): round_or_none(row.get("close")) for row in close_rows}
        close = [close_map.get(row.get("tradeDate", '')) for row in money_flow_rows] if include_quote_rows else None
        return self.drop_none_top_level({
            "fullSymbol": resolved_full_symbol,
            "capitalFlow": self.build_capital_flow_info(quote, moneyflow_history),
            "moneyFlowRows": money_flow_rows,
            "close": close,
            "meta": self.build_meta(resolved_trade_date, [] if money_flow_rows else ["stock_moneyflow_daily 未找到资金流历史"]),
        })

    async def get_target_industry_detail(
        self,
        full_symbol: str,
        trade_date: str | None = None,
    ) -> Dict[str, Any]:
        """行业 Tab 接口。股票使用申万三级行业；指数返回指数基础资料与成分。"""
        requested_trade_date = resolve_trade_date(trade_date)
        stock_basic = await self.basic_repo.get_stock_basic_detail(full_symbol)
        if stock_basic:
            resolved_full_symbol = stock_basic.get("full_symbol") or full_symbol
            quote = await self.exchange_repo.get_stock_latest_snapshot(resolved_full_symbol, requested_trade_date)
            resolved_trade_date = to_str_date((quote or {}).get("trade_date")) or requested_trade_date
            context = await self.load_industry_context(stock_basic, resolved_full_symbol, resolved_trade_date)
            return self.drop_none_top_level({
                "fullSymbol": resolved_full_symbol,
                "industry": self.build_industry_info(context),
                "stockProfile": self.build_stock_profile(stock_basic, resolved_full_symbol),
                "industryPeerRows": self.build_industry_peer_rows(context),
                "industryDetail": context.get("industryDetail"),
                "meta": self.build_meta(resolved_trade_date, [] if context.get("industryDetail") else ["未找到申万行业归属"]),
            })

        index_basic = await self.basic_repo.get_index_basic_detail(full_symbol)
        resolved_full_symbol = (index_basic or {}).get("full_symbol") or full_symbol
        quote = await self.exchange_repo.get_index_latest_snapshot(resolved_full_symbol, requested_trade_date)
        resolved_trade_date = to_str_date((quote or {}).get("trade_date")) or requested_trade_date
        constituent_rows = await self.build_constituent_rows(resolved_full_symbol, resolved_trade_date, limit=200)
        return self.drop_none_top_level({
            "fullSymbol": resolved_full_symbol,
            "industry": self.build_industry_info_from_constituents(constituent_rows),
            "indexProfile": self.build_index_profile(index_basic, resolved_full_symbol) if index_basic else None,
            "constituentRows": constituent_rows,
            "industryPeerRows": constituent_rows,
            "industryDetail": None,
            "meta": self.build_meta(resolved_trade_date, [] if constituent_rows or index_basic else ["未找到指数行业/成分数据"]),
        })

    def normalize_history_limit(self, history_limit: int | None, range_code: str | None, default: int) -> int:
        if history_limit is not None:
            return max(1, min(5000, int(history_limit)))
        code = (range_code or "").upper()
        return RANGE_SIZE_MAP.get(code, default)

    def normalize_valuation_history_limit(self, history_limit: int | None, range_code: str | None, default: int) -> int:
        if history_limit is not None:
            return max(1, min(5000, int(history_limit)))
        code = (range_code or "").upper()
        return VALUATION_RANGE_SIZE_MAP.get(code, default)

    def normalize_financial_row_limit(self, history_limit: int | None) -> int:
        if history_limit is None:
            return 20
        # 年数 -> 季度报告行数，额外加 4 期用于同比或缺报兜底。
        return max(4, min(80, int(history_limit) * 4 + 4))

    def normalize_metric_code(self, metric_code: str | None) -> str:
        code = (metric_code or "PE_TTM").upper()
        return code if code in VALUATION_METRICS else "PE_TTM"

    def build_meta(self, data_version: Any = None, warnings: Optional[List[str]] = None) -> Dict[str, Any]:
        warning_list = [item for item in (warnings or []) if item]
        meta = {
            "dataSource": "type_basic_data,type_exchange_data,type_finance_data",
            "dataVersion": to_str_date(data_version) if data_version else None,
            "generatedAt": get_current_datetime().isoformat(),
            "partialData": bool(warning_list),
        }
        if warning_list:
            meta["warnings"] = warning_list
        return meta

    def build_available_tabs(
        self,
        valuation: Optional[Dict[str, Any]],
        financial: Optional[Dict[str, Any]],
        capital_flow: Optional[Dict[str, Any]],
        industry: Optional[Dict[str, Any]],
    ) -> List[str]:
        tabs = ["QUOTE"]
        if valuation:
            tabs.append("VALUATION")
        if financial:
            tabs.append("FINANCIAL")
        if capital_flow:
            tabs.append("CAPITAL_FLOW")
        if industry:
            tabs.append("INDUSTRY")
        return tabs

    async def load_industry_valuation_stats(
        self,
        basic: Optional[Dict[str, Any]],
        full_symbol: str,
        trade_date: str,
        indicator_history: List[Dict[str, Any]],
        limit: int,
        fields: Optional[Sequence[str]] = None,
    ) -> Dict[str, Dict[str, Optional[float]]]:
        """加载并聚合申万三级行业估值。

        fields 为内部优化参数：
        - None：保持旧行为，计算 PE_TTM / PB / PS_TTM / DV_TTM 全部统计；
        - 指定字段：只聚合当前接口真正需要的估值字段。

        该参数仅影响 service 内部计算，不改变任何对外接口。
        """
        context = await self.load_industry_context(basic, full_symbol, trade_date)
        codes = [item.get("stock_code") for item in context.get("benchmarkComponents", []) if item.get("stock_code")]
        rows = await self.exchange_repo.get_stock_indicator_rows_by_codes(
            codes,
            trade_date,
            limit_trade_dates=min(limit, max(1, len(indicator_history) or limit)),
        ) if codes else []
        return self.build_industry_valuation_stats(rows, fields=fields)

    def apply_adjust_type_to_k_rows(
        self,
        history_rows_desc: List[Dict[str, Any]],
        factor_rows_asc: List[Dict[str, Any]],
        adjust_type: str,
    ) -> List[Dict[str, Any]]:
        """根据 stock_adjust_factor 对 K 线价格做前/后复权。

        因子表每条 trade_date 表示该因子有效的最后一天，因此某日因子取
        第一条 trade_date >= 该日的记录；超出区间时取最后一条记录兜底。
        """
        if adjust_type == "NONE" or not history_rows_desc or not factor_rows_asc:
            return [dict(row) for row in history_rows_desc]

        factors = []
        for row in factor_rows_asc:
            factor = to_float_or_none(row.get("adjust_factor"))
            factor_date = to_str_date(row.get("trade_date"))
            if factor is not None and factor_date:
                factors.append((factor_date, factor))
        if not factors:
            return [dict(row) for row in history_rows_desc]

        latest_trade_date = to_str_date(history_rows_desc[0].get("trade_date"))
        latest_factor = self.factor_for_trade_date(latest_trade_date, factors) or factors[-1][1]
        if not latest_factor:
            return [dict(row) for row in history_rows_desc]

        adjusted: List[Dict[str, Any]] = []
        for row in history_rows_desc:
            item = dict(row)
            trade_date = to_str_date(item.get("trade_date"))
            factor = self.factor_for_trade_date(trade_date, factors)
            if factor:
                if adjust_type == "FORWARD":
                    ratio = factor / latest_factor
                else:  # BACKWARD
                    ratio = latest_factor / factor
                for field in ["open", "high", "low", "close", "pre_close"]:
                    value = to_float_or_none(item.get(field))
                    if value is not None:
                        item[field] = value * ratio
                close = to_float_or_none(item.get("close"))
                pre_close = to_float_or_none(item.get("pre_close"))
                if close is not None and pre_close:
                    item["chg_amt"] = close - pre_close
                    item["chg_pct"] = (close - pre_close) / pre_close * 100
            item["adjust_factor"] = factor
            adjusted.append(item)
        return adjusted

    def factor_for_trade_date(self, trade_date: Optional[str], factors: Sequence[Tuple[str, float]]) -> Optional[float]:
        if not trade_date or not factors:
            return None
        for end_date, factor in factors:
            if trade_date <= end_date:
                return factor
        return factors[-1][1]

    def extract_market_quote_row(self, row: Mapping[str, Any]) -> Dict[str, Any]:
        return self.compact_dict({
            "fullSymbol": row.get("fullSymbol") or row.get("full_symbol"),
            "tradeDate": row.get("tradeDate") or to_str_date(row.get("trade_date")),
            "tradeTime": row.get("tradeTime"),
            "marketStatus": row.get("marketStatus") or "CLOSED",
            "open": round_or_none(row.get("open")),
            "high": round_or_none(row.get("high")),
            "low": round_or_none(row.get("low")),
            "close": round_or_none(row.get("close")),
            "preClose": round_or_none(row.get("preClose") or row.get("pre_close")),
            "chgAmt": round_or_none(row.get("chgAmt") or row.get("chg_amt")),
            "chgPct": round_or_none(row.get("chgPct") or row.get("chg_pct")),
            "amplitude": round_or_none(row.get("amplitude")),
            "volume": round_or_none(row.get("volume"), 2),
            "turnover": round_or_none(row.get("turnover"), 2),
            "turnoverRate": round_or_none(row.get("turnoverRate") or row.get("turnover_rate")),
            "adjustFactor": round_or_none(row.get("adjustFactor") or row.get("adjust_factor")),
            "priceUnit": row.get("priceUnit") or "元",
        })


    def build_range_performance(
        self,
        quote_rows: List[Dict[str, Any]],
        comparison: Dict[str, Optional[float] | Optional[str]],
        range_code: str | None = None,
    ) -> Optional[Dict[str, Any]]:
        if not quote_rows:
            return None
        first, latest = quote_rows[0], quote_rows[-1]
        highs = [v for v in (to_float_or_none(row.get("high")) for row in quote_rows) if v is not None]
        lows = [v for v in (to_float_or_none(row.get("low")) for row in quote_rows) if v is not None]
        closes = [v for v in (to_float_or_none(row.get("close")) for row in quote_rows) if v is not None]
        volumes = [to_float_or_none(row.get("volume")) for row in quote_rows]
        turnovers = [to_float_or_none(row.get("turnover")) for row in quote_rows]
        turnover_rates = [to_float_or_none(row.get("turnoverRate")) for row in quote_rows]
        valid_volumes = [v for v in volumes if v is not None]
        valid_turnovers = [v for v in turnovers if v is not None]
        valid_turnover_rates = [v for v in turnover_rates if v is not None]

        high = max(highs) if highs else None
        low = min(lows) if lows else None
        latest_close = to_float_or_none(latest.get("close"))
        base = to_float_or_none(first.get("preClose")) or to_float_or_none(first.get("close"))
        change = self.calculate_rows_return(quote_rows)
        max_drawdown = max_drawdown_from_rows(quote_rows)
        volatility = annualized_volatility_from_pct([row.get("chgPct") for row in quote_rows])
        years = max(len(quote_rows) / 252, 1 / 252)
        annualized_return = None
        if base and latest_close is not None and latest_close > 0:
            annualized_return = ((latest_close / base) ** (1 / years) - 1) * 100
        sharpe_ratio = annualized_return / volatility if annualized_return is not None and volatility not in (None, 0) else None
        calmar_ratio = annualized_return / abs(max_drawdown) if annualized_return is not None and max_drawdown not in (None, 0) else None

        max_volume, max_volume_date = self.max_value_and_date(quote_rows, "volume")
        max_turnover, max_turnover_date = self.max_value_and_date(quote_rows, "turnover")
        new_high_count, new_low_count = self.count_new_high_low(quote_rows)
        price_position = (latest_close - low) / (high - low) * 100 if latest_close is not None and high is not None and low is not None and high != low else None
        drawdown_from_high = (latest_close - high) / high * 100 if latest_close is not None and high else None
        rebound_from_low = (latest_close - low) / low * 100 if latest_close is not None and low else None

        return self.compact_dict({
            "rangeCode": range_code,
            "startDate": first.get("tradeDate"),
            "endDate": latest.get("tradeDate"),
            "returnRisk": self.compact_dict({
                "changePercent": round_or_none(change),
                "maxDrawdown": round_or_none(max_drawdown),
                "volatility": round_or_none(volatility),
                "rankingText": None,
                "high": round_or_none(high),
                "low": round_or_none(low),
                "amplitude": round_or_none((high - low) / base * 100 if high is not None and low is not None and base else None),
                "annualizedReturn": round_or_none(annualized_return),
                "sharpeRatio": round_or_none(sharpe_ratio),
                "calmarRatio": round_or_none(calmar_ratio),
            }),
            "volume": self.compact_dict({
                "volume": round_or_none(sum(valid_volumes), 2) if valid_volumes else None,
                "turnover": round_or_none(sum(valid_turnovers), 2) if valid_turnovers else None,
                "turnoverRate": round_or_none(sum(valid_turnover_rates), 4) if valid_turnover_rates else None,
                "avgVolume": round_or_none(statistics.fmean(valid_volumes), 2) if valid_volumes else None,
                "avgTurnover": round_or_none(statistics.fmean(valid_turnovers), 2) if valid_turnovers else None,
                "avgTurnoverRate": round_or_none(statistics.fmean(valid_turnover_rates), 4) if valid_turnover_rates else None,
                "maxVolume": round_or_none(max_volume, 2),
                "maxVolumeTradeDate": max_volume_date,
                "maxTurnover": round_or_none(max_turnover, 2),
                "maxTurnoverTradeDate": max_turnover_date,
            }),
            "comparison": comparison,
            "priceStructure": self.compact_dict({
                "open": round_or_none(first.get("open")),
                "high": round_or_none(high),
                "low": round_or_none(low),
                "close": round_or_none(latest_close),
                "preClose": round_or_none(base),
                "pricePositionPercent": round_or_none(price_position),
                "drawdownFromHigh": round_or_none(drawdown_from_high),
                "reboundFromLow": round_or_none(rebound_from_low),
                "newHighCount": new_high_count,
                "newLowCount": new_low_count,
                "priceRangeText": f"{round_or_none(low)} - {round_or_none(high)}" if high is not None and low is not None else None,
                "turnoverRangeText": f"{round_or_none(min(valid_turnovers), 2)} - {round_or_none(max(valid_turnovers), 2)}" if valid_turnovers else None,
            }),
        })

    async def build_quote_range_comparison_for_rows(
        self,
        quote_rows: List[Dict[str, Any]],
        benchmark_code: str | None,
        trade_date: str,
        industry_code: str | None = None,
    ) -> Dict[str, Optional[float] | Optional[str]]:
        stock_return = self.calculate_rows_return(quote_rows)
        market_return = None
        if benchmark_code and quote_rows:
            benchmark_history = await self.exchange_repo.get_index_history(benchmark_code, trade_date, limit=len(quote_rows))
            benchmark_rows = list(reversed(benchmark_history))
            market_return = self.calculate_k_rows_return(benchmark_rows)
        alpha_beta = calculate_alpha_beta_placeholder()
        market_excess = stock_return - market_return if stock_return is not None and market_return is not None else None
        return {
            "marketExcessReturn": round_or_none(market_excess),
            "industryExcessReturn": None,
            "industryRankText": None,
            "marketRankText": None,
            "alpha": alpha_beta.get("alpha"),
            "beta": alpha_beta.get("beta"),
            "correlation": alpha_beta.get("correlation"),
            "relativeStrengthScore": round_or_none(max(0, min(100, 50 + (market_excess or 0)))) if market_excess is not None else None,
        }

    def calculate_rows_return(self, rows: Sequence[Mapping[str, Any]]) -> Optional[float]:
        if len(rows) < 2:
            return None
        first, latest = rows[0], rows[-1]
        base = to_float_or_none(first.get("preClose") or first.get("pre_close") or first.get("close"))
        latest_close = to_float_or_none(latest.get("close"))
        if not base or latest_close is None:
            return None
        return (latest_close - base) / base * 100

    def calculate_k_rows_return(self, rows: Sequence[Mapping[str, Any]]) -> Optional[float]:
        return self.calculate_rows_return(rows)

    def max_value_and_date(self, rows: Sequence[Mapping[str, Any]], field: str) -> Tuple[Optional[float], Optional[str]]:
        best_value: Optional[float] = None
        best_date: Optional[str] = None
        for row in rows:
            value = to_float_or_none(row.get(field))
            if value is not None and (best_value is None or value > best_value):
                best_value = value
                best_date = row.get("tradeDate") or to_str_date(row.get("trade_date"))
        return best_value, best_date

    def count_new_high_low(self, rows: Sequence[Mapping[str, Any]]) -> Tuple[int, int]:
        high_mark: Optional[float] = None
        low_mark: Optional[float] = None
        new_high_count = 0
        new_low_count = 0
        for row in rows:
            high = to_float_or_none(row.get("high"))
            low = to_float_or_none(row.get("low"))
            if high is not None and (high_mark is None or high >= high_mark):
                if high_mark is not None:
                    new_high_count += 1
                high_mark = high
            if low is not None and (low_mark is None or low <= low_mark):
                if low_mark is not None:
                    new_low_count += 1
                low_mark = low
        return new_high_count, new_low_count

    def build_index_profile(self, row: Optional[Dict[str, Any]], fallback_symbol: str) -> Dict[str, Any]:
        row = row or {"full_symbol": fallback_symbol}
        profile = self.camelize_row(row, date_fields={"base_date", "list_date", "exp_date"})
        market = row.get("market")
        profile["marketName"] = market
        tags = [value for value in [row.get("market"), row.get("category"), row.get("index_type")] if value]
        profile["tags"] = tags
        return self.compact_dict(profile)

    async def build_constituent_rows(self, index_code: str, trade_date: str, limit: int = 200) -> List[Dict[str, Any]]:
        components = await self.basic_repo.get_index_components(index_code, limit=limit, trade_date=trade_date)
        codes = [row.get("stock_code", '') for row in components if row.get("stock_code")]
        basic_map = await self.basic_repo.get_stock_basic_by_codes(codes) if codes else {}
        snapshots = await self.exchange_repo.get_stock_latest_snapshots_by_codes(codes, trade_date) if codes else []
        snapshot_map = {row.get("full_symbol"): row for row in snapshots if row.get("full_symbol")}
        memberships = await self.basic_repo.get_stock_sw_industry_memberships_by_codes(codes) if codes else []
        sw3_map = {row.get("stock_code"): row for row in memberships if row.get("stock_code")}
        rows: List[Dict[str, Any]] = []
        for item in components:
            code = item.get("stock_code", '')
            basic = basic_map.get(code, {})
            snap = snapshot_map.get(code, {})
            sw3 = sw3_map.get(code, {})
            rows.append(self.compact_dict({
                "indexCode": item.get("index_code"),
                "stockCode": code,
                "tradeDate": to_str_date(item.get("trade_date") or snap.get("trade_date")),
                "weight": round_or_none(item.get("weight")),
                "name": basic.get("name"),
                "industry": basic.get("industry"),
                "industryL1": None,
                "industryL2": None,
                "industryL3": sw3.get("industry_name"),
                "close": round_or_none(snap.get("close")),
                "chgPct": round_or_none(snap.get("chg_pct")),
                "totalMarketValue": round_or_none(snap.get("total_market_value"), 2),
                "floatMarketValue": round_or_none(snap.get("float_market_value"), 2),
                "peTtm": round_or_none(snap.get("pe_ttm")),
                "pb": round_or_none(snap.get("pb")),
                "psTtm": round_or_none(snap.get("ps_ttm")),
                "turnoverRate": round_or_none(snap.get("turnover_rate")),
            }))
        return rows

    def build_industry_info_from_constituents(self, rows: List[Dict[str, Any]]) -> Optional[Dict[str, Any]]:
        if not rows:
            return None
        chg_values = [to_float_or_none(row.get("chgPct")) for row in rows]
        valid_chg = [v for v in chg_values if v is not None]
        pe_values = [v for v in (to_float_or_none(row.get("peTtm")) for row in rows) if v is not None]
        pb_values = [v for v in (to_float_or_none(row.get("pb")) for row in rows) if v is not None]
        return self.compact_dict({
            "industryCode": None,
            "industryName": None,
            "industryLevel": None,
            "stockCount": len(rows),
            "upCount": sum(1 for v in valid_chg if v > 0),
            "downCount": sum(1 for v in valid_chg if v < 0),
            "flatCount": sum(1 for v in valid_chg if v == 0),
            "avgChangePercent": round_or_none(statistics.fmean(valid_chg) if valid_chg else None),
            "peMedian": round_or_none(statistics.median(pe_values) if pe_values else None),
            "pbMedian": round_or_none(statistics.median(pb_values) if pb_values else None),
        })

    async def get_stock_detail(
        self,
        full_symbol: str,
        trade_date: str | None = None,
        include_charts: bool = True,
        include_financial: bool = True,
        include_history: bool = True,
    ) -> Dict[str, Any]:
        """构建单只股票详情，返回 targetDetailType.TargetDetail。"""
        logger.info("查询股票 TargetDetail，full_symbol=%s, trade_date=%s", full_symbol, trade_date)

        warnings: List[str] = []
        basic = await self.basic_repo.get_stock_basic_detail(full_symbol)
        resolved_full_symbol = (basic or {}).get("full_symbol") or full_symbol

        requested_trade_date = resolve_trade_date(trade_date)
        quote = await self.exchange_repo.get_stock_latest_snapshot(resolved_full_symbol, requested_trade_date)
        resolved_trade_date = to_str_date((quote or {}).get("trade_date")) or requested_trade_date

        if not basic:
            warnings.append("stock_basic_data 未找到股票基础信息")
        if not quote:
            warnings.append("stock_k_daily / stock_indicator_daily 未找到行情快照")

        history_limit = 520 if include_history else 120
        indicator_limit = 1250 if include_history else 260
        history = await self.exchange_repo.get_stock_history(resolved_full_symbol, resolved_trade_date, limit=history_limit)
        indicator_history = await self.exchange_repo.get_stock_indicator_history(
            resolved_full_symbol,
            resolved_trade_date,
            limit=indicator_limit,
        )
        moneyflow_history = await self.exchange_repo.get_stock_moneyflow_history(
            resolved_full_symbol,
            resolved_trade_date,
            limit=indicator_limit,
        )

        financial_bundle: Dict[str, Optional[Dict[str, Any]]] = {}
        financial_detail_bundle: Dict[str, List[Dict[str, Any]]] = {}
        financial_history: List[Dict[str, Any]] = []
        if include_financial:
            financial_bundle = await self.finance_repo.get_latest_financial_bundle(resolved_full_symbol)
            financial_detail_bundle = await self.finance_repo.get_financial_statement_detail(resolved_full_symbol, limit=20)
            financial_history = await self.finance_repo.get_financial_history(resolved_full_symbol, limit=12)
            if not any(financial_bundle.values()):
                warnings.append("type_finance_data 未找到财务数据")

        industry_context = await self.load_industry_context(basic, resolved_full_symbol, resolved_trade_date)
        benchmark_codes = [item.get("stock_code") for item in industry_context.get("benchmarkComponents", []) if item.get("stock_code")]
        industry_indicator_rows = await self.exchange_repo.get_stock_indicator_rows_by_codes(
            benchmark_codes,
            resolved_trade_date,
            limit_trade_dates=len(indicator_history),
        ) if benchmark_codes else []
        industry_valuation_stats = self.build_industry_valuation_stats(industry_indicator_rows)

        quote_rows = self.build_quote_rows(history, indicator_history)
        technical_rows = calculate_technical_indicator_rows(quote_rows) if quote_rows else []
        valuation_rows = self.build_valuation_rows(indicator_history, industry_valuation_stats)
        money_flow_rows = self.build_money_flow_rows(moneyflow_history)
        financial_detail = self.build_financial_detail(financial_detail_bundle) if include_financial else None

        valuation_detail = self.build_valuation_detail(indicator_history, industry_valuation_stats)
        valuation = self.build_valuation_info(quote, indicator_history)
        financial = self.build_financial_summary(financial_bundle) if include_financial else None
        capital_flow = self.build_capital_flow_info(quote, moneyflow_history)
        industry = self.build_industry_info(industry_context)
        industry_peer_rows = self.build_industry_peer_rows(industry_context)
        profile = self.build_target_profile(basic, resolved_full_symbol, industry_context)
        stock_profile = self.build_stock_profile(basic, resolved_full_symbol)
        market_quote = self.build_market_quote(quote)
        overview = self.build_overview(quote, history, valuation, financial)
        metric_cards = self.build_metric_cards(quote, valuation, financial, capital_flow)
        charts = self.build_charts(technical_rows, valuation_rows, money_flow_rows, financial_detail, financial_history) if include_charts else []
        related_targets = self.build_related_targets(industry_context)

        target_detail = {
            "targetProfile": profile,
            "quote": market_quote,
            "overview": overview,
            "metricCards": metric_cards,
            "features": {
                "quote": market_quote is not None,
                "valuation": valuation is not None,
                "financial": financial is not None,
                "capitalFlow": capital_flow is not None,
                "industry": industry is not None,
            },
            "charts": charts,
            "valuation": valuation,
            "financial": financial,
            "capitalFlow": capital_flow,
            "industry": industry,
            "constituents": None,
            "watch": self.build_default_watch_info(),
            "alerts": [],
            "events": [],
            "relatedTargets": related_targets,
            # 详情页扩展载荷
            "quoteTechnicalAnalysis": self.build_quote_technical_analysis(technical_rows, resolved_trade_date),
            "valuationRows": valuation_rows,
            "valuationDetail": valuation_detail,
            "financialDetail": financial_detail,
            "moneyFlowRows": money_flow_rows,
            "stockProfile": stock_profile,
            "industryPeerRows": industry_peer_rows,
            "industryDetail": industry_context.get("industryDetail"),
            "meta": {
                "dataSource": "type_basic_data,type_exchange_data,type_finance_data",
                "dataVersion": resolved_trade_date,
                "generatedAt": get_current_datetime().isoformat(),
                "partialData": bool(warnings),
                # TargetDetailMeta 未声明 warnings，但保留扩展字段，便于排查数据缺口。
                "warnings": warnings,
            },
        }
        return self.drop_none_top_level(target_detail)

    # ------------------------------------------------------------------
    # 基础资料 / 行情 / 顶部概览
    # ------------------------------------------------------------------

    def build_target_profile(
        self,
        row: Optional[Dict[str, Any]],
        fallback_symbol: str,
        industry_context: Optional[Dict[str, Any]] = None,
    ) -> Dict[str, Any]:
        """构建 TargetProfile。行业展示优先使用申万三级行业名称。"""
        row = row or {}
        industry_context = industry_context or {}
        exchange = row.get("exchange") or self.infer_exchange(fallback_symbol)
        industry_detail = industry_context.get("industryDetail") or {}
        industry_name = industry_detail.get("benchmarkIndustryName") or row.get("industry")
        return self.compact_dict({
            "fullSymbol": row.get("full_symbol") or fallback_symbol,
            "symbol": row.get("symbol") or self.strip_exchange(fallback_symbol),
            "name": row.get("name") or row.get("full_name") or fallback_symbol,
            "targetType": "STOCK",
            "marketCode": exchange,
            "marketTypeName": row.get("market_type"),
            "exchangeName": EXCHANGE_NAME_MAP.get(str(exchange or "").upper(), exchange),
            "industryName": industry_name,
            "currencyType": row.get("currency_type"),
            "tags": self.build_profile_tags(row, industry_name),
        })

    def build_stock_profile(self, row: Optional[Dict[str, Any]], fallback_symbol: str) -> Dict[str, Any]:
        """构建 StockProfile 明细，字段来自 stock_basic_data。"""
        row = row or {"full_symbol": fallback_symbol}
        return self.camelize_row(row, date_fields={"list_date", "delist_date"})

    def build_market_quote(self, row: Optional[Dict[str, Any]], price_unit: str = "元") -> Optional[Dict[str, Any]]:
        """根据最新行情 quote 构建首页展示的 MarketQuote。"""
        if not row:
            return None
        pre_close = to_float_or_none(row.get("pre_close"))
        high = to_float_or_none(row.get("high"))
        low = to_float_or_none(row.get("low"))
        amplitude = None
        if pre_close and high is not None and low is not None:
            amplitude = (high - low) / pre_close * 100
        return self.compact_dict({
            "fullSymbol": row.get("full_symbol"),
            "tradeDate": to_str_date(row.get("trade_date")),
            "marketStatus": "CLOSED",
            "open": round_or_none(row.get("open")),
            "high": round_or_none(row.get("high")),
            "low": round_or_none(row.get("low")),
            "close": round_or_none(row.get("close")),
            "preClose": round_or_none(row.get("pre_close")),
            "chgAmt": round_or_none(row.get("chg_amt")),
            "chgPct": round_or_none(row.get("chg_pct")),
            "amplitude": round_or_none(amplitude),
            "volume": round_or_none(row.get("volume"), 2),
            "turnover": round_or_none(row.get("turnover"), 2),
            "turnoverRate": round_or_none(row.get("turnover_rate")),
            "adjustFactor": round_or_none(row.get("adjust_factor")),
            "priceUnit": price_unit,
        })

    def build_quote_rows(self, history: List[Dict[str, Any]], indicator_history: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """构建 quoteRows，历史 K 线与每日指标在 service 层按日期合并。"""
        indicator_map = {to_str_date(row.get("trade_date")): row for row in indicator_history}
        rows: List[Dict[str, Any]] = []
        for row in reversed(history):
            trade_date = to_str_date(row.get("trade_date"))
            ind = indicator_map.get(trade_date, {})
            merged = dict(row)
            merged.update({key: value for key, value in ind.items() if key not in merged or merged.get(key) is None})
            quote = self.build_market_quote(merged)
            if quote:
                rows.append(quote)
        return rows

    def build_overview(
        self,
        quote: Optional[Dict[str, Any]],
        history: List[Dict[str, Any]],
        valuation: Optional[Dict[str, Any]],
        financial: Optional[Dict[str, Any]],
    ) -> Dict[str, Any]:
        """构建 TargetOverview。

        风险等级按需求通过 utils 预留入口返回固定值，后续可替换为模型/规则实现。
        """
        trend_status = self.estimate_trend_status(history)
        valuation_status = (valuation or {}).get("valuationStatus") or "UNKNOWN"
        risk_level = estimate_risk_level_placeholder(quote=quote, history=history, financial=financial)
        chg_pct = to_float_or_none((quote or {}).get("chg_pct")) or 0.0
        roe = to_float_or_none((financial or {}).get("roe"))

        score = 0
        score += 1 if trend_status == "UPTREND" else -1 if trend_status == "DOWNTREND" else 0
        score += 1 if valuation_status in {"UNDERVALUED", "FAIR"} else -1 if valuation_status == "OVERVALUED" else 0
        score += 1 if roe is not None and roe >= 10 else 0
        score += 1 if chg_pct >= 2 else -1 if chg_pct <= -2 else 0

        if score >= 3:
            overall_view, overall_text = "STRONGLY_BULLISH", "强烈看多"
        elif score >= 1:
            overall_view, overall_text = "BULLISH", "偏多"
        elif score <= -3:
            overall_view, overall_text = "STRONGLY_BEARISH", "强烈看空"
        elif score <= -1:
            overall_view, overall_text = "BEARISH", "偏空"
        else:
            overall_view, overall_text = "NEUTRAL", "中性"

        reasons: List[str] = []
        if quote:
            reasons.append(f"最新涨跌幅为{round(chg_pct, 2)}%")
        if trend_status != "UNKNOWN":
            reasons.append(f"短中期趋势判断为{trend_status}")
        if valuation_status != "UNKNOWN":
            reasons.append(f"估值状态为{valuation_status}")
        if roe is not None:
            reasons.append(f"最新ROE为{round(roe, 2)}%")

        risks = ["风险等级当前使用预留入口的固定返回值，待后续接入风险模型后更新。"]
        if valuation_status == "OVERVALUED":
            risks.append("估值处于偏高区间，注意回撤风险。")

        return {
            "overallView": overall_view,
            "overallViewText": overall_text,
            "riskLevel": risk_level,
            "valuationStatus": valuation_status,
            "summary": "；".join(reasons) if reasons else "当前可用数据有限，建议结合更多行情和财务数据观察。",
            "reasons": reasons,
            "risks": risks,
            "suggestions": [
                "优先跟踪申万三级同行业估值与涨跌幅对比。",
                "结合成交额、换手率和资金流变化观察信号延续性。",
                "财务数据以最新公告为准，关注后续报告期变化。",
            ],
            "confidence": self.estimate_confidence(quote, valuation, financial),
        }

    def build_metric_cards(
        self,
        quote: Optional[Dict[str, Any]],
        valuation: Optional[Dict[str, Any]],
        financial: Optional[Dict[str, Any]],
        capital_flow: Optional[Dict[str, Any]],
    ) -> List[Dict[str, Any]]:
        """构建顶部指标卡。"""
        cards = [
            self.metric_card("close", "最新价", (quote or {}).get("close"), "元", sort_order=1),
            self.metric_card("chgPct", "涨跌幅", (quote or {}).get("chg_pct"), "%", sort_order=2),
            self.metric_card("turnover", "成交额", (quote or {}).get("turnover"), "元", sort_order=3),
            self.metric_card("turnoverRate", "换手率", (quote or {}).get("turnover_rate"), "%", sort_order=4),
            self.metric_card("peTtm", "PE TTM", (valuation or {}).get("peTtm"), None, sort_order=5),
            self.metric_card("pb", "PB", (valuation or {}).get("pb"), None, sort_order=6),
            self.metric_card("roe", "ROE", (financial or {}).get("roe"), "%", sort_order=7),
            self.metric_card("netMfAmount", "资金净流入", (capital_flow or {}).get("netMfAmount"), "元", sort_order=8),
        ]
        return [card for card in cards if card.get("value") is not None]

    # ------------------------------------------------------------------
    # 申万三级行业、同行业比较、所属指数
    # ------------------------------------------------------------------

    async def load_industry_context(self, basic: Optional[Dict[str, Any]], full_symbol: str, trade_date: str) -> Dict[str, Any]:
        """加载并加工申万行业上下文。

        个股行业归属以 index_constituent_data 中 .SI 指数为入口；申万一/二/三级
        层级由 index_industry_level 补齐。同行业比较、行业估值和行业摘要均默认使用
        申万三级行业指数成分。
        """
        stock_name = (basic or {}).get("name")
        memberships = await self.basic_repo.get_stock_sw_industry_memberships(full_symbol)
        level3 = [row for row in memberships if self.parse_sw_level(row.get("level")) == 3]
        # 优化选择个股所属的三级行业，若没有，选择最高层级的行业作为叶子节点
        leaf = level3[0] if level3 else memberships[0]
        sw_tree_rows = await self.build_sw_industry_tree_rows(leaf, stock_name, trade_date)

        benchmark_row = next((row for row in sw_tree_rows if row.get("industryLevel") == 3), None) or (sw_tree_rows[-1] if sw_tree_rows else None)
        benchmark_index_code = (benchmark_row or {}).get("indexCode")

        benchmark_components = await self.basic_repo.get_index_components(benchmark_index_code, trade_date=trade_date) if benchmark_index_code else []
        peer_codes = [row.get("stock_code", '') for row in benchmark_components]
        peer_basic_map = await self.basic_repo.get_stock_basic_by_codes(peer_codes) if peer_codes else {}
        peer_snapshots = await self.exchange_repo.get_stock_latest_snapshots_by_codes(peer_codes, trade_date) if peer_codes else []

        belong_index_rows = await self.build_belong_index_rows(full_symbol, stock_name, trade_date)
        return {
            "swIndustryRows": sw_tree_rows,
            "benchmarkIndexCode": benchmark_index_code,
            "benchmarkIndustryCode": (benchmark_row or {}).get("industryCode"),
            "benchmarkIndustryName": (benchmark_row or {}).get("industryName"),
            "benchmarkComponents": benchmark_components,
            "peerComponents": benchmark_components,
            "peerBasicMap": peer_basic_map,
            "peerSnapshots": peer_snapshots,
            "stockName": stock_name,
            "fallbackIndustry": (basic or {}).get("industry"),
            "belongIndexRows": belong_index_rows,
        }

    async def build_sw_industry_tree_rows(self, leaf: Optional[Dict[str, Any]], stock_name: Optional[str], trade_date: str) -> List[Dict[str, Any]]:
        """由申万三级叶子行业向上补齐一/二/三级行业，并获取各指数的成分和快照信息。"""
        if not leaf:
            return []

        rows_by_code: Dict[str, Dict[str, Any]] = {}

        def put(row: Optional[Dict[str, Any]]) -> None:
            if not row:
                return
            for key in [row.get("index_code"), row.get("industry_code")]:
                if key:
                    rows_by_code[str(key)] = row

        put(leaf)
        parent_codes = [leaf.get("parent_code")]
        # 最多补两层父级，满足申万一/二/三级展示。
        for _ in range(2):
            codes = [code for code in parent_codes if code]
            if not codes:
                break
            rows = await self.basic_repo.get_sw_industry_rows_by_codes(codes)
            parent_codes = []
            for row in rows:
                put(row)
                if row.get("parent_code"):
                    parent_codes.append(row.get("parent_code"))

        # index_code 与 industry_code 对应的信息可能重复，去重后按 level 排序。
        unique_rows = list({id(row): row for row in rows_by_code.values()}.values())
        unique_rows.sort(key=lambda row: self.parse_sw_level(row.get("level")) or 99)
        # 获取最终的行业指数代码
        index_codes = [row.get("index_code", '') for row in unique_rows if row.get("index_code")]
        component_counts = await self.basic_repo.get_index_component_counts(index_codes)
        # 获取行业指数快照，便于展示涨跌幅、总市值、换手率等指标
        snapshots = await self.exchange_repo.get_index_latest_snapshots_by_codes(index_codes, trade_date) if index_codes else []
        snapshot_map = {row.get("full_symbol"): row for row in snapshots if row.get("full_symbol")}

        result: List[Dict[str, Any]] = []
        for row in unique_rows:
            index_code = row.get("index_code") or row.get("industry_code")
            snap = snapshot_map.get(row.get("index_code"), {})
            level = self.parse_sw_level(row.get("level"))
            result.append(self.compact_dict({
                "indexCode": row.get("index_code"),
                "fullSymbol": leaf.get("stock_code"),
                "stockName": stock_name,
                "industryCode": index_code,
                "industryName": row.get("industry_name") or row.get("index_name") or index_code,
                "industryLevel": level,
                "parentIndustryCode": row.get("parent_code"),
                "parentIndustryName": self.lookup_industry_name(rows_by_code, row.get("parent_code")),
                "tradeDate": to_str_date(snap.get("trade_date")),
                "chgPct": round_or_none(snap.get("chg_pct")),
                "totalMarketValue": round_or_none(snap.get("total_market_value"), 2),
                "turnover": round_or_none(snap.get("turnover"), 2),
                "componentCount": component_counts.get(row.get("index_code", ''), 0),
                "peTtm": round_or_none(snap.get("pe_ttm")),
                "pb": round_or_none(snap.get("pb")),
                "psTtm": None,
            }))
        return [row for row in result if row.get("industryLevel") in {1, 2, 3}]

    async def build_belong_index_rows(self, full_symbol: str, stock_name: Optional[str], trade_date: str) -> List[Dict[str, Any]]:
        """构建个股所属非行业指数行，覆盖规模、策略、主题等指数。"""
        index_rows = await self.basic_repo.get_stock_related_indices(full_symbol, limit=30, include_industry=False)
        index_codes = [row.get("index_code", '') for row in index_rows if row.get("index_code")]
        snapshots = await self.exchange_repo.get_index_latest_snapshots_by_codes(index_codes, trade_date) if index_codes else []
        snapshot_map = {row.get("full_symbol"): row for row in snapshots if row.get("full_symbol")}

        result = []
        for row in index_rows:
            code = row.get("index_code")
            snap = snapshot_map.get(code, {})
            category_code, category_name = self.classify_related_index(row)
            result.append(self.compact_dict({
                "fullSymbol": full_symbol,
                "stockName": stock_name,
                "indexCode": code,
                "indexName": row.get("name") or row.get("fullname") or code,
                "indexCategory": category_code,
                "indexCategoryName": category_name,
                "tradeDate": to_str_date(snap.get("trade_date") or row.get("trade_date")),
                "chgPct": round_or_none(snap.get("chg_pct")),
                "totalMarketValue": round_or_none(snap.get("total_market_value"), 2),
                "turnover": round_or_none(snap.get("turnover"), 2),
                "weight": round_or_none(row.get("weight")),
            }))
        return result

    def build_industry_info(self, context: Dict[str, Any]) -> Optional[Dict[str, Any]]:
        """构建 IndustryInfo，默认以申万三级行业成分为统计样本。"""
        benchmark_name = context.get("benchmarkIndustryName") or context.get("fallbackIndustry")
        if not benchmark_name:
            return None

        snapshots = context.get("peerSnapshots") or []
        chg_values = [to_float_or_none(row.get("chg_pct")) for row in snapshots]
        valid_chg = [v for v in chg_values if v is not None]
        pe_values = [to_float_or_none(row.get("pe_ttm")) for row in snapshots]
        pb_values = [to_float_or_none(row.get("pb")) for row in snapshots]
        return self.compact_dict({
            "industryCode": context.get("benchmarkIndustryCode"),
            "industryName": benchmark_name,
            "industryLevel": 3 if context.get("benchmarkIndustryCode") else None,
            "stockCount": len(context.get("benchmarkComponents") or snapshots),
            "upCount": sum(1 for v in valid_chg if v > 0),
            "downCount": sum(1 for v in valid_chg if v < 0),
            "flatCount": sum(1 for v in valid_chg if v == 0),
            "avgChangePercent": round_or_none(statistics.fmean(valid_chg) if valid_chg else None),
            "peMedian": round_or_none(statistics.median([v for v in pe_values if v is not None]) if any(v is not None for v in pe_values) else None),
            "pbMedian": round_or_none(statistics.median([v for v in pb_values if v is not None]) if any(v is not None for v in pb_values) else None),
        })

    def build_industry_peer_rows(self, context: Dict[str, Any]) -> List[Dict[str, Any]]:
        """构建 StockIndustryPeerRow，同业样本默认来自申万三级行业指数成分。"""
        peer_components = context.get("peerComponents") or []
        basic_map = context.get("peerBasicMap") or {}
        snapshot_map = {row.get("full_symbol"): row for row in context.get("peerSnapshots", []) if row.get("full_symbol")}
        level_rows = context.get("swIndustryRows") or []
        level_map = {row.get("industryLevel"): row for row in level_rows}

        result: List[Dict[str, Any]] = []
        for item in peer_components:
            code = item.get("stock_code")
            basic = basic_map.get(code, {})
            snap = snapshot_map.get(code, {})
            result.append(self.compact_dict({
                **self.build_stock_profile(basic, code),
                "swIndustryLevel1Code": (level_map.get(1) or {}).get("industryCode"),
                "swIndustryLevel1Name": (level_map.get(1) or {}).get("industryName"),
                "swIndustryLevel2Code": (level_map.get(2) or {}).get("industryCode"),
                "swIndustryLevel2Name": (level_map.get(2) or {}).get("industryName"),
                "swIndustryLevel3Code": (level_map.get(3) or {}).get("industryCode"),
                "swIndustryLevel3Name": (level_map.get(3) or {}).get("industryName"),
                "tradeDate": to_str_date(snap.get("trade_date") or item.get("trade_date")),
                "close": round_or_none(snap.get("close")),
                "chgPct": round_or_none(snap.get("chg_pct")),
                "totalMarketValue": round_or_none(snap.get("total_market_value"), 2),
                "floatMarketValue": round_or_none(snap.get("float_market_value"), 2),
                "peTtm": round_or_none(snap.get("pe_ttm")),
                "pb": round_or_none(snap.get("pb")),
                "psTtm": round_or_none(snap.get("ps_ttm")),
                "turnoverRate": round_or_none(snap.get("turnover_rate")),
            }))
        return result

    def build_related_targets(self, context: Dict[str, Any]) -> List[Dict[str, Any]]:
        """相关标的：申万三级同业股票 + 所属指数。"""
        result: List[Dict[str, Any]] = []
        industry_name = context.get("benchmarkIndustryName") or context.get("fallbackIndustry")
        peer_rows = self.build_industry_peer_rows(context)[:5]
        for row in peer_rows:
            result.append(self.compact_dict({
                "fullSymbol": row.get("fullSymbol"),
                "name": row.get("name") or row.get("fullSymbol"),
                "relationType": "SAME_SW3_INDUSTRY",
                "relationName": industry_name,
                "chgPct": row.get("chgPct"),
            }))
        for row in context.get("belongIndexRows", [])[:5]:
            result.append(self.compact_dict({
                "fullSymbol": row.get("indexCode"),
                "name": row.get("indexName") or row.get("indexCode"),
                "relationType": "BELONG_INDEX",
                "relationName": row.get("indexCategoryName") or "所属指数",
                "chgPct": row.get("chgPct"),
            }))
        return result

    def build_valuation_info(
        self,
        quote: Optional[Dict[str, Any]],
        indicator_history: List[Dict[str, Any]],
    ) -> Optional[Dict[str, Any]]:
        """构建 ValuationInfo 摘要。"""
        if not quote and not indicator_history:
            return None
        source = quote or (indicator_history[0] if indicator_history else {})
        pe_ttm = round_or_none(source.get("pe_ttm"))
        pb = round_or_none(source.get("pb"))
        ps_ttm = round_or_none(source.get("ps_ttm"))
        pe_pct = percentile_rank(pe_ttm, [row.get("pe_ttm") for row in indicator_history])
        pb_pct = percentile_rank(pb, [row.get("pb") for row in indicator_history])
        valuation_status = self.estimate_valuation_status(pe_ttm, pb, pe_pct, pb_pct)
        return self.compact_dict({
            "pe": round_or_none(source.get("pe")),
            "peTtm": pe_ttm,
            "pb": pb,
            "ps": round_or_none(source.get("ps")),
            "psTtm": ps_ttm,
            "dvTtm": round_or_none(source.get("dv_ttm")),
            "totalMarketValue": round_or_none(source.get("total_market_value"), 2),
            "valuationStatus": valuation_status,
        })


    def build_industry_valuation_stats(
        self,
        rows: List[Dict[str, Any]],
        fields: Optional[Sequence[str]] = None,
    ) -> Dict[str, Dict[str, Optional[float]]]:
        """按交易日聚合申万三级行业成分股票估值。

        行业平均值按 float_market_value 加权；若当日缺少有效流通市值，退化为算术平均。
        中位数不加权。

        fields 仅用于内部按需计算：
        - None 时保持原逻辑，聚合 pe_ttm / pb / ps_ttm / dv_ttm；
        - 指定 fields 时仅生成对应的 *_avg / *_median。
        """
        supported_fields = ("pe_ttm", "pb", "ps_ttm", "dv_ttm")
        if fields is None:
            requested_fields = supported_fields
        else:
            requested_fields = tuple(field for field in fields if field in supported_fields)

        grouped: Dict[str, List[Dict[str, Any]]] = {}
        for row in rows:
            trade_date = to_str_date(row.get("trade_date"))
            if trade_date:
                grouped.setdefault(trade_date, []).append(row)

        stats: Dict[str, Dict[str, Optional[float]]] = {}
        for trade_date, date_rows in grouped.items():
            item: Dict[str, Optional[float]] = {}
            for field in requested_fields:
                pairs: List[Tuple[float, Optional[float]]] = []
                for row in date_rows:
                    value = to_float_or_none(row.get(field))
                    if value is None:
                        continue
                    weight = to_float_or_none(row.get("float_market_value"))
                    pairs.append((value, weight))

                values = [value for value, _ in pairs]
                valid_weight_pairs = [(value, weight) for value, weight in pairs if weight is not None and weight > 0]
                if valid_weight_pairs:
                    total_weight = sum(weight for _, weight in valid_weight_pairs if weight is not None)
                    item[f"{field}_avg"] = (
                        sum(value * (weight or 0) for value, weight in valid_weight_pairs) / total_weight
                        if total_weight else None
                    )
                else:
                    item[f"{field}_avg"] = statistics.fmean(values) if values else None
                item[f"{field}_median"] = statistics.median(values) if values else None
            stats[trade_date] = item
        return stats

    def build_valuation_rows(
        self,
        indicator_history: List[Dict[str, Any]],
        industry_stats: Dict[str, Dict[str, Optional[float]]],
    ) -> List[Dict[str, Any]]:
        """构建 ValuationDailyRow，补充申万三级行业平均/中位估值。"""
        result: List[Dict[str, Any]] = []
        for row in reversed(indicator_history):
            trade_date = to_str_date(row.get("trade_date"))
            stats = industry_stats.get(trade_date, {})
            result.append(self.compact_dict({
                "fullSymbol": row.get("full_symbol"),
                "tradeDate": trade_date,
                "close": round_or_none(row.get("close")),
                "totalMarketValue": round_or_none(row.get("total_market_value"), 2),
                "floatMarketValue": round_or_none(row.get("float_market_value"), 2),
                "totalShare": round_or_none(row.get("total_share"), 2),
                "floatShare": round_or_none(row.get("float_share"), 2),
                "freeShare": round_or_none(row.get("free_share"), 2),
                "turnoverRate": round_or_none(row.get("turnover_rate")),
                "turnoverRateFree": round_or_none(row.get("turnover_rate_free")),
                "pe": round_or_none(row.get("pe")),
                "peTtm": round_or_none(row.get("pe_ttm")),
                "pb": round_or_none(row.get("pb")),
                "ps": round_or_none(row.get("ps")),
                "psTtm": round_or_none(row.get("ps_ttm")),
                "dvRatio": round_or_none(row.get("dv_ratio")),
                "dvTtm": round_or_none(row.get("dv_ttm")),
                "industryAveragePeTtm": round_or_none(stats.get("pe_ttm_avg")),
                "industryMedianPeTtm": round_or_none(stats.get("pe_ttm_median")),
                "industryAveragePb": round_or_none(stats.get("pb_avg")),
                "industryMedianPb": round_or_none(stats.get("pb_median")),
                "industryAveragePsTtm": round_or_none(stats.get("ps_ttm_avg")),
                "industryMedianPsTtm": round_or_none(stats.get("ps_ttm_median")),
                "industryAverageDvTtm": round_or_none(stats.get("dv_ttm_avg")),
                "industryMedianDvTtm": round_or_none(stats.get("dv_ttm_median")),
            }))
        return result


    def build_valuation_metric_summaries(
        self,
        indicator_history: List[Dict[str, Any]],
        industry_stats: Dict[str, Dict[str, Optional[float]]],
        history_limit: int = 260,
        metric_codes: Optional[Sequence[str]] = None,
    ) -> List[Dict[str, Any]]:
        """构建估值指标摘要。

        metric_codes=None 时构建 PE_TTM/PB/PS_TTM 全部摘要；
        指定 metric_codes 时仅构建请求指标，供拆分接口按需使用。
        """
        if not indicator_history:
            return []

        limit = max(1, min(int(history_limit or 260), len(indicator_history)))
        latest = indicator_history[0]
        latest_date = to_str_date(latest.get("trade_date"))
        latest_stats = industry_stats.get(latest_date, {})

        requested_codes = tuple(metric_codes) if metric_codes is not None else tuple(VALUATION_METRICS.keys())
        result: List[Dict[str, Any]] = []

        for metric_code in requested_codes:
            cfg = VALUATION_METRICS.get(metric_code)
            if not cfg:
                continue

            field = cfg["field"]
            values = [row.get(field) for row in indicator_history[:limit]]
            current_value = to_float_or_none(latest.get(field))
            avg_key = f"{field}_avg"
            med_key = f"{field}_median"

            result.append(self.compact_dict({
                "metricCode": metric_code,
                "metricName": cfg["name"],
                "currentValue": round_or_none(current_value),
                "percentileValue": percentile_rank(current_value, values),
                "percentile30Value": round_or_none(percentile_value(values, 30)),
                "percentile70Value": round_or_none(percentile_value(values, 70)),
                "industryAverageValue": round_or_none(latest_stats.get(avg_key)),
                "industryMedianValue": round_or_none(latest_stats.get(med_key)),
                "latestTradeDate": latest_date,
                "description": "同行业口径使用申万三级行业成分聚合；行业均值按流通市值加权。",
            }))
        return result

    def build_valuation_channel_rows(
        self,
        indicator_history: List[Dict[str, Any]],
        metric_code: str,
        history_limit: int = 260,
    ) -> List[Dict[str, Any]]:
        """构建指定指标的估值通道。估值通道共包含5条线，中位线为指标中位数，其他4条线为中位数上下各50%的区间。"""
        if not indicator_history:
            return []

        metric_code = self.normalize_metric_code(metric_code)
        field = VALUATION_METRICS[metric_code]["field"]

        limit = max(1, min(int(history_limit or 260), len(indicator_history)))
        values = [to_float_or_none(row.get(field)) for row in indicator_history[:limit]]
        # 仅使用正数指标值计算通道，避免负数或零导致的通道异常。
        numeric_values = [v for v in values if v is not None and v > 0]
        if not numeric_values:
            return []  # 如果没有有效的正数指标值，则无法计算通道，直接返回空列表。

        v_min = min(numeric_values)
        v_median = statistics.median(numeric_values)
        step = (v_median - v_min) * 0.5
        channel_multiples = [
            v_min,
            v_min + step,
            v_median,
            v_median + step,
            v_median + 2 * step,
        ]

        result: List[Dict[str, Any]] = []
        # 按时间升序遍历指标历史，构建每个交易日的估值通道数据。
        for row in list(reversed(indicator_history[:limit])):
            value = to_float_or_none(row.get(field))
            close = to_float_or_none(row.get("close"))
            item: Dict[str, Any] = {
                "fullSymbol": row.get("full_symbol"),
                "tradeDate": to_str_date(row.get("trade_date")),
                "metricCode": metric_code,
                "close": round_or_none(close),
            }
            if value and value > 0 and close is not None:
                base_per_multiple = close / value
                for idx, multiple in enumerate(channel_multiples, start=1):
                    item[f"channelMultiple{idx}"] = round_or_none(multiple)
                    item[f"channelPrice{idx}"] = round_or_none(base_per_multiple * multiple)
            result.append(self.compact_dict(item))
        return result

    def build_valuation_trend_rows(
        self,
        indicator_history: List[Dict[str, Any]],
        industry_stats: Dict[str, Dict[str, Optional[float]]],
        metric_code: str,
    ) -> List[Dict[str, Any]]:
        """构建指定指标的估值趋势。包含指标值、行业均值、中位数、30/70分位数及对应的百分位排名。"""
        if not indicator_history:
            return []

        metric_code = self.normalize_metric_code(metric_code)
        field = VALUATION_METRICS[metric_code]["field"]

        limit = len(indicator_history)
        values = [row.get(field) for row in indicator_history[:limit]]
        p30 = percentile_value(values, 30)
        p70 = percentile_value(values, 70)
        avg_key = f"{field}_avg"
        med_key = f"{field}_median"

        result: List[Dict[str, Any]] = []
        # 按时间升序遍历指标历史，构建每个交易日的估值趋势数据。
        for row in list(reversed(indicator_history[:limit])):
            trade_date = to_str_date(row.get("trade_date"))
            value = to_float_or_none(row.get(field))
            stats = industry_stats.get(trade_date, {})
            result.append(self.compact_dict({
                "fullSymbol": row.get("full_symbol"),
                "tradeDate": trade_date,
                "metricCode": metric_code,
                "stockValue": round_or_none(value),
                "industryAverageValue": round_or_none(stats.get(avg_key)),
                "industryMedianValue": round_or_none(stats.get(med_key)),
                "percentile30Value": round_or_none(p30),
                "percentile70Value": round_or_none(p70),
                "percentileValue": percentile_rank(value, values),
            }))
        return result

    def build_dividend_trend_rows(
        self,
        indicator_history: List[Dict[str, Any]],
        industry_stats: Dict[str, Dict[str, Optional[float]]],
    ) -> List[Dict[str, Any]]:
        """构建分红 TTM 趋势，只依赖 dv_ttm 对应的行业统计。"""
        if not indicator_history:
            return []

        limit = len(indicator_history)
        rows_asc = list(reversed(indicator_history[:limit]))

        result: List[Dict[str, Any]] = []
        for row in rows_asc:
            trade_date = to_str_date(row.get("trade_date"))
            stats = industry_stats.get(trade_date, {})
            result.append(self.compact_dict({
                "fullSymbol": row.get("full_symbol"),
                "tradeDate": trade_date,
                "dvTtm": round_or_none(row.get("dv_ttm")),
                "industryAverageDvTtm": round_or_none(stats.get("dv_ttm_avg")),
                "industryMedianDvTtm": round_or_none(stats.get("dv_ttm_median")),
            }))
        return result

    def build_valuation_detail(
        self,
        indicator_history: List[Dict[str, Any]],
        industry_stats: Dict[str, Dict[str, Optional[float]]],
        history_limit: int = 260,
    ) -> Optional[Dict[str, Any]]:
        """兼容旧 TargetDetail 数据流的完整估值详情聚合器。

        对外结构保持原样：
        {
            metricSummaries,
            channelRows,
            trendRows,
            dividendRows
        }
        """
        if not indicator_history:
            return None

        metric_summaries = self.build_valuation_metric_summaries(
            indicator_history,
            industry_stats,
            history_limit=history_limit,
        )

        channel_rows: List[Dict[str, Any]] = []
        trend_rows: List[Dict[str, Any]] = []
        for metric_code in VALUATION_METRICS:
            channel_rows.extend(
                self.build_valuation_channel_rows(
                    indicator_history,
                    metric_code,
                    history_limit=history_limit,
                )
            )
            trend_rows.extend(
                self.build_valuation_trend_rows(
                    indicator_history,
                    industry_stats,
                    metric_code
                )
            )

        dividend_rows = self.build_dividend_trend_rows(
            indicator_history,
            industry_stats,
        )

        return {
            "metricSummaries": metric_summaries,
            "channelRows": channel_rows,
            "trendRows": trend_rows,
            "dividendRows": dividend_rows,
        }

    def build_financial_summary(self, bundle: Dict[str, Optional[Dict[str, Any]]]) -> Optional[Dict[str, Any]]:
        """构建 FinancialSummary。"""
        if not bundle:
            return None
        income = bundle.get("income") or {}
        balance = bundle.get("balanceSheet") or {}
        cashflow = bundle.get("cashflow") or {}
        indicator = bundle.get("indicator") or {}
        end_date = indicator.get("end_date") or income.get("end_date") or balance.get("end_date") or cashflow.get("end_date")
        if not end_date:
            return None
        report_type_code = self.infer_report_type(end_date)
        summary = self.compact_dict({
            "endDate": to_str_date(end_date),
            "revenue": round_or_none(income.get("revenue"), 2),
            "revenueYoy": round_or_none(indicator.get("revenue_yoy")),
            "nIncomeAttrP": round_or_none(income.get("n_income_attr_p"), 2),
            "netprofitYoy": round_or_none(indicator.get("netprofit_yoy")),
            "profitDedt": round_or_none(indicator.get("profit_dedt"), 2),
            "dtNetprofitYoy": round_or_none(indicator.get("dt_netprofit_yoy")),
            "roe": round_or_none(indicator.get("roe")),
            "roa": round_or_none(indicator.get("roa")),
            "debtToAssets": round_or_none(indicator.get("debt_to_assets")),
            "nCashflowAct": round_or_none(cashflow.get("n_cashflow_act"), 2),
            "freeCashflow": round_or_none(cashflow.get("free_cashflow"), 2),
            "eps": round_or_none(indicator.get("eps") or income.get("basic_eps")),
            "bps": round_or_none(indicator.get("bps")),
            "dividendPerShare": None,
            "dividendPayoutRatio": None,
            "annDate": to_str_date(indicator.get("ann_date") or income.get("ann_date")),
            "fAnnDate": to_str_date(income.get("f_ann_date") or balance.get("f_ann_date") or cashflow.get("f_ann_date")),
            "reportType": income.get("report_type") or balance.get("report_type") or cashflow.get("report_type"),
            "reportTypeText": REPORT_TYPE_TEXT_MAP.get(report_type_code, report_type_code),
        })
        summary["conclusion"] = self.build_financial_conclusion(summary)
        return summary

    def build_financial_detail(self, bundle: Dict[str, List[Dict[str, Any]]]) -> Optional[Dict[str, Any]]:
        """构建 FinancialStatementDetail。"""
        if not bundle or not any(bundle.values()):
            return None
        income_rows = [self.camelize_row(row, date_fields={"ann_date", "f_ann_date", "end_date"}) for row in bundle.get("incomeRows", [])]
        balance_rows = [self.camelize_row(row, date_fields={"ann_date", "f_ann_date", "end_date"}) for row in bundle.get("balanceSheetRows", [])]
        cashflow_rows = [self.camelize_row(row, date_fields={"ann_date", "f_ann_date", "end_date"}) for row in bundle.get("cashflowRows", [])]
        indicator_rows = [self.camelize_row(row, date_fields={"ann_date", "end_date"}) for row in bundle.get("indicatorRows", [])]
        report_info = self.build_financial_report_info(income_rows, balance_rows, cashflow_rows, indicator_rows)
        return self.compact_dict({
            "incomeRows": income_rows,
            "balanceSheetRows": balance_rows,
            "cashflowRows": cashflow_rows,
            "indicatorRows": indicator_rows,
            "reportInfo": report_info,
            "profitTrendRows": self.build_profit_trend_rows(income_rows, indicator_rows),
            "ratioTrendRows": self.build_ratio_trend_rows(indicator_rows),
            "cashflowStructureRows": self.build_cashflow_structure_rows(cashflow_rows),
            "balanceStructureRows": self.build_balance_structure_rows(balance_rows, indicator_rows),
        })

    def build_capital_flow_info(self, quote: Optional[Dict[str, Any]], moneyflow_history: List[Dict[str, Any]]) -> Optional[Dict[str, Any]]:
        """构建 CapitalFlowInfo。"""
        source = quote if quote and quote.get("net_mf_amount") is not None else (moneyflow_history[0] if moneyflow_history else None)
        if not source:
            return None
        net_mf_amount = round_or_none(source.get("net_mf_amount"), 2)
        return self.compact_dict({
            "tradeDate": to_str_date(source.get("trade_date")),
            "netMfAmount": net_mf_amount,
            "elgNetMfAmount": round_or_none(self.net_flow(source, "elg"), 2),
            "lgNetMfAmount": round_or_none(self.net_flow(source, "lg"), 2),
            "mdNetMfAmount": round_or_none(self.net_flow(source, "md"), 2),
            "smNetMfAmount": round_or_none(self.net_flow(source, "sm"), 2),
            "conclusion": "资金净流入为正" if (to_float_or_none(net_mf_amount) or 0) > 0 else "资金净流入为负或暂无明显流入",
        })

    def build_money_flow_rows(self, moneyflow_history: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """构建 StockMoneyflowDailyRow。"""
        return [self.camelize_row(row, date_fields={"trade_date"}) for row in reversed(moneyflow_history)]


    def build_quote_market_charts(
        self,
        technical_rows: Sequence[Mapping[str, Any]],
    ) -> List[Dict[str, Any]]:
        """构建行情 Tab 使用的 K 线和技术指标 ChartGroup。

        每个技术指标使用独立 chartId；所有序列均来自同一批已复权 technical_rows，
        保证价格、技术指标和区间统计使用同一行情口径。
        """
        rows = list(technical_rows)
        if not rows:
            return []

        charts: List[Dict[str, Any]] = []
        x_axis = [row.get("tradeDate") or to_str_date(row.get("trade_date")) for row in rows]

        def row_number(row: Mapping[str, Any], *keys: str) -> Optional[float]:
            for key in keys:
                value = to_float_or_none(row.get(key))
                if value is not None:
                    return value
            return None

        def number_series(*keys: str, digits: int = 4) -> List[Optional[float]]:
            return [round_or_none(row_number(row, *keys), digits) for row in rows]

        def append_chart(chart_id: str, title: str, chart_type: str, series: List[Dict[str, Any]]) -> None:
            charts.append({
                "chartId": chart_id,
                "title": title,
                "chartType": chart_type,
                "period": "DAILY",
                "xAxis": x_axis,
                "series": series,
            })

        charts.append({
            "chartId": "stockKlineDaily",
            "title": "股票日K线",
            "chartType": "CANDLESTICK",
            "period": "DAILY",
            "xAxis": x_axis,
            "series": [{
                "name": "OHLC",
                "unit": "元",
                "data": [self.extract_market_quote_row(row) for row in rows],
            }],
            "options": {
                "mainIndicators": ["MA", "EMA", "BOLL", "SAR", "VWAP", "SUPER_TREND"],
                "subIndicators": ["VOL", "TURNOVER", "TURNOVER_RATE", "MACD", "KDJ", "RSI", "CCI", "DMI", "ATR", "OBV"],
            },
        })

        for window in (5, 10, 20, 60, 120, 250):
            append_chart(f"MA{window}", f"MA{window}", "LINE", [
                {"name": f"MA{window}", "data": number_series(f"ma{window}", f"MA{window}"), "unit": "元"}
            ])

        for window in (9, 12, 20, 26):
            append_chart(f"EMA{window}", f"EMA{window}", "LINE", [
                {"name": f"EMA{window}", "data": number_series(f"ema{window}", f"EMA{window}"), "unit": "元"}
            ])

        append_chart("BOLL", "BOLL", "LINE", [
            {"name": "upper", "data": number_series("bollUpper"), "unit": "元"},
            {"name": "mid", "data": number_series("bollMid"), "unit": "元"},
            {"name": "lower", "data": number_series("bollLower"), "unit": "元"},
        ])
        append_chart("SAR", "SAR", "LINE", [
            {"name": "SAR", "data": number_series("sar", "SAR"), "unit": "元"}
        ])

        for window in (1, 5, 10, 20, 60, 120):
            append_chart(f"VWAP{window}", f"VWAP{window}", "LINE", [
                {"name": f"VWAP{window}", "data": number_series(f"vwap{window}", f"VWAP{window}"), "unit": "元"}
            ])

        append_chart("SUPER_TREND", "SUPER_TREND", "LINE", [
            {"name": "SuperTrend", "data": number_series("superTrend10"), "unit": "元"},
            {"name": "SuperTrend Upper", "data": number_series("superTrendUpper10"), "unit": "元"},
            {"name": "SuperTrend Lower", "data": number_series("superTrendLower10"), "unit": "元"},
        ])
        append_chart("VOL", "VOL", "BAR", [
            {"name": "volume", "data": number_series("volume", digits=2), "unit": "股/手"}
        ])
        append_chart("TURNOVER", "成交额", "BAR", [
            {"name": "turnover", "data": number_series("turnover", digits=2), "unit": "元"}
        ])
        append_chart("TURNOVER_RATE", "换手率", "LINE", [
            {"name": "turnoverRate", "data": number_series("turnoverRate", "turnover_rate"), "unit": "%"}
        ])
        append_chart("MACD", "MACD", "LINE", [
            {"name": "DIF", "data": number_series("macdDiff", "macd_diff")},
            {"name": "DEA", "data": number_series("macdDea", "macd_dea")},
            {"name": "MACD", "data": number_series("macd")},
        ])
        append_chart("KDJ", "KDJ", "LINE", [
            {"name": "K", "data": number_series("kdjK")},
            {"name": "D", "data": number_series("kdjD")},
            {"name": "J", "data": number_series("kdjJ")},
        ])
        append_chart("RSI", "RSI", "LINE", [
            {"name": "RSI14", "data": number_series("rsi14", "RSI14")}
        ])
        append_chart("CCI", "CCI", "LINE", [
            {"name": "CCI14", "data": number_series("cci14", "CCI14")}
        ])
        append_chart("DMI", "DMI", "LINE", [
            {"name": "+DI", "data": number_series("dmiPdi14")},
            {"name": "-DI", "data": number_series("dmiMdi14")},
            {"name": "ADX", "data": number_series("dmiAdx14")},
        ])
        append_chart("ATR", "ATR", "LINE", [
            {"name": "ATR14", "data": number_series("atr14", "ATR14"), "unit": "元"}
        ])
        append_chart("OBV", "OBV", "LINE", [
            {"name": "OBV", "data": number_series("obv", "OBV", digits=2), "unit": "股/手"},
            {"name": "OBV14", "data": number_series("obv14", "OBV14", digits=2), "unit": "股/手"},
        ])
        return charts

    def build_charts(
        self,
        technical_rows: List[Dict[str, Any]],
        valuation_rows: List[Dict[str, Any]],
        money_flow_rows: List[Dict[str, Any]],
        financial_detail: Optional[Dict[str, Any]],
        financial_history: List[Dict[str, Any]],
    ) -> List[Dict[str, Any]]:
        """构建 ChartGroup，图表数据全部来自已查询记录和本地计算结果。"""
        charts: List[Dict[str, Any]] = []
        k_rows = technical_rows[-120:]
        if k_rows:
            charts.append({
                "chartId": "stockKlineDaily",
                "title": "日K线与均线",
                "chartType": "CANDLESTICK",
                "period": "DAILY",
                "xAxis": [row.get("tradeDate") for row in k_rows],
                "series": [
                    {"name": "OHLC", "data": [self.pick(row, ["tradeDate", "open", "high", "low", "close", "preClose", "chgAmt", "chgPct", "volume", "turnover", "turnoverRate"]) for row in k_rows], "unit": "元"},
                    {"name": "MA5", "data": [round_or_none(row.get("ma5")) for row in k_rows], "unit": "元"},
                    {"name": "MA20", "data": [round_or_none(row.get("ma20")) for row in k_rows], "unit": "元"},
                    {"name": "EMA20", "data": [round_or_none(row.get("ema20")) for row in k_rows], "unit": "元"},
                ],
            })
            charts.append({
                "chartId": "bollBand",
                "title": "BOLL带",
                "chartType": "LINE",
                "period": "DAILY",
                "xAxis": [row.get("tradeDate") for row in k_rows],
                "series": [
                    {"name": "收盘价", "data": [round_or_none(row.get("close")) for row in k_rows], "unit": "元"},
                    {"name": "BOLL中轨", "data": [round_or_none(row.get("bollMid")) for row in k_rows], "unit": "元"},
                    {"name": "BOLL上轨", "data": [round_or_none(row.get("bollUpper")) for row in k_rows], "unit": "元"},
                    {"name": "BOLL下轨", "data": [round_or_none(row.get("bollLower")) for row in k_rows], "unit": "元"},
                ],
            })

        val_rows = valuation_rows[-120:]
        if val_rows:
            charts.append({
                "chartId": "valuationHistory",
                "title": "估值历史",
                "chartType": "LINE",
                "period": "DAILY",
                "xAxis": [row.get("tradeDate") for row in val_rows],
                "series": [
                    {"name": "PE TTM", "data": [row.get("peTtm") for row in val_rows]},
                    {"name": "行业PE TTM均值", "data": [row.get("industryAveragePeTtm") for row in val_rows]},
                    {"name": "PB", "data": [row.get("pb") for row in val_rows]},
                    {"name": "行业PB均值", "data": [row.get("industryAveragePb") for row in val_rows]},
                ],
            })

        flow_rows = money_flow_rows[-60:]
        if flow_rows:
            charts.append({
                "chartId": "capitalFlowHistory",
                "title": "资金流历史",
                "chartType": "BAR",
                "period": "DAILY",
                "xAxis": [row.get("tradeDate") for row in flow_rows],
                "series": [
                    {"name": "净流入额", "data": [row.get("netMfAmount") for row in flow_rows], "unit": "元"},
                    {"name": "特大单净流入", "data": [self.calc_flow_from_camel(row, "Elg") for row in flow_rows], "unit": "元"},
                    {"name": "大单净流入", "data": [self.calc_flow_from_camel(row, "Lg") for row in flow_rows], "unit": "元"},
                ],
            })

        profit_rows = (financial_detail or {}).get("profitTrendRows") if financial_detail else None
        if profit_rows:
            rows = list(reversed(profit_rows[-12:])) if profit_rows and profit_rows[0].get("endDate") > profit_rows[-1].get("endDate") else profit_rows[-12:]
            charts.append({
                "chartId": "financialProfitTrend",
                "title": "财务利润趋势",
                "chartType": "LINE",
                "period": "MONTHLY",
                "xAxis": [row.get("endDate") for row in rows],
                "series": [
                    {"name": "营业总收入", "data": [row.get("totalRevenue") for row in rows], "unit": "元"},
                    {"name": "归母净利润", "data": [row.get("nIncomeAttrP") for row in rows], "unit": "元"},
                    {"name": "扣非净利润", "data": [row.get("profitDedt") for row in rows], "unit": "元"},
                ],
            })
        return charts

    def build_quote_technical_analysis(
        self,
        technical_rows: List[Dict[str, Any]],
        trade_date: str | None = None,
    ) -> Optional[Dict[str, Any]]:
        """构建技术指标综合分析，字段对齐 targetDetailType.ts。"""
        if not technical_rows:
            return None
        latest = technical_rows[-1]
        close = to_float_or_none(latest.get("close"))
        ma20 = to_float_or_none(latest.get("ma20") or latest.get("MA20"))
        ma60 = to_float_or_none(latest.get("ma60") or latest.get("MA60"))
        macd = to_float_or_none(latest.get("macd"))
        rsi6 = to_float_or_none(latest.get("rsi6") or latest.get("RSI6"))
        boll_upper = to_float_or_none(latest.get("bollUpper"))
        boll_lower = to_float_or_none(latest.get("bollLower"))

        trend_score = 50
        if close is not None and ma20 is not None:
            trend_score = 70 if close >= ma20 else 40
        if close is not None and ma20 is not None and ma60 is not None and close >= ma20 and close >= ma60:
            trend_score = 82
        momentum_score = 70 if macd is not None and macd >= 0 else 45
        if rsi6 is not None and rsi6 >= 80:
            momentum_score = 55
        elif rsi6 is not None and rsi6 <= 20:
            momentum_score = 45
        volatility_score = 55
        if close is not None and boll_upper is not None and close > boll_upper:
            volatility_score = 45
        elif close is not None and boll_lower is not None and close < boll_lower:
            volatility_score = 40
        volume_score = 60
        consistency_score = 60
        score = round((trend_score + momentum_score + volume_score + volatility_score + consistency_score) / 5)
        signal_strength = "STRONG" if score >= 75 else "MEDIUM" if score >= 55 else "WEAK"

        if close is not None and boll_upper is not None and close > boll_upper:
            label = "突破上轨"
            summary = "价格突破 BOLL 上轨，趋势较强但短线波动风险上升。"
        elif close is not None and boll_lower is not None and close < boll_lower:
            label = "跌破下轨"
            summary = "价格跌破 BOLL 下轨，需观察是否出现趋势修复。"
        elif score >= 65:
            label = "震荡偏强"
            summary = "均线和动能指标整体偏强。"
        elif score <= 45:
            label = "震荡偏弱"
            summary = "均线或动能指标偏弱。"
        else:
            label = "中性震荡"
            summary = "多空信号暂未形成一致方向。"

        bullish = sum(1 for flag in [close is not None and ma20 is not None and close >= ma20, macd is not None and macd >= 0] if flag)
        bearish = sum(1 for flag in [close is not None and ma20 is not None and close < ma20, macd is not None and macd < 0] if flag)

        return {
            "conclusion": {
                "label": label,
                "summary": summary,
                "score": score,
                "signalStrength": signal_strength,
                "period": "DAILY",
                "updatedAt": trade_date or latest.get("tradeDate") or to_str_date(latest.get("trade_date")),
                "levels": [
                    {"key": "trend", "label": "趋势", "level": self.score_to_level(trend_score), "tone": "up" if trend_score >= 60 else "down", "text": "收盘价与 MA20/MA60 的相对位置。"},
                    {"key": "momentum", "label": "动能", "level": self.score_to_level(momentum_score), "tone": "up" if momentum_score >= 60 else "down", "text": "MACD 与 RSI 的动能状态。"},
                    {"key": "volatility", "label": "波动", "level": self.score_to_level(volatility_score), "tone": "risk" if volatility_score < 50 else "flat", "text": "BOLL 位置反映波动风险。"},
                ],
            },
            "dimensionScores": [
                {"dimension": "趋势", "score": trend_score, "explanation": "比较收盘价与 MA20、MA60 的位置。"},
                {"dimension": "动能", "score": momentum_score, "explanation": "结合 MACD 柱体与 RSI 水位。"},
                {"dimension": "量能", "score": volume_score, "explanation": "成交量相关指标已在图表中展示，当前采用中性偏稳评分。"},
                {"dimension": "波动", "score": volatility_score, "explanation": "结合 BOLL 上下轨位置判断短线波动风险。"},
                {"dimension": "信号一致性", "score": consistency_score, "explanation": "统计均线、动能等多空信号的一致程度。"},
            ],
            "indicatorSignals": [
                {"indicator": "MA", "status": "收盘价高于MA20" if close is not None and ma20 is not None and close >= ma20 else "收盘价低于或缺少MA20", "direction": "偏多" if close is not None and ma20 is not None and close >= ma20 else "偏空", "explanation": "价格与20日均线的位置关系。"},
                {"indicator": "MACD", "status": "MACD柱为正" if macd is not None and macd >= 0 else "MACD柱为负或缺失", "direction": "偏多" if macd is not None and macd >= 0 else "偏空", "explanation": "短长均线动能差。"},
                {"indicator": "BOLL", "status": label, "direction": "谨慎" if label in {"突破上轨", "跌破下轨"} else "中性", "explanation": "收盘价与布林带上下轨的位置。"},
                {"indicator": "RSI", "status": f"RSI6={round_or_none(rsi6)}" if rsi6 is not None else "RSI6缺失", "direction": "谨慎" if rsi6 is not None and (rsi6 >= 80 or rsi6 <= 20) else "中性", "explanation": "RSI 用于观察超买超卖。"},
            ],
            "signalConflict": {
                "bullishCount": bullish,
                "bearishCount": bearish,
                "neutralCount": max(0, 4 - bullish - bearish),
                "consistency": "信号较一致" if abs(bullish - bearish) >= 2 else "多空信号交织",
                "supports": ["均线趋势", "动能指标"] if bullish >= bearish else [],
                "risks": ["均线压制", "动能走弱"] if bearish > bullish else [],
            },
            "explanation": "技术指标由 indicator_utils.calculate_technical_indicator_rows 基于历史 K 线统一计算。",
        }


    # ------------------------------------------------------------------
    # 财务趋势明细构建
    # ------------------------------------------------------------------

    def build_financial_report_info(self, *row_groups: List[Dict[str, Any]]) -> Optional[Dict[str, Any]]:
        rows = [row for group in row_groups for row in group if row.get("endDate")]
        if not rows:
            return None
        latest = sorted(rows, key=lambda row: row.get("endDate") or "", reverse=True)[0]
        report_type_code = self.infer_report_type(latest.get("endDate"))
        return self.compact_dict({
            "fullSymbol": latest.get("fullSymbol"),
            "endDate": latest.get("endDate"),
            "annDate": latest.get("annDate"),
            "fAnnDate": latest.get("fAnnDate"),
            "reportType": latest.get("reportType"),
            "reportTypeCode": report_type_code,
            "reportTypeText": REPORT_TYPE_TEXT_MAP.get(report_type_code, report_type_code),
        })

    def build_profit_trend_rows(self, income_rows: List[Dict[str, Any]], indicator_rows: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        indicator_map = {row.get("endDate"): row for row in indicator_rows}
        result = []
        for row in income_rows:
            ind = indicator_map.get(row.get("endDate"), {})
            year, quarter = self.extract_year_quarter(row.get("endDate"))
            result.append(self.compact_dict({
                "fullSymbol": row.get("fullSymbol"),
                "endDate": row.get("endDate"),
                "annDate": row.get("annDate"),
                "year": year,
                "quarterCode": quarter,
                "reportTypeText": REPORT_TYPE_TEXT_MAP.get(self.infer_report_type(row.get("endDate"))),
                "totalRevenue": row.get("totalRevenue"),
                "totalRevenueYoy": ind.get("revenueYoy"),
                "nIncomeAttrP": row.get("nIncomeAttrP"),
                "nIncomeAttrPYoy": ind.get("netprofitYoy"),
                "profitDedt": ind.get("profitDedt"),
                "profitDedtYoy": ind.get("dtNetprofitYoy"),
                "eps": ind.get("eps") or row.get("basicEps"),
                "epsYoy": None,
            }))
        return result

    def build_ratio_trend_rows(self, indicator_rows: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        return [self.compact_dict({
            "fullSymbol": row.get("fullSymbol"),
            "endDate": row.get("endDate"),
            "reportTypeText": REPORT_TYPE_TEXT_MAP.get(self.infer_report_type(row.get("endDate"))),
            "roe": row.get("roe"),
            "grossMargin": row.get("grossMargin"),
            "netprofitMargin": row.get("netprofitMargin"),
            "debtToAssets": row.get("debtToAssets"),
        }) for row in indicator_rows]

    def build_cashflow_structure_rows(self, cashflow_rows: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        result = []
        for row in cashflow_rows:
            year, quarter = self.extract_year_quarter(row.get("endDate"))
            result.append(self.compact_dict({
                "fullSymbol": row.get("fullSymbol"),
                "endDate": row.get("endDate"),
                "year": year,
                "quarterCode": quarter,
                "nCashflowAct": row.get("nCashflowAct"),
                "nCashflowInvAct": row.get("nCashflowInvAct"),
                "nCashFlowsFncAct": row.get("nCashFlowsFncAct"),
                "freeCashflow": row.get("freeCashflow"),
            }))
        return result

    def build_balance_structure_rows(self, balance_rows: List[Dict[str, Any]], indicator_rows: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        indicator_map = {row.get("endDate"): row for row in indicator_rows}
        return [self.compact_dict({
            "fullSymbol": row.get("fullSymbol"),
            "endDate": row.get("endDate"),
            "totalCurAssets": row.get("totalCurAssets"),
            "totalNca": row.get("totalNca"),
            "totalAssets": row.get("totalAssets"),
            "totalCurLiab": row.get("totalCurLiab"),
            "totalNcl": row.get("totalNcl"),
            "totalLiab": row.get("totalLiab"),
            "totalHldrEqyExcMinInt": row.get("totalHldrEqyExcMinInt"),
            "debtToAssets": (indicator_map.get(row.get("endDate"), {}) or {}).get("debtToAssets"),
        }) for row in balance_rows]

    def camelize_row(self, row: Mapping[str, Any], date_fields: Optional[set[str]] = None) -> Dict[str, Any]:
        """snake_case 数据库行转 camelCase，日期转字符串，数值转 float。"""
        date_fields = date_fields or set()
        result: Dict[str, Any] = {}
        for key, value in row.items():
            camel_key = self.snake_to_camel(key)
            if key in date_fields:
                result[camel_key] = to_str_date(value)
            elif isinstance(value, (Decimal, int, float)) and not isinstance(value, bool):
                result[camel_key] = round_or_none(value, 6)
            elif isinstance(value, (date, datetime)):
                result[camel_key] = to_str_date(value)
            else:
                result[camel_key] = value
        return self.compact_dict(result)

    def drop_none_top_level(self, data: Dict[str, Any]) -> Dict[str, Any]:
        """清理顶层 None；列表和对象保留空结构，便于前端稳定访问。"""
        return {key: value for key, value in data.items() if value is not None}

    def pick(self, row: Mapping[str, Any], keys: Sequence[str]) -> Dict[str, Any]:
        return {key: row.get(key) for key in keys if key in row}

    def remove_internal_keys(self, row: Dict[str, Any]) -> Dict[str, Any]:
        """移除 service 内部辅助字段，避免暴露到前端。"""
        return {key: value for key, value in row.items() if key not in {"indexCode"}}

    def parse_sw_level(self, value: Any) -> Optional[int]:
        text = str(value or "").strip()
        if text in {"1", "一级行业", "申万一级行业", "L1"}:
            return 1
        if text in {"2", "二级行业", "申万二级行业", "L2"}:
            return 2
        if text in {"3", "三级行业", "申万三级行业", "L3"}:
            return 3
        try:
            number = int(float(text))
            return number if number in {1, 2, 3} else None
        except (TypeError, ValueError):
            return None

    def lookup_industry_name(self, row_map: Dict[str, Dict[str, Any]], code: Any) -> Optional[str]:
        if not code:
            return None
        row = row_map.get(str(code))
        return (row or {}).get("industry_name")

    def classify_related_index(self, row: Mapping[str, Any]) -> Tuple[str, str]:
        text = " ".join(str(row.get(key) or "") for key in ["category", "index_type", "name", "fullname"])
        if "规模" in text or "宽基" in text:
            return "SIZE", "规模指数"
        if "策略" in text:
            return "STRATEGY", "策略指数"
        if "主题" in text:
            return "THEME", "主题指数"
        if str(row.get("index_code") or "").endswith(".SI"):
            return "INDUSTRY", "行业指数"
        category = str(row.get("category") or row.get("index_type") or "OTHER")
        return category, category if category != "OTHER" else "其他指数"

    def estimate_trend_status(self, history: List[Dict[str, Any]]) -> str:
        if len(history) < 20:
            return "UNKNOWN"
        rows = list(reversed(history[:60]))
        closes = [to_float_or_none(row.get("close")) for row in rows]
        closes = [x for x in closes if x is not None]
        if len(closes) < 20:
            return "UNKNOWN"
        ma5 = statistics.fmean(closes[-5:])
        ma20 = statistics.fmean(closes[-20:])
        if ma5 > ma20 * 1.02:
            return "UPTREND"
        if ma5 < ma20 * 0.98:
            return "DOWNTREND"
        return "SIDEWAYS"

    def estimate_valuation_status(
        self,
        pe_ttm: Optional[float],
        pb: Optional[float],
        pe_percentile: Optional[float],
        pb_percentile: Optional[float],
    ) -> str:
        valid_percentiles = [v for v in [pe_percentile, pb_percentile] if v is not None]
        if valid_percentiles:
            avg_pct = statistics.fmean(valid_percentiles)
            if avg_pct <= 30:
                return "UNDERVALUED"
            if avg_pct >= 70:
                return "OVERVALUED"
            return "FAIR"
        if pe_ttm is None and pb is None:
            return "UNKNOWN"
        if pe_ttm is not None and pe_ttm > 80:
            return "OVERVALUED"
        return "FAIR"

    def estimate_confidence(self, quote: Optional[Dict[str, Any]], valuation: Optional[Dict[str, Any]], financial: Optional[Dict[str, Any]]) -> float:
        score = 0.4 if quote else 0.0
        score += 0.3 if valuation else 0.0
        score += 0.3 if financial else 0.0
        return round(score, 2)

    def build_financial_conclusion(self, summary: Dict[str, Any]) -> str:
        revenue_yoy = summary.get("revenueYoy")
        netprofit_yoy = summary.get("netprofitYoy")
        roe = summary.get("roe")
        parts = []
        if revenue_yoy is not None:
            parts.append(f"营收同比{round(revenue_yoy, 2)}%")
        if netprofit_yoy is not None:
            parts.append(f"归母净利润同比{round(netprofit_yoy, 2)}%")
        if roe is not None:
            parts.append(f"ROE为{round(roe, 2)}%")
        return "，".join(parts) + "。" if parts else "财务摘要数据有限。"

    def infer_report_type(self, end_date: Any) -> str:
        text = to_str_date(end_date).replace("-", "")
        suffix = text[-4:]
        if suffix == "0331":
            return "Q1"
        if suffix == "0630":
            return "HALF_YEAR"
        if suffix == "0930":
            return "Q3"
        if suffix == "1231":
            return "ANNUAL"
        return "ANNUAL"

    def extract_year_quarter(self, end_date: Any) -> Tuple[Optional[int], Optional[str]]:
        text = to_str_date(end_date).replace("-", "")
        if len(text) < 8:
            return None, None
        year = int(text[:4])
        suffix = text[-4:]
        quarter = {"0331": "Q1", "0630": "Q2", "0930": "Q3", "1231": "Q4"}.get(suffix)
        return year, quarter

    def build_profile_tags(self, row: Mapping[str, Any], industry_name: Optional[str] = None) -> List[str]:
        return [str(value) for value in [row.get("region"), row.get("market_type"), row.get("is_hs"), row.get("act_ent_type"), industry_name] if value]

    def infer_exchange(self, full_symbol: str) -> str:
        if not full_symbol or "." not in full_symbol:
            return ""
        suffix = full_symbol.split(".")[-1].upper()
        return {"SH": "SSE", "SZ": "SZSE", "BJ": "BSE"}.get(suffix, suffix)

    def snake_to_camel(self, name: str) -> str:
        parts = name.split("_")
        return parts[0] + "".join(part[:1].upper() + part[1:] for part in parts[1:])

    def score_to_level(self, score: float) -> int:
        if score >= 80:
            return 5
        if score >= 65:
            return 4
        if score >= 50:
            return 3
        if score >= 35:
            return 2
        return 1

    def direction_text(self, direction: str) -> str:
        return {
            "BULLISH": "偏多",
            "BEARISH": "偏空",
            "NEUTRAL": "中性",
            "CAUTION": "警戒",
            "SUPPORT_UP": "支撑增强",
            "RISK_UP": "风险上升",
        }.get(direction, direction)

    def calc_flow_from_camel(self, row: Mapping[str, Any], prefix: str) -> Optional[float]:
        buy = to_float_or_none(row.get(f"buy{prefix}Amount"))
        sell = to_float_or_none(row.get(f"sell{prefix}Amount"))
        if buy is None and sell is None:
            return None
        return round_or_none((buy or 0) - (sell or 0), 2)