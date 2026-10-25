import math
import statistics
from typing import Any, Dict, List, Optional, Sequence, Tuple

from app.utils.datetime_utils import current_market_status, get_current_datetime
from app.utils.typing_utils import pct, to_float, to_str_date


# 详情页通用表现周期，StockService 和 IndexService 使用同一组展示口径。
# offset 表示从按交易日倒序排列的 history 中向前回看多少条记录。
PERFORMANCE_PERIODS: List[Tuple[str, str, int]] = [
    ("5D", "近5日", 5),
    ("1M", "近1月", 20),
    ("3M", "近3月", 60),
    ("6M", "近6月", 120),
    ("1Y", "近1年", 250),
]


def compact_dict(data: Dict[str, Any]) -> Dict[str, Any]:
    """
    删除值为 None 的字段，减少前端需要处理的空字段噪音。

    注意：0、空字符串、False 都是有效业务值，不能被误删；因此这里只过滤 None。
    """
    return {key: value for key, value in data.items() if value is not None}


def round_optional(value: Any, digits: int = 4) -> Optional[float]:
    """
    安全地将数据库数值转换为 float 并四舍五入。

    - None 和空字符串表示数据库无值，保持为 None，方便 compact_dict 统一移除；
    - Decimal、int、float、字符串数字等类型交给 to_float 兼容处理；
    - digits 默认 4 位，金额类字段通常由调用方显式传入 2 位。
    """
    if value is None or value == "":
        return None
    return round(to_float(value), digits)


def metric_trend(value: Optional[float]) -> str:
    """根据指标数值给出前端趋势标识：正数 UP、负数 DOWN、零值 FLAT、缺失 UNKNOWN。"""
    if value is None:
        return "UNKNOWN"
    if value > 0:
        return "UP"
    if value < 0:
        return "DOWN"
    return "FLAT"


def metric_card(
    key: str,
    title: str,
    value: Any,
    unit: str | None = None,
    sort_order: int = 0,
) -> Dict[str, Any]:
    """
    构建详情页顶部指标卡。

    - value 使用 round_optional 保持数值展示口径一致；
    - displayValue 由 value + unit 组成；
    - trend 统一使用 metric_trend。
    """
    numeric_value = round_optional(value, 2)
    return compact_dict({
        "key": key,
        "title": title,
        "value": numeric_value,
        "unit": unit,
        "displayValue": f"{numeric_value}{unit or ''}" if numeric_value is not None else None,
        "trend": metric_trend(numeric_value),
        "status": "NORMAL",
        "description": None,
        "compareText": None,
        "sortOrder": sort_order,
    })


def build_market_quote(
    row: Optional[Dict[str, Any]],
    *,
    price_unit: str,
    include_full_symbol: bool = False,
    include_turnover_rate_free: bool = False,
    trade_time: str = "15:00:00",
) -> Optional[Dict[str, Any]]:
    """
    构建股票/指数共用的行情快照结构。

    不同点通过参数控制：
    - price_unit：股票为“元”，指数为“点”；
    - include_full_symbol：指数页面原有 quote 顶层带 fullSymbol，股票页面原结构不带；
    - include_turnover_rate_free：指数行情需要自由流通换手率，股票行情默认不输出该字段；
    - trade_time：当前日线数据默认使用收盘时间 15:00:00。
    """
    if not row:
        return None

    pre_close = to_float(row.get("pre_close"))
    high = to_float(row.get("high"))
    low = to_float(row.get("low"))
    amplitude = round((high - low) / pre_close * 100, 2) if pre_close else None

    result = {
        "fullSymbol": row.get("full_symbol") if include_full_symbol else None,
        "tradeDate": to_str_date(row.get("trade_date")),
        "tradeTime": trade_time,
        "close": round_optional(row.get("close")),
        "preClose": round_optional(row.get("pre_close")),
        "open": round_optional(row.get("open")),
        "high": round_optional(row.get("high")),
        "low": round_optional(row.get("low")),
        "chgAmt": round_optional(row.get("chg_amt")),
        "chgPct": round_optional(row.get("chg_pct")),
        "amplitude": amplitude,
        "volume": round_optional(row.get("volume"), 2),
        "turnover": round_optional(row.get("turnover"), 2),
        "turnoverRate": round_optional(row.get("turnover_rate")),
        "turnoverRateFree": round_optional(row.get("turnover_rate_free")) if include_turnover_rate_free else None,
        "marketStatus": current_market_status(),
        "priceUnit": price_unit,
    }
    return compact_dict(result)


def build_performance(
    history: List[Dict[str, Any]],
    periods: Sequence[Tuple[str, str, int]] = PERFORMANCE_PERIODS,
) -> List[Dict[str, Any]]:
    """
    根据按交易日倒序排列的 K 线历史计算多周期表现。

    该函数仅依赖 history 中的 close/chg_pct 字段，适用于 stock_k_daily 和 index_k_daily：
    - changePercent：当前收盘价相对回看期基准价的涨跌幅；
    - maxDrawdown：回看窗口内最大回撤；
    - volatility：回看窗口内日涨跌幅标准差。
    """
    if not history:
        return []

    current_close = to_float(history[0].get("close"))
    results: List[Dict[str, Any]] = []
    for period_code, period_name, offset in periods:
        if len(history) <= offset:
            continue

        base_close = to_float(history[offset].get("close"))
        window_desc = history[: offset + 1]
        close_values = [to_float(row.get("close")) for row in reversed(window_desc) if row.get("close") is not None]
        pct_values = [to_float(row.get("chg_pct")) for row in window_desc if row.get("chg_pct") is not None]

        results.append({
            "periodCode": period_code,
            "periodName": period_name,
            "changePercent": round(pct(current_close, base_close), 2),
            "maxDrawdown": max_drawdown(close_values),
            "volatility": volatility(pct_values),
            "rankingText": None,
        })
    return results


def estimate_trend_status(history: List[Dict[str, Any]]) -> str:
    """
    使用 5 日与 20 日均线粗略判断短中期趋势。

    history 约定为倒序排列，最近交易日位于第 0 位；当数据不足 5 条时返回 UNKNOWN。
    """
    if len(history) < 5:
        return "UNKNOWN"

    closes_desc = [to_float(row.get("close")) for row in history if row.get("close") is not None]
    if len(closes_desc) < 5:
        return "UNKNOWN"

    ma5 = sum(closes_desc[:5]) / 5
    ma20 = sum(closes_desc[:20]) / min(20, len(closes_desc))
    if ma5 > ma20 * 1.01:
        return "UPTREND"
    if ma5 < ma20 * 0.99:
        return "DOWNTREND"
    return "SIDEWAYS"


def estimate_valuation_status(
    pe_ttm: Optional[float],
    pb: Optional[float],
    pe_percentile: Optional[float],
    pb_percentile: Optional[float],
) -> str:
    """
    统一估值状态判断口径。

    优先使用历史分位判断，缺失历史分位时回退到 PE/PB 简单阈值：
    - 平均分位 <= 30：低估；
    - 平均分位 >= 70：高估；
    - 其余为合理；
    - PE/PB 都不可用时返回 UNKNOWN。
    """
    percentiles = [value for value in [pe_percentile, pb_percentile] if value is not None]
    if percentiles:
        avg_percentile = sum(percentiles) / len(percentiles)
        if avg_percentile <= 30:
            return "UNDERVALUED"
        if avg_percentile >= 70:
            return "OVERVALUED"
        return "FAIR"

    if pe_ttm and pe_ttm > 0:
        if pe_ttm < 15:
            return "UNDERVALUED"
        if pe_ttm > 45:
            return "OVERVALUED"
        return "FAIR"
    if pb and pb > 0:
        if pb < 1.5:
            return "UNDERVALUED"
        if pb > 5:
            return "OVERVALUED"
        return "FAIR"
    return "UNKNOWN"


def build_valuation_conclusion(
    status: str,
    pe_ttm: Optional[float],
    pb: Optional[float],
    pe_percentile: Optional[float] = None,
    pb_percentile: Optional[float] = None,
) -> str:
    """
    生成估值结论文案。

    股票详情通常只展示 PE/PB；指数详情还会传入 PE/PB 历史分位。
    通过可选参数兼容两类页面，避免 service 层维护两份基本相同的文案逻辑。
    """
    if status == "UNKNOWN":
        return "估值数据不足，暂无法判断。"
    if pe_percentile is not None or pb_percentile is not None:
        return f"当前PE(TTM)为{pe_ttm}，PB为{pb}，PE分位为{pe_percentile}%，PB分位为{pb_percentile}%，综合判断为{status}。"
    return f"当前PE(TTM)为{pe_ttm}，PB为{pb}，综合判断为{status}。"


def build_analysis_history(
    overview: Dict[str, Any],
    *sources: Optional[Dict[str, Any]],
    analysis_id_prefix: str = "RULE",
    scene_name: str = "规则综合分析",
    model_version: str = "rule-v1",
) -> List[Dict[str, Any]]:
    """
    构建一条规则分析记录，保持 AnalysisRecord 结构稳定。

    sources 可传入行情、估值、财务等任意字典；函数会依次查找 camelCase 和 snake_case
    日期字段，兼容 repo 原始行与 service 已转换后的结构。
    """
    source_data_time = ""
    for source in sources:
        if not source:
            continue
        source_data_time = (
            source.get("tradeDate")
            or to_str_date(source.get("trade_date"))
            or source.get("endDate")
            or to_str_date(source.get("end_date"))
        )
        if source_data_time:
            break

    return [
        {
            "analysisId": f"{analysis_id_prefix}-{source_data_time or 'NA'}",
            "analysisTime": get_current_datetime().isoformat(),
            "sceneCode": "CUSTOM",
            "sceneName": scene_name,
            "conclusion": overview["overallView"],
            "conclusionText": overview["overallViewText"],
            "signalStrength": "MEDIUM",
            "riskLevel": overview["riskLevel"],
            "summary": overview["summary"],
            "keyPoints": overview.get("reasons", []),
            "sourceDataTime": source_data_time,
            "modelVersion": model_version,
        }
    ]


def build_default_watch_info() -> Dict[str, Any]:
    """当前数据库未提供自选状态时的统一默认结构，方便前端无需判空。"""
    return {
        "watchStatus": "NONE",
        "watchStatusText": "未关注",
        "groupNames": [],
    }


def build_stock_basic_lookup(stock_basic_map: Dict[str, Dict[str, Any]]) -> Dict[str, Dict[str, Any]]:
    """
    构建成分股基础资料查找表。

    BasicDataRepo.get_stock_basic_by_codes 返回以 full_symbol 为 key 的字典；
    指数成分表中的 stock_code 可能是纯 symbol，也可能是 full_symbol，因此这里同时放入两种 key。
    """
    lookup: Dict[str, Dict[str, Any]] = {}
    for full_symbol, row in stock_basic_map.items():
        if full_symbol:
            lookup[full_symbol] = row
        if row.get("symbol"):
            lookup[row["symbol"]] = row
    return lookup


def aggregate_weight(
    rows: List[Dict[str, Any]],
    field_name: str,
    fallback_name: str,
) -> List[Dict[str, Any]]:
    """
    按指定字段聚合权重，主要用于指数成分股行业/主题分布。

    rows 中的 weight 已经可以是数值或字符串；统一通过 to_float 转换，缺失时按 0 处理。
    """
    weight_map: Dict[str, float] = {}
    for row in rows:
        name = row.get(field_name) or fallback_name
        weight_map[name] = weight_map.get(name, 0.0) + to_float(row.get("weight"))

    return [
        {"name": name, "weight": round(weight, 4)}
        for name, weight in sorted(weight_map.items(), key=lambda item: item[1], reverse=True)
    ]


def net_flow(row: Dict[str, Any], level: str) -> Optional[float]:
    """计算小/中/大/特大单净流入金额，level 取 sm/md/lg/elg。"""
    buy = row.get(f"buy_{level}_amount")
    sell = row.get(f"sell_{level}_amount")
    if buy is None and sell is None:
        return None
    return to_float(buy) - to_float(sell)


def percentile(current: Optional[float], values: List[Any]) -> Optional[float]:
    """
    计算 current 在历史有效值中的百分位。

    仅纳入大于 0 的历史值，避免 PE/PB 缺失值、0 值或异常负值影响估值分位判断。
    """
    if current is None:
        return None
    valid = sorted(to_float(value) for value in values if value is not None and to_float(value) > 0)
    if not valid:
        return None
    count = sum(1 for value in valid if value <= current)
    return round(count / len(valid) * 100, 2)


def max_drawdown(values: List[float]) -> Optional[float]:
    """按正序净值/收盘价序列计算最大回撤百分比。"""
    if not values:
        return None
    peak = values[0]
    max_dd = 0.0
    for value in values:
        peak = max(peak, value)
        if peak:
            max_dd = min(max_dd, (value - peak) / peak * 100)
    return round(max_dd, 2)


def volatility(pct_values: List[float]) -> Optional[float]:
    """使用日涨跌幅标准差作为简单波动率；有效样本不足 2 个时返回 None。"""
    values = [value for value in pct_values if value is not None and not math.isnan(value)]
    if len(values) < 2:
        return None
    return round(statistics.stdev(values), 2)


def strip_exchange(full_symbol: str) -> str:
    """去除 full_symbol 的交易所/市场后缀，例如 000001.SZ -> 000001。"""
    return full_symbol.split(".")[0] if full_symbol else ""
