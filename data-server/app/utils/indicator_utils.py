"""TargetDetail 技术指标与衍生指标工具函数。

本文件只放可复用的计算逻辑，不访问数据库，也不感知 service/repo。
stock_service.py 在拿到 repo 返回的原始行后调用这些函数，完成前端
TargetDetail 所需的 MA、EMA、BOLL、MACD、KDJ、RSI、波动率、回撤、分位等计算。

说明：
- 可直接由历史行情计算的指标在这里完整实现；
- 当前物理表无法直接提供或需要未来引入模型的数据，例如风险等级、Alpha/Beta，
  统一预留入口函数并返回固定/空值，便于后续替换实现。
"""

from __future__ import annotations

import math
import statistics
from decimal import Decimal
from typing import Any, Dict, Iterable, List, Mapping, Optional, Sequence, Tuple, cast

import pandas as pd


def to_float_or_none(value: Any) -> Optional[float]:
    """将数据库 Decimal / int / float / 字符串安全转为 float，空值返回 None。"""
    if value is None:
        return None
    if isinstance(value, bool):
        return float(value)
    if isinstance(value, Decimal):
        return float(value)
    if isinstance(value, (int, float)):
        if isinstance(value, float) and (math.isnan(value) or math.isinf(value)):
            return None
        return float(value)
    try:
        text = str(value).strip()
        if not text:
            return None
        number = float(text)
        return None if math.isnan(number) or math.isinf(number) else number
    except (TypeError, ValueError):
        return None


def round_or_none(value: Any, digits: int = 4) -> Optional[float]:
    """安全四舍五入，保持 None 语义。"""
    number = to_float_or_none(value)
    return round(number, digits) if number is not None else None


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


def _rows_to_dataframe(rows: Sequence[Mapping[str, Any]]) -> pd.DataFrame:
    """将 list[dict] 行情数据转为 DataFrame，保留原始字段。"""
    return pd.DataFrame([dict(row) for row in rows])


def _dataframe_to_rows(df: pd.DataFrame) -> List[Dict[str, Any]]:
    """将 DataFrame 转回 list[dict]，把 NaN/NaT 统一还原为 None。"""
    if df.empty:
        return []
    safe_df = df.astype(object).where(pd.notna(df), None)
    return cast(List[Dict[str, Any]], safe_df.to_dict(orient="records"))


def _numeric_series(df: pd.DataFrame, field: str) -> pd.Series:
    """从 DataFrame 中安全读取数值列，缺失时报错。"""
    if field not in df.columns:
        raise ValueError(f"字段 '{field}' 不存在于 DataFrame 中")
    return pd.to_numeric(df[field].map(to_float_or_none), errors="coerce")


def ema_series(values: Sequence[Any], period: int) -> List[Optional[float]]:
    """计算 EMA 序列。保留旧接口，但内部使用 pandas.DataFrame/Series 实现。"""
    series = pd.Series([to_float_or_none(value) for value in values], dtype="float64")
    ema = series.ewm(span=period, adjust=False, min_periods=1, ignore_na=True).mean()
    return [to_float_or_none(value) for value in ema.astype(object).where(pd.notna(ema), None).tolist()]


def calculate_ma_df(
    df: pd.DataFrame,
    value_field: str = "close",
    periods: Sequence[int] = (5, 10, 20, 60, 120, 250),
    prefix: str = "MA",
) -> pd.DataFrame:
    """基于 DataFrame 计算移动平均线 MA。"""
    result = df.copy()
    values = _numeric_series(result, value_field)
    for period in periods:
        result[f"{prefix}{period}"] = values.rolling(window=period, min_periods=period).mean()
    return result


def calculate_ema_df(
    df: pd.DataFrame,
    value_field: str = "close",
    periods: Sequence[int] = (9, 12, 20, 26),
    prefix: str = "EMA",
) -> pd.DataFrame:
    """基于 DataFrame 计算指数移动平均线 EMA。"""
    result = df.copy()
    values = _numeric_series(result, value_field)
    for period in periods:
        result[f"{prefix}{period}"] = values.ewm(span=period, adjust=False, min_periods=1, ignore_na=True).mean()
    return result


def calculate_boll_df(
    df: pd.DataFrame,
    value_field: str = "close",
    period: int = 20,
    multiple: float = 2.0,
) -> pd.DataFrame:
    """基于 DataFrame 计算布林带 BOLL。"""
    result = df.copy()
    values = _numeric_series(result, value_field)
    mid = values.rolling(window=period, min_periods=period).mean()
    std = values.rolling(window=period, min_periods=period).std(ddof=0)
    result["bollMid"] = mid
    result["bollUpper"] = mid + multiple * std
    result["bollLower"] = mid - multiple * std
    return result


def calculate_macd_df(
    df: pd.DataFrame,
    value_field: str = "close",
    fast: int = 12,
    slow: int = 26,
    signal: int = 9,
) -> pd.DataFrame:
    """基于 DataFrame 计算 MACD：diff=EMA12-EMA26，dea=diff 的 EMA9，macd=2*(diff-dea)。"""
    result = df.copy()
    values = _numeric_series(result, value_field)
    ema_fast = values.ewm(span=fast, adjust=False, min_periods=1, ignore_na=True).mean()
    ema_slow = values.ewm(span=slow, adjust=False, min_periods=1, ignore_na=True).mean()
    diff = ema_fast - ema_slow
    dea = diff.ewm(span=signal, adjust=False, min_periods=1, ignore_na=True).mean()
    result["macdDiff"] = diff
    result["macdDea"] = dea
    result["macd"] = 2 * (diff - dea)
    return result


def calculate_rsi_df(
    df: pd.DataFrame,
    value_field: str = "close",
    periods: Sequence[int] = (6, 12, 14, 24),
    prefix: str = 'RSI'
) -> pd.DataFrame:
    """基于 DataFrame 计算 RSI。默认同时保留历史字段 rsi6/rsi12/rsi24，并追加 rsi14。"""
    result = df.copy()
    closes = _numeric_series(result, value_field)
    delta = closes.diff()
    gains = delta.clip(lower=0)
    losses = (-delta.clip(upper=0))
    for period in periods:
        avg_gain = gains.rolling(window=period, min_periods=period).mean()
        avg_loss = losses.rolling(window=period, min_periods=period).mean()
        rs = avg_gain / avg_loss.replace(0, pd.NA)
        rsi = 100 - 100 / (1 + rs)
        rsi = rsi.mask((avg_loss == 0) & avg_gain.notna(), 100.0)
        result[f"{prefix}{period}"] = rsi
    return result


def calculate_kdj_df(
    df: pd.DataFrame,
    period: int = 9,
    k_period: int = 3,
    d_period: int = 3,
) -> pd.DataFrame:
    """基于 DataFrame 计算 KDJ。K/D 初始值为 50，J=3K-2D。"""
    result = df.copy()
    low = _numeric_series(result, "low")
    high = _numeric_series(result, "high")
    close = _numeric_series(result, "close")
    lowest_low = low.rolling(window=period, min_periods=period).min()
    highest_high = high.rolling(window=period, min_periods=period).max()
    denominator = highest_high - lowest_low
    rsv = ((close - lowest_low) / denominator * 100).where(denominator != 0)

    k_values: List[Optional[float]] = []
    d_values: List[Optional[float]] = []
    j_values: List[Optional[float]] = []
    k_value = 50.0
    d_value = 50.0
    k_weight = (k_period - 1) / k_period if k_period > 0 else 2 / 3
    d_weight = (d_period - 1) / d_period if d_period > 0 else 2 / 3

    for value in rsv.tolist():
        current_rsv = to_float_or_none(value)
        if current_rsv is None:
            k_values.append(None)
            d_values.append(None)
            j_values.append(None)
            continue
        k_value = k_weight * k_value + (1 - k_weight) * current_rsv
        d_value = d_weight * d_value + (1 - d_weight) * k_value
        k_values.append(k_value)
        d_values.append(d_value)
        j_values.append(3 * k_value - 2 * d_value)

    result["kdjK"] = k_values
    result["kdjD"] = d_values
    result["kdjJ"] = j_values
    return result


def calculate_atr_df(df: pd.DataFrame, period: int = 14, prefix: str = 'ATR') -> pd.DataFrame:
    """基于 DataFrame 计算 ATR，用于辅助波动风险判断。"""
    result = df.copy()
    high = _numeric_series(result, "high")
    low = _numeric_series(result, "low")
    close = _numeric_series(result, "close")
    prev_close = close.shift(1)
    true_range = pd.concat([
        high - low,
        (high - prev_close).abs(),
        (low - prev_close).abs(),
    ], axis=1).max(axis=1)
    result[f"{prefix}{period}"] = true_range.rolling(window=period, min_periods=period).mean()
    return result


def calculate_sar_df(df: pd.DataFrame, step: float = 0.02, max_step: float = 0.2) -> pd.DataFrame:
    """基于 DataFrame 计算抛物线转向指标 SAR。"""
    result = df.copy()
    high = _numeric_series(result, "high").tolist()
    low = _numeric_series(result, "low").tolist()
    close = _numeric_series(result, "close").tolist()
    n = len(result)
    sar_values: List[Optional[float]] = [None] * n
    trend_values: List[Optional[str]] = [None] * n
    if n == 0:
        result["SAR"] = sar_values
        result["SARTrend"] = trend_values
        return result

    first_high = to_float_or_none(high[0])
    first_low = to_float_or_none(low[0])
    if first_high is None or first_low is None:
        result["SAR"] = sar_values
        result["SARTrend"] = trend_values
        return result

    if n > 1 and to_float_or_none(close[1]) is not None and to_float_or_none(close[0]) is not None:
        is_uptrend = bool(close[1] >= close[0])
    else:
        is_uptrend = True

    ep = first_high if is_uptrend else first_low
    af = step
    sar_values[0] = first_low if is_uptrend else first_high
    trend_values[0] = "UP" if is_uptrend else "DOWN"

    for idx in range(1, n):
        cur_high = to_float_or_none(high[idx])
        cur_low = to_float_or_none(low[idx])
        if cur_high is None or cur_low is None or sar_values[idx - 1] is None:
            sar_values[idx] = sar_values[idx - 1]
            trend_values[idx] = trend_values[idx - 1]
            continue

        candidate = sar_values[idx - 1] + af * (ep - sar_values[idx - 1]) # type: ignore
        if is_uptrend:
            prev_low = to_float_or_none(low[idx - 1])
            prev2_low = to_float_or_none(low[idx - 2]) if idx >= 2 else prev_low
            floor_values = [value for value in (prev_low, prev2_low) if value is not None]
            if floor_values:
                candidate = min(candidate, *floor_values)
            if cur_low < candidate:
                is_uptrend = False
                sar_values[idx] = ep
                ep = cur_low
                af = step
            else:
                sar_values[idx] = candidate
                if cur_high > ep:
                    ep = cur_high
                    af = min(max_step, af + step)
        else:
            prev_high = to_float_or_none(high[idx - 1])
            prev2_high = to_float_or_none(high[idx - 2]) if idx >= 2 else prev_high
            cap_values = [value for value in (prev_high, prev2_high) if value is not None]
            if cap_values:
                candidate = max(candidate, *cap_values)
            if cur_high > candidate:
                is_uptrend = True
                sar_values[idx] = ep
                ep = cur_high
                af = step
            else:
                sar_values[idx] = candidate
                if cur_low < ep:
                    ep = cur_low
                    af = min(max_step, af + step)
        trend_values[idx] = "UP" if is_uptrend else "DOWN"

    result["SAR"] = sar_values
    result["SARTrend"] = trend_values
    return result


def calculate_vwap_df(
    df: pd.DataFrame,
    periods: Sequence[int] = (1, 5, 10, 20, 60, 120),
    prefix: str = "VWAP",
) -> pd.DataFrame:
    """基于 DataFrame 计算区间 VWAP。"""
    result = df.copy()
    high = _numeric_series(result, "high")
    low = _numeric_series(result, "low")
    close = _numeric_series(result, "close")
    volume = _numeric_series(result, "volume")
    typical_price = (high + low + close) / 3
    amount = typical_price * volume
    for period in periods:
        rolling_volume = volume.rolling(window=period, min_periods=period).sum()
        rolling_amount = amount.rolling(window=period, min_periods=period).sum()
        result[f"{prefix}{period}"] = (rolling_amount / rolling_volume).where(rolling_volume != 0)
    return result


def calculate_super_trend_df(
    df: pd.DataFrame,
    period: int = 10,
    multiplier: float = 3.0,
) -> pd.DataFrame:
    """基于 DataFrame 计算 SuperTrend。"""
    result = df.copy()
    high = _numeric_series(result, "high")
    low = _numeric_series(result, "low")
    close = _numeric_series(result, "close")
    prev_close = close.shift(1)
    true_range = pd.concat([
        high - low,
        (high - prev_close).abs(),
        (low - prev_close).abs(),
    ], axis=1).max(axis=1)
    atr = true_range.rolling(window=period, min_periods=period).mean()
    hl2 = (high + low) / 2
    basic_upper = hl2 + multiplier * atr
    basic_lower = hl2 - multiplier * atr

    final_upper: List[Optional[float]] = [None] * len(result)
    final_lower: List[Optional[float]] = [None] * len(result)
    super_trend: List[Optional[float]] = [None] * len(result)
    direction: List[Optional[str]] = [None] * len(result)

    for idx in range(len(result)):
        bu = to_float_or_none(basic_upper.iloc[idx])
        bl = to_float_or_none(basic_lower.iloc[idx])
        cur_close = to_float_or_none(close.iloc[idx])
        prev_close_value = to_float_or_none(close.iloc[idx - 1]) if idx > 0 else None
        if bu is None or bl is None or cur_close is None:
            continue

        if idx == 0 or final_upper[idx - 1] is None or final_lower[idx - 1] is None:
            final_upper[idx] = bu
            final_lower[idx] = bl
            super_trend[idx] = bl if cur_close >= bl else bu
            direction[idx] = "UP" if super_trend[idx] == bl else "DOWN"
            continue

        prev_fu = final_upper[idx - 1]
        prev_fl = final_lower[idx - 1]
        final_upper[idx] = bu if bu < prev_fu or (prev_close_value is not None and prev_close_value > prev_fu) else prev_fu # type: ignore
        final_lower[idx] = bl if bl > prev_fl or (prev_close_value is not None and prev_close_value < prev_fl) else prev_fl # type: ignore

        prev_st = super_trend[idx - 1]
        if prev_st == prev_fu:
            super_trend[idx] = final_upper[idx] if cur_close <= final_upper[idx] else final_lower[idx] # type: ignore
        elif prev_st == prev_fl:
            super_trend[idx] = final_lower[idx] if cur_close >= final_lower[idx] else final_upper[idx] # type: ignore
        else:
            super_trend[idx] = final_lower[idx] if cur_close >= final_lower[idx] else final_upper[idx] # type: ignore
        direction[idx] = "UP" if super_trend[idx] == final_lower[idx] else "DOWN"

    result[f"superTrend{period}"] = super_trend
    result[f"superTrendUpper{period}"] = final_upper
    result[f"superTrendLower{period}"] = final_lower
    result[f"superTrendDirection{period}"] = direction
    return result


def calculate_cci_df(df: pd.DataFrame, period: int = 14, prefix: str = "CCI") -> pd.DataFrame:
    """基于 DataFrame 计算 CCI。"""
    result = df.copy()
    typical_price = (_numeric_series(result, "high") + _numeric_series(result, "low") + _numeric_series(result, "close")) / 3
    ma = typical_price.rolling(window=period, min_periods=period).mean()
    mean_deviation = typical_price.rolling(window=period, min_periods=period).apply(
        lambda values: abs(values - values.mean()).mean(),
        raw=False,
    )
    result[f"{prefix}{period}"] = ((typical_price - ma) / (0.015 * mean_deviation)).where(mean_deviation != 0)
    return result


def calculate_dmi_df(df: pd.DataFrame, period: int = 14) -> pd.DataFrame:
    """基于 DataFrame 计算 DMI，返回 +DI、-DI、ADX。"""
    result = df.copy()
    high = _numeric_series(result, "high")
    low = _numeric_series(result, "low")
    close = _numeric_series(result, "close")
    up_move = high.diff()
    down_move = -low.diff()
    plus_dm = up_move.where((up_move > down_move) & (up_move > 0), 0.0)
    minus_dm = down_move.where((down_move > up_move) & (down_move > 0), 0.0)
    prev_close = close.shift(1)
    true_range = pd.concat([
        high - low,
        (high - prev_close).abs(),
        (low - prev_close).abs(),
    ], axis=1).max(axis=1)
    tr_sum = true_range.rolling(window=period, min_periods=period).sum()
    plus_di = (100 * plus_dm.rolling(window=period, min_periods=period).sum() / tr_sum).where(tr_sum != 0)
    minus_di = (100 * minus_dm.rolling(window=period, min_periods=period).sum() / tr_sum).where(tr_sum != 0)
    denominator = plus_di + minus_di
    dx = (100 * (plus_di - minus_di).abs() / denominator).where(denominator != 0)
    adx = dx.rolling(window=period, min_periods=period).mean()
    result[f"dmiPdi{period}"] = plus_di
    result[f"dmiMdi{period}"] = minus_di
    result[f"dmiAdx{period}"] = adx
    return result


def calculate_obv_df(df: pd.DataFrame, period: int = 14, prefix: str = "OBV") -> pd.DataFrame:
    """基于 DataFrame 计算 OBV，并追加 OBV 的 period 均线。"""
    result = df.copy()
    close = _numeric_series(result, "close")
    volume = _numeric_series(result, "volume").fillna(0.0)
    direction = close.diff().apply(lambda value: 1 if value > 0 else -1 if value < 0 else 0)
    obv = (direction * volume).fillna(0.0).cumsum()
    result[prefix] = obv
    result[f"{prefix}{period}"] = obv.rolling(window=period, min_periods=period).mean()
    return result


def calculate_turnover_rate_df(df: pd.DataFrame) -> pd.DataFrame:
    """规范化换手率字段，兼容 snake_case 原始字段和 camelCase 返回字段。"""
    result = df.copy()
    if "turnoverRate" not in result.columns:
        if "turnover_rate" in result.columns:
            result["turnoverRate"] = _numeric_series(result, "turnover_rate")
        elif "turnover_rate_free" in result.columns:
            result["turnoverRate"] = _numeric_series(result, "turnover_rate_free")
        else:
            result["turnoverRate"] = None
    return result


def calculate_technical_indicator_df(rows: Sequence[Mapping[str, Any]] | pd.DataFrame) -> pd.DataFrame:
    """一次性计算详情页主图/副图技术指标，输入必须按交易日期升序。"""
    result = rows.copy() if isinstance(rows, pd.DataFrame) else _rows_to_dataframe(rows)
    if result.empty:
        return result

    result = calculate_ma_df(result, periods=(5, 10, 20, 60, 120, 250))
    result = calculate_ema_df(result, periods=(9, 12, 20, 26))
    result = calculate_boll_df(result, period=20, multiple=2.0)
    result = calculate_sar_df(result, step=0.02, max_step=0.2)
    result = calculate_vwap_df(result, periods=(1, 5, 10, 20, 60, 120))
    result = calculate_super_trend_df(result, period=10, multiplier=3.0)

    result = calculate_turnover_rate_df(result)
    result = calculate_macd_df(result, fast=12, slow=26, signal=9)
    result = calculate_kdj_df(result, period=9, k_period=3, d_period=3)
    result = calculate_rsi_df(result, periods=(6, 12, 14, 24))
    result = calculate_cci_df(result, period=14)
    result = calculate_dmi_df(result, period=14)
    result = calculate_atr_df(result, period=14)
    result = calculate_obv_df(result, period=14)
    return result


def calculate_technical_indicator_rows(rows: Sequence[Mapping[str, Any]]) -> List[Dict[str, Any]]:
    """一次性计算详情页常用技术指标，输入必须按交易日期升序。
    这里需要包含足够的前置数据，保证计算结果的准确性。
    最终返回 list[dict]，以兼容 service 层和前端接口结构。
    """
    return _dataframe_to_rows(calculate_technical_indicator_df(rows))

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


def estimate_risk_level_placeholder(*_: Any, **__: Any) -> str:
    """风险等级暂未有稳定物理表/模型支撑，预留统一入口，后续替换真实实现。"""
    return "MEDIUM"


def calculate_alpha_beta_placeholder(*_: Any, **__: Any) -> Dict[str, Optional[float]]:
    """Alpha/Beta/相关性等需要基准收益序列，当前物理表未直接提供，暂返回空值。"""
    return {"alpha": None, "beta": None, "correlation": None}

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
            "highPrice": round_or_none(high_price),
            "lowPrice": round_or_none(low_price),
            "periodReturn": round_or_none(period_return),
            "annualizedReturn": round_or_none(annualized_return),
            "amplitude": round_or_none(amplitude),
            "volatility": round_or_none(volatility),
            "maxDrawdown": round_or_none(max_drawdown),
            "sharpeRatio": round_or_none(sharpe_ratio),
            "calmarRatio": round_or_none(calmar_ratio),
        },
        "trading": {
            "periodTurnover": round_or_none(period_turnover, 2),
            "averageTurnover": round_or_none(avg_turnover, 2),
            "periodTurnoverRate": None,
            "volumeExpansionDays": volume_expansion_days,
            "maxVolumeDate": max_volume_date,
            "turnoverChangeVsPrevious": None,
        },
        "structure": {
            "currentPricePercentile": price_percentile,
            "newHighCount": new_high_count,
            "newLowCount": new_low_count,
            "priceConcentrationRange": None if low_price is None or high_price is None else f"{round_or_none(low_price)}-{round_or_none(high_price)}",
            "turnoverConcentrationRange": None if not turnovers else f"{round_or_none(min(turnovers), 2)}-{round_or_none(max(turnovers), 2)}",
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