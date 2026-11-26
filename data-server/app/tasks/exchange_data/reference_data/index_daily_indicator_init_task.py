import asyncio
import logging
from typing import Any, AsyncIterator, cast

import numpy as np

from app.core.registry import registry
from app.schemas.collection_task.task_template import ExecutionContext, OutputField, TaskRunRequest, TaskRunResult, TaskTemplateInfo
from app.tasks.pipeline import BaseCollectTask
from app.core.providers import ts_provider
from app.core.common.enum import AssetType

logger = logging.getLogger(__name__)

class IndexDailyIndicatorInitDataTask(BaseCollectTask):
    """
    初始化采集大盘指数每日指标数据，根据传入的指数代码列表，逐个指数采集全部历史每日指标数据。
    """

    def __init__(self):

        super().__init__()

        self.task_code = 'index_daily_indicator_init'

        self.ts = ts_provider


    def parse_ts_codes(self, params: dict[str, Any]) -> list[str]:
        """从参数中读取指数代码列表，支持列表或半角逗号分隔字符串"""
        ts_codes = params.get("ts_codes", None)
        if ts_codes == None:
            raise ValueError("ts_codes参数不能为空")

        if isinstance(ts_codes, str):
            parsed_ts_codes = [item.strip() for item in ts_codes.split(",") if item.strip()]
        elif isinstance(ts_codes, list):
            parsed_ts_codes = [str(item).strip() for item in ts_codes if str(item).strip()]
        else:
            raise ValueError("ts_codes参数必须为指数代码列表或半角逗号分隔字符串")

        if len(parsed_ts_codes) == 0:
            raise ValueError("ts_codes参数不能为空")

        return parsed_ts_codes


    async def collect_batches(self, params: dict[str, Any], ctx: ExecutionContext) -> AsyncIterator[list[dict[str, Any]]]:
        """
            批量初始化采集指数每日指标数据：
            1）从参数ts_codes读取指数代码列表，并排除以.SI结尾的申万指数；
            2）对剩余市场指数逐个调用index_daily_indicator获取全部历史每日指标；
            3）调用index_classify(src="SW2021")获取L1/L2/L3三个等级的申万行业指数代码；
            4）对每个申万行业指数代码调用sw_daily，仅传入ts_code获取历史数据；
            5）将sw_daily能够语义对应的字段映射到index_dailybasic标准字段，其余字段为空；
            6）每个指数的数据作为一批yield出去，由基类完成后续标准化、清洗、字段映射和入库。
        """

        ts_codes = self.parse_ts_codes(params)

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

        # 原始ts_codes中的.SI申万指数不直接使用，市场指数仍按传入列表采集
        market_ts_codes = [
            str(ts_code).strip()
            for ts_code in ts_codes
            if str(ts_code).strip()
            and not str(ts_code).strip().upper().endswith(".SI")
        ]

        # 先初始化市场指数每日指标
        for index, ts_code in enumerate(market_ts_codes):
            logger.info(f"开始采集市场指数{ts_code}的历史每日指标数据")

            index_daily_basic = await self.ts.index_daily_indicator(ts_code=ts_code)

            if index_daily_basic is not None and not index_daily_basic.empty:
                index_daily_basic = normalize_index_daily_basic(index_daily_basic)
                index_daily_basic = index_daily_basic.replace([np.inf, -np.inf], np.nan)
                index_daily_basic = index_daily_basic.astype(object).where(index_daily_basic.notnull(), None)

                records = cast(
                    list[dict[str, Any]],
                    index_daily_basic.rename(columns=str).to_dict("records"),
                )
                if len(records) > 0:
                    yield records
            else:
                logger.info(f"市场指数{ts_code}没有获取到历史每日指标数据")

            if index < len(market_ts_codes) - 1:
                await asyncio.sleep(1)

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

        # 再初始化申万行业指数每日指标
        for index, ts_code in enumerate(sw_ts_codes):
            logger.info(f"开始采集申万行业指数{ts_code}的历史每日指标数据")

            # 初始化任务按行业指数逐个采集，sw_daily只传入指数代码
            sw_daily = await self.ts.sw_daily(ts_code=ts_code)

            if sw_daily is not None and not sw_daily.empty:
                sw_daily = normalize_sw_daily(sw_daily)
                sw_daily = sw_daily.replace([np.inf, -np.inf], np.nan)
                sw_daily = sw_daily.astype(object).where(sw_daily.notnull(), None)

                records = cast(
                    list[dict[str, Any]],
                    sw_daily.rename(columns=str).to_dict("records"),
                )
                if len(records) > 0:
                    yield records
            else:
                logger.info(f"申万行业指数{ts_code}没有获取到历史每日指标数据")

            if index < len(sw_ts_codes) - 1:
                await asyncio.sleep(1)


async def index_daily_indicator_init_handler(request: TaskRunRequest) -> TaskRunResult:
    """采集任务的handler，负责调用task的execute方法并返回结果"""
    task = IndexDailyIndicatorInitDataTask()

    return await task.execute(request)


def register_index_daily_indicator_init_task():
    """注册task和handler到registry中"""
    registry.register_handler('index_daily_indicator_init_handler', index_daily_indicator_init_handler)

    registry.register_task_template(
        TaskTemplateInfo(
            task_code='index_daily_indicator_init',
            task_name='大盘指数每日指标初始化采集',
            task_desc='初始化采集指定指数列表的历史每日指标数据。任务从参数ts_codes读取指数代码列表，支持列表或逗号分隔字符串，再逐个指数采集全部历史每日指标数据，每个指数的数据作为一批进行后续处理和入库，相邻两次接口调用之间暂停5秒。注：不是每个指数都有，应该是大盘指数才有每日指标数据，采集前请确认指数代码是否正确且有每日指标数据',
            handler_name='index_daily_indicator_init_handler',
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
                    field_desc="需要初始化的指数代码列表，需要包括后缀，支持列表或半角逗号分隔字符串，如000001.SH,399001.SZ等"
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