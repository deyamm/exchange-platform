package com.exchange.platform.collection.entity.execution;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.exchange.platform.collection.api.enums.ScheduleStatus;
import com.exchange.platform.collection.api.enums.TriggerType;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@TableName("execution_scheduler")
public class ExecutionScheduler {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 调度编号 */
    @TableField("scheduler_id")
    private String schedulerId;

    /** 任务配置编码 */
    @TableField("task_config_code")
    private String taskConfigCode;

    /** 任务编码 */
    @TableField("task_code")
    private String taskCode;

    /** 触发类型 */
    @TableField("trigger_type")
    private TriggerType triggerType;

    /** Cron 表达式 */
    @TableField("cron_expression")
    private String cronExpression;

    /** 时区 */
    @TableField("time_zone")
    private String timeZone;

    /** 业务表数据源编号 */
    @TableField("biz_data_source_code")
    private String bizDataSourceCode;

    /** 业务表模式 */
    @TableField("biz_table_schema")
    private String bizTableSchema;

    /** 业务表名称 */
    @TableField("biz_table_name")
    private String bizTableName;

    /** 业务表调度依据字段 */
    @TableField("biz_schedule_column")
    private String bizScheduleColumn;

    /** 调度状态 */
    @TableField("status")
    private ScheduleStatus status;

    /** 参数模板 */
    @TableField("params_template")
    private String paramsTemplate;

    /** 下次执行时间 */
    @TableField("next_fire_time")
    private LocalDateTime nextFireTime;

    /** 最近执行时间 */
    @TableField("last_fire_time")
    private LocalDateTime lastFireTime;

    /** 最近执行的 runId */
    @TableField("last_run_id")
    private String lastRunId;

    /** 幂等请求号 */
    @TableField("request_id")
    private String requestId;

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
