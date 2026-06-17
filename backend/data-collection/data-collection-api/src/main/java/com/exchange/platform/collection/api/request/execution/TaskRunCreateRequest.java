package com.exchange.platform.collection.api.request.execution;

import java.util.Map;

import javax.validation.constraints.NotBlank;

import com.exchange.platform.collection.api.enums.TriggerType;


/**
 * 创建执行记录请求（I-24）
 *
 * @param taskConfigCode 任务配置编码（读取当前已确认配置）
 * @param taskCode       任务编码
 * @param triggerType    触发方式（MANUAL / SCHEDULED），默认 MANUAL
 * @param requestId      幂等请求号，用于创建防重
 * @param params         执行参数（如 tradeDate）
 * @param cronExpression 定时表达式，仅 triggerType 为 SCHEDULED 时有效
 */
public record TaskRunCreateRequest(
    @NotBlank String taskConfigCode,
    @NotBlank String taskCode,
    TriggerType triggerType,
    @NotBlank String requestId,
    Map<String, Object> params,
    String cronExpression
) {}
