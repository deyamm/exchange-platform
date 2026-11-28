"""通用数值转换与百分比工具。

该模块位于 utils 的基础层，不依赖 indicator/market/target_detail 等业务工具模块。
"""

from __future__ import annotations

import math
from typing import Any, Optional


def to_float(value: Any, default: float = 0.0) -> float:
    """将输入安全转换为 float；缺失或非法值返回 default。"""
    if value is None:
        return default
    try:
        number = float(value)
    except (TypeError, ValueError):
        return default
    if math.isnan(number) or math.isinf(number):
        return default
    return number


def to_float_or_none(value: Any) -> Optional[float]:
    """将输入安全转换为 float；缺失、非法、NaN 或无穷值返回 None。"""
    if value is None or value == "":
        return None
    try:
        number = float(value)
    except (TypeError, ValueError):
        return None
    if math.isnan(number) or math.isinf(number):
        return None
    return number


def round_optional(value: Any, digits: int = 4) -> Optional[float]:
    """安全四舍五入并保持 None 语义。"""
    number = to_float_or_none(value)
    return round(number, digits) if number is not None else None


def pct(current: Any, base: Any) -> float:
    """计算 current 相对 base 的百分比变化。"""
    current_value = to_float(current)
    base_value = to_float(base)
    if base_value == 0:
        return 0.0
    return (current_value / base_value - 1) * 100


def calc_lookback_pct(start_value: float, end_value: float, n: int) -> float:
    """保留原有回看百分比接口。"""
    if n <= 0 or start_value == 0:
        return 0.0
    return round(pct(start_value, end_value), 2)