package com.exchange.platform.collection.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.util.StringUtils;

import com.exchange.platform.collection.entity.datasource.DataSourceConfig;

/**
 * JDBC 通用工具类。
 *
 * <p>用于集中处理服务层中重复出现的 JDBC 基础流程，包括：</p>
 * <ul>
 *     <li>构建 MySQL JDBC URL 与默认连接参数</li>
 *     <li>打开 JDBC 连接、执行无返回 SQL</li>
 *     <li>查询 information_schema 中的表/字段元数据</li>
 * </ul>
 *
 * <p>该类只放置无状态的公共逻辑，不持有 Spring Bean，也不承载业务校验。</p>
 */
public final class JdbcUtils {

    /** 默认数据库时区，与现有业务保持一致。 */
    public static final String DEFAULT_TIME_ZONE = "Asia/Shanghai";

    private JdbcUtils() {
        // 工具类禁止实例化
    }

    /**
     * 根据数据源配置打开 JDBC 连接。
     *
     * @param config 数据源配置
     * @return JDBC 连接
     * @throws SQLException 连接失败时抛出
     */
    public static Connection openConnection(DataSourceConfig config) throws SQLException {
        String url = buildMysqlJdbcUrl(config);
        return DriverManager.getConnection(url, config.getUsername(), config.getPassword());
    }

    /**
     * 执行无返回结果的 SQL，例如 DDL 语句。
     *
     * @param config 数据源配置
     * @param sql    待执行 SQL
     */
    public static void executeSql(DataSourceConfig config, String sql) {
        try (Connection conn = openConnection(config);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to execute SQL: " + sql, ex);
        }
    }

    /**
     * 构建 MySQL JDBC URL。
     *
     * <p>默认参数会始终补充到连接串中，调用方传入的 connectionParams 可以覆盖默认值。
     * connectionParams 支持以 ? 开头，也支持 key1=value1&key2=value2 的形式。</p>
     *
     * @param config 数据源配置
     * @return MySQL JDBC URL
     */
    public static String buildMysqlJdbcUrl(DataSourceConfig config) {
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
     * 构建连接参数字符串，并合并默认参数和额外参数。
     *
     * @param extraParams 额外连接参数，格式示例：key1=value1&key2=value2
     * @return 合并后的连接参数字符串
     */
    public static String buildConnectionParams(String extraParams) {
        Map<String, String> params = new HashMap<>();
        params.put("useUnicode", "true");
        params.put("characterEncoding", "utf8");
        params.put("serverTimezone", DEFAULT_TIME_ZONE);
        params.put("useSSL", "false");

        if (StringUtils.hasText(extraParams)) {
            String normalizedExtraParams = extraParams.trim();
            if (normalizedExtraParams.startsWith("?")) {
                normalizedExtraParams = normalizedExtraParams.substring(1);
            }
            String[] pairs = normalizedExtraParams.split("&");
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
     * 检查指定表是否存在。
     *
     * @param config    数据源配置
     * @param tableName 表名
     * @return true 表示表存在
     */
    public static boolean tableExists(DataSourceConfig config, String tableName) {
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
     * 加载指定表的字段信息。
     *
     * @param config    数据源配置
     * @param tableName 表名
     * @return 字段名到字段类型的映射，字段名已转为小写并去除首尾空白
     */
    public static Map<String, String> loadTableColumns(DataSourceConfig config, String tableName) {
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
     * 规范化列名，用于比较数据库字段和请求字段。
     *
     * @param columnName 列名
     * @return 规范化后的列名
     */
    public static String normalizeColumnName(String columnName) {
        return columnName == null ? null : columnName.trim().toLowerCase();
    }

    /**
     * 规范化字段类型，忽略大小写和多余空白后再比较。
     *
     * @param columnType 字段类型
     * @return 规范化后的字段类型
     */
    public static String normalizeColumnType(String columnType) {
        if (!StringUtils.hasText(columnType)) {
            return "";
        }
        return columnType.trim().toLowerCase().replaceAll("\\s+", "");
    }

}
