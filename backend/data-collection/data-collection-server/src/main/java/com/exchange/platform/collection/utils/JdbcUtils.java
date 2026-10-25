package com.exchange.platform.collection.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
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
 *     <li>SQL 标识符校验与反引号包装</li>
 *     <li>JDBC 时间类型与业务层时间类型的转换</li>
 * </ul>
 *
 * <p>该类只放置无状态的公共逻辑，不持有 Spring Bean，也不承载业务校验。</p>
 */
public final class JdbcUtils {

    /** 默认数据库时区，与现有业务保持一致。 */
    public static final String DEFAULT_TIME_ZONE = "Asia/Shanghai";

    /** 业务表调度相关 SQL 标识符只允许字母、数字、下划线，避免拼接 SQL 时引入注入风险。 */
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[A-Za-z0-9_]+$");

    /** 日期格式化器：yyyy-MM-dd。 */
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    /** 日期时间格式化器：yyyy-MM-ddTHH:mm:ss。 */
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

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

    /**
     * 校验 SQL 标识符，防止拼接表名、字段名时引入非法字符。
     *
     * @param identifier 标识符
     * @param fieldName  字段中文名，用于异常提示
     */
    public static void validateIdentifier(String identifier, String fieldName) {
        if (!StringUtils.hasText(identifier) || !IDENTIFIER_PATTERN.matcher(identifier).matches()) {
            throw new IllegalArgumentException(fieldName + "包含非法字符：" + identifier);
        }
    }

    /**
     * 校验并使用反引号包装 SQL 标识符。
     *
     * @param identifier 标识符
     * @return 反引号包装后的标识符
     */
    public static String quoteIdentifier(String identifier) {
        validateIdentifier(identifier, "SQL 标识符");
        return "`" + identifier + "`";
    }

    /**
     * 拼接 schema.table 形式的完整表名。
     *
     * @param schemaName schema 名
     * @param tableName  表名
     * @return 反引号包装后的完整表名
     */
    public static String qualifiedTableName(String schemaName, String tableName) {
        validateIdentifier(schemaName, "业务表 schema");
        validateIdentifier(tableName, "业务表名");
        return quoteIdentifier(schemaName) + "." + quoteIdentifier(tableName);
    }

    /**
     * 将 JDBC 查询结果中的时间类型转换为便于 JSON 序列化的字符串。
     *
     * @param value JDBC 查询结果值
     * @return 普通值或格式化后的时间字符串
     */
    public static Object normalizeJdbcValue(Object value) {
        if (value instanceof Timestamp) {
            return DATETIME_FORMAT.format(((Timestamp) value).toLocalDateTime());
        }
        if (value instanceof java.sql.Date) {
            return DATE_FORMAT.format(((java.sql.Date) value).toLocalDate());
        }
        if (value instanceof java.sql.Time) {
            return value.toString();
        }
        return value;
    }

    /**
     * 将 JDBC 查询结果转换为 LocalDateTime。
     *
     * @param value JDBC 查询结果值
     * @return 转换后的 LocalDateTime
     */
    public static LocalDateTime toLocalDateTime(Object value) {
        return toLocalDateTime(value, DEFAULT_TIME_ZONE);
    }

    /**
     * 将 JDBC 查询结果转换为 LocalDateTime。
     *
     * @param value           JDBC 查询结果值
     * @param defaultTimeZone 遇到 time 类型时使用的默认日期所属时区
     * @return 转换后的 LocalDateTime
     */
    public static LocalDateTime toLocalDateTime(Object value, String defaultTimeZone) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime) {
            return (LocalDateTime) value;
        }
        if (value instanceof LocalDate) {
            return ((LocalDate) value).atStartOfDay();
        }
        if (value instanceof Timestamp) {
            return ((Timestamp) value).toLocalDateTime();
        }
        if (value instanceof java.sql.Date) {
            return ((java.sql.Date) value).toLocalDate().atStartOfDay();
        }
        if (value instanceof java.sql.Time) {
            return LocalDate.now(resolveZoneId(defaultTimeZone)).atTime(((java.sql.Time) value).toLocalTime());
        }

        String text = String.valueOf(value);
        try {
            return LocalDateTime.parse(text, DATETIME_FORMAT);
        } catch (Exception ignored) {
            return LocalDate.parse(text, DATE_FORMAT).atStartOfDay();
        }
    }

    /**
     * 解析时区字符串，空值时使用默认时区。
     *
     * @param timeZone 时区 ID
     * @return ZoneId
     */
    public static ZoneId resolveZoneId(String timeZone) {
        if (timeZone == null || timeZone.isBlank()) {
            return ZoneId.of(DEFAULT_TIME_ZONE);
        }
        return ZoneId.of(timeZone);
    }
}