
import re
from typing import Dict, List, Tuple


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