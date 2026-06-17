package com.exchange.platform.analytics.controller;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.exchange.platform.analytics.api.enums.ResultType;
import com.exchange.platform.analytics.api.enums.WatchStatus;
import com.exchange.platform.analytics.api.response.AppResultView;
import com.exchange.platform.analytics.api.response.WatchTargetView;
import com.exchange.platform.analytics.service.AppResultService;
import com.exchange.platform.analytics.service.TypeBasicDataService;
import com.exchange.platform.analytics.service.WatchTargetService;
import com.exchange.platform.common.core.page.PageResult;
import com.exchange.platform.common.core.result.ApiResponse;
import com.exchange.platform.common.server.support.RequestIdHolder;

import io.swagger.v3.oas.annotations.Operation;

/**
 * A-07 市场概览模块接口。
 * 对应 I-18 查询市场概览、I-19 查询自选观察概览。
 */
@RestController
@RequestMapping("/api/v1/analytics")
public class MarketOverviewController {

    private static final Logger log = LoggerFactory.getLogger(MarketOverviewController.class);

    private final AppResultService appResultService;
    private final TypeBasicDataService typeBasicDataService;

    public MarketOverviewController(AppResultService appResultService,
            WatchTargetService watchTargetService, TypeBasicDataService typeBasicDataService) {
        this.appResultService = appResultService;
        this.typeBasicDataService = typeBasicDataService;
    }

    @Operation(summary = "查询市场概览", description = "查询市场行情概览、指数概览、板块概览和涨跌分布")
    @GetMapping("/market/overview")
    public ApiResponse<AppResultView> getMarketOverview(
            @RequestParam(required = false) String sceneCode,
            HttpServletRequest request) {

        String requestId = RequestIdHolder.getRequestId(request);

        log.info("[{}] [Controller] getMarketOverview sceneCode={}", requestId, sceneCode);
        
        AppResultView view = appResultService.getLatest(ResultType.MARKET_OVERVIEW, null, sceneCode);

        return ApiResponse.success(view, requestId);
    }

    @Operation(summary = "查询自选观察概览", description = "查询关注标的分组、重点标记和关键指标概览")
    @GetMapping("/watch-targets/overview")
    public ApiResponse<PageResult<WatchTargetView>> getWatchOverview(
            @RequestParam(required = false) String targetName,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String marketCode,
            @RequestParam(required = false) String groupCode,
            @RequestParam(required = false) WatchStatus watchStatus,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "20") Integer pageSize,
            HttpServletRequest request) {

        String requestId = RequestIdHolder.getRequestId(request);

        log.info("[{}] [Controller] getWatchOverview targetName={}, targetType={}, marketCode={}, groupCode={}, watchStatus={}",
                requestId, targetName, targetType, marketCode, groupCode, watchStatus);

        return null; // TODO: 实现分页查询关注标的接口
    }

    @Operation(summary = "查询指数基本信息", description = "查询指数基本信息，支持按市场、指数类别等维度过滤")
    @GetMapping("/market/index-basic")
    public ApiResponse<List<Map<String, Object>>> getIndexBasic(
            HttpServletRequest request) {
        String requestId = RequestIdHolder.getRequestId(request);
        
        log.info("[{}] [Controller] getIndexBasic ", requestId);
        
        return ApiResponse.success(typeBasicDataService.getIndexBasic(), requestId);
    }
}
