package com.exchange.platform.collection.entity.dataType;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 数据类型的逻辑表字段
 */
@Data
@Accessors(chain = true)
@TableName("data_type_field")
public class DataTypeField {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("data_type_code")
    private String dataTypeCode;

    @TableField("field_code")
    private String fieldCode;

    @TableField("field_name")
    private String fieldName;

    @TableField("field_type")
    private String fieldType;

    @TableField("default_value")
    private String defaultValue;

    @TableField("required")
    private Boolean required;

    @TableField("unique_key")
    private Boolean uniqueKey;

    @TableField("sort_no")
    private Integer sortNo;

    @TableField("description")
    private String description;

    @TableField(value = "created_by", fill = FieldFill.INSERT)
    private String createdBy;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_by", fill = FieldFill.INSERT_UPDATE)
    private String updatedBy;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
