package com.exchange.platform.data.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.exchange.platform.data.client.dto.TaskExecuteResult;
import com.exchange.platform.data.client.dto.TaskRunExecuteRequest;
import com.exchange.platform.data.client.dto.TaskTemplateSync;

/**
 * data-service-client 最小 OpenFeign 客户端。
 * <p>
 * 业务服务直接注入该接口即可请求 FastAPI 任务处理接口。
 * </p>
 */
@FeignClient(
    name = "${task-handler.service-name:task-handler}",
    contextId = "dataServiceClient",
    url = "${task-handler.base-url:http://localhost:8000}",
    path = "/api/v1/collection-task"
)
public interface DataServiceClient {

    /**
     * GET /api/v1/collection-task/task-templates/current?taskCode=xxx
     */
    @GetMapping("/task-templates/current")
    List<TaskTemplateSync> fetchCurrentTaskTemplates(
        @RequestParam(value = "taskCode", required = false) String taskCode
    );

    /**
     * POST /api/v1/collection-task/task-runs/execute
     */
    @PostMapping("/task-runs/execute")
    TaskExecuteResult executeTaskRun(@RequestBody TaskRunExecuteRequest executeRequest);
}
