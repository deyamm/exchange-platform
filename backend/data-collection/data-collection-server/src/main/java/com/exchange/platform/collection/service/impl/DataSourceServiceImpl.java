package com.exchange.platform.collection.service.impl;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
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

@Service
public class DataSourceServiceImpl implements DataSourceService {

    private static final Logger log = LoggerFactory.getLogger(DataSourceServiceImpl.class);
    private static final String DEFAULT_TIME_ZONE = "Asia/Shanghai";

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
        try (Connection conn = openConnection(config)) {
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
        Map<String, String> existingColumns = loadTableColumns(config, request.tableName());

        for (DataSourceTableRequest.ColumnItem column : request.columns()) {
            String columnName = normalizeColumnName(column.columnName());
            if (existingColumns.containsKey(columnName)) {
                continue;
            }
            String addColumnSql = buildAddColumnSql(request.tableName(), column);
            executeSql(config, addColumnSql);
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
        executeSql(config, sql);
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
        if (!tableExists(dataSource, tableName)) {
            String sql = buildCreateTableSql(tableName, columns);
            executeSql(dataSource, sql);
        } else {
            Map<String, String> existingColumns = loadTableColumns(dataSource, tableName);
            for (DataSourceTableRequest.ColumnItem column : columns) {
                // 1. 检查字段是否存在
                if (!existingColumns.containsKey(normalizeColumnName(column.columnName()))) {
                    throw new BusinessException(40002, "已选择的表 '" + tableName + "' 中不存在字段 " + column.columnName());
                }
                // 2. 检查字段类型是否一致（简单比较类型字符串，忽略大小写和多余空白）
                String expectedColumnType = normalizeColumnType(column.columnType());
                String actualColumnType = normalizeColumnType(existingColumns.get(normalizeColumnName(column.columnName())));
                if (!expectedColumnType.equals(actualColumnType)) {
                    throw new BusinessException(40002, "已选择的表 '" + tableName + "' 中字段 " + column.columnName()
                        + " 类型不一致，期望 " + column.columnType() + "，实际 " + actualColumnType);
                }
            }
        }
    }

    /**
     * 检查数据表是否存在
     */
    private boolean tableExists(DataSourceConfig config, String tableName) {
        String sql = "SELECT COUNT(1) AS cnt FROM information_schema.tables WHERE table_schema = ? AND table_name = ?";
        try (Connection conn = openConnection(config);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, config.getDatabaseName());
            ps.setString(2, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt("cnt") > 0;
            }
        } catch (Exception ex) {
            throw new RuntimeException("Failed to check table existence: " + tableName, ex);
        }
    }

    /**
     * 加载数据表的列信息，<column_name, column_type>，用于对比请求中的字段定义与数据库中实际的表结构
     */
    private Map<String, String> loadTableColumns(DataSourceConfig config, String tableName) {
        Map<String, String> columns = new HashMap<>();
        String sql = "SELECT column_name, column_type FROM information_schema.columns WHERE table_schema = ? AND table_name = ?";
        try (Connection conn = openConnection(config);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, config.getDatabaseName());
            ps.setString(2, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    columns.put(normalizeColumnName(rs.getString("column_name")), rs.getString("column_type"));
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException("Failed to load columns for table: " + tableName, ex);
        }
        return columns;
    }

    /**
     * 规范化列名，用于比较数据库字段和请求字段
     * @param columnName
     * @return
     */
    private String normalizeColumnName(String columnName) {
        return columnName == null ? null : columnName.trim().toLowerCase();
    }

    /**
     * 规范化字段类型，忽略大小写和多余空白后再比较
     * @param columnType
     * @return
     */
    private String normalizeColumnType(String columnType) {
        if (!StringUtils.hasText(columnType)) {
            return "";
        }
        return columnType.trim().toLowerCase().replaceAll("\\s+", "");
    }

    /**
     * 打开数据库连接
     * @param config
     * @return
     * @throws Exception
     */
    private Connection openConnection(DataSourceConfig config) throws Exception {
        String url = buildJdbcUrl(config);
        return DriverManager.getConnection(url, config.getUsername(), config.getPassword());
    }

    /**
     * 执行SQL语句
     */
    private void executeSql(DataSourceConfig config, String sql) {
        try (Connection conn = openConnection(config);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to execute SQL: " + sql, ex);
        }
    }

    /**
     * 构建JDBC连接URL
     */
    private String buildJdbcUrl(DataSourceConfig config) {
        StringBuilder sb = new StringBuilder();
        sb.append("jdbc:mysql://")
          .append(config.getHost())
          .append(":")
          .append(config.getPort())
          .append("/")
          .append(config.getDatabaseName());

        String params = buildConnectionParams(config.getConnectionParams());
        if (StringUtils.hasText(params)) {
            sb.append("?").append(params);
        }
        return sb.toString();
    }

    /**
     * 构建连接参数字符串，合并默认参数和额外参数，额外参数格式示例：key1=value1&key2=value2
     * @param extraParams
     * @return
     */
    private String buildConnectionParams(String extraParams) {
        Map<String, String> params = new HashMap<>();
        params.put("useUnicode", "true");
        params.put("characterEncoding", "utf8");
        params.put("serverTimezone", DEFAULT_TIME_ZONE);
        params.put("useSSL", "false");

        if (StringUtils.hasText(extraParams)) {
            String[] pairs = extraParams.split("&");
            for (String pair : pairs) {
                if (!StringUtils.hasText(pair)) {
                    continue;
                }
                String[] kv = pair.split("=", 2);
                if (kv.length == 2 && StringUtils.hasText(kv[0])) {
                    params.put(kv[0], kv[1]);
                }
            }
        }

        return params.entrySet().stream()
            .map(entry -> entry.getKey() + "=" + entry.getValue())
            .collect(Collectors.joining("&"));
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
