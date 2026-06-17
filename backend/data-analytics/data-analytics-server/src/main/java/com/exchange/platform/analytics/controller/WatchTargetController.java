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

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exchange.platform.analytics.api.request.WatchTargetSaveRequest;
import com.exchange.platform.analytics.api.response.WatchTargetView;
import com.exchange.platform.analytics.service.WatchTargetService;
import com.exchange.platform.common.core.result.ApiResponse;
import com.exchange.platform.common.server.support.RequestIdHolder;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/api/v1/analytics/watch-targets")
public class WatchTargetController {

    private static final Logger log = LoggerFactory.getLogger(WatchTargetController.class);

    private final WatchTargetService watchTargetService;

    public WatchTargetController(WatchTargetService watchTargetService) {
        this.watchTargetService = watchTargetService;
    }

    @Operation(summary = "分页查询分组下关注标的", description = "根据标的分组编码、标的代码、标的名称、市场类型分页查询关注标的")
    @GetMapping
    public ApiResponse<Page<WatchTargetView>> queryWatchTargetPage(
        @RequestParam(required = false) String groupCode,
        @RequestParam(required = false) String targetCode,
        @RequestParam(required = false) String targetName,
        @RequestParam(required = false) String targetType,
        @RequestParam(required = false) String marketCode,
        @RequestParam(defaultValue = "1") long pageNo,
        @RequestParam(defaultValue = "20") long pageSize,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);
        log.info(
            "[{}] [Controller] queryWatchTargetPage: groupCode={}, targetCode={}, targetName={}, targetType={}, marketCode={}, pageNo={}, pageSize={}",
            requestId, groupCode, targetCode, targetName, targetType, marketCode, pageNo, pageSize
        );
        return ApiResponse.success(
            watchTargetService.queryWatchTargetPage(groupCode, targetCode, targetName, targetType, marketCode, pageNo, pageSize),
            requestId
        );
    }

    @Operation(summary = "保存或更新关注标的", description = "根据请求体中的targetCode创建或更新关注标的")
    @PostMapping
    public ApiResponse<WatchTargetView> saveOrUpdateWatchTarget(
        @RequestBody WatchTargetSaveRequest body,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);
        log.info("[{}] [Controller] saveOrUpdateWatchTarget: body={}", requestId, body);
        return ApiResponse.success(watchTargetService.saveOrUpdateWatchTarget(body), requestId);
    }

    @Operation(summary = "删除关注标的", description = "根据targetCode删除关注标的；传入groupCode时仅从指定分组移除")
    @DeleteMapping("/{targetCode}")
    public ApiResponse<Void> deleteWatchTarget(
        @PathVariable String targetCode,
        @RequestParam(required = false) String targetType,
        @RequestParam(required = false) String groupCode,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);
        log.info("[{}] [Controller] deleteWatchTarget: targetCode={}, targetType={}, groupCode={}", requestId, targetCode, targetType, groupCode);
        watchTargetService.deleteWatchTarget(targetCode, targetType, groupCode);
        return ApiResponse.success(null, requestId);
    }

    @Operation(summary = "获取关注标的详细信息，包括所属分组", description = "根据targetCode获取关注标的信息以及所在分组信息")
    @GetMapping("/{targetCode}/groups")
    public ApiResponse<WatchTargetView> getWatchTargetGroups(
        @PathVariable String targetCode,
        @RequestParam String targetType,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);
        log.info("[{}] [Controller] getWatchTargetGroups: targetCode={}, targetType={}", requestId, targetCode, targetType);
        return ApiResponse.success(watchTargetService.getWatchTargetGroups(targetCode, targetType), requestId);
    }

}