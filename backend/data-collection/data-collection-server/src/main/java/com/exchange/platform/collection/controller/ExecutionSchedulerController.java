package com.exchange.platform.collection.controller;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.*;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.exchange.platform.collection.api.enums.ScheduleStatus;
import com.exchange.platform.collection.api.response.execution.ExecutionSchedulerView;
import com.exchange.platform.collection.service.ExecutionSchedulerService;
import com.exchange.platform.common.core.page.PageResult;
import com.exchange.platform.common.core.result.ApiResponse;
import com.exchange.platform.common.server.support.RequestIdHolder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 执行调度控制器，负责处理与调度任务相关的请求
 * - 包括调度任务分页查询、暂停、恢复、删除等功能
 */
@Tag(name = "执行调度控制", description = "调度任务查询与状态管理")
@RestController
@RequestMapping("/api/v1/collection/execution-schedulers")
public class ExecutionSchedulerController {

    private static final Logger logger = LoggerFactory.getLogger(ExecutionSchedulerController.class);

    private final ExecutionSchedulerService executionSchedulerService;

    public ExecutionSchedulerController(ExecutionSchedulerService executionSchedulerService) {
        this.executionSchedulerService = executionSchedulerService;
    }

    @Operation(summary = "查询调度任务列表", description = "按任务编码、状态分页查询调度任务")
    @GetMapping
    public ApiResponse<PageResult<ExecutionSchedulerView>> querySchedulers(
        @RequestParam(required = false) String taskCode,
        @RequestParam(required = false) ScheduleStatus status,
        @RequestParam(defaultValue = "1") Integer pageNo,
        @RequestParam(defaultValue = "20") Integer pageSize,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);
        logger.info("[{}] [Controller] querySchedulers called with taskCode={}, status={}, pageNo={}, pageSize={}",
            requestId, taskCode, status, pageNo, pageSize);

        PageResult<ExecutionSchedulerView> result = executionSchedulerService.querySchedulers(taskCode, status, pageNo, pageSize);
        return ApiResponse.success(result, requestId);
    }

    @Operation(summary = "暂停调度", description = "将调度状态设置为 PAUSED")
    @PostMapping("/{schedulerId}/pause")
    public ApiResponse<Boolean> pauseScheduler(
        @PathVariable String schedulerId,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);
        logger.info("[{}] [Controller] pauseScheduler called with schedulerId={}", requestId, schedulerId);

        Boolean result = executionSchedulerService.pauseScheduler(schedulerId);
        return ApiResponse.success(result, requestId);
    }

    @Operation(summary = "恢复调度", description = "将调度状态设置为 ACTIVE")
    @PostMapping("/{schedulerId}/resume")
    public ApiResponse<Boolean> resumeScheduler(
        @PathVariable String schedulerId,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);
        logger.info("[{}] [Controller] resumeScheduler called with schedulerId={}", requestId, schedulerId);

        Boolean result = executionSchedulerService.resumeScheduler(schedulerId);
        return ApiResponse.success(result, requestId);
    }

    @Operation(summary = "删除调度", description = "删除调度配置，不影响执行记录与快照")
    @DeleteMapping("/{schedulerId}")
    public ApiResponse<Boolean> deleteScheduler(
        @PathVariable String schedulerId,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);
        logger.info("[{}] [Controller] deleteScheduler called with schedulerId={}", requestId, schedulerId);

        Boolean result = executionSchedulerService.deleteScheduler(schedulerId);
        return ApiResponse.success(result, requestId);
    }
}