
package com.exchange.platform.collection.controller;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.exchange.platform.collection.api.enums.CommonStatus;
import com.exchange.platform.collection.api.request.dataTopic.DataTopicSaveRequest;
import com.exchange.platform.collection.api.response.dataTopic.DataTopicView;
import com.exchange.platform.collection.service.DataTopicService;
import com.exchange.platform.common.core.result.ApiResponse;
import com.exchange.platform.common.server.support.RequestIdHolder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;


/**
 * 数据主题管理控制器
 * 提供数据主题的查询、保存、删除等接口
 */
@Tag(name = "数据主题管理", description = "数据主题相关的接口")
@RestController
@RequestMapping("/api/v1/collection/data-topics")
public class DataTopicController {

    private static final Logger log = LoggerFactory.getLogger(DataTopicController.class);

    private final DataTopicService dataTopicService;

    public DataTopicController(DataTopicService dataTopicService) {
        this.dataTopicService = dataTopicService;
    }

    /**
     * 查询数据主题及层级信息，用于目录树展示和配置选择
     */
    @Operation(summary = "查询数据主题列表", description = "查询数据主题及层级信息，用于目录树展示和配置选择")
    @GetMapping
    public ApiResponse<Object> queryDataTopics(
            @RequestParam(required = false) String parentCode,
            @RequestParam(required = false) String dataTopicName,
            @RequestParam(required = false) String dataTopicLabel,
            @RequestParam(required = false) CommonStatus status,
            @RequestParam(defaultValue = "true") Boolean tree,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "20") Integer pageSize,
            HttpServletRequest servletRequest
    ) {
        String requestId = RequestIdHolder.getRequestId(servletRequest);

        log.info("[Controller] queryDataTopics start, requestId={}, parentCode={}, dataTopicName={}, dataTopicLabel={}, status={}, tree={}, pageNo={}, pageSize={}",
                requestId, parentCode, dataTopicName, dataTopicLabel, status, tree, pageNo, pageSize);

        Object data = dataTopicService.queryDataTopics(parentCode, dataTopicName, dataTopicLabel, status, tree, pageNo, pageSize);
        log.info("[Controller] queryDataTopics success, requestId={}, responseType={}", requestId, data == null ? "null" : data.getClass().getSimpleName());
        return ApiResponse.success(data, requestId);
    }

    /**
     * 保存数据主题信息，包括层级关系和关联的字段信息
     */
    @Operation(summary = "保存数据主题", description = "保存数据主题信息，包括层级关系和关联的字段信息")
    @PostMapping
    public ApiResponse<DataTopicView> saveDataTopic(
            @Valid @RequestBody DataTopicSaveRequest request,
            HttpServletRequest servletRequest
    ) {
        String requestId = RequestIdHolder.getRequestId(servletRequest);

        log.info("[Controller] saveDataTopic start, requestId={}, dataTopicCode={}, parentCode={}, nodeLevel={}, isLeaf={}, status={}",
                requestId, request.dataTopicCode(), request.parentCode(), request.nodeLevel(), request.isLeaf(), request.status());

        DataTopicView saved = dataTopicService.saveDataTopic(request);

        log.info("[Controller] saveDataTopic success, requestId={}, dataTopicCode={}", requestId, saved.getDataTopicCode());
        return ApiResponse.success(saved, requestId);
    }

    /**
     * 根据数据主题编码删除数据主题；若存在子节点则禁止删除
     */
    @Operation(summary = "删除数据主题", description = "根据数据主题编码删除数据主题；若存在子节点则禁止删除")
    @DeleteMapping
    public ApiResponse<Void> deleteDataTopic(
            @RequestParam String dataTopicCode,
            HttpServletRequest servletRequest
    ) {
        String requestId = RequestIdHolder.getRequestId(servletRequest);

        log.info("[Controller] deleteDataTopic start, requestId={}, dataTopicCode={}", requestId, dataTopicCode);

        dataTopicService.deleteDataTopic(dataTopicCode);

        log.info("[Controller] deleteDataTopic success, requestId={}, dataTopicCode={}", requestId, dataTopicCode);
        return ApiResponse.success(null, requestId);
    }

}
