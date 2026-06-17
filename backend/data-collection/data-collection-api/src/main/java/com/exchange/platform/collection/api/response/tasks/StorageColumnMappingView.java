package com.exchange.platform.collection.api.response.tasks;

import lombok.Data;

/**
 * 存储字段映射视图对象，用于展示存储映射的字段级别信息
 * 包含源字段、目标字段、数据类型等信息
 */
@Data
public class StorageColumnMappingView {

    /** 存储映射编码*/
    private String storageMappingCode;

    /** 来源字段编码 */
    private String sourceFieldCode;

    /** 来源字段名称 */
    private String sourceFieldName;

    /** 来源字段类型 */
    private String sourceFieldType;

    /** 目标数据类型字段编码 */
    private String dataTypeFieldCode;

    /** 目标数据类型字段类型 */
    private String dataTypeFieldType;

    /** 目标物理列名 */
    private String physicalColumnName;

    /** 目标物理列类型 */
    private String physicalColumnType;

    /** 默认值（可选） */
    private String defaultValue;

    /** 是否必填 */
    private Boolean required;

    /** 是否业务唯一键 */
    private Boolean uniqueKey;
}
