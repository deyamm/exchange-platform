package com.exchange.platform.collection.controller;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exchange.platform.collection.api.enums.ScheduleStatus;
import com.exchange.platform.collection.api.request.execution.ExecutionSchedulerRequest;
import com.exchange.platform.collection.api.response.execution.ExecutionSchedulerView;
import com.exchange.platform.collection.converter.ExecutionConverter;
import com.exchange.platform.collection.entity.execution.ExecutionScheduler;
import com.exchange.platform.collection.mapper.execution.ExecutionSchedulerMapper;
import com.exchange.platform.common.core.exception.BusinessException;
import com.exchange.platform.common.core.page.PageResult;
import com.exchange.platform.common.core.result.ApiResponse;
import com.exchange.platform.common.server.support.RequestIdHolder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.stream.Collectors;

@Tag(name = "执行调度控制", description = "调度任务查询与状态管理")
@RestController
@RequestMapping("/api/v1/collection/execution-schedulers")
public class ExecutionSchedulerController {

    private static final Logger log = LoggerFactory.getLogger(ExecutionSchedulerController.class);

    private final ExecutionSchedulerMapper schedulerMapper;

    public ExecutionSchedulerController(ExecutionSchedulerMapper schedulerMapper) {
        this.schedulerMapper = schedulerMapper;
    }

    @Operation(summary = "查询调度任务列表", description = "按任务编码、状态分页查询调度任务")
    @GetMapping
    @Transactional(readOnly = true)
    public ApiResponse<PageResult<ExecutionSchedulerView>> querySchedulers(
        @RequestParam(required = false) String taskCode,
        @RequestParam(required = false) ScheduleStatus status,
        @RequestParam(defaultValue = "1") Integer pageNo,
        @RequestParam(defaultValue = "20") Integer pageSize,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);
        ExecutionSchedulerRequest params = new ExecutionSchedulerRequest(taskCode, status, pageNo, pageSize);
        log.info("[{}] [Controller] querySchedulers params={}", requestId, params);

        int safePageNo = (pageNo == null || pageNo < 1) ? 1 : pageNo;
        int safePageSize = (pageSize == null || pageSize < 1) ? 20 : Math.min(200, pageSize);

        LambdaQueryWrapper<ExecutionScheduler> wrapper = new LambdaQueryWrapper<ExecutionScheduler>()
            .eq(StringUtils.hasText(taskCode), ExecutionScheduler::getTaskCode, taskCode)
            .orderByDesc(ExecutionScheduler::getCreatedAt);
        if (status != null) {
            wrapper.eq(ExecutionScheduler::getStatus, status);
        }

        Page<ExecutionScheduler> page = schedulerMapper.selectPage(new Page<>(safePageNo, safePageSize), wrapper);
        PageResult<ExecutionSchedulerView> result = new PageResult<>(
            safePageNo,
            safePageSize,
            page.getTotal(),
            page.getRecords().stream().map(ExecutionConverter::toSchedulerView).collect(Collectors.toList())
        );
        return ApiResponse.success(result, requestId);
    }

    @Operation(summary = "暂停调度", description = "将调度状态设置为 PAUSED")
    @PostMapping("/{schedulerId}/pause")
    public ApiResponse<Boolean> pauseScheduler(@PathVariable String schedulerId, HttpServletRequest request) {
        return updateSchedulerStatus(schedulerId, ScheduleStatus.PAUSED, request);
    }

    @Operation(summary = "恢复调度", description = "将调度状态设置为 ACTIVE")
    @PostMapping("/{schedulerId}/resume")
    public ApiResponse<Boolean> resumeScheduler(@PathVariable String schedulerId, HttpServletRequest request) {
        return updateSchedulerStatus(schedulerId, ScheduleStatus.ACTIVE, request);
    }

    @Operation(summary = "删除调度", description = "删除调度配置，不影响执行记录与快照")
    @DeleteMapping("/{schedulerId}")
    public ApiResponse<Boolean> deleteScheduler(@PathVariable String schedulerId, HttpServletRequest request) {
        String requestId = RequestIdHolder.getRequestId(request);
        int deleted = schedulerMapper.delete(new LambdaQueryWrapper<ExecutionScheduler>()
            .eq(ExecutionScheduler::getSchedulerId, schedulerId)
        );
        if (deleted == 0) {
            throw new BusinessException(40003, "调度任务不存在：" + schedulerId);
        }
        return ApiResponse.success(true, requestId);
    }

    private ApiResponse<Boolean> updateSchedulerStatus(String schedulerId, ScheduleStatus status, HttpServletRequest request) {
        String requestId = RequestIdHolder.getRequestId(request);
        int updated = schedulerMapper.update(null, new LambdaUpdateWrapper<ExecutionScheduler>()
            .set(ExecutionScheduler::getStatus, status)
            .eq(ExecutionScheduler::getSchedulerId, schedulerId)
        );
        if (updated == 0) {
            throw new BusinessException(400003, "调度任务不存在：" + schedulerId);
        }
        return ApiResponse.success(true, requestId);
    }
}
