import logging
from typing import Any, cast

import numpy as np

from app.core.registry import registry
from app.schemas.collection_task.task_template import ExecutionContext, OutputField, TaskRunRequest, TaskRunResult, TaskTemplateInfo
from app.tasks.pipeline import BaseCollectTask
from app.core.providers import ts_provider
from app.core.common.enum import AssetType

logger = logging.getLogger(__name__)

class IndexDailyIndicatorDataTask(BaseCollectTask):
    """
    从tushare接口获取大盘指数每日指标数据，包含指数代码、交易日期、总市值、流通市值、总股本、流通股本、换手率、市盈率、市净率等信息。
    该接口可以用于2种场景：
        1）定期（每日）采集指定交易日期所有大盘指数的每日指标数据；
        2）一次性获取指定大盘指数的历史每日指标数据。
    """

    def __init__(self):

        super().__init__()

        self.task_code = 'index_daily_indicator_data'

        self.ts = ts_provider


    async def collect(self, params: dict[str, Any], ctx: ExecutionContext) -> list[dict[str, Any]]:
        """
            采集指数每日指标数据：
            1）原始ts_codes中同时包含市场指数和申万指数，调用index_daily_indicator前排除.SI结尾的指数；
            2）市场指数逐个调用index_daily_indicator，相邻两次调用之间暂停0.5秒；
            3）通过index_classify(src="SW2021")获取L1/L2/L3三个等级的申万行业指数代码；
            4）传入trade_date时，通过sw_daily(trade_date=trade_date)一次获取当日所有申万指数行情，
               再仅保留index_classify返回的行业指数；
            5）未传入trade_date时，保留原有日期区间采集能力，按申万行业指数逐个调用sw_daily；
            6）最终统一为index_dailybasic标准字段，sw_daily无法可靠对应的字段为空。
        """
        import asyncio

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
            "total_mv",
            "float_mv",
            "total_share",
            "float_share",
            "free_share",
            "turnover_rate",
            "turnover_rate_f",
            "pe",
            "pe_ttm",
            "pb",
        ]

        def normalize_index_daily_basic(index_daily_basic):
            """统一为index_dailybasic标准输出字段。"""
            index_daily_basic = index_daily_basic.copy()
            return index_daily_basic.reindex(columns=output_fields)

        def normalize_sw_daily(sw_daily):
            """
            将sw_daily能够可靠对应的字段映射为index_dailybasic标准字段：
            ts_code、trade_date、total_mv、float_mv、pe、pb。
            其余字段在sw_daily中没有同语义字段，统一留空。
            """
            sw_daily = sw_daily.copy()

            # sw_daily的市值单位为万元，index_dailybasic标准字段单位为元
            if 'total_mv' in sw_daily.columns:
                sw_daily['total_mv'] = sw_daily['total_mv'] * 10000

            if 'float_mv' in sw_daily.columns:
                sw_daily['float_mv'] = sw_daily['float_mv'] * 10000

            return sw_daily.reindex(columns=output_fields)

        # index_daily_indicator仅采集非申万指数
        market_ts_codes = [
            str(ts_code).strip()
            for ts_code in ts_code_list
            if str(ts_code).strip()
            and not str(ts_code).strip().upper().endswith(".SI")
        ]

        result_records: list[dict[str, Any]] = []
        trade_date = params.get("trade_date", None)

        # 采集市场指数每日指标
        if trade_date != None:
            for index, ts_code in enumerate(market_ts_codes):
                logger.info(
                    f"开始采集市场指数每日指标数据，ts_code={ts_code}, trade_date={trade_date}"
                )

                index_daily_basic = await self.ts.index_daily_indicator(
                    ts_code=ts_code,
                    trade_date=trade_date,
                )

                if index_daily_basic is not None and not index_daily_basic.empty:
                    index_daily_basic = normalize_index_daily_basic(index_daily_basic)
                    index_daily_basic = index_daily_basic.replace([np.inf, -np.inf], np.nan)
                    index_daily_basic = index_daily_basic.astype(object).where(
                        index_daily_basic.notnull(),
                        None,
                    )

                    result_records.extend(
                        cast(
                            list[dict[str, Any]],
                            index_daily_basic.rename(columns=str).to_dict("records"),
                        )
                    )
                else:
                    logger.info(
                        f"市场指数{ts_code}在交易日{trade_date}没有获取到每日指标数据"
                    )

                if index < len(market_ts_codes) - 1:
                    await asyncio.sleep(0.5)
        else:
            start_date = params.get("start_date", '')
            end_date = params.get("end_date", '')

            for index, ts_code in enumerate(market_ts_codes):
                logger.info(
                    f"开始采集市场指数每日指标数据，ts_code={ts_code}, "
                    f"start_date={start_date}, end_date={end_date}"
                )

                index_daily_basic = await self.ts.index_daily_indicator(
                    ts_code=ts_code,
                    start_date=start_date,
                    end_date=end_date,
                )

                if index_daily_basic is not None and not index_daily_basic.empty:
                    index_daily_basic = normalize_index_daily_basic(index_daily_basic)
                    index_daily_basic = index_daily_basic.replace([np.inf, -np.inf], np.nan)
                    index_daily_basic = index_daily_basic.astype(object).where(
                        index_daily_basic.notnull(),
                        None,
                    )

                    result_records.extend(
                        cast(
                            list[dict[str, Any]],
                            index_daily_basic.rename(columns=str).to_dict("records"),
                        )
                    )
                else:
                    logger.info(
                        f"市场指数{ts_code}没有获取到指定区间的每日指标数据"
                    )

                if index < len(market_ts_codes) - 1:
                    await asyncio.sleep(0.5)

        # 获取申万2021版L1/L2/L3三个等级的行业指数代码
        sw_classify = await self.ts.index_classify(src="SW2021")

        sw_ts_codes: list[str] = []
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

                if ts_code not in sw_ts_codes:
                    sw_ts_codes.append(ts_code)

        if trade_date != None:
            # 每日更新时只调用一次sw_daily，再按申万行业分类代码过滤
            logger.info(f"开始采集申万行业指数每日指标数据，trade_date={trade_date}")

            sw_daily = await self.ts.sw_daily(trade_date=trade_date)

            if (
                sw_daily is not None
                and not sw_daily.empty
                and 'ts_code' in sw_daily.columns
                and len(sw_ts_codes) > 0
            ):
                sw_ts_code_set = {ts_code.upper() for ts_code in sw_ts_codes}

                sw_daily = sw_daily[
                    sw_daily['ts_code']
                    .astype(str)
                    .str.strip()
                    .str.upper()
                    .isin(sw_ts_code_set)
                ]

                if not sw_daily.empty:
                    sw_daily = normalize_sw_daily(sw_daily)
                    sw_daily = sw_daily.replace([np.inf, -np.inf], np.nan)
                    sw_daily = sw_daily.astype(object).where(
                        sw_daily.notnull(),
                        None,
                    )

                    result_records.extend(
                        cast(
                            list[dict[str, Any]],
                            sw_daily.rename(columns=str).to_dict("records"),
                        )
                    )
        else:
            # 日期区间场景按申万行业指数逐个采集，避免全市场区间查询超过单次返回上限
            start_date = params.get("start_date", '')
            end_date = params.get("end_date", '')

            for ts_code in sw_ts_codes:
                logger.info(
                    f"开始采集申万行业指数每日指标数据，ts_code={ts_code}, "
                    f"start_date={start_date}, end_date={end_date}"
                )

                sw_daily = await self.ts.sw_daily(
                    ts_code=ts_code,
                    start_date=start_date,
                    end_date=end_date,
                )

                if sw_daily is not None and not sw_daily.empty:
                    sw_daily = normalize_sw_daily(sw_daily)
                    sw_daily = sw_daily.replace([np.inf, -np.inf], np.nan)
                    sw_daily = sw_daily.astype(object).where(
                        sw_daily.notnull(),
                        None,
                    )

                    result_records.extend(
                        cast(
                            list[dict[str, Any]],
                            sw_daily.rename(columns=str).to_dict("records"),
                        )
                    )

        return result_records


async def index_daily_indicator_data_handler(request: TaskRunRequest) -> TaskRunResult:
    """采集任务的handler，负责调用task的execute方法并返回结果"""
    task = IndexDailyIndicatorDataTask()

    return await task.execute(request)


def register_index_daily_indicator_data_task():
    """注册task和handler到registry中"""
    registry.register_handler('index_daily_indicator_data_handler', index_daily_indicator_data_handler)

    registry.register_task_template(
        TaskTemplateInfo(
            task_code='index_daily_indicator_data',
            task_name='大盘指数每日指标数据采集',
            task_desc="""采集指定指数列表每日指标，包含两种场景：
            1）传入交易日期，获取该交易日指定指数列表的每日指标数据，此时start_date和end_date不需要传入，如果传入了trade_date参数，start_date和end_date将被忽略；
            2）传入指数代码、开始日期和结束日期，获取指定指数列表在该日期范围内的每日指标数据，用于历史数据采集。如果没有传入开始和结束日期，则默认采集指定大盘指数的所有历史每日指标数据（单次最多3000条）
            从参数ts_codes读取需要初始化的指数代码列表，多个指数代码用英文逗号分隔
            注：不是每个指数都有，应该是大盘指数才有每日指标数据，采集前请确认指数代码是否正确且有每日指标数据""",
            handler_name='index_daily_indicator_data_handler',
            data_source='TUSHARE',
            asset_type=AssetType.INDEX,
            biz_type='每日指标',
            params_schema=[
                OutputField(
                    field_code="ts_codes",
                    field_name="指数代码列表",
                    field_type="STRING",
                    required=True,
                    unique_key=False,
                    sort_no=1,
                    field_desc="指数代码列表，需要采集的指数代码列表，需要包括后缀，选填，如果没有传入trade_date参数，则该参数必填，如000001.SH、399001.SZ等"
                ),
                OutputField(
                    field_code="trade_date",
                    field_name="交易日期",
                    field_type="DATE",
                    required=False,
                    unique_key=False,
                    sort_no=2,
                    field_desc="交易日期，格式为YYYYMMDD，选填，如果传入了trade_date参数，则ts_code、start_date和end_date不需要传入"
                ),
                OutputField(
                    field_code="start_date",
                    field_name="开始日期",
                    field_type="DATE",
                    required=False,
                    unique_key=False,
                    sort_no=3,
                    field_desc="开始日期，格式为YYYYMMDD，选填，如果没有传入start_date，则以end_date向前最多采集3000条数据"
                ),
                OutputField(
                    field_code="end_date",
                    field_name="结束日期",
                    field_type="DATE",
                    required=False,
                    unique_key=False,
                    sort_no=4,
                    field_desc="结束日期，格式为YYYYMMDD，选填，如果没有传入end_date，则以start_date向后最多采集3000条数据"
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
                    field_code="total_mv",
                    field_name="当日总市值（元）",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=3,
                    field_desc="当日总市值，单位为元"
                ),
                OutputField(
                    field_code="float_mv",
                    field_name="当日流通市值（元）",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=4,
                    field_desc="当日流通市值，单位为元"
                ),
                OutputField(
                    field_code="total_share",
                    field_name="当日总股本（股）",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=5,
                    field_desc="当日总股本，单位为股"
                ),
                OutputField(
                    field_code="float_share",
                    field_name="当日流通股本（股）",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=6,
                    field_desc="当日流通股本，单位为股"
                ),
                OutputField(
                    field_code="free_share",
                    field_name="当日自由流通股本（股）",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=7,
                    field_desc="当日自由流通股本，单位为股"
                ),
                OutputField(
                    field_code="turnover_rate",
                    field_name="换手率",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=8,
                    field_desc="换手率"
                ),
                OutputField(
                    field_code="turnover_rate_f",
                    field_name="换手率（自由流通股本）",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=9,
                    field_desc="换手率（基于自由流通股本）"
                ),
                OutputField(
                    field_code="pe",
                    field_name="市盈率",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=10,
                    field_desc="市盈率"
                ),
                OutputField(
                    field_code="pe_ttm",
                    field_name="市盈率TTM",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=11,
                    field_desc="市盈率TTM"
                ),
                OutputField(
                    field_code="pb",
                    field_name="市净率",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=12,
                    field_desc="市净率"
                )
            ]
        )
    )