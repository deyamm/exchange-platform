"""收益、风险、分位与区间表现计算工具。

该模块只依赖基础数值工具，不依赖页面 builder、技术指标模块或 service/repo。
"""

from __future__ import annotations

import math
import statistics
from typing import Any, Dict, Iterable, List, Mapping, Optional, Sequence

from app.utils.number_utils import round_optional, to_float, to_float_or_none


def percentile_value(values: Iterable[Any], percentile: float) -> Optional[float]:
    """计算指定分位值。

    percentile 传入 0-100，例如 30 表示 30 分位。采用线性插值，样本不足时返回 None。
    """
    nums = sorted(v for v in (to_float_or_none(x) for x in values) if v is not None)
    if not nums:
        return None
    if len(nums) == 1:
        return nums[0]
    pct = min(100.0, max(0.0, float(percentile))) / 100.0
    pos = (len(nums) - 1) * pct
    lower = math.floor(pos)
    upper = math.ceil(pos)
    if lower == upper:
        return nums[int(pos)]
    return nums[lower] + (nums[upper] - nums[lower]) * (pos - lower)


def percentile_rank(value: Any, values: Iterable[Any]) -> Optional[float]:
    """计算 value 在样本中的历史分位，返回 0-100。"""
    number = to_float_or_none(value)
    nums = [v for v in (to_float_or_none(x) for x in values) if v is not None]
    if number is None or not nums:
        return None
    less_or_equal = sum(1 for v in nums if v <= number)
    return round(less_or_equal / len(nums) * 100, 2)


def annualized_volatility_from_pct(chg_pct_values: Iterable[Any], periods_per_year: int = 252) -> Optional[float]:
    """由日涨跌幅百分比计算年化波动率，返回百分比。"""
    returns = [to_float_or_none(x) for x in chg_pct_values]
    nums = [x / 100 for x in returns if x is not None]
    if len(nums) < 2:
        return None
    return statistics.pstdev(nums) * math.sqrt(periods_per_year) * 100


def max_drawdown_from_rows(rows: Sequence[Mapping[str, Any]], value_field: str = "close") -> Optional[float]:
    """计算区间最大回撤，返回百分比，输入按日期升序或降序均可。"""
    values = [to_float_or_none(row.get(value_field)) for row in rows]
    values = [v for v in values if v is not None and v > 0]
    if len(values) < 2:
        return None
    peak = values[0]
    max_dd = 0.0
    for value in values:
        peak = max(peak, value)
        if peak > 0:
            max_dd = min(max_dd, (value - peak) / peak)
    return max_dd * 100


def _normalize_rows_ascending(rows: Sequence[Mapping[str, Any]]) -> List[Dict[str, Any]]:
    """按 trade_date 升序规范化行情行，兼容 repo 返回的 DESC 数据。"""
    normalized = [dict(row) for row in rows]
    return sorted(normalized, key=lambda item: str(item.get("trade_date") or ""))


def _format_date(value: Any) -> Optional[str]:
    """轻量日期格式化，避免工具函数依赖 service 层。"""
    if value is None:
        return None
    text = str(value)
    return text[:10]


def _range_window_size(range_code: str) -> int:
    """前端 QuoteRangeCode 到交易日窗口的近似映射。"""
    code = (range_code or "1Y").upper()
    return {
        "1M": 21,
        "3M": 63,
        "6M": 126,
        "1Y": 252,
        "2Y": 504,
    }.get(code, 252)


def build_index_range_performance(
    rows: Sequence[Mapping[str, Any]],
    window_size: int = 250,
) -> Optional[Dict[str, Any]]:
    """构建指数行情走势页区间表现。

    输入为 index_k_daily 历史行情行，可为升序或降序；输出结构对齐
    indexDetailType.IndexRangePerformance。该函数只基于可直接计算的行情字段，
    Sharpe/Calmar 等需要无风险利率或更完整基准口径的字段先采用简化算法。
    """
    if not rows:
        return None

    rows_asc = _normalize_rows_ascending(rows)
    window = rows_asc[-window_size:]
    if len(window) < 2:
        return None

    first = window[0]
    latest = window[-1]
    first_close = to_float_or_none(first.get("close"))
    latest_close = to_float_or_none(latest.get("close"))
    high_values = [to_float_or_none(row.get("high")) for row in window]
    low_values = [to_float_or_none(row.get("low")) for row in window]
    turnover_values = [to_float_or_none(row.get("turnover")) for row in window]
    volume_values = [to_float_or_none(row.get("volume")) for row in window]
    chg_values = [to_float_or_none(row.get("chg_pct")) for row in window]

    highs = [value for value in high_values if value is not None]
    lows = [value for value in low_values if value is not None]
    turnovers = [value for value in turnover_values if value is not None]
    volumes = [value for value in volume_values if value is not None]

    period_return = None
    if first_close not in (None, 0) and latest_close is not None:
        period_return = (latest_close - first_close) / first_close * 100

    years = max(len(window) / 252, 1 / 252)
    annualized_return = None
    if period_return is not None:
        annualized_return = ((1 + period_return / 100) ** (1 / years) - 1) * 100

    volatility = annualized_volatility_from_pct(chg_values)
    max_drawdown = max_drawdown_from_rows(window)
    sharpe_ratio = None
    if annualized_return is not None and volatility not in (None, 0):
        sharpe_ratio = annualized_return / volatility
    calmar_ratio = None
    if annualized_return is not None and max_drawdown not in (None, 0):
        calmar_ratio = annualized_return / abs(max_drawdown)

    high_price = max(highs) if highs else None
    low_price = min(lows) if lows else None
    amplitude = None
    if high_price is not None and low_price is not None and first_close not in (None, 0):
        amplitude = (high_price - low_price) / first_close * 100

    avg_turnover = statistics.fmean(turnovers) if turnovers else None
    period_turnover = sum(turnovers) if turnovers else None
    volume_avg = statistics.fmean(volumes) if volumes else None
    volume_expansion_days = None
    if volume_avg not in (None, 0):
        volume_expansion_days = sum(1 for value in volumes if value is not None and value >= volume_avg * 1.5)

    max_volume_date = None
    if volume_values:
        max_idx = max(range(len(window)), key=lambda idx: volume_values[idx] or -1)
        max_volume_date = _format_date(window[max_idx].get("trade_date"))

    price_percentile = percentile_rank(latest_close, [row.get("close") for row in window])
    new_high_count = sum(1 for row in window if to_float_or_none(row.get("high")) == high_price) if high_price is not None else 0
    new_low_count = sum(1 for row in window if to_float_or_none(row.get("low")) == low_price) if low_price is not None else 0

    result = {
        "windowSize": window_size,
        "startDate": _format_date(first.get("trade_date")),
        "endDate": _format_date(latest.get("trade_date")),
        "riskReturn": {
            "highPrice": round_optional(high_price),
            "lowPrice": round_optional(low_price),
            "periodReturn": round_optional(period_return),
            "annualizedReturn": round_optional(annualized_return),
            "amplitude": round_optional(amplitude),
            "volatility": round_optional(volatility),
            "maxDrawdown": round_optional(max_drawdown),
            "sharpeRatio": round_optional(sharpe_ratio),
            "calmarRatio": round_optional(calmar_ratio),
        },
        "trading": {
            "periodTurnover": round_optional(period_turnover, 2),
            "averageTurnover": round_optional(avg_turnover, 2),
            "periodTurnoverRate": None,
            "volumeExpansionDays": volume_expansion_days,
            "maxVolumeDate": max_volume_date,
            "turnoverChangeVsPrevious": None,
        },
        "structure": {
            "currentPricePercentile": price_percentile,
            "newHighCount": new_high_count,
            "newLowCount": new_low_count,
            "priceConcentrationRange": None if low_price is None or high_price is None else f"{round_optional(low_price)}-{round_optional(high_price)}",
            "turnoverConcentrationRange": None if not turnovers else f"{round_optional(min(turnovers), 2)}-{round_optional(max(turnovers), 2)}",
        },
    }
    result["sections"] = [
        {
            "title": "风险收益",
            "items": [
                {"label": "区间收益率", "value": result["riskReturn"].get("periodReturn"), "unit": "%"},
                {"label": "最大回撤", "value": result["riskReturn"].get("maxDrawdown"), "unit": "%"},
                {"label": "年化波动率", "value": result["riskReturn"].get("volatility"), "unit": "%"},
            ],
        },
        {
            "title": "交易活跃度",
            "items": [
                {"label": "区间成交额", "value": result["trading"].get("periodTurnover"), "unit": "千元"},
                {"label": "平均成交额", "value": result["trading"].get("averageTurnover"), "unit": "千元"},
                {"label": "放量天数", "value": result["trading"].get("volumeExpansionDays"), "unit": "天"},
            ],
        },
    ]
    return result


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