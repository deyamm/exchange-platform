package com.exchange.platform.data.client.dto;

import java.util.Map;

/**
 * 执行任务请求体（应用服务层 → 任务处理层）
 *
 * @param runId                  执行记录编号
 * @param taskCode               任务编码
 * @param taskTemplateVersionNo  任务模板版本号
 * @param handlerName            处理入口名称
 * @param executionContext       执行上下文
 */
public record TaskRunExecuteRequest(
    String runId,
    String taskCode,
    Integer taskTemplateVersionNo,
    String handlerName,
    Map<String, Object> executionContext
) {}
