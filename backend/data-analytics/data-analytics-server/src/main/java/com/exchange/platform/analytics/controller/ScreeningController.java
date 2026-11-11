package com.exchange.platform.analytics.controller;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.exchange.platform.analytics.api.request.ScreeningQueryRequest;
import com.exchange.platform.analytics.api.request.ScreeningResultSaveRequest;
import com.exchange.platform.analytics.api.response.ScreeningResultView;
import com.exchange.platform.analytics.api.response.ScreeningSnapshotView;
import com.exchange.platform.analytics.api.response.TargetScopeOptionsView;
import com.exchange.platform.analytics.service.ScreeningService;
import com.exchange.platform.common.core.enums.AssetType;
import com.exchange.platform.common.core.result.ApiResponse;
import com.exchange.platform.common.server.support.RequestIdHolder;

import io.swagger.v3.oas.annotations.Operation;

/** 条件筛选业务接口；保存方案/快照属于后续持久化能力，本接口只负责当前执行。 */
@RestController
@RequestMapping("/api/v1/analytics/screening")
public class ScreeningController {

    private static final Logger log = LoggerFactory.getLogger(ResearchTagController.class);

    private final ScreeningService screeningService;

    public ScreeningController(ScreeningService screeningService) {
        this.screeningService = screeningService;
    }

    @Operation(summary = "查询筛选范围可选项", description = "仅返回范围选择器所需的主数据编码和名称")
    @GetMapping("/scope-options")
    public ApiResponse<TargetScopeOptionsView> scopeOptions(@RequestParam AssetType targetType,
            HttpServletRequest request) {
        log.info("[Controller] scopeOptions: targetType={}", targetType);
        return ApiResponse.success(screeningService.getScopeOptions(targetType), RequestIdHolder.getRequestId(request));
    }

    @Operation(summary = "执行指标条件筛选", description = "服务端解析标的范围、调用统一指标取数并计算两层条件组")
    @PostMapping("/query")
    public ApiResponse<ScreeningResultView> query(@Valid @RequestBody ScreeningQueryRequest body,
            HttpServletRequest request) {
        log.info("[Controller] query: targetType={}, targetScope={}, conditions={}", body.targetType(),
                body.targetScope(), body.conditionGroups());
        return ApiResponse.success(screeningService.execute(body), RequestIdHolder.getRequestId(request));
    }

    @Operation(summary = "保存筛选结果快照", description = "保存本次已经执行的条件和指标值，不重新读取最新指标")
    @PostMapping("/results")
    public ApiResponse<ScreeningSnapshotView> save(@Valid @RequestBody ScreeningResultSaveRequest body,
            HttpServletRequest request) {
        log.info("[Controller] save: targetType={}, targetScope={}", body.request().targetType(),
                body.request().targetScope());
        return ApiResponse.success(screeningService.saveResult(body), RequestIdHolder.getRequestId(request));
    }

    @Operation(summary = "回看筛选结果快照", description = "直接返回持久化快照，不触发指标重新计算")
    @GetMapping("/results/{resultId}")
    public ApiResponse<ScreeningSnapshotView> getSaved(@PathVariable String resultId, HttpServletRequest request) {
        log.info("[Controller] getSaved: resultId={}", resultId);
        return ApiResponse.success(screeningService.getSavedResult(resultId), RequestIdHolder.getRequestId(request));
    }
}
