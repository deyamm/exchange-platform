import asyncio
import json
import logging
from typing import Any, AsyncIterator, cast

import numpy as np
import pandas as pd

from app.core.registry import registry
from app.schemas.collection_task.task_template import ExecutionContext, OutputField, TaskRunRequest, TaskRunResult, TaskTemplateInfo
from app.tasks.pipeline import BaseCollectTask
from app.core.providers import ts_provider

logger = logging.getLogger(__name__)

class FinanceIncomeInitTask(BaseCollectTask):
    """
    初始化采集股票利润表数据，先获取股票列表，再逐个股票采集全部历史利润表数据。
    """

    def __init__(self):

        super().__init__()

        self.task_code = 'finance_income_init'

        self.ts = ts_provider

        self.collect_fields = ['ts_code', 'ann_date', 'f_ann_date', 'end_date', 'report_type', 'comp_type', 'basic_eps', 'diluted_eps', 'total_revenue', 'revenue', 'total_cogs', 'oper_cost', 'sell_exp', 'admin_exp', 'fin_exp', 'rd_exp', 'invest_income', 'oper_profit', 'total_profit', 'income_tax', 'n_income', 'n_income_attr_p', 'ebit', 'ebitda', 'update_flag', 'raw_json']
        self.field_aliases = {'operate_profit': 'oper_profit'}


    def _to_records(self, data_frame: Any) -> list[dict[str, Any]]:
        """
            保留利润表核心字段，并将Tushare接口原始返回记录写入raw_json字段。
        """

        if data_frame is None or len(data_frame) == 0:
            return []

        data_frame = data_frame.replace([np.inf, -np.inf], np.nan)
        data_frame = data_frame.astype(object).where(data_frame.notnull(), None)
        
        original_records = data_frame.rename(columns=str).to_dict("records")

        if self.field_aliases:
            data_frame = data_frame.rename(columns=self.field_aliases)

        for column in self.collect_fields:
            if column != "raw_json" and column not in data_frame.columns:
                data_frame[column] = None

        result = data_frame[[column for column in self.collect_fields if column != "raw_json"]].copy()
        result["raw_json"] = [json.dumps(record, ensure_ascii=False, default=str) for record in original_records]
        result = result.astype(object).where(result.notnull(), None)

        return cast(list[dict[str, Any]], result.rename(columns=str).to_dict("records"))


    async def collect_batches(self, params: dict[str, Any], ctx: ExecutionContext) -> AsyncIterator[list[dict[str, Any]]]:
        """
            批量初始化采集股票利润表数据：
            1）从股票基本数据接口读取股票列表；
            2）对每个股票代码ts_code，仅传入ts_code参数，采集该股票全部历史利润表数据；
            3）每只股票的数据作为一批yield出去，由基类自动完成后续标准化、清洗、字段映射和入库；
            4）相邻两次读取利润表之间暂停1秒。
        """

        stock_basic = await self.ts.stock_basic()
        ts_codes = stock_basic["ts_code"].dropna().unique().tolist()

        for index, ts_code in enumerate(ts_codes):
            logger.info(f"开始采集股票{ts_code}（{index + 1}/{len(ts_codes)}）的历史利润表数据")
            finance_data = await self.ts.income(ts_code=str(ts_code))

            # 由于接口最多一次返回100条数据，所以当采集到的数据量为100时，取最后一条的end_date作为下次查询的end_date，继续采集一次，暂时每只股采集两次
            if len(finance_data) == 100:
                last_end_date = finance_data["end_date"].dropna().unique().tolist()[-1]
                finance_data_2 = await self.ts.income(ts_code=str(ts_code), end_date=str(last_end_date))
                if len(finance_data_2) > 0:
                    finance_data = pd.concat([finance_data, finance_data_2], ignore_index=True)

            records = self._to_records(finance_data)
            if len(records) > 0:
                yield records
            else:
                logger.info(f"股票{ts_code}没有获取到历史利润表数据")

            if index < len(ts_codes) - 1:
                await asyncio.sleep(1)


async def finance_income_init_handler(request: TaskRunRequest) -> TaskRunResult:
    """采集任务的handler，负责调用task的execute方法并返回结果"""
    task = FinanceIncomeInitTask()

    return await task.execute(request)


def register_finance_income_init_task():
    """注册task和handler到registry中"""
    registry.register_handler('finance_income_init_handler', finance_income_init_handler)

    registry.register_task_template(
        TaskTemplateInfo(
            task_code='finance_income_init',
            task_name='股票利润表初始化采集',
            task_desc='初始化采集所有股票的历史利润表数据。任务先从股票基本数据接口获取股票列表，再逐个股票仅传入ts_code参数采集全部历史利润表数据，每只股票的数据作为一批进行后续处理和入库，相邻两次接口调用之间暂停1秒。',
            handler_name='finance_income_init_handler',
            data_source='TUSHARE',
            asset_type='股票',
            biz_type='利润表',
            params_schema=[],
            output_fields=[
                OutputField(
                    field_code="ts_code",
                    field_name="股票代码全称",
                    field_type="STRING",
                    required=True,
                    unique_key=True,
                    sort_no=1,
                    field_desc="TS股票代码"
                ),
                OutputField(
                    field_code="ann_date",
                    field_name="公告日期",
                    field_type="DATE",
                    required=True,
                    unique_key=True,
                    sort_no=2,
                    field_desc="公告日期，格式为YYYYMMDD"
                ),
                OutputField(
                    field_code="f_ann_date",
                    field_name="实际公告日期",
                    field_type="DATE",
                    required=False,
                    unique_key=False,
                    sort_no=3,
                    field_desc="实际公告日期，格式为YYYYMMDD"
                ),
                OutputField(
                    field_code="end_date",
                    field_name="报告期",
                    field_type="DATE",
                    required=True,
                    unique_key=True,
                    sort_no=4,
                    field_desc="报告期，格式为YYYYMMDD"
                ),
                OutputField(
                    field_code="report_type",
                    field_name="报告类型",
                    field_type="STRING",
                    required=True,
                    unique_key=True,
                    sort_no=5,
                    field_desc="报告类型"
                ),
                OutputField(
                    field_code="comp_type",
                    field_name="公司类型",
                    field_type="STRING",
                    required=False,
                    unique_key=False,
                    sort_no=6,
                    field_desc="公司类型"
                ),
                OutputField(
                    field_code="basic_eps",
                    field_name="基本每股收益",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=7,
                    field_desc="基本每股收益"
                ),
                OutputField(
                    field_code="diluted_eps",
                    field_name="稀释每股收益",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=8,
                    field_desc="稀释每股收益"
                ),
                OutputField(
                    field_code="total_revenue",
                    field_name="营业总收入",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=9,
                    field_desc="营业总收入"
                ),
                OutputField(
                    field_code="revenue",
                    field_name="营业收入",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=10,
                    field_desc="营业收入"
                ),
                OutputField(
                    field_code="total_cogs",
                    field_name="营业总成本",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=11,
                    field_desc="营业总成本"
                ),
                OutputField(
                    field_code="oper_cost",
                    field_name="营业成本",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=12,
                    field_desc="营业成本"
                ),
                OutputField(
                    field_code="sell_exp",
                    field_name="销售费用",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=13,
                    field_desc="销售费用"
                ),
                OutputField(
                    field_code="admin_exp",
                    field_name="管理费用",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=14,
                    field_desc="管理费用"
                ),
                OutputField(
                    field_code="fin_exp",
                    field_name="财务费用",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=15,
                    field_desc="财务费用"
                ),
                OutputField(
                    field_code="rd_exp",
                    field_name="研发费用",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=16,
                    field_desc="研发费用"
                ),
                OutputField(
                    field_code="invest_income",
                    field_name="投资收益",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=17,
                    field_desc="投资收益"
                ),
                OutputField(
                    field_code="oper_profit",
                    field_name="营业利润",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=18,
                    field_desc="营业利润"
                ),
                OutputField(
                    field_code="total_profit",
                    field_name="利润总额",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=19,
                    field_desc="利润总额"
                ),
                OutputField(
                    field_code="income_tax",
                    field_name="所得税费用",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=20,
                    field_desc="所得税费用"
                ),
                OutputField(
                    field_code="n_income",
                    field_name="净利润",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=21,
                    field_desc="净利润"
                ),
                OutputField(
                    field_code="n_income_attr_p",
                    field_name="归属于母公司所有者的净利润",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=22,
                    field_desc="归属于母公司所有者的净利润"
                ),
                OutputField(
                    field_code="ebit",
                    field_name="息税前利润",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=23,
                    field_desc="息税前利润"
                ),
                OutputField(
                    field_code="ebitda",
                    field_name="息税折旧摊销前利润",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=24,
                    field_desc="息税折旧摊销前利润"
                ),
                OutputField(
                    field_code="update_flag",
                    field_name="更新标识",
                    field_type="STRING",
                    required=False,
                    unique_key=False,
                    sort_no=25,
                    field_desc="更新标识"
                ),
                OutputField(
                    field_code="raw_json",
                    field_name="原始JSON记录",
                    field_type="TEXT",
                    required=False,
                    unique_key=False,
                    sort_no=26,
                    field_desc="Tushare接口原始返回记录，JSON字符串"
                )
            ]
        )
    )
