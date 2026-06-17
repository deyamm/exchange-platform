package com.exchange.platform.collection.entity.execution;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@TableName("execution_config_snapshot")
public class ExecutionConfigSnapshot {
    
    /* 主键（自增） */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 快照编号 */
    @TableField("snapshot_id")
    private String snapshotId;

    /** 执行记录编号，这里认为snapshot是一次执行后的快照内容，所以关联的是TaskRun */
    @TableField("run_id")
    private String runId;

    /** 任务配置编码 */
    @TableField("task_config_code")
    private String taskConfigCode;

    /** 任务编码 */
    @TableField("task_code")
    private String taskCode;

    /** 执行时任务模板版本号 */
    @TableField("task_template_version")
    private int taskTemplateVersion;

    /** 执行时数据主题编码 */
    @TableField("data_topic_code")
    private String dataTopicCode;

    /** 执行时数据类型编码 */
    @TableField("data_type_code")
    private String dataTypeCode;
    
    /** 执行时数据类型版本号 */
    @TableField("data_type_version")
    private int dataTypeVersion;

    @TableField("handler_name")
    private String handlerName;

    @TableField("snapshot_content")
    private String snapshotContent;

    /** 创建时间（自动填充） */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 创建人（自动填充） */
    @TableField(value = "created_by", fill = FieldFill.INSERT)
    private String createdBy;

}
