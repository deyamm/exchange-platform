
from datetime import date, datetime
from decimal import Decimal
import re
from typing import Any, Dict, List, Mapping, Optional, Tuple

from app.utils.number_utils import round_optional


IDENTIFIER_RE = re.compile(r"^[A-Za-z_][A-Za-z0-9_]*$")

def quote_identifier(name: str) -> str:
    """
    动态 schema/table/column 必须严格校验，禁止直接拼接未校验输入。
    """
    if not IDENTIFIER_RE.match(name):
        raise ValueError(f"非法数据库标识符: {name}")
    return f'`{name}`'  # mysql使用反引号作为标识符引用符


def clause_params(prefix: str, values: List[str]) -> Tuple[str, Dict[str, str]]:
    """
    生成SQL 占位符和参数字典
    占位符需要求格式为 :key 的参数，参数字典的key与占位符中的key对应，value为实际值。
    例如：prefix="code", values=["A", "B"] -> (":code_0, :code_1", {"code_0": "A", "code_1": "B"})
    """
    names = []
    params = {}

    for i, value in enumerate(values):
        key = f"{prefix}_{i}"
        names.append(f":{key}")
        params[key] = value

    return ",".join(names), params


def _snake_to_camel(name: str) -> str:
    parts = name.split("_")
    return parts[0] + "".join(part[:1].upper() + part[1:] for part in parts[1:])


def camelize_row(row: Mapping[str, Any], date_fields: Optional[set[str]] = None) -> Dict[str, Any]:
    """snake_case 数据库行转 camelCase，日期转字符串，数值转 float。"""
    date_fields = date_fields or set()
    result: Dict[str, Any] = {}
    for key, value in row.items():
        camel_key = _snake_to_camel(key)
        if key in date_fields:
            result[camel_key] = value
        elif isinstance(value, (Decimal, int, float)) and not isinstance(value, bool):
            result[camel_key] = round_optional(value, 6)
        elif isinstance(value, (date, datetime)):
            result[camel_key] = value
        else:
            result[camel_key] = value
    return result