package com.exchange.platform.analytics.utils.masterdata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.exchange.platform.analytics.api.registry.MasterDataTypeDefinition;
import com.exchange.platform.analytics.api.registry.MdFieldDefinition;
import com.exchange.platform.analytics.service.masterdata.MasterDataQuery;

/** Dynamic single-table store driven exclusively by a trusted Definition. */
@Repository
public class MasterDataStore {
    private final JdbcTemplate jdbc;

    public MasterDataStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Map<String, Object> findOne(MasterDataTypeDefinition definition, Map<String, Object> businessKey) {
        MasterDataTypeDefinition d = require(definition);
        Map<String, Object> key = d.businessKeyOf(businessKey);
        String sql = "SELECT " + selectColumns(d) + " FROM " + d.storage().tableName() + where(d, key);
        List<Map<String, Object>> rows = jdbc.queryForList(sql, key.values().toArray());
        return rows.isEmpty() ? null : normalize(d, rows.get(0));
    }

    public List<Map<String, Object>> findByBusinessKeys(MasterDataTypeDefinition definition,
            Collection<Map<String, Object>> businessKeys) {
        if (businessKeys == null || businessKeys.isEmpty())
            return List.of();
        MasterDataTypeDefinition d = require(definition);
        List<Map<String, Object>> normalized = businessKeys.stream().map(d::businessKeyOf).toList();
        List<Map<String, Object>> result = new ArrayList<>();
        for (int offset = 0; offset < normalized.size(); offset += 500) {
            List<Map<String, Object>> chunk = normalized.subList(offset, Math.min(offset + 500, normalized.size()));
            List<Object> args = new ArrayList<>();
            StringJoiner predicates = new StringJoiner(" OR ");
            for (Map<String, Object> key : chunk) {
                predicates.add(key.keySet().stream().map(field -> column(d, field) + " = ?")
                        .reduce((a, b) -> a + " AND " + b).orElseThrow());
                args.addAll(key.values());
            }
            String sql = "SELECT " + selectColumns(d) + " FROM " + d.storage().tableName() + " WHERE (" + predicates
                    + ")";
            result.addAll(jdbc.queryForList(sql, args.toArray()).stream().map(row -> normalize(d, row)).toList());
        }
        return result;
    }

    public List<Map<String, Object>> query(MasterDataTypeDefinition definition, MasterDataQuery query) {
        MasterDataTypeDefinition d = require(definition);
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT ").append(selectColumns(d)).append(" FROM ")
                .append(d.storage().tableName()).append(" WHERE 1=1");
        appendFilters(d, query, sql, args);
        if (query.orderBy() != null) {
            sql.append(" ORDER BY ").append(column(d, query.orderBy())).append(query.ascending() ? " ASC" : " DESC");
        }
        sql.append(" LIMIT ? OFFSET ?");
        args.add(query.pageSize());
        args.add((long) (query.pageNo() - 1) * query.pageSize());
        return jdbc.queryForList(sql.toString(), args.toArray()).stream().map(row -> normalize(d, row)).toList();
    }

    public long countAll(MasterDataTypeDefinition definition) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + require(definition).storage().tableName(), Long.class);
    }

    public long count(MasterDataTypeDefinition definition, MasterDataQuery query) {
        MasterDataTypeDefinition d = require(definition);
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM ").append(d.storage().tableName())
                .append(" WHERE 1=1");
        appendFilters(d, query, sql, args);
        Long value = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
        return value == null ? 0L : value;
    }

    public int update(MasterDataTypeDefinition definition, Map<String, Object> businessKey,
            Map<String, Object> changes) {
        MasterDataTypeDefinition d = require(definition);
        Map<String, Object> key = d.businessKeyOf(businessKey);
        if (changes == null || changes.isEmpty())
            return 0;
        List<String> assignments = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        for (Map.Entry<String, Object> entry : changes.entrySet()) {
            String field = entry.getKey();
            if (!d.storage().columns().containsKey(field))
                throw new IllegalArgumentException("未知主数据字段: " + field);
            if (d.businessKeyFields().contains(field))
                throw new IllegalArgumentException("不可更新业务主键: " + field);
            assignments.add(column(d, field) + " = ?");
            args.add(entry.getValue());
        }
        args.addAll(key.values());
        return jdbc.update(
                "UPDATE " + d.storage().tableName() + " SET " + String.join(", ", assignments) + where(d, key),
                args.toArray());
    }

    public void upsert(MasterDataTypeDefinition definition, Map<String, Object> record) {
        MasterDataTypeDefinition d = require(definition);
        Map<String, Object> key = d.businessKeyOf(record);
        Map<String, Object> existing = findOne(d, key);
        if (existing == null) {
            List<String> fields = d.fields().stream().map(MdFieldDefinition::fieldCode).filter(record::containsKey)
                    .toList();
            StringJoiner columns = new StringJoiner(", ");
            StringJoiner placeholders = new StringJoiner(", ");
            List<Object> args = new ArrayList<>();
            fields.forEach(field -> {
                columns.add(column(d, field));
                placeholders.add("?");
                args.add(record.get(field));
            });
            jdbc.update("INSERT INTO " + d.storage().tableName() + " (" + columns + ") VALUES (" + placeholders + ")",
                    args.toArray());
        } else {
            Map<String, Object> changes = new LinkedHashMap<>(record);
            d.businessKeyFields().forEach(changes::remove);
            update(d, key, changes);
        }
    }

    public int delete(MasterDataTypeDefinition definition, Map<String, Object> businessKey) {
        MasterDataTypeDefinition d = require(definition);
        Map<String, Object> key = d.businessKeyOf(businessKey);
        return jdbc.update("DELETE FROM " + d.storage().tableName() + where(d, key), key.values().toArray());
    }

    private void appendFilters(MasterDataTypeDefinition d, MasterDataQuery query, StringBuilder sql,
            List<Object> args) {
        query.equalsFilters().forEach((field, value) -> {
            sql.append(" AND ").append(column(d, field)).append(" = ?");
            args.add(value);
        });
        query.inFilters().forEach((field, values) -> {
            if (values == null || values.isEmpty()) {
                sql.append(" AND 1=0");
                return;
            }
            StringJoiner placeholders = new StringJoiner(", ");
            values.forEach(value -> {
                placeholders.add("?");
                args.add(value);
            });
            sql.append(" AND ").append(column(d, field)).append(" IN (").append(placeholders).append(")");
        });
        if (query.keyword() != null && !query.keyword().isBlank() && !query.keywordFields().isEmpty()) {
            sql.append(" AND (");
            StringJoiner join = new StringJoiner(" OR ");
            for (String field : query.keywordFields()) {
                join.add(column(d, field) + " LIKE ?");
                args.add("%" + query.keyword().trim() + "%");
            }
            sql.append(join).append(")");
        }
    }

    private String where(MasterDataTypeDefinition d, Map<String, Object> key) {
        StringJoiner join = new StringJoiner(" AND ", " WHERE ", "");
        key.keySet().forEach(field -> join.add(column(d, field) + " = ?"));
        return join.toString();
    }

    private String selectColumns(MasterDataTypeDefinition d) {
        return d.fields().stream().map(MdFieldDefinition::fieldCode).map(field -> column(d, field) + " AS " + field)
                .reduce((a, b) -> a + ", " + b).orElseThrow();
    }

    private String column(MasterDataTypeDefinition d, String field) {
        String column = d.storage().columns().get(field);
        if (column == null)
            throw new IllegalArgumentException("未知主数据字段: " + field);
        return column;
    }

    private Map<String, Object> normalize(MasterDataTypeDefinition d, Map<String, Object> row) {
        return new LinkedHashMap<>(row);
    }

    private MasterDataTypeDefinition require(MasterDataTypeDefinition d) {
        if (d == null)
            throw new IllegalArgumentException("主数据定义不能为空");
        return d;
    }
}
