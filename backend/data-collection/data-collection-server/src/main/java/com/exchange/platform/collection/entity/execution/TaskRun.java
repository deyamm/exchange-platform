package com.exchange.platform.collection.entity.execution;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.exchange.platform.collection.api.enums.RunStatus;
import com.exchange.platform.collection.api.enums.TriggerType;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@TableName("task_run")
public class TaskRun {
    
    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 执行记录编号 */
    @TableField("run_id")
    private String runId;

    /** 任务配置编码 */
    @TableField("task_config_code")
    private String taskConfigCode;
    
    /** 任务编码 */
    @TableField("task_code")
    private String taskCode;

    /** 触发方式 */
    @TableField("trigger_type")
    private TriggerType triggerType;

    /** 调度配置编号 */
    @TableField("scheduler_id")
    private String schedulerId;

    /** 请求号 */
    @TableField("request_id")
    private String requestId;

    /** 执行状态 */
    @TableField("run_status")
    private RunStatus runStatus;

    /** 执行开始时间 */
    @TableField("start_time")
    private LocalDateTime startTime;

    /** 调度触发时间 */
    @TableField("scheduled_fire_time")
    private LocalDateTime scheduledFireTime;
    
    /** 执行结束时间 */
    @TableField("end_time")
    private LocalDateTime endTime;

    /** 执行信息，用于记录执行结果信息、错误信息等 */
    @TableField("run_info")
    private String runInfo;

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
