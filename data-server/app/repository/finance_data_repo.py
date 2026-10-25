from __future__ import annotations

from typing import Any, Dict, List, Optional

from sqlalchemy import text
from sqlalchemy.ext.asyncio import AsyncConnection


INCOME_COLUMNS = [
    "full_symbol", "ann_date", "f_ann_date", "end_date", "report_type", "comp_type",
    "basic_eps", "diluted_eps", "total_revenue", "revenue", "total_cogs", "oper_cost",
    "sell_exp", "admin_exp", "fin_exp", "rd_exp", "invest_income", "oper_profit",
    "total_profit", "income_tax", "n_income", "n_income_attr_p", "ebit", "ebitda", "update_flag",
]

BALANCE_SHEET_COLUMNS = [
    "full_symbol", "ann_date", "f_ann_date", "end_date", "report_type", "comp_type",
    "total_share", "money_cap", "trad_asset", "notes_receiv", "accounts_receiv", "oth_receiv",
    "prepayment", "inventories", "total_cur_assets", "lt_eqt_invest", "invest_real_estate",
    "fix_assets", "cip", "intan_assets", "goodwill", "defer_tax_assets", "total_nca",
    "total_assets", "st_borr", "notes_payable", "acct_payable", "adv_receipts", "payroll_payable",
    "taxes_payable", "oth_payable", "non_cur_liab_due_1y", "total_cur_liab", "lt_borr",
    "bond_payable", "lt_payable", "defer_tax_liab", "total_ncl", "total_liab", "treasury_share",
    "cap_rese", "surplus_rese", "undistr_porfit", "minority_int", "total_hldr_eqy_exc_min_int",
    "total_hldr_eqy_inc_min_int", "total_liab_hldr_eqy", "update_flag",
]

CASHFLOW_COLUMNS = [
    "full_symbol", "ann_date", "f_ann_date", "end_date", "report_type", "comp_type",
    "net_profit", "finan_exp", "c_fr_sale_sg", "recp_tax_rends", "c_fr_oth_operate_a",
    "c_inf_fr_operate_a", "c_paid_goods_s", "c_paid_to_for_empl", "c_paid_for_taxes",
    "oth_cash_pay_oper_act", "st_cash_out_act", "n_cashflow_act", "c_disp_withdrwl_invest",
    "c_recp_return_invest", "n_recp_disp_fiolta", "stot_inflows_inv_act", "c_pay_acq_const_fiolta",
    "c_paid_invest", "stot_out_inv_act", "n_cashflow_inv_act", "c_recp_borrow", "proc_issue_bonds",
    "stot_cash_in_fnc_act", "c_prepay_amt_borr", "c_pay_dist_dpcp_int_exp", "stot_cashout_fnc_act",
    "n_cash_flows_fnc_act", "eff_fx_flu_cash", "n_incr_cash_cash_equ", "c_cash_equ_beg_period",
    "c_cash_equ_end_period", "free_cashflow", "im_net_cashflow_oper_act", "update_flag",
]

FINANCE_INDICATOR_COLUMNS = [
    "full_symbol", "ann_date", "end_date", "eps", "dt_eps", "bps", "ocfps", "cfps", "ebit_ps",
    "fcff_ps", "fcfe_ps", "roe", "roe_waa", "roe_dt", "roa", "roic", "gross_margin",
    "netprofit_margin", "grossprofit_margin", "profit_to_gr", "op_to_ebt", "current_ratio",
    "quick_ratio", "cash_ratio", "debt_to_assets", "assets_to_eqt", "debt_to_eqt", "ebit_to_interest",
    "ar_turn", "inv_turn", "ca_turn", "fa_turn", "assets_turn", "arturn_days", "invturn_days",
    "turn_days", "revenue_ps", "total_revenue_ps", "capital_rese_ps", "surplus_rese_ps",
    "undist_profit_ps", "profit_dedt", "extra_item", "ebit", "ebitda", "fcff", "fcfe",
    "working_capital", "op_yoy", "ebt_yoy", "netprofit_yoy", "dt_netprofit_yoy", "revenue_yoy",
    "assets_yoy", "equity_yoy", "update_flag",
]


class FinanceDataRepo:
    """type_finance_data 库访问层。

    约束：本 repo 只读取财务三大报表和财务指标表，不访问基础库或行情库。
    字段命名保持数据库 snake_case，统一由 StockService 转为前端 camelCase。
    """

    def __init__(self, conn: AsyncConnection):
        self.conn = conn

    async def _fetch_rows(
        self,
        table_name: str,
        columns: List[str],
        full_symbol: str,
        end_date: str | None = None,
        limit: int = 20,
        start_end_date: str | None = None,
    ) -> List[Dict[str, Any]]:
        """通用财务表读取函数，仅内部复用。"""
        if not full_symbol or limit <= 0:
            return []

        params: Dict[str, Any] = {"full_symbol": full_symbol, "limit": max(1, int(limit))}
        date_sql = ""
        if end_date:
            date_sql = "AND end_date <= :end_date"
            params["end_date"] = end_date
        if start_end_date:
            date_sql += " AND end_date >= :start_end_date"
            params["start_end_date"] = start_end_date

        column_sql = ",\n                    ".join(columns)
        result = await self.conn.execute(
            text(
                f"""
                SELECT
                    {column_sql}
                FROM {table_name}
                WHERE full_symbol = :full_symbol
                  {date_sql}
                ORDER BY end_date DESC, ann_date DESC
                LIMIT :limit
                """
            ),
            params,
        )
        return [dict(row._mapping) for row in result]

    async def get_income_rows(self, full_symbol: str, end_date: str | None = None, limit: int = 20, start_end_date: str | None = None) -> List[Dict[str, Any]]:
        """查询利润表明细。"""
        return await self._fetch_rows("income_data", INCOME_COLUMNS, full_symbol, end_date, limit, start_end_date)

    async def get_balance_sheet_rows(self, full_symbol: str, end_date: str | None = None, limit: int = 20, start_end_date: str | None = None) -> List[Dict[str, Any]]:
        """查询资产负债表明细。"""
        return await self._fetch_rows("balance_sheet_data", BALANCE_SHEET_COLUMNS, full_symbol, end_date, limit, start_end_date)

    async def get_cashflow_rows(self, full_symbol: str, end_date: str | None = None, limit: int = 20, start_end_date: str | None = None) -> List[Dict[str, Any]]:
        """查询现金流量表明细。"""
        return await self._fetch_rows("cashflow_data", CASHFLOW_COLUMNS, full_symbol, end_date, limit, start_end_date)

    async def get_finance_indicator_rows(self, full_symbol: str, end_date: str | None = None, limit: int = 20, start_end_date: str | None = None) -> List[Dict[str, Any]]:
        """查询财务指标明细。"""
        return await self._fetch_rows("finance_indicator_data", FINANCE_INDICATOR_COLUMNS, full_symbol, end_date, limit, start_end_date)

    async def get_latest_income(self, full_symbol: str, end_date: str | None = None) -> Optional[Dict[str, Any]]:
        rows = await self.get_income_rows(full_symbol, end_date=end_date, limit=1)
        return rows[0] if rows else None

    async def get_latest_balance_sheet(self, full_symbol: str, end_date: str | None = None) -> Optional[Dict[str, Any]]:
        rows = await self.get_balance_sheet_rows(full_symbol, end_date=end_date, limit=1)
        return rows[0] if rows else None

    async def get_latest_cashflow(self, full_symbol: str, end_date: str | None = None) -> Optional[Dict[str, Any]]:
        rows = await self.get_cashflow_rows(full_symbol, end_date=end_date, limit=1)
        return rows[0] if rows else None

    async def get_latest_finance_indicator(self, full_symbol: str, end_date: str | None = None) -> Optional[Dict[str, Any]]:
        rows = await self.get_finance_indicator_rows(full_symbol, end_date=end_date, limit=1)
        return rows[0] if rows else None

    async def get_latest_financial_bundle(self, full_symbol: str) -> Dict[str, Optional[Dict[str, Any]]]:
        """查询四张财务表最新记录。"""
        income = await self.get_latest_income(full_symbol)
        balance_sheet = await self.get_latest_balance_sheet(full_symbol)
        cashflow = await self.get_latest_cashflow(full_symbol)
        indicator = await self.get_latest_finance_indicator(full_symbol)
        return {
            "income": income,
            "balanceSheet": balance_sheet,
            "cashflow": cashflow,
            "indicator": indicator,
        }

    async def get_financial_statement_detail(
        self,
        full_symbol: str,
        limit: int = 20,
        start_end_date: str | None = None,
    ) -> Dict[str, List[Dict[str, Any]]]:
        """查询财务详情页需要的四表明细。

        start_end_date 可用于前端按年数回溯时过滤报告期；默认保持旧行为。
        """
        return {
            "incomeRows": await self.get_income_rows(full_symbol, limit=limit, start_end_date=start_end_date),
            "balanceSheetRows": await self.get_balance_sheet_rows(full_symbol, limit=limit, start_end_date=start_end_date),
            "cashflowRows": await self.get_cashflow_rows(full_symbol, limit=limit, start_end_date=start_end_date),
            "indicatorRows": await self.get_finance_indicator_rows(full_symbol, limit=limit, start_end_date=start_end_date),
        }

    async def get_financial_history(self, full_symbol: str, limit: int = 8) -> List[Dict[str, Any]]:
        """查询财务趋势所需的轻量历史，兼容旧 StockService 调用。"""
        if not full_symbol or limit <= 0:
            return []
        result = await self.conn.execute(
            text(
                """
                SELECT
                    i.full_symbol,
                    i.end_date,
                    i.ann_date,
                    i.revenue,
                    i.total_revenue,
                    i.n_income_attr_p,
                    ind.profit_dedt,
                    ind.eps,
                    ind.roe,
                    ind.roa,
                    ind.gross_margin,
                    ind.netprofit_margin,
                    ind.debt_to_assets,
                    ind.revenue_yoy,
                    ind.netprofit_yoy,
                    ind.dt_netprofit_yoy
                FROM income_data i
                LEFT JOIN finance_indicator_data ind
                    ON ind.full_symbol = i.full_symbol
                   AND ind.end_date = i.end_date
                WHERE i.full_symbol = :full_symbol
                ORDER BY i.end_date DESC, i.ann_date DESC
                LIMIT :limit
                """
            ),
            {"full_symbol": full_symbol, "limit": max(1, int(limit))},
        )
        return [dict(row._mapping) for row in result]

    async def get_index_financial_bundle(self, full_symbol: str) -> Dict[str, Optional[Dict[str, Any]]]:
        """指数暂无财务四表，保留兼容接口。"""
        return {"income": None, "balanceSheet": None, "cashflow": None, "indicator": None}

    async def get_index_financial_history(self, full_symbol: str, limit: int = 8) -> List[Dict[str, Any]]:
        """指数暂无财务历史，保留兼容接口。"""
        return []