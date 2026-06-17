package com.exchange.platform.analytics.controller;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.exchange.platform.analytics.api.enums.CommonStatus;
import com.exchange.platform.analytics.api.request.ResearchTagSaveRequest;
import com.exchange.platform.analytics.api.response.ResearchTagView;
import com.exchange.platform.analytics.service.ResearchTagService;
import com.exchange.platform.common.core.page.PageResult;
import com.exchange.platform.common.core.result.ApiResponse;
import com.exchange.platform.common.server.support.RequestIdHolder;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/api/v1/analytics/research-tags")
public class ResearchTagController {

    private static final Logger log = LoggerFactory.getLogger(ResearchTagController.class);

    private final ResearchTagService researchTagService;

    public ResearchTagController(ResearchTagService researchTagService) {
        this.researchTagService = researchTagService;
    }

    @Operation(summary = "获取研报标签列表", description = "获取研报标签列表，支持分页和多条件过滤")
    @GetMapping
    public ApiResponse<PageResult<ResearchTagView>> getResearchTagList(
        @RequestParam(required = false) String tagName,
        @RequestParam(required = false) String tagCode,
        @RequestParam(required = false) String tagCategory,
        @RequestParam(required = false) CommonStatus status,
        @RequestParam(defaultValue = "1") Integer pageNo,
        @RequestParam(defaultValue = "20") Integer pageSize,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);

        log.info("[{}] [Controller] getResearchTagList: tagName={}, tagCode={}, tagCategory={}, status={}, pageNo={}, pageSize={}", requestId, tagName, tagCode, tagCategory, status, pageNo, pageSize);

        return ApiResponse.success(researchTagService.getResearchTagList(tagName, tagCode, tagCategory, status, pageNo, pageSize), requestId);
    }

    @Operation(summary = "保存或更新研报标签", description = "根据请求体中的tagCode创建或更新研报标签")
    @PostMapping
    public ApiResponse<ResearchTagView> saveOrUpdateResearchTag(
        @RequestBody ResearchTagSaveRequest body,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);

        log.info("[{}] [Controller] saveOrUpdateResearchTag: body={}", requestId, body);

        return ApiResponse.success(researchTagService.saveOrUpdateResearchTag(body), requestId);
    }

    @Operation(summary = "删除研报标签", description = "根据tagCode删除研报标签")
    @DeleteMapping("/{tagCode}")
    public ApiResponse<Void> deleteResearchTag(
        @PathVariable String tagCode,
        HttpServletRequest request
    ) {
        String requestId = RequestIdHolder.getRequestId(request);

        log.info("[{}] [Controller] deleteResearchTag: tagCode={}", requestId, tagCode);
        researchTagService.deleteResearchTag(tagCode);
        return ApiResponse.success(null, requestId);
    }
}