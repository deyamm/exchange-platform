package com.exchange.platform.analytics.controller;

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

import com.exchange.platform.analytics.api.enums.CommonStatus;
import com.exchange.platform.analytics.api.request.AppMetricSaveRequest;
import com.exchange.platform.analytics.api.response.AppMetricView;
import com.exchange.platform.analytics.service.AppMetricService;
import com.exchange.platform.common.core.page.PageResult;
import com.exchange.platform.common.core.result.ApiResponse;
import com.exchange.platform.common.server.support.RequestIdHolder;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/api/v1/analytics/metrics")
public class MetricController {

    private static final Logger log = LoggerFactory.getLogger(MetricController.class);

    private final AppMetricService metricService;

    public MetricController(AppMetricService metricService) {
        this.metricService = metricService;
    }

    @Operation(summary = "获取指标列表", description = "获取指标列表，支持分页和多条件过滤")
    @GetMapping
    public ApiResponse<PageResult<AppMetricView>> getMetricList(
        @RequestParam(required = false) String metricName,
        @RequestParam(required = false) String metricCode,
        @RequestParam(required = false) String metricCategory,
        @RequestParam(required = false) String dataSourceCode,
        @RequestParam(required = false) CommonStatus status,
        @RequestParam(defaultValue = "1") Integer pageNo,
        @RequestParam(defaultValue = "20") Integer pageSize,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);

        log.info("[{}] [Controller] getMetricList: metricName={}, metricCode={}, metricCategory={}, dataSourceCode={}, status={}, pageNo={}, pageSize={}", requestId, metricName, metricCode, metricCategory, dataSourceCode, status, pageNo, pageSize);

        return ApiResponse.success(metricService.getMetricList(metricName, metricCode, metricCategory, dataSourceCode, status, pageNo, pageSize), requestId);
    }

    @Operation(summary = "保存或更新指标", description = "根据请求体中的metricCode创建或更新指标")
    @PostMapping
    public ApiResponse<AppMetricView> saveOrUpdateMetric(
        @RequestBody AppMetricSaveRequest body,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);

        log.info("[{}] [Controller] saveOrUpdateMetric: body={}", requestId, body);

        return ApiResponse.success(metricService.saveOrUpdateMetric(body), requestId);
    }

    @Operation(summary = "删除指标", description = "根据metricCode删除指标")
    @DeleteMapping("/{metricCode}")
    public ApiResponse<Void> deleteMetric(
        @PathVariable String metricCode,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);

        log.info("[{}] [Controller] deleteMetric: metricCode={}", requestId, metricCode);
        
        metricService.deleteMetric(metricCode);
        return ApiResponse.success(null, requestId);
    }
}