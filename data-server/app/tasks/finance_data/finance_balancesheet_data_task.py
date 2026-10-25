import json
import logging
from typing import Any, cast

import numpy as np

from app.core.registry import registry
from app.schemas.collection_task.task_template import ExecutionContext, OutputField, TaskRunRequest, TaskRunResult, TaskTemplateInfo
from app.tasks.pipeline import BaseCollectTask
from app.core.providers import ts_provider

logger = logging.getLogger(__name__)

class FinanceBalancesheetDataTask(BaseCollectTask):
    """
    从tushare接口获取股票资产负债表数据。
    该接口用于后续增量更新场景：传入股票代码和公告日期，获取指定股票在指定公告日期发布的资产负债表数据。
    """

    def __init__(self):

        super().__init__()

        self.task_code = 'finance_balancesheet_data'

        self.ts = ts_provider

        self.collect_fields = ['ts_code', 'ann_date', 'f_ann_date', 'end_date', 'report_type', 'comp_type', 'total_share', 'money_cap', 'trad_asset', 'notes_receiv', 'accounts_receiv', 'oth_receiv', 'prepayment', 'inventories', 'total_cur_assets', 'lt_eqt_invest', 'invest_real_estate', 'fix_assets', 'cip', 'intan_assets', 'goodwill', 'defer_tax_assets', 'total_nca', 'total_assets', 'st_borr', 'notes_payable', 'acct_payable', 'adv_receipts', 'payroll_payable', 'taxes_payable', 'oth_payable', 'non_cur_liab_due_1y', 'total_cur_liab', 'lt_borr', 'bond_payable', 'lt_payable', 'defer_tax_liab', 'total_ncl', 'total_liab', 'treasury_share', 'cap_rese', 'surplus_rese', 'undistr_porfit', 'minority_int', 'total_hldr_eqy_exc_min_int', 'total_hldr_eqy_inc_min_int', 'total_liab_hldr_eqy', 'update_flag', 'raw_json']
        self.field_aliases = {}


    def _to_records(self, data_frame: Any) -> list[dict[str, Any]]:
        """
            保留资产负债表核心字段，并将Tushare接口原始返回记录写入raw_json字段。
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
            采集股票资产负债表更新数据：
            1）传入股票代码ts_code；
            2）传入公告日期ann_date；
            3）调用Tushare的balancesheet接口，获取指定股票在指定公告日期发布的资产负债表数据。
        """

        ts_code = params.get("ts_code", None)
        ann_date = params.get("ann_date", None)
        if ts_code == None or ann_date == None:
            raise ValueError("ts_code和ann_date参数不能为空")

        finance_data = await self.ts.balancesheet(ts_code=ts_code, ann_date=ann_date)

        return self._to_records(finance_data)


async def finance_balancesheet_data_handler(request: TaskRunRequest) -> TaskRunResult:
    """采集任务的handler，负责调用task的execute方法并返回结果"""
    task = FinanceBalancesheetDataTask()

    return await task.execute(request)


def register_finance_balancesheet_data_task():
    """注册task和handler到registry中"""
    registry.register_handler('finance_balancesheet_data_handler', finance_balancesheet_data_handler)

    registry.register_task_template(
        TaskTemplateInfo(
            task_code='finance_balancesheet_data',
            task_name='股票资产负债表数据采集',
            task_desc='采集股票资产负债表更新数据。任务需要传入ts_code和ann_date参数，调用Tushare的balancesheet接口获取指定股票在指定公告日期发布的资产负债表数据。',
            handler_name='finance_balancesheet_data_handler',
            data_source='TUSHARE',
            asset_type='股票',
            biz_type='资产负债表',
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
                    field_code="total_share",
                    field_name="期末总股本",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=7,
                    field_desc="期末总股本"
                ),
                OutputField(
                    field_code="money_cap",
                    field_name="货币资金",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=8,
                    field_desc="货币资金"
                ),
                OutputField(
                    field_code="trad_asset",
                    field_name="交易性金融资产",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=9,
                    field_desc="交易性金融资产"
                ),
                OutputField(
                    field_code="notes_receiv",
                    field_name="应收票据",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=10,
                    field_desc="应收票据"
                ),
                OutputField(
                    field_code="accounts_receiv",
                    field_name="应收账款",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=11,
                    field_desc="应收账款"
                ),
                OutputField(
                    field_code="oth_receiv",
                    field_name="其他应收款",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=12,
                    field_desc="其他应收款"
                ),
                OutputField(
                    field_code="prepayment",
                    field_name="预付款项",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=13,
                    field_desc="预付款项"
                ),
                OutputField(
                    field_code="inventories",
                    field_name="存货",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=14,
                    field_desc="存货"
                ),
                OutputField(
                    field_code="total_cur_assets",
                    field_name="流动资产合计",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=15,
                    field_desc="流动资产合计"
                ),
                OutputField(
                    field_code="lt_eqt_invest",
                    field_name="长期股权投资",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=16,
                    field_desc="长期股权投资"
                ),
                OutputField(
                    field_code="invest_real_estate",
                    field_name="投资性房地产",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=17,
                    field_desc="投资性房地产"
                ),
                OutputField(
                    field_code="fix_assets",
                    field_name="固定资产",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=18,
                    field_desc="固定资产"
                ),
                OutputField(
                    field_code="cip",
                    field_name="在建工程",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=19,
                    field_desc="在建工程"
                ),
                OutputField(
                    field_code="intan_assets",
                    field_name="无形资产",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=20,
                    field_desc="无形资产"
                ),
                OutputField(
                    field_code="goodwill",
                    field_name="商誉",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=21,
                    field_desc="商誉"
                ),
                OutputField(
                    field_code="defer_tax_assets",
                    field_name="递延所得税资产",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=22,
                    field_desc="递延所得税资产"
                ),
                OutputField(
                    field_code="total_nca",
                    field_name="非流动资产合计",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=23,
                    field_desc="非流动资产合计"
                ),
                OutputField(
                    field_code="total_assets",
                    field_name="资产总计",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=24,
                    field_desc="资产总计"
                ),
                OutputField(
                    field_code="st_borr",
                    field_name="短期借款",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=25,
                    field_desc="短期借款"
                ),
                OutputField(
                    field_code="notes_payable",
                    field_name="应付票据",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=26,
                    field_desc="应付票据"
                ),
                OutputField(
                    field_code="acct_payable",
                    field_name="应付账款",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=27,
                    field_desc="应付账款"
                ),
                OutputField(
                    field_code="adv_receipts",
                    field_name="预收款项",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=28,
                    field_desc="预收款项"
                ),
                OutputField(
                    field_code="payroll_payable",
                    field_name="应付职工薪酬",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=29,
                    field_desc="应付职工薪酬"
                ),
                OutputField(
                    field_code="taxes_payable",
                    field_name="应交税费",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=30,
                    field_desc="应交税费"
                ),
                OutputField(
                    field_code="oth_payable",
                    field_name="其他应付款",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=31,
                    field_desc="其他应付款"
                ),
                OutputField(
                    field_code="non_cur_liab_due_1y",
                    field_name="一年内到期的非流动负债",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=32,
                    field_desc="一年内到期的非流动负债"
                ),
                OutputField(
                    field_code="total_cur_liab",
                    field_name="流动负债合计",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=33,
                    field_desc="流动负债合计"
                ),
                OutputField(
                    field_code="lt_borr",
                    field_name="长期借款",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=34,
                    field_desc="长期借款"
                ),
                OutputField(
                    field_code="bond_payable",
                    field_name="应付债券",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=35,
                    field_desc="应付债券"
                ),
                OutputField(
                    field_code="lt_payable",
                    field_name="长期应付款",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=36,
                    field_desc="长期应付款"
                ),
                OutputField(
                    field_code="defer_tax_liab",
                    field_name="递延所得税负债",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=37,
                    field_desc="递延所得税负债"
                ),
                OutputField(
                    field_code="total_ncl",
                    field_name="非流动负债合计",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=38,
                    field_desc="非流动负债合计"
                ),
                OutputField(
                    field_code="total_liab",
                    field_name="负债合计",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=39,
                    field_desc="负债合计"
                ),
                OutputField(
                    field_code="treasury_share",
                    field_name="库存股",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=40,
                    field_desc="库存股"
                ),
                OutputField(
                    field_code="cap_rese",
                    field_name="资本公积",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=41,
                    field_desc="资本公积"
                ),
                OutputField(
                    field_code="surplus_rese",
                    field_name="盈余公积",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=42,
                    field_desc="盈余公积"
                ),
                OutputField(
                    field_code="undistr_porfit",
                    field_name="未分配利润",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=43,
                    field_desc="未分配利润"
                ),
                OutputField(
                    field_code="minority_int",
                    field_name="少数股东权益",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=44,
                    field_desc="少数股东权益"
                ),
                OutputField(
                    field_code="total_hldr_eqy_exc_min_int",
                    field_name="归属于母公司股东权益合计",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=45,
                    field_desc="归属于母公司股东权益合计"
                ),
                OutputField(
                    field_code="total_hldr_eqy_inc_min_int",
                    field_name="股东权益合计",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=46,
                    field_desc="股东权益合计"
                ),
                OutputField(
                    field_code="total_liab_hldr_eqy",
                    field_name="负债和股东权益总计",
                    field_type="DECIMAL",
                    required=False,
                    unique_key=False,
                    sort_no=47,
                    field_desc="负债和股东权益总计"
                ),
                OutputField(
                    field_code="update_flag",
                    field_name="更新标识",
                    field_type="STRING",
                    required=False,
                    unique_key=False,
                    sort_no=48,
                    field_desc="更新标识"
                ),
                OutputField(
                    field_code="raw_json",
                    field_name="原始JSON记录",
                    field_type="TEXT",
                    required=False,
                    unique_key=False,
                    sort_no=49,
                    field_desc="Tushare接口原始返回记录，JSON字符串"
                )
            ]
        )
    )
