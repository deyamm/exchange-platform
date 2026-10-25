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

    @Operation(summary = "查询股票详细信息", description = "根据股票完整代码查询股票的详细信息，包括历史价格、财务指标、估值指标等")
    @GetMapping("/{targetCode}/details")
    public ApiResponse<Map<String, Object>> getStockDetails(
        @PathVariable String targetCode,
        @RequestParam String tradeDate,
        @RequestParam(defaultValue = "True") boolean includeCharts,
        @RequestParam(defaultValue = "True") boolean includeFinancial,
        @RequestParam(defaultValue = "True") boolean includeHistory,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);
        log.info("[{}] [Controller] getStockDetails: targetCode={}, tradeDate={}, includeCharts={}, includeFinancial={}, includeHistory={}", requestId, targetCode, tradeDate, includeCharts, includeFinancial, includeHistory);
        return ApiResponse.success(watchTargetService.getStockDetails(targetCode, tradeDate, includeCharts, includeFinancial, includeHistory), requestId);
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