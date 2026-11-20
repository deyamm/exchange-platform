package com.exchange.platform.analytics.controller;

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

import com.exchange.platform.analytics.api.response.AlertMessageView;
import com.exchange.platform.analytics.service.AlertMessageService;
import com.exchange.platform.common.core.result.ApiResponse;
import com.exchange.platform.common.server.support.RequestIdHolder;

@Tag(name = "分析AlertMessage")
@RestController
@RequestMapping("/api/v1/analytics/alert-messages")
public class AlertMessageController {

    private static final Logger log = LoggerFactory.getLogger(AlertMessageController.class);
    private final AlertMessageService service;
    public AlertMessageController(AlertMessageService service) { this.service = service; }

    @GetMapping("/recent")
    @Operation(summary = "查询最近预警消息", description = "按标的类型和编码查询最近预警消息")
    public ApiResponse<List<AlertMessageView>> recent(@RequestParam String targetType, @RequestParam String targetCode,
            @RequestParam(defaultValue = "5") int limit, HttpServletRequest request) {
        String requestId = RequestIdHolder.getRequestId(request);
        log.info("[{}] [Controller] recent targetType={}, targetCode={}, limit={}", requestId, targetType, targetCode, limit);
        return ApiResponse.success(service.listRecent(targetType, targetCode, limit), requestId);
    }

    /** 供调度器、数据更新事件和运维手工触发共用的幂等评估入口。 */
    @PostMapping("/evaluate")
    @Operation(summary = "执行预警规则评估", description = "评估所有启用的预警规则并生成消息")
    public ApiResponse<Integer> evaluate(HttpServletRequest request) {
        String requestId = RequestIdHolder.getRequestId(request);
        log.info("[{}] [Controller] evaluate", requestId);
        return ApiResponse.success(service.evaluateEnabledRules(), requestId);
    }
}
