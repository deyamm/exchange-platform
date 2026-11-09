package com.exchange.platform.collection.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.exchange.platform.collection.api.request.execution.DatasetMetadataUpdateRequest;
import com.exchange.platform.collection.api.request.execution.DatasetMetricBindingQueryRequest;
import com.exchange.platform.collection.api.response.execution.DatasetDetailView;
import com.exchange.platform.collection.api.response.execution.DatasetMetricBindingView;
import com.exchange.platform.collection.api.response.execution.DatasetMetadataView;
import com.exchange.platform.collection.api.response.execution.DatasetView;
import com.exchange.platform.collection.service.DatasetService;
import com.exchange.platform.common.core.page.PageResult;
import com.exchange.platform.common.core.result.ApiResponse;
import com.exchange.platform.common.server.support.RequestIdHolder;
import com.exchange.platform.common.core.enums.AssetType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 数据集查询与元数据维护接口。
 * <p>提供数据集目录、详情、字段结构、来源信息及扩展元数据维护能力。</p>
 */
@RestController
@RequestMapping("/api/v1/collection/monitor/datasets")
@Tag(name = "数据集管理", description = "数据集目录、详情、字段结构和扩展元数据维护")
public class DatasetController {
    private static final Logger log = LoggerFactory.getLogger(DatasetController.class);

    private final DatasetService datasetService;

    /** 构造数据集控制器。 */
    public DatasetController(DatasetService datasetService) {
        this.datasetService = datasetService;
    }

    /** 分页查询数据集目录。 */
    @Operation(summary = "查询数据集目录", description = "按编码、名称、主题、类型、状态和最近成功时间分页查询数据集")
    @GetMapping
    public ApiResponse<PageResult<DatasetView>> query(
        @RequestParam(required = false) String datasetCode, @RequestParam(required = false) String datasetName,
        @RequestParam(required = false) String dataTopicCode, @RequestParam(required = false) String dataTypeCode,
        @RequestParam(required = false) AssetType assetType,
        @RequestParam(required = false) Integer datasetStatus, @RequestParam(required = false) String taskConfigCode,
        @RequestParam(required = false) String startTime, @RequestParam(required = false) String endTime,
        @RequestParam(defaultValue = "1") Integer pageNo, @RequestParam(defaultValue = "20") Integer pageSize,
        HttpServletRequest request) {
        String requestId = RequestIdHolder.getRequestId(request);
        log.info("[{}] [Controller] query datasets, datasetCode={}, dataTypeCode={}, assetType={}, datasetStatus={}, pageNo={}, pageSize={}",
            requestId, datasetCode, dataTypeCode, assetType, datasetStatus, pageNo, pageSize);
        return ApiResponse.success(datasetService.query(datasetCode, datasetName, dataTopicCode, dataTypeCode, assetType, datasetStatus, taskConfigCode, startTime, endTime, pageNo, pageSize), requestId);
    }

    /** 查询数据集详情。 */
    @Operation(summary = "查询数据集详情", description = "查询数据集主档、元数据、字段结构和最近执行信息")
    @GetMapping("/{datasetCode}")
    public ApiResponse<DatasetDetailView> detail(@PathVariable String datasetCode, HttpServletRequest request) {
        String requestId = RequestIdHolder.getRequestId(request);
        log.info("[{}] [Controller] get dataset detail, datasetCode={}", requestId, datasetCode);
        return ApiResponse.success(datasetService.getDetail(datasetCode), requestId);
    }

    /** 查询数据集扩展元数据。 */
    @Operation(summary = "查询数据集元数据", description = "查询数据集的业务、管理和存储扩展元数据")
    @GetMapping("/{datasetCode}/metadata")
    public ApiResponse<List<DatasetMetadataView>> metadata(@PathVariable String datasetCode, HttpServletRequest request) {
        String requestId = RequestIdHolder.getRequestId(request);
        log.info("[{}] [Controller] get dataset metadata, datasetCode={}", requestId, datasetCode);
        return ApiResponse.success(datasetService.getMetadata(datasetCode), requestId);
    }

    /** 更新数据集可维护的扩展元数据。 */
    @Operation(summary = "更新数据集元数据", description = "维护数据集名称、说明及非存储类扩展元数据")
    @PutMapping("/{datasetCode}/metadata")
    public ApiResponse<Boolean> updateMetadata(@PathVariable String datasetCode, @Valid @RequestBody DatasetMetadataUpdateRequest body, HttpServletRequest request) {
        String requestId = RequestIdHolder.getRequestId(request);
        log.info("[{}] [Controller] update dataset metadata, datasetCode={}, itemCount={}", requestId, datasetCode,
            body == null || body.items() == null ? 0 : body.items().size());
        datasetService.updateMetadata(datasetCode, body);
        return ApiResponse.success(true, requestId);
    }

    /** 查询当前数据类型版本的字段结构。 */
    @Operation(summary = "查询数据集字段结构", description = "从数据集当前引用的数据类型版本读取字段结构")
    @GetMapping("/{datasetCode}/structure")
    public ApiResponse<List<?>> structure(@PathVariable String datasetCode, HttpServletRequest request) {
        String requestId = RequestIdHolder.getRequestId(request);
        log.info("[{}] [Controller] get dataset structure, datasetCode={}", requestId, datasetCode);
        return ApiResponse.success(datasetService.getStructure(datasetCode), requestId);
    }

    /**
     * 批量读取应用指标绑定所需的 Dataset 主档与当前版本字段快照。
     * 这是只读元数据接口，特意不返回物理表、列或 StorageMapping 信息。
     */
    @Operation(summary = "批量查询指标绑定元数据", description = "返回数据集状态及当前数据类型版本的标准字段")
    @PostMapping("/metric-bindings/query")
    public ApiResponse<List<DatasetMetricBindingView>> metricBindings(
            @Valid @RequestBody DatasetMetricBindingQueryRequest body, HttpServletRequest request) {
        String requestId = RequestIdHolder.getRequestId(request);
        log.info("[{}] [Controller] query metric bindings, datasetCount={}", requestId, body.datasetCodes().size());
        return ApiResponse.success(datasetService.getMetricBindings(body), requestId);
    }

    /** 查询数据集来源任务、最近成功执行及结果摘要。 */
    @Operation(summary = "查询数据集来源", description = "查询数据集对应的来源任务、最近成功执行和结果摘要")
    @GetMapping("/{datasetCode}/source")
    public ApiResponse<DatasetDetailView> source(@PathVariable String datasetCode, HttpServletRequest request) {
        String requestId = RequestIdHolder.getRequestId(request);
        log.info("[{}] [Controller] get dataset source, datasetCode={}", requestId, datasetCode);
        return ApiResponse.success(datasetService.getDetail(datasetCode), requestId);
    }
}
