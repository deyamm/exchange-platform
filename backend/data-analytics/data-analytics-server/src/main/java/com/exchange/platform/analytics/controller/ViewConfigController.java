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
import com.exchange.platform.analytics.api.request.ViewConfigSaveRequest;
import com.exchange.platform.analytics.api.response.ViewConfigView;
import com.exchange.platform.analytics.service.ViewConfigService;
import com.exchange.platform.common.core.page.PageResult;
import com.exchange.platform.common.core.result.ApiResponse;
import com.exchange.platform.common.server.support.RequestIdHolder;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/api/v1/analytics/view-configs")
public class ViewConfigController {

    private static final Logger log = LoggerFactory.getLogger(ViewConfigController.class);

    private final ViewConfigService viewConfigService;

    public ViewConfigController(ViewConfigService viewConfigService) {
        this.viewConfigService = viewConfigService;
    }

    @Operation(summary = "获取视图配置列表", description = "获取视图配置列表，支持分页和多条件过滤")
    @GetMapping
    public ApiResponse<PageResult<ViewConfigView>> getViewConfigList(
        @RequestParam(required = false) String viewName,
        @RequestParam(required = false) String viewCode,
        @RequestParam(required = false) String sceneCode,
        @RequestParam(required = false) String viewType,
        @RequestParam(required = false) CommonStatus status,
        @RequestParam(defaultValue = "1") Integer pageNo,
        @RequestParam(defaultValue = "20") Integer pageSize,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);

        log.info("[{}] [Controller] getViewConfigList: viewName={}, viewCode={}, sceneCode={}, viewType={}, status={}, pageNo={}, pageSize={}", requestId, viewName, viewCode, sceneCode, viewType, status, pageNo, pageSize);

        return ApiResponse.success(viewConfigService.getViewConfigList(viewName, viewCode, sceneCode, viewType, status, pageNo, pageSize), requestId);
    }

    @Operation(summary = "保存或更新视图配置", description = "根据请求体中的viewCode创建或更新视图配置")
    @PostMapping
    public ApiResponse<ViewConfigView> saveOrUpdateViewConfig(
        @RequestBody ViewConfigSaveRequest body,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);

        log.info("[{}] [Controller] saveOrUpdateViewConfig: body={}", requestId, body);

        return ApiResponse.success(viewConfigService.saveOrUpdateViewConfig(body), requestId);
    }

    @Operation(summary = "删除视图配置", description = "根据viewCode删除视图配置")
    @DeleteMapping("/{viewCode}")
    public ApiResponse<Void> deleteViewConfig(
        @PathVariable String viewCode,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);

        log.info("[{}] [Controller] deleteViewConfig: viewCode={}", requestId, viewCode);
        
        viewConfigService.deleteViewConfig(viewCode);
        return ApiResponse.success(null, requestId);
    }
}