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

class FinanceCashflowInitTask(BaseCollectTask):
    """
    初始化采集股票现金流量表数据，先获取股票列表，再逐个股票采集全部历史现金流量表数据。
    """

    def __init__(self):

        super().__init__()

        self.task_code = 'finance_cashflow_init'

        self.ts = ts_provider

        self.collect_fields = ['ts_code', 'ann_date', 'f_ann_date', 'end_date', 'report_type', 'comp_type', 'net_profit', 'finan_exp', 'c_fr_sale_sg', 'recp_tax_rends', 'c_fr_oth_operate_a', 'c_inf_fr_operate_a', 'c_paid_goods_s', 'c_paid_to_for_empl', 'c_paid_for_taxes', 'oth_cash_pay_oper_act', 'st_cash_out_act', 'n_cashflow_act', 'c_disp_withdrwl_invest', 'c_recp_return_invest', 'n_recp_disp_fiolta', 'stot_inflows_inv_act', 'c_pay_acq_const_fiolta', 'c_paid_invest', 'stot_out_inv_act', 'n_cashflow_inv_act', 'c_recp_borrow', 'proc_issue_bonds', 'stot_cash_in_fnc_act', 'c_prepay_amt_borr', 'c_pay_dist_dpcp_int_exp', 'stot_cashout_fnc_act', 'n_cash_flows_fnc_act', 'eff_fx_flu_cash', 'n_incr_cash_cash_equ', 'c_cash_equ_beg_period', 'c_cash_equ_end_period', 'free_cashflow', 'im_net_cashflow_oper_act', 'update_flag', 'raw_json']
        self.field_aliases = {}


    def _to_records(self, data_frame: Any) -> list[dict[str, Any]]:
        """
            保留现金流量表核心字段，并将Tushare接口原始返回记录写入raw_json字段。
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
            批量初始化采集股票现金流量表数据：
            1）从股票基本数据接口读取股票列表；
            2）对每个股票代码ts_code，仅传入ts_code参数，采集该股票全部历史现金流量表数据；
            3）每只股票的数据作为一批yield出去，由基类自动完成后续标准化、清洗、字段映射和入库；
            4）相邻两次读取现金流量表之间暂停1秒。
        """

        stock_basic = await self.ts.stock_basic()
        ts_codes = stock_basic["ts_code"].dropna().unique().tolist()

        for index, ts_code in enumerate(ts_codes):
            logger.info(f"开始采集股票{ts_code}（{index + 1}/{len(ts_codes)}）的历史现金流量表数据")
            finance_data = await self.ts.cashflow(ts_code=str(ts_code))
            # 由于接口最多一次返回100条数据，所以当采集到的数据量为100时，取最后一条的end_date作为下次查询的end_date，继续采集一次，暂时每只股采集两次
            if len(finance_data) == 100:
                last_end_date = finance_data["end_date"].dropna().unique().tolist()[-1]
                finance_data_2 = await self.ts.cashflow(ts_code=str(ts_code), end_date=str(last_end_date))
                if len(finance_data_2) > 0:
                    finance_data = pd.concat([finance_data, finance_data_2], ignore_index=True)

            records = self._to_records(finance_data)
            if len(records) > 0:
                yield records
            else:
                logger.info(f"股票{ts_code}没有获取到历史现金流量表数据")

            if index < len(ts_codes) - 1:
                await asyncio.sleep(1)


async def finance_cashflow_init_handler(request: TaskRunRequest) -> TaskRunResult:
    """采集任务的handler，负责调用task的execute方法并返回结果"""
    task = FinanceCashflowInitTask()

    return await task.execute(request)


def register_finance_cashflow_init_task():
    """注册task和handler到registry中"""
    registry.register_handler('finance_cashflow_init_handler', finance_cashflow_init_handler)

    registry.register_task_template(
        TaskTemplateInfo(
            task_code='finance_cashflow_init',
            task_name='股票现金流量表初始化采集',
            task_desc='初始化采集所有股票的历史现金流量表数据。任务先从股票基本数据接口获取股票列表，再逐个股票仅传入ts_code参数采集全部历史现金流量表数据，每只股票的数据作为一批进行后续处理和入库，相邻两次接口调用之间暂停1秒。',
            handler_name='finance_cashflow_init_handler',
            data_source='TUSHARE',
            asset_type='股票',
            biz_type='现金流量表',
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
                    field_code="net_profit",
                    field_name="净利润",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=7,
                    field_desc="净利润"
                ),
                OutputField(
                    field_code="finan_exp",
                    field_name="财务费用",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=8,
                    field_desc="财务费用"
                ),
                OutputField(
                    field_code="c_fr_sale_sg",
                    field_name="销售商品、提供劳务收到的现金",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=9,
                    field_desc="销售商品、提供劳务收到的现金"
                ),
                OutputField(
                    field_code="recp_tax_rends",
                    field_name="收到的税费返还",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=10,
                    field_desc="收到的税费返还"
                ),
                OutputField(
                    field_code="c_fr_oth_operate_a",
                    field_name="收到其他与经营活动有关的现金",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=11,
                    field_desc="收到其他与经营活动有关的现金"
                ),
                OutputField(
                    field_code="c_inf_fr_operate_a",
                    field_name="经营活动现金流入小计",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=12,
                    field_desc="经营活动现金流入小计"
                ),
                OutputField(
                    field_code="c_paid_goods_s",
                    field_name="购买商品、接受劳务支付的现金",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=13,
                    field_desc="购买商品、接受劳务支付的现金"
                ),
                OutputField(
                    field_code="c_paid_to_for_empl",
                    field_name="支付给职工以及为职工支付的现金",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=14,
                    field_desc="支付给职工以及为职工支付的现金"
                ),
                OutputField(
                    field_code="c_paid_for_taxes",
                    field_name="支付的各项税费",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=15,
                    field_desc="支付的各项税费"
                ),
                OutputField(
                    field_code="oth_cash_pay_oper_act",
                    field_name="支付其他与经营活动有关的现金",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=16,
                    field_desc="支付其他与经营活动有关的现金"
                ),
                OutputField(
                    field_code="st_cash_out_act",
                    field_name="经营活动现金流出小计",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=17,
                    field_desc="经营活动现金流出小计"
                ),
                OutputField(
                    field_code="n_cashflow_act",
                    field_name="经营活动产生的现金流量净额",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=18,
                    field_desc="经营活动产生的现金流量净额"
                ),
                OutputField(
                    field_code="c_disp_withdrwl_invest",
                    field_name="收回投资收到的现金",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=19,
                    field_desc="收回投资收到的现金"
                ),
                OutputField(
                    field_code="c_recp_return_invest",
                    field_name="取得投资收益收到的现金",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=20,
                    field_desc="取得投资收益收到的现金"
                ),
                OutputField(
                    field_code="n_recp_disp_fiolta",
                    field_name="处置固定资产、无形资产和其他长期资产收回的现金净额",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=21,
                    field_desc="处置固定资产、无形资产和其他长期资产收回的现金净额"
                ),
                OutputField(
                    field_code="stot_inflows_inv_act",
                    field_name="投资活动现金流入小计",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=22,
                    field_desc="投资活动现金流入小计"
                ),
                OutputField(
                    field_code="c_pay_acq_const_fiolta",
                    field_name="购建固定资产、无形资产和其他长期资产支付的现金",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=23,
                    field_desc="购建固定资产、无形资产和其他长期资产支付的现金"
                ),
                OutputField(
                    field_code="c_paid_invest",
                    field_name="投资支付的现金",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=24,
                    field_desc="投资支付的现金"
                ),
                OutputField(
                    field_code="stot_out_inv_act",
                    field_name="投资活动现金流出小计",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=25,
                    field_desc="投资活动现金流出小计"
                ),
                OutputField(
                    field_code="n_cashflow_inv_act",
                    field_name="投资活动产生的现金流量净额",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=26,
                    field_desc="投资活动产生的现金流量净额"
                ),
                OutputField(
                    field_code="c_recp_borrow",
                    field_name="取得借款收到的现金",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=27,
                    field_desc="取得借款收到的现金"
                ),
                OutputField(
                    field_code="proc_issue_bonds",
                    field_name="发行债券收到的现金",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=28,
                    field_desc="发行债券收到的现金"
                ),
                OutputField(
                    field_code="stot_cash_in_fnc_act",
                    field_name="筹资活动现金流入小计",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=29,
                    field_desc="筹资活动现金流入小计"
                ),
                OutputField(
                    field_code="c_prepay_amt_borr",
                    field_name="偿还债务支付的现金",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=30,
                    field_desc="偿还债务支付的现金"
                ),
                OutputField(
                    field_code="c_pay_dist_dpcp_int_exp",
                    field_name="分配股利、利润或偿付利息支付的现金",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=31,
                    field_desc="分配股利、利润或偿付利息支付的现金"
                ),
                OutputField(
                    field_code="stot_cashout_fnc_act",
                    field_name="筹资活动现金流出小计",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=32,
                    field_desc="筹资活动现金流出小计"
                ),
                OutputField(
                    field_code="n_cash_flows_fnc_act",
                    field_name="筹资活动产生的现金流量净额",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=33,
                    field_desc="筹资活动产生的现金流量净额"
                ),
                OutputField(
                    field_code="eff_fx_flu_cash",
                    field_name="汇率变动对现金的影响",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=34,
                    field_desc="汇率变动对现金的影响"
                ),
                OutputField(
                    field_code="n_incr_cash_cash_equ",
                    field_name="现金及现金等价物净增加额",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=35,
                    field_desc="现金及现金等价物净增加额"
                ),
                OutputField(
                    field_code="c_cash_equ_beg_period",
                    field_name="期初现金及现金等价物余额",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=36,
                    field_desc="期初现金及现金等价物余额"
                ),
                OutputField(
                    field_code="c_cash_equ_end_period",
                    field_name="期末现金及现金等价物余额",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=37,
                    field_desc="期末现金及现金等价物余额"
                ),
                OutputField(
                    field_code="free_cashflow",
                    field_name="企业自由现金流量",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=38,
                    field_desc="企业自由现金流量"
                ),
                OutputField(
                    field_code="im_net_cashflow_oper_act",
                    field_name="间接法经营活动现金流量净额",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=39,
                    field_desc="间接法经营活动现金流量净额"
                ),
                OutputField(
                    field_code="update_flag",
                    field_name="更新标识",
                    field_type="STRING",
                    required=False,
                    unique_key=False,
                    sort_no=40,
                    field_desc="更新标识"
                ),
                OutputField(
                    field_code="raw_json",
                    field_name="原始JSON记录",
                    field_type="TEXT",
                    required=False,
                    unique_key=False,
                    sort_no=41,
                    field_desc="Tushare接口原始返回记录，JSON字符串"
                )
            ]
        )
    )
