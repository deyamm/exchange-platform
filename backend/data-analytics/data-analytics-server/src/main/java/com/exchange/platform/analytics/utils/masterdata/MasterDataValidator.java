package com.exchange.platform.analytics.utils.masterdata;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.exchange.platform.analytics.api.registry.MasterDataTypeDefinition;
import com.exchange.platform.analytics.api.registry.MdFieldDefinition;
import com.exchange.platform.analytics.api.registry.ValidationRule;

public class MasterDataValidator {

    public static void validate(MasterDataTypeDefinition definition, List<Map<String, Object>> candidates) {
        Set<BusinessKey> keys = new HashSet<>();
        for (Map<String, Object> candidate : candidates) {
            for (MdFieldDefinition field : definition.fields()) {
                if (field.required() && candidate.get(field.fieldCode()) == null) {
                    throw new IllegalArgumentException("字段 " + field.fieldCode() + " 必填");
                }
            }
            Map<String, Object> keyValues = definition.businessKeyOf(candidate);
            if (!keys.add(new BusinessKey(keyValues)))
                throw new IllegalArgumentException("重复业务主键: " + keyValues);
        }
        for (ValidationRule rule : definition.validationRules()) {
            switch (rule.type()) {
                case "INTEGER_MIN" -> validateMin(rule, candidates);
                case "DATE_ORDER" -> validateDateOrder(rule, candidates);
                default -> throw new IllegalArgumentException("不支持的主数据规则: " + rule.type());
            }
        }
    }

    public static void validateMin(ValidationRule rule, List<Map<String, Object>> rows) {
        String field = rule.arguments().get(0);
        int min = Integer.parseInt(rule.arguments().get(1));
        for (Map<String, Object> row : rows) {
            Object value = row.get(field);
            if (value != null && ((Number) value).intValue() < min)
                throw new IllegalArgumentException("字段 " + field + " 不得小于 " + min);
        }
    }

    public static void validateDateOrder(ValidationRule rule, List<Map<String, Object>> rows) {
        String from = rule.arguments().get(0), to = rule.arguments().get(1);
        for (Map<String, Object> row : rows) {
            Object left = row.get(from), right = row.get(to);
            if (left != null && right != null && ((LocalDate) left).isAfter((LocalDate) right))
                throw new IllegalArgumentException("字段 " + from + " 不得晚于 " + to);
        }
    }

    public static record BusinessKey(Map<String, Object> values) {
        public BusinessKey {
            values = Map.copyOf(new LinkedHashMap<>(values));
        }
    }
}
