import statistics
import logging
from typing import Any, Dict, List, Optional, Tuple

from sqlalchemy.ext.asyncio import AsyncConnection

from app.repository.basic_data_repo import BasicDataRepo
from app.repository.exchange_data_repo import ExchangeDataRepo
from app.utils.datetime_utils import (
    current_market_status,
    get_current_datetime,
    resolve_trade_date,
    get_previous_trade_date,
)
from app.utils.market_utils import (
    build_component_rise_stats,
    build_distribution_tabs,
    build_hot_stock_list,
    build_stock_snapshot_map,
)
from app.utils.typing_utils import pct, to_float, to_str_date

logger = logging.getLogger(__name__)

MAJOR_INDEX_CODES = [
    "000001.SH",
    "399001.SZ",
    "399006.SZ",
    "000680.SH",
    "000016.SH",
    "000300.SH",
    "000905.SH",
    "932000.CSI",
]


class MarketService:
    """
        由fastapi依赖注入的async connection。
        basic_conn连接basic_data_repo，exchange_conn连接exchange_data_repo。

        性能说明：
        - 市场总览接口会先读取一次全市场股票快照；
        - 后续指数成分股涨跌统计、领涨股计算优先复用该快照；
        - 只有在没有传入快照的单独调用场景下，才回退到repo查询成分股行情。
    """
    def __init__(self, basic_conn: AsyncConnection, exchange_conn: AsyncConnection):
        self.basic_conn = basic_conn
        self.exchange_conn = exchange_conn

        # 根据表所在数据库分别创建repo实例，service层负责组合跨库查询结果。
        self.basic_repo = BasicDataRepo(basic_conn)
        self.exchange_repo = ExchangeDataRepo(exchange_conn)

    async def get_market_overview(self, trade_date: str | None) -> Dict[str, Any]:
        """
            trade_date: 指定交易日期，格式 YYYY-MM-DD；为空时返回最新数据。

            查询结构：
            1. 读取一次全市场股票快照；
            2. 补充股票基础信息；
            3. 构建stock_snapshot_map，供市场概况、热股、分布、指数成分股统计共同复用；
            4. 指数成分股只查basic库中的成分股关系，不再为每个指数重复查询stock_k_daily。
        """
        logger.info(f"查询市场概览，trade_date={trade_date}")

        # 未传入 trade_date 时，统一使用 datetime_utils.resolve_trade_date 获取最近交易日；
        # 传入历史日期时保持原值，避免查询日期被二次回退。
        latest_trade_date = resolve_trade_date(trade_date)

        # 一次性读取全市场股票快照，后续计算尽量复用该结果，避免大量重复查库。
        stock_rows = await self.exchange_repo.get_stock_snapshot(latest_trade_date)
        stock_rows = await self.enrich_stock_rows_with_basic_data(stock_rows)
        # 构建以full_symbol为key的股票快照字典，供后续指数成分股统计复用，避免重复查询行情库。
        stock_snapshot_map = build_stock_snapshot_map(stock_rows)

        # 成分股缓存用于避免同一个指数在“成分股统计”和“领涨股”中重复查basic库。
        component_cache: Dict[str, List[Dict[str, Any]]] = {}

        return {
            "marketSummary": await self.build_market_summary(latest_trade_date, stock_rows),
            "majorIndexList": await self.get_major_index_list(
                trade_date=latest_trade_date,
                stock_snapshot_map=stock_snapshot_map,
                component_cache=component_cache,
            ),
            "coreIndustryIndexList": [],
            "hotStockList": build_hot_stock_list(stock_rows, limit=10),
            "distributionTabs": build_distribution_tabs(stock_rows),
        }

    async def enrich_stock_rows_with_basic_data(self, stock_rows: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """
            将股票基础信息合并到股票行情快照。
            该逻辑组合了basic_data_repo和exchange_data_repo的结果，因此放在service层。
        """
        stock_basic_map = await self.basic_repo.get_stock_basic_map()

        enriched_rows = []
        for row in stock_rows:
            item = dict(row)
            stock_code = item.get("full_symbol", '')
            basic_info = stock_basic_map.get(stock_code, {})

            item["name"] = item.get("name") or basic_info.get("name") or stock_code or ""
            item["industry"] = item.get("industry") or basic_info.get("industry") or "未分类"

            enriched_rows.append(item)

        return enriched_rows

    async def build_market_summary(self, trade_date: str, stock_rows: List[Dict[str, Any]]) -> Dict[str, Any]:
        """
            构建市场概况。

            说明：
            - 当前函数只依赖已读取的stock_rows进行当日统计；
            - 额外查一次前一交易日成交额，用于amountChangePct；
            - 涨跌停数量暂按±9.8%近似统计，后续若接入涨跌停价字段可替换为精确口径。
        """
        valid = [row for row in stock_rows if row.get("pct_chg") is not None]
        pct_values = [to_float(row["pct_chg"]) for row in valid]

        total_count = len(valid)
        rise_count = sum(1 for value in pct_values if value > 0)
        limit_up_count = sum(1 for value in pct_values if value >= 9.8)
        limit_down_count = sum(1 for value in pct_values if value <= -9.8)

        total_amount = sum(to_float(row.get("amount")) for row in stock_rows)

        prev_date = get_previous_trade_date(trade_date)
        # 仅额外查询一次前一交易日全市场总成交额，避免逐股票或逐模块重复查询。
        prev_amount = await self.exchange_repo.get_total_stock_amount(prev_date.strftime("%Y-%m-%d"))

        return {
            "tradeDate": trade_date,
            "marketStatus": current_market_status(),
            "updateTime": get_current_datetime().strftime("%H:%M:%S"),
            "marketScope": "A 股全市场",
            "limitUpCount": limit_up_count,
            "limitDownCount": limit_down_count,
            "riseRatio": round(rise_count / total_count * 100, 2) if total_count else 0,
            "totalAmount": round(total_amount, 2),
            "amountChangePct": round(pct(total_amount, prev_amount), 2),
            "avgChangePct": round(sum(pct_values) / len(pct_values), 2) if pct_values else 0,
            "medianChangePct": round(statistics.median(pct_values), 2) if pct_values else 0,
        }

    async def get_index_components_cached(
        self,
        index_code: str,
        component_cache: Optional[Dict[str, List[Dict[str, Any]]]] = None,
    ) -> List[Dict[str, Any]]:
        """
            查询并缓存指数成分股。
            在总览接口中，同一个指数的成分股会被统计和领涨股计算共同使用，因此需要缓存。
        """
        if component_cache is not None and index_code in component_cache:
            return component_cache[index_code]

        components = await self.basic_repo.get_index_components(index_code)

        if component_cache is not None:
            component_cache[index_code] = components

        return components

    async def get_index_component_stats(
        self,
        index_code: str,
        trade_date: str,
        stock_snapshot_map: Optional[Dict[str, Dict[str, Any]]] = None,
        components: Optional[List[Dict[str, Any]]] = None,
    ) -> Dict[str, Any]:
        """
            构建指数成分股涨跌统计。

            优先级：
            1. 如果传入stock_snapshot_map，则直接从全市场快照中取成分股行情；
            2. 如果没有快照，才调用exchange_repo.get_stock_k_rows查询成分股行情。

            这样既保证market_overview避免大量查库，也保留单独调用该函数时的通用性。
        """
        if components is None:
            components = await self.basic_repo.get_index_components(index_code)

        stock_codes = [row.get("stock_code", '') for row in components if row.get("stock_code")]

        if stock_snapshot_map is not None:
            stock_rows = [
                stock_snapshot_map[stock_code]
                for stock_code in stock_codes
                if stock_code in stock_snapshot_map
            ]
        else:
            # 兜底路径：用于没有预加载全市场快照的单指数详情等场景。
            stock_rows = await self.exchange_repo.get_stock_k_rows(stock_codes, trade_date)

        return build_component_rise_stats(stock_rows)

    async def get_index_leading_stock(
        self,
        index_code: str,
        trade_date: str,
        stock_snapshot_map: Optional[Dict[str, Dict[str, Any]]] = None,
        components: Optional[List[Dict[str, Any]]] = None,
    ) -> Tuple[str, float]:
        """
            查询指数成分股中的领涨股票。

            在market_overview中优先复用已补充name的stock_snapshot_map；
            没有快照时，才回退为按成分股代码查询行情并补充股票名称。
        """
        if components is None:
            components = await self.basic_repo.get_index_components(index_code)

        stock_codes = [row.get("stock_code", '') for row in components if row.get("stock_code")]

        if stock_snapshot_map is not None:
            stock_rows = [
                stock_snapshot_map[stock_code]
                for stock_code in stock_codes
                if stock_code in stock_snapshot_map
            ]
        else:
            # 兜底路径：用于没有预加载全市场快照的单独调用场景。
            stock_rows = await self.exchange_repo.get_stock_k_rows(stock_codes, trade_date)

        valid_rows = [row for row in stock_rows if row.get("pct_chg") is not None]
        if not valid_rows:
            return "", 0.0

        leader = max(valid_rows, key=lambda row: to_float(row.get("pct_chg")))
        leader_code = leader.get("full_symbol") or ""

        # 如果来自全市场快照，enrich_stock_rows已经补齐name；否则再少量补查一次基础信息。
        leader_name = leader.get("name")
        if not leader_name and leader_code:
            stock_basic_map = await self.basic_repo.get_stock_basic_by_codes([leader_code])
            leader_name = stock_basic_map.get(leader_code, {}).get("name")

        return leader_name or leader_code or "", round(to_float(leader.get("pct_chg")), 2)

    async def build_index_item(
        self,
        row: Dict[str, Any],
        is_industry: bool = False,
        stock_snapshot_map: Optional[Dict[str, Dict[str, Any]]] = None,
        component_cache: Optional[Dict[str, List[Dict[str, Any]]]] = None,
    ) -> Dict[str, Any]:
        """
            构建指数列表单项结果。

            该过程会组合：
            - exchange库的指数行情；
            - basic库的指数基础信息；
            - basic库的成分股关系；
            - 股票快照中的成分股行情。

            因此该组合逻辑放在service层。
        """
        full_symbol = row.get("full_symbol") or ""
        trade_date = to_str_date(row.get("trade_date"))
        index_name = row.get("name") or row.get("fullname") or full_symbol

        history = await self.exchange_repo.get_index_history(full_symbol, trade_date, limit=21)

        current_close = to_float(row.get("close"))
        pre_close = to_float(row.get("pre_close"))
        high = to_float(row.get("high"))
        low = to_float(row.get("low"))

        amplitude = round((high - low) / pre_close * 100, 2) if pre_close else 0.0

        # 历史数据不足时5日/20日涨跌幅固定为0.0，避免因数据不足导致异常。
        change_pct_5d = round(pct(current_close, history[5].get("close")), 2) if len(history) > 5 else 0.0
        change_pct_20d = round(pct(current_close, history[20].get("close")), 2) if len(history) > 20 else 0.0

        components = await self.get_index_components_cached(
            index_code=full_symbol,
            component_cache=component_cache,
        )

        component_stats = await self.get_index_component_stats(
            index_code=full_symbol,
            trade_date=trade_date,
            stock_snapshot_map=stock_snapshot_map,
            components=components,
        )

        leading_name = ""
        leading_pct = 0.0
        if is_industry:
            leading_name, leading_pct = await self.get_index_leading_stock(
                index_code=full_symbol,
                trade_date=trade_date,
                stock_snapshot_map=stock_snapshot_map,
                components=components,
            )

        return {
            "indexCode": full_symbol,
            "indexName": index_name,
            "indexType": row.get("index_type") or row.get("category") or "指数",
            "market": row.get("market") or "",
            "latestPoint": round(current_close, 2),
            "preClosePoint": round(pre_close, 2),
            "changePoint": round(to_float(row.get("change")), 2),
            "changePct": round(to_float(row.get("pct_chg")), 2),
            "amplitude": amplitude,
            "amount": round(to_float(row.get("amount")), 2),
            "changePct5d": change_pct_5d,
            "changePct20d": change_pct_20d,
            "componentRiseRatio": component_stats["componentRiseRatio"],
            "updateTime": get_current_datetime().strftime("%H:%M:%S"),
            "leadingStockName": leading_name,
            "leadingStockChangePct": leading_pct,
        }

    async def get_major_index_list(
        self,
        trade_date: str,
        stock_snapshot_map: Optional[Dict[str, Dict[str, Any]]] = None,
        component_cache: Optional[Dict[str, List[Dict[str, Any]]]] = None,
    ) -> List[Dict[str, Any]]:
        """
            获取主要指数列表。
            index_k_daily来自exchange库，index_basic_data来自basic库，service层按full_symbol合并。
        """
        rows = await self.exchange_repo.get_index_k_rows(MAJOR_INDEX_CODES, trade_date)
        index_basic_map = await self.basic_repo.get_index_basic_map(
            [row.get("full_symbol", '') for row in rows if row.get("full_symbol")]
        )

        enriched_rows = []
        for row in rows:
            item = dict(row)
            full_symbol = item.get("full_symbol", '')
            basic_info = index_basic_map.get(full_symbol, {})

            item["name"] = item.get("name") or basic_info.get("name") or full_symbol
            item["fullname"] = item.get("fullname") or basic_info.get("fullname") or ""
            item["market"] = item.get("market") or basic_info.get("market") or ""
            item["index_type"] = item.get("index_type") or basic_info.get("index_type") or ""
            item["category"] = item.get("category") or basic_info.get("category") or ""

            enriched_rows.append(item)

        order_map = {code: index for index, code in enumerate(MAJOR_INDEX_CODES)}
        enriched_rows.sort(key=lambda row: order_map.get(row.get("full_symbol"), 999))

        return [
            await self.build_index_item(
                row,
                is_industry=False,
                stock_snapshot_map=stock_snapshot_map,
                component_cache=component_cache,
            )
            for row in enriched_rows
        ]
