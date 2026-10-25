import logging
from typing import Any, cast

import numpy as np
import pandas as pd

from app.core.registry import registry
from app.schemas.collection_task.task_template import ExecutionContext, OutputField, TaskRunRequest, TaskRunResult, TaskTemplateInfo
from app.tasks.pipeline import BaseCollectTask
from app.core.providers import ts_provider

logger = logging.getLogger(__name__)


class IndexIndustryLevelTask(BaseCollectTask):
    """
    采集申万行业分类数据，包括指数代码、行业名称、行业级别、行业代码、是否发布指数、父级代码、分类来源等信息。
    当前支持的市场参数为SW=申万行业分类，采集时固定使用Tushare申万2021版分类源SW2021，并将返回结果中的src字段统一处理为SW。
    """

    def __init__(self):

        super().__init__()

        self.task_code = 'index_industry_level'

        self.ts = ts_provider

    async def collect(self, params: dict[str, Any], ctx: ExecutionContext) -> list[dict[str, Any]]:
        """
        采集申万行业分类数据。

        参数说明：
            market: 市场，目前仅支持SW=申万行业分类。
        """

        market = params.get("market", None)
        if market == None or market == "":
            raise ValueError("market参数不能为空，当前仅支持SW=申万行业分类")

        market = str(market).strip().upper()
        if market not in ["SW"]:
            raise ValueError("market参数当前仅支持SW=申万行业分类")

        logger.info(f"开始采集申万行业分类数据，市场={market}，src=SW2021")

        index_industry_level = await self.ts.index_classify(src="SW2021")

        if "src" in index_industry_level.columns:
            index_industry_level["src"] = "SW"

        index_industry_level = index_industry_level.replace([np.inf, -np.inf], np.nan)
        index_industry_level = index_industry_level.astype(object).where(index_industry_level.notnull(), None)

        return cast(list[dict[str, Any]], index_industry_level.rename(columns=str).to_dict("records"))


async def index_industry_level_handler(request: TaskRunRequest) -> TaskRunResult:
    """采集任务的handler，负责调用task的execute方法并返回结果"""
    task = IndexIndustryLevelTask()

    return await task.execute(request)


def register_index_industry_level_task():
    """注册task和handler到registry中"""
    registry.register_handler('index_industry_level_handler', index_industry_level_handler)

    registry.register_task_template(
        TaskTemplateInfo(
            task_code='index_industry_level',
            task_name='申万行业分类数据采集',
            task_desc='采集申万行业分类数据，包括指数代码、行业名称、行业级别、行业代码、是否发布指数、父级代码、分类来源等信息。当前支持的参数market为SW=申万行业分类；采集时固定调用Tushare申万行业分类接口index_classify，src参数固定为SW2021，并将接口返回结果中的src字段统一处理为SW。',
            handler_name='index_industry_level_handler',
            data_source='TUSHARE',
            asset_type='指数',
            biz_type='行业分类',
            params_schema=[
                OutputField(
                    field_code="market",
                    field_name="市场",
                    field_type="STRING",
                    required=True,
                    unique_key=False,
                    sort_no=1,
                    field_desc="市场，当前可选值为SW=申万行业分类"
                )
            ],
            output_fields=[
                OutputField(
                    field_code="index_code",
                    field_name="指数代码",
                    field_type="STRING",
                    required=True,
                    unique_key=True,
                    sort_no=1,
                    field_desc="指数代码"
                ),
                OutputField(
                    field_code="industry_name",
                    field_name="行业名称",
                    field_type="STRING",
                    required=False,
                    unique_key=False,
                    sort_no=2,
                    field_desc="行业名称"
                ),
                OutputField(
                    field_code="level",
                    field_name="行业级别",
                    field_type="STRING",
                    required=False,
                    unique_key=False,
                    sort_no=3,
                    field_desc="行业级别，L1=一级行业，L2=二级行业，L3=三级行业"
                ),
                OutputField(
                    field_code="industry_code",
                    field_name="行业代码",
                    field_type="STRING",
                    required=False,
                    unique_key=False,
                    sort_no=4,
                    field_desc="行业代码"
                ),
                OutputField(
                    field_code="is_pub",
                    field_name="是否发布指数",
                    field_type="INTEGER",
                    required=False,
                    unique_key=False,
                    sort_no=5,
                    field_desc="是否发布指数，1=是，0=否"
                ),
                OutputField(
                    field_code="parent_code",
                    field_name="父级代码",
                    field_type="STRING",
                    required=False,
                    unique_key=False,
                    sort_no=6,
                    field_desc="父级行业代码，一级行业的父级代码通常为0"
                ),
                OutputField(
                    field_code="src",
                    field_name="行业分类来源",
                    field_type="STRING",
                    required=False,
                    unique_key=False,
                    sort_no=7,
                    field_desc="行业分类来源，采集时Tushare接口传入SW2021，返回结果中统一处理为SW"
                )
            ]
        )
    )
