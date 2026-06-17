package com.exchange.platform.collection.entity.tasks;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.exchange.platform.collection.api.enums.MappingStatus;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 存储映射主表实体（T-08 storage_mapping）
 * <p>保存任务配置、任务模板版本、数据类型版本与物理表之间的映射主信息；
 * 字段级映射见 StorageColumnMapping。</p>
 */
@Data
@Accessors(chain = true)
@TableName("storage_mapping")
public class StorageMapping {

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 存储映射编码，全局唯一 */
    @TableField("storage_mapping_code")
    private String storageMappingCode;

    /** 关联的任务配置编码 */
    @TableField("task_config_code")
    private String taskConfigCode;

    /** 数据源编码 */
    @TableField("data_source_code")
    private String dataSourceCode;

    /** 目标物理库名 */
    @TableField("physical_schema_name")
    private String physicalSchemaName;

    /** 目标物理表名 */
    @TableField("physical_table_name")
    private String physicalTableName;

    /** 写入策略（INSERT / UPSERT / REPLACE） */
    @TableField("write_strategy")
    private String writeStrategy;

    /** 映射状态：DRAFT / CONFIRMED / ENABLED / DISABLED */
    @TableField("mapping_status")
    private MappingStatus mappingStatus;

    /** 确认备注 */
    @TableField("confirm_remark")
    private String confirmRemark;

    /** 创建时间（自动填充） */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间（自动填充） */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 创建人（自动填充） */
    @TableField(value = "created_by", fill = FieldFill.INSERT)
    private String createdBy;

    /** 更新人（自动填充） */
    @TableField(value = "updated_by", fill = FieldFill.INSERT_UPDATE)
    private String updatedBy;
}
