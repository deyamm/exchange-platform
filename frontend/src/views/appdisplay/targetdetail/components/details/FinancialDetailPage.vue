<template>
    <div class="financial-detail-page">
        <template v-if="financial || hasFinancialDetail">
            <el-row :gutter="12">
                <el-col :xs="24" :sm="12" :md="8" :lg="4" v-for="item in summaryCards" :key="item.label">
                    <el-card class="metric-card" shadow="never">
                        <div class="metric-label">{{ item.label }}</div>
                        <div class="metric-value" :class="item.className">{{ item.value }}</div>
                        <div class="metric-desc">{{ item.desc }}</div>
                    </el-card>
                </el-col>
            </el-row>

            <el-alert
                v-if="financial?.conclusion"
                class="conclusion"
                :title="financial.conclusion"
                type="info"
                show-icon
                :closable="false"
            />

            <el-card class="detail-card" shadow="never">
                <template #header>
                    <div class="card-header">
                        <div>
                            <span class="card-title">利润趋势</span>
                            <span class="card-tip">营业总收入、归母净利润、扣非净利润、每股收益</span>
                        </div>
                        <el-select v-model="profitDisplayMode" size="small" style="width: 128px">
                            <el-option label="全部" value="ALL" />
                            <el-option label="单季度" value="SINGLE_QUARTER" />
                        </el-select>
                    </div>
                </template>

                <el-tabs v-model="activeProfitMetric" class="metric-tabs">
                    <el-tab-pane
                        v-for="item in profitMetricOptions"
                        :key="item.value"
                        :label="item.label"
                        :name="item.value"
                    />
                </el-tabs>

                <el-empty v-if="!profitChartRows.length" description="暂无利润趋势数据" />
                <TargetEcharts v-else :option="profitTrendOption" height="420px" />
            </el-card>

            <el-card class="detail-card" shadow="never">
                <template #header>
                    <div class="card-header">
                        <div>
                            <span class="card-title">盈利、成长与负债率</span>
                            <span class="card-tip">ROE、毛利率、净利率、资产负债率</span>
                        </div>
                    </div>
                </template>

                <el-tabs v-model="activeRatioMetric" class="metric-tabs">
                    <el-tab-pane
                        v-for="item in ratioMetricOptions"
                        :key="item.value"
                        :label="item.label"
                        :name="item.value"
                    />
                </el-tabs>

                <el-empty v-if="!ratioTrendRows.length" description="暂无财务指标明细" />
                <TargetEcharts v-else :option="ratioTrendOption" height="360px" />
            </el-card>

            <el-row :gutter="12">
                <el-col :xs="24" :lg="12">
                    <el-card class="detail-card" shadow="never">
                        <template #header>
                            <span class="card-title">现金流结构</span>
                        </template>
                        <el-empty v-if="!cashflowChartRows.length" description="暂无现金流量表明细" />
                        <TargetEcharts v-else :option="cashflowStructureOption" height="380px" />
                    </el-card>
                </el-col>

                <el-col :xs="24" :lg="12">
                    <el-card class="detail-card" shadow="never">
                        <template #header>
                            <span class="card-title">资产负债结构</span>
                        </template>
                        <el-empty v-if="!balanceChartRows.length" description="暂无资产负债表明细" />
                        <TargetEcharts v-else :option="balanceStructureOption" height="380px" />
                    </el-card>
                </el-col>
            </el-row>

            <el-card class="detail-card" shadow="never">
                <template #header>
                    <div class="card-header">
                        <div>
                            <span class="card-title">三大报表与财务指标明细</span>
                            <span class="card-tip">所有列名均使用物理表字段驼峰格式</span>
                        </div>
                    </div>
                </template>

                <el-tabs v-model="activeStatementTab">
                    <el-tab-pane label="利润表" name="income">
                        <el-table :data="incomeRows.slice().reverse()" border height="460">
                            <el-table-column prop="endDate" label="报告期" width="110" align="center" fixed />
                            <el-table-column prop="annDate" label="公告日期" width="110" align="center" />
                            <el-table-column prop="reportType" label="报告类型" width="110" />
                            <el-table-column prop="basicEps" label="basicEps" width="100" align="right" />
                            <el-table-column prop="dilutedEps" label="dilutedEps" width="110" align="right" />
                            <el-table-column label="totalRevenue" min-width="130" align="right">
                                <template #default="{ row }">{{ formatAmount(row.totalRevenue) }}</template>
                            </el-table-column>
                            <el-table-column label="revenue" min-width="130" align="right">
                                <template #default="{ row }">{{ formatAmount(row.revenue) }}</template>
                            </el-table-column>
                            <el-table-column label="totalCogs" min-width="130" align="right">
                                <template #default="{ row }">{{ formatAmount(row.totalCogs) }}</template>
                            </el-table-column>
                            <el-table-column label="operCost" min-width="130" align="right">
                                <template #default="{ row }">{{ formatAmount(row.operCost) }}</template>
                            </el-table-column>
                            <el-table-column label="sellExp" min-width="120" align="right">
                                <template #default="{ row }">{{ formatAmount(row.sellExp) }}</template>
                            </el-table-column>
                            <el-table-column label="adminExp" min-width="120" align="right">
                                <template #default="{ row }">{{ formatAmount(row.adminExp) }}</template>
                            </el-table-column>
                            <el-table-column label="finExp" min-width="120" align="right">
                                <template #default="{ row }">{{ formatAmount(row.finExp) }}</template>
                            </el-table-column>
                            <el-table-column label="rdExp" min-width="120" align="right">
                                <template #default="{ row }">{{ formatAmount(row.rdExp) }}</template>
                            </el-table-column>
                            <el-table-column label="operProfit" min-width="130" align="right">
                                <template #default="{ row }">{{ formatAmount(row.operProfit) }}</template>
                            </el-table-column>
                            <el-table-column label="nIncomeAttrP" min-width="140" align="right">
                                <template #default="{ row }">{{ formatAmount(row.nIncomeAttrP) }}</template>
                            </el-table-column>
                            <el-table-column label="ebit" min-width="120" align="right">
                                <template #default="{ row }">{{ formatAmount(row.ebit) }}</template>
                            </el-table-column>
                            <el-table-column label="ebitda" min-width="120" align="right">
                                <template #default="{ row }">{{ formatAmount(row.ebitda) }}</template>
                            </el-table-column>
                        </el-table>
                    </el-tab-pane>

                    <el-tab-pane label="资产负债表" name="balance">
                        <el-table :data="balanceSheetRows.slice().reverse()" border height="460">
                            <el-table-column prop="endDate" label="报告期" width="110" align="center" fixed />
                            <el-table-column prop="annDate" label="公告日期" width="110" align="center" />
                            <el-table-column label="moneyCap" min-width="120" align="right">
                                <template #default="{ row }">{{ formatAmount(row.moneyCap) }}</template>
                            </el-table-column>
                            <el-table-column label="accountsReceiv" min-width="140" align="right">
                                <template #default="{ row }">{{ formatAmount(row.accountsReceiv) }}</template>
                            </el-table-column>
                            <el-table-column label="inventories" min-width="120" align="right">
                                <template #default="{ row }">{{ formatAmount(row.inventories) }}</template>
                            </el-table-column>
                            <el-table-column label="totalCurAssets" min-width="140" align="right">
                                <template #default="{ row }">{{ formatAmount(row.totalCurAssets) }}</template>
                            </el-table-column>
                            <el-table-column label="fixAssets" min-width="120" align="right">
                                <template #default="{ row }">{{ formatAmount(row.fixAssets) }}</template>
                            </el-table-column>
                            <el-table-column label="goodwill" min-width="120" align="right">
                                <template #default="{ row }">{{ formatAmount(row.goodwill) }}</template>
                            </el-table-column>
                            <el-table-column label="totalAssets" min-width="140" align="right">
                                <template #default="{ row }">{{ formatAmount(row.totalAssets) }}</template>
                            </el-table-column>
                            <el-table-column label="stBorr" min-width="120" align="right">
                                <template #default="{ row }">{{ formatAmount(row.stBorr) }}</template>
                            </el-table-column>
                            <el-table-column label="totalCurLiab" min-width="140" align="right">
                                <template #default="{ row }">{{ formatAmount(row.totalCurLiab) }}</template>
                            </el-table-column>
                            <el-table-column label="ltBorr" min-width="120" align="right">
                                <template #default="{ row }">{{ formatAmount(row.ltBorr) }}</template>
                            </el-table-column>
                            <el-table-column label="totalLiab" min-width="140" align="right">
                                <template #default="{ row }">{{ formatAmount(row.totalLiab) }}</template>
                            </el-table-column>
                            <el-table-column label="totalHldrEqyExcMinInt" min-width="180" align="right">
                                <template #default="{ row }">{{ formatAmount(row.totalHldrEqyExcMinInt) }}</template>
                            </el-table-column>
                        </el-table>
                    </el-tab-pane>

                    <el-tab-pane label="现金流量表" name="cashflow">
                        <el-table :data="cashflowRows.slice().reverse()" border height="460">
                            <el-table-column prop="endDate" label="报告期" width="110" align="center" fixed />
                            <el-table-column prop="annDate" label="公告日期" width="110" align="center" />
                            <el-table-column label="netProfit" min-width="120" align="right">
                                <template #default="{ row }">{{ formatAmount(row.netProfit) }}</template>
                            </el-table-column>
                            <el-table-column label="cInfFrOperateA" min-width="150" align="right">
                                <template #default="{ row }">{{ formatAmount(row.cInfFrOperateA) }}</template>
                            </el-table-column>
                            <el-table-column label="stCashOutAct" min-width="140" align="right">
                                <template #default="{ row }">{{ formatAmount(row.stCashOutAct) }}</template>
                            </el-table-column>
                            <el-table-column label="nCashflowAct" min-width="140" align="right">
                                <template #default="{ row }">{{ formatAmount(row.nCashflowAct) }}</template>
                            </el-table-column>
                            <el-table-column label="nCashflowInvAct" min-width="150" align="right">
                                <template #default="{ row }">{{ formatAmount(row.nCashflowInvAct) }}</template>
                            </el-table-column>
                            <el-table-column label="nCashFlowsFncAct" min-width="160" align="right">
                                <template #default="{ row }">{{ formatAmount(row.nCashFlowsFncAct) }}</template>
                            </el-table-column>
                            <el-table-column label="nIncrCashCashEqu" min-width="160" align="right">
                                <template #default="{ row }">{{ formatAmount(row.nIncrCashCashEqu) }}</template>
                            </el-table-column>
                            <el-table-column label="freeCashflow" min-width="140" align="right">
                                <template #default="{ row }">{{ formatAmount(row.freeCashflow) }}</template>
                            </el-table-column>
                            <el-table-column label="cCashEquEndPeriod" min-width="160" align="right">
                                <template #default="{ row }">{{ formatAmount(row.cCashEquEndPeriod) }}</template>
                            </el-table-column>
                        </el-table>
                    </el-tab-pane>

                    <el-tab-pane label="财务指标" name="indicator">
                        <el-table :data="indicatorRows.slice().reverse()" border height="460">
                            <el-table-column prop="endDate" label="报告期" width="110" align="center" fixed />
                            <el-table-column prop="annDate" label="公告日期" width="110" align="center" />
                            <el-table-column prop="eps" label="eps" width="90" align="right" />
                            <el-table-column prop="bps" label="bps" width="90" align="right" />
                            <el-table-column label="roe" width="100" align="right">
                                <template #default="{ row }">{{ formatPercent(row.roe) }}</template>
                            </el-table-column>
                            <el-table-column label="roa" width="100" align="right">
                                <template #default="{ row }">{{ formatPercent(row.roa) }}</template>
                            </el-table-column>
                            <el-table-column label="roic" width="100" align="right">
                                <template #default="{ row }">{{ formatPercent(row.roic) }}</template>
                            </el-table-column>
                            <el-table-column label="grossMargin" width="130" align="right">
                                <template #default="{ row }">{{ formatPercent(row.grossMargin) }}</template>
                            </el-table-column>
                            <el-table-column label="netprofitMargin" width="150" align="right">
                                <template #default="{ row }">{{ formatPercent(row.netprofitMargin) }}</template>
                            </el-table-column>
                            <el-table-column prop="currentRatio" label="currentRatio" width="130" align="right" />
                            <el-table-column prop="quickRatio" label="quickRatio" width="120" align="right" />
                            <el-table-column label="debtToAssets" width="130" align="right">
                                <template #default="{ row }">{{ formatPercent(row.debtToAssets) }}</template>
                            </el-table-column>
                            <el-table-column prop="arTurn" label="arTurn" width="100" align="right" />
                            <el-table-column prop="invTurn" label="invTurn" width="100" align="right" />
                            <el-table-column prop="assetsTurn" label="assetsTurn" width="110" align="right" />
                            <el-table-column label="revenueYoy" width="120" align="right">
                                <template #default="{ row }">{{ formatPercent(row.revenueYoy, 2, true) }}</template>
                            </el-table-column>
                            <el-table-column label="netprofitYoy" width="130" align="right">
                                <template #default="{ row }">{{ formatPercent(row.netprofitYoy, 2, true) }}</template>
                            </el-table-column>
                            <el-table-column label="dtNetprofitYoy" width="140" align="right">
                                <template #default="{ row }">{{ formatPercent(row.dtNetprofitYoy, 2, true) }}</template>
                            </el-table-column>
                        </el-table>
                    </el-tab-pane>
                </el-tabs>
            </el-card>

        </template>

        <el-empty v-else description="暂无财务数据" />
    </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import type { EChartsOption } from 'echarts'
import type {
    BalanceSheetRow,
    CashflowStatementRow,
    FinanceIndicatorRow,
    FinancialBalanceStructureRow,
    FinancialCashflowStructureRow,
    FinancialProfitDisplayMode,
    FinancialProfitMetricCode,
    FinancialProfitTrendRow,
    FinancialQuarterCode,
    FinancialRatioMetricCode,
    FinancialRatioTrendRow,
    FinancialReportInfo,
    FinancialReportTypeCode,
    IncomeStatementRow,
    TargetFinancialDetail
} from '/@/types/targetdetail/financialDetailType'
import { queryTargetFinancialDetail } from '/@/api/targetdetail/financialDetailApi'
import TargetEcharts from '/@/components/common/TargetEcharts.vue'
import { formatAmount, formatNumber, formatPercent, signedClass } from '/@/utils/marketUtils'

const props = withDefaults(defineProps<TargetFinancialDetail>(), {
    financialDetail: () => ({}),
    meta: () => ({})
})

const fetched = ref<TargetFinancialDetail | null>(null)
const state = computed<TargetFinancialDetail>(() => fetched.value ?? props)
const financial = computed(() => state.value.financial)
const financialDetail = computed(() => state.value.financialDetail)

onMounted(() => {
    if (props.fullSymbol) {
        void loadFinancialDetail()
    }
})

/** 加载财务摘要、三大报表与财务指标明细。 */
async function loadFinancialDetail(): Promise<void> {
    try {
        fetched.value = await queryTargetFinancialDetail({
            fullSymbol: props.fullSymbol as string,
            historyLimit: 5
        })
    } catch (error) {
        console.error('加载财务详情数据失败', error)
    }
}

type StatementTabName = 'income' | 'balance' | 'cashflow' | 'indicator'

type ProfitMetricOption = {
    label: string
    value: FinancialProfitMetricCode
    field: keyof FinancialProfitTrendRow
    yoyField: keyof FinancialProfitTrendRow
    unitType: 'amount' | 'number'
}

type RatioMetricOption = {
    label: string
    value: FinancialRatioMetricCode
    field: keyof FinancialRatioTrendRow
}

type ProfitYearChartRow = {
    year: string
    q1Value?: number | null
    q2Value?: number | null
    q3Value?: number | null
    q4Value?: number | null
    q1Yoy?: number | null
    q2Yoy?: number | null
    q3Yoy?: number | null
    q4Yoy?: number | null
    totalValue?: number | null
    totalYoy?: number | null
}

const activeStatementTab = ref<StatementTabName>('income')
const activeProfitMetric = ref<FinancialProfitMetricCode>('TOTAL_REVENUE')
const profitDisplayMode = ref<FinancialProfitDisplayMode>('ALL')
const activeRatioMetric = ref<FinancialRatioMetricCode>('ROE')

const profitMetricOptions: ProfitMetricOption[] = [
    {
        label: '营业总收入',
        value: 'TOTAL_REVENUE',
        field: 'totalRevenue',
        yoyField: 'totalRevenueYoy',
        unitType: 'amount'
    },
    {
        label: '归母净利润',
        value: 'N_INCOME_ATTR_P',
        field: 'nIncomeAttrP',
        yoyField: 'nIncomeAttrPYoy',
        unitType: 'amount'
    },
    {
        label: '扣非净利润',
        value: 'PROFIT_DEDT',
        field: 'profitDedt',
        yoyField: 'profitDedtYoy',
        unitType: 'amount'
    },
    {
        label: '每股收益',
        value: 'EPS',
        field: 'eps',
        yoyField: 'epsYoy',
        unitType: 'number'
    }
]

const ratioMetricOptions: RatioMetricOption[] = [
    { label: 'ROE', value: 'ROE', field: 'roe' },
    { label: '毛利率', value: 'GROSS_MARGIN', field: 'grossMargin' },
    { label: '净利率', value: 'NETPROFIT_MARGIN', field: 'netprofitMargin' },
    { label: '资产负债率', value: 'DEBT_TO_ASSETS', field: 'debtToAssets' }
]

const incomeRows = computed<IncomeStatementRow[]>(() => sortByEndDate(financialDetail.value?.incomeRows || []))
const balanceSheetRows = computed<BalanceSheetRow[]>(() => sortByEndDate(financialDetail.value?.balanceSheetRows || []))
const cashflowRows = computed<CashflowStatementRow[]>(() => sortByEndDate(financialDetail.value?.cashflowRows || []))
const indicatorRows = computed<FinanceIndicatorRow[]>(() => sortByEndDate(financialDetail.value?.indicatorRows || []))

const latestIncome = computed(() => incomeRows.value[incomeRows.value.length - 1])
const latestCashflow = computed(() => cashflowRows.value[cashflowRows.value.length - 1])
const latestIndicator = computed(() => indicatorRows.value[indicatorRows.value.length - 1])
const latestBalance = computed(() => balanceSheetRows.value[balanceSheetRows.value.length - 1])

const hasFinancialDetail = computed(() => Boolean(
    incomeRows.value.length ||
    balanceSheetRows.value.length ||
    cashflowRows.value.length ||
    indicatorRows.value.length ||
    financialDetail.value?.profitTrendRows?.length ||
    financialDetail.value?.ratioTrendRows?.length ||
    financialDetail.value?.cashflowStructureRows?.length ||
    financialDetail.value?.balanceStructureRows?.length
))

const latestReportInfo = computed<FinancialReportInfo>(() => {
    const reportInfo = financialDetail.value?.reportInfo
    if (reportInfo) return reportInfo

    const latestEndDate = latestIncome.value?.endDate ||
        latestIndicator.value?.endDate ||
        latestCashflow.value?.endDate ||
        latestBalance.value?.endDate ||
        financial.value?.endDate ||
        null

    const sourceRow = latestIncome.value ||
        latestCashflow.value ||
        latestBalance.value ||
        latestIndicator.value

    return {
        fullSymbol: sourceRow?.fullSymbol,
        endDate: latestEndDate,
        annDate: financial.value?.annDate || sourceRow?.annDate || null,
        fAnnDate: financial.value?.fAnnDate || ('fAnnDate' in (sourceRow || {}) ? (sourceRow as IncomeStatementRow | BalanceSheetRow | CashflowStatementRow).fAnnDate : null),
        reportType: financial.value?.reportType || ('reportType' in (sourceRow || {}) ? (sourceRow as IncomeStatementRow | BalanceSheetRow | CashflowStatementRow).reportType : null),
        reportTypeCode: getReportTypeCode(latestEndDate),
        reportTypeText: financial.value?.reportTypeText || getReportTypeText(latestEndDate)
    }
})

const summaryCards = computed(() => [
    {
        label: '营业收入',
        value: formatAmount(latestIncome.value?.revenue ?? financial.value?.revenue),
        desc: `营收同比：${formatPercent(latestIndicator.value?.revenueYoy ?? financial.value?.revenueYoy, 2, true)}`,
        className: ''
    },
    {
        label: '归母净利润',
        value: formatAmount(latestIncome.value?.nIncomeAttrP ?? financial.value?.nIncomeAttrP),
        desc: `净利润同比：${formatPercent(latestIndicator.value?.netprofitYoy ?? financial.value?.netprofitYoy, 2, true)}`,
        className: signedClass(latestIndicator.value?.netprofitYoy ?? financial.value?.netprofitYoy)
    },
    {
        label: '经营现金流',
        value: formatAmount(latestCashflow.value?.nCashflowAct ?? financial.value?.nCashflowAct),
        desc: `自由现金流：${formatAmount(latestCashflow.value?.freeCashflow ?? financial.value?.freeCashflow)}`,
        className: signedClass(latestCashflow.value?.nCashflowAct ?? financial.value?.nCashflowAct)
    },
    {
        label: 'ROE / 资产负债率',
        value: `${formatPercent(latestIndicator.value?.roe ?? financial.value?.roe)} / ${formatPercent(latestIndicator.value?.debtToAssets ?? financial.value?.debtToAssets)}`,
        desc: `EPS：${formatNumber(latestIndicator.value?.eps ?? financial.value?.eps, 2)}`,
        className: ''
    },
    {
        label: '最新财报发布日期',
        value: latestReportInfo.value.annDate || latestReportInfo.value.fAnnDate || '-',
        desc: `报告期：${latestReportInfo.value.endDate || '-'}`,
        className: ''
    },
    {
        label: '财报类型',
        value: latestReportInfo.value.reportTypeText || '-',
        desc: latestReportInfo.value.reportType ? `reportType：${latestReportInfo.value.reportType}` : '按报告期自动识别',
        className: ''
    }
])

const profitTrendRows = computed<FinancialProfitTrendRow[]>(() => {
    if (financialDetail.value?.profitTrendRows?.length) {
        return sortByEndDate(financialDetail.value?.profitTrendRows || [])
    }

    return buildProfitTrendRows()
})

const ratioTrendRows = computed<FinancialRatioTrendRow[]>(() => {
    if (financialDetail.value?.ratioTrendRows?.length) {
        return sortByEndDate(financialDetail.value?.ratioTrendRows || [])
    }

    return indicatorRows.value.map(row => ({
        fullSymbol: row.fullSymbol,
        endDate: row.endDate,
        reportTypeText: getReportTypeText(row.endDate),
        roe: row.roe,
        grossMargin: row.grossMargin,
        netprofitMargin: row.netprofitMargin,
        debtToAssets: row.debtToAssets
    }))
})

const cashflowChartRows = computed<FinancialCashflowStructureRow[]>(() => {
    if (financialDetail.value?.cashflowStructureRows?.length) {
        return sortByEndDate(financialDetail.value?.cashflowStructureRows || [])
    }

    return buildStandaloneQuarterRows(cashflowRows.value, row => ({
        fullSymbol: row.fullSymbol,
        endDate: row.endDate,
        year: getYear(row.endDate),
        quarterCode: getQuarterCode(row.endDate),
        nCashflowAct: row.nCashflowAct,
        nCashflowInvAct: row.nCashflowInvAct,
        nCashFlowsFncAct: row.nCashFlowsFncAct,
        freeCashflow: row.freeCashflow
    }), ['nCashflowAct', 'nCashflowInvAct', 'nCashFlowsFncAct', 'freeCashflow'])
})

const balanceChartRows = computed<FinancialBalanceStructureRow[]>(() => {
    if (financialDetail.value?.balanceStructureRows?.length) {
        return sortByEndDate(financialDetail.value?.balanceStructureRows || [])
    }

    return balanceSheetRows.value.map(row => {
        const indicator = findByEndDate(indicatorRows.value, row.endDate)
        const totalNca = normalizeNumber(row.totalNca) ?? subtract(row.totalAssets, row.totalCurAssets)
        const totalNcl = normalizeNumber(row.totalNcl) ?? subtract(row.totalLiab, row.totalCurLiab)

        return {
            fullSymbol: row.fullSymbol,
            endDate: row.endDate,
            totalCurAssets: row.totalCurAssets,
            totalNca,
            totalAssets: row.totalAssets,
            totalCurLiab: row.totalCurLiab,
            totalNcl,
            totalLiab: row.totalLiab,
            totalHldrEqyExcMinInt: row.totalHldrEqyExcMinInt,
            debtToAssets: indicator?.debtToAssets ?? calcPercent(row.totalLiab, row.totalAssets)
        }
    })
})

const activeProfitMetricConfig = computed(() => (
    profitMetricOptions.find(item => item.value === activeProfitMetric.value) || profitMetricOptions[0]
))

const activeRatioMetricConfig = computed(() => (
    ratioMetricOptions.find(item => item.value === activeRatioMetric.value) || ratioMetricOptions[0]
))

const profitChartRows = computed<ProfitYearChartRow[]>(() => buildProfitYearChartRows(profitTrendRows.value, activeProfitMetricConfig.value))

const profitTrendOption = computed<EChartsOption>(() => {
    const config = activeProfitMetricConfig.value
    const rows = profitChartRows.value
    const xAxisData = rows.map(row => row.year)
    const amountFormatter = config.unitType === 'amount' ? formatAmount : (value?: number | null) => formatNumber(value, 2)

    const barSeries = (['q1Value', 'q2Value', 'q3Value', 'q4Value'] as const).map((field, index) => {
        const quarterName = `Q${index + 1}`
        return {
            name: quarterName,
            type: 'bar' as const,
            stack: profitDisplayMode.value === 'ALL' ? 'total' : undefined,
            barMaxWidth: 34,
            itemStyle: profitDisplayMode.value === 'SINGLE_QUARTER' ? { color: '#409EFF' } : undefined,
            label: {
                show: profitDisplayMode.value === 'ALL' ? index === 3 : true,
                position: 'top' as const,
                formatter(params: any) {
                    if (profitDisplayMode.value === 'ALL') {
                        return amountFormatter(params.data?.totalValue)
                    }

                    return amountFormatter(params.data?.value)
                }
            },
            data: rows.map(row => ({
                value: normalizeNumber(row[field]) || 0,
                totalValue: normalizeNumber(row.totalValue) || 0,
                yoy: normalizeNumber(row[`${field.replace('Value', 'Yoy')}` as keyof ProfitYearChartRow]) || 0
            }))
        }
    })

    return {
        tooltip: {
            trigger: 'axis',
            axisPointer: { type: 'shadow' as const },
            formatter(params: any) {
                const items = Array.isArray(params) ? params : [params]
                const year = items[0]?.axisValue || ''
                const row = rows.find(item => item.year === String(year))
                if (!row) return year

                return [
                    `${year}年`,
                    `合计：${amountFormatter(row.totalValue)}`,
                    `总体同比：${formatPercent(row.totalYoy, 2, true)}`,
                    `Q1：${amountFormatter(row.q1Value)} / 同比 ${formatPercent(row.q1Yoy, 2, true)}`,
                    `Q2：${amountFormatter(row.q2Value)} / 同比 ${formatPercent(row.q2Yoy, 2, true)}`,
                    `Q3：${amountFormatter(row.q3Value)} / 同比 ${formatPercent(row.q3Yoy, 2, true)}`,
                    `Q4：${amountFormatter(row.q4Value)} / 同比 ${formatPercent(row.q4Yoy, 2, true)}`
                ].join('<br/>')
            }
        },
        legend: { top: 0, left: 8 },
        grid: { left: 58, right: 58, top: 52, bottom: 42 },
        xAxis: {
            type: 'category' as const,
            data: xAxisData,
            axisLabel: { hideOverlap: true }
        },
        yAxis: [
            {
                type: 'value' as const,
                name: config.unitType === 'amount' ? '金额' : '数值',
                axisLabel: {
                    formatter(value: number) {
                        return config.unitType === 'amount' ? compactAmount(value) : formatNumber(value, 2)
                    }
                },
                splitLine: { lineStyle: { type: 'dashed' } }
            },
            {
                type: 'value' as const,
                name: '同比',
                axisLabel: {
                    formatter(value: number) {
                        return `${value}%`
                    }
                }
            }
        ],
        series: [
            ...barSeries,
            {
                name: '总体同比增长率',
                type: 'line' as const,
                yAxisIndex: 1,
                smooth: true,
                showSymbol: true,
                data: rows.map(row => normalizeNumber(row.totalYoy) || 0)
            }
        ]
    }
})

const ratioTrendOption = computed<EChartsOption>(() => {
    const config = activeRatioMetricConfig.value
    const rows = ratioTrendRows.value

    return {
        tooltip: {
            trigger: 'axis',
            axisPointer: { type: 'shadow' as const },
            formatter(params: any) {
                const item = Array.isArray(params) ? params[0] : params
                return `${item.axisValue}<br/>${item.marker}${config.label}：${formatPercent(item.data?.value)}`
            }
        },
        grid: { left: 58, right: 24, top: 36, bottom: 42 },
        xAxis: {
            type: 'category' as const,
            data: rows.map(row => row.endDate),
            axisLabel: { hideOverlap: true }
        },
        yAxis: {
            type: 'value' as const,
            name: '%',
            axisLabel: {
                formatter(value: number) {
                    return `${value}%`
                }
            },
            splitLine: { lineStyle: { type: 'dashed' } }
        },
        series: [
            {
                name: config.label,
                type: 'bar' as const,
                barMaxWidth: 34,
                label: {
                    show: true,
                    position: 'top',
                    formatter(params: any) {
                        return formatPercent(params.data?.value)
                    }
                },
                data: rows.map(row => ({
                    value: normalizeNumber(row[config.field]) || 0
                }))
            }
        ]
    }
})

const cashflowStructureOption = computed<EChartsOption>(() => {
    const rows = cashflowChartRows.value

    return {
        tooltip: {
            trigger: 'axis',
            axisPointer: { type: 'shadow' },
            formatter(params: any) {
                const items = Array.isArray(params) ? params : [params]
                const date = items[0]?.axisValue || ''
                const row = rows.find(item => item.endDate === date)
                if (!row) return date

                return [
                    `${date}（${row.quarterCode || '-'}）`,
                    `经营现金流：${formatAmount(row.nCashflowAct)}`,
                    `投资现金流：${formatAmount(row.nCashflowInvAct)}`,
                    `筹资现金流：${formatAmount(row.nCashFlowsFncAct)}`,
                    `自由现金流：${formatAmount(row.freeCashflow)}`
                ].join('<br/>')
            }
        },
        legend: { top: 0, left: 8 },
        grid: { left: 58, right: 24, top: 52, bottom: 42 },
        xAxis: {
            type: 'category' as const,
            data: rows.map(row => row.endDate),
            axisLabel: { hideOverlap: true }
        },
        yAxis: {
            type: 'value' as const,
            axisLabel: {
                formatter(value: number) {
                    return compactAmount(value)
                }
            },
            splitLine: { lineStyle: { type: 'dashed' } }
        },
        series: [
            buildCashflowBarSeries('经营现金流', rows.map(row => row.nCashflowAct)),
            buildCashflowBarSeries('投资现金流', rows.map(row => row.nCashflowInvAct)),
            buildCashflowBarSeries('筹资现金流', rows.map(row => row.nCashFlowsFncAct)),
            buildCashflowBarSeries('自由现金流', rows.map(row => row.freeCashflow))
        ]
    }
})

const balanceStructureOption = computed<EChartsOption>(() => {
    const rows = balanceChartRows.value

    return {
        tooltip: {
            trigger: 'axis',
            axisPointer: { type: 'shadow' as const },
            formatter(params: any) {
                const items = Array.isArray(params) ? params : [params]
                const date = items[0]?.axisValue || ''
                const row = rows.find(item => item.endDate === date)
                if (!row) return date

                return [
                    date,
                    `总资产：${formatAmount(row.totalAssets)}`,
                    `总负债：${formatAmount(row.totalLiab)}`,
                    `归母权益：${formatAmount(row.totalHldrEqyExcMinInt)}`,
                    `资产负债率：${formatPercent(row.debtToAssets)}`
                ].join('<br/>')
            }
        },
        legend: { top: 0, left: 8 },
        grid: { left: 58, right: 58, top: 52, bottom: 42 },
        xAxis: {
            type: 'category' as const,
            data: rows.map(row => row.endDate),
            axisLabel: { hideOverlap: true }
        },
        yAxis: [
            {
                type: 'value' as const,
                name: '金额',
                axisLabel: {
                    formatter(value: number) {
                        return compactAmount(value)
                    }
                },
                splitLine: { lineStyle: { type: 'dashed' } }
            },
            {
                type: 'value' as const,
                name: '资产负债率',
                axisLabel: {
                    formatter(value: number) {
                        return `${value}%`
                    }
                }
            }
        ],
        series: [
            {
                name: '流动资产',
                type: 'bar' as const,
                stack: '资产',
                barMaxWidth: 28,
                data: rows.map(row => normalizeNumber(row.totalCurAssets) || 0)
            },
            {
                name: '非流动资产',
                type: 'bar' as const,
                stack: '资产',
                barMaxWidth: 28,
                data: rows.map(row => normalizeNumber(row.totalNca) || 0)
            },
            {
                name: '流动负债',
                type: 'bar' as const,
                stack: '负债',
                barMaxWidth: 28,
                data: rows.map(row => normalizeNumber(row.totalCurLiab) || 0)
            },
            {
                name: '非流动负债',
                type: 'bar' as const,
                stack: '负债',
                barMaxWidth: 28,
                data: rows.map(row => normalizeNumber(row.totalNcl) || 0)
            },
            {
                name: '归母权益',
                type: 'bar' as const,
                stack: '权益',
                barMaxWidth: 28,
                data: rows.map(row => normalizeNumber(row.totalHldrEqyExcMinInt) || 0)
            },
            {
                name: '资产负债率',
                type: 'line' as const,
                yAxisIndex: 1,
                smooth: true,
                showSymbol: true,
                data: rows.map(row => normalizeNumber(row.debtToAssets) || 0)
            }
        ]
    }
})

function buildCashflowBarSeries(name: string, values: Array<number | null | undefined>) {
    return {
        name,
        type: 'bar' as const,
        barMaxWidth: 24,
        label: { show: false },
        data: values.map(value => normalizeNumber(value) || 0)
    }
}

function buildProfitTrendRows(): FinancialProfitTrendRow[] {
    const incomeByEndDate = new Map(incomeRows.value.map(row => [row.endDate, row]))
    const indicatorByEndDate = new Map(indicatorRows.value.map(row => [row.endDate, row]))
    const endDates = Array.from(new Set([...incomeByEndDate.keys(), ...indicatorByEndDate.keys()]))
        .sort((a, b) => String(a).localeCompare(String(b)))

    const cumulativeRows = endDates.map(endDate => {
        const income = incomeByEndDate.get(endDate)
        const indicator = indicatorByEndDate.get(endDate)

        return {
            fullSymbol: income?.fullSymbol || indicator?.fullSymbol || '',
            endDate,
            annDate: income?.annDate || indicator?.annDate || null,
            year: getYear(endDate),
            quarterCode: getQuarterCode(endDate),
            reportTypeText: getReportTypeText(endDate),
            totalRevenue: income?.totalRevenue,
            totalRevenueYoy: indicator?.revenueYoy,
            nIncomeAttrP: income?.nIncomeAttrP,
            nIncomeAttrPYoy: indicator?.netprofitYoy,
            profitDedt: indicator?.profitDedt,
            profitDedtYoy: indicator?.dtNetprofitYoy,
            eps: indicator?.eps ?? income?.basicEps,
            epsYoy: null
        } as FinancialProfitTrendRow
    })

    return fillYoyForEps(cumulativeRows)
}

function buildProfitYearChartRows(rows: FinancialProfitTrendRow[], config: ProfitMetricOption): ProfitYearChartRow[] {
    const standaloneRows = buildStandaloneQuarterRows(rows, row => row, [config.field])
    const rowsByYear = new Map<string, ProfitYearChartRow>()

    standaloneRows.forEach(row => {
        const year = String(row.year || getYear(row.endDate) || '-')
        const quarter = row.quarterCode || getQuarterCode(row.endDate)
        const value = normalizeNumber(row[config.field])
        const yoy = findQuarterYoy(standaloneRows, row, config.field)

        if (!rowsByYear.has(year)) {
            rowsByYear.set(year, { year })
        }

        const yearRow = rowsByYear.get(year)!
        if (quarter === 'Q1') {
            yearRow.q1Value = value
            yearRow.q1Yoy = yoy
        }
        if (quarter === 'Q2') {
            yearRow.q2Value = value
            yearRow.q2Yoy = yoy
        }
        if (quarter === 'Q3') {
            yearRow.q3Value = value
            yearRow.q3Yoy = yoy
        }
        if (quarter === 'Q4') {
            yearRow.q4Value = value
            yearRow.q4Yoy = yoy
        }
    })

    const result = Array.from(rowsByYear.values())
        .sort((a, b) => a.year.localeCompare(b.year))
        .map(row => ({
            ...row,
            totalValue: sumValues([row.q1Value, row.q2Value, row.q3Value, row.q4Value])
        }))

    result.forEach((row, index) => {
        const previous = result[index - 1]
        row.totalYoy = calcYoy(row.totalValue, previous?.totalValue)
    })

    return result
}

function buildStandaloneQuarterRows<T extends { endDate: string; year?: number | null; quarterCode?: FinancialQuarterCode | null }>(
    rows: T[],
    mapper: (row: T) => T,
    valueFields: Array<keyof T>
): T[] {
    const mappedRows = sortByEndDate(rows.map(mapper))
    const rowsByYear = new Map<number, T[]>()

    mappedRows.forEach(row => {
        const year = row.year || getYear(row.endDate)
        if (!year) return
        if (!rowsByYear.has(year)) rowsByYear.set(year, [])
        rowsByYear.get(year)!.push(row)
    })

    const result: T[] = []

    rowsByYear.forEach(yearRows => {
        const sortedRows = sortByEndDate(yearRows)
        let previousRow: T | null = null

        sortedRows.forEach(row => {
            const standaloneRow = { ...row }
            valueFields.forEach(field => {
                const currentValue = normalizeNumber(row[field])
                const previousValue = previousRow ? normalizeNumber(previousRow[field]) : null

                ;(standaloneRow as any)[field] = currentValue === null
                    ? null
                    : previousValue === null
                        ? currentValue
                        : currentValue - previousValue
            })
            result.push(standaloneRow)
            previousRow = row
        })
    })

    return sortByEndDate(result)
}

function fillYoyForEps(rows: FinancialProfitTrendRow[]): FinancialProfitTrendRow[] {
    return rows.map(row => {
        const previous = rows.find(item => item.endDate === shiftYear(row.endDate, -1))
        return {
            ...row,
            epsYoy: calcYoy(row.eps, previous?.eps)
        }
    })
}

function findQuarterYoy<T extends { endDate: string; year?: number | null; quarterCode?: FinancialQuarterCode | null }>(
    rows: T[],
    currentRow: T,
    field: keyof T
): number | null {
    const currentYear = currentRow.year || getYear(currentRow.endDate)
    const quarterCode = currentRow.quarterCode || getQuarterCode(currentRow.endDate)
    const previous = rows.find(row => (row.year || getYear(row.endDate)) === (currentYear || 0) - 1 &&
        (row.quarterCode || getQuarterCode(row.endDate)) === quarterCode)

    return calcYoy(normalizeNumber(currentRow[field]), previous ? normalizeNumber(previous[field]) : null)
}

function findByEndDate<T extends { endDate: string }>(rows: T[], endDate: string): T | undefined {
    return rows.find(row => row.endDate === endDate)
}

function sortByEndDate<T extends { endDate?: string | null }>(rows: T[] = []): T[] {
    return rows.slice().sort((a, b) => String(a.endDate || '').localeCompare(String(b.endDate || '')))
}

function getYear(endDate?: string | null): number | null {
    if (!endDate) return null
    const year = Number(String(endDate).slice(0, 4))
    return Number.isNaN(year) ? null : year
}

function getQuarterCode(endDate?: string | null): FinancialQuarterCode | null {
    if (!endDate) return null
    const normalizedDate = String(endDate)
    const month = normalizedDate.includes('-')
        ? Number(normalizedDate.slice(5, 7))
        : Number(normalizedDate.slice(4, 6))

    if (month <= 3) return 'Q1'
    if (month <= 6) return 'Q2'
    if (month <= 9) return 'Q3'
    return 'Q4'
}

function getReportTypeCode(endDate?: string | null): FinancialReportTypeCode | null {
    const quarterCode = getQuarterCode(endDate)
    if (quarterCode === 'Q1') return 'Q1'
    if (quarterCode === 'Q2') return 'HALF_YEAR'
    if (quarterCode === 'Q3') return 'Q3'
    if (quarterCode === 'Q4') return 'ANNUAL'
    return null
}

function getReportTypeText(endDate?: string | null): string {
    const quarterCode = getQuarterCode(endDate)
    if (quarterCode === 'Q1') return '一季报'
    if (quarterCode === 'Q2') return '半年报'
    if (quarterCode === 'Q3') return '三季报'
    if (quarterCode === 'Q4') return '年报'
    return '-'
}

function shiftYear(endDate: string, yearOffset: number): string {
    const year = Number(endDate.slice(0, 4))
    if (Number.isNaN(year)) return endDate
    return `${year + yearOffset}${endDate.slice(4)}`
}

function calcYoy(current?: number | null, previous?: number | null): number | null {
    const currentValue = normalizeNumber(current)
    const previousValue = normalizeNumber(previous)

    if (currentValue === null || previousValue === null || previousValue === 0) return null

    return ((currentValue - previousValue) / Math.abs(previousValue)) * 100
}

function calcPercent(numerator?: number | null, denominator?: number | null): number | null {
    const numeratorValue = normalizeNumber(numerator)
    const denominatorValue = normalizeNumber(denominator)

    if (numeratorValue === null || denominatorValue === null || denominatorValue === 0) return null

    return (numeratorValue / denominatorValue) * 100
}

function subtract(value?: number | null, subtrahend?: number | null): number | null {
    const firstValue = normalizeNumber(value)
    const secondValue = normalizeNumber(subtrahend)

    if (firstValue === null || secondValue === null) return null

    return firstValue - secondValue
}

function sumValues(values: Array<number | null | undefined>): number | null {
    const validValues = values.map(normalizeNumber).filter((value): value is number => value !== null)

    if (!validValues.length) return null

    return validValues.reduce((sum, value) => sum + value, 0)
}

function normalizeNumber(value: unknown): number | null {
    if (value === undefined || value === null || value === '') return null
    const numberValue = Number(value)
    return Number.isNaN(numberValue) ? null : numberValue
}

function compactAmount(value?: number | null): string {
    const numberValue = normalizeNumber(value)

    if (numberValue === null) return '-'
    if (Math.abs(numberValue) >= 100000000) return `${formatNumber(numberValue / 100000000, 1)}亿`
    if (Math.abs(numberValue) >= 10000) return `${formatNumber(numberValue / 10000, 1)}万`

    return formatNumber(numberValue, 0)
}
</script>

<style scoped>
.financial-detail-page {
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.metric-card,
.detail-card {
    border-radius: 10px;
}

.metric-card {
    height: 118px;
}

.metric-label,
.metric-desc,
.card-tip {
    color: var(--el-text-color-secondary);
    font-size: 12px;
}

.metric-value {
    margin-top: 10px;
    font-size: 20px;
    font-weight: 700;
    line-height: 1.2;
}

.metric-desc {
    margin-top: 8px;
}

.card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
}

.card-title {
    margin-right: 8px;
    font-weight: 600;
    color: var(--el-text-color-primary);
}

.metric-tabs {
    margin-bottom: 8px;
}

.conclusion {
    margin-bottom: 0;
}

.is-up {
    color: var(--el-color-danger);
}

.is-down {
    color: var(--el-color-success);
}

.is-flat {
    color: var(--el-text-color-regular);
}

@media (max-width: 768px) {
    .card-header {
        align-items: flex-start;
        flex-direction: column;
    }
}
</style>