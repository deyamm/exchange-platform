package com.exchange.platform.collection.entity.dataType;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.exchange.platform.collection.api.enums.PublishStatus;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 数据类型版本实体类
 * 用于记录数据类型的版本信息，支持版本控制和历史记录查询
 */
@Data
@Accessors(chain = true)
@TableName("data_type_version")
public class DataTypeVersion {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("version")
    private Integer version;

    @TableField("data_type_code")
    private String dataTypeCode;
    
    @TableField("version_name")
    private String versionName;

    @TableField("field_schema_content")
    private String fieldSchemaContent;

    @TableField("publish_status")
    private PublishStatus publishStatus;

    @TableField("publish_time")
    private LocalDateTime publishTime;

    @TableField("change_summary")
    private String changeSummary;

    @TableField(value = "created_by", fill = FieldFill.INSERT)
    private String createdBy;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_by", fill = FieldFill.INSERT_UPDATE)
    private String updatedBy;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
