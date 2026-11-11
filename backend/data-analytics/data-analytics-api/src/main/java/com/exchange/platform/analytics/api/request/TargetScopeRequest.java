package com.exchange.platform.analytics.api.request;

import java.util.List;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import com.exchange.platform.common.core.enums.AssetType;

/**
 * 条件筛选使用的标的范围协议。
 *
 * <p>范围值仅保存主数据编码，例如行业编码或关注分组编码；名称、物理表及 SQL
 * 都不属于前端请求。具体编码如何解析由分析服务根据 {@code targetType} 决定。</p>
 */
public record TargetScopeRequest(
        @NotNull(message = "范围标的类型不能为空") AssetType targetType,
        @NotBlank(message = "范围类型不能为空") String scopeType,
        List<String> scopeValues) {
}
