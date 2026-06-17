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
 * 采集任务模板版本实体（T-06 task_template_version）
 * <p>保存 Python 任务处理层每次同步形成的模板快照，包含返回字段 JSON、
 * 字段哈希和原始同步报文，用于历史追溯。</p>
 */
@Data
@Accessors(chain = true)
@TableName("task_template_version")
public class TaskTemplateVersion {

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 任务编码（关联 task_template.task_code） */
    @TableField("task_code")
    private String taskCode;

    /** 版本号（单个任务内自增） */
    @TableField("version_no")
    private Integer versionNo;

    /** 任务名称快照 */
    @TableField("task_name")
    private String taskName;

    /** 任务说明 */
    @TableField("task_desc")
    private String taskDesc;

    /** 任务处理入口（Python handler 名称） */
    @TableField("handler_name")
    private String handlerName;

    /** 数据源标识 */
    @TableField("data_source")
    private String dataSource;

    /** 资产类型 */
    @TableField("asset_type")
    private String assetType;

    /** 业务分类 */
    @TableField("biz_type")
    private String bizType;

    /** 执行参数结构 JSON（paramsSchema） */
    @TableField("params_schema_json")
    private String paramsSchemaJson;

    /** 接口可返回字段结构 JSON（outputFields） */
    @TableField("output_fields_json")
    private String outputFieldsJson;

    /** 返回字段哈希（用于幂等判断） */
    @TableField("output_fields_hash")
    private String outputFieldsHash;

    /** 可选清洗规则类别编码列表 JSON */
    @TableField("rule_category_codes")
    private String ruleCategoryCodes;

    /** 变更摘要 */
    @TableField("change_summary")
    private String changeSummary;

    /** 同步时间 */
    @TableField("sync_time")
    private LocalDateTime syncTime;

    /** 创建时间（自动填充） */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 创建人（自动填充） */
    @TableField(value = "created_by", fill = FieldFill.INSERT)
    private String createdBy;
}
