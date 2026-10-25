package com.exchange.platform.collection.service;

import com.exchange.platform.collection.api.enums.ScheduleStatus;
import com.exchange.platform.collection.api.response.execution.ExecutionSchedulerView;
import com.exchange.platform.common.core.page.PageResult;

/**
 * 执行调度服务，负责调度任务查询与状态管理
 */
public interface ExecutionSchedulerService {

    /**
     * 查询调度任务列表
     *
     * @param taskCode 任务编码
     * @param status 调度状态
     * @param pageNo 页码
     * @param pageSize 每页条数
     * @return 调度任务分页结果
     */
    PageResult<ExecutionSchedulerView> querySchedulers(String taskCode, ScheduleStatus status, Integer pageNo, Integer pageSize);

    /**
     * 暂停调度任务
     *
     * @param schedulerId 调度任务 ID
     * @return 是否暂停成功
     */
    Boolean pauseScheduler(String schedulerId);

    /**
     * 恢复调度任务
     *
     * @param schedulerId 调度任务 ID
     * @return 是否恢复成功
     */
    Boolean resumeScheduler(String schedulerId);

    /**
     * 删除调度任务
     *
     * @param schedulerId 调度任务 ID
     * @return 是否删除成功
     */
    Boolean deleteScheduler(String schedulerId);
}