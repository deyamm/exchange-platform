import json
import logging
from typing import Any, cast

import numpy as np

from app.core.registry import registry
from app.schemas.collection_task.task_template import ExecutionContext, OutputField, TaskRunRequest, TaskRunResult, TaskTemplateInfo
from app.tasks.pipeline import BaseCollectTask
from app.core.providers import ts_provider

logger = logging.getLogger(__name__)

class FinanceIndicatorDataTask(BaseCollectTask):
    """
    从tushare接口获取股票财务指标数据。
    该接口用于后续增量更新场景：传入股票代码和公告日期，获取指定股票在指定公告日期发布的财务指标数据。
    """

    def __init__(self):

        super().__init__()

        self.task_code = 'finance_indicator_data'

        self.ts = ts_provider

        self.collect_fields = ['ts_code', 'ann_date', 'end_date', 'eps', 'dt_eps', 'bps', 'ocfps', 'cfps', 'ebit_ps', 'fcff_ps', 'fcfe_ps', 'roe', 'roe_waa', 'roe_dt', 'roa', 'roic', 'gross_margin', 'netprofit_margin', 'grossprofit_margin', 'profit_to_gr', 'op_to_ebt', 'current_ratio', 'quick_ratio', 'cash_ratio', 'debt_to_assets', 'assets_to_eqt', 'debt_to_eqt', 'ebit_to_interest', 'ar_turn', 'inv_turn', 'ca_turn', 'fa_turn', 'assets_turn', 'arturn_days', 'invturn_days', 'turn_days', 'revenue_ps', 'total_revenue_ps', 'capital_rese_ps', 'surplus_rese_ps', 'undist_profit_ps', 'profit_dedt', 'extra_item', 'ebit', 'ebitda', 'fcff', 'fcfe', 'working_capital', 'op_yoy', 'ebt_yoy', 'netprofit_yoy', 'dt_netprofit_yoy', 'revenue_yoy', 'assets_yoy', 'equity_yoy', 'update_flag', 'raw_json']
        self.field_aliases = {'or_yoy': 'revenue_yoy'}


    def _to_records(self, data_frame: Any) -> list[dict[str, Any]]:
        """
            保留财务指标核心字段，并将Tushare接口原始返回记录写入raw_json字段。
        """

        if data_frame is None or len(data_frame) == 0:
            return []

        data_frame = data_frame.replace([np.inf, -np.inf], np.nan)
        data_frame = data_frame.astype(object).where(data_frame.notnull(), None)
        # 只保留update_flag=0的记录
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


    async def collect(self, params: dict[str, Any], ctx: ExecutionContext) -> list[dict[str, Any]]:
        """
            采集股票财务指标更新数据：
            1）传入股票代码ts_code；
            2）传入公告日期ann_date；
            3）调用Tushare的fina_indicator接口，获取指定股票在指定公告日期发布的财务指标数据。
        """

        ts_code = params.get("ts_code", None)
        ann_date = params.get("ann_date", None)
        if ts_code == None or ann_date == None:
            raise ValueError("ts_code和ann_date参数不能为空")

        finance_data = await self.ts.fina_indicator(ts_code=ts_code, ann_date=ann_date)

        return self._to_records(finance_data)


async def finance_indicator_data_handler(request: TaskRunRequest) -> TaskRunResult:
    """采集任务的handler，负责调用task的execute方法并返回结果"""
    task = FinanceIndicatorDataTask()

    return await task.execute(request)


def register_finance_indicator_data_task():
    """注册task和handler到registry中"""
    registry.register_handler('finance_indicator_data_handler', finance_indicator_data_handler)

    registry.register_task_template(
        TaskTemplateInfo(
            task_code='finance_indicator_data',
            task_name='股票财务指标数据采集',
            task_desc='采集股票财务指标更新数据。任务需要传入ts_code和ann_date参数，调用Tushare的fina_indicator接口获取指定股票在指定公告日期发布的财务指标数据。',
            handler_name='finance_indicator_data_handler',
            data_source='TUSHARE',
            asset_type='股票',
            biz_type='财务指标',
            params_schema=[
                OutputField(
                    field_code="ts_code",
                    field_name="股票代码",
                    field_type="STRING",
                    required=True,
                    unique_key=False,
                    sort_no=1,
                    field_desc="股票代码，需要包括后缀，来源于股票基本数据接口返回的ts_code字段，如000001.SZ、600000.SH等"
                ),
                OutputField(
                    field_code="ann_date",
                    field_name="公告日期",
                    field_type="DATE",
                    required=True,
                    unique_key=False,
                    sort_no=2,
                    field_desc="公告日期，格式为YYYYMMDD"
                )
            ],
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
                    field_code="end_date",
                    field_name="报告期",
                    field_type="DATE",
                    required=True,
                    unique_key=True,
                    sort_no=3,
                    field_desc="报告期，格式为YYYYMMDD"
                ),
                OutputField(
                    field_code="eps",
                    field_name="基本每股收益",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=4,
                    field_desc="基本每股收益"
                ),
                OutputField(
                    field_code="dt_eps",
                    field_name="稀释每股收益",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=5,
                    field_desc="稀释每股收益"
                ),
                OutputField(
                    field_code="bps",
                    field_name="每股净资产",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=6,
                    field_desc="每股净资产"
                ),
                OutputField(
                    field_code="ocfps",
                    field_name="每股经营活动现金流量净额",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=7,
                    field_desc="每股经营活动现金流量净额"
                ),
                OutputField(
                    field_code="cfps",
                    field_name="每股现金流量净额",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=8,
                    field_desc="每股现金流量净额"
                ),
                OutputField(
                    field_code="ebit_ps",
                    field_name="每股息税前利润",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=9,
                    field_desc="每股息税前利润"
                ),
                OutputField(
                    field_code="fcff_ps",
                    field_name="每股企业自由现金流量",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=10,
                    field_desc="每股企业自由现金流量"
                ),
                OutputField(
                    field_code="fcfe_ps",
                    field_name="每股股东自由现金流量",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=11,
                    field_desc="每股股东自由现金流量"
                ),
                OutputField(
                    field_code="roe",
                    field_name="净资产收益率",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=12,
                    field_desc="净资产收益率"
                ),
                OutputField(
                    field_code="roe_waa",
                    field_name="加权平均净资产收益率",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=13,
                    field_desc="加权平均净资产收益率"
                ),
                OutputField(
                    field_code="roe_dt",
                    field_name="扣非净资产收益率",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=14,
                    field_desc="扣非净资产收益率"
                ),
                OutputField(
                    field_code="roa",
                    field_name="总资产报酬率",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=15,
                    field_desc="总资产报酬率"
                ),
                OutputField(
                    field_code="roic",
                    field_name="投入资本回报率",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=16,
                    field_desc="投入资本回报率"
                ),
                OutputField(
                    field_code="gross_margin",
                    field_name="毛利率",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=17,
                    field_desc="毛利率"
                ),
                OutputField(
                    field_code="netprofit_margin",
                    field_name="净利率",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=18,
                    field_desc="净利率"
                ),
                OutputField(
                    field_code="grossprofit_margin",
                    field_name="销售毛利率",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=19,
                    field_desc="销售毛利率"
                ),
                OutputField(
                    field_code="profit_to_gr",
                    field_name="净利润/营业总收入",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=20,
                    field_desc="净利润/营业总收入"
                ),
                OutputField(
                    field_code="op_to_ebt",
                    field_name="经营活动利润/利润总额",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=21,
                    field_desc="经营活动利润/利润总额"
                ),
                OutputField(
                    field_code="current_ratio",
                    field_name="流动比率",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=22,
                    field_desc="流动比率"
                ),
                OutputField(
                    field_code="quick_ratio",
                    field_name="速动比率",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=23,
                    field_desc="速动比率"
                ),
                OutputField(
                    field_code="cash_ratio",
                    field_name="现金比率",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=24,
                    field_desc="现金比率"
                ),
                OutputField(
                    field_code="debt_to_assets",
                    field_name="资产负债率",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=25,
                    field_desc="资产负债率"
                ),
                OutputField(
                    field_code="assets_to_eqt",
                    field_name="权益乘数",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=26,
                    field_desc="权益乘数"
                ),
                OutputField(
                    field_code="debt_to_eqt",
                    field_name="产权比率",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=27,
                    field_desc="产权比率"
                ),
                OutputField(
                    field_code="ebit_to_interest",
                    field_name="息税前利润/利息费用",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=28,
                    field_desc="息税前利润/利息费用"
                ),
                OutputField(
                    field_code="ar_turn",
                    field_name="应收账款周转率",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=29,
                    field_desc="应收账款周转率"
                ),
                OutputField(
                    field_code="inv_turn",
                    field_name="存货周转率",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=30,
                    field_desc="存货周转率"
                ),
                OutputField(
                    field_code="ca_turn",
                    field_name="流动资产周转率",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=31,
                    field_desc="流动资产周转率"
                ),
                OutputField(
                    field_code="fa_turn",
                    field_name="固定资产周转率",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=32,
                    field_desc="固定资产周转率"
                ),
                OutputField(
                    field_code="assets_turn",
                    field_name="总资产周转率",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=33,
                    field_desc="总资产周转率"
                ),
                OutputField(
                    field_code="arturn_days",
                    field_name="应收账款周转天数",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=34,
                    field_desc="应收账款周转天数"
                ),
                OutputField(
                    field_code="invturn_days",
                    field_name="存货周转天数",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=35,
                    field_desc="存货周转天数"
                ),
                OutputField(
                    field_code="turn_days",
                    field_name="营业周期",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=36,
                    field_desc="营业周期"
                ),
                OutputField(
                    field_code="revenue_ps",
                    field_name="每股营业收入",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=37,
                    field_desc="每股营业收入"
                ),
                OutputField(
                    field_code="total_revenue_ps",
                    field_name="每股营业总收入",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=38,
                    field_desc="每股营业总收入"
                ),
                OutputField(
                    field_code="capital_rese_ps",
                    field_name="每股资本公积",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=39,
                    field_desc="每股资本公积"
                ),
                OutputField(
                    field_code="surplus_rese_ps",
                    field_name="每股盈余公积",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=40,
                    field_desc="每股盈余公积"
                ),
                OutputField(
                    field_code="undist_profit_ps",
                    field_name="每股未分配利润",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=41,
                    field_desc="每股未分配利润"
                ),
                OutputField(
                    field_code="profit_dedt",
                    field_name="扣除非经常性损益后的净利润",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=42,
                    field_desc="扣除非经常性损益后的净利润"
                ),
                OutputField(
                    field_code="extra_item",
                    field_name="非经常性损益",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=43,
                    field_desc="非经常性损益"
                ),
                OutputField(
                    field_code="ebit",
                    field_name="息税前利润",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=44,
                    field_desc="息税前利润"
                ),
                OutputField(
                    field_code="ebitda",
                    field_name="息税折旧摊销前利润",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=45,
                    field_desc="息税折旧摊销前利润"
                ),
                OutputField(
                    field_code="fcff",
                    field_name="企业自由现金流量",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=46,
                    field_desc="企业自由现金流量"
                ),
                OutputField(
                    field_code="fcfe",
                    field_name="股东自由现金流量",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=47,
                    field_desc="股东自由现金流量"
                ),
                OutputField(
                    field_code="working_capital",
                    field_name="营运资金",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=48,
                    field_desc="营运资金"
                ),
                OutputField(
                    field_code="op_yoy",
                    field_name="营业利润同比增长率",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=49,
                    field_desc="营业利润同比增长率"
                ),
                OutputField(
                    field_code="ebt_yoy",
                    field_name="利润总额同比增长率",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=50,
                    field_desc="利润总额同比增长率"
                ),
                OutputField(
                    field_code="netprofit_yoy",
                    field_name="净利润同比增长率",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=51,
                    field_desc="净利润同比增长率"
                ),
                OutputField(
                    field_code="dt_netprofit_yoy",
                    field_name="扣非净利润同比增长率",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=52,
                    field_desc="扣非净利润同比增长率"
                ),
                OutputField(
                    field_code="revenue_yoy",
                    field_name="营业收入同比增长率",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=53,
                    field_desc="营业收入同比增长率"
                ),
                OutputField(
                    field_code="assets_yoy",
                    field_name="总资产同比增长率",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=54,
                    field_desc="总资产同比增长率"
                ),
                OutputField(
                    field_code="equity_yoy",
                    field_name="净资产同比增长率",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=55,
                    field_desc="净资产同比增长率"
                ),
                OutputField(
                    field_code="update_flag",
                    field_name="更新标识",
                    field_type="STRING",
                    required=False,
                    unique_key=False,
                    sort_no=56,
                    field_desc="更新标识"
                ),
                OutputField(
                    field_code="raw_json",
                    field_name="原始JSON记录",
                    field_type="TEXT",
                    required=False,
                    unique_key=False,
                    sort_no=57,
                    field_desc="Tushare接口原始返回记录，JSON字符串"
                )
            ]
        )
    )
