from typing import Any, Callable, Dict, List, Tuple

from app.utils.typing_utils import to_float


def build_hot_stock_list(stock_rows: List[Dict[str, Any]], limit: int = 10) -> List[Dict[str, Any]]:
    """
        从全市场股票数据中筛选热股。
        该函数只依赖已查询出的股票行情、指标、资金流和基础信息，不涉及数据库连接。
    """
    valid = [
        r for r in stock_rows
        if r.get("close") is not None
        and r.get("chg_pct") is not None
        and r.get("turnover") is not None
    ]

    if not valid:
        return []

    max_turnover = max(to_float(r["turnover"]) for r in valid) or 1
    max_abs_pct = max(abs(to_float(r["chg_pct"])) for r in valid) or 1
    max_net_mf = max(abs(to_float(r.get("net_mf_amount"))) for r in valid) or 1

    def score(row: Dict[str, Any]) -> float:
        turnover_score = to_float(row["turnover"]) / max_turnover * 45
        pct_score = abs(to_float(row["chg_pct"])) / max_abs_pct * 25
        money_score = max(to_float(row.get("net_mf_amount")), 0) / max_net_mf * 25
        rise_bonus = 5 if to_float(row["chg_pct"]) > 0 else 0
        return turnover_score + pct_score + money_score + rise_bonus

    rows = sorted(valid, key=score, reverse=True)[:limit]

    result = []
    for row in rows:
        change_pct = to_float(row["chg_pct"])
        net_mf_amount = to_float(row.get("net_mf_amount"))

        if net_mf_amount > 0 and change_pct > 0:
            reason = "主力资金净流入且股价上涨"
        elif change_pct >= 9.8:
            reason = "涨停或接近涨停，短线关注度高"
        elif to_float(row["turnover"]) >= max_turnover * 0.6:
            reason = "成交额居前，资金参与度高"
        elif change_pct > 5:
            reason = "涨幅居前，板块情绪活跃"
        else:
            reason = "量价及资金表现活跃"

        industry_name = row.get("industry") or "未分类"

        result.append({
            "stockCode": row.get("full_symbol") or row.get("symbol") or "",
            "stockName": row.get("name") or row.get("full_symbol") or "",
            "industryName": industry_name,
            "sectorName": industry_name,
            "latestPrice": round(to_float(row.get("close")), 2),
            "changePct": round(change_pct, 2),
            "turnover": round(to_float(row.get("turnover")), 2),
            "turnoverRate": round(to_float(row.get("turnover_rate")), 2),
            "hotScore": int(min(100, round(score(row)))),
            "reason": reason,
        })

    return result


def build_stock_buckets(stock_rows: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
    """构建个股涨跌幅分布；涨跌停阈值暂按A股常见的±9.8%近似处理。"""
    values = [to_float(r["chg_pct"]) for r in stock_rows if r.get("chg_pct") is not None]
    total = len(values)

    defs: List[Tuple[str, Callable[[float], bool], str]] = [
        ("涨停", lambda x: x >= 9.8, "positive"),
        ("≥ 7%", lambda x: 7 <= x < 9.8, "positive"),
        ("5%~7%", lambda x: 5 <= x < 7, "positive"),
        ("3%~5%", lambda x: 3 <= x < 5, "positive"),
        ("1%~3%", lambda x: 1 <= x < 3, "positive"),
        ("0%~1%", lambda x: 0 < x < 1, "positive"),
        ("0%", lambda x: x == 0, "neutral"),
        ("-1%~0%", lambda x: -1 < x < 0, "negative"),
        ("-3%~-1%", lambda x: -3 < x <= -1, "negative"),
        ("-5%~-3%", lambda x: -5 < x <= -3, "negative"),
        ("-7%~-5%", lambda x: -7 < x <= -5, "negative"),
        ("≤ -7%", lambda x: -9.8 < x <= -7, "negative"),
        ("跌停", lambda x: x <= -9.8, "negative"),
    ]

    result = []
    for label, fn, typ in defs:
        count = sum(1 for value in values if fn(value))
        result.append({
            "rangeLabel": label,
            "count": count,
            "ratio": round(count / total * 100, 2) if total else 0.0,
            "type": typ,
        })

    return result


def build_group_stats(stock_rows: List[Dict[str, Any]], group_key: str = "industry") -> List[Dict[str, Any]]:
    """按行业、概念等字段聚合股票涨跌、成交额和主力资金数据。"""
    groups: Dict[str, List[Dict[str, Any]]] = {}

    for row in stock_rows:
        group_name = row.get(group_key) or "未分类"
        groups.setdefault(group_name, []).append(row)

    stats = []

    for name, rows in groups.items():
        pct_values = [to_float(row["chg_pct"]) for row in rows if row.get("chg_pct") is not None]

        if not pct_values:
            continue

        turnover = sum(to_float(row.get("turnover")) for row in rows)
        net_mf_amount = sum(to_float(row.get("net_mf_amount")) for row in rows)

        rise_count = sum(1 for value in pct_values if value > 0)
        fall_count = sum(1 for value in pct_values if value < 0)
        flat_count = sum(1 for value in pct_values if value == 0)

        avg_change_pct = sum(pct_values) / len(pct_values)

        stats.append({
            "name": name,
            "turnover": turnover,
            "netMfAmount": net_mf_amount,
            "avgChangePct": avg_change_pct,
            "riseCount": rise_count,
            "fallCount": fall_count,
            "flatCount": flat_count,
            "total": len(pct_values),
        })

    return stats


def build_group_buckets(group_stats: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
    """构建板块/概念平均涨跌幅分布。"""
    values = [to_float(group["avgChangePct"]) for group in group_stats]
    total = len(values)

    defs: List[Tuple[str, Callable[[float], bool], str]] = [
        ("≥ 3%", lambda x: x >= 3, "positive"),
        ("2%~3%", lambda x: 2 <= x < 3, "positive"),
        ("1%~2%", lambda x: 1 <= x < 2, "positive"),
        ("0%~1%", lambda x: 0 < x < 1, "positive"),
        ("0%", lambda x: x == 0, "neutral"),
        ("-1%~0%", lambda x: -1 < x < 0, "negative"),
        ("-2%~-1%", lambda x: -2 < x <= -1, "negative"),
        ("-3%~-2%", lambda x: -3 < x <= -2, "negative"),
        ("≤ -3%", lambda x: x <= -3, "negative"),
    ]

    result = []
    for label, fn, typ in defs:
        count = sum(1 for value in values if fn(value))
        result.append({
            "rangeLabel": label,
            "count": count,
            "ratio": round(count / total * 100, 2) if total else 0.0,
            "type": typ,
        })

    return result


def build_amount_rank_from_stocks(stock_rows: List[Dict[str, Any]], limit: int = 5) -> List[Dict[str, Any]]:
    """构建个股成交额排行。"""
    rows = sorted(
        [row for row in stock_rows if row.get("amount") is not None],
        key=lambda row: to_float(row["amount"]),
        reverse=True,
    )[:limit]

    return [
        {
            "name": row.get("name") or row.get("full_symbol") or "",
            "amount": round(to_float(row.get("amount")), 2),
            "extra": f"{row.get('industry') or '未分类'} · 换手率 {round(to_float(row.get('turnover_rate')), 2)}%",
        }
        for row in rows
    ]


def build_main_fund_rank_from_stocks(stock_rows: List[Dict[str, Any]], limit: int = 5) -> List[Dict[str, Any]]:
    """构建个股主力资金净流入排行。"""
    rows = sorted(
        [row for row in stock_rows if row.get("net_mf_amount") is not None],
        key=lambda row: to_float(row["net_mf_amount"]),
        reverse=True,
    )[:limit]

    return [
        {
            "name": row.get("name") or row.get("full_symbol") or "",
            "mainFundInflow": round(to_float(row.get("net_mf_amount")), 2),
            "extra": f"{row.get('industry') or '未分类'} · 主力净流入",
        }
        for row in rows
    ]


def build_amount_rank_from_groups(group_stats: List[Dict[str, Any]], limit: int = 5) -> List[Dict[str, Any]]:
    """构建板块/概念成交额排行。"""
    rows = sorted(group_stats, key=lambda group: to_float(group["turnover"]), reverse=True)[:limit]

    return [
        {
            "name": group["name"],
            "turnover": round(to_float(group["turnover"]), 2),
            "extra": f"上涨占比 {round(group['riseCount'] / group['total'] * 100, 2) if group['total'] else 0}%",
        }
        for group in rows
    ]


def build_main_fund_rank_from_groups(group_stats: List[Dict[str, Any]], limit: int = 5) -> List[Dict[str, Any]]:
    """构建板块/概念主力资金净流入排行。"""
    rows = sorted(group_stats, key=lambda group: to_float(group["netMfAmount"]), reverse=True)[:limit]

    return [
        {
            "name": group["name"],
            "mainFundInflow": round(to_float(group["netMfAmount"]), 2),
            "extra": "板块内个股主力净流入汇总",
        }
        for group in rows
    ]


def build_distribution_tabs(stock_rows: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
    """
        构建个股、板块和概念分布。
        当前缺少概念关系表，因此概念模块暂用行业聚合结果兜底，并在description中明确提示。
    """
    valid = [row for row in stock_rows if row.get("chg_pct") is not None]
    pct_values = [to_float(row["chg_pct"]) for row in valid]

    stock_rise = sum(1 for value in pct_values if value > 0)
    stock_fall = sum(1 for value in pct_values if value < 0)
    stock_flat = sum(1 for value in pct_values if value == 0)

    sector_stats = build_group_stats(stock_rows, "industry")
    sector_pct_values = [to_float(group["avgChangePct"]) for group in sector_stats]
    sector_rise = sum(1 for value in sector_pct_values if value > 0)
    sector_fall = sum(1 for value in sector_pct_values if value < 0)
    sector_flat = sum(1 for value in sector_pct_values if value == 0)

    return [
        {
            "id": "stock",
            "label": "个股",
            "description": "按全市场有效个股统计涨跌区间，并展示个股交易额与主力资金流入排行",
            "riseCount": stock_rise,
            "fallCount": stock_fall,
            "flatCount": stock_flat,
            "avgChangePct": round(sum(pct_values) / len(pct_values), 2) if pct_values else 0,
            "buckets": build_stock_buckets(stock_rows),
            "amountRank": build_amount_rank_from_stocks(stock_rows),
            "mainFundInflowRank": build_main_fund_rank_from_stocks(stock_rows),
        },
        {
            "id": "sector",
            "label": "板块",
            "description": "按个股所属行业聚合统计板块涨跌分布，并展示板块交易额与主力资金流入排行",
            "riseCount": sector_rise,
            "fallCount": sector_fall,
            "flatCount": sector_flat,
            "avgChangePct": round(sum(sector_pct_values) / len(sector_pct_values), 2) if sector_pct_values else 0,
            "buckets": build_group_buckets(sector_stats),
            "amountRank": build_amount_rank_from_groups(sector_stats),
            "mainFundInflowRank": build_main_fund_rank_from_groups(sector_stats),
        },
        {
            "id": "concept",
            "label": "概念",
            "description": "暂未提供概念关系表，当前使用行业聚合数据作为概念模块兜底",
            "riseCount": sector_rise,
            "fallCount": sector_fall,
            "flatCount": sector_flat,
            "avgChangePct": round(sum(sector_pct_values) / len(sector_pct_values), 2) if sector_pct_values else 0,
            "buckets": build_group_buckets(sector_stats),
            "amountRank": build_amount_rank_from_groups(sector_stats),
            "mainFundInflowRank": build_main_fund_rank_from_groups(sector_stats),
        },
    ]


def build_stock_snapshot_map(stock_rows: List[Dict[str, Any]]) -> Dict[str, Dict[str, Any]]:
    """
    将全市场股票快照转换成以 full_symbol 为 key 的字典。

    MarketService 中指数成分股统计、行业指数领涨股等逻辑都需要按代码反查行情，
    统一封装后可以避免多个 service 重复书写字典推导式，也能集中处理空代码行。
    """
    return {
        row["full_symbol"]: row
        for row in stock_rows
        if row.get("full_symbol")
    }


def build_component_rise_stats(stock_rows: List[Dict[str, Any]]) -> Dict[str, Any]:
    """
    根据成分股行情计算上涨数量和上涨占比。

    当前 MarketService 的成分股行情字段来自 stock_k_daily/全市场快照，涨跌幅字段为 chg_pct；
    该函数只处理已查询出的纯数据，不访问数据库，方便在总览页和其他列表页复用。
    """
    pct_values = [
        to_float(row.get("chg_pct"))
        for row in stock_rows
        if row.get("chg_pct") is not None
    ]

    total = len(pct_values)
    rise = sum(1 for value in pct_values if value > 0)

    return {
        "totalCount": total,
        "riseCount": rise,
        "componentRiseRatio": round(rise / total * 100, 2) if total else 0.0,
    }