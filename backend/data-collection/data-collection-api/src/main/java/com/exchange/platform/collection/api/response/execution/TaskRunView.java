package com.exchange.platform.collection.api.response.execution;

import java.time.LocalDateTime;

import com.exchange.platform.collection.api.enums.RunStatus;
import com.exchange.platform.collection.api.enums.TriggerType;

import lombok.Data;

/**
 * 执行记录视图（列表展示）
 */
@Data
public class TaskRunView {

    /** 执行记录编号 */
    private String runId;

    /** 任务配置编码 */
    private String taskConfigCode;

    /** 任务编码 */
    private String taskCode;

    /** 触发方式 */
    private TriggerType triggerType;

    /** 调度配置编号 */
    private String schedulerId;

    /** 执行状态 */
    private RunStatus runStatus;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime endTime;

    /** 调度触发时间 */
    private LocalDateTime scheduledFireTime;

    /** 失败原因 */
    private String runInfo;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
