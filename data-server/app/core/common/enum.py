from datetime import date
from enum import Enum
import logging


class AssetType(str, Enum):
    """
    资产类型
    STOCK: 股票
    INDEX: 指数
    FUTURE: 期货
    FUND: 基金
    BOND: 债券
    """
    STOCK = "STOCK"
    INDEX = "INDEX"
    FUTURE = "FUTURE"
    FUND = "FUND"
    BOND = "BOND"


class PriceAdjustType(str, Enum):
    """
    股价的复权方法
    NONE: ""，不复权
    FORWARD: qfq，前复权
    BACKWARD: hfq，后复权
    """

    code: str
    ts_param: str

    NONE = ("NONE", "")
    FORWARD = ("FORWARD", "qfq")
    BACKWARD = ("BACKWARD", "hfq")

    def __new__(cls, code: str, ts_param: str):
        obj = str.__new__(cls, code)
        obj._value_ = code
        obj.ts_param = ts_param
        return obj


class PricePeriodType(str, Enum):
    """
    K线级别
    DAILY: daily，日K线
    WEEKLY：weekly，周K线
    MONTHLY：monthly，月k线
    """
    DAILY = "daily"
    WEEKLY = "weekly"
    MONTHLY = "monthly"

    def db_code(self) -> int:
        return {'daily': 1, 'weekly': 2, 'monthly': 3}[self.value]

    def em_period_chg(self) -> str:
        return {'daily': '日k', 'weekly': '周k', 'monthly': '月k'}[self.value]

class StockMarketType(str, Enum):
    """参考 Tushare 股票基础信息接口返回的 market 字段，设置市场类型枚举"""
    MAIN = "主板"  # 主板
    CYB = "创业板"  # 创业板
    KCB = "科创板"  # 科创板
    BSE = "北交所"  # 北京证券交易所
    OTHER = "其他"  # 其他市场


class PublisherType(str, Enum):
    """
    指数发布方类型。

    基于 Tushare 指数基本信息接口中的市场分类，
    补充东方财富、同花顺等常用指数发布方。

    Attributes:
        value:
            发布方代码，例如 SSE、CSI、SW。
        num:
            内部数字编号。
        suffix:
            内部使用的发布方/指数代码后缀。
        label:
            中文显示名称。
        exchange_name:
            如果发布方本身是证券交易所，则记录交易所全称，否则为空字符串。
    """

    num: int
    suffix: str
    label: str
    exchange_name: str

    # ------------------------------------------------------------------
    # 交易所指数
    # ------------------------------------------------------------------

    SSE = ("SSE", 1, "SH", "上交所指数", "上海证券交易所")
    SZSE = ("SZSE", 2, "SZ", "深交所指数", "深圳证券交易所")
    BSE = ("BSE", 3, "BJ", "北交所指数", "北京证券交易所")

    # ------------------------------------------------------------------
    # 专业指数公司 / 金融机构
    # ------------------------------------------------------------------

    CSI = ("CSI", 4, "CSI", "中证指数", "")
    CICC = ("CICC", 5, "CIC", "中金指数", "")
    SW = ("SW", 6, "SI", "申万指数", "")
    MSCI = ("MSCI", 7, "MI", "MSCI指数", "")

    # ------------------------------------------------------------------
    # 数据平台指数
    # ------------------------------------------------------------------

    EM = ("EM", 8, "DC", "东方财富指数", "")
    THS = ("THS", 9, "TI", "同花顺指数", "")

    # ------------------------------------------------------------------
    # 其他
    # ------------------------------------------------------------------

    OTH = ("OTH", 99, "", "其他指数", "")

    def __new__(cls, code: str, num: int, suffix: str, label: str, exchange_name: str):
        obj = str.__new__(cls, code)
        obj._value_ = code

        obj.num = num
        obj.suffix = suffix
        obj.label = label
        obj.exchange_name = exchange_name

        return obj

    # ==================================================================
    # 基础表现
    # ==================================================================

    def __str__(self) -> str:
        return self.value

    def __int__(self) -> int:
        """允许 int(PublisherType.SSE)。"""
        return self.num

    @property
    def code(self) -> str:
        """value 的语义化别名。"""
        return self.value

    @property
    def display_name(self) -> str:
        """用于 UI / 日志展示。"""
        return self.label

    # ==================================================================
    # 类型判断
    # ==================================================================

    @property
    def is_other(self) -> bool:
        """是否为未知/其他发布方。"""
        return self is PublisherType.OTH

    @property
    def is_exchange(self) -> bool:
        """
        发布方本身是否为证券交易所。

        SSE / SZSE / BSE -> True
        CSI / SW / MSCI ... -> False
        """
        return bool(self.exchange_name)

    @property
    def is_data_platform(self) -> bool:
        """是否属于行情/财经数据平台发布的指数。"""
        return self in {
            PublisherType.EM,
            PublisherType.THS,
        }

    @property
    def is_professional_index_provider(self) -> bool:
        """是否属于专业指数机构/金融机构。"""
        return self in {
            PublisherType.CSI,
            PublisherType.CICC,
            PublisherType.SW,
            PublisherType.MSCI,
        }

    # ==================================================================
    # value 查询
    # ==================================================================

    @classmethod
    def from_value(cls, value: str) -> "PublisherType":
        """
        根据 code 获取 PublisherType。

        严格模式：
            找不到时抛 ValueError。

        Example:
            PublisherType.from_value("SSE")
        """
        if not value:
            raise ValueError("PublisherType value cannot be empty")

        value = str(value).strip().upper()

        try:
            return cls(value)
        except ValueError:
            raise ValueError(f"No PublisherType with value={value!r}") from None

    @classmethod
    def search_value(cls, value: str) -> "PublisherType":
        """
        根据 value 查找。

        容错模式：
            找不到时返回 OTH，并记录 warning。

        适合外部数据清洗。
        """
        if not value:
            return cls.OTH

        value = str(value).strip().upper()

        try:
            return cls(value)
        except ValueError:
            logging.warning("未找到匹配的指数发布方，value=%s，返回默认值 OTH", value)
            return cls.OTH

    # ==================================================================
    # num 查询
    # ==================================================================

    @classmethod
    def from_num(cls, num: int) -> "PublisherType":
        """根据数字编号查询，找不到抛异常。"""
        for item in cls:
            if item.num == num:
                return item

        raise ValueError(f"No PublisherType with num={num!r}")

    @classmethod
    def search_num(cls, num: int) -> "PublisherType":
        """根据数字编号查询，找不到返回 OTH。"""
        try:
            return cls.from_num(int(num))
        except (TypeError, ValueError):
            logging.warning("未找到匹配的指数发布方，num=%s，返回默认值 OTH", num)
            return cls.OTH

    # ==================================================================
    # label 查询
    # ==================================================================

    @classmethod
    def from_label(cls, label: str) -> "PublisherType":
        """根据中文 label 查询，找不到抛异常。"""
        if not label:
            raise ValueError("PublisherType label cannot be empty")

        label = str(label).strip()

        for item in cls:
            if item.label == label:
                return item

        raise ValueError(f"No PublisherType with label={label!r}")

    @classmethod
    def search_label(cls, label: str) -> "PublisherType":
        """根据中文 label 查询，找不到返回 OTH。"""
        if not label:
            return cls.OTH

        label = str(label).strip()

        for item in cls:
            if item.label == label:
                return item

        logging.warning("未找到匹配的指数发布方，label=%s，返回默认值 OTH", label)
        return cls.OTH

    # ==================================================================
    # suffix 查询
    # ==================================================================

    @classmethod
    def from_suffix(cls, suffix: str) -> "PublisherType":
        """
        根据 suffix 查询发布方。

        Example:
            PublisherType.from_suffix("SH") -> PublisherType.SSE
        """
        if not suffix:
            raise ValueError("PublisherType suffix cannot be empty")

        suffix = str(suffix).strip().upper().lstrip(".")

        for item in cls:
            if item.suffix and item.suffix.upper() == suffix:
                return item

        raise ValueError(f"No PublisherType with suffix={suffix!r}")

    @classmethod
    def search_suffix(cls, suffix: str) -> "PublisherType":
        """根据 suffix 查询，找不到返回 OTH。"""
        try:
            return cls.from_suffix(suffix)
        except (TypeError, ValueError):
            return cls.OTH

    # ==================================================================
    # 集合信息
    # ==================================================================

    @classmethod
    def codes(cls) -> tuple[str, ...]:
        """返回所有 code。"""
        return tuple(item.value for item in cls)

    @classmethod
    def nums(cls) -> tuple[int, ...]:
        """返回所有数字编号。"""
        return tuple(item.num for item in cls)

    @classmethod
    def labels(cls) -> tuple[str, ...]:
        """返回所有中文名称。"""
        return tuple(item.label for item in cls)

    @classmethod
    def suffixes(cls) -> tuple[str, ...]:
        """返回所有非空 suffix。"""
        return tuple(item.suffix for item in cls if item.suffix)

    @classmethod
    def exchange_publishers(cls) -> tuple["PublisherType", ...]:
        """返回交易所类指数发布方。"""
        return tuple(item for item in cls if item.is_exchange)

    @classmethod
    def non_exchange_publishers(cls) -> tuple["PublisherType", ...]:
        """返回非交易所类指数发布方。"""
        return tuple(item for item in cls if not item.is_exchange and not item.is_other)

    # ==================================================================
    # 序列化
    # ==================================================================

    def to_dict(self) -> dict:
        """完整序列化。"""
        return {
            "code": self.value,
            "num": self.num,
            "suffix": self.suffix,
            "label": self.label,
            "exchange_name": self.exchange_name,
        }

    @classmethod
    def dict_list(cls) -> list[dict]:
        """
        返回所有枚举项的字典列表。

        很适合接口返回给前端。
        """
        return [
            item.to_dict()
            for item in cls
        ]


class IndexCategory(str, Enum):
    """
    指数类别，基于Tushare指数基本信息接口中的分类
    """
    num: int
    label: str

    COMPOSITE = ('COMPOSITE', 1, '综合指数')
    SIZE = ('SIZE', 2, '规模指数')
    STYLE = ('STYLE', 3, '风格指数')
    THEMATIC = ('THEMATIC', 4, '主题指数')
    INDUSTRY = ('INDUSTRY', 5, '行业指数')
    STRATEGY = ('STRATEGY', 6, '策略指数')
    GROWTH = ('GROWTH', 7, '成长指数')
    VALUE = ('VALUE', 8, '价值指数')
    CONCEPT = ('CONCEPT', 9, '概念指数') # 用于同花顺和东方财富的概念指数分类
    OTHER = ('OTHER', 99, '其他指数')

    def __new__(cls, code: str, num: int, label: str):
        obj = str.__new__(cls, code)
        obj._value_ = code
        obj.num = num
        obj.label = label
        return obj

    def __str__(self) -> str:
        return self.value

    @classmethod
    def search_value(cls, value: str) -> "IndexCategory":
        for item in cls:
            if item.value == value:
                return item
        logging.warning(f"未找到匹配的指数分类，value={value}，返回默认值OTHER")
        return cls.OTHER

    @classmethod
    def from_num(cls, num: int) -> "IndexCategory":
        for item in cls:
            if item.num == num:
                return item
        raise ValueError(f"No IndexCategory with num={num}")

    @classmethod
    def from_label(cls, label: str) -> "IndexCategory":
        for item in cls:
            if item.label == label:
                return item
        raise ValueError(f"No IndexCategory with label={label}")

    def to_dict(self) -> dict:
        return {
            "code": self.value,
            "num": self.num,
            "label": self.label,
        }


class QuarterType(str, Enum):
    """自然季度及对应的 A 股定期财务报告类型"""

    number: int
    report_type: str
    name_cn: str
    report_name: str
    start_md: str
    end_md: str
    disclosure_deadline_md: str

    Q1 = ("Q1", 1, "Q1",        "第一季度", "一季报", "0101", "0331", "0430")
    Q2 = ("Q2", 2, "HALF_YEAR", "第二季度", "半年报", "0401", "0630", "0831")
    Q3 = ("Q3", 3, "Q3",        "第三季度", "三季报", "0701", "0930", "1031")
    Q4 = ("Q4", 4, "ANNUAL",    "第四季度", "年报",   "1001", "1231", "0430")

    def __new__(cls, code, number, report_type, name_cn, report_name, start_md, end_md, disclosure_deadline_md):
        obj = str.__new__(cls, code)
        obj._value_ = code
        obj.number = number
        obj.report_type = report_type
        obj.name_cn = name_cn
        obj.report_name = report_name
        obj.start_md = start_md
        obj.end_md = end_md
        obj.disclosure_deadline_md = disclosure_deadline_md
        return obj

    @property
    def code(self) -> str:
        return self.value

    @property
    def start_month(self) -> int:
        return int(self.start_md[:2])

    @property
    def end_month(self) -> int:
        return int(self.end_md[:2])

    @property
    def is_annual(self) -> bool:
        return self == QuarterType.Q4

    def start_date(self, year: int) -> date:
        """获取季度起始日期"""
        return date(year, self.start_month, 1)

    def end_date(self, year: int) -> date:
        """获取季度结束日期，即报告期截止日"""
        return date(
            year, self.end_month, int(self.end_md[2:])
        )

    def disclosure_deadline(self, year: int) -> date:
        """获取常规财报披露截止日期"""
        if self.is_annual:
            year += 1

        md = self.disclosure_deadline_md
        return date(year, int(md[:2]), int(md[2:]))

    @classmethod
    def from_number(cls, number: int) -> "QuarterType":
        """根据季度序号获取季度"""
        return cls(f"Q{number}")

    @classmethod
    def from_date(cls, dt: date) -> "QuarterType":
        """根据日期获取所属季度"""
        return cls.from_number((dt.month - 1) // 3 + 1)

    def previous(self) -> "QuarterType":
        """上一季度，不处理年份"""
        return self.from_number((self.number - 2) % 4 + 1)

    def next(self) -> "QuarterType":
        """下一季度，不处理年份"""
        return self.from_number(self.number % 4 + 1)


class TradingRange(str, Enum):

    code: str
    trading_days: int
    
    M1 = ("1M", 22)
    M3 = ("3M", 66)
    M6 = ("6M", 132)
    Y1 = ("1Y", 250)
    Y2 = ("2Y", 500)
    Y3 = ("3Y", 750)
    Y5 = ("5Y", 1250)
    Y10 = ("10Y", 2500)

    def __new__(cls, code: str, trading_days: int):
        obj = str.__new__(cls, code)
        obj._value_ = code
        obj.trading_days = trading_days
        return obj