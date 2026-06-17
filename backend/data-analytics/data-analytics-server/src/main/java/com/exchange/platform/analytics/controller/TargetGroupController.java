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
import com.exchange.platform.analytics.api.request.TargetGroupSaveRequest;
import com.exchange.platform.analytics.api.response.TargetGroupView;
import com.exchange.platform.analytics.service.TargetGroupService;
import com.exchange.platform.common.core.page.PageResult;
import com.exchange.platform.common.core.result.ApiResponse;
import com.exchange.platform.common.server.support.RequestIdHolder;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/api/v1/analytics/target-groups")
public class TargetGroupController {

    private static final Logger log = LoggerFactory.getLogger(TargetGroupController.class);

    private final TargetGroupService targetGroupService;

    public TargetGroupController(TargetGroupService targetGroupService) {
        this.targetGroupService = targetGroupService;
    }

    @Operation(summary = "获取标的分组列表", description = "获取标的分组列表，支持分页和多条件过滤")
    @GetMapping
    public ApiResponse<PageResult<TargetGroupView>> getTargetGroupList(
        @RequestParam(required = false) String groupName,
        @RequestParam(required = false) String groupCode,
        @RequestParam(required = false) CommonStatus status,
        @RequestParam(defaultValue = "1") Integer pageNo,
        @RequestParam(defaultValue = "20") Integer pageSize,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);

        log.info("[{}] [Controller] getTargetGroupList: groupName={}, groupCode={}, status={}, pageNo={}, pageSize={}", requestId, groupName, groupCode, status, pageNo, pageSize);

        return ApiResponse.success(targetGroupService.getTargetGroupList(groupName, groupCode, status, pageNo, pageSize), requestId);
    }

    @Operation(summary = "保存或更新标的分组", description = "根据请求体中的groupCode创建或更新标的分组")
    @PostMapping
    public ApiResponse<TargetGroupView> saveOrUpdateTargetGroup(
        @RequestBody TargetGroupSaveRequest body,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);

        log.info("[{}] [Controller] saveOrUpdateTargetGroup: body={}", requestId, body);

        return ApiResponse.success(targetGroupService.saveOrUpdateTargetGroup(body), requestId);
    }

    @Operation(summary = "删除标的分组", description = "根据groupCode删除标的分组")
    @DeleteMapping("/{groupCode}")
    public ApiResponse<Void> deleteTargetGroup(
        @PathVariable String groupCode,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);

        log.info("[{}] [Controller] deleteTargetGroup: groupCode={}", requestId, groupCode);
        targetGroupService.deleteTargetGroup(groupCode);
        
        return ApiResponse.success(null, requestId);
    }
}