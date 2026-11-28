package com.exchange.platform.analytics.client;

import java.util.List;
import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.exchange.platform.analytics.api.request.PublishedMasterDataQueryRequest;
import com.exchange.platform.analytics.api.response.PublishedMasterDataTypeDetailView;
import com.exchange.platform.analytics.api.response.PublishedMasterDataTypeView;
import com.exchange.platform.common.core.page.PageResult;
import com.exchange.platform.common.core.result.ApiResponse;

/** Collection-side read client for published master data. */
@FeignClient(
    name = "${data-analytics.service-name:data-analytics}", 
    contextId = "masterDataClient",
    url = "${analytics.base-url:http://localhost:8081}", 
    path = "/api/v1/analytics/master-data"
)

public interface MasterDataClient {
    @GetMapping("/published-types")
    ApiResponse<List<PublishedMasterDataTypeView>> publishedTypes();

    @GetMapping("/published-types/{typeCode}")
    ApiResponse<PublishedMasterDataTypeDetailView> publishedTypeDetail(@PathVariable("typeCode") String typeCode);

    @PostMapping("/search")
    ApiResponse<PageResult<Map<String, Object>>> publishedRows(@RequestBody PublishedMasterDataQueryRequest request);
}
