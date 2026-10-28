from __future__ import annotations

from typing import Any, Dict, List, Optional

from sqlalchemy import text
from sqlalchemy.ext.asyncio import AsyncConnection

from app.utils.db_utils import clause_params


class BasicDataRepo:
    """type_basic_data 库访问层。

    约束：本 repo 只读取 type_basic_data 库中的基础资料、指数成分和申万行业层级表；
    不访问行情库和财务库。跨库字段补充统一由 StockService 完成。
    """

    def __init__(self, conn: AsyncConnection):
        self.conn = conn

    async def get_stock_basic_detail(self, stock_code: str) -> Optional[Dict[str, Any]]:
        """查询单只股票基础资料，兼容 full_symbol 与不带交易所后缀的 symbol。"""
        if not stock_code or not stock_code.strip():
            return None

        clean_code = stock_code.strip()
        result = await self.conn.execute(
            text(
                """
                SELECT
                    full_symbol,
                    symbol,
                    name,
                    region,
                    industry,
                    full_name,
                    en_name,
                    cn_spell,
                    market_type,
                    exchange,
                    currency_type,
                    list_status,
                    list_date,
                    delist_date,
                    is_hs,
                    act_name,
                    act_ent_type
                FROM stock_basic_data
                WHERE full_symbol = :stock_code
                   OR symbol = :stock_code
                ORDER BY CASE WHEN full_symbol = :stock_code THEN 0 ELSE 1 END
                LIMIT 1
                """
            ),
            {"stock_code": clean_code},
        )
        row = result.first()
        return dict(row._mapping) if row else None

    async def get_stock_basic_by_codes(self, stock_codes: List[str]) -> Dict[str, Dict[str, Any]]:
        """批量查询股票基础资料，返回 full_symbol -> row。"""
        codes = [code.strip() for code in stock_codes if code and code.strip()]
        if not codes:
            return {}

        in_sql, params = clause_params("stock", codes)
        result = await self.conn.execute(
            text(
                f"""
                SELECT
                    full_symbol,
                    symbol,
                    name,
                    region,
                    industry,
                    full_name,
                    en_name,
                    cn_spell,
                    market_type,
                    exchange,
                    currency_type,
                    list_status,
                    list_date,
                    delist_date,
                    is_hs,
                    act_name,
                    act_ent_type
                FROM stock_basic_data
                WHERE full_symbol IN ({in_sql})
                   OR symbol IN ({in_sql})
                """
            ),
            params,
        )
        rows = [dict(row._mapping) for row in result]
        return {row["full_symbol"]: row for row in rows if row.get("full_symbol")}

    async def get_stock_basic_map(self) -> Dict[str, Dict[str, Any]]:
        """查询全量股票基础资料，仅供市场全量聚合场景使用。"""
        result = await self.conn.execute(
            text(
                """
                SELECT
                    full_symbol,
                    symbol,
                    name,
                    industry,
                    region,
                    market_type,
                    exchange,
                    currency_type,
                    list_status,
                    list_date,
                    delist_date
                FROM stock_basic_data
                """
            )
        )
        rows = [dict(row._mapping) for row in result]
        return {row["full_symbol"]: row for row in rows if row.get("full_symbol")}

    async def get_index_basic_map(self, index_codes: List[str]) -> Dict[str, Dict[str, Any]]:
        """批量查询指数基础资料，返回 full_symbol -> row。"""
        codes = [code.strip() for code in index_codes if code and code.strip()]
        if not codes:
            return {}

        in_sql, params = clause_params("index", codes)
        result = await self.conn.execute(
            text(
                f"""
                SELECT
                    full_symbol,
                    name,
                    fullname,
                    market,
                    publisher,
                    index_type,
                    category,
                    base_date,
                    base_point,
                    list_date,
                    weight_rule,
                    description,
                    exp_date
                FROM index_basic_data
                WHERE full_symbol IN ({in_sql})
                """
            ),
            params,
        )
        rows = [dict(row._mapping) for row in result]
        return {row["full_symbol"]: row for row in rows if row.get("full_symbol")}

    async def get_index_basic_detail(self, index_code: str) -> Optional[Dict[str, Any]]:
        """查询单个指数基础资料。"""
        if not index_code or not index_code.strip():
            return None

        clean_code = index_code.strip()
        result = await self.conn.execute(
            text(
                """
                SELECT
                    full_symbol,
                    name,
                    fullname,
                    market,
                    publisher,
                    index_type,
                    category,
                    base_date,
                    base_point,
                    list_date,
                    weight_rule,
                    description,
                    exp_date
                FROM index_basic_data
                WHERE full_symbol = :index_code
                   OR name = :index_code
                   OR fullname = :index_code
                ORDER BY CASE WHEN full_symbol = :index_code THEN 0 ELSE 1 END
                LIMIT 1
                """
            ),
            {"index_code": clean_code},
        )
        row = result.first()
        return dict(row._mapping) if row else None

    async def get_index_components(
        self,
        index_code: str,
        limit: Optional[int] = None,
        trade_date: str | None = None,
    ) -> List[Dict[str, Any]]:
        """查询指定指数最新成分股。

        只读取 type_basic_data.index_constituent_data，不在 repo 层补股票名称、行情或估值。
        若传入 trade_date，则取小于等于该日期的最新成分日期，避免历史成分重复返回。
        """
        if not index_code:
            return []

        params: Dict[str, Any] = {"index_code": index_code}
        limit_sql = ""
        date_sql = ""
        if trade_date:
            date_sql = "AND trade_date <= :trade_date"
            params["trade_date"] = trade_date
        if limit is not None:
            params["limit"] = max(1, int(limit))
            limit_sql = "LIMIT :limit"

        result = await self.conn.execute(
            text(
                f"""
                WITH latest_trade_date AS (
                    SELECT MAX(trade_date) AS trade_date
                    FROM index_constituent_data
                    WHERE index_code = :index_code
                      {date_sql}
                )
                SELECT
                    c.index_code AS index_code,
                    c.stock_code AS stock_code,
                    c.trade_date AS trade_date,
                    c.weight AS weight
                FROM index_constituent_data c
                JOIN latest_trade_date d
                  ON d.trade_date = c.trade_date
                WHERE c.index_code = :index_code
                ORDER BY c.weight DESC, c.stock_code
                {limit_sql}
                """
            ),
            params,
        )
        return [dict(row._mapping) for row in result]

    async def get_index_components_by_indices(
        self,
        index_codes: List[str],
        trade_date: str | None = None,
    ) -> List[Dict[str, Any]]:
        """批量查询多个指数各自最新成分股。

        用于申万 L1/L2 行业指数：index_constituent_data 仅保存 L3 成分，
        service 层先解析下属 L3 index_code，再通过本函数一次性读取成分。
        """
        codes = [code.strip() for code in index_codes if code and code.strip()]
        if not codes:
            return []

        in_sql, params = clause_params("index", codes)
        date_sql = ""
        if trade_date:
            date_sql = "AND trade_date <= :trade_date"
            params["trade_date"] = trade_date

        result = await self.conn.execute(
            text(
                f"""
                WITH latest_dates AS (
                    SELECT index_code, MAX(trade_date) AS trade_date
                    FROM index_constituent_data
                    WHERE index_code IN ({in_sql})
                      {date_sql}
                    GROUP BY index_code
                )
                SELECT
                    c.index_code AS index_code,
                    c.stock_code AS stock_code,
                    c.trade_date AS trade_date,
                    c.weight AS weight
                FROM index_constituent_data c
                JOIN latest_dates d
                  ON d.index_code = c.index_code
                 AND d.trade_date = c.trade_date
                ORDER BY c.index_code, c.weight DESC, c.stock_code
                """
            ),
            params,
        )
        return [dict(row._mapping) for row in result]

    async def get_index_component_counts(self, index_codes: List[str]) -> Dict[str, int]:
        """批量统计指数成分数量，用于申万行业层级行展示 stockCount。"""
        codes = [code.strip() for code in index_codes if code and code.strip()]
        if not codes:
            return {}

        in_sql, params = clause_params("index", codes)
        result = await self.conn.execute(
            text(
                f"""
                SELECT index_code, COUNT(*) AS stock_count
                FROM index_constituent_data
                WHERE index_code IN ({in_sql})
                GROUP BY index_code
                """
            ),
            params,
        )
        return {row._mapping["index_code"]: int(row._mapping["stock_count"] or 0) for row in result}

    async def get_stock_related_indices(
        self,
        stock_code: str,
        limit: int = 30,
        include_industry: bool = False,
    ) -> List[Dict[str, Any]]:
        """查询包含某只股票的指数关系。

        - 默认排除 .SI 申万行业指数，供“所属指数”展示规模/策略/主题等指数；
        - 行业归属由 get_stock_sw_industry_memberships 单独处理，避免口径混杂。
        """
        if not stock_code or limit <= 0:
            return []

        industry_sql = "" if include_industry else "AND c.index_code NOT LIKE '%.SI'"
        result = await self.conn.execute(
            text(
                f"""
                SELECT
                    c.index_code,
                    c.stock_code,
                    c.trade_date,
                    c.weight,
                    i.full_symbol,
                    i.name,
                    i.fullname,
                    i.market,
                    i.publisher,
                    i.index_type,
                    i.category,
                    i.base_date,
                    i.base_point,
                    i.list_date,
                    i.weight_rule,
                    i.description,
                    i.exp_date
                FROM index_constituent_data c
                LEFT JOIN index_basic_data i
                    ON i.full_symbol = c.index_code
                WHERE c.stock_code = :stock_code
                  {industry_sql}
                ORDER BY c.weight DESC, c.index_code
                LIMIT :limit
                """
            ),
            {"stock_code": stock_code, "limit": max(1, int(limit))},
        )
        return [dict(row._mapping) for row in result]

    async def get_stock_sw_industry_memberships(self, stock_code: str) -> List[Dict[str, Any]]:
        """查询个股所属申万行业指数，可能包含多级行业。

        申万行业指数用 .SI 后缀识别；index_industry_level 提供行业层级、父级和名称。
        这里不在 repo 中做行业树加工，只读取所需字段，树形补全由 StockService 完成。
        """
        if not stock_code:
            return []

        result = await self.conn.execute(
            text(
                """
                SELECT
                    c.index_code,
                    c.stock_code,
                    c.trade_date,
                    c.weight,
                    i.name AS index_name,
                    i.fullname AS index_fullname,
                    i.market,
                    i.category,
                    i.index_type,
                    l.industry_name,
                    l.level,
                    l.industry_code,
                    l.parent_code,
                    l.src,
                    l.is_pub
                FROM index_constituent_data c
                LEFT JOIN index_basic_data i
                    ON i.full_symbol = c.index_code
                LEFT JOIN index_industry_level l
                    ON l.index_code = c.index_code
                WHERE c.stock_code = :stock_code
                  AND c.index_code LIKE '%.SI'
                ORDER BY
                    c.trade_date DESC,
                    CASE
                        WHEN l.level = 'L1' THEN 0
                        WHEN l.level = 'L2' THEN 1
                        WHEN l.level = 'L3' THEN 2
                        ELSE 9
                    END,
                    c.index_code
                """
            ),
            {"stock_code": stock_code},
        )
        return [dict(row._mapping) for row in result]

    async def get_sw_industry_rows_by_codes(self, codes: List[str]) -> List[Dict[str, Any]]:
        """按 codes 查询对应行业的申万行业层级信息"""
        clean_codes = [code.strip() for code in codes if code and code.strip()]
        if not clean_codes:
            return []

        in_sql, params = clause_params("industry", clean_codes)
        result = await self.conn.execute(
            text(
                f"""
                SELECT
                    index_code,
                    industry_name,
                    level,
                    industry_code,
                    is_pub,
                    parent_code,
                    src
                FROM index_industry_level
                WHERE index_code IN ({in_sql})
                ORDER BY
                    CASE
                        WHEN level = 'L1' THEN 0
                        WHEN level = 'L2' THEN 1
                        WHEN level = 'L3' THEN 2
                        ELSE 9
                    END,
                    index_code
                """
            ),
            params,
        )
        return [dict(row._mapping) for row in result]

    async def get_sw_industry_level_rows(self) -> List[Dict[str, Any]]:
        """查询申万行业分级关系。

        repo 只返回 index_industry_level 原始字段；
        L1/L2/L3 树形关系和成分聚合由 IndexService 根据 index_code / industry_code / parent_code 完成。
        """
        result = await self.conn.execute(
            text(
                """
                SELECT
                    index_code,
                    industry_name,
                    level,
                    industry_code,
                    is_pub,
                    parent_code,
                    src
                FROM index_industry_level
                WHERE src = 'SW'
                ORDER BY
                    CASE
                        WHEN level = 'L1' THEN 0
                        WHEN level = 'L2' THEN 1
                        WHEN level = 'L3' THEN 2
                        ELSE 9
                    END,
                    industry_code,
                    index_code
                """
            )
        )
        return [dict(row._mapping) for row in result]

    async def get_stock_sw_industry_memberships_by_codes(self, stock_codes: List[str]) -> List[Dict[str, Any]]:
        """批量查询股票所属申万三级行业候选。

        index_constituent_data 中申万行业成分通常只覆盖三级行业；本函数只读取
        股票 -> L3 行业关系及 L3 层级行，父级补全交给 service 层处理。
        """
        codes: List[str] = []
        for code in stock_codes:
            clean = code.strip() if code else ""
            if not clean:
                continue
            codes.append(clean)
            if "." in clean:
                codes.append(clean.split(".")[0])
        codes = list(dict.fromkeys(codes))
        if not codes:
            return []

        in_sql, params = clause_params("stock", codes)
        result = await self.conn.execute(
            text(
                f"""
                SELECT
                    c.index_code,
                    c.stock_code,
                    c.trade_date,
                    c.weight,
                    l.industry_name,
                    l.level,
                    l.industry_code,
                    l.parent_code,
                    l.src,
                    l.is_pub
                FROM index_constituent_data c
                JOIN index_industry_level l
                  ON l.index_code = c.index_code
                WHERE c.stock_code IN ({in_sql})
                  AND l.level = 'L3'
                ORDER BY c.stock_code, c.trade_date DESC, c.index_code
                """
            ),
            params,
        )
        return [dict(row._mapping) for row in result]

    async def get_industry_peer_stocks(
        self,
        industry: str,
        exclude_code: str | None = None,
        limit: int = 20,
    ) -> List[Dict[str, Any]]:
        """旧口径兜底：按 stock_basic_data.industry 查询同行业股票。优先使用申万三级成分。"""
        if not industry or limit <= 0:
            return []

        params: Dict[str, Any] = {"industry": industry, "limit": max(1, int(limit))}
        exclude_sql = ""
        if exclude_code:
            exclude_sql = "AND full_symbol <> :exclude_code"
            params["exclude_code"] = exclude_code

        result = await self.conn.execute(
            text(
                f"""
                SELECT
                    full_symbol,
                    symbol,
                    name,
                    region,
                    industry,
                    full_name,
                    en_name,
                    cn_spell,
                    market_type,
                    exchange,
                    currency_type,
                    list_status,
                    list_date,
                    delist_date,
                    is_hs,
                    act_name,
                    act_ent_type
                FROM stock_basic_data
                WHERE industry = :industry
                  {exclude_sql}
                ORDER BY full_symbol
                LIMIT :limit
                """
            ),
            params,
        )
        return [dict(row._mapping) for row in result]

    async def get_sw_l3_component_codes(
        self,
        industry_index_code: str,
        trade_date: str | None = None,
        limit: Optional[int] = None,
    ) -> List[str]:
        """查询申万三级行业指数成分代码，供 service 做同业聚合。"""
        rows = await self.get_index_components(industry_index_code, limit=limit, trade_date=trade_date)
        return [row["stock_code"] for row in rows if row.get("stock_code")]

    async def get_index_peer_indices(
        self,
        market: str | None = None,
        category: str | None = None,
        exclude_code: str | None = None,
        limit: int = 10,
    ) -> List[Dict[str, Any]]:
        """查询同市场/同类别指数基础资料，仅访问 index_basic_data。"""
        if limit <= 0:
            return []

        params: Dict[str, Any] = {"limit": max(1, int(limit)), "market": market, "category": category}
        where_parts: List[str] = []
        if market:
            where_parts.append("market = :market")
        if category:
            where_parts.append("category = :category")
        if exclude_code:
            where_parts.append("full_symbol <> :exclude_code")
            params["exclude_code"] = exclude_code
        where_sql = " AND ".join(where_parts) if where_parts else "1 = 1"

        result = await self.conn.execute(
            text(
                f"""
                SELECT
                    full_symbol,
                    name,
                    fullname,
                    market,
                    publisher,
                    index_type,
                    category,
                    base_date,
                    base_point,
                    list_date,
                    weight_rule,
                    description,
                    exp_date
                FROM index_basic_data
                WHERE {where_sql}
                ORDER BY full_symbol
                LIMIT :limit
                """
            ),
            params,
        )
        return [dict(row._mapping) for row in result]