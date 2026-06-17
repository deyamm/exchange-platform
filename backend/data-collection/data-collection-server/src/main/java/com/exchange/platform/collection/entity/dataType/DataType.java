package com.exchange.platform.collection.entity.dataType;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.exchange.platform.collection.api.enums.CommonStatus;
import com.exchange.platform.collection.api.enums.DataTypeNodeType;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 数据类型实体类，包含数据类型的基本信息和属性
 */
@Data
@Accessors (chain = true)
@TableName("data_type")
public class DataType {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("data_type_code")
    private String dataTypeCode;

    @TableField("data_type_name")
    private String dataTypeName;

    @TableField("data_type_label")
    private String dataTypeLabel;

    @TableField("parent_code")
    private String parentCode;

    @TableField("node_type")
    private DataTypeNodeType nodeType;

    @TableField("sort_no")
    private Integer sortNo;

    @TableField("node_level")
    private Integer nodeLevel;

    @TableField("is_leaf")
    private Boolean isLeaf;

    @TableField("status")
    private CommonStatus status;

    @TableField("version")
    private Integer version;

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
