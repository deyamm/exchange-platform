package com.exchange.platform.collection.controller;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.exchange.platform.collection.api.request.execution.TaskRunRequest;
import com.exchange.platform.collection.api.response.monitor.ExceptionRecordView;
import com.exchange.platform.collection.api.response.monitor.ProblemRecordView;
import com.exchange.platform.collection.api.response.monitor.RunLogView;
import com.exchange.platform.collection.service.MonitorService;
import com.exchange.platform.common.core.page.PageResult;
import com.exchange.platform.common.core.result.ApiResponse;
import com.exchange.platform.common.server.support.RequestIdHolder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "异常与监控", description = "运行日志、问题数据、异常记录、trace等")
@RestController
@RequestMapping("/api/v1/collection/monitor")
public class MonitorController {
    private static final Logger log = LoggerFactory.getLogger(MonitorController.class);

    private final MonitorService monitorService;

    public MonitorController(MonitorService monitorService) {
        this.monitorService = monitorService;
    }

    @Operation(summary = "查询运行日志", description = "根据执行记录ID查询对应的运行日志，支持分页")
    @GetMapping("/run-logs")
    public ApiResponse<PageResult<RunLogView>> queryRunLogs(
        @RequestParam(required = false) String runId,
        @RequestParam(required = false) String taskCode,
        @RequestParam(required = false) String logLevel,
        @RequestParam(required = false) String startTime,
        @RequestParam(required = false) String endTime,
        @RequestParam(defaultValue = "1") Integer pageNo,
        @RequestParam(defaultValue = "20") Integer pageSize,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);
        log.info("[{}] [Controller] queryRunLogs runId={}", requestId, runId);

        TaskRunRequest params = new TaskRunRequest(runId, taskCode, null, null, startTime, endTime, pageNo, pageSize);
        return ApiResponse.success(monitorService.queryRunLogs(params), requestId);
    }

    @Operation(summary = "查询问题记录", description = "根据执行记录ID查询对应的问题数据，支持分页")
    @GetMapping("/problem-records")
    public ApiResponse<PageResult<ProblemRecordView>> queryProblemRecords(
        @RequestParam(required = false) String runId,
        @RequestParam(required = false) String taskCode,
        @RequestParam(required = false) String startTime,
        @RequestParam(required = false) String endTime,
        @RequestParam(defaultValue = "1") Integer pageNo,
        @RequestParam(defaultValue = "20") Integer pageSize,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);
        log.info("[{}] [Controller] queryProblemRecords runId={}", requestId, runId);

        TaskRunRequest params = new TaskRunRequest(runId, taskCode, null, null, startTime, endTime, pageNo, pageSize);
        return ApiResponse.success(monitorService.queryProblemRecords(params), requestId);
    }

    @Operation(summary = "查询异常记录", description = "根据执行记录ID查询对应的异常记录，支持分页")
    @GetMapping("/exception-records")
    public ApiResponse<PageResult<ExceptionRecordView>> queryExceptionRecords(
        @RequestParam(required = false) String runId,
        @RequestParam(required = false) String taskCode,
        @RequestParam(required = false) String startTime,
        @RequestParam(required = false) String endTime,
        @RequestParam(defaultValue = "1") Integer pageNo,
        @RequestParam(defaultValue = "20") Integer pageSize,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);
        log.info("[{}] [Controller] queryExceptionRecords runId={}", requestId, runId);

        TaskRunRequest params = new TaskRunRequest(runId, taskCode, null, null, startTime, endTime, pageNo, pageSize);
        return ApiResponse.success(monitorService.queryExceptionRecords(params), requestId);
    }

    @Operation(summary = "查询链路追踪", description = "根据 runId 聚合查询执行配置快照、模板版本、结果摘要和运行留痕索引")
    @GetMapping("/{runId}/trace")
    public ApiResponse<Object> queryExecutionTrace(
        @PathVariable String runId,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);
        log.info("[{}] [Controller] queryExecutionTrace runId={}", requestId, runId);

        Object traceData = monitorService.getTrace(runId);
        return ApiResponse.success(traceData, requestId);
    }
}
