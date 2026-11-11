package com.exchange.platform.analytics.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
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
import com.exchange.platform.analytics.api.request.MetricQueryRequest;
import com.exchange.platform.analytics.api.request.RankingQueryRequest;
import com.exchange.platform.analytics.api.request.RankingResultSaveRequest;
import com.exchange.platform.analytics.api.request.RankingRuleRequest;
import com.exchange.platform.analytics.api.request.TargetScopeRequest;
import com.exchange.platform.analytics.api.response.MetricValueView;
import com.exchange.platform.analytics.api.response.RankingResultRowView;
import com.exchange.platform.analytics.api.response.RankingResultView;
import com.exchange.platform.analytics.api.response.RankingSnapshotView;
import com.exchange.platform.analytics.entity.AppResult;
import com.exchange.platform.analytics.entity.TargetGroup;
import com.exchange.platform.analytics.entity.WatchTarget;
import com.exchange.platform.analytics.mapper.AppResultMapper;
import com.exchange.platform.analytics.mapper.TargetGroupMapper;
import com.exchange.platform.analytics.mapper.TypeBasicDataMapper;
import com.exchange.platform.analytics.mapper.WatchTargetMapper;
import com.exchange.platform.analytics.service.MetricValueQueryService;
import com.exchange.platform.analytics.service.RankingService;
import com.exchange.platform.analytics.service.support.TargetScopeTarget;
import com.exchange.platform.common.core.enums.AssetType;
import com.exchange.platform.common.core.exception.BusinessException;

/** 多字段排序：缺失值始终最后，最后以 targetCode ASC 稳定兜底。 */
@Service
public class RankingServiceImpl implements RankingService {
    private final MetricValueQueryService metricValueQueryService;
    private final TypeBasicDataMapper typeBasicDataMapper;
    private final TargetGroupMapper targetGroupMapper;
    private final WatchTargetMapper watchTargetMapper;
    private final AppResultMapper appResultMapper;

    public RankingServiceImpl(MetricValueQueryService metricValueQueryService, TypeBasicDataMapper typeBasicDataMapper,
            TargetGroupMapper targetGroupMapper, WatchTargetMapper watchTargetMapper, AppResultMapper appResultMapper) {
        this.metricValueQueryService = metricValueQueryService;
        this.typeBasicDataMapper = typeBasicDataMapper;
        this.targetGroupMapper = targetGroupMapper;
        this.watchTargetMapper = watchTargetMapper;
        this.appResultMapper = appResultMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public RankingResultView execute(RankingQueryRequest request) {
        validate(request);
        LinkedHashMap<String, TargetScopeTarget> targets = resolveTargets(request.targetType(), request.targetScope());
        List<RankingRuleRequest> rules = orderedRules(request.rankingRules());
        List<String> metrics = rules.stream().map(r -> r.metricCode().trim()).toList();
        Map<String, Map<String, Object>> matrix = new LinkedHashMap<>();
        targets.keySet().forEach(code -> {
            Map<String, Object> values = new LinkedHashMap<>();
            metrics.forEach(metric -> values.put(metric, null));
            matrix.put(code, values);
        });
        if (!targets.isEmpty()) {
            List<MetricValueView> values = metricValueQueryService.queryMetricValues(
                    new MetricQueryRequest(request.targetType(), new ArrayList<>(targets.keySet()), metrics, request.asOfTime()));
            values.forEach(value -> {
                Map<String, Object> row = matrix.get(value.getTargetCode());
                if (row != null && row.containsKey(value.getMetricCode())) row.put(value.getMetricCode(), value.getValue());
            });
        }
        List<RankingResultRowView> rows = targets.entrySet().stream().map(entry -> {
            RankingResultRowView row = new RankingResultRowView();
            row.setTargetCode(entry.getKey()); row.setTargetName(entry.getValue().getTargetName());
            row.setMetricValues(matrix.get(entry.getKey())); return row;
        }).collect(Collectors.toCollection(ArrayList::new));
        Comparator<RankingResultRowView> comparator = Comparator.comparing(RankingResultRowView::getTargetCode);
        for (int i = rules.size() - 1; i >= 0; i--) {
            RankingRuleRequest rule = rules.get(i);
            Comparator<RankingResultRowView> key = (left, right) -> compareNullable(
                    left.getMetricValues().get(rule.metricCode().trim()), right.getMetricValues().get(rule.metricCode().trim()),
                    "DESC".equals(normalizeDirection(rule.sortDirection())));
            comparator = key.thenComparing(comparator);
        }
        rows.sort(comparator);
        int topN = request.topN() == null ? 50 : request.topN();
        long total = rows.size();
        if (topN > 0 && rows.size() > topN) rows = new ArrayList<>(rows.subList(0, topN));
        for (int i = 0; i < rows.size(); i++) rows.get(i).setRank(i + 1L);
        RankingResultView result = new RankingResultView(); result.setTotal(total); result.setRows(rows); return result;
    }

    @Override @Transactional(rollbackFor = Exception.class)
    public RankingSnapshotView saveResult(RankingResultSaveRequest save) {
        validate(save.request());
        RankingSnapshotView snapshot = new RankingSnapshotView();
        snapshot.setResultId(java.util.UUID.randomUUID().toString().replace("-", ""));
        snapshot.setExecutedAt(LocalDateTime.now()); snapshot.setRequest(save.request()); snapshot.setResult(save.result());
        appResultMapper.insert(new AppResult().setResultId(snapshot.getResultId()).setResultType(ResultType.RANKING)
                .setRelatedObjectType("RANKING_SNAPSHOT").setResultSummaryJson(JSON.toJSONString(snapshot))
                .setSourceDataTime(save.request().asOfTime()).setCreatedAt(snapshot.getExecutedAt()));
        return snapshot;
    }

    @Override @Transactional(readOnly = true)
    public RankingSnapshotView getSavedResult(String resultId) {
        if (!StringUtils.hasText(resultId)) throw new BusinessException(40001, "排行结果标识不能为空");
        AppResult entity = appResultMapper.selectOne(new LambdaQueryWrapper<AppResult>().eq(AppResult::getResultId, resultId.trim())
                .eq(AppResult::getResultType, ResultType.RANKING));
        if (entity == null || !StringUtils.hasText(entity.getResultSummaryJson())) throw new BusinessException(40001, "排行结果不存在：" + resultId);
        return JSON.parseObject(entity.getResultSummaryJson(), RankingSnapshotView.class);
    }

    private void validate(RankingQueryRequest request) {
        if (request == null || request.targetType() == null || request.targetScope() == null || request.rankingRules() == null || request.rankingRules().isEmpty())
            throw new BusinessException(40001, "排行请求不完整");
        if (request.targetType() != AssetType.STOCK && request.targetType() != AssetType.INDEX) throw new BusinessException(40001, "当前仅支持股票和指数排行");
        if (request.targetScope().targetType() != request.targetType()) throw new BusinessException(40001, "请求标的类型与范围标的类型不一致");
        validateScopeProtocol(request.targetType(), request.targetScope());
        Set<String> codes = new LinkedHashSet<>();
        for (RankingRuleRequest rule : request.rankingRules()) {
            if (rule == null || !StringUtils.hasText(rule.metricCode())) throw new BusinessException(40001, "排行指标不能为空");
            if (!codes.add(rule.metricCode().trim())) throw new BusinessException(40001, "排行指标不能重复：" + rule.metricCode());
            if (!Set.of("ASC", "DESC").contains(normalizeDirection(rule.sortDirection()))) throw new BusinessException(40001, "排行方向只支持 ASC 或 DESC");
        }
        if (request.topN() != null && (request.topN() < 1 || request.topN() > 10000)) throw new BusinessException(40001, "Top N 必须在 1 到 10000 之间");
    }

    private List<RankingRuleRequest> orderedRules(List<RankingRuleRequest> rules) {
        // 请求列表顺序就是优先级；priority 仅作为持久化/展示字段，不能反过来改变语义。
        return List.copyOf(rules);
    }

    private int compareNullable(Object left, Object right, boolean descending) {
        if (left == null && right == null) return 0; if (left == null) return 1; if (right == null) return -1;
        int compared;
        try { compared = new BigDecimal(String.valueOf(left)).compareTo(new BigDecimal(String.valueOf(right))); }
        catch (RuntimeException e) { compared = String.valueOf(left).compareTo(String.valueOf(right)); }
        return descending ? -compared : compared;
    }

    private LinkedHashMap<String, TargetScopeTarget> resolveTargets(AssetType type, TargetScopeRequest scope) {
        String kind = scope.scopeType().trim().toUpperCase(); List<String> values = normalize(scope.scopeValues());
        List<TargetScopeTarget> targets;
        if ("WATCH_GROUP".equals(kind)) {
            targets = watchTargetMapper.selectList(new LambdaQueryWrapper<WatchTarget>().eq(WatchTarget::getTargetType, type.getValue()).in(WatchTarget::getGroupCode, values)
                    .orderByAsc(WatchTarget::getSortNo)).stream().map(w -> { TargetScopeTarget target = new TargetScopeTarget(); target.setTargetCode(w.getTargetCode()); target.setTargetName(w.getTargetName()); return target; }).toList();
        } else if (type == AssetType.STOCK && "ALL".equals(kind)) targets = typeBasicDataMapper.selectActiveStockTargets(List.of());
        else if (type == AssetType.STOCK && "BOARD".equals(kind)) targets = typeBasicDataMapper.selectActiveStockTargets(values);
        else if (type == AssetType.INDEX && "ALL".equals(kind)) targets = typeBasicDataMapper.selectIndexTargets("", List.of());
        else if (type == AssetType.INDEX && ("INDEX_TYPE".equals(kind) || "MARKET".equals(kind))) targets = typeBasicDataMapper.selectIndexTargets(kind, values);
        else throw new BusinessException(40001, "标的类型不支持该范围类型：" + kind);
        return targets.stream().filter(t -> StringUtils.hasText(t.getTargetCode())).collect(Collectors.toMap(TargetScopeTarget::getTargetCode, t -> t, (a,b) -> a, LinkedHashMap::new));
    }

    private void validateScopeProtocol(AssetType type, TargetScopeRequest scope) {
        String kind = scope.scopeType().trim().toUpperCase(); Set<String> supported = type == AssetType.STOCK ? Set.of("ALL", "BOARD", "WATCH_GROUP") : Set.of("ALL", "INDEX_TYPE", "MARKET", "WATCH_GROUP");
        if (!supported.contains(kind)) throw new BusinessException(40001, "不支持的范围类型：" + kind);
        List<String> values = normalize(scope.scopeValues()); if ("ALL".equals(kind) && !values.isEmpty()) throw new BusinessException(40001, "ALL 范围不允许传范围值");
        if (!"ALL".equals(kind) && values.isEmpty()) throw new BusinessException(40001, "范围值不能为空");
    }
    private List<String> normalize(Collection<String> values) { return values == null ? List.of() : values.stream().filter(StringUtils::hasText).map(String::trim).distinct().toList(); }
    private String normalizeDirection(String value) { return StringUtils.hasText(value) ? value.trim().toUpperCase() : "DESC"; }
}
