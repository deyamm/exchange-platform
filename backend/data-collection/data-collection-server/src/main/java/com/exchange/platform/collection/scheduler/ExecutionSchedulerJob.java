package com.exchange.platform.collection.scheduler;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.exchange.platform.collection.api.enums.ScheduleStatus;
import com.exchange.platform.collection.entity.execution.ExecutionScheduler;
import com.exchange.platform.collection.entity.execution.TaskRun;
import com.exchange.platform.collection.mapper.execution.ExecutionSchedulerMapper;
import com.exchange.platform.collection.service.impl.ExecutionServiceImpl;

@Component
public class ExecutionSchedulerJob {

    private static final Logger log = LoggerFactory.getLogger(ExecutionSchedulerJob.class);
    private static final int MAX_BATCH_SIZE = 20;
    private static final String DEFAULT_TIME_ZONE = "Asia/Shanghai";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final ExecutionSchedulerMapper schedulerMapper;
    private final ExecutionServiceImpl executionService;

    public ExecutionSchedulerJob(ExecutionSchedulerMapper schedulerMapper, ExecutionServiceImpl executionService) {
        this.schedulerMapper = schedulerMapper;
        this.executionService = executionService;
    }

    /**
     * 从数据库轮询到期的调度配置，并创建对应的执行记录和执行任务
     */
    @Scheduled(fixedDelayString = "${execution.scheduler.poll-interval-ms:120000}")
    public void pollAndExecute() {
        ZoneId defaultZone = ZoneId.of(DEFAULT_TIME_ZONE);
        LocalDateTime now = LocalDateTime.now(defaultZone);

        // 查询ACTIVE状态且 nextFireTime <= 当前时间的调度配置，按 nextFireTime 升序，限制批次大小
        List<ExecutionScheduler> dueSchedulers = schedulerMapper.selectList(
            new LambdaQueryWrapper<ExecutionScheduler>()
                .eq(ExecutionScheduler::getStatus, ScheduleStatus.ACTIVE)
                .le(ExecutionScheduler::getNextFireTime, now)
                .orderByAsc(ExecutionScheduler::getNextFireTime)
                .last("LIMIT " + MAX_BATCH_SIZE)
        );
        
        // 遍历调度配置，尝试执行
        for (ExecutionScheduler scheduler : dueSchedulers) {
            try {
                handleScheduler(scheduler, now);
            } catch (Exception e) {
                log.error("Failed to execute schedulerId={}, error={}", scheduler.getSchedulerId(), e.getMessage(), e);
            }
        }
    }

    /**
     * 更新调度配置的执行时间、下次执行时间等信息，创建执行记录并触发执行
     * @param scheduler
     * @param now
     */
    private void handleScheduler(ExecutionScheduler scheduler, LocalDateTime now) {
        ZoneId zoneId = resolveZoneId(scheduler.getTimeZone());
        LocalDateTime fireTime = scheduler.getNextFireTime() != null ? scheduler.getNextFireTime() : now;
        LocalDateTime nextFireTime = computeNextFireTime(scheduler.getCronExpression(), zoneId, fireTime);
        if (nextFireTime == null) {
            log.warn("Scheduler cron has no next fire time, pausing schedulerId={}", scheduler.getSchedulerId());
            schedulerMapper.update(null, new UpdateWrapper<ExecutionScheduler>()
                .set("status", ScheduleStatus.PAUSED.getValue())
                .eq("id", scheduler.getId())
            );
            return;
        }

        int claimed = schedulerMapper.update(null, new UpdateWrapper<ExecutionScheduler>()
            .set("last_fire_time", fireTime)
            .set("next_fire_time", nextFireTime)
            .eq("id", scheduler.getId())
            .eq("next_fire_time", scheduler.getNextFireTime())
            .eq("status", ScheduleStatus.ACTIVE.getValue())
        );

        if (claimed != 1) {
            return;
        }
        // 根据创建调度时的参数模板，解析出本次执行的参数，创建执行记录，并触发执行
        Map<String, Object> params = resolveParams(scheduler, zoneId, fireTime);
        TaskRun taskRun = executionService.createScheduledRun(scheduler, params, fireTime);
        executionService.executeTaskRun(taskRun.getRunId());

        schedulerMapper.update(null, new UpdateWrapper<ExecutionScheduler>()
            .set("last_run_id", taskRun.getRunId())
            .eq("id", scheduler.getId())
        );
    }

    /**
     * 根据调度配置的参数模板和当前触发时间，解析出本次执行的参数值，支持嵌套结构和日期占位符解析
     * @param scheduler
     * @param zoneId
     * @param fireTime
     * @return
     */
    private Map<String, Object> resolveParams(ExecutionScheduler scheduler, ZoneId zoneId, LocalDateTime fireTime) {
        if (scheduler.getParamsTemplate() == null || scheduler.getParamsTemplate().isBlank()) {
            return Map.of();
        }

        // 通过JSON.parseObject，除日期字符串外，其他类型的值会被正确解析为对应的Java类型（如数字、布尔值、列表、对象等），然后通过递归解析日期占位符，符合日期点位符的字符串会被转换为对应的日期字符串，不符合的原样返回，最终得到一个完整解析后的参数Map
        @SuppressWarnings("unchecked")
        Map<String, Object> template = JSON.parseObject(scheduler.getParamsTemplate(), Map.class);
        return resolveTemplateMap(template, zoneId, fireTime);
    }

    /**
     * 递归解析Map
     * @param template
     * @param zoneId
     * @param fireTime
     * @return
     */
    private Map<String, Object> resolveTemplateMap(Map<String, Object> template, ZoneId zoneId, LocalDateTime fireTime) {
        Map<String, Object> resolved = new HashMap<>();
        for (Map.Entry<String, Object> entry : template.entrySet()) {
            resolved.put(entry.getKey(), resolveTemplateValue(entry.getValue(), zoneId, fireTime));
        }
        return resolved;
    }

    /**
     * 解析模板值，支持 Map、List、String 类型的递归解析，字符串中支持日期占位符解析
     * @param value
     * @param zoneId
     * @param fireTime
     * @return
     */
    private Object resolveTemplateValue(Object value, ZoneId zoneId, LocalDateTime fireTime) {
        if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> nested = (Map<String, Object>) value;
            return resolveTemplateMap(nested, zoneId, fireTime);
        }
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            List<Object> resolved = new ArrayList<>(list.size());
            for (Object item : list) {
                resolved.add(resolveTemplateValue(item, zoneId, fireTime));
            }
            return resolved;
        }
        if (value instanceof String) {
            return resolveTemplateString((String) value, zoneId, fireTime);
        }
        return value;
    }
    
    /**
     * 解析参数模板中的日期字符串，支持 ${dataSource:today}、${dataSource:yesterday}、${dataSource:now}、${dataSource:fireTime} 等占位符，其他字符串原样返回
     * @param template
     * @param zoneId
     * @param fireTime
     * @return
     */
    private String resolveTemplateString(String template, ZoneId zoneId, LocalDateTime fireTime) {
        if (template == null || template.isEmpty()) return template;

        // 此处复制一份template，防止破坏原有字符串
        String dateStr = template;
        LocalDate today = LocalDate.now(zoneId);
        LocalDate yesterday = today.minusDays(1);
        LocalDate tomorrow = today.plusDays(1);
        // 直接使用':'分隔数据源和token，不需要正则表达式
        // 格式必须严格为 {dataSource:token}，不支持其他变体
        if (template.startsWith("{")) {
            dateStr = template.substring(1);
        }
        if (template.endsWith("}")) {
            dateStr = dateStr.substring(0, dateStr.length() - 1);
        }
        String[] parts = dateStr.split(":", 2);
        // 这里对分隔符进行简单处理，规则为：如果parts为1，则认为没有数据源，直接使用默认格式解析；如果parts为2，则第一个部分为数据源，第二个部分为token，如果parts多于2，固定使用第一个和第二个部分，忽略后续部分
        String dataSource = parts.length > 1 ? parts[0] : "default";
        String tok = parts.length > 1 ? parts[1] : parts[0];
        if (tok.equals("today")) {
            dateStr = formatDateForDataSource(dataSource, DATE_FORMAT.format(today), DATE_FORMAT, zoneId, today, fireTime);
        } else if (tok.equals("yesterday")) {
            dateStr = formatDateForDataSource(dataSource, DATE_FORMAT.format(yesterday), DATE_FORMAT, zoneId, yesterday, fireTime);
        } else if (tok.equals("tomorrow")) {
            dateStr = formatDateForDataSource(dataSource, DATE_FORMAT.format(tomorrow), DATE_FORMAT, zoneId, tomorrow, fireTime);
        } else if (tok.equals("fireDate")) {
            dateStr = formatDateForDataSource(dataSource, DATE_FORMAT.format(fireTime.toLocalDate()), DATE_FORMAT, zoneId, fireTime.toLocalDate(), fireTime);
        } else if (tok.equals("now")) {
            // now uses full datetime
            dateStr = DATETIME_FORMAT.format(LocalDateTime.now(zoneId));
        } else if (tok.equals("fireTime")) {
            dateStr = DATETIME_FORMAT.format(fireTime);
        } else {
            // 不支持的token，原样返回
            dateStr = template;
        }
        return dateStr;
    }

    /**
     * 根据数据源类型，使用不同的日期格式化方式，目前支持默认格式（yyyy-MM-dd）和tushare格式（yyyyMMdd），如果dataSource未知则使用默认格式
     */
    private String formatDateForDataSource(String dataSource, String defaultDateStr, DateTimeFormatter defaultFormatter, ZoneId zoneId, LocalDate date, LocalDateTime fireTime) {
        if (dataSource == null || dataSource.isBlank()) {
            return defaultDateStr;
        }
        String ds = dataSource.toLowerCase();
        if (ds.equals("tushare")) {
            DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyyMMdd");
            return f.format(date);
        }
        // akshare and default: yyyy-MM-dd
        return defaultFormatter.format(date);
    }

    private LocalDateTime computeNextFireTime(String cronExpression, ZoneId zoneId, LocalDateTime fromTime) {
        CronExpression cron = CronExpression.parse(cronExpression);
        ZonedDateTime next = cron.next(fromTime.atZone(zoneId));
        return next != null ? next.toLocalDateTime() : null;
    }

    private ZoneId resolveZoneId(String timeZone) {
        if (timeZone == null || timeZone.isBlank()) {
            return ZoneId.of(DEFAULT_TIME_ZONE);
        }
        return ZoneId.of(timeZone);
    }
}
