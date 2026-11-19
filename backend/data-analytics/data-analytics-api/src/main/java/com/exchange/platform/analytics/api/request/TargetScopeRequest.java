package com.exchange.platform.analytics.api.request;

import java.util.List;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import com.exchange.platform.common.core.enums.MarketAssetType;

/**
 * 条件筛选使用的标的范围协议。
 *
 * <p>范围值仅保存主数据编码，例如行业指数编码或关注分组编码；名称、物理表及 SQL
 * 都不属于前端请求。股票板块以行业指数表示，必须同时指定发布方和层级，避免把
 * SW、EM、THS 的同名行业合并为一个范围。</p>
 */
public record TargetScopeRequest(
        @NotNull(message = "范围标的类型不能为空") MarketAssetType targetType,
        @NotBlank(message = "范围类型不能为空") String scopeType,
        List<String> scopeValues,
        String industryPublisherCode,
        Integer industryLevel) {
}
