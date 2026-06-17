package com.exchange.platform.collection.api.response.datasource;

import java.util.List;

import lombok.Data;

@Data
public class DataSourceTable {
    private String tableName;
    private String schemaName;
    private String dataSourceCode;
    private Integer currentRows;
    private List<DataSourceTableColumn> columns;

    @Data
    public static class DataSourceTableColumn {
        private String columnName;
        private String columnType;
        private Boolean nullable;
        private Boolean uniqueKey;
        private String defaultValue;
    }
}

