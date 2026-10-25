package com.exchange.platform.data.client;

import java.util.List;
import java.util.Map;

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
 * 业务服务直接注入该接口即可请求 FastAPI 任务处理接口。
 */
@FeignClient(
    name = "${task-handler.service-name:task-handler}",
    contextId = "dataServiceClient",
    url = "${task-handler.base-url:http://localhost:8000}",
    path = "/api/v1"
)
public interface DataServiceClient {

    /**
     * GET /api/v1/collection-task/task-templates/current?taskCode=xxx
     */
    @GetMapping("/collection-task/task-templates/current")
    List<TaskTemplateSync> fetchCurrentTaskTemplates(
        @RequestParam(value = "taskCode", required = false) String taskCode
    );

    /**
     * POST /api/v1/collection-task/task-runs/execute
     */
    @PostMapping("/collection-task/task-runs/execute")
    TaskExecuteResult executeTaskRun(@RequestBody TaskRunExecuteRequest executeRequest);

    /**
     * GET /api/v1/data-analytics/market-overview?tradeDate=xxx
     */
    @GetMapping("/data-analytics/market-overview")
    Map<String, Object> getMarketOverview(
        @RequestParam(value = "tradeDate", required = false) String tradeDate
    );

    /**
     * GET /api/v1/data-analytics/stock-detail?fullSymbol=xxx
     */
    @GetMapping("/data-analytics/stock-detail")
    Map<String, Object> getStockDetail(
        @RequestParam(value = "fullSymbol") String fullSymbol,
        @RequestParam(value = "tradeDate", required = false) String tradeDate,
        @RequestParam(value = "includeCharts", required = false, defaultValue = "True") boolean includeCharts,
        @RequestParam(value = "includeFinancial", required = false, defaultValue = "True") boolean includeFinancial,
        @RequestParam(value = "includeHistory", required = false, defaultValue = "True") boolean includeHistory
    );

    /**
     * GET /api/v1/data-analytics/index-detail?fullSymbol=xxx
     */
    @GetMapping("/data-analytics/index-detail")
    Map<String, Object> getIndexDetail(
        @RequestParam(value = "fullSymbol") String fullSymbol,
        @RequestParam(value = "tradeDate", required = false) String tradeDate
    );

    /**
     * GET /api/v1/data-analytics/index-detail/market?fullSymbol=xxx&tradeDate=xxx&historyLimit=xxx
     */
    @GetMapping("/data-analytics/index-detail/market")
    Map<String, Object> getIndexDetailMarket(
        @RequestParam(value = "fullSymbol") String fullSymbol,
        @RequestParam(value = "tradeDate", required = false) String tradeDate,
        @RequestParam(value = "historyLimit", required = false, defaultValue = "250") int historyLimit
    );

    /**
     * GET /api/v1/data-analytics/index-detail/constituents?fullSymbol=xxx&tradeDate=xxx
     */
    @GetMapping("/data-analytics/index-detail/constituents")
    Map<String, Object> getIndexConstituents(
        @RequestParam(value = "fullSymbol") String fullSymbol,
        @RequestParam(value = "tradeDate", required = false) String tradeDate
    );

    /**
     * GET /api/v1/data-analytics/index-detail/constituent-page?fullSymbol=xxx&tradeDate=xxx&page=xxx&pageSize=xxx
     */
    @GetMapping("/data-analytics/index-detail/constituent-page")
    List<Map<String, Object>> getIndexConstituentPage(
        @RequestParam(value = "fullSymbol") String fullSymbol,
        @RequestParam(value = "tradeDate", required = false) String tradeDate,
        @RequestParam(value = "page", required = false, defaultValue = "1") int page,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") int pageSize
    );
}
