package com.exchange.platform.analytics.api.registry;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.exchange.platform.analytics.api.enums.MasterDataType;

/**
 * 用于主数据类型注册表的定义。
 */
public record MasterDataTypeDefinition(
    // 主数据类型代码，唯一标识
    MasterDataType typeCode, 
    // 用于展示的名称
    String displayName,
    // 主数据所属的领域，暂时还未界定范围
    String domain,
    // 领域中所属的类别，暂时还未界定范围
    String category,
    // 主数据表中所有字段的定义，包含业务字段和扩展字段，该字段也对应物理表中的列定义
    List<MdFieldDefinition> fields,
    // 主数据的业务主键字段列表，必须是 fields 中的字段
    List<String> businessKeyFields,
    // 主数据字段与物理表的列名映射
    StorageDefinition storage,
    // 主数据的校验规则列表，暂未使用
    List<ValidationRule> validationRules
) {
    public MasterDataTypeDefinition {
        if (typeCode == null || fields == null || fields.isEmpty() || storage == null) {
            throw new IllegalArgumentException("主数据定义不完整");
        }
        fields = fields == null ? List.of() : List.copyOf(fields);
        businessKeyFields = businessKeyFields == null ? List.of() : List.copyOf(businessKeyFields);
        validationRules = validationRules == null ? List.of() : List.copyOf(validationRules);
        List<MdFieldDefinition> definedFields = fields;
        var codes = new java.util.HashSet<String>();
        fields.forEach(field -> {
            if (field == null || field.fieldCode() == null || !codes.add(field.fieldCode())) {
                throw new IllegalArgumentException("主数据字段编码重复或为空");
            }
        });
        if (businessKeyFields.isEmpty()) {
            throw new IllegalArgumentException("主数据业务主键不能为空");
        }
        businessKeyFields.forEach(key -> {
            MdFieldDefinition field = definedFields.stream().filter(item -> key.equals(item.fieldCode())).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("业务主键字段未定义: " + key));
            if (!field.required() || !storage.columns().containsKey(key)) {
                throw new IllegalArgumentException("业务主键字段必须为必填且映射到存储列: " + key);
            }
        });
    }

    /**
     * 根据主数据记录获取业务主键字段的值，返回一个不可变的 Map。
     * @param record
     * @return
     */
    public Map<String, Object> businessKeyOf(Map<String, Object> record) {
        Map<String, Object> key = new LinkedHashMap<>();
        for (String field : businessKeyFields) {
            if (record == null || !record.containsKey(field) || record.get(field) == null
                    || (record.get(field) instanceof String text && text.isBlank())) {
                throw new IllegalArgumentException("业务主键字段不能为空: " + field);
            }
            key.put(field, record.get(field) instanceof String text ? text.trim() : record.get(field));
        }
        return Collections.unmodifiableMap(key);
    }
}
