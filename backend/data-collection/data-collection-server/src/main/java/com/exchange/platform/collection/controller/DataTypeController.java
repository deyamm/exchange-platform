package com.exchange.platform.collection.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.slf4j.*;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.exchange.platform.collection.api.enums.CommonStatus;
import com.exchange.platform.collection.api.request.dataType.DataTypeFieldSaveRequest;
import com.exchange.platform.collection.api.request.dataType.DataTypeSaveRequest;
import com.exchange.platform.collection.api.request.dataType.DataTypeVersionPublishRequest;
import com.exchange.platform.collection.api.response.dataType.DataTypeDetailView;
import com.exchange.platform.collection.api.response.dataType.DataTypeFieldView;
import com.exchange.platform.collection.api.response.dataType.DataTypeVersionView;
import com.exchange.platform.collection.api.response.dataType.DataTypeView;
import com.exchange.platform.collection.service.DataTypeService;
import com.exchange.platform.common.core.result.ApiResponse;
import com.exchange.platform.common.server.support.RequestIdHolder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 数据类型控制器，负责处理与数据类型相关的请求
 * - 包括数据类型的增删改查、发布状态管理等功能
 * - 包括数据类型逻辑表字段结构、版本管理
 */
@Tag(name = "数据类型管理", description = "负责数据类型的增删改查、发布状态、逻辑表结构管理等功能")
@RestController
@RequestMapping("/api/v1/collection/data-types")
public class DataTypeController {

    private static final Logger logger = LoggerFactory.getLogger(DataTypeController.class);
    
    private final DataTypeService dataTypeService;

    public DataTypeController(DataTypeService dataTypeService) {
        this.dataTypeService = dataTypeService;
    }

    @Operation(summary = "查询数据类型列表", description = "支持按树形结构或分页方式查询数据类型列表")
    @GetMapping
    public ApiResponse<Object> queryDataTypes(
        @RequestParam(required = false) String parentCode,
        @RequestParam(required = false) String dataTypeName,
        @RequestParam(required = false) String dataTypeLabel,
        @RequestParam(required = false) CommonStatus status,
        @RequestParam(defaultValue = "true") Boolean tree,
        @RequestParam(defaultValue = "1") Integer pageNo,
        @RequestParam(defaultValue = "20") Integer pageSize,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);
        logger.info("[{}] [Controller] queryDataTypes called with parentCode={}, dataTypeName={}, dataTypeLabel={}, status={}, tree={}, pageNo={}, pageSize={}", 
            requestId, parentCode, dataTypeName, dataTypeLabel, status, tree, pageNo, pageSize);
        
        Object result = dataTypeService.queryDataTypes(parentCode, dataTypeName, dataTypeLabel, status, tree, pageNo, pageSize);
        return ApiResponse.success(result, requestId);
    }

    @Operation(summary = "查询数据类型详情", description = "获取数据类型的基本信息和字段列表")
    @GetMapping("/{dataTypeCode}")
    public ApiResponse<DataTypeDetailView> getDataTypeDetail(
        @PathVariable String dataTypeCode,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);
        logger.info("[{}] [Controller] getDataTypeDetail called with dataTypeCode={}", requestId, dataTypeCode);
        
        DataTypeDetailView detail = dataTypeService.getDataTypeDetail(dataTypeCode);
        return ApiResponse.success(detail, requestId);
    }

    @Operation(summary = "保存数据类型", description = "创建或更新数据类型")
    @PostMapping
    public ApiResponse<DataTypeView> saveDataType(
        @Valid @RequestBody DataTypeSaveRequest dataType,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);
        logger.info("[{}] [Controller] saveDataType called with dataType={}", requestId, dataType);
        
        DataTypeView saved = dataTypeService.saveDataType(dataType);
        return ApiResponse.success(saved, requestId);
    }

    @Operation(summary = "删除数据类型", description = "根据数据类型编码删除数据类型")
    @DeleteMapping("/{dataTypeCode}")
    public ApiResponse<Void> deleteDataType(
        @PathVariable String dataTypeCode,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);
        logger.info("[{}] [Controller] deleteDataType called with dataTypeCode={}", requestId, dataTypeCode);

        dataTypeService.deleteDataType(dataTypeCode);
        return ApiResponse.success(null, requestId);
    }
    

    @Operation(summary = "查询数据类型字段", description = "获取指定数据类型的字段列表")
    @GetMapping("/{dataTypeCode}/fields")
    public ApiResponse<Object> queryDataTypeFields(
        @PathVariable String dataTypeCode,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);
        logger.info("[{}] [Controller] queryDataTypeFields called with dataTypeCode={}", requestId, dataTypeCode);
        
        List<DataTypeFieldView> fields = dataTypeService.queryDataTypeFields(dataTypeCode);
        return ApiResponse.success(fields, requestId);
    }

    @Operation(summary = "保存数据类型字段", description = "保存或更新指定数据类型的字段列表")
    @PostMapping("/{dataTypeCode}/fields")
    public ApiResponse<Object> saveDataTypeFields(
        @PathVariable String dataTypeCode,
        @Valid @RequestBody DataTypeFieldSaveRequest request,
        HttpServletRequest httpRequest
    ) {
        String requestId = RequestIdHolder.getRequestId(httpRequest);
        logger.info("[{}] [Controller] saveDataTypeFields called with dataTypeCode={}, request={}", requestId, dataTypeCode, request);
        
        List<DataTypeFieldView> savedFields = dataTypeService.saveDataTypeFields(dataTypeCode, request);
        return ApiResponse.success(savedFields, requestId);
    }

    @Operation(summary = "发布数据类型版本", description = "发布指定数据类型的一个新版本")
    @PostMapping("/{dataTypeCode}/versions")
    public ApiResponse<DataTypeVersionView> publishDataTypeVersion(
        @PathVariable String dataTypeCode,
        @Valid @RequestBody DataTypeVersionPublishRequest request,
        HttpServletRequest httpRequest
    ) {
        String requestId = RequestIdHolder.getRequestId(httpRequest);
        logger.info("[{}] [Controller] publishDataTypeVersion called with dataTypeCode={}, request={}", requestId, dataTypeCode, request);
        
        DataTypeVersionView versionView = dataTypeService.publishDataTypeVersion(dataTypeCode, request);
        return ApiResponse.success(versionView, requestId);
    }

    @Operation(summary = "查询数据类型版本", description = "获取指定数据类型的历史版本列表")
    @GetMapping("/{dataTypeCode}/versions")
    public ApiResponse<List<DataTypeVersionView>> queryDataTypeVersion(
        @PathVariable String dataTypeCode,
        HttpServletRequest httpRequest
    ) {
        String requestId = RequestIdHolder.getRequestId(httpRequest);
        logger.info("[{}] [Controller] queryDataTypeVersion called with dataTypeCode={}", requestId, dataTypeCode);
        
        List<DataTypeVersionView> versions = dataTypeService.queryDataTypeVersion(dataTypeCode);
        return ApiResponse.success(versions, requestId);
    }
}
