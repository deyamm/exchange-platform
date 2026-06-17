package com.exchange.platform.collection.entity.tasks;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 存储字段映射实体（T-09 storage_column_mapping）
 * <p>保存来源字段（任务返回字段）、数据类型逻辑字段与目标物理列之间的字段级映射；
 * 一条 StorageMapping 对应多条 StorageColumnMapping。</p>
 */
@Data
@Accessors(chain = true)
@TableName("storage_column_mapping")
public class StorageColumnMapping {

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 所属存储映射编码（关联 storage_mapping.storage_mapping_code） */
    @TableField("storage_mapping_code")
    private String storageMappingCode;

    /** 来源字段编码（任务返回字段中的 fieldCode） */
    @TableField("source_field_code")
    private String sourceFieldCode;

    /** 来源字段名称（展示用） */
    @TableField("source_field_name")
    private String sourceFieldName;

    /** 来源字段类型 */
    @TableField("source_field_type")
    private String sourceFieldType;

    /** 目标数据类型字段编码（关联 data_type_field.field_code） */
    @TableField("data_type_field_code")
    private String dataTypeFieldCode;

    /** 目标数据类型字段类型 */
    @TableField("data_type_field_type")
    private String dataTypeFieldType;

    /** 目标物理列名 */
    @TableField("physical_column_name")
    private String physicalColumnName;

    /** 目标物理列类型 */
    @TableField("physical_column_type")
    private String physicalColumnType;

    /** 默认值（可选） */
    @TableField("default_value")
    private String defaultValue;

    /** 是否必填字段 */
    @TableField("required")
    private Boolean required;

    /** 是否业务唯一键字段 */
    @TableField("unique_key")
    private Boolean uniqueKey;

    /** 创建时间（自动填充） */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间（自动填充） */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
