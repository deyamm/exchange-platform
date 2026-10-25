package com.exchange.platform.collection.api.response.execution;

import java.time.LocalDateTime;

import com.exchange.platform.collection.api.enums.ScheduleStatus;
import com.exchange.platform.collection.api.enums.TriggerType;

import lombok.Data;

@Data
public class ExecutionSchedulerView {

    /** 调度编号 */
    private String schedulerId;

    /** 任务配置编码 */
    private String taskConfigCode;

    /** 任务编码 */
    private String taskCode;

    /** Cron 表达式 */
    private String cronExpression;

    /** 触发类型 */
    private TriggerType triggerType;

    /** 业务表数据源编号 */
    private String bizDataSourceCode;

    /** 业务表模式 */
    private String bizTableSchema;

    /** 业务表名称 */
    private String bizTableName;

    /** 业务表调度依据字段 */
    private String bizScheduleColumn;

    /** 时区 */
    private String timeZone;

    /** 调度状态 */
    private ScheduleStatus status;

    /** 下次执行时间 */
    private LocalDateTime nextFireTime;

    /** 最近执行时间 */
    private LocalDateTime lastFireTime;

    /** 最近执行的 runId */
    private String lastRunId;

    /** 幂等请求号 */
    private String requestId;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
