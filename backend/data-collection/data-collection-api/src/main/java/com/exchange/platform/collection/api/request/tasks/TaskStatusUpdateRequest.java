package com.exchange.platform.collection.api.request.tasks;

import javax.validation.constraints.NotNull;

import com.exchange.platform.collection.api.enums.CommonStatus;


/**
 * 任务启停状态更新请求
 *
 * @param status 启停状态
 */
public record TaskStatusUpdateRequest(
    @NotNull CommonStatus status
) {}
