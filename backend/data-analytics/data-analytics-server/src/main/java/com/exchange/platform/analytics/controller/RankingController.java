package com.exchange.platform.analytics.controller;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.exchange.platform.analytics.api.request.RankingQueryRequest;
import com.exchange.platform.analytics.api.request.RankingResultSaveRequest;
import com.exchange.platform.analytics.api.response.RankingResultView;
import com.exchange.platform.analytics.api.response.RankingSnapshotView;
import com.exchange.platform.analytics.service.RankingService;
import com.exchange.platform.common.core.result.ApiResponse;
import com.exchange.platform.common.server.support.RequestIdHolder;

@RestController
@RequestMapping("/api/v1/analytics/ranking")
public class RankingController {
    private final RankingService rankingService;
    public RankingController(RankingService rankingService) { this.rankingService = rankingService; }

    @PostMapping("/query")
    public ApiResponse<RankingResultView> query(@Valid @RequestBody RankingQueryRequest body, HttpServletRequest request) {
        return ApiResponse.success(rankingService.execute(body), RequestIdHolder.getRequestId(request));
    }

    @PostMapping("/results")
    public ApiResponse<RankingSnapshotView> save(@Valid @RequestBody RankingResultSaveRequest body, HttpServletRequest request) {
        return ApiResponse.success(rankingService.saveResult(body), RequestIdHolder.getRequestId(request));
    }

    @GetMapping("/results/{resultId}")
    public ApiResponse<RankingSnapshotView> get(@PathVariable String resultId, HttpServletRequest request) {
        return ApiResponse.success(rankingService.getSavedResult(resultId), RequestIdHolder.getRequestId(request));
    }
}
