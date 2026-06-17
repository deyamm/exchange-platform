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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.exchange.platform.collection.api.request.datasource.DataSourceSaveRequest;
import com.exchange.platform.collection.api.request.datasource.DataSourceTableRequest;
import com.exchange.platform.collection.api.response.datasource.DataSourceTable;
import com.exchange.platform.collection.api.response.datasource.DataSourceView;
import com.exchange.platform.collection.service.DataSourceService;
import com.exchange.platform.common.core.result.ApiResponse;
import com.exchange.platform.common.server.support.RequestIdHolder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "数据源管理", description = "负责数据源的增删改查、连接测试、表管理")
@RestController
@RequestMapping("/api/v1/collection/data-sources")
public class DataSourceController {

    private static final Logger log = LoggerFactory.getLogger(DataSourceController.class);

    private final DataSourceService dataSourceService;

    public DataSourceController(DataSourceService dataSourceService) {
        this.dataSourceService = dataSourceService;
    }

    @Operation(summary = "查询数据源列表", description = "按名称/编码过滤数据源列表")
    @GetMapping
    public ApiResponse<List<DataSourceView>> listDataSources(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String dataSourceType,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);
        log.info("[{}] [Controller] listDataSources keyword={}, type={}", requestId, keyword, dataSourceType);
        List<DataSourceView> views = dataSourceService.listDataSources(keyword, dataSourceType);
        return ApiResponse.success(views, requestId);
    }

    @Operation(summary = "查询数据源详情", description = "获取指定数据源详情")
    @GetMapping("/{dataSourceCode}")
    public ApiResponse<DataSourceView> getDataSource(
        @PathVariable String dataSourceCode,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);
        log.info("[{}] [Controller] getDataSource dataSourceCode={}", requestId, dataSourceCode);
        DataSourceView view = dataSourceService.getDataSource(dataSourceCode);
        return ApiResponse.success(view, requestId);
    }

    @Operation(summary = "保存数据源", description = "创建或更新数据源配置")
    @PostMapping
    public ApiResponse<DataSourceView> saveDataSource(
        @Valid @RequestBody DataSourceSaveRequest body,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);
        log.info("[{}] [Controller] saveDataSource dataSourceCode={}", requestId, body.dataSourceCode());
        DataSourceView saved = dataSourceService.saveDataSource(body);
        return ApiResponse.success(saved, requestId);
    }

    @Operation(summary = "删除数据源", description = "删除指定数据源")
    @DeleteMapping("/{dataSourceCode}")
    public ApiResponse<Void> deleteDataSource(
        @PathVariable String dataSourceCode,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);
        log.info("[{}] [Controller] deleteDataSource dataSourceCode={}", requestId, dataSourceCode);
        dataSourceService.deleteDataSource(dataSourceCode);
        return ApiResponse.success(null, requestId);
    }

    @Operation(summary = "测试数据源连接", description = "测试数据源连接是否可用")
    @PostMapping("/{dataSourceCode}/test")
    public ApiResponse<Boolean> testDataSource(
        @PathVariable String dataSourceCode,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);
        log.info("[{}] [Controller] testDataSource dataSourceCode={}", requestId, dataSourceCode);
        boolean ok = dataSourceService.testConnection(dataSourceCode);
        return ApiResponse.success(ok, requestId);
    }

    @Operation(summary = "查询数据源表列表", description = "查询指定数据源的表列表")
    @GetMapping("/{dataSourceCode}/tables")
    public ApiResponse<List<DataSourceTable>> listTables(
        @PathVariable String dataSourceCode,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);
        log.info("[{}] [Controller] listTables dataSourceCode={}", requestId, dataSourceCode);
        List<DataSourceTable> tables = dataSourceService.listTables(dataSourceCode);
        return ApiResponse.success(tables, requestId);
    }

    @Operation(summary = "创建表", description = "在指定数据源创建表")
    @PostMapping("/{dataSourceCode}/tables")
    public ApiResponse<Void> createTable(
        @PathVariable String dataSourceCode,
        @Valid @RequestBody DataSourceTableRequest body,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);
        log.info("[{}] [Controller] createTable dataSourceCode={}, tableName={}", requestId, dataSourceCode, body.tableName());
        dataSourceService.createTable(dataSourceCode, body);
        return ApiResponse.success(null, requestId);
    }

    @Operation(summary = "修改表", description = "在指定数据源修改表结构")
    @PutMapping("/{dataSourceCode}/tables")
    public ApiResponse<Void> alterTable(
        @PathVariable String dataSourceCode,
        @Valid @RequestBody DataSourceTableRequest body,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);
        log.info("[{}] [Controller] alterTable dataSourceCode={}, tableName={}", requestId, dataSourceCode, body.tableName());
        dataSourceService.alterTable(dataSourceCode, body);
        return ApiResponse.success(null, requestId);
    }

    @Operation(summary = "删除表", description = "删除指定表")
    @DeleteMapping("/{dataSourceCode}/tables/{tableName}")
    public ApiResponse<Void> dropTable(
        @PathVariable String dataSourceCode,
        @PathVariable String tableName,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);
        log.info("[{}] [Controller] dropTable dataSourceCode={}, tableName={}", requestId, dataSourceCode, tableName);
        dataSourceService.dropTable(dataSourceCode, tableName);
        return ApiResponse.success(null, requestId);
    }
}
