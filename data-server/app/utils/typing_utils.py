from datetime import date, datetime
from decimal import Decimal
import math
from typing import Any, Dict, Mapping, Optional

from app.utils.target_detail_utils import compact_dict
from app.utils.indicator_utils import round_or_none


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


def snake_to_camel(name: str) -> str:
    parts = name.split("_")
    return parts[0] + "".join(part[:1].upper() + part[1:] for part in parts[1:])


def camelize_row(row: Mapping[str, Any], date_fields: Optional[set[str]] = None) -> Dict[str, Any]:
        """snake_case 数据库行转 camelCase，日期转字符串，数值转 float。"""
        date_fields = date_fields or set()
        result: Dict[str, Any] = {}
        for key, value in row.items():
            camel_key = snake_to_camel(key)
            if key in date_fields:
                result[camel_key] = value
            elif isinstance(value, (Decimal, int, float)) and not isinstance(value, bool):
                result[camel_key] = round_or_none(value, 6)
            elif isinstance(value, (date, datetime)):
                result[camel_key] = value
            else:
                result[camel_key] = value
        return compact_dict(result)