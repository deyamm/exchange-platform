package com.exchange.platform.collection.api.request.execution;

import com.exchange.platform.collection.api.enums.ScheduleStatus;

/**
 * 调度任务查询参数
 *
 * @param taskCode 任务编码
 * @param status   调度状态
 * @param pageNo   页码
 * @param pageSize 每页条数
 */
public record ExecutionSchedulerRequest(
    String taskCode,
    ScheduleStatus status,
    Integer pageNo,
    Integer pageSize
) {}
