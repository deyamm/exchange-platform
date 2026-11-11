package com.exchange.platform.analytics.controller;

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.exchange.platform.analytics.api.response.AlertMessageView;
import com.exchange.platform.analytics.service.AlertMessageService;
import com.exchange.platform.common.core.result.ApiResponse;
import com.exchange.platform.common.server.support.RequestIdHolder;

@RestController
@RequestMapping("/api/v1/analytics/alert-messages")
public class AlertMessageController {
    private final AlertMessageService service;
    public AlertMessageController(AlertMessageService service) { this.service = service; }

    @GetMapping("/recent")
    public ApiResponse<List<AlertMessageView>> recent(@RequestParam String targetType, @RequestParam String targetCode,
            @RequestParam(defaultValue = "5") int limit, HttpServletRequest request) {
        return ApiResponse.success(service.listRecent(targetType, targetCode, limit), RequestIdHolder.getRequestId(request));
    }

    /** 供调度器、数据更新事件和运维手工触发共用的幂等评估入口。 */
    @PostMapping("/evaluate")
    public ApiResponse<Integer> evaluate(HttpServletRequest request) {
        return ApiResponse.success(service.evaluateEnabledRules(), RequestIdHolder.getRequestId(request));
    }
}
