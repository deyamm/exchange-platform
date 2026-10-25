package com.exchange.platform.collection.service.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exchange.platform.collection.mapper.datasource.DataSourceConfigMapper;
import com.exchange.platform.collection.mapper.execution.TaskRunMapper;
import com.exchange.platform.collection.mapper.monitor.ExceptionRecordMapper;
import com.exchange.platform.collection.mapper.monitor.ProblemRecordMapper;
import com.exchange.platform.collection.mapper.monitor.RunLogMapper;
import com.exchange.platform.collection.mapper.tasks.StorageColumnMappingMapper;
import com.exchange.platform.collection.mapper.tasks.StorageMappingMapper;
import com.exchange.platform.collection.mapper.tasks.TaskConfigMapper;
import com.exchange.platform.collection.mapper.tasks.TaskTemplateVersionMapper;
import com.exchange.platform.collection.service.ExecutionService;
import com.exchange.platform.common.core.exception.BusinessException;
import com.exchange.platform.common.core.page.PageResult;
import com.exchange.platform.common.core.util.IdGenerator;
import com.exchange.platform.data.client.DataServiceClient;
import com.exchange.platform.data.client.dto.TaskExecuteResult;
import com.exchange.platform.data.client.dto.TaskRunExecuteRequest;
import com.exchange.platform.collection.api.enums.RunStatus;
import com.exchange.platform.collection.api.enums.ScheduleStatus;
import com.exchange.platform.collection.api.enums.TriggerType;
import com.exchange.platform.collection.api.request.execution.TaskRunCreateRequest;
import com.exchange.platform.collection.api.request.execution.TaskRunRequest;
import com.exchange.platform.collection.api.response.execution.TaskRunDetailView;
import com.exchange.platform.collection.api.response.execution.TaskRunView;
import com.exchange.platform.collection.converter.ExecutionConverter;
import com.exchange.platform.collection.entity.datasource.DataSourceConfig;
import com.exchange.platform.collection.entity.execution.ExecutionConfigSnapshot;
import com.exchange.platform.collection.entity.execution.ExecutionScheduler;
import com.exchange.platform.collection.entity.execution.ResultSummary;
import com.exchange.platform.collection.entity.execution.TaskRun;
import com.exchange.platform.collection.entity.monitor.ExceptionRecord;
import com.exchange.platform.collection.entity.monitor.ProblemRecord;
import com.exchange.platform.collection.entity.monitor.RunLog;
import com.exchange.platform.collection.entity.tasks.StorageColumnMapping;
import com.exchange.platform.collection.entity.tasks.StorageMapping;
import com.exchange.platform.collection.entity.tasks.TaskConfig;
import com.exchange.platform.collection.entity.tasks.TaskTemplateVersion;
import com.exchange.platform.collection.mapper.execution.ExecutionConfigSnapshotMapper;
import com.exchange.platform.collection.mapper.execution.ExecutionSchedulerMapper;
import com.exchange.platform.collection.mapper.execution.ResultSummaryMapper;
import com.exchange.platform.collection.utils.JdbcUtils;


@Service
public class ExecutionServiceImpl extends ServiceImpl<TaskRunMapper, TaskRun> implements ExecutionService {
    
    private static final Logger log = LoggerFactory.getLogger(ExecutionServiceImpl.class);

    private final TaskConfigMapper taskConfigMapper;
    private final ExecutionConfigSnapshotMapper snapshotMapper;
    private final StorageMappingMapper storageMappingMapper;
    private final StorageColumnMappingMapper storageColumnMapper;
    private final TaskTemplateVersionMapper taskTemplateVersionMapper;
    private final ResultSummaryMapper resultSummaryMapper;  
    private final DataServiceClient dataServiceClient;
    private final ExecutionSchedulerMapper schedulerMapper;
    private final DataSourceConfigMapper dataSourceConfigMapper;
    private final RunLogMapper runLogMapper;
    private final ProblemRecordMapper problemRecordMapper;
    private final ExceptionRecordMapper exceptionRecordMapper;

    public ExecutionServiceImpl(TaskConfigMapper taskConfigMapper, ExecutionConfigSnapshotMapper snapshotMapper, StorageMappingMapper storageMappingMapper, StorageColumnMappingMapper storageColumnMapper, TaskTemplateVersionMapper taskTemplateVersionMapper, ResultSummaryMapper resultSummaryMapper, DataServiceClient dataServiceClient, ExecutionSchedulerMapper schedulerMapper, DataSourceConfigMapper dataSourceConfigMapper, RunLogMapper runLogMapper, ProblemRecordMapper problemRecordMapper, ExceptionRecordMapper exceptionRecordMapper) {
        this.taskConfigMapper = taskConfigMapper;
        this.snapshotMapper = snapshotMapper;
        this.storageMappingMapper = storageMappingMapper;
        this.storageColumnMapper = storageColumnMapper;
        this.taskTemplateVersionMapper = taskTemplateVersionMapper;
        this.resultSummaryMapper = resultSummaryMapper;
        this.dataServiceClient = dataServiceClient;
        this.schedulerMapper = schedulerMapper;
        this.dataSourceConfigMapper = dataSourceConfigMapper;
        this.runLogMapper = runLogMapper;
        this.problemRecordMapper = problemRecordMapper;
        this.exceptionRecordMapper = exceptionRecordMapper;
    }


    /**
     * 创建执行记录并生成执行配置快照，分为两种场景：手动触发和定时调度触发
      - 手动触发：直接创建执行记录，状态为 CREATED，由用户调用执行接口触发执行
      - 定时调度触发：创建调度配置，在调度器中定时触发执行记录创建和执行，由调度器调用执行接口触发执行
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskRunView createTaskRun(TaskRunCreateRequest request) {
        log.info("Creating TaskRun for taskConfigCode={}, taskCode={}, triggerType={}, requestId={}",
            request.taskConfigCode(), request.taskCode(), request.triggerType(), request.requestId());

        TriggerType triggerType = request.triggerType() != null ? request.triggerType() : TriggerType.MANUAL;

        // 如果是定时调度或业务表调度类型的请求，则创建调度配置并返回调度信息，不创建执行记录
        if (triggerType == TriggerType.SCHEDULED || triggerType == TriggerType.BIZ_TABLE) {
            return createScheduledTask(request, triggerType);
        }

        // 如果requestId已存在，说明是重复请求，直接返回已有记录（幂等处理）
        TaskRun existingRun = this.getOne(
            new LambdaQueryWrapper<TaskRun>()
            .eq(TaskRun::getRequestId, request.requestId())
            .last("LIMIT 1")
        );
        if (existingRun != null) {
            log.warn("Duplicate TaskRun creation request detected for requestId={}, existing runId={}",
                request.requestId(), existingRun.getRunId());
            return ExecutionConverter.toRunView(existingRun);
        }

        // 获取当前已确认的任务配置
        TaskConfig taskConfig = requireTaskConfig(request.taskConfigCode());

        TaskRun taskRun = createTaskRunWithSnapshot(
            taskConfig,
            request.taskCode(),
            triggerType,
            request.requestId(),
            request.params(),
            request.cronExpression(),
            null,
            null
        );

        log.info("[ExecutionServiceImpl] Created execution config snapshot for runId: {}", taskRun.getRunId());
        return ExecutionConverter.toRunView(taskRun);
    }

    /**
     * 创建定时调度配置并生成对应的执行记录（状态为 CREATED），由调度器定时触发执行
     */
    @Transactional(rollbackFor = Exception.class)
    public TaskRun createScheduledRun(ExecutionScheduler scheduler, Map<String, Object> params, LocalDateTime fireTime) {
        TaskConfig taskConfig = requireTaskConfig(scheduler.getTaskConfigCode());
        TaskRun taskRun = createTaskRunWithSnapshot(
            taskConfig,
            scheduler.getTaskCode(),
            scheduler.getTriggerType() != null ? scheduler.getTriggerType() : TriggerType.SCHEDULED,
            null,
            params,
            scheduler.getCronExpression(),
            scheduler.getSchedulerId(),
            fireTime
        );
        return taskRun;
    }

    /**
     * 异步执行任务运行，该方法会立刻收到任务处理层返回的状态，并通过回调接口（processTaskRunCallback）接收最终的执行结果
     * 该方法不处理执行结果摘要、日志等信息，相关信息会在 processTaskRunCallback 中处理
     * @param runId
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskRunView executeTaskRun(String runId) {
        log.info("[Service] executeTaskRun runId={}", runId);

        TaskRun taskRun = requireTaskRun(runId);
        // 只有 CREATED/PENDING 状态的执行记录才能执行
        if (taskRun.getRunStatus() != RunStatus.CREATED && taskRun.getRunStatus() != RunStatus.FAILED) {
            throw new BusinessException(40005, "只有 CREATED/FAILED 状态的执行记录才能执行，当前状态：" + taskRun.getRunStatus());
        }

        // 获取执行配置快照
        ExecutionConfigSnapshot snapshot = snapshotMapper.selectOne(
            new LambdaQueryWrapper<ExecutionConfigSnapshot>()
            .eq(ExecutionConfigSnapshot::getRunId, runId)
            .last("LIMIT 1")
        );
        if (snapshot == null) {
            throw new BusinessException(40003, "执行配置快照不存在，无法执行任务运行：" + runId);
        }

        // 组装上下文信息并调用 Python 处理层
        TaskRunExecuteRequest executeRequest = buildExecuteRequest(taskRun, snapshot);
        TaskExecuteResult executeResult;
        try {
            executeResult = dataServiceClient.executeTaskRun(executeRequest);
            // 设置开始时间，更改为任务执行层立刻返回的状态（RUNNING）
            taskRun.setRunStatus(RunStatus.valueOf(executeResult.status()));
            taskRun.setStartTime(java.time.LocalDateTime.now());
            this.updateById(taskRun);
        } catch (Exception e) {
            log.error("Failed to execute task run via DataServiceClient for runId={}, error={}", runId, e.getMessage(), e);
            // 调用失败则将执行记录状态改为 FAILED，并保存异常信息
            taskRun.setRunStatus(RunStatus.FAILED);
            taskRun.setEndTime(LocalDateTime.now());
            taskRun.setRunInfo("调用任务处理层执行失败：" + e.getMessage());
            this.updateById(taskRun);
            return ExecutionConverter.toRunView(taskRun);
        }

        log.info("[Service] executeTaskRun running for runId={}", runId);
        return ExecutionConverter.toRunView(taskRun);
    }

    /**
     * 删除执行记录
     * @param runId
     * @return
     */
    @Override
    public void deleteTaskRun(String runId) {
        log.info("[Service] deleteTaskRun runId={}", runId);
        // 删除执行记录
        this.remove(new LambdaQueryWrapper<TaskRun>().eq(TaskRun::getRunId, runId));
        // 删除执行配置快照
        snapshotMapper.delete(new LambdaQueryWrapper<ExecutionConfigSnapshot>().eq(ExecutionConfigSnapshot::getRunId, runId));
    }

    /**
     * 查询执行记录列表（分页）
     * @param params
     * @return
     */
    @Override
    @Transactional(readOnly = true)
    public PageResult<TaskRunView> queryTaskRuns(TaskRunRequest params) {
        log.info("[Service] queryTaskRuns params={}", params);

        int pageNo = (params.pageNo() == null || params.pageNo() < 1) ? 1 : params.pageNo();
        int pageSize = (params.pageSize() == null || params.pageSize() < 1) ? 20 : Math.min(200, params.pageSize());

        // 根据taskCode、runStatus、startTime、endTime来筛选，其他条件根据需要后期添加
        LambdaQueryWrapper<TaskRun> wrapper = new LambdaQueryWrapper<TaskRun>()
            .eq(StringUtils.hasText(params.taskCode()), TaskRun::getTaskCode, params.taskCode())
            .ge(StringUtils.hasText(params.startTime()), TaskRun::getStartTime, params.startTime())
            .le(StringUtils.hasText(params.endTime()), TaskRun::getEndTime, params.endTime())
            .orderByDesc(TaskRun::getCreatedAt);
        if (params.runStatus() != null) {
            wrapper.eq(TaskRun::getRunStatus, params.runStatus());
        }

        // 分页结果
        Page<TaskRun> page = this.page(new Page<>(pageNo, pageSize), wrapper);
        List<TaskRunView> runViews = page.getRecords().stream()
            .map(ExecutionConverter::toRunView)
            .collect(Collectors.toList());
        return new PageResult<>(pageNo, pageSize, page.getTotal(), runViews);
    }

    /**
     * 查询执行记录详情
     * @param runId
     * @return
     */
    @Override
    @Transactional(readOnly = true)
    public TaskRunDetailView getTaskRunDetail(String runId) {
        log.info("[Service] getTaskRunDetail runId={}", runId);

        TaskRunDetailView detailView = new TaskRunDetailView();

        // 获取执行记录基础信息
        TaskRun taskRun = requireTaskRun(runId);
        detailView.setTaskRun(ExecutionConverter.toRunView(taskRun));  

        // 获取执行配置快照
        ExecutionConfigSnapshot snapshot = snapshotMapper.selectOne(
            new LambdaQueryWrapper<ExecutionConfigSnapshot>()
            .eq(ExecutionConfigSnapshot::getRunId, runId)
            .last("LIMIT 1")
        );
        detailView.setSnapshot(ExecutionConverter.toSnapshotView(snapshot));

        // 获取结果摘要信息
        ResultSummary resultSummary = resultSummaryMapper.selectOne(
            new LambdaQueryWrapper<ResultSummary>()
            .eq(ResultSummary::getRunId, runId)
            .last("LIMIT 1")
        );
        detailView.setResultSummary(resultSummary != null ? ExecutionConverter.toSummaryView(resultSummary) : null);
        return detailView;
    }

    /**
     * 处理任务处理层的异步回调，保存结果摘要、运行日志、问题样本与异常记录，并更新 TaskRun 状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processTaskRunCallback(TaskExecuteResult callbackRequest) {
        log.info("[Service] processTaskRunCallback runId={}, status={}", callbackRequest.runId(), callbackRequest.status());

        TaskRun taskRun = requireTaskRun(callbackRequest.runId());

        // 保存结果摘要，当前结果摘要为python中的runtime
        ResultSummary resultSummary = new ResultSummary()
            .setSummaryId(IdGenerator.generate("SUM"))
            .setRunId(callbackRequest.runId())
            .setTotalCount(callbackRequest.totalCount())
            .setSuccessCount(callbackRequest.successCount())
            .setFailureCount(callbackRequest.failureCount())
            .setResultLocation(callbackRequest.resultLocation() != null ? JSON.toJSONString(callbackRequest.resultLocation()) : null)
            .setSummaryContent(callbackRequest.summaryContent() != null ? JSON.toJSONString(callbackRequest.summaryContent()) : null);
        resultSummaryMapper.insert(resultSummary);

        // 保存log，返回结果的多条Log保存为一个record，日志内容以JSON格式保存，方便后续查询和分析
        // 日志级别根据执行状态设置，如果状态为FAILED则设置为ERROR，否则设置为INFO
        // traiceId暂不支持，先设置为null，后续如果需要支持分布式追踪可以改为接收并保存traceId TODO
        // TODO 日志内容如果过大，可能需要考虑单条日志记录过大导致的数据库性能问题，可以考虑后续增加日志内容长度限制或者将日志内容存储到专门的日志系统中（如Elasticsearch）并在数据库中保存索引和链接
        RunLog runLog = new RunLog()
            .setLogId(IdGenerator.generate("LOG"))
            .setRunId(callbackRequest.runId())
            .setLogLevel(!"FAILED".equalsIgnoreCase(callbackRequest.status()) ? "INFO" : "ERROR")
            // .setLogContent(JSON.toJSONString(callbackRequest.logs()))
            .setTraceId(null);
        runLogMapper.insert(runLog);

        // 如果存在问题，保存问题记录，处理方法暂时同RunLog一致
        if (callbackRequest.problems() != null && !callbackRequest.problems().isEmpty()) {
            ProblemRecord pr = new ProblemRecord()
                .setProblemId(IdGenerator.generate("PRB"))
                .setRunId(callbackRequest.runId())
                .setProblemType(null)
                .setProblemMessage(JSON.toJSONString(callbackRequest.problems()))
                .setSampleDataJson(null);
            problemRecordMapper.insert(pr);
        }
                
        // 如果存在异常，保存异常记录
        if (callbackRequest.exceptions() != null && !callbackRequest.exceptions().isEmpty()) {
            ExceptionRecord er = new ExceptionRecord()
                .setExceptionId(IdGenerator.generate("ECP"))
                .setRunId(callbackRequest.runId())
                .setExceptionType(null)
                .setExceptionMessage(JSON.toJSONString(callbackRequest.exceptions()))
                .setExceptionContext(null);
            exceptionRecordMapper.insert(er);
        }
                
        // update task run status and end time
        RunStatus finalStatus = "SUCCESS".equalsIgnoreCase(callbackRequest.status()) ? RunStatus.SUCCESS : RunStatus.FAILED;
        taskRun.setRunStatus(finalStatus);
        taskRun.setEndTime(LocalDateTime.now());
        taskRun.setRunInfo(callbackRequest.summaryContent() != null ? JSON.toJSONString(callbackRequest.summaryContent()) : "");
        this.updateById(taskRun);
    }



    // ==================== 私有工具方法 ====================
    /** 根据任务配置代码获取任务配置，如果不存在则抛出异常 */
    private TaskConfig requireTaskConfig(String taskConfigCode) {
        TaskConfig taskConfig = taskConfigMapper.selectOne(
            new LambdaQueryWrapper<TaskConfig>()
            .eq(TaskConfig::getTaskConfigCode, taskConfigCode)
            .last("LIMIT 1")
        );
        if (taskConfig == null) {
            throw new BusinessException(40003, "任务配置不存在：" + taskConfigCode);
        }
        return taskConfig;
    }

    /**
     * 根据 runId 获取执行记录，如果不存在则抛出异常
     * @param runId
     * @return
     */
    private TaskRun requireTaskRun(String runId) {
        TaskRun run = this.getOne(new LambdaQueryWrapper<TaskRun>()
            .eq(TaskRun::getRunId, runId).last("LIMIT 1"));
        if (run == null) {
            throw new BusinessException(40003, "执行记录不存在：" + runId);
        }
        return run;
    }

    /**
     * 前端发送创建定时调度的请求时，创建调度配置并返回调度信息，不创建执行记录；由调度器定时触发执行记录创建和执行
     * 包括：创建判断是否已存在（requestId幂等处理）、调度配置、计算下次触发时间、
     * @param request
     * @param triggerType
     * @return
     */
    private TaskRunView createScheduledTask(TaskRunCreateRequest request, TriggerType triggerType) {
        validateScheduledTaskRequest(request, triggerType);

        // 幂等处理：如果 requestId 已存在，则直接返回已有的调度信息，避免重复创建调度配置和执行记录
        ExecutionScheduler existingScheduler = schedulerMapper.selectOne(
            new LambdaQueryWrapper<ExecutionScheduler>()
                .eq(ExecutionScheduler::getRequestId, request.requestId())
                .last("LIMIT 1")
        );
        if (existingScheduler != null) {
            log.warn("Duplicate scheduler creation request detected for requestId={}, schedulerId={}",
                request.requestId(), existingScheduler.getSchedulerId());
            return ExecutionConverter.toScheduledTaskRunView(existingScheduler, triggerType);
        }

        // 获取当前已确认的任务配置，计算下次触发时间，创建调度配置
        TaskConfig taskConfig = requireTaskConfig(request.taskConfigCode());
        ZoneId zoneId = ZoneId.of(JdbcUtils.DEFAULT_TIME_ZONE);
        LocalDateTime now = LocalDateTime.now(zoneId);
        LocalDateTime nextFireTime;
        if (triggerType == TriggerType.SCHEDULED) {
            try {
                nextFireTime = computeNextFireTime(request.cronExpression(), zoneId, now);
            } catch (IllegalArgumentException ex) {
                throw new BusinessException(40007, "cronExpression 格式错误：" + request.cronExpression());
            }
        } else {
            nextFireTime = computeInitialBizTableFireTime(request, now);
        }

        String schedulerId = IdGenerator.generate("SCH");
        ExecutionScheduler scheduler = new ExecutionScheduler()
            .setSchedulerId(schedulerId)
            .setTaskConfigCode(taskConfig.getTaskConfigCode())
            .setTaskCode(taskConfig.getTaskCode())
            .setTriggerType(triggerType)
            .setCronExpression(triggerType == TriggerType.SCHEDULED ? request.cronExpression() : null)
            .setTimeZone(JdbcUtils.DEFAULT_TIME_ZONE)
            .setBizDataSourceCode(triggerType == TriggerType.BIZ_TABLE ? request.bizDataSourceCode() : null)
            .setBizTableSchema(triggerType == TriggerType.BIZ_TABLE ? request.bizTableSchema() : null)
            .setBizTableName(triggerType == TriggerType.BIZ_TABLE ? request.bizTableName() : null)
            .setBizScheduleColumn(triggerType == TriggerType.BIZ_TABLE ? request.bizScheduleColumn() : null)
            .setStatus(ScheduleStatus.ACTIVE)
            .setParamsTemplate(request.params() != null ? JSON.toJSONString(request.params()) : null)
            .setNextFireTime(nextFireTime)
            .setRequestId(request.requestId());
        schedulerMapper.insert(scheduler);

        return ExecutionConverter.toScheduledTaskRunView(scheduler, triggerType);
    }

    /**
     * 校验定时调度和业务表调度各自必需的参数
     */
    private void validateScheduledTaskRequest(TaskRunCreateRequest request, TriggerType triggerType) {
        if (triggerType == TriggerType.SCHEDULED) {
            if (!StringUtils.hasText(request.cronExpression())) {
                throw new BusinessException(40006, "定时任务缺少 cronExpression，无法创建调度配置");
            }
            return;
        }

        if (triggerType != TriggerType.BIZ_TABLE) {
            return;
        }
        if (!StringUtils.hasText(request.bizDataSourceCode())) {
            throw new BusinessException(40006, "业务表调度缺少数据源编码，无法创建调度配置");
        }
        if (!StringUtils.hasText(request.bizTableSchema())) {
            throw new BusinessException(40006, "业务表调度缺少数据库，无法创建调度配置");
        }
        if (!StringUtils.hasText(request.bizTableName())) {
            throw new BusinessException(40006, "业务表调度缺少表名，无法创建调度配置");
        }
        if (!StringUtils.hasText(request.bizScheduleColumn())) {
            throw new BusinessException(40006, "业务表调度缺少依据列，无法创建调度配置");
        }
    }

    /**
     * 创建执行记录并生成执行配置快照的核心方法，包含手动触发和定时调度触发两种场景
      - 手动触发：直接创建执行记录，状态为 CREATED，由用户调用执行接口触发执行
      - 定时调度触发：创建调度配置，在调度器中定时触发执行记录创建和执行，由调度器调用执行接口触发执行
     */
    private TaskRun createTaskRunWithSnapshot(
        TaskConfig taskConfig,
        String taskCode,
        TriggerType triggerType,
        String requestId,
        Map<String, Object> params,
        String cronExpression,
        String schedulerId,
        LocalDateTime scheduledFireTime
    ) {
        if (taskConfig.getStorageMappingCode() == null) {
            throw new BusinessException(40004, "任务配置缺少存储映射，无法创建执行记录：" + taskConfig.getTaskConfigCode());
        }

        String runId = IdGenerator.generate("RUN");
        TaskRun taskRun = new TaskRun()
            .setRunId(runId)
            .setTaskConfigCode(taskConfig.getTaskConfigCode())
            .setTaskCode(taskCode)
            .setTriggerType(triggerType)
            .setRequestId(requestId)
            .setSchedulerId(schedulerId)
            .setScheduledFireTime(scheduledFireTime)
            .setRunStatus(RunStatus.CREATED);
        this.save(taskRun);

        StorageMapping storageMapping = storageMappingMapper.selectOne(
            new LambdaQueryWrapper<StorageMapping>()
                .eq(StorageMapping::getStorageMappingCode, taskConfig.getStorageMappingCode())
                .last("LIMIT 1")
        );
        List<StorageColumnMapping> columns = storageColumnMapper.selectList(
            new LambdaQueryWrapper<StorageColumnMapping>()
                .eq(StorageColumnMapping::getStorageMappingCode, taskConfig.getStorageMappingCode())
        );
        TaskTemplateVersion taskTemplateVersion = taskTemplateVersionMapper.selectOne(
            new LambdaQueryWrapper<TaskTemplateVersion>()
                .eq(TaskTemplateVersion::getTaskCode, taskConfig.getTaskCode())
                .eq(TaskTemplateVersion::getVersionNo, taskConfig.getTaskTemplateVersionNo())
                .last("LIMIT 1")
        );

        Map<String, Object> snapshotContent = new HashMap<>();
        // snapshotContent.put("taskTemplate", JSON.toJSONString(taskTemplateVersion));
        snapshotContent.put("taskConfig", taskConfig);
        snapshotContent.put("params", params);
        snapshotContent.put("cronExpression", cronExpression);
        snapshotContent.put("storageMapping", storageMapping);
        snapshotContent.put("columns", columns);
        if (scheduledFireTime != null) {
            snapshotContent.put("scheduledFireTime", scheduledFireTime);
        }

        ExecutionConfigSnapshot snapshot = new ExecutionConfigSnapshot()
            .setSnapshotId(IdGenerator.generate("SNP"))
            .setRunId(runId)
            .setTaskConfigCode(taskConfig.getTaskConfigCode())
            .setTaskCode(taskConfig.getTaskCode())
            .setTaskTemplateVersion(taskConfig.getTaskTemplateVersionNo())
            .setDataTopicCode(taskConfig.getDataTopicCode())
            .setDataTypeCode(taskConfig.getDataTypeCode())
            .setDataTypeVersion(taskConfig.getDataTypeVersionNo())
            .setHandlerName(taskTemplateVersion.getHandlerName())
            .setSnapshotContent(JSON.toJSONString(snapshotContent));
        snapshotMapper.insert(snapshot);

        return taskRun;
    }

    /**
     * 构建执行请求，直接将snapshot中的内容还原到执行上下文中，后续如果执行上下文需要调整再修改这里的构建逻辑
     * @param taskRun
     * @param snapshot
     * @return
     */
    private TaskRunExecuteRequest buildExecuteRequest(TaskRun taskRun, ExecutionConfigSnapshot snapshot) {
        // 从快照内容中还原执行上下文

        @SuppressWarnings("unchecked")
        Map<String, Object> snapshotContent = JSON.parseObject(snapshot.getSnapshotContent(), Map.class);

        return new TaskRunExecuteRequest(
            taskRun.getRunId(),
            taskRun.getTaskCode(),
            snapshot.getTaskTemplateVersion(),
            snapshot.getHandlerName(),
            snapshotContent
        );
    }


    /**
     * 创建业务表调度时，取小于等于当前时间且距离当前时间最近的字段值作为首次触发时间。
     */
    private LocalDateTime computeInitialBizTableFireTime(TaskRunCreateRequest request, LocalDateTime now) {
        return queryBizTableFireTime(
            request.bizDataSourceCode(),
            request.bizTableSchema(),
            request.bizTableName(),
            request.bizScheduleColumn(),
            now
        );
    }

    /**
     * 调度任务执行后计算下一次业务表触发时间。当前触发时间的相同记录会在同一轮全部生成任务，所以下次触发时间只取小于当前触发时间且最近的时间。
     */
    public LocalDateTime computeNextBizTableFireTime(ExecutionScheduler scheduler, LocalDateTime fireTime) {
        validateBizTableScheduler(scheduler);
        if (fireTime == null) {
            return queryBizTableFireTime(
                scheduler.getBizDataSourceCode(),
                scheduler.getBizTableSchema(),
                scheduler.getBizTableName(),
                scheduler.getBizScheduleColumn(),
                LocalDateTime.now(JdbcUtils.resolveZoneId(scheduler.getTimeZone()))
            );
        }

        return queryBizTableFireTime(
            scheduler.getBizDataSourceCode(),
            scheduler.getBizTableSchema(),
            scheduler.getBizTableName(),
            scheduler.getBizScheduleColumn(),
            fireTime
        );
    }

    /**
     * 解析 {biz_table:{column_name}} 参数模板时，按本次触发时间读取业务表中对应字段的值。
     * 一次读取所有本次触发时间对应的记录，并返回对应的字段值列表
     */
    public List<Map<String, Object>> resolveBizTableColumnValue(ExecutionScheduler scheduler, List<String> columnNames, LocalDateTime fireTime) {
        validateBizTableScheduler(scheduler);
        columnNames.forEach(columnName -> validateJdbcIdentifier(columnName, "业务表字段名"));

        String tableName = JdbcUtils.qualifiedTableName(scheduler.getBizTableSchema(), scheduler.getBizTableName());
        String scheduleColumn = JdbcUtils.quoteIdentifier(scheduler.getBizScheduleColumn());
        // 生成的targetColumn包含了反引号
        List<String> targetColumns = columnNames.stream().map(JdbcUtils::quoteIdentifier).collect(Collectors.toList());
        String sql = "SELECT " + String.join(", ", targetColumns) + " FROM " + tableName
            + " WHERE " + scheduleColumn + " = ? ";

        try (Connection connection = openBizDataSourceConnection(scheduler.getBizDataSourceCode());
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.valueOf(fireTime));
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Map<String, Object>> values = new ArrayList<>();

                while (resultSet.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 0; i < targetColumns.size(); i++) {
                        // 这里需要去掉反引号，保证返回的字段名与参数模板中的字段名一致
                        row.put(targetColumns.get(i).substring(1, targetColumns.get(i).length() - 1), JdbcUtils.normalizeJdbcValue(resultSet.getObject(i + 1)));
                    }
                    values.add(row);
                }

                return values;
            }
        } catch (SQLException ex) {
            throw new BusinessException(40008, "读取业务表字段失败：" + ex.getMessage());
        }
    }

    /**
     * 查询业务表中满足条件的最近触发时间。
     * 即基于所给的参考时间，查询业务表中调度依据字段大于参考时间的最小值。
     */
    private LocalDateTime queryBizTableFireTime(String dataSourceCode, String schemaName, String tableName, String columnName, LocalDateTime referenceTime) {
        validateJdbcIdentifier(schemaName, "业务表 schema");
        validateJdbcIdentifier(tableName, "业务表名");
        validateJdbcIdentifier(columnName, "业务表调度依据字段");

        String qualifiedTable = JdbcUtils.qualifiedTableName(schemaName, tableName);
        String scheduleColumn = JdbcUtils.quoteIdentifier(columnName);
        String sql = "SELECT " + scheduleColumn + " FROM " + qualifiedTable
            + " WHERE " + scheduleColumn + " > ?"
            + " ORDER BY " + scheduleColumn + " ASC LIMIT 1";

        try (Connection connection = openBizDataSourceConnection(dataSourceCode);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.valueOf(referenceTime));
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return JdbcUtils.toLocalDateTime(resultSet.getObject(1));
            }
        } catch (SQLException ex) {
            throw new BusinessException(40008, "计算业务表下次触发时间失败：" + ex.getMessage());
        }
    }

    /**
     * 统计业务表中指定触发时间对应的记录数量。
     */
    public long countBizTableRowsByFireTime(ExecutionScheduler scheduler, LocalDateTime fireTime) {
        validateBizTableScheduler(scheduler);
        String tableName = JdbcUtils.qualifiedTableName(scheduler.getBizTableSchema(), scheduler.getBizTableName());
        String scheduleColumn = JdbcUtils.quoteIdentifier(scheduler.getBizScheduleColumn());
        String sql = "SELECT COUNT(1) FROM " + tableName + " WHERE " + scheduleColumn + " = ?";

        try (Connection connection = openBizDataSourceConnection(scheduler.getBizDataSourceCode());
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.valueOf(fireTime));
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong(1) : 0L;
            }
        } catch (SQLException ex) {
            throw new BusinessException(40008, "统计业务表触发时间记录数失败：" + ex.getMessage());
        }
    }

    private void validateBizTableScheduler(ExecutionScheduler scheduler) {
        if (scheduler == null) {
            throw new BusinessException(40008, "业务表调度配置不存在");
        }
        if (!StringUtils.hasText(scheduler.getBizDataSourceCode())) {
            throw new BusinessException(40008, "业务表调度缺少数据源编号");
        }
        if (!StringUtils.hasText(scheduler.getBizTableSchema())) {
            throw new BusinessException(40008, "业务表调度缺少 schema");
        }
        if (!StringUtils.hasText(scheduler.getBizTableName())) {
            throw new BusinessException(40008, "业务表调度缺少表名");
        }
        if (!StringUtils.hasText(scheduler.getBizScheduleColumn())) {
            throw new BusinessException(40008, "业务表调度缺少调度依据字段");
        }
        validateJdbcIdentifier(scheduler.getBizTableSchema(), "业务表 schema");
        validateJdbcIdentifier(scheduler.getBizTableName(), "业务表名");
        validateJdbcIdentifier(scheduler.getBizScheduleColumn(), "业务表调度依据字段");
    }

    private DataSourceConfig requireDataSource(String dataSourceCode) {
        DataSourceConfig dataSource = dataSourceConfigMapper.selectOne(
            new LambdaQueryWrapper<DataSourceConfig>()    
                .eq(DataSourceConfig::getDataSourceCode, dataSourceCode)
                .last("LIMIT 1")
        );
        if (dataSource == null) {
            throw new BusinessException(40008, "数据源不存在：" + dataSourceCode);
        }
        return dataSource;
    }

    private Connection openBizDataSourceConnection(String dataSourceCode) throws SQLException {
        DataSourceConfig dataSource = requireDataSource(dataSourceCode);
        if (!"MYSQL".equalsIgnoreCase(String.valueOf(dataSource.getDataSourceType()))) {
            throw new BusinessException(40008, "业务表调度暂只支持 MYSQL 数据源：" + dataSourceCode);
        }
        return JdbcUtils.openConnection(dataSource);
    }

    /**
     * 将 JDBC 工具类中的 SQL 标识符校验异常转换为业务异常，保持原有错误码语义。
     */
    private void validateJdbcIdentifier(String identifier, String fieldName) {
        try {
            JdbcUtils.validateIdentifier(identifier, fieldName);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(40008, ex.getMessage());
        }
    }

    private LocalDateTime computeNextFireTime(@NonNull String cronExpression, ZoneId zoneId, LocalDateTime fromTime) {
        CronExpression cron = CronExpression.parse(cronExpression);
        ZonedDateTime next = cron.next(fromTime.atZone(zoneId));
        return next != null ? next.toLocalDateTime() : null;
    }
}