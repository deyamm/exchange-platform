import asyncio
import logging
import datetime
from typing import Any, AsyncIterator, cast

import numpy as np
from sqlalchemy import text

from app.core.registry import registry
from app.schemas.collection_task.task_template import ExecutionContext, OutputField, TaskRunRequest, TaskRunResult, TaskTemplateInfo
from app.tasks.pipeline import BaseCollectTask, TaskRuntime
from app.core.providers import ts_provider
from app.core.db import get_engine_by_schema
from app.core.config import settings

from app.utils.datetime_utils import get_first_day_of_month, get_last_day_of_month
from app.utils.db_utils import quote_identifier

logger = logging.getLogger(__name__)

class IndexConstituentDataTask(BaseCollectTask):
    """
    采集指数成分股数据，包括指数代码、成分股代码、权重等信息，该接口只能返回MSCI、中证、上交所、深交所、中金、申万及其他指数的成分股，同花顺、东财等版块、概念等指数需要其他接口。
    """

    def __init__(self):
        
        super().__init__()
        
        self.task_code = 'index_constituent_data'

        self.ts = ts_provider

    async def before_collect(self, ctx: ExecutionContext, runtime: TaskRuntime) -> None:
        """
        在执行任务前，先清空库中原有数据，仅保留最新的成分股数据
        """
        mapping = ctx.storage_mapping

        # 验证schema是否在允许范围内，防止写入不受控的schema
        if mapping.physical_schema_name not in settings.allowed_schema_set:
            raise ValueError(f"不允许写入的 schema: {mapping.physical_schema_name}")
        
        engine = get_engine_by_schema(mapping.physical_schema_name)

        # 验证表名和列名是否合法，防止SQL注入
        schema = quote_identifier(mapping.physical_schema_name)
        table = quote_identifier(mapping.physical_table_name)

        logger.info("开始清空原有指数成分股数据")

        async with engine.begin() as conn:
            await conn.execute(text('delete from {table}'.format(table=table)))

        logger.info("完成清空原有指数成分股数据")

    async def collect_batches(self, params: dict[str, Any], ctx: ExecutionContext) -> AsyncIterator[list[dict[str, Any]]]:
        # 读取参数，包括指数代码、开始日期、结束日期等参数，其中index_code必填，start_date和end_date可选，默认值为当月第一天和当月最后一天
        ts_codes = params.get("ts_codes", None)
        if ts_codes == None or ts_codes == "":
            raise ValueError("ts_codes参数不能为空")

        if isinstance(ts_codes, str):
            ts_code_list = [item.strip() for item in ts_codes.split(",") if item.strip()]
        else:
            ts_code_list = list(ts_codes)

        if len(ts_code_list) == 0:
            raise ValueError("ts_codes参数不能为空")
        
        start_date = params.get("start_date", get_first_day_of_month(datetime.date.today()))
        end_date = params.get("end_date", get_last_day_of_month(datetime.date.today()))

        index_industry_level = await self.ts.index_classify(src="SW2021")

        # 在传入的指数代码列表中，添加申万三级行业指数代码，确保采集申万三级行业指数成分股数据
        ts_code_list.extend(index_industry_level[index_industry_level['level'] == 'L3']["index_code"].dropna().unique().tolist())

        # 采集普通指数的成分股，返回指数代码、成分股代码、交易日期、权重
        for ts_code in ts_code_list:
            if ts_code.endswith(".SI"):
                logger.info(f"采集申万三级行业指数成分股数据，ts_code={ts_code}")
                
                index_constituent = await self.ts.index_member_all(l3_code=ts_code, is_new='Y')

                # 只保留l3_code, ts_code, in_date 3列并重命名为 index_code, constituent_code, trade_date，添加一列 null 的权重列，与返回的普通指数成分股数据保持一致，方便后续入库
                index_constituent = index_constituent[["l3_code", "ts_code", "in_date"]].rename(columns={
                    "l3_code": "index_code",
                    "ts_code": "constituent_code",
                    "in_date": "trade_date"
                })
                index_constituent["weight"] = None

            else:
                logger.info(f"开始采集指数成分股数据，ts_code={ts_code}")

                index_constituent = await self.ts.index_weight(index_code=ts_code, start_date=start_date, end_date=end_date)

                index_constituent = index_constituent.rename(columns={
                    "con_code": "constituent_code"
                })

            index_constituent = index_constituent.replace([np.inf, -np.inf], np.nan)
            index_constituent = index_constituent.astype(object).where(index_constituent.notnull(), None)

            yield cast(list[dict[str, Any]], index_constituent.rename(columns=str).to_dict("records"))

            await asyncio.sleep(1)
        


async def index_constituent_data_handler(request: TaskRunRequest) -> TaskRunResult:
    """采集任务的handler，负责调用task的execute方法并返回结果"""
    task = IndexConstituentDataTask()
    
    return await task.execute(request)


def register_index_constituent_data_task():
    """注册task和handler到registry中"""
    registry.register_handler('index_constituent_data_handler', index_constituent_data_handler)

    registry.register_task_template(
        TaskTemplateInfo(
            task_code='index_constituent_data',
            task_name='指数成分股数据采集',
            task_desc="""采集特定指数列表的在某月的成分股信息，成分股为月度更新数据，包含指数代码、成分股代码、权重等信息。
            此外：该接口会直接采集申万三级行业指数的成分股数据，采集过程中会删除已被剔除的股票代码，仅保留最新的成分股数据。
            注：来源接口只能返回单个指数的成分股，该任务会自动循环调用接口获取多个指数的成分股数据，每次采集时会清空原有库中数据，仅保存最新的成分股数据""",
            handler_name='index_constituent_data_handler',
            data_source='TUSHARE',
            asset_type='指数',
            biz_type='基本数据',
            params_schema=[
                OutputField(
                    field_code="ts_codes",
                    field_name="指数代码",
                    field_type="STRING",
                    required=True,
                    unique_key=True,
                    sort_no=1,
                    field_desc="指数代码列表，以英文逗号分隔，需要包括后缀，必填，来源于指数基本数据接口返回的index_code字段，如000300.SH、399001.SZ等"
                ),
                OutputField(
                    field_code="start_date",
                    field_name="开始日期",
                    field_type="DATE",
                    required=False,
                    unique_key=False,
                    sort_no=2,
                    field_desc="开始日期，一般为当月第一天，格式为YYYYMMDD，选填，默认为当月第一天"
                ),
                OutputField(
                    field_code="end_date",
                    field_name="结束日期",
                    field_type="DATE",
                    required=False,
                    unique_key=False,
                    sort_no=3,
                    field_desc="结束日期，一般为当月最后一天，格式为YYYYMMDD，选填，默认为当月最后一天"
                )
            ],
            output_fields = [
                OutputField(
                    field_code="index_code",
                    field_name="指数代码",
                    field_type="STRING",
                    required=True,
                    unique_key=True,
                    sort_no=1,
                    field_desc="指数代码全称，包含交易所后缀"
                ),
                OutputField(
                    field_code="constituent_code",
                    field_name="成分股代码",
                    field_type="STRING",
                    required=True,
                    unique_key=True,
                    sort_no=2,
                    field_desc="成分股代码全称，包含交易所后缀"
                ),
                OutputField(
                    field_code="trade_date",
                    field_name="交易日期",
                    field_type="DATE",
                    required=False,
                    unique_key=True,
                    sort_no=3,
                    field_desc="成分股对应的交易日期，格式为YYYYMMDD"
                ),
                OutputField(
                    field_code="weight",
                    field_name="权重",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=4,
                    field_desc="成分股的权重，单位为百分比，保留4位小数"
                )
            ]

                
        )
    )
