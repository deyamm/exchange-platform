from __future__ import annotations

import logging
from typing import Any, Dict, List, Optional

from sqlalchemy import text
from sqlalchemy.ext.asyncio import AsyncConnection

from app.utils.db_utils import clause_params

logger = logging.getLogger(__name__)


class ExchangeDataRepo:
    """type_exchange_data 库访问层。

    约束：本 repo 只读取行情、估值、资金流和复权因子等 exchange 库表；
    股票/指数名称、行业层级、财务数据不在此处关联。
    """

    def __init__(self, conn: AsyncConnection):
        self.conn = conn

    async def get_stock_snapshot(self, trade_date: str) -> List[Dict[str, Any]]:
        """查询指定交易日全市场股票行情快照。"""
        if not trade_date:
            return []
        result = await self.conn.execute(
            text(
                """
                SELECT
                    k.full_symbol,
                    k.trade_date,
                    k.open,
                    k.high,
                    k.low,
                    k.close,
                    k.volume,
                    k.turnover,
                    k.pre_close,
                    k.chg_pct,
                    k.chg_amt,
                    ind.turnover_rate,
                    ind.turnover_rate_free,
                    ind.volume_ratio,
                    ind.pe,
                    ind.pe_ttm,
                    ind.pb,
                    ind.ps,
                    ind.ps_ttm,
                    ind.dv_ratio,
                    ind.dv_ttm,
                    ind.total_share,
                    ind.float_share,
                    ind.free_share,
                    ind.total_market_value,
                    ind.float_market_value,
                    mf.buy_sm_amount,
                    mf.sell_sm_amount,
                    mf.buy_md_amount,
                    mf.sell_md_amount,
                    mf.buy_lg_amount,
                    mf.sell_lg_amount,
                    mf.buy_elg_amount,
                    mf.sell_elg_amount,
                    mf.net_mf_amount,
                    mf.net_mf_vol
                FROM stock_k_daily k
                LEFT JOIN stock_indicator_daily ind
                    ON ind.full_symbol = k.full_symbol
                   AND ind.trade_date = k.trade_date
                LEFT JOIN stock_moneyflow_daily mf
                    ON mf.full_symbol = k.full_symbol
                   AND mf.trade_date = k.trade_date
                WHERE k.trade_date = :trade_date
                """
            ),
            {"trade_date": trade_date},
        )
        return [dict(row._mapping) for row in result]

    async def get_stock_latest_snapshot(
        self,
        full_symbol: str,
        trade_date: str | None = None,
    ) -> Optional[Dict[str, Any]]:
        """查询单只股票等于指定日期的最新行情、估值和资金流快照。"""
        if not full_symbol:
            return None

        params: Dict[str, Any] = {"full_symbol": full_symbol}
        date_sql = ""
        if trade_date:
            date_sql = "AND k.trade_date = :trade_date"
            params["trade_date"] = trade_date

        result = await self.conn.execute(
            text(
                f"""
                SELECT
                    k.full_symbol,
                    k.trade_date,
                    k.open,
                    k.high,
                    k.low,
                    k.close,
                    k.volume,
                    k.turnover,
                    k.pre_close,
                    k.chg_pct,
                    k.chg_amt,
                    ind.turnover_rate,
                    ind.turnover_rate_free,
                    ind.volume_ratio,
                    ind.pe,
                    ind.pe_ttm,
                    ind.pb,
                    ind.ps,
                    ind.ps_ttm,
                    ind.dv_ratio,
                    ind.dv_ttm,
                    ind.total_share,
                    ind.float_share,
                    ind.free_share,
                    ind.total_market_value,
                    ind.float_market_value,
                    mf.buy_sm_vol,
                    mf.buy_sm_amount,
                    mf.sell_sm_vol,
                    mf.sell_sm_amount,
                    mf.buy_md_vol,
                    mf.buy_md_amount,
                    mf.sell_md_vol,
                    mf.sell_md_amount,
                    mf.buy_lg_vol,
                    mf.buy_lg_amount,
                    mf.sell_lg_vol,
                    mf.sell_lg_amount,
                    mf.buy_elg_vol,
                    mf.buy_elg_amount,
                    mf.sell_elg_vol,
                    mf.sell_elg_amount,
                    mf.net_mf_amount,
                    mf.net_mf_vol
                FROM stock_k_daily k
                LEFT JOIN stock_indicator_daily ind
                    ON ind.full_symbol = k.full_symbol
                   AND ind.trade_date = k.trade_date
                LEFT JOIN stock_moneyflow_daily mf
                    ON mf.full_symbol = k.full_symbol
                   AND mf.trade_date = k.trade_date
                WHERE k.full_symbol = :full_symbol
                  {date_sql}
                ORDER BY k.trade_date DESC
                LIMIT 1
                """
            ),
            params,
        )
        row = result.first()
        return dict(row._mapping) if row else None

    async def get_stock_latest_snapshots_by_codes(
        self,
        stock_codes: List[str],
        trade_date: str | None = None,
    ) -> List[Dict[str, Any]]:
        """批量查询一组股票等于指定日期的最新快照。

        用窗口函数避免 service 层循环访问数据库。若底层数据库不支持窗口函数，可退回
        get_stock_snapshots_by_codes 的指定日期精确查询。
        """
        codes = [code for code in stock_codes if code]
        if not codes:
            return []

        in_sql, params = clause_params("stock", codes)
        date_sql = ""
        if trade_date:
            date_sql = "AND k.trade_date = :trade_date"
            params["trade_date"] = trade_date
        
        result = await self.conn.execute(
            text(
                f"""
                SELECT
                    k.full_symbol,
                    k.trade_date,
                    k.open,
                    k.high,
                    k.low,
                    k.close,
                    k.volume,
                    k.turnover,
                    k.pre_close,
                    k.chg_pct,
                    k.chg_amt,
                    ind.turnover_rate,
                    ind.turnover_rate_free,
                    ind.volume_ratio,
                    ind.pe,
                    ind.pe_ttm,
                    ind.pb,
                    ind.ps,
                    ind.ps_ttm,
                    ind.dv_ratio,
                    ind.dv_ttm,
                    ind.total_share,
                    ind.float_share,
                    ind.free_share,
                    ind.total_market_value,
                    ind.float_market_value,
                    mf.net_mf_amount
                FROM stock_k_daily k
                LEFT JOIN stock_indicator_daily ind
                    ON ind.full_symbol = k.full_symbol
                    AND ind.trade_date = k.trade_date
                LEFT JOIN stock_moneyflow_daily mf
                    ON mf.full_symbol = k.full_symbol
                    AND mf.trade_date = k.trade_date
                WHERE k.full_symbol IN ({in_sql})
                    {date_sql}
                """
            ),
            params,
        )
        return [dict(row._mapping) for row in result]

    async def get_stock_snapshots_by_codes(self, stock_codes: List[str], trade_date: str) -> List[Dict[str, Any]]:
        """批量查询一组股票指定交易日的快照，保留旧调用兼容。"""
        codes = [code for code in stock_codes if code]
        if not codes or not trade_date:
            return []
        in_sql, params = clause_params("stock", codes)
        params["trade_date"] = trade_date
        result = await self.conn.execute(
            text(
                f"""
                SELECT
                    k.full_symbol,
                    k.trade_date,
                    k.open,
                    k.high,
                    k.low,
                    k.close,
                    k.volume,
                    k.turnover,
                    k.pre_close,
                    k.chg_pct,
                    k.chg_amt,
                    ind.turnover_rate,
                    ind.volume_ratio,
                    ind.pe,
                    ind.pe_ttm,
                    ind.pb,
                    ind.ps,
                    ind.ps_ttm,
                    ind.dv_ttm,
                    ind.total_market_value,
                    ind.float_market_value,
                    mf.net_mf_amount
                FROM stock_k_daily k
                LEFT JOIN stock_indicator_daily ind
                    ON ind.full_symbol = k.full_symbol
                   AND ind.trade_date = k.trade_date
                LEFT JOIN stock_moneyflow_daily mf
                    ON mf.full_symbol = k.full_symbol
                   AND mf.trade_date = k.trade_date
                WHERE k.trade_date = :trade_date
                  AND k.full_symbol IN ({in_sql})
                """
            ),
            params,
        )
        return [dict(row._mapping) for row in result]

    async def get_stock_history(
        self,
        full_symbol: str,
        trade_date: str | None = None,
        limit: int = 520,
        adj: str | None = None,
    ) -> List[Dict[str, Any]]:
        """查询股票 K 线历史，返回 trade_date DESC。

        adj 参数保留兼容位；本实现默认返回物理表原始价格，复权可在后续基于
        stock_adjust_factor 扩展。
        """
        if not full_symbol or limit <= 0:
            return []
        params: Dict[str, Any] = {"full_symbol": full_symbol, "limit": max(1, int(limit))}
        date_sql = ""
        if trade_date:
            date_sql = "AND trade_date <= :trade_date"
            params["trade_date"] = trade_date
        result = await self.conn.execute(
            text(
                f"""
                SELECT
                    full_symbol,
                    trade_date,
                    open,
                    high,
                    low,
                    close,
                    volume,
                    turnover,
                    pre_close,
                    chg_pct,
                    chg_amt
                FROM stock_k_daily
                WHERE full_symbol = :full_symbol
                  {date_sql}
                ORDER BY trade_date DESC
                LIMIT :limit
                """
            ),
            params,
        )
        return [dict(row._mapping) for row in result]

    async def get_stock_indicator_history(
        self,
        full_symbol: str,
        trade_date: str | None = None,
        limit: int = 1250,
    ) -> List[Dict[str, Any]]:
        """查询股票估值指标历史，返回 trade_date DESC。"""
        if not full_symbol or limit <= 0:
            return []
        params: Dict[str, Any] = {"full_symbol": full_symbol, "limit": max(1, int(limit))}
        date_sql = ""
        if trade_date:
            date_sql = "AND trade_date <= :trade_date"
            params["trade_date"] = trade_date
        result = await self.conn.execute(
            text(
                f"""
                SELECT
                    full_symbol,
                    trade_date,
                    close,
                    turnover_rate,
                    turnover_rate_free,
                    volume_ratio,
                    pe,
                    pe_ttm,
                    pb,
                    ps,
                    ps_ttm,
                    dv_ratio,
                    dv_ttm,
                    total_share,
                    float_share,
                    free_share,
                    total_market_value,
                    float_market_value
                FROM stock_indicator_daily
                WHERE full_symbol = :full_symbol
                  {date_sql}
                ORDER BY trade_date DESC
                LIMIT :limit
                """
            ),
            params,
        )
        return [dict(row._mapping) for row in result]

    async def get_stock_indicator_rows_by_codes(
        self,
        stock_codes: List[str],
        trade_date: str | None = None,
        limit_trade_dates: int = 260,
    ) -> List[Dict[str, Any]]:
        """批量查询多个股票最近若干交易日的估值指标。

        用于 service 层按申万三级行业成分聚合行业平均/中位估值。repo 只读取
        stock_indicator_daily，不做行业判断。
        """
        codes = [code for code in stock_codes if code]
        if not codes or limit_trade_dates <= 0:
            return []
        in_sql, params = clause_params("stock", codes)
        params["limit_trade_dates"] = str(max(1, int(limit_trade_dates)))
        date_sql = ""
        if trade_date:
            date_sql = "WHERE trade_date <= :trade_date"
            params["trade_date"] = trade_date
        result = await self.conn.execute(
            text(
                f"""
                WITH dates AS (
                    SELECT DISTINCT trade_date
                    FROM stock_indicator_daily
                    {date_sql}
                    ORDER BY trade_date DESC
                    LIMIT :limit_trade_dates
                )
                SELECT
                    s.full_symbol,
                    s.trade_date,
                    s.close,
                    s.turnover_rate,
                    s.turnover_rate_free,
                    s.volume_ratio,
                    s.pe,
                    s.pe_ttm,
                    s.pb,
                    s.ps,
                    s.ps_ttm,
                    s.dv_ratio,
                    s.dv_ttm,
                    s.total_share,
                    s.float_share,
                    s.free_share,
                    s.total_market_value,
                    s.float_market_value
                FROM stock_indicator_daily s
                INNER JOIN dates d ON d.trade_date = s.trade_date
                WHERE s.full_symbol IN ({in_sql})
                ORDER BY s.trade_date DESC, s.full_symbol
                """
            ),
            params,
        )
        return [dict(row._mapping) for row in result]

    async def get_stock_moneyflow_history(
        self,
        full_symbol: str,
        trade_date: str | None = None,
        limit: int = 60,
    ) -> List[Dict[str, Any]]:
        """查询股票资金流历史，返回 trade_date DESC。"""
        if not full_symbol or limit <= 0:
            return []
        params: Dict[str, Any] = {"full_symbol": full_symbol, "limit": max(1, int(limit))}
        date_sql = ""
        if trade_date:
            date_sql = "AND trade_date <= :trade_date"
            params["trade_date"] = trade_date
        result = await self.conn.execute(
            text(
                f"""
                SELECT
                    full_symbol,
                    trade_date,
                    buy_sm_vol,
                    buy_sm_amount,
                    sell_sm_vol,
                    sell_sm_amount,
                    buy_md_vol,
                    buy_md_amount,
                    sell_md_vol,
                    sell_md_amount,
                    buy_lg_vol,
                    buy_lg_amount,
                    sell_lg_vol,
                    sell_lg_amount,
                    buy_elg_vol,
                    buy_elg_amount,
                    sell_elg_vol,
                    sell_elg_amount,
                    net_mf_vol,
                    net_mf_amount
                FROM stock_moneyflow_daily
                WHERE full_symbol = :full_symbol
                  {date_sql}
                ORDER BY trade_date DESC
                LIMIT :limit
                """
            ),
            params,
        )
        return [dict(row._mapping) for row in result]

    async def get_index_latest_snapshot(self, full_symbol: str, trade_date: str | None = None) -> Optional[Dict[str, Any]]:
        """查询单个指数小于等于指定日期的最新行情与指标快照。"""
        rows = await self.get_index_latest_snapshots_by_codes([full_symbol], trade_date)
        return rows[0] if rows else None

    async def get_index_latest_snapshots_by_codes(
        self,
        index_codes: List[str],
        trade_date: str | None = None,
    ) -> List[Dict[str, Any]]:
        """批量查询指数等于指定日期的最新行情和估值规模快照。"""
        codes = [code for code in index_codes if code]
        if not codes:
            return []
        in_sql, params = clause_params("index", codes)
        date_sql = ""
        if trade_date:
            date_sql = "AND k.trade_date = :trade_date"
            params["trade_date"] = trade_date
        result = await self.conn.execute(
            text(
                f"""
                SELECT
                    k.full_symbol,
                    k.trade_date,
                    k.open,
                    k.high,
                    k.low,
                    k.close,
                    k.volume,
                    k.turnover,
                    k.pre_close,
                    k.chg_pct,
                    k.chg_amt,
                    ind.total_market_value,
                    ind.float_market_value,
                    ind.total_share,
                    ind.float_share,
                    ind.free_share,
                    ind.turnover_rate,
                    ind.turnover_rate_free,
                    ind.pe,
                    ind.pe_ttm,
                    ind.pb
                FROM index_k_daily k
                LEFT JOIN index_indicator_daily ind
                    ON ind.full_symbol = k.full_symbol
                    AND ind.trade_date = k.trade_date
                WHERE k.full_symbol IN ({in_sql})
                    {date_sql}
                """
            ),
            params,
        )
        return [dict(row._mapping) for row in result]

    async def get_index_snapshots_by_codes(self, index_codes: List[str], trade_date: str) -> List[Dict[str, Any]]:
        """指定交易日批量查询指数快照，保留旧调用兼容。"""
        codes = [code for code in index_codes if code]
        if not codes or not trade_date:
            return []
        in_sql, params = clause_params("index", codes)
        params["trade_date"] = trade_date
        result = await self.conn.execute(
            text(
                f"""
                SELECT
                    k.full_symbol,
                    k.trade_date,
                    k.open,
                    k.high,
                    k.low,
                    k.close,
                    k.volume,
                    k.turnover,
                    k.pre_close,
                    k.chg_pct,
                    k.chg_amt,
                    ind.total_market_value,
                    ind.float_market_value,
                    ind.turnover_rate,
                    ind.turnover_rate_free,
                    ind.pe,
                    ind.pe_ttm,
                    ind.pb
                FROM index_k_daily k
                LEFT JOIN index_indicator_daily ind
                    ON ind.full_symbol = k.full_symbol
                   AND ind.trade_date = k.trade_date
                WHERE k.trade_date = :trade_date
                  AND k.full_symbol IN ({in_sql})
                """
            ),
            params,
        )
        return [dict(row._mapping) for row in result]

    async def get_index_indicator_history(
        self,
        full_symbol: str,
        trade_date: str | None = None,
        limit: int = 1250,
    ) -> List[Dict[str, Any]]:
        """查询指数估值规模指标历史。"""
        if not full_symbol or limit <= 0:
            return []
        params: Dict[str, Any] = {"full_symbol": full_symbol, "limit": max(1, int(limit))}
        date_sql = ""
        if trade_date:
            date_sql = "AND trade_date <= :trade_date"
            params["trade_date"] = trade_date
        result = await self.conn.execute(
            text(
                f"""
                SELECT
                    full_symbol,
                    trade_date,
                    total_market_value,
                    float_market_value,
                    total_share,
                    float_share,
                    free_share,
                    turnover_rate,
                    turnover_rate_free,
                    pe,
                    pe_ttm,
                    pb
                FROM index_indicator_daily
                WHERE full_symbol = :full_symbol
                  {date_sql}
                ORDER BY trade_date DESC
                LIMIT :limit
                """
            ),
            params,
        )
        return [dict(row._mapping) for row in result]

    async def get_index_history(self, full_symbol: str, trade_date: str | None = None, limit: int = 520) -> List[Dict[str, Any]]:
        """查询指数历史行情，返回 trade_date DESC。"""
        if not full_symbol or limit <= 0:
            return []
        params: Dict[str, Any] = {"full_symbol": full_symbol, "limit": max(1, int(limit))}
        date_sql = ""
        if trade_date:
            date_sql = "AND trade_date <= :trade_date"
            params["trade_date"] = trade_date
        result = await self.conn.execute(
            text(
                f"""
                SELECT
                    full_symbol,
                    trade_date,
                    open,
                    high,
                    low,
                    close,
                    volume,
                    turnover,
                    pre_close,
                    chg_pct,
                    chg_amt
                FROM index_k_daily
                WHERE full_symbol = :full_symbol
                  {date_sql}
                ORDER BY trade_date DESC
                LIMIT :limit
                """
            ),
            params,
        )
        return [dict(row._mapping) for row in result]

    async def get_stock_k_rows(self, stock_codes: List[str], trade_date: str) -> List[Dict[str, Any]]:
        """批量查询指定股票在某交易日的 K 线。"""
        codes = [code for code in stock_codes if code]
        if not codes or not trade_date:
            return []
        in_sql, params = clause_params("stock", codes)
        params["trade_date"] = trade_date
        result = await self.conn.execute(
            text(
                f"""
                SELECT
                    full_symbol,
                    trade_date,
                    close,
                    open,
                    high,
                    low,
                    pre_close,
                    chg_amt,
                    chg_pct,
                    volume,
                    turnover
                FROM stock_k_daily
                WHERE trade_date = :trade_date
                  AND full_symbol IN ({in_sql})
                """
            ),
            params,
        )
        return [dict(row._mapping) for row in result]

    async def get_index_k_rows(self, index_codes: List[str], trade_date: str) -> List[Dict[str, Any]]:
        """批量查询指定指数在某交易日的 K 线。"""
        codes = [code for code in index_codes if code]
        if not codes or not trade_date:
            return []
        in_sql, params = clause_params("index", codes)
        params["trade_date"] = trade_date
        result = await self.conn.execute(
            text(
                f"""
                SELECT
                    full_symbol,
                    trade_date,
                    close,
                    open,
                    high,
                    low,
                    pre_close,
                    chg_amt,
                    chg_pct,
                    volume,
                    turnover
                FROM index_k_daily
                WHERE trade_date = :trade_date
                  AND full_symbol IN ({in_sql})
                """
            ),
            params,
        )
        return [dict(row._mapping) for row in result]

    async def get_target_history(
        self,
        full_symbol: str,
        target_type: str = "STOCK",
        trade_date: str | None = None,
        limit: int = 520,
    ) -> List[Dict[str, Any]]:
        """按标的类型读取 K 线历史，repo 只做物理表分发。"""
        if (target_type or "STOCK").upper() == "INDEX":
            return await self.get_index_history(full_symbol, trade_date=trade_date, limit=limit)
        return await self.get_stock_history(full_symbol, trade_date=trade_date, limit=limit)

    async def get_target_indicator_history(
        self,
        full_symbol: str,
        target_type: str = "STOCK",
        trade_date: str | None = None,
        limit: int = 1250,
    ) -> List[Dict[str, Any]]:
        """按标的类型读取估值/规模指标历史。"""
        if (target_type or "STOCK").upper() == "INDEX":
            return await self.get_index_indicator_history(full_symbol, trade_date=trade_date, limit=limit)
        return await self.get_stock_indicator_history(full_symbol, trade_date=trade_date, limit=limit)

    async def get_target_latest_snapshot(
        self,
        full_symbol: str,
        target_type: str = "STOCK",
        trade_date: str | None = None,
    ) -> Optional[Dict[str, Any]]:
        """按标的类型读取最新快照。"""
        if (target_type or "STOCK").upper() == "INDEX":
            return await self.get_index_latest_snapshot(full_symbol, trade_date=trade_date)
        return await self.get_stock_latest_snapshot(full_symbol, trade_date=trade_date)

    async def get_total_stock_amount(self, trade_date: str) -> float:
        """查询指定交易日全市场股票总成交额。"""
        if not trade_date:
            return 0.0
        result = await self.conn.execute(
            text("SELECT SUM(turnover) AS total_turnover FROM stock_k_daily WHERE trade_date = :trade_date"),
            {"trade_date": trade_date},
        )
        row = result.first()
        value = row._mapping["total_turnover"] if row else None
        return float(value or 0)

    async def get_stock_adjust_factors(
        self,
        full_symbol: str,
        start_trade_date: str | None = None,
        end_trade_date: str | None = None,
    ) -> List[Dict[str, Any]]:
        """查询单只股票复权因子，返回 trade_date ASC。

        start/end 为可选兜底过滤；service 默认读取全量因子，因为因子行的
        trade_date 表示该因子有效的最后一天。
        """
        if not full_symbol:
            return []
        params: Dict[str, Any] = {"full_symbol": full_symbol}
        date_sql = ""
        if start_trade_date:
            date_sql += " AND trade_date >= :start_trade_date"
            params["start_trade_date"] = start_trade_date
        if end_trade_date:
            date_sql += " AND trade_date <= :end_trade_date"
            params["end_trade_date"] = end_trade_date
        result = await self.conn.execute(
            text(
                f"""
                SELECT full_symbol, trade_date, adjust_factor
                FROM stock_adjust_factor
                WHERE full_symbol = :full_symbol
                  {date_sql}
                ORDER BY trade_date ASC
                """
            ),
            params,
        )
        return [dict(row._mapping) for row in result]