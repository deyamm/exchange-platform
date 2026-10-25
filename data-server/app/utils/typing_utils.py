from decimal import Decimal
import math
from typing import Any


def to_float(v: Any, default: float = 0.0) -> float:
    if v is None:
        return default
    if isinstance(v, Decimal):
        return float(v)
    try:
        f = float(v)
        if math.isnan(f):
            return default
        return f
    except Exception:
        return default


def to_str_date(v: Any) -> str:
    if v is None:
        return ""
    return str(v)[:10]


def pct(current: Any, base: Any) -> float:
    c = to_float(current)
    b = to_float(base)
    if b == 0:
        return 0.0
    return (c / b - 1) * 100


def calc_lookback_pct(start_value: float, end_value: float, n: int) -> float:
    if n <= 0 or start_value == 0:
        return 0.0
    return round(pct(start_value, end_value), 2)