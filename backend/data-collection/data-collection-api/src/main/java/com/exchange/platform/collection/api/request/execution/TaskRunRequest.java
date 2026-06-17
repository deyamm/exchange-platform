package com.exchange.platform.collection.api.request.execution;

/**
 * 执行/结果/监控查询通用参数
 *
 * @param runId             执行记录编号
 * @param taskCode          任务编码
 * @param runStatus         执行状态
 * @param triggerType       触发方式
 * @param startTime         开始时间（>=）
 * @param endTime           结束时间（<=）
 * @param pageNo            页码
 * @param pageSize          每页条数
 */
public record TaskRunRequest(
    String runId,
    String taskCode,
    String runStatus,
    String triggerType,
    String startTime,
    String endTime,
    Integer pageNo,
    Integer pageSize
) {}
