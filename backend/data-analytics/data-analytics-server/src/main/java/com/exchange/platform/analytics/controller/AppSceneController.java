package com.exchange.platform.analytics.controller;

import java.util.List;

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

import com.exchange.platform.analytics.api.enums.AppSceneType;
import com.exchange.platform.analytics.api.enums.CommonStatus;
import com.exchange.platform.analytics.api.request.SceneSaveRequest;
import com.exchange.platform.analytics.api.response.AppSceneView;
import com.exchange.platform.analytics.service.AppSceneService;
import com.exchange.platform.common.core.page.PageResult;
import com.exchange.platform.common.core.result.ApiResponse;
import com.exchange.platform.common.server.support.RequestIdHolder;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/api/v1/analytics/app-scenes")
public class AppSceneController {
    
    private final AppSceneService appSceneService;

    private static final Logger log = LoggerFactory.getLogger(AppSceneController.class);

    public AppSceneController(AppSceneService appSceneService) {
        this.appSceneService = appSceneService;
    }

    @Operation(summary = "获取应用场景分页列表", description = "获取应用场景列表，支持分页和按名称、分类、状态过滤")
    @GetMapping
    public ApiResponse<PageResult<AppSceneView>> getAppScenePageResult(
        @RequestParam(required = false) String sceneName,
        @RequestParam(required = false) AppSceneType sceneType,
        @RequestParam(required = false) CommonStatus status,
        @RequestParam(defaultValue = "1") Integer pageNo,
        @RequestParam(defaultValue = "20") Integer pageSize,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);

        log.info("[{}] [Controller] getAppScenePageResult: sceneName={}, sceneType={}, status={}, pageNo={}, pageSize={}", requestId, sceneName, sceneType, status, pageNo, pageSize);

        return ApiResponse.success(appSceneService.getAppScenePageResult(sceneName, sceneType, status, pageNo, pageSize), requestId);
    }

    @Operation(summary = "获取所有已启用的应用场景列表", description = "获取所有已启用的应用场景列表，不分页")
    @GetMapping("/enabled")
    public ApiResponse<List<AppSceneView>> getAllEnabledAppSceneList(HttpServletRequest request) {
        String requestId = RequestIdHolder.getRequestId(request);
        log.info("[{}] [Controller] getAllEnabledAppSceneList", requestId);
        return ApiResponse.success(appSceneService.getAllEnabledAppSceneList(), requestId);
    }

    @Operation(summary = "保存或更新应用场景", description = "根据请求体中的场景编码创建或更新应用场景")
    @PostMapping
    public ApiResponse<AppSceneView> saveOrUpdateAppScene(
        @RequestBody SceneSaveRequest body, 
        HttpServletRequest request
    ) {
        
        String requestId = RequestIdHolder.getRequestId(request);

        log.info("[{}] [Controller] saveOrUpdateAppScene: appSceneView={}", requestId, body);

        AppSceneView saved = appSceneService.saveOrUpdateAppScene(body);

        return ApiResponse.success(saved, requestId);
    }

     /**
     * 删除应用场景
     * @param sceneCode 场景编码
     * @return
     */
    @Operation(summary = "删除应用场景", description = "根据场景编码删除应用场景")
    @DeleteMapping("/{sceneCode}")
    public ApiResponse<Void> deleteAppScene(
        @PathVariable String sceneCode, 
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);

        log.info("[{}] [Controller] deleteAppScene: sceneCode={}", requestId, sceneCode);

        appSceneService.deleteAppScene(sceneCode);

        return ApiResponse.success(null, requestId);
    }
}
