import json
import logging
from typing import Any, cast

import numpy as np

from app.core.registry import registry
from app.schemas.collection_task.task_template import ExecutionContext, OutputField, TaskRunRequest, TaskRunResult, TaskTemplateInfo
from app.tasks.pipeline import BaseCollectTask
from app.core.providers import ts_provider

logger = logging.getLogger(__name__)

class FinanceCashflowDataTask(BaseCollectTask):
    """
    从tushare接口获取股票现金流量表数据。
    该接口用于后续增量更新场景：传入股票代码和公告日期，获取指定股票在指定公告日期发布的现金流量表数据。
    """

    def __init__(self):

        super().__init__()

        self.task_code = 'finance_cashflow_data'

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
        # 只保留update_flag=0的记录
        data_frame = data_frame[data_frame["update_flag"] == '0']
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
            采集股票现金流量表更新数据：
            1）传入股票代码ts_code；
            2）传入公告日期ann_date；
            3）调用Tushare的cashflow接口，获取指定股票在指定公告日期发布的现金流量表数据。
        """

        ts_code = params.get("ts_code", None)
        ann_date = params.get("ann_date", None)
        if ts_code == None or ann_date == None:
            raise ValueError("ts_code和ann_date参数不能为空")

        finance_data = await self.ts.cashflow(ts_code=ts_code, ann_date=ann_date)

        return self._to_records(finance_data)


async def finance_cashflow_data_handler(request: TaskRunRequest) -> TaskRunResult:
    """采集任务的handler，负责调用task的execute方法并返回结果"""
    task = FinanceCashflowDataTask()

    return await task.execute(request)


def register_finance_cashflow_data_task():
    """注册task和handler到registry中"""
    registry.register_handler('finance_cashflow_data_handler', finance_cashflow_data_handler)

    registry.register_task_template(
        TaskTemplateInfo(
            task_code='finance_cashflow_data',
            task_name='股票现金流量表数据采集',
            task_desc='采集股票现金流量表更新数据。任务需要传入ts_code和ann_date参数，调用Tushare的cashflow接口获取指定股票在指定公告日期发布的现金流量表数据。',
            handler_name='finance_cashflow_data_handler',
            data_source='TUSHARE',
            asset_type='股票',
            biz_type='现金流量表',
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
