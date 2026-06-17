package com.exchange.platform.collection.controller;

import java.util.List;

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

import com.exchange.platform.collection.api.request.tasks.StorageMappingRequest;
import com.exchange.platform.collection.api.request.tasks.TaskConfigSaveRequest;
import com.exchange.platform.collection.api.request.tasks.TaskStatusUpdateRequest;
import com.exchange.platform.collection.api.request.tasks.TaskSyncRequest;
import com.exchange.platform.collection.api.response.tasks.StorageMappingView;
import com.exchange.platform.collection.api.response.tasks.TaskConfigView;
import com.exchange.platform.collection.api.response.tasks.TaskDetailView;
import com.exchange.platform.collection.api.response.tasks.TaskTemplateVersionView;
import com.exchange.platform.collection.api.response.tasks.TaskTemplateView;
import com.exchange.platform.collection.service.CollectionTaskService;
import com.exchange.platform.common.core.page.PageResult;
import com.exchange.platform.common.core.result.ApiResponse;
import com.exchange.platform.common.server.support.RequestIdHolder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "采集任务管理", description = "M-01 任务配置、任务模板版本、数据类型版本与物理表之间的映射管理")
@RestController
@RequestMapping("/api/v1/collection/tasks")
public class CollectionTaskController {
    
    private static final Logger log = LoggerFactory.getLogger(CollectionTaskController.class);

    private final CollectionTaskService taskTemplateService;

    public CollectionTaskController(CollectionTaskService taskTemplateService) {
        this.taskTemplateService = taskTemplateService;
    }

    @Operation(summary = "查询采集任务列表", description = "支持按任务编码、名称，结果分页返回")
    @GetMapping
    public ApiResponse<PageResult<TaskTemplateView>> queryTaskList(
        @RequestParam(required = false) String taskCode,
        @RequestParam(required = false) String taskName,
        @RequestParam(defaultValue = "1")  Integer pageNo,
        @RequestParam(defaultValue = "20") Integer pageSize,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);
        log.info("[{}] [Controller] queryTaskList taskCode={}, taskName={}, pageNo={}, pageSize={}",
                requestId, taskCode, taskName, pageNo, pageSize);
        
        PageResult<TaskTemplateView> pageResult = taskTemplateService.queryTaskTemplates(
            taskCode, taskName, pageNo, pageSize
        );
        
        return ApiResponse.success(pageResult, requestId);
    }

    @Operation(summary = "查询采集任务详情", description = "查询指定任务编码的详情信息，包含任务模板基本信息、最新版本的配置信息、以及相关联的存储映射和列映射信息")
    @GetMapping("/{taskCode}")
    public ApiResponse<TaskDetailView> getTaskDetail(
        @PathVariable String taskCode,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);
        log.info("[{}] [Controller] getTaskDetail taskCode={}", requestId, taskCode);
        
        TaskDetailView detailView = taskTemplateService.getTaskDetail(taskCode);
        
        return ApiResponse.success(detailView, requestId);
    }

    @Operation(summary = "删除采集任务", description = "删除指定任务编码的采集任务，需要无配置时才能删除，同时删除相关的任务模板和版本信息")
    @DeleteMapping("/{taskCode}")
    public ApiResponse<Void> deleteTaskTemplate(
        @PathVariable String taskCode,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);
        log.info("[{}] [Controller] deleteTaskTemplate taskCode={}", requestId, taskCode);

        taskTemplateService.deleteTaskTemplate(taskCode);
        return ApiResponse.success(null, requestId);
    }

    @Operation(summary = "新增或更新采集任务配置", description = "根据请求参数新增或更新采集任务配置，包括数据主题、数据类型、状态、存储映射等，返回最新的配置视图")
    @PostMapping("/configs")
    public ApiResponse<TaskConfigView> saveTaskConfig(
        @Valid @RequestBody TaskConfigSaveRequest body,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);
        log.info("[{}] [Controller] saveTaskConfig taskConfigCode={}, taskCode={}",
                requestId, body.taskConfigCode(), body.taskCode());
        
        TaskConfigView configView = taskTemplateService.saveTaskConfig(body);
        return ApiResponse.success(configView, requestId);
    }

    @Operation(summary = "删除采集任务配置", description = "根据任务配置编码删除采集任务配置，同时删除相关联的存储映射、列映射等关联信息")
    @DeleteMapping("/configs/{taskConfigCode}")
    public ApiResponse<Void> deleteTaskConfigs(
        @PathVariable String taskConfigCode,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);
        log.info("[{}] [Controller] deleteTaskConfigs taskConfigCode={}", requestId, taskConfigCode);
        
        taskTemplateService.deleteTaskConfig(taskConfigCode);
        return ApiResponse.success(null, requestId);
    }

    @Operation(summary = "任务模板同步", description = "根据请求参数触发任务模板同步操作，确保本地数据与外部系统保持一致")
    @PostMapping("/sync")
    public ApiResponse<Void> syncTaskTemplates(
        @Valid @RequestBody TaskSyncRequest body,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);
        log.info("[{}] [Controller] syncTaskTemplate taskCode={}, syncMode={}", requestId, body.taskCode(), body.syncMode());

        taskTemplateService.syncTaskTemplates(body);
        return ApiResponse.success(null, requestId);
    }

    @Operation(summary = "查询任务模板版本列表", description = "查询指定任务编码的所有版本信息，用于版本管理展示")
    @GetMapping("/{taskCode}/versions")
    public ApiResponse<List<TaskTemplateVersionView>> listTemplateVersions(
        @PathVariable String taskCode,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);
        log.info("[{}] [Controller] listTemplateVersions taskCode={}", requestId, taskCode);
        
        List<TaskTemplateVersionView> versionViews = taskTemplateService.listTemplateVersions(taskCode);
        return ApiResponse.success(versionViews, requestId);
    }

    @Operation(summary = "查询任务模板版本详情", description = "查询指定任务编码和版本号的版本详情信息，用于版本详情展示")
    @GetMapping("/{taskCode}/versions/{versionNo}")
    public ApiResponse<TaskTemplateVersionView> getTemplateVersionDetail(
        @PathVariable String taskCode,
        @PathVariable Integer versionNo,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);
        log.info("[{}] [Controller] getTemplateVersionDetail taskCode={}, versionNo={}", requestId, taskCode, versionNo);

        TaskTemplateVersionView versionDetail = taskTemplateService.getTemplateVersionDetail(taskCode, versionNo);

        return ApiResponse.success(versionDetail, requestId);
    }

    @Operation(summary = "保存或更新存储映射关系", description = "保存或更新指定任务编码的存储映射关系")
    @PostMapping("/{taskCode}/storage-mapping")
    public ApiResponse<StorageMappingView> saveStorageMapping(
        @PathVariable String taskCode,
        @Valid @RequestBody StorageMappingRequest body,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);
        log.info("[{}] [Controller] saveStorageMapping taskCode={}, storageConfig={}", requestId, taskCode, body.taskConfigCode());

        StorageMappingView mappingView = taskTemplateService.saveStorageMapping(taskCode, body);
        return ApiResponse.success(mappingView, requestId);
    }

    @Operation(summary = "更新任务启停状态", description = "更新指定任务的启停状态，启用时需确保配置已确认")
    @PostMapping("/{taskCode}/status")
    public ApiResponse<TaskConfigView> updateTaskStatus(
        @PathVariable String taskCode,
        @Valid @RequestBody TaskStatusUpdateRequest body,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);
        log.info("[{}] [Controller] updateTaskStatus taskCode={}, status={}", requestId, taskCode, body.status());

        TaskConfigView configView = taskTemplateService.updateTaskStatus(taskCode, body);
        return ApiResponse.success(configView, requestId);
    }

    @Operation(summary = "确认存储映射关系", description = "确认指定任务编码的存储映射关系，并在必要时创建物理表")
    @PostMapping("/{taskCode}/storage-mapping/confirm")
    public ApiResponse<StorageMappingView> confirmStorageMapping(
        @PathVariable String taskCode,
        @Valid @RequestBody StorageMappingRequest body,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);
        log.info("[{}] [Controller] confirmStorageMapping taskCode={}, storageConfig={}", requestId, taskCode, body.taskConfigCode());

        StorageMappingView mappingView = taskTemplateService.confirmStorageMapping(taskCode, body);
        return ApiResponse.success(mappingView, requestId);
    }

    @Operation(summary = "查询存储映射关系", description = "查询指定物理库表的存储映射信息，用于配置页自动加载")
    @GetMapping("/storage-mapping")
    public ApiResponse<StorageMappingView> getStorageMapping(
        @RequestParam String physicalSchemaName,
        @RequestParam String physicalTableName,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);
        log.info("[{}] [Controller] getStorageMapping physicalSchemaName={}, physicalTableName={}", requestId, physicalSchemaName, physicalTableName);
        StorageMappingView mappingView = taskTemplateService.getStorageMapping(physicalSchemaName, physicalTableName);
        return ApiResponse.success(mappingView, requestId);
    }
    
}
