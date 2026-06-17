package com.exchange.platform.data.client.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;



/**
 * 由python数据处理层在同步任务模版信息后返回的数据结构
 */
public record TaskTemplateSync(
    @NotBlank String taskCode,
    @NotBlank String taskName,
    String taskDesc,
    @NotBlank String handlerName,
    String dataSource,
    String assetType,
    String bizType,
    List<Map<String, Object>> paramsSchema,
    @NotNull List<OutputFieldItem> outputFields,
    List<String> ruleCategoryCodes,
    LocalDateTime syncTime
) {
    /**
     * 采集任务的输出字段信息
     */
    public record OutputFieldItem(
        @NotBlank String fieldCode,
        String fieldName,
        String fieldType,
        Boolean required,
        Boolean uniqueKey,
        Integer sortNo,
        String fieldDesc
    ) {}
}
