package com.exchange.platform.analytics.controller;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.exchange.platform.analytics.api.request.*;
import com.exchange.platform.analytics.api.response.*;
import com.exchange.platform.analytics.api.enums.IndexCategory;
import com.exchange.platform.analytics.api.enums.StockMarketType;
import com.exchange.platform.analytics.api.enums.MasterDataType;
import com.exchange.platform.analytics.api.enums.TriggerType;
import com.exchange.platform.analytics.service.MasterDataService;
import com.exchange.platform.analytics.service.SourceBindingService;
import com.exchange.platform.analytics.service.masterdata.MasterDataTypeRegistry;
import com.exchange.platform.common.core.enums.MarketAssetType;
import com.exchange.platform.common.core.page.PageResult;
import com.exchange.platform.common.core.result.ApiResponse;
import com.exchange.platform.common.server.support.RequestIdHolder;

@Tag(name = "分析MasterData")
@RestController
@RequestMapping("/api/v1/analytics/master-data")
public class MasterDataController {

    private static final Logger log = LoggerFactory.getLogger(MasterDataController.class);

    private final MasterDataService service;
    private final SourceBindingService sourceBindingService;
    private final MasterDataTypeRegistry registry;

    public MasterDataController(MasterDataService service, SourceBindingService sourceBindingService,
            MasterDataTypeRegistry registry) {
        this.service = service;
        this.sourceBindingService = sourceBindingService;
        this.registry = registry;
    }

    @GetMapping("/registry/types")
    public ApiResponse<List<MasterDataTypeView>> registry(HttpServletRequest request) {

        log.info("[Controller] Fetching master data registry types");

        return ApiResponse.success(registry.list().stream().map(item -> new MasterDataTypeView(item.typeCode(),
                item.displayName(), item.domain(), item.category(), item.fields())).toList(), requestId(request));
    }

    @GetMapping("/registry/types/{typeCode}")
    public ApiResponse<MasterDataTypeView> registryDetail(@PathVariable MasterDataType typeCode,
            HttpServletRequest request) {
        log.info("[Controller] Fetching master data registry detail for type: {}", typeCode);
        var item = registry.get(typeCode);
        return ApiResponse.success(new MasterDataTypeView(item.typeCode(), item.displayName(), item.domain(),
                item.category(), item.fields()), requestId(request));
    }

    @GetMapping("/published-types")
    public ApiResponse<List<PublishedMasterDataTypeView>> publishedTypes(
            @RequestParam(required = false) String keyword, HttpServletRequest request) {
        log.info("[Controller] Listing published master data types with keyword: {}", keyword);
        return ApiResponse.success(service.publishedTypes(keyword), requestId(request));
    }

    @GetMapping("/published-types/{typeCode}")
    public ApiResponse<PublishedMasterDataTypeDetailView> publishedTypeDetail(@PathVariable MasterDataType typeCode,
            HttpServletRequest request) {
        log.info("[Controller] Fetching published master data type detail: {}", typeCode);
        return ApiResponse.success(service.publishedTypeDetail(typeCode), requestId(request));
    }

    @GetMapping("/published-types/{typeCode}/records")
    public ApiResponse<PageResult<Map<String, Object>>> publishedRows(@PathVariable MasterDataType typeCode,
            @RequestParam(defaultValue = "1") int pageNo, @RequestParam(defaultValue = "20") int pageSize,
            HttpServletRequest request) {
        log.info("[Controller] Listing published master data rows: type={}, pageNo={}, pageSize={}", typeCode,
                pageNo, pageSize);
        return ApiResponse.success(service.publishedRows(typeCode, pageNo, pageSize), requestId(request));
    }

    @GetMapping("/sources")
    public ApiResponse<PageResult<SourceBindingView>> sources(@RequestParam(required = false) String keyword,
            @RequestParam(required = false) String datasetCode, @RequestParam(required = false) String masterDataType,
            @RequestParam(required = false) String triggerMode, @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) String readiness, @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize, HttpServletRequest request) {
        log.info(
                "[Controller] Listing source bindings with filters - keyword: {}, datasetCode: {}, masterDataType: {}, triggerMode: {}, enabled: {}, readiness: {}, pageNo: {}, pageSize: {}",
                keyword, datasetCode, masterDataType, triggerMode, enabled, readiness, pageNo, pageSize);
        return ApiResponse.success(sourceBindingService.list(keyword, datasetCode, masterDataType, triggerMode, enabled,
                readiness, pageNo, pageSize), requestId(request));
    }

    @GetMapping("/sources/{id}")
    public ApiResponse<SourceBindingView> sourceDetail(@PathVariable Long id, HttpServletRequest request) {
        log.info("[Controller] Fetching source binding detail for ID: {}", id);
        return ApiResponse.success(sourceBindingService.detail(id), requestId(request));
    }

    @PostMapping("/sources")
    public ApiResponse<SourceBindingView> saveSource(@RequestParam(required = false) Long id,
            @RequestBody @javax.validation.Valid SourceBindingSaveRequest body, HttpServletRequest request) {
        log.info("[Controller] Saving source binding with ID: {} and body: {}", id, body);
        return ApiResponse.success(sourceBindingService.save(id, body), requestId(request));
    }

    @GetMapping("/sources/{id}/mappings")
    public ApiResponse<List<SourceFieldMappingView>> sourceMappings(@PathVariable Long id, HttpServletRequest request) {
        log.info("[Controller] Fetching source field mappings for source ID: {}", id);
        return ApiResponse.success(sourceBindingService.mappings(id), requestId(request));
    }

    @PutMapping("/sources/{id}/mappings")
    public ApiResponse<List<SourceFieldMappingView>> saveSourceMappings(@PathVariable Long id,
            @RequestBody @javax.validation.Valid SourceFieldMappingSaveRequest body, HttpServletRequest request) {
        log.info("[Controller] Saving source field mappings for source ID: {} with body: {}", id, body);
        return ApiResponse.success(sourceBindingService.saveMappings(id, body), requestId(request));
    }

    @GetMapping("/sources/{id}/readiness")
    public ApiResponse<ReadinessView> sourceReadiness(@PathVariable Long id, HttpServletRequest request) {
        log.info("[Controller] Checking source readiness for ID: {}", id);
        return ApiResponse.success(sourceBindingService.readiness(id), requestId(request));
    }

    @PutMapping("/sources/{id}/enabled")
    public ApiResponse<Boolean> sourceEnabled(@PathVariable Long id, @RequestParam boolean enabled,
            HttpServletRequest request) {
        log.info("[Controller] Updating source enabled status for ID: {} to {}", id, enabled);
        sourceBindingService.updateEnabled(id, enabled);
        return ApiResponse.success(true, requestId(request));
    }

    @PostMapping("/sources/{id}/publish")
    public ApiResponse<SyncBatchView> publishSource(@PathVariable Long id, @RequestBody BindingPublishRequest body, HttpServletRequest request) {

        log.info("[Controller] Publishing source binding with ID: {} and body: {}", id, body);
        return ApiResponse.success(service.publishBinding(id, body.taskRunId(), body.syncType(), TriggerType.MANUAL), requestId(request));
    }

    @GetMapping("/search")
    public ApiResponse<List<Map<String, Object>>> search(@RequestParam String keyword,
            @RequestParam(required = false) MarketAssetType marketAssetType,
            @RequestParam(defaultValue = "20") int limit, HttpServletRequest request) {
        log.info("[Controller] Searching master data with keyword: {}, marketAssetType: {}, limit: {}", keyword, marketAssetType, limit);
        return ApiResponse.success(service.search(keyword, marketAssetType, limit), requestId(request));
    }

    @GetMapping
    public ApiResponse<PageResult<Map<String, Object>>> list(@RequestParam(required = false) String keyword,
            @RequestParam(required = false) MarketAssetType marketAssetType,
            @RequestParam(required = false) StockMarketType marketType,
            @RequestParam(required = false) String exchangeCode, @RequestParam(required = false) String publisherCode,
            @RequestParam(required = false) IndexCategory indexCategory,
            @RequestParam(required = false) String schemeCode, @RequestParam(required = false) Integer level,
            @RequestParam(defaultValue = "1") Integer pageNo, @RequestParam(defaultValue = "20") Integer pageSize,
            HttpServletRequest request) {
        log.info("[Controller] Listing master data with filters - keyword: {}, marketAssetType: {}, marketType: {}, exchangeCode: {}, publisherCode: {}, indexCategory: {}, schemeCode: {}, level: {}, pageNo: {}, pageSize: {}",
                keyword, marketAssetType, marketType, exchangeCode, publisherCode, indexCategory, schemeCode, level, pageNo, pageSize);
        return ApiResponse.success(service.list(keyword, marketAssetType, marketType, exchangeCode, publisherCode, indexCategory, schemeCode, level, pageNo, pageSize), requestId(request));
    }

    @GetMapping("/{marketAssetType}/{assetCode}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable MarketAssetType marketAssetType,
            @PathVariable String assetCode, HttpServletRequest request) {
        log.info("[Controller] Fetching master data detail for marketAssetType: {}, assetCode: {}", marketAssetType, assetCode);
        return ApiResponse.success(service.detail(marketAssetType, assetCode), requestId(request));
    }

    @GetMapping("/sync-batches")
    public ApiResponse<PageResult<SyncBatchView>> batches(@RequestParam(required = false) String masterDataType,
            @RequestParam(required = false) String datasetCode, @RequestParam(required = false) String status,
            @RequestParam(required = false) Long bindingId, @RequestParam(required = false) String triggerType,
            @RequestParam(defaultValue = "1") int pageNo, @RequestParam(defaultValue = "20") int pageSize,
            HttpServletRequest request) {
        log.info("[Controller] Listing sync batches with filters - masterDataType: {}, datasetCode: {}, status: {}, bindingId: {}, triggerType: {}, pageNo: {}, pageSize: {}",
                masterDataType, datasetCode, status, bindingId, triggerType, pageNo, pageSize);
        return ApiResponse.success(service.batches(masterDataType, datasetCode, status, bindingId, triggerType, pageNo, pageSize), requestId(request));
    }

    @PostMapping("/sync-batches/publish")
    public ApiResponse<SyncBatchView> publish(@RequestBody SyncPublishRequest body, HttpServletRequest request) {
        
        log.info("[Controller] Publishing sync batch with body: {}", body);
        return ApiResponse.success(service.publish(body.masterDataType(), body.sourceCode(), body.taskRunId(),
                body.datasetCode(), body.syncType()), requestId(request));
    }

    @PostMapping("/sync-batches/{batchId}/republish")
    public ApiResponse<SyncBatchView> republish(@PathVariable Long batchId, HttpServletRequest request) {
        log.info("[Controller] Republishing sync batch with ID: {}", batchId);
        return ApiResponse.success(service.republish(batchId), requestId(request));
    }

    private String requestId(HttpServletRequest request) {
        return RequestIdHolder.getRequestId(request);
    }
}
