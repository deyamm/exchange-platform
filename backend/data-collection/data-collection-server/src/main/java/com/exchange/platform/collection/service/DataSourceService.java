package com.exchange.platform.collection.service;

import java.util.List;

import com.exchange.platform.collection.api.request.datasource.DataSourceSaveRequest;
import com.exchange.platform.collection.api.request.datasource.DataSourceTableRequest;
import com.exchange.platform.collection.api.response.datasource.DataSourceTable;
import com.exchange.platform.collection.api.response.datasource.DataSourceView;
import com.exchange.platform.collection.entity.datasource.DataSourceConfig;

public interface DataSourceService {

    List<DataSourceView> listDataSources(String keyword, String dataSourceType);

    DataSourceView getDataSource(String dataSourceCode);

    DataSourceView saveDataSource(DataSourceSaveRequest request);

    void deleteDataSource(String dataSourceCode);

    boolean testConnection(String dataSourceCode);

    List<DataSourceTable> listTables(String dataSourceCode);

    void createTable(String dataSourceCode, DataSourceTableRequest request);

    void alterTable(String dataSourceCode, DataSourceTableRequest request);

    void dropTable(String dataSourceCode, String tableName);

    DataSourceConfig requireDataSource(String dataSourceCode);

    void createTableIfMissing(DataSourceConfig dataSource, String tableName, List<DataSourceTableRequest.ColumnItem> columns);
}
