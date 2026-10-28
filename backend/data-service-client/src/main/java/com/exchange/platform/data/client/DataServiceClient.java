package com.exchange.platform.data.client;

import java.util.List;
import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.exchange.platform.data.client.dto.TaskExecuteResult;
import com.exchange.platform.data.client.dto.TaskRunExecuteRequest;
import com.exchange.platform.data.client.dto.TaskTemplateSync;

/**
 * data-service-client 最小 OpenFeign 客户端。
 * 业务服务直接注入该接口即可请求 FastAPI 任务处理接口。
 */
@FeignClient(
    name = "${task-handler.service-name:task-handler}",
    contextId = "dataServiceClient",
    url = "${task-handler.base-url:http://localhost:8000}",
    path = "/api/v1"
)
public interface DataServiceClient {

    /**
     * GET /api/v1/collection-task/task-templates/current?taskCode=xxx
     */
    @GetMapping("/collection-task/task-templates/current")
    List<TaskTemplateSync> fetchCurrentTaskTemplates(
        @RequestParam(value = "taskCode", required = false) String taskCode
    );

    /**
     * POST /api/v1/collection-task/task-runs/execute
     */
    @PostMapping("/collection-task/task-runs/execute")
    TaskExecuteResult executeTaskRun(@RequestBody TaskRunExecuteRequest executeRequest);

    /**
     * GET /api/v1/data-analytics/market-overview?tradeDate=xxx
     */
    @GetMapping("/data-analytics/market-overview")
    Map<String, Object> getMarketOverview(
        @RequestParam(value = "tradeDate", required = false) String tradeDate
    );

    /**
     * GET /api/v1/data-analytics/stock-detail?fullSymbol=xxx&tradeDate=xxx
     *
     * 标的详情首屏轻量数据：标的基础信息、最新行情、指标卡与各模块摘要。
     */
    @GetMapping("/data-analytics/stock-detail")
    Map<String, Object> getTargetDetailShell(
        @RequestParam(value = "fullSymbol") String fullSymbol,
        @RequestParam(value = "tradeDate", required = false) String tradeDate
    );

    /**
     * GET /api/v1/data-analytics/stock-detail/overview?fullSymbol=xxx&tradeDate=xxx
     *
     * 标的综合概览：综合观点、风险等级、估值状态、理由与建议。
     */
    @GetMapping("/data-analytics/stock-detail/overview")
    Map<String, Object> getTargetOverviewDetail(
        @RequestParam(value = "fullSymbol") String fullSymbol,
        @RequestParam(value = "tradeDate", required = false) String tradeDate
    );

    /**
     * GET /api/v1/data-analytics/stock-detail/quote?fullSymbol=xxx&tradeDate=xxx&rangeCode=xxx&adjustType=xxx
     *
     * 行情详情：K 线、技术指标图表、技术分析与区间表现。
     */
    @GetMapping("/data-analytics/stock-detail/quote")
    Map<String, Object> getTargetQuoteDetail(
        @RequestParam(value = "fullSymbol") String fullSymbol,
        @RequestParam(value = "tradeDate", required = false) String tradeDate,
        @RequestParam(value = "rangeCode", required = false) String rangeCode,
        @RequestParam(value = "adjustType", required = false, defaultValue = "NONE") String adjustType,
        @RequestParam(value = "benchmarkCode", required = false) String benchmarkCode,
        @RequestParam(value = "industryCode", required = false) String industryCode
    );

    /**
     * GET /api/v1/data-analytics/stock-detail/valuation-summary?fullSymbol=xxx&tradeDate=xxx
     *
     * 估值摘要：PE/PB/PS/股息率与总市值。
     */
    @GetMapping("/data-analytics/stock-detail/valuation-summary")
    Map<String, Object> getTargetValuationSummary(
        @RequestParam(value = "fullSymbol") String fullSymbol,
        @RequestParam(value = "tradeDate", required = false) String tradeDate,
        @RequestParam(value = "historyLimit", required = false) Integer historyLimit,
        @RequestParam(value = "rangeCode", required = false) String rangeCode
    );

    /**
     * GET /api/v1/data-analytics/stock-detail/valuation-channel?fullSymbol=xxx&metricCode=xxx
     *
     * 估值通道：收盘价与 5 条估值通道线。
     */
    @GetMapping("/data-analytics/stock-detail/valuation-channel")
    Map<String, Object> getTargetValuationChannel(
        @RequestParam(value = "fullSymbol") String fullSymbol,
        @RequestParam(value = "tradeDate", required = false) String tradeDate,
        @RequestParam(value = "metricCode", required = false, defaultValue = "PE_TTM") String metricCode,
        @RequestParam(value = "historyLimit", required = false) Integer historyLimit,
        @RequestParam(value = "rangeCode", required = false) String rangeCode
    );

    /**
     * GET /api/v1/data-analytics/stock-detail/valuation-trend?fullSymbol=xxx&metricCode=xxx
     *
     * 估值走势：股票估值、行业平均/中位数与 30/70 分位。
     */
    @GetMapping("/data-analytics/stock-detail/valuation-trend")
    Map<String, Object> getTargetValuationTrend(
        @RequestParam(value = "fullSymbol") String fullSymbol,
        @RequestParam(value = "tradeDate", required = false) String tradeDate,
        @RequestParam(value = "metricCode", required = false, defaultValue = "PE_TTM") String metricCode,
        @RequestParam(value = "historyLimit", required = false) Integer historyLimit,
        @RequestParam(value = "rangeCode", required = false) String rangeCode
    );

    /**
     * GET /api/v1/data-analytics/stock-detail/dividend-trend?fullSymbol=xxx&tradeDate=xxx
     *
     * 分红走势：股票分红 TTM、行业平均与中位数分红率。
     */
    @GetMapping("/data-analytics/stock-detail/dividend-trend")
    Map<String, Object> getTargetDividendTrend(
        @RequestParam(value = "fullSymbol") String fullSymbol,
        @RequestParam(value = "tradeDate", required = false) String tradeDate,
        @RequestParam(value = "historyLimit", required = false) Integer historyLimit,
        @RequestParam(value = "rangeCode", required = false) String rangeCode
    );

    /**
     * GET /api/v1/data-analytics/stock-detail/financial?fullSymbol=xxx&historyLimit=xxx
     *
     * 财务详情：财务摘要、三大报表与财务指标明细。
     */
    @GetMapping("/data-analytics/stock-detail/financial")
    Map<String, Object> getTargetFinancialDetail(
        @RequestParam(value = "fullSymbol") String fullSymbol,
        @RequestParam(value = "historyLimit", required = false) Integer historyLimit
    );

    /**
     * GET /api/v1/data-analytics/stock-detail/capital-flow?fullSymbol=xxx&tradeDate=xxx
     *
     * 资金流向：资金流摘要、历史明细与价格联动序列。
     */
    @GetMapping("/data-analytics/stock-detail/capital-flow")
    Map<String, Object> getTargetCapitalFlowDetail(
        @RequestParam(value = "fullSymbol") String fullSymbol,
        @RequestParam(value = "tradeDate", required = false) String tradeDate,
        @RequestParam(value = "historyLimit", required = false) Integer historyLimit,
        @RequestParam(value = "rangeCode", required = false) String rangeCode,
        @RequestParam(value = "includeQuoteRows", required = false, defaultValue = "true") boolean includeQuoteRows
    );

    /**
     * GET /api/v1/data-analytics/stock-detail/industry?fullSymbol=xxx&tradeDate=xxx
     *
     * 行业详情：申万行业归属、所属指数、同业对比或指数成分。
     */
    @GetMapping("/data-analytics/stock-detail/industry")
    Map<String, Object> getTargetIndustryDetail(
        @RequestParam(value = "fullSymbol") String fullSymbol,
        @RequestParam(value = "tradeDate", required = false) String tradeDate
    );

    /**
     * GET /api/v1/data-analytics/index-detail?fullSymbol=xxx
     */
    @GetMapping("/data-analytics/index-detail")
    Map<String, Object> getIndexDetail(
        @RequestParam(value = "fullSymbol") String fullSymbol,
        @RequestParam(value = "tradeDate", required = false) String tradeDate
    );

    /**
     * GET /api/v1/data-analytics/index-detail/market?fullSymbol=xxx&tradeDate=xxx&historyLimit=xxx
     */
    @GetMapping("/data-analytics/index-detail/market")
    Map<String, Object> getIndexDetailMarket(
        @RequestParam(value = "fullSymbol") String fullSymbol,
        @RequestParam(value = "tradeDate", required = false) String tradeDate,
        @RequestParam(value = "historyLimit", required = false, defaultValue = "250") int historyLimit
    );

    /**
     * GET /api/v1/data-analytics/index-detail/constituents?fullSymbol=xxx&tradeDate=xxx
     */
    @GetMapping("/data-analytics/index-detail/constituents")
    Map<String, Object> getIndexConstituents(
        @RequestParam(value = "fullSymbol") String fullSymbol,
        @RequestParam(value = "tradeDate", required = false) String tradeDate
    );

    /**
     * GET /api/v1/data-analytics/index-detail/constituent-page?fullSymbol=xxx&tradeDate=xxx&page=xxx&pageSize=xxx
     */
    @GetMapping("/data-analytics/index-detail/constituent-page")
    List<Map<String, Object>> getIndexConstituentPage(
        @RequestParam(value = "fullSymbol") String fullSymbol,
        @RequestParam(value = "tradeDate", required = false) String tradeDate,
        @RequestParam(value = "page", required = false, defaultValue = "1") int page,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") int pageSize
    );
}
