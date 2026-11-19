package com.exchange.platform.analytics.controller;

import java.time.LocalDateTime;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import com.exchange.platform.analytics.api.request.*;
import com.exchange.platform.analytics.api.response.*;
import com.exchange.platform.analytics.api.enums.IndexCategory;
import com.exchange.platform.analytics.api.enums.StockMarketType;
import com.exchange.platform.analytics.api.enums.MasterDataType;
import com.exchange.platform.analytics.api.enums.TriggerType;
import com.exchange.platform.analytics.service.masterdata.MasterDataService;
import com.exchange.platform.analytics.service.masterdata.MasterDataTypeRegistry;
import com.exchange.platform.analytics.service.masterdata.SourceBindingService;
import com.exchange.platform.common.core.enums.MarketAssetType;
import com.exchange.platform.common.core.page.PageResult;
import com.exchange.platform.common.core.result.ApiResponse;
import com.exchange.platform.common.server.support.RequestIdHolder;

@RestController
@RequestMapping("/api/v1/analytics/master-data")
public class MasterDataController {
    private final MasterDataService service;
    private final SourceBindingService sourceBindingService;
    private final MasterDataTypeRegistry registry;
    public MasterDataController(MasterDataService service, SourceBindingService sourceBindingService, MasterDataTypeRegistry registry) {
        this.service = service; this.sourceBindingService = sourceBindingService; this.registry = registry;
    }

    @GetMapping("/registry/types")
    public ApiResponse<List<MasterDataTypeView>> registry(HttpServletRequest request) {
        return ApiResponse.success(registry.list().stream().map(item -> new MasterDataTypeView(item.typeCode(), item.displayName(), item.domain(), item.category(), item.fields())).toList(), requestId(request));
    }

    @GetMapping("/registry/types/{typeCode}")
    public ApiResponse<MasterDataTypeView> registryDetail(@PathVariable MasterDataType typeCode, HttpServletRequest request) {
        var item = registry.get(typeCode);
        return ApiResponse.success(new MasterDataTypeView(item.typeCode(), item.displayName(), item.domain(), item.category(), item.fields()), requestId(request));
    }

    @GetMapping("/sources")
    public ApiResponse<PageResult<SourceBindingView>> sources(@RequestParam(required = false) String keyword, @RequestParam(required = false) String datasetCode,
            @RequestParam(required = false) String masterDataType, @RequestParam(required = false) String triggerMode, @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) String readiness, @RequestParam(defaultValue = "1") int pageNo, @RequestParam(defaultValue = "20") int pageSize, HttpServletRequest request) {
        return ApiResponse.success(sourceBindingService.list(keyword, datasetCode, masterDataType, triggerMode, enabled, readiness, pageNo, pageSize), requestId(request));
    }

    @GetMapping("/sources/{id}")
    public ApiResponse<SourceBindingView> sourceDetail(@PathVariable Long id, HttpServletRequest request) { return ApiResponse.success(sourceBindingService.detail(id), requestId(request)); }

    @PostMapping("/sources")
    public ApiResponse<SourceBindingView> saveSource(@RequestParam(required = false) Long id, @RequestBody @javax.validation.Valid SourceBindingSaveRequest body, HttpServletRequest request) { return ApiResponse.success(sourceBindingService.save(id, body), requestId(request)); }

    @GetMapping("/sources/{id}/mappings")
    public ApiResponse<List<SourceFieldMappingView>> sourceMappings(@PathVariable Long id, HttpServletRequest request) { return ApiResponse.success(sourceBindingService.mappings(id), requestId(request)); }

    @PutMapping("/sources/{id}/mappings")
    public ApiResponse<List<SourceFieldMappingView>> saveSourceMappings(@PathVariable Long id, @RequestBody @javax.validation.Valid SourceFieldMappingSaveRequest body, HttpServletRequest request) { return ApiResponse.success(sourceBindingService.saveMappings(id, body), requestId(request)); }

    @GetMapping("/sources/{id}/readiness")
    public ApiResponse<ReadinessView> sourceReadiness(@PathVariable Long id, HttpServletRequest request) { return ApiResponse.success(sourceBindingService.readiness(id), requestId(request)); }

    @PutMapping("/sources/{id}/enabled")
    public ApiResponse<Boolean> sourceEnabled(@PathVariable Long id, @RequestParam boolean enabled, HttpServletRequest request) { sourceBindingService.updateEnabled(id, enabled); return ApiResponse.success(true, requestId(request)); }

    @PostMapping("/sources/{id}/publish")
    public ApiResponse<SyncBatchView> publishSource(@PathVariable Long id, @RequestBody BindingPublishRequest body, HttpServletRequest request) { return ApiResponse.success(sourceBindingService.publish(id, body.taskRunId(), body.syncType(), TriggerType.MANUAL), requestId(request)); }

    @GetMapping("/search")
    public ApiResponse<List<MarketAssetView>> search(@RequestParam String keyword, @RequestParam(required = false) MarketAssetType marketAssetType,
            @RequestParam(defaultValue = "20") int limit, HttpServletRequest request) {
        return ApiResponse.success(service.search(keyword, marketAssetType, limit), requestId(request));
    }

    @GetMapping
    public ApiResponse<PageResult<MarketAssetView>> list(@RequestParam(required = false) String keyword, @RequestParam(required = false) MarketAssetType marketAssetType,
            @RequestParam(required = false) StockMarketType marketType, @RequestParam(required = false) String exchangeCode,
            @RequestParam(required = false) String publisherCode, @RequestParam(required = false) IndexCategory indexCategory,
            @RequestParam(required = false) String schemeCode, @RequestParam(required = false) Integer level,
            @RequestParam(defaultValue = "1") Integer pageNo, @RequestParam(defaultValue = "20") Integer pageSize, HttpServletRequest request) {
        return ApiResponse.success(service.list(new MarketAssetListRequest(keyword, marketAssetType, marketType, exchangeCode, publisherCode, indexCategory, schemeCode, level, pageNo, pageSize)), requestId(request));
    }

    @GetMapping("/{marketAssetType}/{assetCode}")
    public ApiResponse<MarketAssetView> detail(@PathVariable MarketAssetType marketAssetType, @PathVariable String assetCode, HttpServletRequest request) {
        return ApiResponse.success(service.detail(marketAssetType, assetCode), requestId(request));
    }

    @PostMapping("/batch-detail")
    public ApiResponse<List<MarketAssetView>> batch(@RequestBody List<MarketAssetRef> assets, HttpServletRequest request) {
        return ApiResponse.success(service.batchDetail(assets), requestId(request));
    }

    @GetMapping("/INDEX/{assetCode}/members")
    public ApiResponse<List<AssetMemberView>> members(@PathVariable String assetCode, @RequestParam(required = false) LocalDateTime asOfTime, HttpServletRequest request) {
        return ApiResponse.success(service.members(assetCode, asOfTime), requestId(request));
    }

    @PostMapping("/scope/resolve")
    public ApiResponse<List<MarketAssetView>> scope(@RequestBody ScopeResolveRequest body, HttpServletRequest request) {
        return ApiResponse.success(service.resolveScope(body), requestId(request));
    }

    @GetMapping("/sync-batches")
    public ApiResponse<PageResult<SyncBatchView>> batches(@RequestParam(required = false) String masterDataType, @RequestParam(required = false) String datasetCode,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long bindingId, @RequestParam(required = false) String triggerType,
            @RequestParam(defaultValue = "1") int pageNo, @RequestParam(defaultValue = "20") int pageSize, HttpServletRequest request) {
        return ApiResponse.success(service.batches(masterDataType, datasetCode, status, bindingId, triggerType, pageNo, pageSize), requestId(request));
    }

    @PostMapping("/sync-batches/publish")
    public ApiResponse<SyncBatchView> publish(@RequestBody SyncPublishRequest body, HttpServletRequest request) {
        return ApiResponse.success(service.publish(body.masterDataType(), body.sourceCode(), body.taskRunId(), body.datasetCode(), body.syncType()), requestId(request));
    }

    @PostMapping("/sync-batches/{batchId}/republish")
    public ApiResponse<SyncBatchView> republish(@PathVariable Long batchId, HttpServletRequest request) {
        return ApiResponse.success(service.republish(batchId), requestId(request));
    }

    private String requestId(HttpServletRequest request) { return RequestIdHolder.getRequestId(request); }
}
