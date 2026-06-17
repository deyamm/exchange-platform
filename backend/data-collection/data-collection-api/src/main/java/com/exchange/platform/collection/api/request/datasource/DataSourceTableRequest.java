package com.exchange.platform.collection.api.request.datasource;

import java.util.List;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 数据源表操作请求
 *
 * @param tableName 表名
 * @param columns   列定义
 */
public record DataSourceTableRequest(
    @NotBlank String tableName,
    @NotNull List<ColumnItem> columns
) {
    public record ColumnItem(
        @NotBlank String columnName,
        @NotBlank String columnType,
        Boolean nullable,
        String defaultValue,
        Boolean uniqueKey
    ) {}
}
