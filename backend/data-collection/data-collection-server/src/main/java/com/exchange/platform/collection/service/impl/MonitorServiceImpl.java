package com.exchange.platform.collection.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exchange.platform.collection.api.request.execution.TaskRunRequest;
import com.exchange.platform.collection.api.response.monitor.ExceptionRecordView;
import com.exchange.platform.collection.api.response.monitor.ProblemRecordView;
import com.exchange.platform.collection.api.response.monitor.RunLogView;
import com.exchange.platform.collection.api.response.monitor.TraceView;
import com.exchange.platform.collection.converter.ExecutionConverter;
import com.exchange.platform.collection.converter.TaskTemplateConverter;
import com.exchange.platform.collection.entity.execution.ExecutionConfigSnapshot;
import com.exchange.platform.collection.entity.execution.ResultSummary;
import com.exchange.platform.collection.entity.execution.TaskRun;
import com.exchange.platform.collection.entity.monitor.ExceptionRecord;
import com.exchange.platform.collection.entity.monitor.ProblemRecord;
import com.exchange.platform.collection.entity.monitor.RunLog;
import com.exchange.platform.collection.entity.tasks.TaskTemplateVersion;
import com.exchange.platform.collection.mapper.execution.ExecutionConfigSnapshotMapper;
import com.exchange.platform.collection.mapper.execution.ResultSummaryMapper;
import com.exchange.platform.collection.mapper.execution.TaskRunMapper;
import com.exchange.platform.collection.mapper.monitor.ExceptionRecordMapper;
import com.exchange.platform.collection.mapper.monitor.ProblemRecordMapper;
import com.exchange.platform.collection.mapper.monitor.RunLogMapper;
import com.exchange.platform.collection.mapper.tasks.TaskTemplateVersionMapper;
import com.exchange.platform.collection.service.MonitorService;
import com.exchange.platform.common.core.exception.BusinessException;
import com.exchange.platform.common.core.page.PageResult;


@Service
public class MonitorServiceImpl extends ServiceImpl<RunLogMapper, RunLog> implements MonitorService {

    private static final Logger log = LoggerFactory.getLogger(MonitorServiceImpl.class);

    private final ExceptionRecordMapper exceptionRecordMapper;
    private final ProblemRecordMapper problemRecordMapper;
    private final TaskRunMapper taskRunMapper;
    private final ExecutionConfigSnapshotMapper snapshotMapper;
    private final TaskTemplateVersionMapper taskTemplateVersionMapper;
    private final ResultSummaryMapper resultSummaryMapper;
    private final RunLogMapper runLogMapper;

    public MonitorServiceImpl(ExceptionRecordMapper exceptionRecordMapper, ProblemRecordMapper problemRecordMapper, TaskRunMapper taskRunMapper, ExecutionConfigSnapshotMapper snapshotMapper, TaskTemplateVersionMapper taskTemplateVersionMapper, ResultSummaryMapper resultSummaryMapper, RunLogMapper runLogMapper) {
        this.exceptionRecordMapper = exceptionRecordMapper;
        this.problemRecordMapper = problemRecordMapper;
        this.taskRunMapper = taskRunMapper;
        this.snapshotMapper = snapshotMapper;
        this.taskTemplateVersionMapper = taskTemplateVersionMapper;
        this.resultSummaryMapper = resultSummaryMapper;
        this.runLogMapper = runLogMapper;
    }

    /**
     * 分页查询运行日志
     */
    @Override
    @Transactional(readOnly = true)
    public PageResult<RunLogView> queryRunLogs(TaskRunRequest params) {
        log.info("[Service] queryRunLogs params={}", params);

        int pageNo = (params.pageNo() == null || params.pageNo() < 1) ? 1 : params.pageNo();
        int pageSize = (params.pageSize() == null || params.pageSize() < 1) ? 20 : Math.min(200, params.pageSize());

        LambdaQueryWrapper<RunLog> wrapper = new LambdaQueryWrapper<RunLog>()
            .eq(StringUtils.hasText(params.runId()), RunLog::getRunId, params.runId())
            .ge(StringUtils.hasText(params.startTime()), RunLog::getCreatedAt, params.startTime())
            .le(StringUtils.hasText(params.endTime()), RunLog::getCreatedAt, params.endTime())
            .orderByDesc(RunLog::getCreatedAt);

        Page<RunLog> page = this.page(new Page<>(pageNo, pageSize), wrapper);
        List<RunLogView> records = page.getRecords()
            .stream()
            .map(ExecutionConverter::toLogView)
            .collect(Collectors.toList());
        
        return new PageResult<>(pageNo, pageSize, page.getTotal(), records);
    }

    /**
     * 分页查询异常记录
     */
    @Override
    @Transactional(readOnly = true)
    public PageResult<ExceptionRecordView> queryExceptionRecords(TaskRunRequest params) {
        log.info("[Service] queryExceptionRecords params={}", params);

        int pageNo = (params.pageNo() == null || params.pageNo() < 1) ? 1 : params.pageNo();
        int pageSize = (params.pageSize() == null || params.pageSize() < 1) ? 20 : Math.min(200, params.pageSize());

        LambdaQueryWrapper<ExceptionRecord> wrapper = new LambdaQueryWrapper<ExceptionRecord>()
            .eq(StringUtils.hasText(params.runId()), ExceptionRecord::getRunId, params.runId())
            .ge(StringUtils.hasText(params.startTime()), ExceptionRecord::getCreatedAt, params.startTime())
            .le(StringUtils.hasText(params.endTime()), ExceptionRecord::getCreatedAt, params.endTime())
            .orderByDesc(ExceptionRecord::getCreatedAt);

        Page<ExceptionRecord> page = exceptionRecordMapper.selectPage(new Page<>(pageNo, pageSize), wrapper);
        List<ExceptionRecordView> records = page.getRecords()
            .stream()
            .map(ExecutionConverter::toExceptionView)
            .collect(Collectors.toList());
        return new PageResult<>(pageNo, pageSize, page.getTotal(), records);
    }

    /**
     * 分页查询问题记录
     */
    @Override
    @Transactional(readOnly = true)
    public PageResult<ProblemRecordView> queryProblemRecords(TaskRunRequest params) {
        log.info("[Service] queryProblemRecords params={}", params);

        int pageNo = (params.pageNo() == null || params.pageNo() < 1) ? 1 : params.pageNo();
        int pageSize = (params.pageSize() == null || params.pageSize() < 1) ? 20 : Math.min(200, params.pageSize());

        LambdaQueryWrapper<ProblemRecord> wrapper = new LambdaQueryWrapper<ProblemRecord>()
            .eq(StringUtils.hasText(params.runId()), ProblemRecord::getRunId, params.runId())
            .ge(StringUtils.hasText(params.startTime()), ProblemRecord::getCreatedAt, params.startTime())
            .le(StringUtils.hasText(params.endTime()), ProblemRecord::getCreatedAt, params.endTime())
            .orderByDesc(ProblemRecord::getCreatedAt);
        
        Page<ProblemRecord> page = problemRecordMapper.selectPage(new Page<>(pageNo, pageSize), wrapper);
        List<ProblemRecordView> records = page.getRecords()
            .stream()
            .map(ExecutionConverter::toProblemView)
            .collect(Collectors.toList());  
        return new PageResult<>(pageNo, pageSize, page.getTotal(), records);
    }

    @Override
    @Transactional(readOnly = true)
    public TraceView getTrace(String runId) {
        log.info("[Service] getTrace runId={}", runId);

        TraceView trace = new TraceView();

        // 任务执行记录
        TaskRun run = taskRunMapper.selectOne(new LambdaQueryWrapper<TaskRun>()
            .eq(TaskRun::getRunId, runId).last("LIMIT 1"));
        if (run == null) {
            throw new BusinessException(40003, "执行记录不存在：" + runId);
        }
        trace.setTaskRun(ExecutionConverter.toRunView(run));

        // 执行配置快照
        ExecutionConfigSnapshot snapshot = snapshotMapper.selectOne(new LambdaQueryWrapper<ExecutionConfigSnapshot>()
            .eq(ExecutionConfigSnapshot::getRunId, runId).last("LIMIT 1"));
        trace.setSnapshot(ExecutionConverter.toSnapshotView(snapshot));

        // 执行时任务模板版本（以执行记录冻结的版本主键为准）
        if (Integer.valueOf(snapshot.getTaskTemplateVersion()) != null) {
            TaskTemplateVersion version = taskTemplateVersionMapper
            .selectOne(new LambdaQueryWrapper<TaskTemplateVersion>()
                .eq(TaskTemplateVersion::getTaskCode, snapshot.getTaskCode()).eq(TaskTemplateVersion::getVersionNo, snapshot.getTaskTemplateVersion())
                .last("LIMIT 1")
            );
            trace.setTemplateVersion(TaskTemplateConverter.toVersionView(version));
        }

        // 结果摘要
        ResultSummary summary = resultSummaryMapper.selectOne(new LambdaQueryWrapper<ResultSummary>()
            .eq(ResultSummary::getRunId, runId)
            .last("LIMIT 1")
        );
        trace.setResultSummary(ExecutionConverter.toSummaryView(summary));

        // 运行留痕索引：运行日志、异常记录、问题记录列表
        trace.setRunLogs(
            runLogMapper.selectList(
                new LambdaQueryWrapper<RunLog>()
                    .eq(RunLog::getRunId, runId)
                    .orderByDesc(RunLog::getCreatedAt)
                )
                .stream()
                .map(ExecutionConverter::toLogView)
                .collect(Collectors.toList())
        );
        
        trace.setExceptionRecords(
            exceptionRecordMapper.selectList(
                new LambdaQueryWrapper<ExceptionRecord>()
                    .eq(ExceptionRecord::getRunId, runId)
                    .orderByDesc(ExceptionRecord::getCreatedAt)
                )
                .stream()
                .map(ExecutionConverter::toExceptionView)
                .collect(Collectors.toList())
        );
        
        trace.setProblemRecords(
            problemRecordMapper.selectList(
                new LambdaQueryWrapper<ProblemRecord>()
                    .eq(ProblemRecord::getRunId, runId)
                    .orderByDesc(ProblemRecord::getCreatedAt)
                )
                .stream()
                .map(ExecutionConverter::toProblemView)
                .collect(Collectors.toList())
        );

        return trace;
    }
    
}
