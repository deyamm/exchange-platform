package com.exchange.platform.analytics.api.registry;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Collections;

/** 经过验证的主数据存储定义，主数据 field -> 物理表 columns */
public record StorageDefinition(
    String tableName, 
    Map<String, String> columns
) {
    public StorageDefinition {
        if (tableName == null || !tableName.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException("非法主数据存储表名");
        }

        Map<String, String> safeColumns = columns == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(columns));

        safeColumns.keySet().forEach(key -> {
            String column = safeColumns.get(key);
            if (key == null || !key.matches("[A-Za-z_][A-Za-z0-9_]*")) 
                throw new IllegalArgumentException("非法主数据字段名");
            if (column == null || !column.matches("[A-Za-z_][A-Za-z0-9_]*")) 
                throw new IllegalArgumentException("非法主数据列名");
        });

        columns = safeColumns;
    }
}
