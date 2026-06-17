package com.exchange.platform.collection.controller;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

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

import com.exchange.platform.collection.api.request.execution.TaskRunCreateRequest;
import com.exchange.platform.collection.api.request.execution.TaskRunRequest;
import com.exchange.platform.collection.api.response.execution.TaskRunDetailView;
import com.exchange.platform.collection.api.response.execution.TaskRunView;
import com.exchange.platform.collection.service.ExecutionService;
import com.exchange.platform.common.core.page.PageResult;
import com.exchange.platform.common.core.result.ApiResponse;
import com.exchange.platform.common.server.support.RequestIdHolder;
import com.exchange.platform.data.client.dto.TaskExecuteResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "采集执行控制", description = "执行记录创建、触发、查询")
@RestController
@RequestMapping("/api/v1/collection/task-runs")
public class TaskRunController {
    
    private static final Logger log = LoggerFactory.getLogger(TaskRunController.class);

    private final ExecutionService executionService;

    public TaskRunController(ExecutionService executionService) {
        this.executionService = executionService;
    }

    @Operation(summary = "创建执行记录", description = "读取当前已确认任务配置，创建执行记录并生成执行配置快照")
    @PostMapping
    public ApiResponse<TaskRunView> createTaskRun(
        @Valid @RequestBody TaskRunCreateRequest body,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);
        log.info("[{}] [Controller] createTaskRun taskConfigCode={}", requestId, body.taskConfigCode());

        TaskRunView taskRun = executionService.createTaskRun(body);
        return ApiResponse.success(taskRun, requestId);
    }

    @Operation(summary = "触发执行", description = "触发已创建的执行记录进入执行链路，成功后归集结果摘要并生成数据集，当前是同步执行，后续会改为异步执行")
    @PostMapping("/{runId}/execute")
    public ApiResponse<TaskRunView> executeTaskRun(
        @PathVariable String runId,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);
        log.info("[{}] [Controller] executeTaskRun taskRunId={}", requestId, runId);

        TaskRunView taskRun = executionService.executeTaskRun(runId);
        return ApiResponse.success(taskRun, requestId);
    }

    @Operation(summary = "删除执行记录", description = "删除指定的执行记录")
    @DeleteMapping("/{runId}")
    public ApiResponse<Boolean> deleteTaskRun(
        @PathVariable String runId,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);
        log.info("[{}] [Controller] deleteTaskRun runId={}", requestId, runId);

        executionService.deleteTaskRun(runId);
        return ApiResponse.success(true, requestId);
    }

    @Operation(summary = "查询执行记录列表", description = "支持按任务、状态、维度和时间分页查询执行记录")
    @GetMapping
    public ApiResponse<PageResult<TaskRunView>> queryTaskRuns(
        @RequestParam(required = false) String taskCode,
        @RequestParam(required = false) String runStatus,
        @RequestParam(required = false) String triggerType,
        @RequestParam(required = false) String startTime,
        @RequestParam(required = false) String endTime,
        @RequestParam(defaultValue = "1") Integer pageNo,
        @RequestParam(defaultValue = "20") Integer pageSize,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);
        log.info("[{}] [Controller] queryTaskRuns taskCode={}, runStatus={}", requestId, taskCode, runStatus);

        TaskRunRequest params = new TaskRunRequest(null, taskCode, runStatus, triggerType, startTime, endTime, pageNo, pageSize);
        return ApiResponse.success(executionService.queryTaskRuns(params), requestId);
    }

    @Operation(summary = "查询执行记录详情", description = "查询执行记录的详细信息，包括执行配置、结果摘要和数据集信息")
    @GetMapping("/{runId}/detail")
    public ApiResponse<TaskRunDetailView> getTaskRunDetail(
        @PathVariable String runId,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);
        log.info("[{}] [Controller] getTaskRunDetail runId={}", requestId, runId);

        return ApiResponse.success(executionService.getTaskRunDetail(runId), requestId);
    }

    @Operation(summary = "异步执行回调（任务处理层调用）", description = "任务处理层在异步执行完成后回调此接口，应用服务层归集结果、日志、问题与异常")
    @PostMapping("/callback")
    public ApiResponse<Boolean> taskRunCallback(
        @RequestBody TaskExecuteResult body,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);
        log.info("[{}] [Controller] taskRunCallback runId={} status={}", requestId, body.runId(), body.status());

        executionService.processTaskRunCallback(body);
        return ApiResponse.success(true, requestId);
    }

}
