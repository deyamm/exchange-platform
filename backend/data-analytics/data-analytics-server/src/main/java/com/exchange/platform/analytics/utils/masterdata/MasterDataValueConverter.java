package com.exchange.platform.analytics.utils.masterdata;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.Locale;

import com.exchange.platform.analytics.api.registry.MdFieldDefinition;

/** Converts Dataset values using field metadata only. */

public class MasterDataValueConverter {

    public static Object convert(MdFieldDefinition field, Object raw) {
        if (raw == null || raw.toString().isBlank()) return null;
        String value = raw.toString().trim();
        try {
            return switch (field.valueType().toUpperCase(Locale.ROOT)) {
                case "STRING" -> value;
                case "INTEGER" -> raw instanceof Number n ? n.intValue() : Integer.valueOf(value);
                case "DATE" -> toDate(raw, value);
                case "DATETIME" -> toDateTime(raw, value);
                case "ENUM" -> enumValue(field, value);
                default -> throw new IllegalArgumentException("不支持的字段类型 " + field.valueType());
            };
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("字段 " + field.fieldCode() + " 类型转换失败: " + value, ex);
        }
    }

    /** 将 JDBC 读取值规范化为 API 稳定类型；枚举统一暴露业务 code 字符串。 */
    public static Object read(MdFieldDefinition field, Object raw) {
        if (raw == null) return null;
        String value = raw.toString().trim();
        try {
            return switch (field.valueType().toUpperCase(Locale.ROOT)) {
                case "STRING" -> value;
                case "INTEGER" -> raw instanceof Number n ? n.intValue() : Integer.valueOf(value);
                case "DATE" -> toDate(raw, value);
                case "DATETIME" -> toDateTime(raw, value);
                case "ENUM" -> enumCode(raw, field);
                default -> throw new IllegalArgumentException("不支持的字段类型 " + field.valueType());
            };
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("字段 " + field.fieldCode() + " JDBC 类型转换失败: " + value, ex);
        }
    }

    private static LocalDate toDate(Object raw, String value) {
        if (raw instanceof LocalDate d) return d;
        if (raw instanceof LocalDateTime dt) return dt.toLocalDate();
        if (raw instanceof Date d) return d.toLocalDate();
        if (raw instanceof Timestamp ts) return ts.toLocalDateTime().toLocalDate();
        return LocalDate.parse(value);
    }

    private static LocalDateTime toDateTime(Object raw, String value) {
        if (raw instanceof LocalDateTime dt) return dt;
        if (raw instanceof Timestamp ts) return ts.toLocalDateTime();
        if (raw instanceof Date d) return d.toLocalDate().atStartOfDay();
        return LocalDateTime.parse(value);
    }

    private static String enumCode(Object raw, MdFieldDefinition field) {
        if (raw instanceof Enum<?> e) {
            try {
                return String.valueOf(e.getClass().getMethod("getValue").invoke(e));
            } catch (ReflectiveOperationException ignored) {
                return e.name();
            }
        }
        return raw.toString();
    }

    private static Object enumValue(MdFieldDefinition field, String value) {
        if (field.enumType() == null || field.enumType().isBlank()) return value;
        try {
            Class<?> type = Class.forName(field.enumType());
            if (!type.isEnum()) throw new IllegalArgumentException("不是枚举类型");
            for (Object constant : type.getEnumConstants()) {
                if (value.equalsIgnoreCase(((Enum<?>) constant).name())) return constant;
                try {
                    Object code = type.getMethod("getValue").invoke(constant);
                    if (value.equalsIgnoreCase(String.valueOf(code))) return constant;
                } catch (NoSuchMethodException ignored) {
                    // Name-only enums, such as status, use the branch above.
                }
            }
            throw new IllegalArgumentException("枚举值不合法");
        } catch (ReflectiveOperationException ex) {
            throw new IllegalArgumentException("枚举定义不可用", ex);
        }
    }
}
