package com.exchange.platform.analytics.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.exchange.platform.analytics.api.request.MetricQueryRequest;
import com.exchange.platform.analytics.api.response.MetricValueView;
import com.exchange.platform.analytics.service.MetricValueQueryService;
import com.exchange.platform.common.core.result.ApiResponse;
import com.exchange.platform.common.server.support.RequestIdHolder;

import io.swagger.v3.oas.annotations.Operation;

/** 统一指标取数入口，不包含筛选、排行或提醒等业务计算。 */
@RestController
@RequestMapping("/api/v1/analytics/metric-values")
public class MetricValueController {
    private final MetricValueQueryService metricValueQueryService;

    public MetricValueController(MetricValueQueryService metricValueQueryService) {
        this.metricValueQueryService = metricValueQueryService;
    }

    @Operation(summary = "批量查询统一指标值", description = "按指标配置和 Dataset 分组读取截至时间内的最新有效值")
    @PostMapping("/query")
    public ApiResponse<List<MetricValueView>> query(@Valid @RequestBody MetricQueryRequest body,
            HttpServletRequest request) {
        String requestId = RequestIdHolder.getRequestId(request);
        return ApiResponse.success(metricValueQueryService.queryMetricValues(body), requestId);
    }
}
