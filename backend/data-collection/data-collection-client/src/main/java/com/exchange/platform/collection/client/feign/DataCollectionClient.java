package com.exchange.platform.collection.client.feign;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.exchange.platform.collection.api.request.execution.MetricDatasetQueryRequest;
import com.exchange.platform.collection.api.response.execution.MetricDatasetConfigView;
import com.exchange.platform.collection.api.response.execution.MetricDatasetExecutionView;
import com.exchange.platform.common.core.result.ApiResponse;

/**
 * data-collection 服务的只读 Feign 客户端。
 */
@FeignClient(
    name = "${data-collection.service-name:data-collection}",
    contextId = "dataCollectionClient",
    url = "${collection-metadata.base-url:http://localhost:8082}",
    path = "/api/v1/collection/monitor/datasets"
)
public interface DataCollectionClient {

    /** 批量查询指标配置所需的数据集元数据。 */
    @PostMapping("/metric-configuration/query")
    ApiResponse<List<MetricDatasetConfigView>> queryMetricDatasetConfigurations(
        @RequestBody MetricDatasetQueryRequest request
    );

    /** 批量查询指标执行所需的数据集读取绑定。 */
    @PostMapping("/metric-execution/query")
    ApiResponse<List<MetricDatasetExecutionView>> queryMetricDatasetExecutions(
        @RequestBody MetricDatasetQueryRequest request
    );
}
