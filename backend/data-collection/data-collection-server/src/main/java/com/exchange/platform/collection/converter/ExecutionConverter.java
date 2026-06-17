package com.exchange.platform.collection.converter;

import com.exchange.platform.collection.api.enums.RunStatus;
import com.exchange.platform.collection.api.enums.TriggerType;
import com.exchange.platform.collection.api.response.monitor.ExceptionRecordView;
import com.exchange.platform.collection.api.response.execution.ExecutionConfigSnapshotView;
import com.exchange.platform.collection.api.response.execution.ExecutionSchedulerView;
import com.exchange.platform.collection.api.response.monitor.ProblemRecordView;
import com.exchange.platform.collection.api.response.execution.ResultSummaryView;
import com.exchange.platform.collection.api.response.monitor.RunLogView;
import com.exchange.platform.collection.api.response.execution.TaskRunView;
import com.exchange.platform.collection.entity.monitor.ExceptionRecord;
import com.exchange.platform.collection.entity.execution.ExecutionConfigSnapshot;
import com.exchange.platform.collection.entity.execution.ExecutionScheduler;
import com.exchange.platform.collection.entity.monitor.ProblemRecord;
import com.exchange.platform.collection.entity.execution.ResultSummary;
import com.exchange.platform.collection.entity.monitor.RunLog;
import com.exchange.platform.collection.entity.execution.TaskRun;


public class ExecutionConverter {
    
    private ExecutionConverter() {}

    /** 任务执行记录转换：TaskRun -> TaskRunView */
    public static TaskRunView toRunView(TaskRun taskRun) {
        if (taskRun == null) {
            return null;
        }
        TaskRunView view = new TaskRunView();
        // 设置 view 的属性
        view.setRunId(taskRun.getRunId());
        view.setTaskConfigCode(taskRun.getTaskConfigCode());
        view.setTaskCode(taskRun.getTaskCode());
        view.setTriggerType(taskRun.getTriggerType());
        view.setSchedulerId(taskRun.getSchedulerId());
        view.setRunStatus(taskRun.getRunStatus());
        view.setStartTime(taskRun.getStartTime());
        view.setEndTime(taskRun.getEndTime());
        view.setScheduledFireTime(taskRun.getScheduledFireTime());
        view.setRunInfo(taskRun.getRunInfo());
        view.setCreatedAt(taskRun.getCreatedAt());
        return view;
    }

    public static ExecutionConfigSnapshotView toSnapshotView(ExecutionConfigSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        ExecutionConfigSnapshotView view = new ExecutionConfigSnapshotView();
        view.setSnapshotId(snapshot.getSnapshotId());
        view.setRunId(snapshot.getRunId());
        view.setTaskConfigCode(snapshot.getTaskConfigCode());
        view.setTaskCode(snapshot.getTaskCode());
        view.setTaskTemplateVersion(snapshot.getTaskTemplateVersion());
        view.setDataTopicCode(snapshot.getDataTopicCode());
        view.setDataTypeCode(snapshot.getDataTypeCode());
        view.setDataTypeVersion(snapshot.getDataTypeVersion());
        view.setHandlerName(snapshot.getHandlerName());
        view.setSnapshotContent(snapshot.getSnapshotContent());
        view.setCreatedAt(snapshot.getCreatedAt());
        return view;
    }

    public static ResultSummaryView toSummaryView(ResultSummary resultSummary) {
        if (resultSummary == null) {
            return null;
        }
        ResultSummaryView view = new ResultSummaryView();
        view.setSummaryId(resultSummary.getSummaryId());
        view.setRunId(resultSummary.getRunId());
        view.setTotalCount(resultSummary.getTotalCount());
        view.setSuccessCount(resultSummary.getSuccessCount());
        view.setFailureCount(resultSummary.getFailureCount());
        view.setResultLocation(resultSummary.getResultLocation());
        view.setSummaryContent(resultSummary.getSummaryContent());
        view.setCreatedAt(resultSummary.getCreatedAt());
        return view;
    }

    public static RunLogView toLogView(RunLog taskRun) {
        if (taskRun == null) {
            return null;
        }
        RunLogView view = new RunLogView();
        view.setRunId(taskRun.getRunId());
        view.setLogId(taskRun.getLogId());
        view.setLogLevel(taskRun.getLogLevel());
        view.setLogContent(taskRun.getLogContent());
        view.setTraceId(taskRun.getTraceId());
        view.setCreatedAt(taskRun.getCreatedAt());
        return view;
    }

    public static ExceptionRecordView toExceptionView(ExceptionRecord exceptionRecord) {
        if (exceptionRecord == null) {
            return null;
        }
        ExceptionRecordView view = new ExceptionRecordView();
        view.setExceptionId(exceptionRecord.getExceptionId());
        view.setRunId(exceptionRecord.getRunId());
        view.setExceptionType(exceptionRecord.getExceptionType());
        view.setExceptionMessage(exceptionRecord.getExceptionMessage());
        view.setCreatedAt(exceptionRecord.getCreatedAt());
        return view;
    }

    public static ProblemRecordView toProblemView(ProblemRecord problemRecord) {
        if (problemRecord == null) {
            return null;
        }
        ProblemRecordView view = new ProblemRecordView();
        view.setProblemId(problemRecord.getProblemId());
        view.setRunId(problemRecord.getRunId());
        view.setProblemType(problemRecord.getProblemType());
        view.setProblemMessage(problemRecord.getProblemMessage());
        view.setSampleDataJson(problemRecord.getSampleDataJson());
        view.setCreatedAt(problemRecord.getCreatedAt());
        return view;
    }

    public static TaskRunView toScheduledTaskRunView(ExecutionScheduler scheduler, TriggerType triggerType) {
        TaskRunView view = new TaskRunView();
        view.setRunId(null);
        view.setTaskConfigCode(scheduler.getTaskConfigCode());
        view.setTaskCode(scheduler.getTaskCode());
        view.setTriggerType(triggerType);
        view.setRunStatus(RunStatus.PENDING);
        view.setSchedulerId(scheduler.getSchedulerId());
        view.setCreatedAt(scheduler.getCreatedAt());
        return view;
    }

    public static ExecutionSchedulerView toSchedulerView(ExecutionScheduler scheduler) {
        if (scheduler == null) {
            return null;
        }
        ExecutionSchedulerView view = new ExecutionSchedulerView();
        view.setSchedulerId(scheduler.getSchedulerId());
        view.setTaskConfigCode(scheduler.getTaskConfigCode());
        view.setTaskCode(scheduler.getTaskCode());
        view.setCronExpression(scheduler.getCronExpression());
        view.setTimeZone(scheduler.getTimeZone());
        view.setStatus(scheduler.getStatus());
        view.setNextFireTime(scheduler.getNextFireTime());
        view.setLastFireTime(scheduler.getLastFireTime());
        view.setLastRunId(scheduler.getLastRunId());
        view.setRequestId(scheduler.getRequestId());
        view.setCreatedAt(scheduler.getCreatedAt());
        view.setUpdatedAt(scheduler.getUpdatedAt());
        return view;
    }
}
