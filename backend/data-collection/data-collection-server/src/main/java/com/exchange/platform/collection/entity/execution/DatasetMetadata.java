package com.exchange.platform.collection.entity.execution;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.experimental.Accessors;

/** 数据集扩展元数据实体，对应 dataset_metadata 表。 */
@Data
@Accessors(chain = true)
@TableName("dataset_metadata")
public class DatasetMetadata {

    /** 主键。 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /** 所属数据集主档 ID。 */
    @TableField("dataset_id")
    private Long datasetId;
    /** 元数据分组。 */
    @TableField("metadata_group")
    private String metadataGroup;
    /** 元数据编码。 */
    @TableField("metadata_key")
    private String metadataKey;
    /** 元数据名称。 */
    @TableField("metadata_name")
    private String metadataName;
    /** 元数据值。 */
    @TableField("metadata_value")
    private String metadataValue;
    /** 元数据值类型。 */
    @TableField("value_type")
    private String valueType;
    /** 备注。 */
    @TableField("remark")
    private String remark;
    /** 创建时间。 */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    /** 更新时间。 */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    /** 创建人。 */
    @TableField(value = "created_by", fill = FieldFill.INSERT)
    private String createdBy;
    /** 更新人。 */
    @TableField(value = "updated_by", fill = FieldFill.INSERT_UPDATE)
    private String updatedBy;
}
