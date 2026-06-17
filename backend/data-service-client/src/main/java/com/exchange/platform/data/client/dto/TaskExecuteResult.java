package com.exchange.platform.data.client.dto;

import java.util.List;
import java.util.Map;

/**
 * 执行结果返回体（I-36，任务处理层 → 应用服务层）
 * <p>任务处理层以 camelCase 返回结果摘要和定位信息，由应用服务层归集入库。</p>
 *
 * @param runId          执行记录编号
 * @param status         执行状态（SUCCESS / FAILED / PARTIAL_SUCCESS）
 * @param totalCount     总处理条数
 * @param successCount   成功条数
 * @param failureCount   失败条数
 * @param resultLocation 结果定位信息
 * @param summaryContent 结果摘要内容
 */
public record TaskExecuteResult(
    String runId,
    String status,
    Long totalCount,
    Long successCount,
    Long failureCount,
    Map<String, Object> resultLocation,
    Map<String, Object> summaryContent,
    List<Map<String, Object>> logs,
    List<Map<String, Object>> problems,
    List<Map<String, Object>> exceptions
) {}
