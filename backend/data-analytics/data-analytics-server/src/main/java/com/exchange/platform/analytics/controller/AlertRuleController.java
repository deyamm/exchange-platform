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
import com.exchange.platform.analytics.api.enums.RuleType;
import com.exchange.platform.analytics.api.request.AlertRuleSaveRequest;
import com.exchange.platform.analytics.api.response.AlertRuleView;
import com.exchange.platform.analytics.service.AlertRuleService;
import com.exchange.platform.common.core.page.PageResult;
import com.exchange.platform.common.core.result.ApiResponse;
import com.exchange.platform.common.server.support.RequestIdHolder;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/api/v1/analytics/alert-rules")
public class AlertRuleController {

    private static final Logger log = LoggerFactory.getLogger(AlertRuleController.class);

    private final AlertRuleService alertRuleService;

    public AlertRuleController(AlertRuleService alertRuleService) {
        this.alertRuleService = alertRuleService;
    }

    @Operation(summary = "获取预警规则列表", description = "获取预警规则列表，支持分页和多条件过滤")
    @GetMapping
    public ApiResponse<PageResult<AlertRuleView>> getAlertRuleList(
        @RequestParam(required = false) String ruleName,
        @RequestParam(required = false) String ruleCode,
        @RequestParam(required = false) RuleType ruleType,
        @RequestParam(required = false) String targetCode,
        @RequestParam(required = false) String groupCode,
        @RequestParam(required = false) String metricCode,
        @RequestParam(required = false) CommonStatus status,
        @RequestParam(defaultValue = "1") Integer pageNo,
        @RequestParam(defaultValue = "20") Integer pageSize,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);

        log.info("[{}] [Controller] getAlertRuleList: ruleName={}, ruleCode={}, ruleType={}, targetCode={}, groupCode={}, metricCode={}, status={}, pageNo={}, pageSize={}", requestId, ruleName, ruleCode, ruleType, targetCode, groupCode, metricCode, status, pageNo, pageSize);

        return ApiResponse.success(alertRuleService.getAlertRuleList(ruleName, ruleCode, ruleType, targetCode, groupCode, metricCode, status, pageNo, pageSize), requestId);
    }

    @Operation(summary = "保存或更新预警规则", description = "根据请求体中的ruleCode创建或更新预警规则")
    @PostMapping
    public ApiResponse<AlertRuleView> saveOrUpdateAlertRule(
        @RequestBody AlertRuleSaveRequest body,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);

        log.info("[{}] [Controller] saveOrUpdateAlertRule: body={}", requestId, body);

        return ApiResponse.success(alertRuleService.saveOrUpdateAlertRule(body), requestId);
    }

    @Operation(summary = "删除预警规则", description = "根据ruleCode删除预警规则")
    @DeleteMapping("/{ruleCode}")
    public ApiResponse<Void> deleteAlertRule(
        @PathVariable String ruleCode,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);

        log.info("[{}] [Controller] deleteAlertRule: ruleCode={}", requestId, ruleCode);
        alertRuleService.deleteAlertRule(ruleCode);
        return ApiResponse.success(null, requestId);
    }
}