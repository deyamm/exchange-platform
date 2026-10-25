"""
业务常量：K 线技术指标配置。

本文件根据前端指标参数文案整理，提供：
- 主图指标 main_indicators
- 副图指标 sub_indicators
- 常用查询与校验函数

约定：
- code 使用前端枚举值，例如 MA、MACD、TURNOVER_RATE
- default_period 适合周期型指标
- default_params 保存更明确的参数结构
- default_param_text 与前端展示文案保持一致
"""

from __future__ import annotations

from typing import Any, Literal

IndicatorCategory = Literal["main", "sub"]

main_indicators: dict[str, dict[str, Any]] = {
    "MA": {
        "name": "均线",
        "unit": "点",
        "description": (
            "均线是指一定时期内价格的平均值，常用来判断趋势方向、支撑位和阻力位。"
            "周期越短越灵敏，周期越长越平滑。"
        ),
        "default_period": [5, 10, 20, 60, 120, 250],
        "default_params": {"periods": [5, 10, 20, 60, 120, 250]},
        "default_param_text": "默认 5、10、20、60、120、250",
        "support_customization": True,
    },
    "EMA": {
        "name": "指数移动平均线",
        "unit": "点",
        "description": (
            "EMA 是对近期价格赋予更高权重的移动平均线，相比普通均线对价格变化更敏感，"
            "常用于趋势判断和交叉信号分析。"
        ),
        "default_period": [12, 26, 9],
        "default_params": {"periods": [12, 26, 9]},
        "default_param_text": "默认 12、26、9",
        "support_customization": True,
    },
    "BOLL": {
        "name": "布林线",
        "unit": "点",
        "description": (
            "布林线由中轨、上轨和下轨组成，中轨通常为移动平均线，上下轨根据标准差计算，"
            "常用于观察价格波动区间、压力支撑和突破行情。"
        ),
        "default_period": [20],
        "default_params": {"middle_period": 20, "std_multiplier": 2},
        "default_param_text": "默认中轨 20，边轨 2",
        "support_customization": True,
    },
    "SAR": {
        "name": "抛物线转向",
        "unit": "点",
        "description": (
            "SAR 用于跟踪趋势并提示潜在转向位置，价格在 SAR 点位上方通常表示偏强，"
            "价格跌破 SAR 点位可能表示趋势转弱。"
        ),
        "default_period": [],
        "default_params": {"step": 0.02, "max_step": 0.2},
        "default_param_text": "默认 0.02、0.2",
        "support_customization": True,
    },
    "VWAP": {
        "name": "成交量加权平均价",
        "unit": "点",
        "description": (
            "VWAP 按成交量对价格进行加权平均，常用于观察市场平均成交成本，"
            "也可作为盘中强弱和交易执行质量的参考。"
        ),
        "default_period": [1, 5, 10, 20, 60, 120],
        "default_params": {"periods": [1, 5, 10, 20, 60, 120]},
        "default_param_text": "默认 1、5、10、20、60、120",
        "support_customization": True,
    },
    "SUPER_TREND": {
        "name": "超级趋势",
        "unit": "点",
        "description": (
            "SuperTrend 基于 ATR 计算趋势跟踪线，常用于识别当前趋势方向和止损参考位。"
            "价格位于趋势线上方通常偏多，位于下方通常偏空。"
        ),
        "default_period": [10],
        "default_params": {"period": 10, "multiplier": 3},
        "default_param_text": "默认周期 10，乘数 3",
        "support_customization": True,
    },
}


sub_indicators: dict[str, dict[str, Any]] = {
    "VOL": {
        "name": "成交量",
        "unit": "股/手",
        "description": (
            "成交量以柱状图展示一定周期内的成交数量，常用于配合价格判断放量、缩量和交易活跃度。"
        ),
        "default_period": [],
        "default_params": {},
        "default_param_text": "成交量柱状图",
        "support_customization": False,
    },
    "TURNOVER": {
        "name": "成交额",
        "unit": "元",
        "description": (
            "成交额以柱状图展示一定周期内的成交金额，常用于观察资金流入流出和交易活跃度。"
        ),
        "default_period": [],
        "default_params": {},
        "default_param_text": "成交额柱状图",
        "support_customization": False,
    },
    "TURNOVER_RATE": {
        "name": "换手率",
        "unit": "%",
        "description": (
            "换手率表示一定区间内成交量相对于流通股本的比例，常用于衡量股票交易活跃程度。"
        ),
        "default_period": [],
        "default_params": {},
        "default_param_text": "区间换手率",
        "support_customization": False,
    },
    "MACD": {
        "name": "指数平滑异同移动平均线",
        "unit": "点",
        "description": (
            "MACD 由快线、慢线和柱状图组成，常用于判断趋势强弱、买卖信号和背离现象。"
        ),
        "default_period": [12, 26, 9],
        "default_params": {"fast_period": 12, "slow_period": 26, "signal_period": 9},
        "default_param_text": "默认 12、26、9",
        "support_customization": True,
    },
    "KDJ": {
        "name": "随机指标",
        "unit": "点",
        "description": (
            "KDJ 用于衡量价格在一定周期内的相对位置，常用于短中期超买超卖和拐点判断。"
        ),
        "default_period": [9, 3, 3],
        "default_params": {"period": 9, "k_period": 3, "d_period": 3},
        "default_param_text": "默认 9、3、3",
        "support_customization": True,
    },
    "RSI": {
        "name": "相对强弱指标",
        "unit": "点",
        "description": (
            "RSI 用于衡量上涨和下跌力量的相对强弱，常用于识别超买、超卖和趋势动能变化。"
        ),
        "default_period": [14],
        "default_params": {"period": 14},
        "default_param_text": "默认 14",
        "support_customization": True,
    },
    "CCI": {
        "name": "顺势指标",
        "unit": "点",
        "description": (
            "CCI 用于衡量价格偏离统计均值的程度，常用于判断超买超卖、趋势加速和潜在反转。"
        ),
        "default_period": [14],
        "default_params": {"period": 14},
        "default_param_text": "默认 14",
        "support_customization": True,
    },
    "DMI": {
        "name": "动向指标",
        "unit": "点",
        "description": (
            "DMI 用于分析价格趋势方向和趋势强度，通常包含 +DI、-DI、ADX 等线。"
        ),
        "default_period": [14],
        "default_params": {"period": 14},
        "default_param_text": "默认 14",
        "support_customization": True,
    },
    "ATR": {
        "name": "真实波幅",
        "unit": "点",
        "description": (
            "ATR 用于衡量价格波动幅度，不直接判断方向，常用于波动率分析、仓位控制和止损距离设置。"
        ),
        "default_period": [14],
        "default_params": {"period": 14},
        "default_param_text": "默认 14",
        "support_customization": True,
    },
    "OBV": {
        "name": "能量潮",
        "unit": "点",
        "description": (
            "OBV 将成交量按涨跌方向累加，用于观察资金流向和量价配合情况。"
        ),
        "default_period": [14],
        "default_params": {"period": 14},
        "default_param_text": "默认 14",
        "support_customization": True,
    },
}


def normalize_indicator_code(code: str) -> str:
    """统一指标 code 格式。"""

    if not isinstance(code, str) or not code.strip():
        raise ValueError("indicator code must be a non-empty string")
    return code.strip().upper()


def get_main_indicator(code: str) -> dict[str, Any] | None:
    """根据 code 获取主图指标配置，未找到时返回 None。"""

    return main_indicators.get(normalize_indicator_code(code))


def get_sub_indicator(code: str) -> dict[str, Any] | None:
    """根据 code 获取副图指标配置，未找到时返回 None。"""

    return sub_indicators.get(normalize_indicator_code(code))


def get_indicator(code: str, category: IndicatorCategory | None = None) -> dict[str, Any] | None:
    """
    根据 code 获取指标配置。

    Args:
        code: 指标编码，例如 MA、MACD。
        category: 可选，传 main 只查主图，传 sub 只查副图；不传则先查主图再查副图。
    """

    normalized_code = normalize_indicator_code(code)

    if category == "main":
        return main_indicators.get(normalized_code)
    if category == "sub":
        return sub_indicators.get(normalized_code)

    return main_indicators.get(normalized_code) or sub_indicators.get(normalized_code)


def list_main_indicator_codes() -> list[str]:
    """返回所有主图指标 code。"""

    return list(main_indicators.keys())


def list_sub_indicator_codes() -> list[str]:
    """返回所有副图指标 code。"""

    return list(sub_indicators.keys())


def list_all_indicator_codes() -> list[str]:
    """返回所有指标 code，主图指标在前，副图指标在后。"""

    return list_main_indicator_codes() + list_sub_indicator_codes()


def is_main_indicator(code: str) -> bool:
    """判断 code 是否为主图指标。"""

    return normalize_indicator_code(code) in main_indicators


def is_sub_indicator(code: str) -> bool:
    """判断 code 是否为副图指标。"""

    return normalize_indicator_code(code) in sub_indicators


def is_valid_indicator(code: str) -> bool:
    """判断 code 是否为已支持的指标。"""

    normalized_code = normalize_indicator_code(code)
    return normalized_code in main_indicators or normalized_code in sub_indicators


def supports_customization(code: str, category: IndicatorCategory | None = None) -> bool:
    """判断指定指标是否支持参数自定义。"""

    indicator = get_indicator(code, category)
    return bool(indicator and indicator.get("support_customization"))


def get_default_period(code: str, category: IndicatorCategory | None = None) -> list[int | float]:
    """获取指标默认周期；没有周期参数时返回空列表。"""

    indicator = get_indicator(code, category)
    if not indicator:
        return []
    return list(indicator.get("default_period") or [])


def get_default_params(code: str, category: IndicatorCategory | None = None) -> dict[str, Any]:
    """获取指标默认参数；没有参数时返回空字典。"""

    indicator = get_indicator(code, category)
    if not indicator:
        return {}
    return dict(indicator.get("default_params") or {})


def get_default_param_text(code: str, category: IndicatorCategory | None = None) -> str:
    """获取指标默认参数展示文案。"""

    indicator = get_indicator(code, category)
    if not indicator:
        return ""
    return str(indicator.get("default_param_text") or "")


__all__ = [
    "IndicatorCategory",
    "main_indicators",
    "sub_indicators",
    "normalize_indicator_code",
    "get_main_indicator",
    "get_sub_indicator",
    "get_indicator",
    "list_main_indicator_codes",
    "list_sub_indicator_codes",
    "list_all_indicator_codes",
    "is_main_indicator",
    "is_sub_indicator",
    "is_valid_indicator",
    "supports_customization",
    "get_default_period",
    "get_default_params",
    "get_default_param_text",
]