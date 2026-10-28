package com.exchange.platform.analytics.controller;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exchange.platform.analytics.api.request.WatchTargetSaveRequest;
import com.exchange.platform.analytics.api.response.WatchTargetView;
import com.exchange.platform.analytics.service.WatchTargetService;
import com.exchange.platform.common.core.result.ApiResponse;
import com.exchange.platform.common.server.support.RequestIdHolder;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/api/v1/analytics/watch-targets")
public class WatchTargetController {

    private static final Logger log = LoggerFactory.getLogger(WatchTargetController.class);

    private final WatchTargetService watchTargetService;

    public WatchTargetController(WatchTargetService watchTargetService) {
        this.watchTargetService = watchTargetService;
    }

    @Operation(summary = "分页查询分组下关注标的", description = "根据标的分组编码、标的代码、标的名称、市场类型分页查询关注标的")
    @GetMapping
    public ApiResponse<Page<WatchTargetView>> queryWatchTargetPage(
        @RequestParam(required = false) String groupCode,
        @RequestParam(required = false) String targetCode,
        @RequestParam(required = false) String targetName,
        @RequestParam(required = false) String targetType,
        @RequestParam(required = false) String marketCode,
        @RequestParam(defaultValue = "1") long pageNo,
        @RequestParam(defaultValue = "20") long pageSize,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);
        log.info(
            "[{}] [Controller] queryWatchTargetPage: groupCode={}, targetCode={}, targetName={}, targetType={}, marketCode={}, pageNo={}, pageSize={}",
            requestId, groupCode, targetCode, targetName, targetType, marketCode, pageNo, pageSize
        );
        return ApiResponse.success(
            watchTargetService.queryWatchTargetPage(groupCode, targetCode, targetName, targetType, marketCode, pageNo, pageSize),
            requestId
        );
    }

    @Operation(summary = "保存或更新关注标的", description = "根据请求体中的targetCode创建或更新关注标的")
    @PostMapping
    public ApiResponse<WatchTargetView> saveOrUpdateWatchTarget(
        @RequestBody WatchTargetSaveRequest body,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);
        log.info("[{}] [Controller] saveOrUpdateWatchTarget: body={}", requestId, body);
        return ApiResponse.success(watchTargetService.saveOrUpdateWatchTarget(body), requestId);
    }

    @Operation(summary = "删除关注标的", description = "根据targetCode删除关注标的；传入groupCode时仅从指定分组移除")
    @DeleteMapping("/{targetCode}")
    public ApiResponse<Void> deleteWatchTarget(
        @PathVariable String targetCode,
        @RequestParam(required = false) String targetType,
        @RequestParam(required = false) String groupCode,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);
        log.info("[{}] [Controller] deleteWatchTarget: targetCode={}, targetType={}, groupCode={}", requestId, targetCode, targetType, groupCode);
        watchTargetService.deleteWatchTarget(targetCode, targetType, groupCode);
        return ApiResponse.success(null, requestId);
    }

    @Operation(summary = "获取关注标的基本信息，包括所属分组", description = "根据targetCode获取关注标的信息以及所在分组信息")
    @GetMapping("/{targetCode}/groups")
    public ApiResponse<WatchTargetView> getWatchTargetGroups(
        @PathVariable String targetCode,
        @RequestParam String targetType,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);
        log.info("[{}] [Controller] getWatchTargetGroups: targetCode={}, targetType={}", requestId, targetCode, targetType);
        return ApiResponse.success(watchTargetService.getWatchTargetGroups(targetCode, targetType), requestId);
    }

    @Operation(summary = "查询标的详情首屏数据", description = "返回详情页首屏所需的标的基础信息、最新行情、指标卡与各模块摘要，不包含大数组")
    @GetMapping("/{targetCode}/details")
    public ApiResponse<Map<String, Object>> getStockDetailShell(
        @PathVariable String targetCode,
        @RequestParam(required = false) String tradeDate,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);
        log.info("[{}] [Controller] getStockDetailShell: targetCode={}, tradeDate={}", requestId, targetCode, tradeDate);
        return ApiResponse.success(watchTargetService.getStockDetailShell(targetCode, tradeDate), requestId);
    }

    @Operation(summary = "查询标的综合概览", description = "返回综合分析结论，包括综合观点、风险等级、估值状态、理由、风险点和建议")
    @GetMapping("/{targetCode}/overview")
    public ApiResponse<Map<String, Object>> getTargetOverview(
        @PathVariable String targetCode,
        @RequestParam(required = false) String tradeDate,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);
        log.info("[{}] [Controller] getTargetOverview: targetCode={}, tradeDate={}", requestId, targetCode, tradeDate);
        return ApiResponse.success(watchTargetService.getTargetOverview(targetCode, tradeDate), requestId);
    }

    @Operation(summary = "查询行情详情", description = "返回复权后的 K 线、技术指标图表、技术指标综合分析与区间表现")
    @GetMapping("/{targetCode}/quote-detail")
    public ApiResponse<Map<String, Object>> getTargetQuoteDetail(
        @PathVariable String targetCode,
        @RequestParam(required = false) String tradeDate,
        @RequestParam(required = false) String rangeCode,
        @RequestParam(required = false, defaultValue = "NONE") String adjustType,
        @RequestParam(required = false) String benchmarkCode,
        @RequestParam(required = false) String industryCode,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);
        log.info(
            "[{}] [Controller] getTargetQuoteDetail: targetCode={}, tradeDate={}, rangeCode={}, adjustType={}, benchmarkCode={}, industryCode={}",
            requestId, targetCode, tradeDate, rangeCode, adjustType, benchmarkCode, industryCode
        );
        return ApiResponse.success(
            watchTargetService.getTargetQuoteDetail(targetCode, tradeDate, rangeCode, adjustType, benchmarkCode, industryCode),
            requestId
        );
    }

    @Operation(summary = "查询估值摘要", description = "返回估值 Tab 顶部 PE/PB/PS/股息率与总市值等摘要信息")
    @GetMapping("/{targetCode}/valuation-summary")
    public ApiResponse<Map<String, Object>> getTargetValuationSummary(
        @PathVariable String targetCode,
        @RequestParam(required = false) String tradeDate,
        @RequestParam(required = false) Integer historyLimit,
        @RequestParam(required = false) String rangeCode,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);
        log.info(
            "[{}] [Controller] getTargetValuationSummary: targetCode={}, tradeDate={}, historyLimit={}, rangeCode={}",
            requestId, targetCode, tradeDate, historyLimit, rangeCode
        );
        return ApiResponse.success(
            watchTargetService.getTargetValuationSummary(targetCode, tradeDate, historyLimit, rangeCode),
            requestId
        );
    }

    @Operation(summary = "查询估值通道", description = "返回指定估值指标的收盘价与 5 条估值通道线")
    @GetMapping("/{targetCode}/valuation-channel")
    public ApiResponse<Map<String, Object>> getTargetValuationChannel(
        @PathVariable String targetCode,
        @RequestParam(required = false) String tradeDate,
        @RequestParam(required = false, defaultValue = "PE_TTM") String metricCode,
        @RequestParam(required = false) Integer historyLimit,
        @RequestParam(required = false) String rangeCode,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);
        log.info(
            "[{}] [Controller] getTargetValuationChannel: targetCode={}, tradeDate={}, metricCode={}, historyLimit={}, rangeCode={}",
            requestId, targetCode, tradeDate, metricCode, historyLimit, rangeCode
        );
        return ApiResponse.success(
            watchTargetService.getTargetValuationChannel(targetCode, tradeDate, metricCode, historyLimit, rangeCode),
            requestId
        );
    }

    @Operation(summary = "查询估值走势", description = "返回指定估值指标的股票值、行业平均/中位数、30/70 分位与当前分位排名")
    @GetMapping("/{targetCode}/valuation-trend")
    public ApiResponse<Map<String, Object>> getTargetValuationTrend(
        @PathVariable String targetCode,
        @RequestParam(required = false) String tradeDate,
        @RequestParam(required = false, defaultValue = "PE_TTM") String metricCode,
        @RequestParam(required = false) Integer historyLimit,
        @RequestParam(required = false) String rangeCode,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);
        log.info(
            "[{}] [Controller] getTargetValuationTrend: targetCode={}, tradeDate={}, metricCode={}, historyLimit={}, rangeCode={}",
            requestId, targetCode, tradeDate, metricCode, historyLimit, rangeCode
        );
        return ApiResponse.success(
            watchTargetService.getTargetValuationTrend(targetCode, tradeDate, metricCode, historyLimit, rangeCode),
            requestId
        );
    }

    @Operation(summary = "查询分红走势", description = "返回股票分红 TTM、申万三级行业平均与中位数分红率走势")
    @GetMapping("/{targetCode}/dividend-trend")
    public ApiResponse<Map<String, Object>> getTargetDividendTrend(
        @PathVariable String targetCode,
        @RequestParam(required = false) String tradeDate,
        @RequestParam(required = false) Integer historyLimit,
        @RequestParam(required = false) String rangeCode,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);
        log.info(
            "[{}] [Controller] getTargetDividendTrend: targetCode={}, tradeDate={}, historyLimit={}, rangeCode={}",
            requestId, targetCode, tradeDate, historyLimit, rangeCode
        );
        return ApiResponse.success(
            watchTargetService.getTargetDividendTrend(targetCode, tradeDate, historyLimit, rangeCode),
            requestId
        );
    }

    @Operation(summary = "查询财务详情", description = "返回财务摘要、三大报表、财务指标与衍生趋势数据")
    @GetMapping("/{targetCode}/financial-detail")
    public ApiResponse<Map<String, Object>> getTargetFinancialDetail(
        @PathVariable String targetCode,
        @RequestParam(required = false) Integer historyLimit,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);
        log.info("[{}] [Controller] getTargetFinancialDetail: targetCode={}, historyLimit={}", requestId, targetCode, historyLimit);
        return ApiResponse.success(
            watchTargetService.getTargetFinancialDetail(targetCode, historyLimit),
            requestId
        );
    }

    @Operation(summary = "查询资金流向详情", description = "返回资金流摘要、历史明细以及资金与价格联动所需的收盘价序列")
    @GetMapping("/{targetCode}/capital-flow-detail")
    public ApiResponse<Map<String, Object>> getTargetCapitalFlowDetail(
        @PathVariable String targetCode,
        @RequestParam(required = false) String tradeDate,
        @RequestParam(required = false) Integer historyLimit,
        @RequestParam(required = false) String rangeCode,
        @RequestParam(required = false, defaultValue = "true") boolean includeQuoteRows,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);
        log.info(
            "[{}] [Controller] getTargetCapitalFlowDetail: targetCode={}, tradeDate={}, historyLimit={}, rangeCode={}, includeQuoteRows={}",
            requestId, targetCode, tradeDate, historyLimit, rangeCode, includeQuoteRows
        );
        return ApiResponse.success(
            watchTargetService.getTargetCapitalFlowDetail(targetCode, tradeDate, historyLimit, rangeCode, includeQuoteRows),
            requestId
        );
    }

    @Operation(summary = "查询行业详情", description = "股票返回申万三级行业归属、所属指数与同业对比；指数返回指数资料与成分股")
    @GetMapping("/{targetCode}/industry-detail")
    public ApiResponse<Map<String, Object>> getTargetIndustryDetail(
        @PathVariable String targetCode,
        @RequestParam(required = false) String tradeDate,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);
        log.info("[{}] [Controller] getTargetIndustryDetail: targetCode={}, tradeDate={}", requestId, targetCode, tradeDate);
        return ApiResponse.success(
            watchTargetService.getTargetIndustryDetail(targetCode, tradeDate),
            requestId
        );
    }

    @Operation(summary = "查询指数详细信息", description = "根据指数完整代码查询指数的页面基本信息，主要包括指数概览、最新行情等，历史行业表现、技术指标、成分股等由其他接口提供")
    @GetMapping("/{targetCode}/index-details")
    public ApiResponse<Map<String, Object>> getIndexDetails(
        @PathVariable String targetCode,
        @RequestParam String tradeDate,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);
        log.info("[{}] [Controller] getIndexDetails: targetCode={}, tradeDate={}", requestId, targetCode, tradeDate);
        return ApiResponse.success(watchTargetService.getIndexDetails(targetCode, tradeDate), requestId);
    }

    @Operation(summary = "查询指数市场表现", description = "根据指数完整代码查询指数的市场表现数据，包括历史行情、技术指标、图表数据等")
    @GetMapping("/{targetCode}/index-market")
    public ApiResponse<Map<String, Object>> getIndexMarket(
        @PathVariable String targetCode,
        @RequestParam String tradeDate,
        @RequestParam(defaultValue = "250") int historyLimit,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);
        log.info("[{}] [Controller] getIndexMarket: targetCode={}, tradeDate={}, historyLimit={}", requestId, targetCode, tradeDate, historyLimit);
        return ApiResponse.success(watchTargetService.getIndexMarket(targetCode, tradeDate, historyLimit), requestId);
    }

    @Operation(summary = "查询指数成分股", description = "根据指数完整代码查询指数的成分股数据，包括成分股列表、权重、行业分布等")
    @GetMapping("/{targetCode}/index-constituents")
    public ApiResponse<Map<String, Object>> getIndexConstituents(
        @PathVariable String targetCode,
        @RequestParam String tradeDate,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int pageSize,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);
        log.info("[{}] [Controller] getIndexConstituents: targetCode={}, tradeDate={}, page={}, pageSize={}", requestId, targetCode, tradeDate, page, pageSize);
        return ApiResponse.success(watchTargetService.getIndexConstituents(targetCode, tradeDate), requestId);
    }

    @Operation(summary = "查询指数成分股分页数据", description = "根据指数完整代码查询指数的成分股分页数据，包括成分股列表、权重、行业分布等")
    @GetMapping("/{targetCode}/index-constituent-page")
    public ApiResponse<Page<Map<String, Object>>> getIndexConstituentPage(
        @PathVariable String targetCode,
        @RequestParam String tradeDate,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int pageSize,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);
        log.info("[{}] [Controller] getIndexConstituentPage: targetCode={}, tradeDate={}, page={}, pageSize={}", requestId, targetCode, tradeDate, page, pageSize);
        return ApiResponse.success(watchTargetService.getIndexConstituentPage(targetCode, tradeDate, page, pageSize), requestId);
    }

}