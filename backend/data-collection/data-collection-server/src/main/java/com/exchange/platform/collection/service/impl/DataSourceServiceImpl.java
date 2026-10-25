package com.exchange.platform.collection.service.impl;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exchange.platform.collection.api.enums.CommonStatus;
import com.exchange.platform.collection.api.request.datasource.DataSourceSaveRequest;
import com.exchange.platform.collection.api.request.datasource.DataSourceTableRequest;
import com.exchange.platform.collection.api.response.datasource.DataSourceTable;
import com.exchange.platform.collection.api.response.datasource.DataSourceTable.DataSourceTableColumn;
import com.exchange.platform.collection.api.response.datasource.DataSourceView;
import com.exchange.platform.collection.entity.datasource.DataSourceConfig;
import com.exchange.platform.collection.mapper.datasource.DataSourceConfigMapper;
import com.exchange.platform.collection.service.DataSourceService;
import com.exchange.platform.common.core.exception.BusinessException;
import com.exchange.platform.collection.utils.JdbcUtils;

@Service
public class DataSourceServiceImpl implements DataSourceService {

    private static final Logger log = LoggerFactory.getLogger(DataSourceServiceImpl.class);
    private final DataSourceConfigMapper dataSourceConfigMapper;

    public DataSourceServiceImpl(DataSourceConfigMapper dataSourceConfigMapper) {
        this.dataSourceConfigMapper = dataSourceConfigMapper;
    }

    /**
     * 查询数据源配置列表，支持按名称/编码模糊搜索，以及按数据源类型过滤
     */
    @Override
    @Transactional(readOnly = true)
    public List<DataSourceView> listDataSources(String keyword, String dataSourceType) {
        LambdaQueryWrapper<DataSourceConfig> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(q -> q.like(DataSourceConfig::getDataSourceName, keyword)
                .or()
                .like(DataSourceConfig::getDataSourceCode, keyword));
        }
        wrapper.eq(StringUtils.hasText(dataSourceType), DataSourceConfig::getDataSourceType, dataSourceType)
            .orderByDesc(DataSourceConfig::getUpdatedAt);

        List<DataSourceConfig> configs = dataSourceConfigMapper.selectList(wrapper);
        return configs.stream().map(this::toViewMasked).collect(Collectors.toList());
    }

    /**
     * 根据数据源编码查询数据源配置详情
     */
    @Override
    @Transactional(readOnly = true)
    public DataSourceView getDataSource(String dataSourceCode) {
        DataSourceConfig config = requireDataSource(dataSourceCode);
        return toViewMasked(config);
    }

    /**
     * 保存数据源配置（新增或更新），目前仅支持MYSQL类型的数据源
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DataSourceView saveDataSource(DataSourceSaveRequest request) {
        if (!"MYSQL".equalsIgnoreCase(request.dataSourceType())) {
            throw new RuntimeException("Only MYSQL data source is supported");
        }
        DataSourceConfig config = dataSourceConfigMapper.selectOne(new LambdaQueryWrapper<DataSourceConfig>()
            .eq(DataSourceConfig::getDataSourceCode, request.dataSourceCode())
            .last("LIMIT 1")
        );

        // 根据saveOrUpdate标识判断操作类型
        boolean isCreate;
        if ("save".equalsIgnoreCase(request.saveOrUpdate())) {
            // 新增：校验编码不能已存在
            if (config != null) {
                throw new BusinessException(40001, "数据源编码已存在：" + request.dataSourceCode());
            }
            config = new DataSourceConfig();
            isCreate = true;
        } else if ("update".equalsIgnoreCase(request.saveOrUpdate())) {
            // 更新：校验编码必须存在
            if (config == null) {
                throw new BusinessException(40001, "数据源编码不存在：" + request.dataSourceCode());
            }
            isCreate = false;
        } else {
            throw new BusinessException(40001, "saveOrUpdate参数值无效，必须为save或update");
        }

        config
            .setDataSourceCode(request.dataSourceCode())
            .setDataSourceName(request.dataSourceName())
            .setDataSourceType(request.dataSourceType())
            .setHost(request.host())
            .setPort(request.port())
            .setUsername(request.username())
            .setPassword(request.password())
            .setDatabaseName(request.databaseName())
            .setConnectionParams(request.connectionParams())
            .setStatus(request.status() != null ? request.status() : CommonStatus.ENABLED)
            .setRemark(request.remark());

        if (isCreate) {
            dataSourceConfigMapper.insert(config);
        } else {
            dataSourceConfigMapper.updateById(config);
        }

        return toViewMasked(config);
    }

    /**
     * 删除数据源配置
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDataSource(String dataSourceCode) {
        dataSourceConfigMapper.delete(new LambdaQueryWrapper<DataSourceConfig>()
            .eq(DataSourceConfig::getDataSourceCode, dataSourceCode)
        );
    }

    /**
     * 测试数据源连接是否成功
     */
    @Override
    public boolean testConnection(String dataSourceCode) {
        DataSourceConfig config = requireDataSource(dataSourceCode);
        try (Connection conn = JdbcUtils.openConnection(config)) {
            return conn != null && conn.isValid(2);
        } catch (Exception ex) {
            log.warn("Test connection failed for dataSourceCode={}, error={}", dataSourceCode, ex.getMessage());
            return false;
        }
    }

    /**
     * 列出指定数据源下的所有表名
     */
    @Override
    @Transactional(readOnly = true)
    public List<DataSourceTable> listTables(String dataSourceCode) {
        DataSourceConfig config = requireDataSource(dataSourceCode);
        
        List<Map<String, Object>> tablesAndColumns = dataSourceConfigMapper.selectTablesInfo(config.getDatabaseName());

        // 如果没有表信息，直接返回空列表
        if (tablesAndColumns == null || tablesAndColumns.isEmpty()) {
            return new ArrayList<>();
        }

        // 以schemaName.tableName作为key，避免不同schema下同名表冲突，同时保持表的顺序
        Map<String, DataSourceTable> tableMap = new LinkedHashMap<>();

        for (Map<String, Object> row : tablesAndColumns) {
            String schemaName = row.get("schemaName") != null ? row.get("schemaName").toString() : "";
            String tableName = row.get("tableName") != null ? row.get("tableName").toString() : "";

            // 避免不同 schema 下存在同名表
            String tableKey = schemaName + "." + tableName;

            // 如果表已存在则复用，否则创建新表对象
            DataSourceTable table = tableMap.computeIfAbsent(tableKey, key -> {
                DataSourceTable item = new DataSourceTable();
                item.setSchemaName(schemaName);
                item.setTableName(tableName);
                item.setDataSourceCode(dataSourceCode);
                item.setCurrentRows(row.get("currentRows") != null ? Integer.valueOf(row.get("currentRows").toString()) : 0);
                item.setColumns(new ArrayList<>());
                return item;
            });
            
            // 处理列信息
            DataSourceTableColumn column = new DataSourceTableColumn();
            column.setColumnName(row.get("columnName") != null ? row.get("columnName").toString() : "");
            column.setColumnType(row.get("columnType") != null ? row.get("columnType").toString() : "");
            column.setDefaultValue(row.get("defaultValue") != null ? row.get("defaultValue").toString() : "");
            column.setNullable(row.get("nullable") != null ? Boolean.valueOf(row.get("nullable").toString()) : false);
            column.setUniqueKey(row.get("uniqueKey") != null ? Boolean.valueOf(row.get("uniqueKey").toString()) : false);
            column.setComment(row.get("comment") != null ? row.get("comment").toString() : "");

            table.getColumns().add(column);
        }
        return new ArrayList<>(tableMap.values());
    }

    /**
     * 创建数据表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createTable(String dataSourceCode, DataSourceTableRequest request) {
        DataSourceConfig config = requireDataSource(dataSourceCode);
        createTableIfMissing(config, request.tableName(), request.columns());
    }

    /**
     * 修改数据表结构（目前仅支持新增字段，不支持修改或删除已有字段）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void alterTable(String dataSourceCode, DataSourceTableRequest request) {
        DataSourceConfig config = requireDataSource(dataSourceCode);
        Map<String, String> existingColumns = JdbcUtils.loadTableColumns(config, request.tableName());

        for (DataSourceTableRequest.ColumnItem column : request.columns()) {
            String columnName = JdbcUtils.normalizeColumnName(column.columnName());
            if (existingColumns.containsKey(columnName)) {
                continue;
            }
            String addColumnSql = buildAddColumnSql(request.tableName(), column);
            JdbcUtils.executeSql(config, addColumnSql);
        }
    }

    /**
     * 删除数据表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dropTable(String dataSourceCode, String tableName) {
        DataSourceConfig config = requireDataSource(dataSourceCode);
        String sql = "DROP TABLE IF EXISTS `" + tableName + "`";
        JdbcUtils.executeSql(config, sql);
    }

    /**
     * 根据数据源编码查询数据源配置详情，如果未找到则抛出异常
     */
    @Override
    public DataSourceConfig requireDataSource(String dataSourceCode) {
        DataSourceConfig config = dataSourceConfigMapper.selectOne(new LambdaQueryWrapper<DataSourceConfig>()
            .eq(DataSourceConfig::getDataSourceCode, dataSourceCode)
            .last("LIMIT 1")
        );
        if (config == null) {
            throw new RuntimeException("DataSource not found for dataSourceCode: " + dataSourceCode);
        }
        return config;
    }

    /**
     * 如果数据表不存在，则创建该数据表，如果表已存在，则对比当前表结构与请求中的字段，如果不一致则抛出异常
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createTableIfMissing(DataSourceConfig dataSource, String tableName, List<DataSourceTableRequest.ColumnItem> columns) {
        if (!JdbcUtils.tableExists(dataSource, tableName)) {
            String sql = buildCreateTableSql(tableName, columns);
            JdbcUtils.executeSql(dataSource, sql);
        } else {
            Map<String, String> existingColumns = JdbcUtils.loadTableColumns(dataSource, tableName);
            for (DataSourceTableRequest.ColumnItem column : columns) {
                // 1. 检查字段是否存在
                if (!existingColumns.containsKey(JdbcUtils.normalizeColumnName(column.columnName()))) {
                    throw new BusinessException(40002, "已选择的表 '" + tableName + "' 中不存在字段 " + column.columnName());
                }
                // 2. 检查字段类型是否一致（简单比较类型字符串，忽略大小写和多余空白）
                String expectedColumnType = JdbcUtils.normalizeColumnType(column.columnType());
                String actualColumnType = JdbcUtils.normalizeColumnType(existingColumns.get(JdbcUtils.normalizeColumnName(column.columnName())));
                if (!expectedColumnType.equals(actualColumnType)) {
                    throw new BusinessException(40002, "已选择的表 '" + tableName + "' 中字段 " + column.columnName()
                        + " 类型不一致，期望 " + column.columnType() + "，实际 " + actualColumnType);
                }
            }
        }
    }

    /**
     * 构建创建数据表的SQL语句
     * @param tableName
     * @param columns
     * @return
     */
    private String buildCreateTableSql(String tableName, List<DataSourceTableRequest.ColumnItem> columns) {
        if (columns == null || columns.isEmpty()) {
            throw new RuntimeException("Table columns cannot be empty");
        }

        List<String> columnDefs = new ArrayList<>();
        List<String> uniqueColumns = new ArrayList<>();

        for (DataSourceTableRequest.ColumnItem column : columns) {
            StringBuilder builder = new StringBuilder();
            builder.append("`").append(column.columnName()).append("` ")
                .append(column.columnType());

            if (Boolean.FALSE.equals(column.nullable())) {
                builder.append(" NOT NULL");
            }

            String defaultValue = normalizeDefaultValue(column.defaultValue());
            if (defaultValue != null) {
                builder.append(" DEFAULT ").append(defaultValue);
            }

            String comment = column.comment();
            if (StringUtils.hasText(comment)) {
                String escapedComment = comment.replace("'", " ");
                builder.append(" COMMENT '").append(escapedComment).append("'");
            }

            columnDefs.add(builder.toString());

            if (Boolean.TRUE.equals(column.uniqueKey())) {
                uniqueColumns.add("`" + column.columnName() + "`");
            }
        }

        if (!uniqueColumns.isEmpty()) {
            columnDefs.add("UNIQUE KEY `uk_" + tableName + "_biz` (" + String.join(",", uniqueColumns) + ")");
        }

        return "CREATE TABLE IF NOT EXISTS `" + tableName + "` (" + String.join(", ", columnDefs) + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
    }

    /**
     * 构建新增字段的SQL语句
     * @param tableName
     * @param column
     * @return
     */
    private String buildAddColumnSql(String tableName, DataSourceTableRequest.ColumnItem column) {
        StringBuilder builder = new StringBuilder();
        builder.append("ALTER TABLE `").append(tableName).append("` ADD COLUMN `")
            .append(column.columnName()).append("` ")
            .append(column.columnType());

        if (Boolean.FALSE.equals(column.nullable())) {
            builder.append(" NOT NULL");
        }

        String defaultValue = normalizeDefaultValue(column.defaultValue());
        if (defaultValue != null) {
            builder.append(" DEFAULT ").append(defaultValue);
        }

        return builder.toString();
    }

    /**
     * 规范化默认值，处理空字符串、null字符串、函数表达式等特殊情况
     * @param defaultValue
     * @return
     */
    private String normalizeDefaultValue(String defaultValue) {
        if (!StringUtils.hasText(defaultValue)) {
            return null;
        }

        String trimmed = defaultValue.trim();
        if (!StringUtils.hasText(trimmed) || "null".equalsIgnoreCase(trimmed)) {
            return null;
        }

        if ("CURRENT_TIMESTAMP".equalsIgnoreCase(trimmed) || "NOW()".equalsIgnoreCase(trimmed)) {
            return trimmed;
        }

        String escaped = trimmed.replace("'", "''");
        return "'" + escaped + "'";
    }

    /**
     * 将数据源配置转换为视图对象，密码字段进行掩码处理
     * @param config 数据源配置对象
     * @return 视图对象
     */
    private DataSourceView toViewMasked(DataSourceConfig config) {
        if (config == null) {
            return null;
        }
        DataSourceView view = new DataSourceView();
        view.setDataSourceCode(config.getDataSourceCode());
        view.setDataSourceName(config.getDataSourceName());
        view.setDataSourceType(config.getDataSourceType());
        view.setHost(config.getHost());
        view.setPort(config.getPort());
        view.setUsername(config.getUsername());
        view.setPassword(maskPassword(config.getPassword()));
        view.setDatabaseName(config.getDatabaseName());
        view.setConnectionParams(config.getConnectionParams());
        view.setStatus(config.getStatus());
        view.setRemark(config.getRemark());
        view.setCreatedAt(config.getCreatedAt());
        view.setUpdatedAt(config.getUpdatedAt());
        return view;
    }

    private String maskPassword(String password) {
        if (!StringUtils.hasText(password)) {
            return "";
        }
        return "******";
    }
}