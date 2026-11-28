"""行情与技术指标图表结构构建工具。

只负责把已计算好的数据转换为前端图表结构，不负责指标计算。
"""

from __future__ import annotations

from typing import Any, Dict, List, Mapping, Optional, Sequence

from app.utils.datetime_utils import to_date
from app.utils.number_utils import round_optional


def build_ohlc_item(row: Mapping[str, Any]) -> Dict[str, Any]:
    """构建单根 K 线数据，附带常用技术指标字段。"""
    return {
        "fullSymbol": row.get("full_symbol"),
        "tradeDate": to_date(row.get("trade_date")).isoformat() if row.get("trade_date") else None,
        "open": round_optional(row.get("open")),
        "high": round_optional(row.get("high")),
        "low": round_optional(row.get("low")),
        "close": round_optional(row.get("close")),
        "preClose": round_optional(row.get("pre_close")),
        "chgAmt": round_optional(row.get("chg_amt")),
        "chgPct": round_optional(row.get("chg_pct")),
        "volume": round_optional(row.get("volume"), 2),
        "turnover": round_optional(row.get("turnover"), 2),
        "ma5": round_optional(row.get("ma5")),
        "ma10": round_optional(row.get("ma10")),
        "ma20": round_optional(row.get("ma20")),
        "ma60": round_optional(row.get("ma60")),
        "ma120": round_optional(row.get("ma120")),
        "ma250": round_optional(row.get("ma250")),
        "ema9": round_optional(row.get("ema9")),
        "ema12": round_optional(row.get("ema12")),
        "ema20": round_optional(row.get("ema20")),
        "ema26": round_optional(row.get("ema26")),
        "bollMid": round_optional(row.get("bollMid")),
        "bollUpper": round_optional(row.get("bollUpper")),
        "bollLower": round_optional(row.get("bollLower")),
        "sar": round_optional(row.get("sar")),
        "sarTrend": row.get("sarTrend"),
        "vwap1": round_optional(row.get("vwap1")),
        "vwap5": round_optional(row.get("vwap5")),
        "vwap10": round_optional(row.get("vwap10")),
        "vwap20": round_optional(row.get("vwap20")),
        "vwap60": round_optional(row.get("vwap60")),
        "vwap120": round_optional(row.get("vwap120")),
        "superTrend10": round_optional(row.get("superTrend10")),
        "superTrendUpper10": round_optional(row.get("superTrendUpper10")),
        "superTrendLower10": round_optional(row.get("superTrendLower10")),
        "superTrendDirection10": row.get("superTrendDirection10"),
        "turnoverRate": round_optional(row.get("turnoverRate")),
        "macdDiff": round_optional(row.get("macdDiff")),
        "macdDea": round_optional(row.get("macdDea")),
        "macd": round_optional(row.get("macd")),
        "kdjK": round_optional(row.get("kdjK")),
        "kdjD": round_optional(row.get("kdjD")),
        "kdjJ": round_optional(row.get("kdjJ")),
        "rsi6": round_optional(row.get("rsi6")),
        "rsi12": round_optional(row.get("rsi12")),
        "rsi14": round_optional(row.get("rsi14")),
        "rsi24": round_optional(row.get("rsi24")),
        "cci14": round_optional(row.get("cci14")),
        "dmiPdi14": round_optional(row.get("dmiPdi14")),
        "dmiMdi14": round_optional(row.get("dmiMdi14")),
        "dmiAdx14": round_optional(row.get("dmiAdx14")),
        "atr14": round_optional(row.get("atr14")),
        "obv": round_optional(row.get("obv"), 2),
        "obv14": round_optional(row.get("obv14"), 2),
    }


def build_technical_charts(
    rows: Sequence[Mapping[str, Any]],
    price_unit: str = "点",
    volume_unit: str = "股/手",
) -> List[Dict[str, Any]]:
    """构建技术指标图表。

    技术指标统一来自 indicator_utils.calculate_technical_indicator_rows。
    前端字段结构保持 chartId/title/chartType/period/xAxis/series，不再把多个主图指标混在
    indexTechnicalIndicators 一个图表里，而是按指标和周期拆成独立 chart。
    """
    charts: List[Dict[str, Any]] = []

    def number_series(field: str, digits: int = 4) -> List[Optional[float]]:
        return [round_optional(row.get(field), digits) for row in rows]

    def append_indicator_chart(
        chart_id: str,
        title: str,
        chart_type: str,
        x_axis: List[str],
        series: List[Dict[str, Any]],
    ) -> None:
        charts.append({
            "chartId": chart_id,
            "title": title,
            "chartType": chart_type,
            "period": "DAILY",
            "xAxis": x_axis,
            "series": series,
        })

    x_axis = [to_date(row.get("trade_date")).isoformat() for row in rows]
    # k线图
    charts.append({
        "chartId": "KlineDaily",
        "title": "日K线",
        "chartType": "CANDLESTICK",
        "period": "DAILY",
        "xAxis": x_axis,
        "series": [{
            "name": "OHLC",
            "unit": price_unit,
            "data": [build_ohlc_item(row) for row in rows],
        }],
        "options": {
            "mainIndicators": ["MA", "EMA", "BOLL", "SAR", "VWAP", "SUPER_TREND"],
            "subIndicators": ["VOL", "TURNOVER_RATE", "MACD", "KDJ", "RSI", "CCI", "DMI", "ATR", "OBV"],
        },
    })

    # 主图指标：MA
    for period in (5, 10, 20, 60, 120, 250):
        append_indicator_chart(
            chart_id=f"MA{period}",
            title=f"MA{period}",
            chart_type="LINE",
            x_axis=x_axis,
            series=[{"name": f"MA{period}", "data": number_series(f"MA{period}"), "unit": price_unit}],
        )

    # 主图指标：EMA。保留旧的 EMA20 字段，同时追加前端默认 9/12/26。
    for period in (9, 12, 20, 26):
        append_indicator_chart(
            chart_id=f"EMA{period}",
            title=f"EMA{period}",
            chart_type="LINE",
            x_axis=x_axis,
            series=[{"name": f"EMA{period}", "data": number_series(f"EMA{period}"), "unit": price_unit}],
        )

    # 主图指标：BOLL
    append_indicator_chart(
        chart_id="BOLL",
        title="BOLL",
        chart_type="LINE",
        x_axis=x_axis,
        series=[
            {"name": "upper", "data": number_series("bollUpper"), "unit": price_unit},
            {"name": "mid", "data": number_series("bollMid"), "unit": price_unit},
            {"name": "lower", "data": number_series("bollLower"), "unit": price_unit},
        ],
    )

    # 主图指标：SAR
    append_indicator_chart(
        chart_id="SAR",
        title="SAR",
        chart_type="LINE",
        x_axis=x_axis,
        series=[{"name": "SAR", "data": number_series("SAR"), "unit": price_unit}],
    )

    # 主图指标：VWAP
    for period in (1, 5, 10, 20, 60, 120):
        append_indicator_chart(
            chart_id=f"VWAP{period}",
            title=f"VWAP{period}",
            chart_type="LINE",
            x_axis=x_axis,
            series=[{"name": f"VWAP{period}", "data": number_series(f"VWAP{period}"), "unit": price_unit}],
        )

    # 主图指标：SuperTrend
    append_indicator_chart(
        chart_id="SUPER_TREND",
        title="SUPER_TREND",
        chart_type="LINE",
        x_axis=x_axis,
        series=[
            {"name": "SuperTrend", "data": number_series("superTrend10"), "unit": price_unit},
            {"name": "SuperTrend Upper", "data": number_series("superTrendUpper10"), "unit": price_unit},
            {"name": "SuperTrend Lower", "data": number_series("superTrendLower10"), "unit": price_unit},
        ],
    )

    # 副图指标：VOL
    append_indicator_chart(
        chart_id="VOL",
        title="VOL",
        chart_type="BAR",
        x_axis=x_axis,
        series=[{"name": "volume", "data": [round_optional(row.get("volume"), 2) for row in rows], "unit": "股/手"}],
    )

    # 保留原有成交额历史图，避免影响前端已有功能。
    charts.append({
        "chartId": "TURNOVER",
        "title": "成交额",
        "chartType": "BAR",
        "period": "DAILY",
        "xAxis": x_axis,
        "series": [{"name": "turnover", "data": [round_optional(row.get("turnover"), 2) for row in rows], "unit": "元"}],
    })

    # 副图指标：TURNOVER_RATE
    append_indicator_chart(
        chart_id="TURNOVER_RATE",
        title="换手率",
        chart_type="LINE",
        x_axis=x_axis,
        series=[{"name": "turnoverRate", "data": number_series("turnoverRate"), "unit": "%"}],
    )

    # 副图指标：MACD
    append_indicator_chart(
        chart_id="MACD",
        title="MACD",
        chart_type="LINE",
        x_axis=x_axis,
        series=[
            {"name": "DIF", "data": number_series("macdDiff"), "unit": price_unit},
            {"name": "DEA", "data": number_series("macdDea"), "unit": price_unit},
            {"name": "MACD", "data": number_series("macd"), "unit": price_unit},
        ],
    )

    # 副图指标：KDJ
    append_indicator_chart(
        chart_id="KDJ",
        title="KDJ",
        chart_type="LINE",
        x_axis=x_axis,
        series=[
            {"name": "K", "data": number_series("kdjK"), "unit": price_unit},
            {"name": "D", "data": number_series("kdjD"), "unit": price_unit},
            {"name": "J", "data": number_series("kdjJ"), "unit": price_unit},
        ],
    )

    # 副图指标：RSI
    append_indicator_chart(
        chart_id="RSI",
        title="RSI",
        chart_type="LINE",
        x_axis=x_axis,
        series=[{"name": "RSI14", "data": number_series("RSI14"), "unit": price_unit}],
    )

    # 副图指标：CCI
    append_indicator_chart(
        chart_id="CCI",
        title="CCI",
        chart_type="LINE",
        x_axis=x_axis,
        series=[{"name": "CCI14", "data": number_series("CCI14"), "unit": price_unit}],
    )

    # 副图指标：DMI
    append_indicator_chart(
        chart_id="DMI",
        title="DMI",
        chart_type="LINE",
        x_axis=x_axis,
        series=[
            {"name": "+DI", "data": number_series("dmiPdi14"), "unit": price_unit},
            {"name": "-DI", "data": number_series("dmiMdi14"), "unit": price_unit},
            {"name": "ADX", "data": number_series("dmiAdx14"), "unit": price_unit},
        ],
    )

    # 副图指标：ATR
    append_indicator_chart(
        chart_id="ATR",
        title="ATR",
        chart_type="LINE",
        x_axis=x_axis,
        series=[{"name": "ATR14", "data": number_series("ATR14"), "unit": price_unit}],
    )

    # 副图指标：OBV
    append_indicator_chart(
        chart_id="OBV",
        title="OBV",
        chart_type="LINE",
        x_axis=x_axis,
        series=[
            {"name": "OBV", "data": number_series("OBV", 2), "unit": price_unit},
            {"name": "OBV14", "data": number_series("OBV14", 2), "unit": price_unit},
        ],
    )

    return charts


def build_index_valuation_chart(indicator_history: Sequence[Mapping[str, Any]]) -> Dict[str, Any]:
    """构建指数估值历史图表，输入按日期升序或降序均可。"""

    valuation_rows = list(reversed(list(indicator_history)))
    return {
        "chartId": "indexValuationHistory",
        "title": "指数估值历史",
        "chartType": "LINE",
        "period": "DAILY",
        "xAxis": [to_date(row.get("trade_date")).isoformat() for row in valuation_rows],
        "series": [
            {"name": "peTtm", "data": [round_optional(row.get("pe_ttm")) for row in valuation_rows]},
            {"name": "pb", "data": [round_optional(row.get("pb")) for row in valuation_rows]},
        ],
    }