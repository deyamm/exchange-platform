from datetime import date, datetime, time, timedelta
import calendar
from zoneinfo import ZoneInfo

time_zone = ZoneInfo("Asia/Shanghai")

def get_current_datetime() -> datetime:
    """获取当前日期时间，时区为上海"""
    return datetime.now(time_zone)

def get_first_day_of_month(date: date) -> date:
    """获取指定日期所在月份的第一天"""
    return date.replace(day=1)

def get_last_day_of_month(date: date) -> date:
    """获取指定日期所在月份的最后一天"""
    return date.replace(day=calendar.monthrange(date.year, date.month)[1])

def get_latest_trade_date(trade_date: str | None) -> date:
    """
    获取最新的交易日期，
    后期可以从tushare获取交易日历
    当前先实现一个简单的逻辑：
     - 如果当前日期是周末，返回上一个工作日
     - 如果当前时间易于17:30，将今天作为交易日，否则返回上一个工作日
    
    :param trade_date: 指定交易日期，格式 YYYY-MM-DD；为空时返回最新数据
    :return: 最新的交易日期
    """
    if trade_date:
        now = datetime.strptime(trade_date, "%Y-%m-%d").replace(tzinfo=time_zone)
    else:
        now = datetime.now(time_zone)

    if now.weekday() >= 5:  # 如果是周末，返回上一个工作日
        return now.date() - timedelta(days=now.weekday() - 4)
    elif now.time() < datetime.strptime("17:30:00", "%H:%M:%S").time():  # 如果当前时间早于17:30，返回上一个工作日
        return now.date() - timedelta(days=1)
    return now.date()


def get_previous_trade_date(trade_date: date | str) -> date:
    """获取指定交易日期的前一个交易日期"""
    if isinstance(trade_date, str):
        trade_date = datetime.strptime(trade_date, "%Y-%m-%d").date()
    previous_date = trade_date - timedelta(days=1)
    while previous_date.weekday() >= 5:  # 跳过周末
        previous_date -= timedelta(days=1)
    return previous_date

def current_market_status() -> str:
    now = get_current_datetime()

    if now.weekday() >= 5:
        return "休市"

    t = now.time()
    if time(9, 30) <= t <= time(11, 30):
        return "交易中"
    if time(13, 0) <= t <= time(15, 0):
        return "交易中"

    return "已收盘"


def resolve_trade_date(trade_date: str | None = None) -> str:
    """
    统一 service 层的交易日期解析口径。

    - 调用方传入 trade_date 时，直接使用该日期，避免把指定历史日期再次按“最新交易日”回退；
    - 未传入 trade_date 时，统一调用 get_latest_trade_date(None) 获取最近可用交易日；
    - 返回值固定为 YYYY-MM-DD 字符串，便于 repo 查询参数和 meta.dataVersion 复用。
    """
    if trade_date and trade_date.strip():
        return trade_date.strip()
    return get_latest_trade_date(None).strftime("%Y-%m-%d")