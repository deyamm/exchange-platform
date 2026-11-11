package com.exchange.platform.collection.api.enums;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 统一元数据标准定义，也是后端维护接口的校验依据。 */
public enum DatasetMetadataDefinition {
    BUSINESS_DEFINITION("BUSINESS", "business_definition", "业务定义", "STRING", DatasetMetadataMaintainMode.MANUAL),
    BUSINESS_SCOPE("BUSINESS", "business_scope", "业务范围", "STRING", DatasetMetadataMaintainMode.MANUAL),
    DATA_GRANULARITY("BUSINESS", "data_granularity", "数据粒度", "JSON", DatasetMetadataMaintainMode.MANUAL),
    DATA_TIME_FIELD("BUSINESS", "data_time_field", "数据时间字段", "STRING", DatasetMetadataMaintainMode.MANUAL),
    UNIT("BUSINESS", "unit", "数据单位", "STRING", DatasetMetadataMaintainMode.MANUAL),
    CURRENCY("BUSINESS", "currency", "货币类型", "STRING", DatasetMetadataMaintainMode.MANUAL),
    BUSINESS_KEY("BUSINESS", "business_key", "业务主键", "JSON", DatasetMetadataMaintainMode.CONFIG_DERIVED),
    DATA_OWNER("MANAGEMENT", "data_owner", "数据负责人", "STRING", DatasetMetadataMaintainMode.MANUAL),
    MAINTAINER("MANAGEMENT", "maintainer", "维护人员", "STRING", DatasetMetadataMaintainMode.MANUAL),
    CONTACT("MANAGEMENT", "contact", "联系方式", "STRING", DatasetMetadataMaintainMode.MANUAL),
    UPDATE_FREQUENCY("MANAGEMENT", "update_frequency", "更新频率", "JSON", DatasetMetadataMaintainMode.MANUAL),
    SENSITIVITY_LEVEL("MANAGEMENT", "sensitivity_level", "敏感级别", "STRING", DatasetMetadataMaintainMode.MANUAL),
    USAGE_SCOPE("MANAGEMENT", "usage_scope", "使用范围", "STRING", DatasetMetadataMaintainMode.MANUAL),
    RETENTION_POLICY("MANAGEMENT", "retention_policy", "保留策略", "STRING", DatasetMetadataMaintainMode.MANUAL),
    STORAGE_TYPE("STORAGE", "storage_type", "存储类型", "STRING", DatasetMetadataMaintainMode.CONFIG_DERIVED),
    DATASOURCE_CODE("STORAGE", "datasource_code", "数据源编码", "STRING", DatasetMetadataMaintainMode.CONFIG_DERIVED),
    SCHEMA_NAME("STORAGE", "schema_name", "Schema", "STRING", DatasetMetadataMaintainMode.CONFIG_DERIVED),
    TABLE_NAME("STORAGE", "table_name", "物理表", "STRING", DatasetMetadataMaintainMode.CONFIG_DERIVED),
    STORAGE_UNIQUE_KEY("STORAGE", "storage_unique_key", "物理唯一键", "JSON", DatasetMetadataMaintainMode.CONFIG_DERIVED),
    RECORD_COUNT("STATISTICS", "record_count", "数据总条数", "NUMBER", DatasetMetadataMaintainMode.EXECUTION_DERIVED),
    TIME_COVERAGE_FIELD("STATISTICS", "time_coverage_field", "时间覆盖统计字段", "STRING",
            DatasetMetadataMaintainMode.EXECUTION_DERIVED),
    TIME_COVERAGE_START("STATISTICS", "time_coverage_start", "数据起始时间", "STRING",
            DatasetMetadataMaintainMode.EXECUTION_DERIVED),
    TIME_COVERAGE_END("STATISTICS", "time_coverage_end", "数据结束时间", "STRING",
            DatasetMetadataMaintainMode.EXECUTION_DERIVED),
    STATISTICS_UPDATED_AT("STATISTICS", "statistics_updated_at", "统计更新时间", "STRING",
            DatasetMetadataMaintainMode.EXECUTION_DERIVED),
    TAGS("EXTENSION", "tags", "数据标签", "JSON", DatasetMetadataMaintainMode.MANUAL),
    KEYWORDS("EXTENSION", "keywords", "关键词", "JSON", DatasetMetadataMaintainMode.MANUAL),
    CUSTOM_REMARK("EXTENSION", "custom_remark", "扩展说明", "STRING", DatasetMetadataMaintainMode.MANUAL),
    CUSTOM_PROPERTIES("EXTENSION", "custom_properties", "自定义属性", "JSON", DatasetMetadataMaintainMode.MANUAL);

    private static final Map<String, DatasetMetadataDefinition> INDEX = Collections.unmodifiableMap(
            Arrays.stream(values()).collect(Collectors.toMap(d -> d.group + ":" + d.key, Function.identity())));
    private final String group;
    private final String key;
    private final String name;
    private final String valueType;
    private final DatasetMetadataMaintainMode maintainMode;

    DatasetMetadataDefinition(String group, String key, String name, String valueType,
            DatasetMetadataMaintainMode maintainMode) {
        this.group = group;
        this.key = key;
        this.name = name;
        this.valueType = valueType;
        this.maintainMode = maintainMode;
    }

    public String getGroup() {
        return group;
    }

    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public String getValueType() {
        return valueType;
    }

    public DatasetMetadataMaintainMode getMaintainMode() {
        return maintainMode;
    }

    public static DatasetMetadataDefinition find(String group, String key) {
        return INDEX.get(group + ":" + key);
    }
}
