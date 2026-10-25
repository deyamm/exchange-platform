package com.exchange.platform.collection.service.impl;

import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exchange.platform.collection.api.enums.ScheduleStatus;
import com.exchange.platform.collection.api.response.execution.ExecutionSchedulerView;
import com.exchange.platform.collection.converter.ExecutionConverter;
import com.exchange.platform.collection.entity.execution.ExecutionScheduler;
import com.exchange.platform.collection.mapper.execution.ExecutionSchedulerMapper;
import com.exchange.platform.collection.service.ExecutionSchedulerService;
import com.exchange.platform.common.core.exception.BusinessException;
import com.exchange.platform.common.core.page.PageResult;

/**
 * 执行调度服务实现，负责调度任务查询与状态管理
 */
@Service
public class ExecutionSchedulerServiceImpl implements ExecutionSchedulerService {

    private final ExecutionSchedulerMapper schedulerMapper;

    public ExecutionSchedulerServiceImpl(ExecutionSchedulerMapper schedulerMapper) {
        this.schedulerMapper = schedulerMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ExecutionSchedulerView> querySchedulers(String taskCode, ScheduleStatus status, Integer pageNo, Integer pageSize) {
        int safePageNo = (pageNo == null || pageNo < 1) ? 1 : pageNo;
        int safePageSize = (pageSize == null || pageSize < 1) ? 20 : Math.min(200, pageSize);

        LambdaQueryWrapper<ExecutionScheduler> wrapper = new LambdaQueryWrapper<ExecutionScheduler>()
            .eq(StringUtils.hasText(taskCode), ExecutionScheduler::getTaskCode, taskCode)
            .orderByDesc(ExecutionScheduler::getCreatedAt);
        if (status != null) {
            wrapper.eq(ExecutionScheduler::getStatus, status);
        }

        Page<ExecutionScheduler> page = schedulerMapper.selectPage(new Page<>(safePageNo, safePageSize), wrapper);
        return new PageResult<>(
            safePageNo,
            safePageSize,
            page.getTotal(),
            page.getRecords().stream().map(ExecutionConverter::toSchedulerView).collect(Collectors.toList())
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean pauseScheduler(String schedulerId) {
        return updateSchedulerStatus(schedulerId, ScheduleStatus.PAUSED);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean resumeScheduler(String schedulerId) {
        return updateSchedulerStatus(schedulerId, ScheduleStatus.ACTIVE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteScheduler(String schedulerId) {
        int deleted = schedulerMapper.delete(new LambdaQueryWrapper<ExecutionScheduler>()
            .eq(ExecutionScheduler::getSchedulerId, schedulerId)
        );
        if (deleted == 0) {
            throw new BusinessException(40003, "调度任务不存在：" + schedulerId);
        }
        return true;
    }

    private Boolean updateSchedulerStatus(String schedulerId, ScheduleStatus status) {
        int updated = schedulerMapper.update(null, new LambdaUpdateWrapper<ExecutionScheduler>()
            .set(ExecutionScheduler::getStatus, status)
            .eq(ExecutionScheduler::getSchedulerId, schedulerId)
        );
        if (updated == 0) {
            throw new BusinessException(400003, "调度任务不存在：" + schedulerId);
        }
        return true;
    }
}