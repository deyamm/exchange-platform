package com.exchange.platform.collection.api.request.tasks;

import javax.validation.constraints.NotBlank;

/**
 * 同步任务模板请求
 *
 * @param taskCode 任务编码；为空时执行全量同步
 * @param syncMode 同步模式：MANUAL（手动）
 */
public record TaskSyncRequest(
    String taskCode,
    @NotBlank String syncMode
) {}
