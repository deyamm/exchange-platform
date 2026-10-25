from __future__ import annotations

import json
import logging
import math
import statistics
from collections import defaultdict
from typing import Any, Dict, List, Mapping, Optional, Sequence, Tuple
from redis.asyncio import Redis

from sqlalchemy.ext.asyncio import AsyncConnection

from app.repository.basic_data_repo import BasicDataRepo
from app.repository.exchange_data_repo import ExchangeDataRepo
from app.repository.finance_data_repo import FinanceDataRepo
from app.utils.datetime_utils import get_current_datetime, resolve_trade_date
from app.utils.indicator_utils import (
    build_index_range_performance,
    calculate_technical_indicator_rows,
    estimate_risk_level_placeholder,
    round_or_none,
    to_float_or_none,
)
from app.utils.target_detail_utils import (
    build_market_quote,
    build_performance,
    build_valuation_conclusion,
    compact_dict,
    estimate_trend_status,
    estimate_valuation_status,
    metric_card,
    percentile,
    round_optional,
    strip_exchange,
)
from app.utils.typing_utils import to_str_date

logger = logging.getLogger(__name__)

INDEX_MARKET_NAME_MAP = {
    "MSCI": "MSCI指数",
    "CSI": "中证指数",
    "SSE": "上交所指数",
    "SZSE": "深交所指数",
    "CICC": "中金指数",
    "SW": "申万指数",
    "OTH": "其他指数",
}

INDEX_EXCHANGE_NAME_MAP = {
    "SSE": "上海证券交易所",
    "SZSE": "深圳证券交易所",
    "CSI": "中证指数有限公司",
    "MSCI": "MSCI",
    "CICC": "中金公司",
    "SW": "申万宏源研究",
    "SH": "上海证券交易所",
    "SZ": "深圳证券交易所",
}

CHANGE_RANGE_DEFS: Sequence[Tuple[str, str, float, float, int]] = (
    # rangeCode, label, min, max, direction
    ("up-gt-10", ">10%", 10.0, 100.0, 1),
    ("up-7-10", "7-10%", 7.0, 10.0, 1),
    ("up-5-7", "5-7%", 5.0, 7.0, 1),
    ("up-3-5", "3-5%", 3.0, 5.0, 1),
    ("up-0-3", "0-3%", 0.0, 3.0, 1),
    ("flat", "0", 0.0, 0.0, 0),
    ("down-0-3", "0-3%", -3.0, 0.0, -1),
    ("down-3-5", "3-5%", -5.0, -3.0, -1),
    ("down-5-7", "5-7%", -7.0, -5.0, -1),
    ("down-7-10", "7-10%", -10.0, -7.0, -1),
    ("down-gt-10", ">10%", -100.0, -10.0, -1),
)


class IndexService:
    """指数详情服务。

    设计边界：
    - BasicDataRepo 只访问 type_basic_data：指数基础信息、指数成分、申万行业层级、股票基础信息；
    - ExchangeDataRepo 只访问 type_exchange_data：指数/股票行情、估值与规模指标；
    - FinanceDataRepo 只访问 type_finance_data：当前指数详情暂无财务数据，保留扩展入口；
    - IndexService 负责跨库组合、申万行业层级聚合、技术指标计算调用和前端 IndexDetail 结构组装。
    """

    compact_dict = staticmethod(compact_dict)
    build_performance = staticmethod(build_performance)
    build_valuation_conclusion = staticmethod(build_valuation_conclusion)
    estimate_trend_status = staticmethod(estimate_trend_status)
    estimate_valuation_status = staticmethod(estimate_valuation_status)
    metric_card = staticmethod(metric_card)
    percentile = staticmethod(percentile)
    round_optional = staticmethod(round_optional)
    strip_exchange = staticmethod(strip_exchange)

    def __init__(
        self,
        basic_conn: AsyncConnection,
        exchange_conn: AsyncConnection,
        finance_conn: AsyncConnection,
        redis_client: Redis
    ):
        self.basic_repo = BasicDataRepo(basic_conn)
        self.exchange_repo = ExchangeDataRepo(exchange_conn)
        self.finance_repo = FinanceDataRepo(finance_conn)
        self.redis_client = redis_client

    async def get_index_detail(
        self,
        full_symbol: str,
        trade_date: str | None = None
    ) -> Dict[str, Any]:
        """构建前端 ``indexDetailType.IndexDetail`` 所需的数据结构，该接口只返回基础信息和最新quote相关内容，具体行情、技术指标、成分股由其他接口提供。

        参数说明：
        - full_symbol：指数完整代码，优先按 index_basic_data.full_symbol 匹配；
        - trade_date：可选查询日期，未传时由 resolve_trade_date 统一兜底；
        - range_code：行情走势页区间表现口径，默认 1Y；
        - include_*：用于接口降级或压测时控制可选模块，默认全部返回。
        """

        clean_symbol = full_symbol.strip()
        requested_trade_date = resolve_trade_date(trade_date)
        
        basic = await self.basic_repo.get_index_basic_detail(clean_symbol)
        
        resolved_symbol = (basic or {}).get("full_symbol") or clean_symbol
        profile = self.build_index_profile(basic, resolved_symbol)

        # 指数行情快照与历史行情均来自 exchange 库。quote 中已经 LEFT JOIN index_indicator_daily，
        # 因此 indicator 可直接用 quote 作为最新源，再结合 indicator_history 计算估值分位。
        quote = await self.exchange_repo.get_index_latest_snapshot(resolved_symbol, requested_trade_date)
        resolved_trade_date = to_str_date((quote or {}).get("trade_date")) or requested_trade_date

        market_quote = build_market_quote(
            quote,
            price_unit="点",
            include_full_symbol=True,
            include_turnover_rate_free=True,
        )
        # 指定交易日（最新）的指标信息
        indicator = self.build_indicator_info(quote)

        # 页面中指数综合观察的内容
        overview = self.build_overview(quote, indicator, None, None)
        # 页面中的指标卡片
        metric_cards = self.build_metric_cards(quote, indicator)

        # Finance repo 当前只提供占位扩展入口，不在 service 中拼接任何跨库 SQL。
        finance_bundle = await self.finance_repo.get_index_financial_bundle(resolved_symbol)
        warnings = self.build_warnings(basic, quote, indicator)

        return self.compact_dict({
            "profile": profile,
            "quote": market_quote,
            "indicator": indicator,
            "overview": overview,
            "metricCards": metric_cards,
            "meta": {
                "dataSource": "type_basic_data,type_exchange_data,type_finance_data",
                "dataVersion": resolved_trade_date,
                "generatedAt": get_current_datetime().isoformat(),
                "partialData": bool(warnings),
                "warnings": warnings,
                "financeAvailable": any(finance_bundle.values()),
            },
        })

    async def get_index_market(self, full_symbol: str, trade_date: str | None, history_limit: int = 250) -> Dict[str, Any]:
        """获取指数行情和技术指标历史数据，返回给前端绘制行情走势图和技术指标图表。"""
        
        clean_symbol = full_symbol.strip()
        resolved_trade_date = resolve_trade_date(trade_date)
        # 在基础 limit 的基础上增加 250 条历史数据，保证技术指标计算时有足够的前置数据。
        history_extra = 250

        # 读取历史行情和指标
        history_desc = await self.exchange_repo.get_index_history(clean_symbol, resolved_trade_date, limit=history_limit+history_extra)
        indicator_history = await self.exchange_repo.get_index_indicator_history(clean_symbol, resolved_trade_date, limit=history_limit+history_extra)

        # 由于取的history包含一些额外的前置数据，因此在返回给前端时需要截取到指定的历史 limit 条数。
        technical_rows = self.build_technical_rows(history_desc)[-history_limit:]

        # 根据各技术指标来分析当前指数的技术面情况，返回给前端展示
        quote_analysis = self.build_quote_technical_analysis(technical_rows, resolved_trade_date)

        # 构建前端所需的各个行情详情的charts数据
        market_charts = self.build_market_charts(technical_rows, indicator_history)
        return self.compact_dict({
            "marketCharts": market_charts,
            "quoteAnalysis": quote_analysis,
            "rangePerformance": build_index_range_performance(history_desc, window_size=history_limit),
            "meta": {
                "dataSource": "type_basic_data,type_exchange_data,type_finance_data",
                "dataVersion": resolved_trade_date,
                "generatedAt": get_current_datetime().isoformat(),
            },
        })
    
    async def get_index_constituent(self, full_symbol: str, trade_date: str | None) -> Optional[Dict[str, Any]]:
        """获取指数成分股信息，兼容普通指数和申万行业指数两种权重口径。包括成分股相关的图表数据
        成分股列表以分页形式返回，该函数只返回 page=1、page_size=10 的成分股数据，具体分页数据由 get_index_constituent_page 接口提供。
        同时会把完整成分股 records 列表缓存到 Redis，缓存内容只包含成分股列表本身。
        """

        clean_symbol = full_symbol.strip()
        resolved_trade_date = resolve_trade_date(trade_date)
        industry_context = await self.load_industry_context(clean_symbol)

        # 对于行业详情页面的内容
        constituents = await self.load_and_build_constituents(
            index_code=clean_symbol,
            trade_date=resolved_trade_date,
            industry_context=industry_context,
        )

        if constituents:
            all_records = list(constituents.get("records") or [])
            await self.cache_index_constituent_records(clean_symbol, resolved_trade_date, all_records)

            # 首次接口只返回第一页，避免一次性把所有成分股返回给前端。
            constituents = dict(constituents)
            constituents["records"] = all_records[:10]
        else:
            await self.cache_index_constituent_records(clean_symbol, resolved_trade_date, [])

        return self.compact_dict({
            "constituents": constituents,
            "meta": {
                "dataSource": "type_basic_data,type_exchange_data,type_finance_data",
                "dataVersion": resolved_trade_date,
                "generatedAt": get_current_datetime().isoformat(),
            },
        })

    async def get_index_constituent_page(self, full_symbol: str, trade_date: str | None, page: int = 1, page_size: int = 10) -> List[Dict[str, Any]]:
        """从 Redis List 中分页返回指定页码的成分股数据。

        如果缓存不存在，则显式调用 get_index_constituent 生成缓存；本函数只返回分页后的 list，
        不额外封装 page、page_size、total 等分页元信息。
        """

        clean_symbol = full_symbol.strip()
        resolved_trade_date = resolve_trade_date(trade_date)
        normalized_page = max(int(page or 1), 1)
        normalized_page_size = max(int(page_size or 10), 1)

        cached_count = await self.get_cached_index_constituent_count(clean_symbol, resolved_trade_date)
        if cached_count == 0:
            await self.get_index_constituent(clean_symbol, resolved_trade_date)
            cached_count = await self.get_cached_index_constituent_count(clean_symbol, resolved_trade_date)

        if cached_count == 0:
            return []

        start = (normalized_page - 1) * normalized_page_size
        end = start + normalized_page_size - 1
        return await self.get_cached_index_constituent_records(
            clean_symbol,
            resolved_trade_date,
            start=start,
            end=end,
        )

    def build_index_constituent_cache_key(self, full_symbol: str, trade_date: str) -> str:
        """构建指数成分股 records 列表缓存 key。"""
        return f"index:constituents:{full_symbol}:{trade_date}"

    async def cache_index_constituent_records(
        self,
        full_symbol: str,
        trade_date: str,
        records: Sequence[Mapping[str, Any]],
    ) -> None:
        """使用 Redis List 缓存完整成分股 records 列表。

        List 中每个元素是一条成分股记录的 JSON 字符串；只缓存 records，不缓存 constituents 中的统计、
        图表等其他属性。写入前先删除旧 key，确保同一指数同一交易日的缓存顺序与最新 records 完全一致。
        """

        cache_key = self.build_index_constituent_cache_key(full_symbol, trade_date)
        pipe = self.redis_client.pipeline()
        pipe.delete(cache_key)

        serialized_records = [
            json.dumps(dict(record), ensure_ascii=False, default=str)
            for record in records or []
        ]
        if serialized_records:
            pipe.rpush(cache_key, *serialized_records)

        await pipe.execute()

    async def get_cached_index_constituent_count(
        self,
        full_symbol: str,
        trade_date: str,
    ) -> int:
        """读取 Redis List 中缓存的成分股数量。"""
        cache_key = self.build_index_constituent_cache_key(full_symbol, trade_date)
        return int(await self.redis_client.llen(cache_key) or 0)

    async def get_cached_index_constituent_records(
        self,
        full_symbol: str,
        trade_date: str,
        start: int = 0,
        end: int = -1,
    ) -> List[Dict[str, Any]]:
        """使用 Redis LRANGE 读取指定范围内的成分股 records。"""
        cache_key = self.build_index_constituent_cache_key(full_symbol, trade_date)
        cached_items = await self.redis_client.lrange(cache_key, start, end)
        if not cached_items:
            return []

        records: List[Dict[str, Any]] = []
        for item in cached_items:
            if isinstance(item, bytes):
                item = item.decode("utf-8")

            try:
                record = json.loads(item)
            except (TypeError, json.JSONDecodeError):
                logger.warning("Invalid index constituent cache item: %s", cache_key)
                continue

            if isinstance(record, dict):
                records.append(record)

        return records

    def build_index_profile(
        self,
        row: Optional[Dict[str, Any]],
        fallback_symbol: str,
    ) -> Dict[str, Any]:
        """构建 IndexProfile，字段严格使用前端 camelCase 命名。"""
        row = row or {}
        market = row.get("market") or self.infer_exchange(fallback_symbol)
        return self.compact_dict({
            "fullSymbol": row.get("full_symbol") or fallback_symbol,
            "name": row.get("name") or row.get("fullname") or fallback_symbol,
            "fullname": row.get("fullname"),
            "market": market,
            "marketName": INDEX_MARKET_NAME_MAP.get(str(market or "").upper(), market),
            "publisher": row.get("publisher"),
            "indexType": row.get("index_type"),
            "category": row.get("category"),
            "baseDate": to_str_date(row.get("base_date")),
            "basePoint": self.round_optional(row.get("base_point")),
            "listDate": to_str_date(row.get("list_date")),
            "weightRule": row.get("weight_rule"),
            "description": row.get("description"),
            "expDate": to_str_date(row.get("exp_date")),
            "tags": self.build_profile_tags(row),
        })

    def build_indicator_info(
        self,
        quote: Optional[Dict[str, Any]],
    ) -> Optional[Dict[str, Any]]:
        """构建 IndexIndicatorDaily 指数估值与规模指标内容，并附加页面层估值状态与结论。
        包括最新市值、股本、换手率、pe、pb等，该函数返回最新日的指标
        indicator_history 用于计算 pe、pb 的分位数，辅助估值状态判断。
        TODO: 目前从数据源采集的指数指标只局限于大盘指数，行业指数、主题指数等还需要进一步的研究和扩展。
        """
        if not quote:
            return None

        source = quote
        pe_ttm = self.round_optional(source.get("pe_ttm"))
        pb = self.round_optional(source.get("pb"))
        # pe_percentile = self.percentile(pe_ttm, [row.get("pe_ttm") for row in indicator_history])
        pe_percentile = None
        # pb_percentile = self.percentile(pb, [row.get("pb") for row in indicator_history])
        pb_percentile = None
        valuation_status = self.estimate_valuation_status(pe_ttm, pb, pe_percentile, pb_percentile)

        return self.compact_dict({
            "fullSymbol": source.get("full_symbol"),
            "tradeDate": to_str_date(source.get("trade_date")),
            "totalMarketValue": self.round_optional(source.get("total_market_value"), 2),
            "floatMarketValue": self.round_optional(source.get("float_market_value"), 2),
            "totalShare": self.round_optional(source.get("total_share"), 2),
            "floatShare": self.round_optional(source.get("float_share"), 2),
            "freeShare": self.round_optional(source.get("free_share"), 2),
            "turnoverRate": self.round_optional(source.get("turnover_rate")),
            "turnoverRateFree": self.round_optional(source.get("turnover_rate_free")),
            "pe": self.round_optional(source.get("pe")),
            "peTtm": pe_ttm,
            "pb": pb,
            "valuationStatus": valuation_status,
            "conclusion": self.build_valuation_conclusion(valuation_status, pe_ttm, pb, pe_percentile, pb_percentile),
        })

    async def load_industry_context(self, index_code: str) -> Dict[str, Any]:
        """读取申万行业层级上下文，service 层基于层级关系做 L1/L2/L3 聚合。

        index_industry_level 的 parent_code 存 industry_code。
        """
        rows = await self.basic_repo.get_sw_industry_level_rows()
        row_by_industry = {row.get("industry_code"): row for row in rows if row.get("industry_code")}

        # 申万行业分类树，根据数据源，parent_code = 0 是根节点
        children_by_parent: Dict[str, List[Dict[str, Any]]] = defaultdict(list)
        current: Optional[Dict[str, Any]] = None

        for row in rows:
            if row.get("index_code") == index_code:
                current = row
            parent_code = row.get("parent_code")
            if parent_code:
                children_by_parent[parent_code].append(row)

        # 这里只根据所查询 index_code 是否在申万行业层级表中来判断
        is_sw_industry = bool(current)

        descendant_l3_codes = self.resolve_descendant_l3_codes(current, children_by_parent) if is_sw_industry else []

        return {
            "rows": rows,
            "rowByIndustry": row_by_industry,
            "childrenByParent": children_by_parent,
            "currentRow": current,
            "isIndustryIndex": is_sw_industry,
            "descendantL3Codes": descendant_l3_codes,
        }

    def resolve_descendant_l3_codes(
        self,
        current: Optional[Mapping[str, Any]],
        children_by_parent: Mapping[str, List[Dict[str, Any]]],
    ) -> List[str]:
        """根据申万层级行解析当前行业指数下属三级行业指数代码。如果当前就是 L3，则返回自身 index_code。"""
        
        if not current:
            return []
        if str(current.get("level", "")).upper() == "L3":
            return [current.get("index_code", '')] if current.get("index_code") else []

        queue = [dict(current)]
        result: List[str] = []
        seen: set[str] = set()
        # BFS 遍历申万行业层级树，收集 current 所有下属 L3 行的 index_code
        while queue:
            node = queue.pop(0)
            node_key = node.get("industry_code")
            if not node_key or node_key in seen:
                continue
            seen.add(node_key)
            if str(node.get("level", "")).upper() == "L3" and node.get("index_code"):
                result.append(node["index_code"])
                continue
            child_candidates = []
            if node.get("industry_code"):
                child_candidates.extend(children_by_parent.get(node["industry_code"], []))
            queue.extend(child_candidates)
        return list(dict.fromkeys(result))

    async def load_and_build_constituents(
        self,
        index_code: str,
        trade_date: str,
        industry_context: Mapping[str, Any],
    ) -> Optional[Dict[str, Any]]:
        """读取并加工成分股数据，兼容普通指数和申万行业指数两种权重口径。"""
        is_industry_index = bool(industry_context.get("isIndustryIndex"))
        descendant_l3_codes = list(industry_context.get("descendantL3Codes") or [])

        # 如果是申万行业，需要先读取所有下属 L3 成分，再在本函数中按股票去查询对应的成分股。
        if is_industry_index and descendant_l3_codes:
            component_rows = await self.basic_repo.get_index_components_by_indices(descendant_l3_codes, trade_date=trade_date)
        else:
            component_rows = await self.basic_repo.get_index_components(index_code, trade_date=trade_date)

        if not component_rows:
            return None

        component_rows = self.deduplicate_component_rows(component_rows)

        raw_stock_codes = sorted({row.get("stock_code", '') for row in component_rows if row.get("stock_code")})
        stock_basic_map = await self.basic_repo.get_stock_basic_by_codes(raw_stock_codes)
        basic_lookup = self.build_stock_basic_lookup(stock_basic_map)
        quote_codes = set(raw_stock_codes)
        quote_codes.update(row.get("full_symbol", '') for row in stock_basic_map.values() if row.get("full_symbol"))
        stock_quote_rows = await self.exchange_repo.get_stock_latest_snapshots_by_codes(sorted(quote_codes), trade_date=trade_date)
        stock_quote_map = {row.get("full_symbol", ''): row for row in stock_quote_rows if row.get("full_symbol")}
        membership_rows = await self.basic_repo.get_stock_sw_industry_memberships_by_codes(raw_stock_codes)
        stock_industry_map = self.build_stock_industry_lookup(membership_rows, industry_context)

        return self.build_constituents(
            current_index_code=index_code,
            component_rows=component_rows,
            basic_lookup=basic_lookup,
            stock_quote_map=stock_quote_map,
            stock_industry_map=stock_industry_map,
            is_industry_index=is_industry_index,
            industry_context=industry_context,
        )

    def deduplicate_component_rows(self, rows: Sequence[Mapping[str, Any]]) -> List[Dict[str, Any]]:
        """同一股票在聚合口径中仅保留一行，避免 L1/L2 聚合时重复计数。"""
        deduped: Dict[str, Dict[str, Any]] = {}
        for row in rows:
            stock_code = row.get("stock_code")
            if not stock_code:
                continue
            current = deduped.get(stock_code)
            if current is None or str(row.get("trade_date") or "") > str(current.get("trade_date") or ""):
                deduped[stock_code] = dict(row)
        return list(deduped.values())

    def build_stock_basic_lookup(self, stock_basic_map: Mapping[str, Mapping[str, Any]]) -> Dict[str, Dict[str, Any]]:
        """把 stock_basic_data 结果同时按 full_symbol / symbol 建索引。"""
        lookup: Dict[str, Dict[str, Any]] = {}
        for row in stock_basic_map.values():
            item = dict(row)
            for key in [item.get("full_symbol"), item.get("symbol"), self.strip_exchange(item.get("full_symbol") or "")]:
                if key:
                    lookup[key] = item
        return lookup

    def build_stock_industry_lookup(
        self,
        membership_rows: Sequence[Mapping[str, Any]],
        industry_context: Mapping[str, Any],
    ) -> Dict[str, Dict[str, Any]]:
        """将股票对应的申万三级行业补齐为 L1/L2/L3 展示字段。"""
        row_by_industry: Mapping[str, Mapping[str, Any]] = industry_context.get("rowByIndustry") or {}

        # membership_rows 已按 trade_date DESC 排序，同一股票取第一条三级行业归属。
        selected: Dict[str, Mapping[str, Any]] = {}
        for row in membership_rows:
            stock_code = row.get("stock_code")
            if stock_code and stock_code not in selected:
                selected[stock_code] = row

        result: Dict[str, Dict[str, Any]] = {}
        for stock_code, l3_row in selected.items():
            l2 = self.find_parent_industry_row(l3_row, row_by_industry)
            l1 = self.find_parent_industry_row(l2, row_by_industry) if l2 else None
            item = self.compact_dict({
                "industryL1": (l1 or {}).get("industry_name"),
                "industryL1Code": (l1 or {}).get("index_code", '') or (l1 or {}).get("industry_code", ''),
                "industryL2": (l2 or {}).get("industry_name"),
                "industryL2Code": (l2 or {}).get("index_code") or (l2 or {}).get("industry_code"),
                "industryL3": (l3_row or {}).get("industry_name"),
                "industryL3Code": (l3_row or {}).get("index_code") or (l3_row or {}).get("industry_code"),
            })
            result[stock_code] = item
            stripped = self.strip_exchange(stock_code)
            if stripped:
                result[stripped] = item
        return result

    def find_parent_industry_row(
        self,
        row: Optional[Mapping[str, Any]],
        row_by_industry: Mapping[str, Mapping[str, Any]],
    ) -> Optional[Mapping[str, Any]]:
        """根据 parent_code 查找父级行业行。"""
        if not row:
            return None
        parent_code = row.get("parent_code")
        if not parent_code:
            return None
        return row_by_industry.get(parent_code)

    def build_constituents(
        self,
        current_index_code: str,
        component_rows: Sequence[Mapping[str, Any]],
        basic_lookup: Mapping[str, Mapping[str, Any]],
        stock_quote_map: Mapping[str, Mapping[str, Any]],
        stock_industry_map: Mapping[str, Mapping[str, Any]],
        is_industry_index: bool,
        industry_context: Mapping[str, Any],
    ) -> Optional[Dict[str, Any]]:
        """组装 IndexConstituentInfo。

        - 普通指数：weight 直接使用 index_constituent_data.weight；
        - 申万行业指数：weight 使用成分股 float_market_value / 当前指数成分总流通市值；
        - L1/L2 申万指数：先读取所有下属 L3 成分，再在本函数中按股票去重和聚合。
        """
        if not component_rows:
            return None

        # 申万行业指数权重按流通市值计算，先收集分母。
        total_float_mv = 0.0
        if is_industry_index:
            for row in component_rows:
                stock_code = row.get("stock_code") or ""
                basic = basic_lookup.get(stock_code, {})
                resolved_stock = basic.get("full_symbol") or stock_code
                quote = stock_quote_map.get(resolved_stock) or stock_quote_map.get(stock_code) or {}
                total_float_mv += to_float_or_none(quote.get("float_market_value")) or 0.0

        records: List[Dict[str, Any]] = []
        for row in component_rows:
            raw_stock_code = row.get("stock_code") or ""
            basic = basic_lookup.get(raw_stock_code, {})
            resolved_stock_code = basic.get("full_symbol") or raw_stock_code
            quote = stock_quote_map.get(resolved_stock_code) or stock_quote_map.get(raw_stock_code) or {}
            industry = (
                stock_industry_map.get(raw_stock_code)
                or stock_industry_map.get(resolved_stock_code)
                or stock_industry_map.get(self.strip_exchange(raw_stock_code))
                or stock_industry_map.get(self.strip_exchange(resolved_stock_code))
                or {}
            )

            float_mv = to_float_or_none(quote.get("float_market_value"))
            if is_industry_index:
                weight = round(float_mv / total_float_mv * 100, 4) if float_mv is not None and total_float_mv > 0 else None
            else:
                weight = self.round_optional(row.get("weight"), 4)

            chg_pct = self.round_optional(quote.get("chg_pct"), 4)
            contribution = round(weight * chg_pct / 100, 4) if weight is not None and chg_pct is not None else None
            industry_l3 = industry.get("industryL3") or basic.get("industry")

            records.append(self.compact_dict({
                "indexCode": current_index_code,
                "sourceIndexCode": row.get("index_code") if row.get("index_code") != current_index_code else None,
                "stockCode": resolved_stock_code,
                "rawStockCode": raw_stock_code if raw_stock_code != resolved_stock_code else None,
                "tradeDate": to_str_date(row.get("trade_date")),
                "weight": weight,
                "name": basic.get("name"),
                "industry": industry_l3,
                "industryL1": industry.get("industryL1"),
                "industryL1Code": industry.get("industryL1Code"),
                "industryL2": industry.get("industryL2"),
                "industryL2Code": industry.get("industryL2Code"),
                "industryL3": industry_l3,
                "industryL3Code": industry.get("industryL3Code"),
                "close": self.round_optional(quote.get("close")),
                "chgPct": chg_pct,
                "totalMarketValue": self.round_optional(quote.get("total_market_value"), 2),
                "floatMarketValue": self.round_optional(float_mv, 2),
                "peTtm": self.round_optional(quote.get("pe_ttm")),
                "pb": self.round_optional(quote.get("pb")),
                "turnoverRate": self.round_optional(quote.get("turnover_rate")),
                "contribution": contribution,
            }))

        records.sort(key=lambda item: self.number_for_sort(item.get("weight")), reverse=True)
        total = len(records)
        up_count = sum(1 for item in records if self.number_for_sort(item.get("chgPct")) > 0)
        down_count = sum(1 for item in records if self.number_for_sort(item.get("chgPct")) < 0)
        flat_count = total - up_count - down_count

        top_weight_values = [to_float_or_none(item.get("weight")) or 0.0 for item in records[:10]]
        industry_weights = None if is_industry_index else self.build_regular_industry_weights(records)
        industry_tree = self.build_industry_tree(records, industry_context) if is_industry_index else None

        return self.compact_dict({
            "tradeDate": records[0].get("tradeDate") if records else None,
            "totalCount": total,
            "upCount": up_count,
            "downCount": down_count,
            "flatCount": flat_count,
            "topTenWeightConcentration": round(sum(top_weight_values), 4),
            "industryWeightMode": "INDUSTRY_INDEX" if is_industry_index else "REGULAR_INDEX",
            "industryWeights": industry_weights,
            "industryTree": industry_tree,
            "changeRanges": self.build_change_ranges(records),
            "breadthStats": self.build_breadth_stats(up_count, down_count, flat_count, total),
            "limitStats": self.build_limit_stats(records),
            "topWeights": records[:10],
            "topGainers": sorted(records, key=lambda item: self.number_for_sort(item.get("chgPct")), reverse=True)[:10],
            "topLosers": sorted(records, key=lambda item: self.number_for_sort(item.get("chgPct")))[:10],
            "records": records,
        })

    def build_regular_industry_weights(self, records: Sequence[Mapping[str, Any]]) -> List[Dict[str, Any]]:
        """普通指数默认按申万一级行业聚合权重；缺少申万映射时退回 stock_basic_data.industry。"""
        buckets: Dict[str, Dict[str, Any]] = {}
        for item in records:
            name = item.get("industryL1") or item.get("industry") or "未分类"
            code = item.get("industryL1Code") or name
            bucket = buckets.setdefault(str(name), {"name": name, "code": code, "weight": 0.0, "constituentCount": 0})
            bucket["weight"] += to_float_or_none(item.get("weight")) or 0.0
            bucket["constituentCount"] += 1
        return sorted(
            [self.compact_dict({**bucket, "level": "SW_L1", "weight": round(bucket["weight"], 4)}) for bucket in buckets.values()],
            key=lambda item: self.number_for_sort(item.get("weight")),
            reverse=True,
        )

    def build_industry_tree(
        self,
        records: Sequence[Mapping[str, Any]],
        industry_context: Mapping[str, Any],
    ) -> List[Dict[str, Any]]:
        """构建申万行业指数的 L1/L2/L3/股票树，节点权重由股票权重向上汇总。"""
        current = industry_context.get("currentRow") or {}
        current_level = str(current.get("level") or "").upper()
        root_levels = {"L1": ["L1", "L2", "L3"], "L2": ["L2", "L3"], "L3": ["L3"]}.get(current_level, ["L1", "L2", "L3"])

        root_map: Dict[Tuple[str, str], Dict[str, Any]] = {}

        def get_or_create(parent: Optional[Dict[str, Any]], level: str, code: str, name: str) -> Dict[str, Any]:
            container = parent.setdefault("_childMap", {}) if parent is not None else root_map
            key = (level, code or name)
            if key not in container:
                container[key] = {
                    "name": name or "未分类",
                    "code": code or name,
                    "level": f"SW_{level}",
                    "weight": 0.0,
                    "floatMarketValue": 0.0,
                    "_stocks": set(),
                    "children": [],
                }
            return container[key]

        for item in records:
            parent = None
            for level in root_levels:
                name = item.get(f"industry{level}") or (current.get("industry_name") if current_level == level else None) or "未分类"
                code = item.get(f"industry{level}Code") or (current.get("index_code") if current_level == level else None) or name
                parent = get_or_create(parent, level, str(code), str(name))
                parent["weight"] += to_float_or_none(item.get("weight")) or 0.0
                parent["floatMarketValue"] += to_float_or_none(item.get("floatMarketValue")) or 0.0
                parent["_stocks"].add(item.get("stockCode"))

            if parent is not None:
                parent["children"].append(self.compact_dict({
                    "name": item.get("name") or item.get("stockCode"),
                    "code": item.get("stockCode"),
                    "level": "STOCK",
                    "weight": item.get("weight"),
                    "constituentCount": 1,
                    "floatMarketValue": item.get("floatMarketValue"),
                    "stockCode": item.get("stockCode"),
                }))

        def materialize(node: Dict[str, Any]) -> Dict[str, Any]:
            child_map = node.pop("_childMap", {})
            children = list(child_map.values()) + node.get("children", [])
            children = [materialize(child) if "_stocks" in child else child for child in children]
            children.sort(key=lambda child: self.number_for_sort(child.get("weight")), reverse=True)
            stocks = node.pop("_stocks", set())
            return self.compact_dict({
                "name": node.get("name"),
                "code": node.get("code"),
                "level": node.get("level"),
                "weight": round(node.get("weight") or 0.0, 4),
                "constituentCount": len([x for x in stocks if x]),
                "floatMarketValue": round(node.get("floatMarketValue") or 0.0, 2),
                "children": children,
            })

        roots = [materialize(node) for node in root_map.values()]
        roots.sort(key=lambda node: self.number_for_sort(node.get("weight")), reverse=True)
        return roots

    def build_change_ranges(self, records: Sequence[Mapping[str, Any]]) -> List[Dict[str, Any]]:
        """按涨跌幅分布构建区间统计。"""
        total = len(records)
        values = [to_float_or_none(item.get("chgPct")) or 0.0 for item in records]
        result = []
        for code, label, lower, upper, direction in CHANGE_RANGE_DEFS:
            if direction == 0:
                count = sum(1 for value in values if value == 0)
            else:
                count = sum(1 for value in values if lower < value <= upper)
            result.append({"rangeCode": code, "label": label, "count": count, "ratio": self.ratio(count, total), "direction": direction})
        return result

    def build_breadth_stats(self, up_count: int, down_count: int, flat_count: int, total: int) -> List[Dict[str, Any]]:
        """构建涨跌平盘数量与占比。"""
        return [
            {"key": "UP", "label": "上涨", "count": up_count, "ratio": self.ratio(up_count, total)},
            {"key": "DOWN", "label": "下跌", "count": down_count, "ratio": self.ratio(down_count, total)},
            {"key": "FLAT", "label": "平盘", "count": flat_count, "ratio": self.ratio(flat_count, total)},
        ]

    def build_limit_stats(self, records: Sequence[Mapping[str, Any]]) -> Dict[str, Any]:
        """按 A 股常用 9.8% 阈值粗略统计涨跌停数量。"""
        total = len(records)
        limit_up = sum(1 for item in records if self.number_for_sort(item.get("chgPct")) >= 9.8)
        limit_down = sum(1 for item in records if self.number_for_sort(item.get("chgPct")) <= -9.8)
        return {
            "limitUpCount": limit_up,
            "limitDownCount": limit_down,
            "limitUpRatio": self.ratio(limit_up, total),
            "limitDownRatio": self.ratio(limit_down, total),
        }

    def build_metric_cards(
        self,
        quote: Optional[Dict[str, Any]],
        indicator: Optional[Dict[str, Any]],
    ) -> List[Dict[str, Any]]:
        """构建首页顶部指标卡，字段对齐 MetricCard。"""
        cards = [
            self.metric_card("close", "最新点位", (quote or {}).get("close"), "点", sort_order=1),
            self.metric_card("chgPct", "涨跌幅", (quote or {}).get("chg_pct"), "%", sort_order=2),
            self.metric_card("turnover", "成交额", (quote or {}).get("turnover"), "千元", sort_order=3),
            self.metric_card("turnoverRate", "换手率", (indicator or {}).get("turnoverRate"), "%", sort_order=4),
            self.metric_card("peTtm", "市盈率TTM", (indicator or {}).get("peTtm"), None, sort_order=5),
            self.metric_card("pb", "市净率", (indicator or {}).get("pb"), None, sort_order=6),
            self.metric_card("totalMarketValue", "总市值", (indicator or {}).get("totalMarketValue"), "元", sort_order=7),
            # self.metric_card("totalCount", "成分数量", (constituents or {}).get("totalCount"), "只", sort_order=8),
        ]
        return [card for card in cards if card.get("value") is not None]

    def build_overview(
        self,
        quote: Optional[Dict[str, Any]],
        indicator: Optional[Dict[str, Any]],
        constituents: Optional[Dict[str, Any]],
        quote_analysis: Optional[Dict[str, Any]],
    ) -> Dict[str, Any]:
        """构建 IndexOverview。风险等级目前走占位入口，便于后续替换模型/风控规则。"""
        chg_pct = to_float_or_none((quote or {}).get("chg_pct"))
        trend_status = self.estimate_trend_status([])
        valuation_status = (indicator or {}).get("valuationStatus") or "UNKNOWN"
        breadth_status = self.estimate_breadth_status(constituents)
        risk_level = estimate_risk_level_placeholder()

        score = 0
        score += 1 if trend_status == "UPTREND" else -1 if trend_status == "DOWNTREND" else 0
        score += 1 if valuation_status in {"UNDERVALUED", "FAIR"} else -1 if valuation_status == "OVERVALUED" else 0
        score += 1 if breadth_status == "BROAD_UP" else -1 if breadth_status == "BROAD_DOWN" else 0
        score += 1 if (chg_pct or 0.0) >= 2 else -1 if (chg_pct or 0.0) <= -2 else 0

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
        if chg_pct is not None:
            reasons.append(f"最新涨跌幅为{round(chg_pct, 2)}%")
        if quote_analysis:
            reasons.append(quote_analysis.get("conclusion", {}).get("summary") or "技术指标已完成计算")
        if valuation_status != "UNKNOWN":
            reasons.append(f"估值状态为{valuation_status}")
        if constituents:
            reasons.append(f"成分股上涨{constituents.get('upCount', 0)}只，下跌{constituents.get('downCount', 0)}只")

        risks = []
        if valuation_status == "OVERVALUED":
            risks.append("估值处于偏高区间，注意估值回归风险")
        if breadth_status == "BROAD_DOWN":
            risks.append("成分股多数下跌，指数上涨持续性可能不足")
        if risk_level in {"HIGH", "EXTREME"}:
            risks.append("风险等级偏高，需要关注回撤和波动放大")
        if not risks:
            risks.append("暂无明显极端风险信号，仍需结合市场环境持续跟踪")

        return {
            "overallView": overall_view,
            "overallViewText": overall_text,
            "valuationStatus": valuation_status,
            "riskLevel": risk_level,
            "confidence": self.estimate_confidence(quote, indicator, quote_analysis),
            "summary": "；".join(reasons) if reasons else "当前可用数据有限，建议等待更多行情和成分股数据确认。",
            "reasons": reasons,
            "risks": risks,
            "suggestions": [
                "结合成交额、换手率和成分股涨跌分布观察信号延续性",
                "普通指数权重来自 index_constituent_data；申万行业指数权重来自成分股流通市值占比",
                "风险等级目前为预留入口返回值，后续可替换为模型或风控规则",
            ],
        }

    def build_technical_rows(self, history_desc: Sequence[Mapping[str, Any]]) -> List[Dict[str, Any]]:
        """将 repo 返回的倒序行情转为升序，并调用 indicator_utils 计算 MA/EMA/BOLL/MACD/KDJ/RSI。
        这里需要包含足够的前置数据，避免计算指标时出现空值。
        """
        rows_asc = [dict(row) for row in reversed(history_desc)]
        return calculate_technical_indicator_rows(rows_asc)

    def build_quote_technical_analysis(self, technical_rows: Sequence[Mapping[str, Any]], trade_date: str) -> Dict[str, Any]:
        """构建 QuoteTechnicalAnalysis，供行情走势页直接展示。"""
        latest = technical_rows[-1] if technical_rows else {}
        close = to_float_or_none(latest.get("close"))
        ma20 = to_float_or_none(latest.get("ma20"))
        ma60 = to_float_or_none(latest.get("ma60"))
        macd_diff = to_float_or_none(latest.get("macdDiff"))
        macd_dea = to_float_or_none(latest.get("macdDea"))
        rsi6 = to_float_or_none(latest.get("rsi6"))
        boll_upper = to_float_or_none(latest.get("bollUpper"))
        boll_lower = to_float_or_none(latest.get("bollLower"))

        trend_score = 50
        if close is not None and ma20 is not None:
            trend_score += 20 if close >= ma20 else -15
        if ma20 is not None and ma60 is not None:
            trend_score += 15 if ma20 >= ma60 else -15
        trend_score = self.clamp_score(trend_score)

        momentum_score = 50
        if macd_diff is not None and macd_dea is not None:
            momentum_score += 20 if macd_diff >= macd_dea else -20
        if rsi6 is not None:
            if rsi6 >= 80:
                momentum_score -= 10
            elif rsi6 <= 20:
                momentum_score += 10
        momentum_score = self.clamp_score(momentum_score)

        volatility_score = 50
        if close is not None and boll_upper is not None and boll_lower is not None:
            if close > boll_upper:
                volatility_score = 35
            elif close < boll_lower:
                volatility_score = 45
            else:
                volatility_score = 65

        score = round(statistics.fmean([trend_score, momentum_score, volatility_score]), 2)
        label = "强势上涨" if score >= 75 else "震荡偏强" if score >= 60 else "中性震荡" if score >= 45 else "偏弱调整"
        strength = "STRONG" if score >= 75 or score < 35 else "MEDIUM" if score >= 55 or score < 45 else "WEAK"

        indicator_signals = [
            self.build_indicator_signal("MA", trend_score, "收盘价与均线位置决定趋势方向"),
            self.build_indicator_signal("MACD", momentum_score, "MACD diff/dea 用于观察动能变化"),
            self.build_indicator_signal("BOLL", volatility_score, "价格在 BOLL 带内外用于判断波动与位置风险"),
        ]
        bullish_count = sum(1 for item in indicator_signals if item["direction"] == "偏多")
        bearish_count = sum(1 for item in indicator_signals if item["direction"] == "偏空")
        neutral_count = len(indicator_signals) - bullish_count - bearish_count

        return {
            "conclusion": {
                "label": label,
                "summary": f"技术指标综合得分{score}，当前判断为{label}。",
                "score": score,
                "signalStrength": strength,
                "period": "DAILY",
                "updatedAt": trade_date,
                "levels": [
                    {"key": "trend", "label": "趋势", "level": self.score_to_level(trend_score), "tone": self.score_to_tone(trend_score), "text": f"趋势得分{trend_score}"},
                    {"key": "momentum", "label": "动能", "level": self.score_to_level(momentum_score), "tone": self.score_to_tone(momentum_score), "text": f"动能得分{momentum_score}"},
                    {"key": "volatility", "label": "波动", "level": self.score_to_level(volatility_score), "tone": self.score_to_tone(volatility_score), "text": f"波动得分{volatility_score}"},
                ],
            },
            "dimensionScores": [
                {"dimension": "趋势", "score": trend_score, "explanation": "基于 close、MA20、MA60 的相对位置计算。"},
                {"dimension": "动能", "score": momentum_score, "explanation": "基于 MACD 与 RSI 的动能状态计算。"},
                {"dimension": "波动", "score": volatility_score, "explanation": "基于价格相对 BOLL 上下轨的位置计算。"},
            ],
            "indicatorSignals": indicator_signals,
            "signalConflict": {
                "bullishCount": bullish_count,
                "bearishCount": bearish_count,
                "neutralCount": neutral_count,
                "consistency": "高" if max(bullish_count, bearish_count, neutral_count) >= 2 else "中",
                "supports": [item["indicator"] for item in indicator_signals if item["direction"] == "偏多"],
                "risks": [item["indicator"] for item in indicator_signals if item["direction"] == "偏空"],
            },
            "explanation": "MA、EMA、BOLL、SAR、VWAP、SUPER_TREND、VOL、TURNOVER_RATE、MACD、KDJ、RSI、CCI、DMI、ATR、OBV 等可计算指标来自 indicator_utils；风险等级等不可直接计算指标保留占位入口。",
        }

    def build_indicator_signal(self, indicator: str, score: float, explanation: str) -> Dict[str, Any]:
        """把指标分数转成前端展示信号。"""
        if score >= 60:
            status, direction = "BULLISH", "偏多"
        elif score <= 45:
            status, direction = "BEARISH", "偏空"
        else:
            status, direction = "NEUTRAL", "中性"
        return {"indicator": indicator, "status": status, "direction": direction, "explanation": explanation}

    def build_market_charts(
        self,
        technical_rows: Sequence[Mapping[str, Any]],
        indicator_history: Sequence[Mapping[str, Any]],
    ) -> List[Dict[str, Any]]:
        """构建 ChartGroup 列表，历史行情中附带技术指标序列。

        技术指标统一来自 indicator_utils.calculate_technical_indicator_rows。
        前端字段结构保持 chartId/title/chartType/period/xAxis/series，不再把多个主图指标混在
        indexTechnicalIndicators 一个图表里，而是按指标和周期拆成独立 chart。
        """
        charts: List[Dict[str, Any]] = []
        k_rows = list(technical_rows)

        def number_series(field: str, digits: int = 4) -> List[Optional[float]]:
            return [round_or_none(row.get(field), digits) for row in k_rows]

        def append_indicator_chart(
            chart_id: str,
            title: str,
            chart_type: str,
            x_axis: List[str],
            series: List[Dict[str, Any]],
        ) -> None:
            charts.append({
                "chartId": chart_id,
                "title": title,
                "chartType": chart_type,
                "period": "DAILY",
                "xAxis": x_axis,
                "series": series,
            })

        if k_rows:
            x_axis = [to_str_date(row.get("trade_date")) for row in k_rows]
            # k线图
            charts.append({
                "chartId": "indexKlineDaily",
                "title": "指数日K线",
                "chartType": "CANDLESTICK",
                "period": "DAILY",
                "xAxis": x_axis,
                "series": [{
                    "name": "OHLC",
                    "unit": "点",
                    "data": [self.build_ohlc_item(row) for row in k_rows],
                }],
                "options": {
                    "mainIndicators": ["MA", "EMA", "BOLL", "SAR", "VWAP", "SUPER_TREND"],
                    "subIndicators": ["VOL", "TURNOVER_RATE", "MACD", "KDJ", "RSI", "CCI", "DMI", "ATR", "OBV"],
                },
            })

            # 主图指标：MA
            for period in (5, 10, 20, 60, 120, 250):
                append_indicator_chart(
                    chart_id=f"MA{period}",
                    title=f"MA{period}",
                    chart_type="LINE",
                    x_axis=x_axis,
                    series=[{"name": f"MA{period}", "data": number_series(f"MA{period}"), "unit": "点"}],
                )

            # 主图指标：EMA。保留旧的 EMA20 字段，同时追加前端默认 9/12/26。
            for period in (9, 12, 20, 26):
                append_indicator_chart(
                    chart_id=f"EMA{period}",
                    title=f"EMA{period}",
                    chart_type="LINE",
                    x_axis=x_axis,
                    series=[{"name": f"EMA{period}", "data": number_series(f"EMA{period}"), "unit": "点"}],
                )

            # 主图指标：BOLL
            append_indicator_chart(
                chart_id="BOLL",
                title="BOLL",
                chart_type="LINE",
                x_axis=x_axis,
                series=[
                    {"name": "upper", "data": number_series("bollUpper"), "unit": "点"},
                    {"name": "mid", "data": number_series("bollMid"), "unit": "点"},
                    {"name": "lower", "data": number_series("bollLower"), "unit": "点"},
                ],
            )

            # 主图指标：SAR
            append_indicator_chart(
                chart_id="SAR",
                title="SAR",
                chart_type="LINE",
                x_axis=x_axis,
                series=[{"name": "SAR", "data": number_series("SAR"), "unit": "点"}],
            )

            # 主图指标：VWAP
            for period in (1, 5, 10, 20, 60, 120):
                append_indicator_chart(
                    chart_id=f"VWAP{period}",
                    title=f"VWAP{period}",
                    chart_type="LINE",
                    x_axis=x_axis,
                    series=[{"name": f"VWAP{period}", "data": number_series(f"VWAP{period}"), "unit": "点"}],
                )

            # 主图指标：SuperTrend
            append_indicator_chart(
                chart_id="SUPER_TREND",
                title="SUPER_TREND",
                chart_type="LINE",
                x_axis=x_axis,
                series=[
                    {"name": "SuperTrend", "data": number_series("superTrend10"), "unit": "点"},
                    {"name": "SuperTrend Upper", "data": number_series("superTrendUpper10"), "unit": "点"},
                    {"name": "SuperTrend Lower", "data": number_series("superTrendLower10"), "unit": "点"},
                ],
            )

            # 副图指标：VOL
            append_indicator_chart(
                chart_id="VOL",
                title="VOL",
                chart_type="BAR",
                x_axis=x_axis,
                series=[{"name": "volume", "data": [self.round_optional(row.get("volume"), 2) for row in k_rows], "unit": "股/手"}],
            )

            # 保留原有成交额历史图，避免影响前端已有功能。
            charts.append({
                "chartId": "TURNOVER",
                "title": "指数成交额",
                "chartType": "BAR",
                "period": "DAILY",
                "xAxis": x_axis,
                "series": [{"name": "turnover", "data": [self.round_optional(row.get("turnover"), 2) for row in k_rows], "unit": "元"}],
            })

            # 副图指标：TURNOVER_RATE
            append_indicator_chart(
                chart_id="TURNOVER_RATE",
                title="TURNOVER_RATE",
                chart_type="LINE",
                x_axis=x_axis,
                series=[{"name": "turnoverRate", "data": number_series("turnoverRate"), "unit": "%"}],
            )

            # 副图指标：MACD
            append_indicator_chart(
                chart_id="MACD",
                title="MACD",
                chart_type="LINE",
                x_axis=x_axis,
                series=[
                    {"name": "DIF", "data": number_series("macdDiff"), "unit": "点"},
                    {"name": "DEA", "data": number_series("macdDea"), "unit": "点"},
                    {"name": "MACD", "data": number_series("macd"), "unit": "点"},
                ],
            )

            # 副图指标：KDJ
            append_indicator_chart(
                chart_id="KDJ",
                title="KDJ",
                chart_type="LINE",
                x_axis=x_axis,
                series=[
                    {"name": "K", "data": number_series("kdjK"), "unit": "点"},
                    {"name": "D", "data": number_series("kdjD"), "unit": "点"},
                    {"name": "J", "data": number_series("kdjJ"), "unit": "点"},
                ],
            )

            # 副图指标：RSI
            append_indicator_chart(
                chart_id="RSI",
                title="RSI",
                chart_type="LINE",
                x_axis=x_axis,
                series=[{"name": "RSI14", "data": number_series("RSI14"), "unit": "点"}],
            )

            # 副图指标：CCI
            append_indicator_chart(
                chart_id="CCI",
                title="CCI",
                chart_type="LINE",
                x_axis=x_axis,
                series=[{"name": "CCI14", "data": number_series("CCI14"), "unit": "点"}],
            )

            # 副图指标：DMI
            append_indicator_chart(
                chart_id="DMI",
                title="DMI",
                chart_type="LINE",
                x_axis=x_axis,
                series=[
                    {"name": "+DI", "data": number_series("dmiPdi14"), "unit": "点"},
                    {"name": "-DI", "data": number_series("dmiMdi14"), "unit": "点"},
                    {"name": "ADX", "data": number_series("dmiAdx14"), "unit": "点"},
                ],
            )

            # 副图指标：ATR
            append_indicator_chart(
                chart_id="ATR",
                title="ATR",
                chart_type="LINE",
                x_axis=x_axis,
                series=[{"name": "ATR14", "data": number_series("ATR14"), "unit": "点"}],
            )

            # 副图指标：OBV
            append_indicator_chart(
                chart_id="OBV",
                title="OBV",
                chart_type="LINE",
                x_axis=x_axis,
                series=[
                    {"name": "OBV", "data": number_series("OBV", 2), "unit": "点"},
                    {"name": "OBV14", "data": number_series("OBV14", 2), "unit": "点"},
                ],
            )

        # 估值list没有日期升序排序，所以需要倒序再计算
        valuation_rows = list(reversed(list(indicator_history)))
        if valuation_rows:
            charts.append({
                "chartId": "indexValuationHistory",
                "title": "指数估值历史",
                "chartType": "LINE",
                "period": "DAILY",
                "xAxis": [to_str_date(row.get("trade_date")) for row in valuation_rows],
                "series": [
                    {"name": "peTtm", "data": [self.round_optional(row.get("pe_ttm")) for row in valuation_rows]},
                    {"name": "pb", "data": [self.round_optional(row.get("pb")) for row in valuation_rows]},
                ],
            })
        return charts

    def build_constituent_charts(self, constituents: Dict[str, Any]) -> List[Dict[str, Any]]:
        charts: List[Dict[str, Any]] = []
        weight_items = constituents.get("industryWeights") or constituents.get("industryTree") or []
        if weight_items:
            charts.append({
                "chartId": "constituentIndustryWeights",
                "title": "成分股行业权重",
                "chartType": "PIE",
                "period": "DAILY",
                "series": [{
                    "name": "industryWeights",
                    "unit": "%",
                    "data": [{"name": item.get("name"), "value": item.get("weight")} for item in weight_items],
                }],
            })
        if constituents.get("breadthStats"):
            charts.append({
                "chartId": "constituentBreadth",
                "title": "成分股涨跌分布",
                "chartType": "PIE",
                "period": "DAILY",
                "series": [{
                    "name": "breadth",
                    "unit": "只",
                    "data": [{"name": item.get("label"), "value": item.get("count")} for item in constituents.get("breadthStats", [])],
                }],
            })
        return charts
    
    def build_ohlc_item(self, row: Mapping[str, Any]) -> Dict[str, Any]:
        """构建单根 K 线数据，附带常用技术指标字段。"""
        return self.compact_dict({
            "tradeDate": to_str_date(row.get("trade_date")),
            "open": self.round_optional(row.get("open")),
            "high": self.round_optional(row.get("high")),
            "low": self.round_optional(row.get("low")),
            "close": self.round_optional(row.get("close")),
            "preClose": self.round_optional(row.get("pre_close")),
            "chgAmt": self.round_optional(row.get("chg_amt")),
            "chgPct": self.round_optional(row.get("chg_pct")),
            "volume": self.round_optional(row.get("volume"), 2),
            "turnover": self.round_optional(row.get("turnover"), 2),
            "ma5": round_or_none(row.get("ma5")),
            "ma10": round_or_none(row.get("ma10")),
            "ma20": round_or_none(row.get("ma20")),
            "ma60": round_or_none(row.get("ma60")),
            "ma120": round_or_none(row.get("ma120")),
            "ma250": round_or_none(row.get("ma250")),
            "ema9": round_or_none(row.get("ema9")),
            "ema12": round_or_none(row.get("ema12")),
            "ema20": round_or_none(row.get("ema20")),
            "ema26": round_or_none(row.get("ema26")),
            "bollMid": round_or_none(row.get("bollMid")),
            "bollUpper": round_or_none(row.get("bollUpper")),
            "bollLower": round_or_none(row.get("bollLower")),
            "sar": round_or_none(row.get("sar")),
            "sarTrend": row.get("sarTrend"),
            "vwap1": round_or_none(row.get("vwap1")),
            "vwap5": round_or_none(row.get("vwap5")),
            "vwap10": round_or_none(row.get("vwap10")),
            "vwap20": round_or_none(row.get("vwap20")),
            "vwap60": round_or_none(row.get("vwap60")),
            "vwap120": round_or_none(row.get("vwap120")),
            "superTrend10": round_or_none(row.get("superTrend10")),
            "superTrendUpper10": round_or_none(row.get("superTrendUpper10")),
            "superTrendLower10": round_or_none(row.get("superTrendLower10")),
            "superTrendDirection10": row.get("superTrendDirection10"),
            "turnoverRate": round_or_none(row.get("turnoverRate")),
            "macdDiff": round_or_none(row.get("macdDiff")),
            "macdDea": round_or_none(row.get("macdDea")),
            "macd": round_or_none(row.get("macd")),
            "kdjK": round_or_none(row.get("kdjK")),
            "kdjD": round_or_none(row.get("kdjD")),
            "kdjJ": round_or_none(row.get("kdjJ")),
            "rsi6": round_or_none(row.get("rsi6")),
            "rsi12": round_or_none(row.get("rsi12")),
            "rsi14": round_or_none(row.get("rsi14")),
            "rsi24": round_or_none(row.get("rsi24")),
            "cci14": round_or_none(row.get("cci14")),
            "dmiPdi14": round_or_none(row.get("dmiPdi14")),
            "dmiMdi14": round_or_none(row.get("dmiMdi14")),
            "dmiAdx14": round_or_none(row.get("dmiAdx14")),
            "atr14": round_or_none(row.get("atr14")),
            "obv": round_or_none(row.get("obv"), 2),
            "obv14": round_or_none(row.get("obv14"), 2),
        })

    def build_warnings(
        self,
        basic: Optional[Dict[str, Any]],
        quote: Optional[Dict[str, Any]],
        indicator: Optional[Dict[str, Any]],
    ) -> List[str]:
        """根据关键数据缺失情况生成 ResponseMeta.warnings。"""
        warnings = []
        if not basic:
            warnings.append("index_basic_data 未找到指数基础信息")
        if not quote:
            warnings.append("index_k_daily 未找到指数行情快照")
        if not indicator:
            warnings.append("index_indicator_daily 未找到指数估值和规模指标")
        return warnings

    def estimate_breadth_status(self, constituents: Optional[Dict[str, Any]]) -> str:
        """使用成分股上涨/下跌占比粗略判断市场宽度。"""
        if not constituents or not constituents.get("totalCount"):
            return "UNKNOWN"
        total = to_float_or_none(constituents.get("totalCount")) or 0.0
        if total <= 0:
            return "UNKNOWN"
        up_ratio = (to_float_or_none(constituents.get("upCount")) or 0.0) / total
        down_ratio = (to_float_or_none(constituents.get("downCount")) or 0.0) / total
        if up_ratio >= 0.65:
            return "BROAD_UP"
        if down_ratio >= 0.65:
            return "BROAD_DOWN"
        return "MIXED"

    def estimate_confidence(
        self,
        quote: Optional[Dict[str, Any]],
        indicator: Optional[Dict[str, Any]],
        quote_analysis: Optional[Dict[str, Any]],
    ) -> float:
        """根据行情、估值、成分股、技术分析四类数据齐备情况估计置信度。"""
        score = 0.35 if quote else 0.0
        score += 0.25 if indicator else 0.0
        score += 0.15 if quote_analysis else 0.0
        return round(score, 2)

    def build_profile_tags(self, row: Mapping[str, Any]) -> List[str]:
        """指数基础资料标签，过滤空值并去重。"""
        values = [row.get("market"), row.get("category"), row.get("index_type"), row.get("weight_rule")]
        return list(dict.fromkeys(str(value) for value in values if value))

    def infer_exchange(self, full_symbol: str) -> str:
        """从 full_symbol 后缀推断交易所或指数市场。"""
        if not full_symbol or "." not in full_symbol:
            return ""
        suffix = full_symbol.split(".")[-1].upper()
        if suffix == "SH":
            return "SSE"
        if suffix == "SZ":
            return "SZSE"
        if suffix == "SI":
            return "SW"
        return suffix

    @staticmethod
    def ratio(count: int, total: int) -> float:
        """数量占比，单位百分比。"""
        return round(count / total * 100, 4) if total else 0.0

    @staticmethod
    def number_for_sort(value: Any) -> float:
        """排序用数值转换，None 统一视为 0。"""
        number = to_float_or_none(value)
        return number if number is not None else 0.0

    @staticmethod
    def clamp_score(score: float) -> float:
        """限制技术评分在 0-100。"""
        return round(max(0.0, min(100.0, score)), 2)

    @staticmethod
    def score_to_level(score: float) -> int:
        """把 0-100 分映射成 1-5 级。"""
        return max(1, min(5, int(math.ceil(score / 20))))

    @staticmethod
    def score_to_tone(score: float) -> str:
        """把指标得分映射成前端 tone。"""
        if score >= 60:
            return "up"
        if score <= 45:
            return "down"
        return "flat"