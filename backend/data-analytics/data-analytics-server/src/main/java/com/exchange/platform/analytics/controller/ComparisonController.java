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
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

import com.exchange.platform.analytics.api.request.ComparisonQueryRequest;
import com.exchange.platform.analytics.api.request.ComparisonResultSaveRequest;
import com.exchange.platform.analytics.api.response.ComparisonResultView;
import com.exchange.platform.analytics.api.response.ComparisonSnapshotView;
import com.exchange.platform.analytics.service.ComparisonService;
import com.exchange.platform.common.core.result.ApiResponse;
import com.exchange.platform.common.server.support.RequestIdHolder;

@Tag(name = "分析Comparison")
@RestController
@RequestMapping("/api/v1/analytics/comparison")
public class ComparisonController {

    private static final Logger log = LoggerFactory.getLogger(ComparisonController.class);
    private final ComparisonService comparisonService;
    public ComparisonController(ComparisonService comparisonService) { this.comparisonService = comparisonService; }

    @PostMapping("/query")
    @Operation(summary = "执行指标对比查询", description = "按请求条件返回多个标的的指标对比结果")
    public ApiResponse<ComparisonResultView> query(@Valid @RequestBody ComparisonQueryRequest body, HttpServletRequest request) {
        String requestId = RequestIdHolder.getRequestId(request);
        log.info("[{}] [Controller] query body={}", requestId, body);
        return ApiResponse.success(comparisonService.execute(body), requestId);
    }
    @PostMapping("/results")
    @Operation(summary = "保存对比结果", description = "保存指标对比结果快照")
    public ApiResponse<ComparisonSnapshotView> save(@Valid @RequestBody ComparisonResultSaveRequest body, HttpServletRequest request) {
        String requestId = RequestIdHolder.getRequestId(request);
        log.info("[{}] [Controller] save body={}", requestId, body);
        return ApiResponse.success(comparisonService.saveResult(body), requestId);
    }
    @GetMapping("/results/{resultId}")
    @Operation(summary = "查询对比结果", description = "按结果编码查询已保存的对比快照")
    public ApiResponse<ComparisonSnapshotView> get(@PathVariable String resultId, HttpServletRequest request) {
        String requestId = RequestIdHolder.getRequestId(request);
        log.info("[{}] [Controller] get resultId={}", requestId, resultId);
        return ApiResponse.success(comparisonService.getSavedResult(resultId), requestId);
    }
}
