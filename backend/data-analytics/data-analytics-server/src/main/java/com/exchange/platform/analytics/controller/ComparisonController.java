package com.exchange.platform.analytics.controller;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.exchange.platform.analytics.api.request.ComparisonQueryRequest;
import com.exchange.platform.analytics.api.request.ComparisonResultSaveRequest;
import com.exchange.platform.analytics.api.response.ComparisonResultView;
import com.exchange.platform.analytics.api.response.ComparisonSnapshotView;
import com.exchange.platform.analytics.service.ComparisonService;
import com.exchange.platform.common.core.result.ApiResponse;
import com.exchange.platform.common.server.support.RequestIdHolder;

@RestController
@RequestMapping("/api/v1/analytics/comparison")
public class ComparisonController {
    private final ComparisonService comparisonService;
    public ComparisonController(ComparisonService comparisonService) { this.comparisonService = comparisonService; }

    @PostMapping("/query")
    public ApiResponse<ComparisonResultView> query(@Valid @RequestBody ComparisonQueryRequest body, HttpServletRequest request) {
        return ApiResponse.success(comparisonService.execute(body), RequestIdHolder.getRequestId(request));
    }
    @PostMapping("/results")
    public ApiResponse<ComparisonSnapshotView> save(@Valid @RequestBody ComparisonResultSaveRequest body, HttpServletRequest request) {
        return ApiResponse.success(comparisonService.saveResult(body), RequestIdHolder.getRequestId(request));
    }
    @GetMapping("/results/{resultId}")
    public ApiResponse<ComparisonSnapshotView> get(@PathVariable String resultId, HttpServletRequest request) {
        return ApiResponse.success(comparisonService.getSavedResult(resultId), RequestIdHolder.getRequestId(request));
    }
}
