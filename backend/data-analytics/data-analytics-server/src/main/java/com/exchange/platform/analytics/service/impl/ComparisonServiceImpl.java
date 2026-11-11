package com.exchange.platform.analytics.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exchange.platform.analytics.api.enums.CommonStatus;
import com.exchange.platform.analytics.api.enums.ResultType;
import com.exchange.platform.analytics.api.request.ComparisonQueryRequest;
import com.exchange.platform.analytics.api.request.ComparisonResultSaveRequest;
import com.exchange.platform.analytics.api.request.MetricQueryRequest;
import com.exchange.platform.analytics.api.response.ComparisonMetricRowView;
import com.exchange.platform.analytics.api.response.ComparisonResultView;
import com.exchange.platform.analytics.api.response.ComparisonSnapshotView;
import com.exchange.platform.analytics.api.response.ComparisonTargetView;
import com.exchange.platform.analytics.api.response.MetricValueView;
import com.exchange.platform.analytics.entity.AppMetric;
import com.exchange.platform.analytics.entity.AppResult;
import com.exchange.platform.analytics.mapper.AppResultMapper;
import com.exchange.platform.analytics.mapper.MetricMapper;
import com.exchange.platform.analytics.mapper.TypeBasicDataMapper;
import com.exchange.platform.analytics.service.ComparisonService;
import com.exchange.platform.analytics.service.MetricValueQueryService;
import com.exchange.platform.common.core.enums.AssetType;
import com.exchange.platform.common.core.exception.BusinessException;

/** 明确标的集合的横向矩阵；指标为行、标的为列，不计算优劣或综合评分。 */
@Service
public class ComparisonServiceImpl implements ComparisonService {
    private final MetricValueQueryService metricValueQueryService;
    private final MetricMapper metricMapper;
    private final AppResultMapper appResultMapper;
    private final TypeBasicDataMapper typeBasicDataMapper;

    public ComparisonServiceImpl(MetricValueQueryService metricValueQueryService, MetricMapper metricMapper, AppResultMapper appResultMapper, TypeBasicDataMapper typeBasicDataMapper) {
        this.metricValueQueryService = metricValueQueryService; this.metricMapper = metricMapper; this.appResultMapper = appResultMapper; this.typeBasicDataMapper = typeBasicDataMapper;
    }

    @Override @Transactional(readOnly = true)
    public ComparisonResultView execute(ComparisonQueryRequest request) {
        validate(request);
        List<String> targets = normalize(request.targetCodes()); List<String> metrics = normalize(request.metricCodes());
        Map<String, AppMetric> metricCatalog = metricMapper.selectList(new LambdaQueryWrapper<AppMetric>().in(AppMetric::getMetricCode, metrics))
                .stream().collect(Collectors.toMap(AppMetric::getMetricCode, metric -> metric, (a,b) -> a, LinkedHashMap::new));
        Map<String, Map<String, Object>> values = new LinkedHashMap<>(); Map<String, Map<String, String>> times = new LinkedHashMap<>();
        metrics.forEach(metric -> { values.put(metric, new LinkedHashMap<>()); times.put(metric, new LinkedHashMap<>()); targets.forEach(target -> values.get(metric).put(target, null)); });
        List<MetricValueView> queried = metricValueQueryService.queryMetricValues(new MetricQueryRequest(request.targetType(), targets, metrics, request.asOfTime()));
        queried.forEach(value -> { if (values.containsKey(value.getMetricCode()) && values.get(value.getMetricCode()).containsKey(value.getTargetCode())) { values.get(value.getMetricCode()).put(value.getTargetCode(), value.getValue()); times.get(value.getMetricCode()).put(value.getTargetCode(), value.getDataTime()); } });
        ComparisonResultView result = new ComparisonResultView(); result.setTargetType(request.targetType());
        Map<String, String> targetNames = typeBasicDataMapper.selectTargetsByCodes(request.targetType().getValue(), targets).stream()
                .collect(Collectors.toMap(target -> target.getTargetCode(), target -> target.getTargetName(), (left, right) -> left));
        result.setTargets(targets.stream().map(code -> new ComparisonTargetView(code, targetNames.getOrDefault(code, code))).toList());
        result.setRows(metrics.stream().map(metric -> { ComparisonMetricRowView row = new ComparisonMetricRowView(); AppMetric definition = metricCatalog.get(metric); row.setMetricCode(metric); row.setMetricName(definition == null ? metric : definition.getMetricName()); row.setUnit(definition == null ? null : definition.getUnit()); row.setValues(values.get(metric)); row.setDataTimes(times.get(metric)); return row; }).toList());
        return result;
    }

    @Override @Transactional(rollbackFor = Exception.class)
    public ComparisonSnapshotView saveResult(ComparisonResultSaveRequest save) {
        validate(save.request()); ComparisonSnapshotView snapshot = new ComparisonSnapshotView(); snapshot.setResultId(java.util.UUID.randomUUID().toString().replace("-", "")); snapshot.setExecutedAt(LocalDateTime.now()); snapshot.setRequest(save.request()); snapshot.setResult(save.result());
        appResultMapper.insert(new AppResult().setResultId(snapshot.getResultId()).setResultType(ResultType.COMPARISON).setRelatedObjectType("COMPARISON_SNAPSHOT").setResultSummaryJson(JSON.toJSONString(snapshot)).setSourceDataTime(save.request().asOfTime()).setCreatedAt(snapshot.getExecutedAt()));
        return snapshot;
    }

    @Override @Transactional(readOnly = true)
    public ComparisonSnapshotView getSavedResult(String resultId) {
        if (!StringUtils.hasText(resultId)) throw new BusinessException(40001, "对比结果标识不能为空");
        AppResult entity = appResultMapper.selectOne(new LambdaQueryWrapper<AppResult>().eq(AppResult::getResultId, resultId.trim()).eq(AppResult::getResultType, ResultType.COMPARISON));
        if (entity == null || !StringUtils.hasText(entity.getResultSummaryJson())) throw new BusinessException(40001, "对比结果不存在：" + resultId);
        return JSON.parseObject(entity.getResultSummaryJson(), ComparisonSnapshotView.class);
    }

    private void validate(ComparisonQueryRequest request) {
        if (request == null || request.targetType() == null || request.targetCodes() == null || request.metricCodes() == null) throw new BusinessException(40001, "对比请求不完整");
        if (request.targetType() != AssetType.STOCK && request.targetType() != AssetType.INDEX) throw new BusinessException(40001, "当前仅支持股票和指数对比");
        if (normalize(request.targetCodes()).isEmpty() || normalize(request.metricCodes()).isEmpty()) throw new BusinessException(40001, "对比标的和指标不能为空");
    }
    private List<String> normalize(List<String> raw) { if (raw == null) return List.of(); Set<String> set = new LinkedHashSet<>(); raw.stream().filter(StringUtils::hasText).map(String::trim).forEach(set::add); return new ArrayList<>(set); }
}
