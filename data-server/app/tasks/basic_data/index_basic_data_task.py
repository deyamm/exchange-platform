import asyncio
import logging
from typing import Any, cast

import numpy as np
import pandas as pd

from app.core.registry import registry
from app.schemas.collection_task.task_template import ExecutionContext, OutputField, TaskRunRequest, TaskRunResult, TaskTemplateInfo
from app.tasks.pipeline import BaseCollectTask
from app.core.providers import ts_provider
from app.core.common.enum import AssetType, IndexCategory



logger = logging.getLogger(__name__)

class IndexBasicDataTask(BaseCollectTask):
    """
    采集指数基本数据，包括指数代码、简称、全称、市场、发布方等信息，该接口只能返回MSCI、中证、上交所、深交所、中金、申万及其他指数，同花顺、东财等版块、概念等指数需要其他接口。
    由于接口一次最多返回8000条数据，因此需要按市场来分批采集，市场包括MSCI=MSCI指数，CSI=中证指数，SSE=上交所指数，SZSE=深交所指数，CICC=中金指数，SW=申万指数，OTH=其他指数。
    """

    def __init__(self):
        
        super().__init__()
        
        self.task_code = 'index_basic_data'

        self.ts = ts_provider

    @staticmethod
    def _normalize_category(category: Any) -> str | None:
        """
        将 Tushare index_basic 返回的 category 转换为 IndexCategory 的英文大写枚举名。

        例如：
        - 综合指数 -> COMPOSITE
        - 规模指数 -> SIZE
        - 主题指数 -> THEMATIC
        - 一级行业指数 / 二级行业指数 / 三级行业指数 -> INDUSTRY
        - 枚举未覆盖的其他类别 -> OTHER
        """
        if category is None or pd.isna(category):
            return None

        category_str = str(category).strip()
        if not category_str:
            return None

        # 按枚举 label 建立映射，同时兼容可能不带“指数”后缀的类别值。
        category_mapping: dict[str, str] = {}
        for item in IndexCategory:
            category_mapping[item.label] = item.name
            if item.label.endswith("指数"):
                category_mapping[item.label.removesuffix("指数")] = item.name

        # Tushare 申万指数会返回“一级行业指数”“二级行业指数”等层级类别。
        if "行业" in category_str:
            return IndexCategory.INDUSTRY.name

        return category_mapping.get(category_str, IndexCategory.OTHER.name)

    async def collect(self, params: dict[str, Any], ctx: ExecutionContext) -> list[dict[str, Any]]:

        all_index_basic = []
        index_codes = {
            '000985',  # 中证全指
            '930050',  # 中证A50
            '000903',  # 中证A100
            '000510',  # 中证A500
            '000300',  # 沪深300
            '000906',  # 中证800
            '000905',  # 中证500
            '000852',  # 中证1000
            '932000',  # 中证2000
            '932672',  # 中证全指成长
            '932671',  # 中证全指价值
            '000918',  # 300成长
            '000919',  # 300价值
            'H30351',  # 500成长
            'H30352',  # 500价值
            'H30353',  # 500R成长
            'H30354',  # 500R价值
            'H30355',  # 800成长
            'H30356',  # 800价值
            '000922',  # 中证红利
            'H30269',  # 红利低波
            '931468',  # 红利质量
            '932309',  # 红利增长
            '931130',  # 红利成长低波
            '932154',  # A500红利
            '932422',  # A500红利低波
            '932430',  # 中证质量
            '932431',  # 300质量
            '930855',  # 中证质量成长
            '931156',  # 800质量成长
            '932365',  # 中证现金流
            '932368',  # 800现金流
            '931082',  # A500现金流
            '932535',  # 全指等权
            '000984',  # 300等权
            '000982',  # 500等权
            '000842',  # 800等权
            'H30260',  # 300动量
            '000828',  # 300高贝
            '000829',  # 300低贝
            '000830',  # 500高贝
            '000831',  # 500低贝
            '000925',  # 基本面50
            '000965',  # 基本面200
            '000966',  # 基本面400
            '000967',  # 基本面600
            '931586',  # 300价值稳健
            '931587',  # 500价值稳健
            '931588',  # 1000价值稳健
            '000001',  # 上证指数
            '000016',  # 上证50
            '399001',  # 深证成指
            '399005',  # 中小100
            '399006',  # 创业板指
            '399016',  # 深证创新
            '399673',  # 创业板50
            '000688',  # 科创50
            '000680',  # 科创综指
            '899050'  # 北证50
        }
        collected_codes = set()

        # 采集各市场的指数基本数据，按市场分批采集
        for market in ['MSCI', 'CSI', 'SSE', 'SZSE', 'CICC', 'OTH']:
            logger.info(f"开始采集指数基本数据，市场={market}")
            index_basic = await self.ts.index_basic(market=market)

            if not index_basic.empty:
                # JSON白名单只用于按基础代码筛选，不改变Tushare返回字段及字段值。
                base_codes = index_basic["ts_code"].astype(str).str.rsplit(".", n=1).str[0]
                keep_mask = (
                    base_codes.isin(index_codes)
                    & ~base_codes.isin(collected_codes)
                    & ~base_codes.duplicated()
                )

                index_basic = index_basic.loc[keep_mask]
                collected_codes.update(base_codes.loc[keep_mask].tolist())

            all_index_basic.append(index_basic)
            await asyncio.sleep(5)  # 5s采集一次，避免接口限频

        # 采集申万L1行业指数信息
        logger.info("开始采集申万L1行业指数基本数据")

        # index_classify 接口只获取L1行业指数分类信息，L2/L3行业指数不在采集范围内。
        sw_index_classify = await self.ts.index_classify(level="L1", src="SW2021")
        sw_index_codes = set(sw_index_classify["index_code"].dropna().astype(str).str.strip())
        await asyncio.sleep(1)

        sw_index_basic = await self.ts.index_basic(market="SW")
        if not sw_index_basic.empty:
            # 仅保留 index_classify 返回的申万L1行业指数代码。
            sw_base_codes = sw_index_basic["ts_code"].astype(str)
            keep_mask = (
                sw_base_codes.isin(sw_index_codes)
                & ~sw_base_codes.isin(collected_codes)
                & ~sw_base_codes.duplicated()
            )

            sw_index_basic = sw_index_basic.loc[keep_mask]
            # 接口中返回的申万行业简称前都带有 “申万”前缀，去掉前缀后再更新已采集代码集合。
            sw_index_basic["name"] = sw_index_basic["name"].str.removeprefix("申万")
            collected_codes.update(sw_base_codes.loc[keep_mask].tolist())

        all_index_basic.append(sw_index_basic)

        all_index_basic = pd.concat(all_index_basic, ignore_index=True)

        if "category" in all_index_basic.columns:
            all_index_basic["category"] = all_index_basic["category"].apply(self._normalize_category)

        all_index_basic = all_index_basic.replace([np.inf, -np.inf], np.nan)
        all_index_basic = all_index_basic.astype(object).where(all_index_basic.notnull(), None)

        return cast(list[dict[str, Any]], all_index_basic.rename(columns=str).to_dict("records"))


async def index_basic_data_handler(request: TaskRunRequest) -> TaskRunResult:
    """采集任务的handler，负责调用task的execute方法并返回结果"""
    task = IndexBasicDataTask()
    
    return await task.execute(request)


def register_index_basic_data_task():
    """注册task和handler到registry中"""
    registry.register_handler('index_basic_data_handler', index_basic_data_handler)

    registry.register_task_template(
        TaskTemplateInfo(
            task_code='index_basic_data',
            task_name='指数基本数据采集',
            task_desc='采集指数基本数据，包括指数代码、简称、全称、市场、发布方等信息，该接口只能返回MSCI、中证、上交所、深交所、中金、申万及其他指数，同花顺、东财等版块、概念等指数需要其他接口。由于接口一次最多返回8000条数据，因此需要按市场来分批采集，市场包括MSCI=MSCI指数，CSI=中证指数，SSE=上交所指数，SZSE=深交所指数，CICC=中金指数，SW=申万指数，OTH=其他指数',
            handler_name='index_basic_data_handler',
            data_source='TUSHARE',
            asset_type=AssetType.INDEX,
            biz_type='基本数据',
            params_schema=[],
            output_fields = [
                OutputField(
                    field_code="ts_code",
                    field_name="代码全称",
                    field_type="STRING",
                    required=True,
                    unique_key=True,
                    sort_no=1,
                    field_desc="指数代码全称，包含交易所后缀"
                ),
                OutputField(
                    field_code="name",
                    field_name="简称",
                    field_type="STRING",
                    required=False,
                    unique_key=False,
                    sort_no=2,
                    field_desc="简称"
                ),
                OutputField(
                    field_code="fullname",
                    field_name="指数全称",
                    field_type="STRING",
                    required=False,
                    unique_key=False,
                    sort_no=3,
                    field_desc="指数全称"
                ),
                OutputField(
                    field_code="market",
                    field_name="市场",
                    field_type="STRING",
                    required=False,
                    unique_key=False,
                    sort_no=4,
                    field_desc="市场，MSCI=MSCI指数，CSI=中证指数，SSE=上交所指数，SZSE=深交所指数，CICC=中金指数，SW=申万指数，OTH=其他指数"
                ),
                OutputField(
                    field_code="publisher",
                    field_name="发布方",
                    field_type="STRING",
                    required=False,
                    unique_key=False,
                    sort_no=5,
                    field_desc="发布方"
                ),
                OutputField(
                    field_code="index_type",
                    field_name="指数风格",
                    field_type="STRING",
                    required=False,
                    unique_key=False,
                    sort_no=6,
                    field_desc="指数风格"
                ),
                OutputField(
                    field_code="category",
                    field_name="指数类别",
                    field_type="STRING",
                    required=False,
                    unique_key=False,
                    sort_no=7,
                    field_desc="指数类别，已转换为IndexCategory英文大写枚举名：COMPOSITE、SIZE、STYLE、THEMATIC、INDUSTRY、STRATEGY、GROWTH、VALUE、CONCEPT、OTHER"
                ),
                OutputField(
                    field_code="base_date",
                    field_name="基期",
                    field_type="DATE",
                    required=False,
                    unique_key=False,
                    sort_no=8,
                    field_desc="基期"
                ),
                OutputField(
                    field_code="base_point",
                    field_name="基点",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=9,
                    field_desc="基点"
                ),
                OutputField(
                    field_code="list_date",
                    field_name="发布日期",
                    field_type="DATE",
                    required=False,
                    unique_key=False,
                    sort_no=10,
                    field_desc="发布日期"
                ),
                OutputField(
                    field_code="weight_rule",
                    field_name="加权方式",
                    field_type="STRING",
                    required=False,
                    unique_key=False,
                    sort_no=11,
                    field_desc="加权方式"
                ),
                OutputField(
                    field_code="desc",
                    field_name="描述",
                    field_type="STRING",
                    required=False,
                    unique_key=False,
                    sort_no=12,
                    field_desc="描述"
                ),
                OutputField(
                    field_code="exp_date",
                    field_name="终止日期",
                    field_type="DATE",
                    required=False,
                    unique_key=False,
                    sort_no=13,
                    field_desc="终止日期"
                ),
            ]

                
        )
    )