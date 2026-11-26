import asyncio
import logging
from typing import Any, AsyncIterator, cast

from app.core.registry import registry
from app.schemas.collection_task.task_template import ExecutionContext, OutputField, TaskRunRequest, TaskRunResult, TaskTemplateInfo
from app.tasks.pipeline import BaseCollectTask
from app.core.providers import ts_provider
from app.core.common.enum import AssetType

logger = logging.getLogger(__name__)


class StockDailyPriceInitTask(BaseCollectTask):
    """
    初始化采集股票日线价格数据，先获取股票列表，再逐个股票采集全部历史日线价格数据。
    """

    def __init__(self):

        super().__init__()

        self.task_code = 'stock_daily_price_init'

        self.ts = ts_provider

    async def collect_batches(self, params: dict[str, Any], ctx: ExecutionContext) -> AsyncIterator[list[dict[str, Any]]]:
        """
            批量初始化采集股票日线价格数据：
            1）从股票基本数据接口读取股票列表；
            2）对每个股票代码ts_code，采集该股票全部历史日线价格数据；
            3）将交易额从千元转换为元、交易量从手转换为股；
            4）每只股票的数据作为一批yield出去，由基类自动完成后续标准化、清洗、字段映射和入库；
            5）相邻两次读取日线价格之间暂停1秒。
        """

        stock_basic = await self.ts.stock_basic()
        ts_codes = stock_basic["ts_code"].dropna().unique().tolist()

        for index, ts_code in enumerate(ts_codes):
            logger.info(f"开始采集股票{ts_code}的历史日线价格数据")
            stock_daily = await self.ts.stock_daily(ts_code=str(ts_code))

            # 交易额单位为千元，转换为元
            stock_daily['amount'] = stock_daily['amount'] * 1000
            # 将交易量单位为手，转换为股
            stock_daily['vol'] = stock_daily['vol'] * 100

            records = cast(list[dict[str, Any]], stock_daily.rename(columns=str).to_dict("records"))
            if len(records) > 0:
                yield records
            else:
                logger.info(f"股票{ts_code}没有获取到历史日线价格数据")

            if index < len(ts_codes) - 1:
                await asyncio.sleep(1)


async def stock_daily_price_init_handler(request: TaskRunRequest) -> TaskRunResult:
    """采集任务的handler，负责调用task的execute方法并返回结果"""
    task = StockDailyPriceInitTask()

    return await task.execute(request)


def register_stock_daily_price_init_task():
    """注册task和handler到registry中"""
    registry.register_handler('stock_daily_price_init_handler', stock_daily_price_init_handler)

    registry.register_task_template(
        TaskTemplateInfo(
            task_code='stock_daily_price_init',
            task_name='股票日线价格数据初始化采集',
            task_desc='初始化采集所有股票的历史日线价格数据。任务先从股票基本数据接口获取股票列表，再逐个股票采集全部历史日线价格数据，每只股票的数据作为一批进行后续处理和入库，相邻两次接口调用之间暂停1秒。',
            handler_name='stock_daily_price_init_handler',
            data_source='TUSHARE',
            asset_type=AssetType.STOCK,
            biz_type='日线价格',
            params_schema=[],
            output_fields=[
                OutputField(
                    field_code="ts_code",
                    field_name="股票代码全称",
                    field_type="STRING",
                    required=True,
                    unique_key=True,
                    sort_no=1,
                    field_desc="股票代码全称"
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
                    field_code="open",
                    field_name="开盘价",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=3,
                    field_desc="开盘价"
                ),
                OutputField(
                    field_code="high",
                    field_name="最高价",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=4,
                    field_desc="最高价"
                ),
                OutputField(
                    field_code="low",
                    field_name="最低价",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=5,
                    field_desc="最低价"
                ),
                OutputField(
                    field_code="close",
                    field_name="收盘价",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=6,
                    field_desc="收盘价"
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