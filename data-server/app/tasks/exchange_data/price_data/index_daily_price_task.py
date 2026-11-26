import logging
from typing import Any, cast
import numpy as np
import pandas as pd

from app.schemas.collection_task.task_template import ExecutionContext, OutputField, TaskRunRequest, TaskRunResult, TaskTemplateInfo
from app.tasks.pipeline import BaseCollectTask
from app.core.registry import registry
from app.core.providers import ts_provider
from app.core.common.enum import AssetType

logger = logging.getLogger(__name__)

class IndexDailyPriceTask(BaseCollectTask):
    """
    从tushare接口获取指定指数日线价格数据，包含指数代码、交易日期、开盘价、最高价、最低价、收盘价、成交量、成交金额等信息。
    该接口可以用于2种场景：
        1）定期（每日）采集指定指数的日线价格数据；
        2）一次性获取指定指数的历史日线价格数据。
    """

    def __init__(self):
        
        super().__init__()
        
        self.task_code = 'index_daily_price'

        self.ts = ts_provider

    async def collect(self, params: dict[str, Any], ctx: ExecutionContext) -> list[dict[str, Any]]:
        """
            采集指定指数的日线价格：
            1）原始ts_codes中同时包含市场指数和申万指数，调用index_daily前排除以.SI结尾的申万指数；
            2）市场指数逐个调用index_daily，相邻两次index_daily调用之间暂停0.5秒；
            3）通过index_classify(src="SW2021")获取L1/L2/L3三个等级的申万行业指数代码；
            4）传入trade_date时，通过sw_daily(trade_date=trade_date)一次获取当日申万指数行情，
               再仅保留index_classify返回的行业指数行情；
            5）未传入trade_date时，保留原有日期区间采集能力，按申万行业指数代码逐个调用sw_daily；
            6）最终统一整理为TaskTemplateInfo中output_fields定义的字段。
        """
        import asyncio

        # 读取参数，指数代码必填
        ts_codes = params.get("ts_codes", None)
        if ts_codes == None or ts_codes == "":
            raise ValueError("ts_codes参数不能为空")

        if isinstance(ts_codes, str):
            ts_code_list = [item.strip() for item in ts_codes.split(",") if item.strip()]
        else:
            ts_code_list = list(ts_codes)

        if len(ts_code_list) == 0:
            raise ValueError("ts_codes参数不能为空")

        output_fields = [
            "ts_code",
            "trade_date",
            "close",
            "open",
            "high",
            "low",
            "pre_close",
            "change",
            "pct_chg",
            "vol",
            "amount",
        ]

        # index_daily不采集申万行业指数，先从原始ts_codes中排除.SI结尾的指数
        market_ts_code_list = [
            str(ts_code).strip()
            for ts_code in ts_code_list
            if str(ts_code).strip() and not str(ts_code).strip().upper().endswith(".SI")
        ]

        def normalize_index_daily(index_daily: pd.DataFrame) -> pd.DataFrame:
            """将index_daily结果统一为任务输出字段及单位。"""
            index_daily = index_daily.copy()

            # index_daily交易额单位为千元，转换为元
            if 'amount' in index_daily.columns:
                index_daily['amount'] = index_daily['amount'] * 1000

            # index_daily交易量单位为手，转换为股
            if 'vol' in index_daily.columns:
                index_daily['vol'] = index_daily['vol'] * 100

            return index_daily.reindex(columns=output_fields)

        def normalize_sw_daily(sw_daily: pd.DataFrame) -> pd.DataFrame:
            """将sw_daily结果统一为index_daily的任务输出字段及单位。"""
            sw_daily = sw_daily.copy()

            # sw_daily涨跌幅字段为pct_change，统一为pct_chg
            if 'pct_change' in sw_daily.columns:
                sw_daily = sw_daily.rename(columns={'pct_change': 'pct_chg'})

            # sw_daily不返回pre_close，根据close和change还原昨收点位
            if 'pre_close' not in sw_daily.columns:
                if 'close' in sw_daily.columns and 'change' in sw_daily.columns:
                    sw_daily['pre_close'] = sw_daily['close'] - sw_daily['change']
                else:
                    sw_daily['pre_close'] = np.nan

            # sw_daily成交额单位为万元，转换为元
            if 'amount' in sw_daily.columns:
                sw_daily['amount'] = sw_daily['amount'] * 10000

            # sw_daily成交量单位为万股，转换为股
            if 'vol' in sw_daily.columns:
                sw_daily['vol'] = sw_daily['vol'] * 10000

            return sw_daily.reindex(columns=output_fields)

        result_list: list[pd.DataFrame] = []
        trade_date = params.get("trade_date", None)

        # 采集市场指数
        if trade_date != None:
            for index, index_code in enumerate(market_ts_code_list):
                logger.info(
                    f"开始采集市场指数日线价格数据，ts_code={index_code}, trade_date={trade_date}"
                )
                index_daily = await self.ts.index_daily(
                    ts_code=index_code,
                    trade_date=trade_date,
                )
                if index_daily is not None and not index_daily.empty:
                    result_list.append(normalize_index_daily(index_daily))

                # 相邻两次index_daily调用之间暂停0.5秒，防止超过接口频率限制
                if index < len(market_ts_code_list) - 1:
                    await asyncio.sleep(0.5)
        else:
            start_date = params.get("start_date", '')
            end_date = params.get("end_date", '')

            for index, index_code in enumerate(market_ts_code_list):
                logger.info(
                    f"开始采集市场指数日线价格数据，ts_code={index_code}, "
                    f"start_date={start_date}, end_date={end_date}"
                )
                index_daily = await self.ts.index_daily(
                    ts_code=index_code,
                    start_date=start_date,
                    end_date=end_date,
                )
                if index_daily is not None and not index_daily.empty:
                    result_list.append(normalize_index_daily(index_daily))

                # 相邻两次index_daily调用之间暂停0.5秒，防止超过接口频率限制
                if index < len(market_ts_code_list) - 1:
                    await asyncio.sleep(0.5)

        # 获取申万2021版L1/L2/L3三个等级的行业指数代码
        sw_classify = await self.ts.index_classify(src="SW2021")

        sw_ts_code_list: list[str] = []
        if (
            sw_classify is not None
            and not sw_classify.empty
            and 'index_code' in sw_classify.columns
        ):
            if 'level' in sw_classify.columns:
                sw_classify = sw_classify[
                    sw_classify['level'].astype(str).isin(["L1", "L2", "L3"])
                ]

            for index_code in sw_classify['index_code'].dropna().tolist():
                index_code = str(index_code).strip()
                if not index_code:
                    continue

                ts_code = (
                    index_code
                    if index_code.upper().endswith(".SI")
                    else f"{index_code}.SI"
                )
                if ts_code not in sw_ts_code_list:
                    sw_ts_code_list.append(ts_code)

        if trade_date != None:
            # 每日更新时一次获取当日所有申万指数行情，再过滤出三级行业分类中的指数
            logger.info(f"开始采集申万行业指数日线价格数据，trade_date={trade_date}")
            sw_daily = await self.ts.sw_daily(trade_date=trade_date)

            if (
                sw_daily is not None
                and not sw_daily.empty
                and 'ts_code' in sw_daily.columns
                and len(sw_ts_code_list) > 0
            ):
                sw_ts_code_set = {ts_code.upper() for ts_code in sw_ts_code_list}
                sw_daily = sw_daily[
                    sw_daily['ts_code']
                    .astype(str)
                    .str.strip()
                    .str.upper()
                    .isin(sw_ts_code_set)
                ]

                if not sw_daily.empty:
                    result_list.append(normalize_sw_daily(sw_daily))
        else:
            # 保留原有日期区间采集场景；为避免sw_daily全市场区间查询超过单次行数限制，
            # 按行业指数代码逐个提取
            start_date = params.get("start_date", '')
            end_date = params.get("end_date", '')

            for index_code in sw_ts_code_list:
                logger.info(
                    f"开始采集申万行业指数日线价格数据，ts_code={index_code}, "
                    f"start_date={start_date}, end_date={end_date}"
                )
                sw_daily = await self.ts.sw_daily(
                    ts_code=index_code,
                    start_date=start_date,
                    end_date=end_date,
                )
                if sw_daily is not None and not sw_daily.empty:
                    result_list.append(normalize_sw_daily(sw_daily))
                await asyncio.sleep(1)

        if len(result_list) > 0:
            index_daily = pd.concat(result_list, ignore_index=True)
        else:
            index_daily = pd.DataFrame(columns=output_fields)

        index_daily = index_daily.reindex(columns=output_fields)
        index_daily = index_daily.replace([np.inf, -np.inf], np.nan)
        index_daily = index_daily.astype(object).where(index_daily.notnull(), None)

        return cast(
            list[dict[str, Any]],
            index_daily.rename(columns=str).to_dict("records"),
        )


async def index_daily_price_handler(request: TaskRunRequest) -> TaskRunResult:
    """采集任务的handler，负责调用task的execute方法并返回结果"""
    task = IndexDailyPriceTask()
    
    return await task.execute(request)


def register_index_daily_price_task():
    """注册task和handler到registry中"""
    registry.register_handler('index_daily_price_handler', index_daily_price_handler)

    registry.register_task_template(
        TaskTemplateInfo(
            task_code='index_daily_price',
            task_name='指数日线价格数据采集',
            task_desc="""采集指定指数列表的日线价格，包含两种场景：
            1）传入交易日期，获取指定指数在该交易日的日线价格数据，此时start_date和end_date不需要传入，如果传入了trade_date参数，start_date和end_date将被忽略；
            2）传入开始日期和结束日期，获取指定指数在该日期范围内的日线价格数据，用于历史数据采集。如果没有传入开始和结束日期，则默认采集指定指数的所有历史日线价格数据（最大8000条）
            从参数ts_codes读取需要初始化的指数代码列表，多个指数代码用英文逗号分隔，在实际采集过程中对每个指数代码单独采集数据，并将采集结果合并后返回。""",
            handler_name='index_daily_price_handler',
            data_source='TUSHARE',
            asset_type=AssetType.INDEX,
            biz_type='日线价格',
            params_schema=[
                OutputField(
                    field_code="ts_codes",
                    field_name="指数代码",
                    field_type="STRING",
                    required=True,
                    unique_key=False,
                    sort_no=1,
                    field_desc="指数代码列表，需要采集的指数代码列表，需要包括后缀，多个指数代码用英文逗号分隔，如000001.SH,399001.SZ等"
                ),
                OutputField(
                    field_code="trade_date",
                    field_name="交易日期",
                    field_type="DATE",
                    required=False,
                    unique_key=False,
                    sort_no=2,
                    field_desc="交易日期，格式为YYYYMMDD，选填，如果传入了trade_date参数，则start_date和end_date不需要传入"
                ),
                OutputField(
                    field_code="start_date",
                    field_name="开始日期",
                    field_type="DATE",
                    required=False,
                    unique_key=False,
                    sort_no=3,
                    field_desc="开始日期，一般为当月第一天，格式为YYYYMMDD，选填，如果没有传入start_date，则以end_date向前最多采集8000条数据"
                ),
                OutputField(
                    field_code="end_date",
                    field_name="结束日期",
                    field_type="DATE",
                    required=False,
                    unique_key=False,
                    sort_no=4,
                    field_desc="结束日期，一般为当月最后一天，格式为YYYYMMDD，选填，如果没有传入end_date，则以start_date向后最多采集8000条数据"
                )
            ],
            output_fields=[
                OutputField(
                    field_code="ts_code",
                    field_name="指数代码全称",
                    field_type="STRING",
                    required=True,
                    unique_key=True,
                    sort_no=1,
                    field_desc="指数代码全称"
                ),
                OutputField(
                    field_code="trade_date",
                    field_name="交易日期",
                    field_type="DATE",
                    required=True,
                    unique_key=True,
                    sort_no=2,
                    field_desc="交易日期"
                ),
                OutputField(
                    field_code="close",
                    field_name="收盘价",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=3,
                    field_desc="收盘价"
                ),
                OutputField(
                    field_code="open",
                    field_name="开盘价",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=4,
                    field_desc="开盘价"
                ),
                OutputField(
                    field_code="high",
                    field_name="最高价",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=5,
                    field_desc="最高价"
                ),
                OutputField(
                    field_code="low",
                    field_name="最低价",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=6,
                    field_desc="最低价"
                ),
                OutputField(
                    field_code="pre_close",
                    field_name="昨收价",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=7,
                    field_desc="昨收价"
                ),
                OutputField(
                    field_code="change",
                    field_name="涨跌额",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=8,
                    field_desc="涨跌额"
                ),                
                OutputField(
                    field_code="pct_chg",
                    field_name="涨跌幅",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=9,
                    field_desc="涨跌幅（百分比）"
                ),
                OutputField(
                    field_code="vol",
                    field_name="成交量（股）",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=10,
                    field_desc="成交量，单位为股"
                ),
                OutputField(
                    field_code="amount",
                    field_name="成交金额（元）",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=11,
                    field_desc="成交金额，单位为元"
                )
            ]
        )
    )