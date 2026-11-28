package com.exchange.platform.analytics.api.request;

import java.util.List;
import java.util.Map;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.exchange.platform.analytics.api.enums.MasterDataType;

/**
 * 已发布主数据的通用查询条件。
 *
 * <p>筛选字段使用主数据注册表中的逻辑字段编码，而不是物理列名。</p>
 */
public record PublishedMasterDataQueryRequest(
        @NotNull(message = "主数据类型不能为空") MasterDataType typeCode,
        String keywords,
        Map<String, Object> equalsFilters,
        Map<String, List<Object>> inFilters,
        List<String> keywordFields,
        String orderBy,
        Boolean ascending,
        @Min(value = 1, message = "页码必须大于 0") Integer pageNo,
        @Min(value = 1, message = "每页条数必须大于 0")
        @Max(value = 200, message = "每页条数不能超过 200") Integer pageSize) {
}
