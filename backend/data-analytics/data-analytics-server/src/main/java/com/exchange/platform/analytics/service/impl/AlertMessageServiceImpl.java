package com.exchange.platform.analytics.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exchange.platform.analytics.api.enums.CommonStatus;
import com.exchange.platform.analytics.api.enums.RuleType;
import com.exchange.platform.analytics.api.response.AlertMessageView;
import com.exchange.platform.analytics.entity.AlertMessage;
import com.exchange.platform.analytics.entity.AlertRule;
import com.exchange.platform.analytics.entity.AlertRuleRuntime;
import com.exchange.platform.analytics.entity.AppMetric;
import com.exchange.platform.analytics.entity.WatchTarget;
import com.exchange.platform.analytics.mapper.AlertMessageMapper;
import com.exchange.platform.analytics.mapper.AlertRuleMapper;
import com.exchange.platform.analytics.mapper.AlertRuleRuntimeMapper;
import com.exchange.platform.analytics.mapper.MetricMapper;
import com.exchange.platform.analytics.mapper.WatchTargetMapper;
import com.exchange.platform.analytics.service.AlertMessageService;
import com.exchange.platform.analytics.service.MetricValueQueryService;
import com.exchange.platform.analytics.api.request.MetricQueryRequest;
import com.exchange.platform.analytics.api.response.MetricValueView;

@Service
public class AlertMessageServiceImpl implements AlertMessageService {
    private static final Set<String> OPERATORS = Set.of("GT", "GTE", "LT", "LTE", "EQ", "NE");
    private final AlertRuleMapper ruleMapper;
    private final AlertMessageMapper messageMapper;
    private final AlertRuleRuntimeMapper runtimeMapper;
    private final MetricMapper metricMapper;
    private final WatchTargetMapper watchTargetMapper;
    private final MetricValueQueryService metricValueQueryService;

    public AlertMessageServiceImpl(AlertRuleMapper ruleMapper, AlertMessageMapper messageMapper,
            AlertRuleRuntimeMapper runtimeMapper, MetricMapper metricMapper, WatchTargetMapper watchTargetMapper,
            MetricValueQueryService metricValueQueryService) {
        this.ruleMapper = ruleMapper;
        this.messageMapper = messageMapper;
        this.runtimeMapper = runtimeMapper;
        this.metricMapper = metricMapper;
        this.watchTargetMapper = watchTargetMapper;
        this.metricValueQueryService = metricValueQueryService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int evaluateEnabledRules() {
        LocalDateTime now = LocalDateTime.now();
        List<AlertRule> rules = ruleMapper.selectList(new LambdaQueryWrapper<AlertRule>()
                .eq(AlertRule::getRuleType, RuleType.METRIC)
                .eq(AlertRule::getStatus, CommonStatus.ENABLED));
        rules = rules.stream().filter(rule -> inEffectivePeriod(rule, now)).collect(Collectors.toList());
        if (rules.isEmpty()) return 0;

        Map<String, List<AlertRule>> byType = rules.stream().collect(Collectors.groupingBy(AlertRule::getTargetType,
                LinkedHashMap::new, Collectors.toList()));
        Map<String, MetricValueView> values = new HashMap<>();
        for (Map.Entry<String, List<AlertRule>> entry : byType.entrySet()) {
            List<String> targets = entry.getValue().stream().map(AlertRule::getTargetCode).distinct().toList();
            List<String> metrics = entry.getValue().stream().map(AlertRule::getMetricCode).distinct().toList();
            List<MetricValueView> rows = metricValueQueryService.queryMetricValues(
                    new MetricQueryRequest(com.exchange.platform.common.core.enums.AssetType.fromCode(entry.getKey()), targets, metrics, null));
            for (MetricValueView row : rows) values.put(key(row.getTargetCode(), row.getMetricCode()), row);
        }

        int generated = 0;
        for (AlertRule rule : rules) {
            MetricValueView value = values.get(key(rule.getTargetCode(), rule.getMetricCode()));
            if (value == null || value.getValue() == null || !StringUtils.hasText(value.getDataTime())) continue;
            LocalDateTime dataTime = parseTime(value.getDataTime());
            if (dataTime == null) continue;
            AlertRuleRuntime runtime = runtimeMapper.selectOne(new LambdaQueryWrapper<AlertRuleRuntime>()
                    .eq(AlertRuleRuntime::getRuleId, rule.getId()));
            if (runtime != null && runtime.getLastEvaluatedDataTime() != null
                    && !dataTime.isAfter(runtime.getLastEvaluatedDataTime())) continue;

            Map<String, Object> condition = parseCondition(rule.getConditionJson());
            String operator = String.valueOf(condition.getOrDefault("operator", condition.getOrDefault("compareOperator", "")));
            operator = canonicalOperator(operator);
            BigDecimal threshold = decimal(condition.containsKey("threshold") ? condition.get("threshold") : condition.get("thresholdValue"));
            BigDecimal triggerValue = decimal(value.getValue());
            if (!OPERATORS.contains(operator) || threshold == null || triggerValue == null) {
                saveRuntime(runtime, rule.getId(), dataTime, false);
                continue;
            }
            boolean matched = compare(triggerValue, operator, threshold);
            boolean wasTrue = runtime != null && Boolean.TRUE.equals(runtime.getConditionTrue());
            saveRuntime(runtime, rule.getId(), dataTime, matched);
            if (matched && !wasTrue) {
                generated += createMessage(rule, triggerValue, threshold, operator, dataTime);
            }
        }
        return generated;
    }

    private int createMessage(AlertRule rule, BigDecimal value, BigDecimal threshold, String operator, LocalDateTime dataTime) {
        AppMetric metric = metricMapper.selectOne(new LambdaQueryWrapper<AppMetric>().eq(AppMetric::getMetricCode, rule.getMetricCode()));
        String metricName = metric == null ? rule.getMetricCode() : metric.getMetricName();
        String unit = metric == null ? "" : metric.getUnit();
        WatchTarget target = watchTargetMapper.selectList(new LambdaQueryWrapper<WatchTarget>()
                .eq(WatchTarget::getTargetCode, rule.getTargetCode())
                .eq(WatchTarget::getTargetType, rule.getTargetType())).stream().findFirst().orElse(null);
        String targetName = target == null ? rule.getTargetCode() : target.getTargetName();
        String symbol = operatorText(operator);
        AlertMessage message = new AlertMessage().setRuleId(rule.getId()).setTargetType(rule.getTargetType())
                .setTargetCode(rule.getTargetCode()).setTargetNameSnapshot(targetName).setMetricCode(rule.getMetricCode())
                .setMetricNameSnapshot(metricName).setUnitSnapshot(unit).setOperator(operator).setThreshold(threshold)
                .setTriggerValue(value).setMessageTitle(targetName + " · " + metricName + " 指标提醒")
                .setMessageContent(metricName + "当前值为 " + value.stripTrailingZeros().toPlainString() + unit
                        + "，已满足“" + symbol + " " + threshold.stripTrailingZeros().toPlainString() + unit + "”的提醒条件。")
                .setDataTime(dataTime).setTriggeredAt(LocalDateTime.now()).setReadStatus("UNREAD")
                .setProcessStatus("PENDING").setCreatedAt(LocalDateTime.now());
        try {
            messageMapper.insert(message);
            return 1;
        } catch (DuplicateKeyException ignored) {
            return 0;
        }
    }

    private void saveRuntime(AlertRuleRuntime runtime, Long ruleId, LocalDateTime dataTime, boolean matched) {
        if (runtime == null) runtime = new AlertRuleRuntime().setRuleId(ruleId);
        runtime.setLastEvaluatedDataTime(dataTime).setConditionTrue(matched).setUpdatedAt(LocalDateTime.now());
        if (runtime.getId() == null) {
            try {
                runtimeMapper.insert(runtime);
            } catch (DuplicateKeyException ignored) {
                AlertRuleRuntime existing = runtimeMapper.selectOne(new LambdaQueryWrapper<AlertRuleRuntime>()
                        .eq(AlertRuleRuntime::getRuleId, ruleId));
                if (existing != null) {
                    existing.setLastEvaluatedDataTime(dataTime).setConditionTrue(matched).setUpdatedAt(LocalDateTime.now());
                    runtimeMapper.updateById(existing);
                }
            }
        } else runtimeMapper.updateById(runtime);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlertMessageView> listRecent(String targetType, String targetCode, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        return messageMapper.selectList(new LambdaQueryWrapper<AlertMessage>()
                .eq(StringUtils.hasText(targetType), AlertMessage::getTargetType, targetType)
                .eq(StringUtils.hasText(targetCode), AlertMessage::getTargetCode, targetCode)
                .orderByDesc(AlertMessage::getTriggeredAt).last("LIMIT " + safeLimit))
                .stream().map(this::toView).toList();
    }

    private AlertMessageView toView(AlertMessage m) {
        AlertMessageView v = new AlertMessageView();
        v.setId(m.getId()); v.setRuleId(m.getRuleId()); v.setTargetType(m.getTargetType()); v.setTargetCode(m.getTargetCode());
        v.setTargetNameSnapshot(m.getTargetNameSnapshot()); v.setMetricCode(m.getMetricCode()); v.setMetricNameSnapshot(m.getMetricNameSnapshot());
        v.setUnitSnapshot(m.getUnitSnapshot()); v.setOperator(m.getOperator()); v.setThreshold(m.getThreshold()); v.setTriggerValue(m.getTriggerValue());
        v.setMessageTitle(m.getMessageTitle()); v.setMessageContent(m.getMessageContent()); v.setDataTime(m.getDataTime()); v.setTriggeredAt(m.getTriggeredAt());
        v.setReadStatus(m.getReadStatus()); v.setProcessStatus(m.getProcessStatus()); return v;
    }
    private boolean inEffectivePeriod(AlertRule r, LocalDateTime now) {
        return (r.getEffectiveStartTime() == null || !now.isBefore(r.getEffectiveStartTime()))
                && (r.getEffectiveEndTime() == null || !now.isAfter(r.getEffectiveEndTime()));
    }
    @SuppressWarnings("unchecked") private Map<String, Object> parseCondition(String json) {
        if (!StringUtils.hasText(json)) return Map.of();
        try { return JSON.parseObject(json, Map.class); } catch (RuntimeException ex) { return Map.of(); }
    }
    private BigDecimal decimal(Object value) { try { return value == null ? null : new BigDecimal(String.valueOf(value)); } catch (RuntimeException ex) { return null; } }
    private boolean compare(BigDecimal v, String op, BigDecimal t) { int c = v.compareTo(t); return switch (op) { case "GT" -> c > 0; case "GTE" -> c >= 0; case "LT" -> c < 0; case "LTE" -> c <= 0; case "EQ" -> c == 0; case "NE" -> c != 0; default -> false; }; }
    private String operatorText(String op) { return Map.of("GT", ">", "GTE", "≥", "LT", "<", "LTE", "≤", "EQ", "=", "NE", "≠").getOrDefault(op, op); }
    private String canonicalOperator(String op) { return Map.of("GREATER_THAN", "GT", "GREATER_EQUAL", "GTE", "LESS_THAN", "LT", "LESS_EQUAL", "LTE", "EQUAL", "EQ", "NOT_EQUAL", "NE").getOrDefault(op, op); }
    private String key(String target, String metric) { return target + "\u0000" + metric; }
    private LocalDateTime parseTime(String value) { try { return LocalDateTime.parse(value); } catch (RuntimeException ignored) { try { return OffsetDateTime.parse(value).toLocalDateTime(); } catch (RuntimeException ignored2) { try { return LocalDate.parse(value).atStartOfDay(); } catch (RuntimeException ignored3) { return null; } } } }
}
