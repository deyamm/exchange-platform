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

class IndexDailyPriceInitTask(BaseCollectTask):
    """
    批量初始化指数日线行情数据，包括指数代码、交易日期、开盘点位、最高点位、最低点位、收盘点位、成交量、成交金额等信息。
    该任务从传入参数中读取需要初始化的指数代码列表，然后逐个指数读取其历史日线行情数据。
    """

    def __init__(self):

        super().__init__()

        self.task_code = 'index_daily_price_init'

        self.ts = ts_provider


    async def collect_batches(self, params: dict[str, Any], ctx: ExecutionContext) -> AsyncIterator[list[dict[str, Any]]]:
        """
            批量初始化指数日线行情数据：
            1）从参数ts_codes读取需要初始化的指数代码列表，并排除以.SI结尾的申万行业指数；
            2）对剩余市场指数逐个调用index_daily读取历史日线行情数据；
            3）调用index_classify(src="SW2021")获取L1/L2/L3三个等级的申万行业指数代码，
               再逐个调用sw_daily读取历史日线行情数据；
            4）将index_daily和sw_daily统一整理为TaskTemplateInfo中output_fields定义的字段；
            5）每个指数的日线行情数据作为一个批次yield出去，后续由基类自动执行标准化、清洗、字段映射和入库。
        """

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

        # 原始ts_codes只保留市场指数，传入的申万行业指数统一排除
        market_ts_code_list = [
            ts_code
            for ts_code in ts_code_list
            if not str(ts_code).strip().upper().endswith(".SI")
        ]

        # 先采集市场指数行情
        for index, ts_code in enumerate(market_ts_code_list):
            logger.info(f"开始采集市场指数日线行情初始化数据，ts_code={ts_code}")

            index_daily = await self.ts.index_daily(ts_code=ts_code)

            # 交易额单位为千元，转换为元
            if 'amount' in index_daily.columns:
                index_daily['amount'] = index_daily['amount'] * 1000

            # 交易量单位为手，转换为股
            if 'vol' in index_daily.columns:
                index_daily['vol'] = index_daily['vol'] * 100

            # 统一为TaskTemplateInfo中output_fields定义的字段
            index_daily = index_daily.reindex(columns=output_fields)
            index_daily = index_daily.replace([np.inf, -np.inf], np.nan)
            index_daily = index_daily.astype(object).where(index_daily.notnull(), None)

            yield cast(list[dict[str, Any]], index_daily.rename(columns=str).to_dict("records"))

            if index < len(market_ts_code_list) - 1:
                await asyncio.sleep(2)

        # 获取申万2021版L1/L2/L3三个等级的行业指数代码
        sw_classify = await self.ts.index_classify(src="SW2021")

        sw_ts_code_list: list[str] = []
        if sw_classify is not None and not sw_classify.empty and 'index_code' in sw_classify.columns:
            if 'level' in sw_classify.columns:
                sw_classify = sw_classify[sw_classify['level'].astype(str).isin(["L1", "L2", "L3"])]

            for index_code in sw_classify['index_code'].dropna().tolist():
                index_code = str(index_code).strip()
                if not index_code:
                    continue

                ts_code = index_code if index_code.upper().endswith(".SI") else f"{index_code}.SI"
                if ts_code not in sw_ts_code_list:
                    sw_ts_code_list.append(ts_code)

        # 再逐个采集申万行业指数行情
        for index, ts_code in enumerate(sw_ts_code_list):
            logger.info(f"开始采集申万行业指数日线行情初始化数据，ts_code={ts_code}")

            # sw_daily只传入指数代码，获取该指数全部历史行情
            sw_daily = await self.ts.sw_daily(ts_code=ts_code)

            # sw_daily的涨跌幅字段名为pct_change，统一为index_daily的pct_chg
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

            # 统一为TaskTemplateInfo中output_fields定义的字段，并去除sw_daily额外字段
            sw_daily = sw_daily.reindex(columns=output_fields)
            sw_daily = sw_daily.replace([np.inf, -np.inf], np.nan)
            sw_daily = sw_daily.astype(object).where(sw_daily.notnull(), None)

            yield cast(list[dict[str, Any]], sw_daily.rename(columns=str).to_dict("records"))

            if index < len(sw_ts_code_list) - 1:
                await asyncio.sleep(2)


async def index_daily_price_init_handler(request: TaskRunRequest) -> TaskRunResult:
    """采集任务的handler，负责调用task的execute方法并返回结果"""
    task = IndexDailyPriceInitTask()

    return await task.execute(request)


def register_index_daily_price_init_task():
    """注册task和handler到registry中"""
    registry.register_handler('index_daily_price_init_handler', index_daily_price_init_handler)

    registry.register_task_template(
        TaskTemplateInfo(
            task_code='index_daily_price_init',
            task_name='指数日线行情初始化数据采集',
            task_desc='批量初始化采集指定指数列表的历史日线行情数据。从参数ts_codes读取需要初始化的指数代码列表，多个指数代码用英文逗号分隔，逐个指数代码读取历史日线行情数据，每个指数作为一个批次进入后续标准化、清洗、字段映射和入库流程，相邻两次接口调用之间暂停5秒。',
            handler_name='index_daily_price_init_handler',
            data_source='TUSHARE',
            asset_type=AssetType.INDEX,
            biz_type='日线价格',
            params_schema=[
                OutputField(
                    field_code="ts_codes",
                    field_name="指数代码列表",
                    field_type="STRING",
                    required=True,
                    unique_key=False,
                    sort_no=1,
                    field_desc="需要初始化的指数代码列表，需要包括后缀，多个指数代码用英文逗号分隔，如000001.SH,399001.SZ等"
                )
            ],
            output_fields = [
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
                    field_name="收盘点位",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=3,
                    field_desc="收盘点位"
                ),
                OutputField(
                    field_code="open",
                    field_name="开盘点位",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=4,
                    field_desc="开盘点位"
                ),
                OutputField(
                    field_code="high",
                    field_name="最高点位",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=5,
                    field_desc="最高点位"
                ),
                OutputField(
                    field_code="low",
                    field_name="最低点位",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=6,
                    field_desc="最低点位"
                ),
                OutputField(
                    field_code="pre_close",
                    field_name="昨收点位",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=7,
                    field_desc="昨日收盘点位"
                ),
                OutputField(
                    field_code="change",
                    field_name="涨跌点",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=8,
                    field_desc="涨跌点"
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