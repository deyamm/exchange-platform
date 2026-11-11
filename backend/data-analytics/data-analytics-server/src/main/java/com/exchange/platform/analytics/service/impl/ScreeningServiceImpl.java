package com.exchange.platform.analytics.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exchange.platform.analytics.api.enums.CommonStatus;
import com.exchange.platform.analytics.api.request.MetricConditionGroupRequest;
import com.exchange.platform.analytics.api.request.MetricConditionRequest;
import com.exchange.platform.analytics.api.request.MetricQueryRequest;
import com.exchange.platform.analytics.api.request.ScreeningQueryRequest;
import com.exchange.platform.analytics.api.request.ScreeningResultSaveRequest;
import com.exchange.platform.analytics.api.request.TargetScopeRequest;
import com.exchange.platform.analytics.api.response.MetricValueView;
import com.exchange.platform.analytics.api.response.ScreeningResultRowView;
import com.exchange.platform.analytics.api.response.ScreeningResultView;
import com.exchange.platform.analytics.api.response.ScreeningSnapshotView;
import com.exchange.platform.analytics.api.response.TargetScopeOptionView;
import com.exchange.platform.analytics.api.response.TargetScopeOptionsView;
import com.exchange.platform.analytics.entity.TargetGroup;
import com.exchange.platform.analytics.entity.WatchTarget;
import com.exchange.platform.analytics.entity.AppResult;
import com.exchange.platform.analytics.api.enums.ResultType;
import com.exchange.platform.analytics.mapper.AppResultMapper;
import com.exchange.platform.analytics.mapper.TargetGroupMapper;
import com.exchange.platform.analytics.mapper.TypeBasicDataMapper;
import com.exchange.platform.analytics.mapper.WatchTargetMapper;
import com.exchange.platform.analytics.service.MetricValueQueryService;
import com.exchange.platform.analytics.service.ScreeningService;
import com.exchange.platform.analytics.service.support.TargetScopeTarget;
import com.exchange.platform.common.core.enums.AssetType;
import com.exchange.platform.common.core.exception.BusinessException;

/**
 * 条件筛选计算层。
 *
 * <p>本类只负责范围解析、二维指标矩阵和条件表达式。指标实际读取必须经由
 * {@link MetricValueQueryService}，因此不会让前端携带 Dataset/字段/SQL，也不会在这里
 * 直接访问物理指标表或执行跨 Dataset JOIN。</p>
 */
@Service
public class ScreeningServiceImpl implements ScreeningService {
    private static final Set<String> NUMERIC_OPERATORS = Set.of("GT", "GTE", "LT", "LTE", "BETWEEN");
    private static final Set<String> VALUE_OPERATORS = Set.of("EQ", "NE", "GT", "GTE", "LT", "LTE", "BETWEEN", "IN", "NOT_IN");
    private static final Set<String> NULL_OPERATORS = Set.of("IS_NULL", "IS_NOT_NULL");

    private final MetricValueQueryService metricValueQueryService;
    private final TypeBasicDataMapper typeBasicDataMapper;
    private final TargetGroupMapper targetGroupMapper;
    private final WatchTargetMapper watchTargetMapper;
    private final AppResultMapper appResultMapper;

    public ScreeningServiceImpl(MetricValueQueryService metricValueQueryService,
            TypeBasicDataMapper typeBasicDataMapper, TargetGroupMapper targetGroupMapper,
            WatchTargetMapper watchTargetMapper, AppResultMapper appResultMapper) {
        this.metricValueQueryService = metricValueQueryService;
        this.typeBasicDataMapper = typeBasicDataMapper;
        this.targetGroupMapper = targetGroupMapper;
        this.watchTargetMapper = watchTargetMapper;
        this.appResultMapper = appResultMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public ScreeningResultView execute(ScreeningQueryRequest request) {
        validateRequest(request);
        LinkedHashMap<String, TargetScopeTarget> targets = resolveTargets(request.targetType(), request.targetScope());
        List<String> metricCodes = collectMetricCodes(request.conditionGroups());

        // 先为每个“标的 × 指标”补 null。统一取数接口没有返回记录也必须被解释为 missing，不能变成 0。
        Map<String, Map<String, Object>> matrix = new LinkedHashMap<>();
        targets.keySet().forEach(targetCode -> {
            Map<String, Object> values = new LinkedHashMap<>();
            metricCodes.forEach(metricCode -> values.put(metricCode, null));
            matrix.put(targetCode, values);
        });
        if (!targets.isEmpty()) {
            List<MetricValueView> metricValues = metricValueQueryService.queryMetricValues(new MetricQueryRequest(
                    request.targetType(), new ArrayList<>(targets.keySet()), metricCodes, request.asOfTime()));
            for (MetricValueView metricValue : metricValues) {
                Map<String, Object> targetValues = matrix.get(metricValue.getTargetCode());
                if (targetValues != null && targetValues.containsKey(metricValue.getMetricCode())) {
                    targetValues.put(metricValue.getMetricCode(), metricValue.getValue());
                }
            }
        }

        List<ScreeningResultRowView> rows = new ArrayList<>();
        for (Map.Entry<String, TargetScopeTarget> entry : targets.entrySet()) {
            Map<String, Object> values = matrix.get(entry.getKey());
            if (matchesAllGroups(values, request.conditionGroups(), request.groupOperator())) {
                ScreeningResultRowView row = new ScreeningResultRowView();
                row.setTargetCode(entry.getKey());
                row.setTargetName(entry.getValue().getTargetName());
                row.setMetricValues(values);
                rows.add(row);
            }
        }
        sortRows(rows, request.sortMetricCode(), request.sortDirection(), metricCodes);

        ScreeningResultView result = new ScreeningResultView();
        result.setTotal(rows.size());
        result.setRows(rows);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public TargetScopeOptionsView getScopeOptions(AssetType targetType) {
        assertSupportedTargetType(targetType);
        TargetScopeOptionsView view = new TargetScopeOptionsView();
        view.setWatchGroup(targetGroupMapper.selectList(new LambdaQueryWrapper<TargetGroup>()
                .eq(TargetGroup::getStatus, CommonStatus.ENABLED)
                .orderByAsc(TargetGroup::getSortNo)).stream()
                .map(group -> new TargetScopeOptionView(group.getGroupCode(), group.getGroupName()))
                .collect(Collectors.toList()));
        if (targetType == AssetType.STOCK) {
            view.setBoard(toOptions(typeBasicDataMapper.selectStockBoardOptions()));
            view.setIndexType(List.of());
            view.setMarket(List.of());
        } else {
            view.setBoard(List.of());
            view.setIndexType(toOptions(typeBasicDataMapper.selectIndexTypeOptions()));
            view.setMarket(toOptions(typeBasicDataMapper.selectIndexMarketOptions()));
        }
        return view;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScreeningSnapshotView saveResult(ScreeningResultSaveRequest saveRequest) {
        // 保存前复用完整请求校验，防止调用方绕过范围/操作符的协议约束写入不可回放数据。
        validateRequest(saveRequest.request());
        LocalDateTime executedAt = LocalDateTime.now();
        ScreeningSnapshotView snapshot = new ScreeningSnapshotView();
        snapshot.setResultId(java.util.UUID.randomUUID().toString().replace("-", ""));
        snapshot.setExecutedAt(executedAt);
        snapshot.setRequest(saveRequest.request());
        snapshot.setResult(saveRequest.result());

        AppResult entity = new AppResult();
        entity.setResultId(snapshot.getResultId())
                .setResultType(ResultType.FILTERING)
                .setRelatedObjectType("SCREENING_SNAPSHOT")
                .setResultSummaryJson(JSON.toJSONString(snapshot))
                // asOfTime 是用户声明的历史边界；null 明确表示当时按“最新”执行。
                .setSourceDataTime(saveRequest.request().asOfTime())
                .setCreatedAt(executedAt);
        appResultMapper.insert(entity);
        return snapshot;
    }

    @Override
    @Transactional(readOnly = true)
    public ScreeningSnapshotView getSavedResult(String resultId) {
        if (!StringUtils.hasText(resultId)) {
            throw new BusinessException(40001, "筛选结果标识不能为空");
        }
        AppResult entity = appResultMapper.selectOne(new LambdaQueryWrapper<AppResult>()
                .eq(AppResult::getResultId, resultId.trim())
                .eq(AppResult::getResultType, ResultType.FILTERING));
        if (entity == null || !StringUtils.hasText(entity.getResultSummaryJson())) {
            throw new BusinessException(40001, "筛选结果快照不存在：" + resultId);
        }
        return JSON.parseObject(entity.getResultSummaryJson(), ScreeningSnapshotView.class);
    }

    private LinkedHashMap<String, TargetScopeTarget> resolveTargets(AssetType targetType, TargetScopeRequest scope) {
        String scopeType = normalized(scope.scopeType());
        List<String> values = normalizeCodes(scope.scopeValues(), "范围值");
        List<TargetScopeTarget> resolved;
        if ("WATCH_GROUP".equals(scopeType)) {
            resolved = resolveWatchGroupTargets(targetType, values);
        } else if (targetType == AssetType.STOCK && "ALL".equals(scopeType)) {
            assertNoScopeValues(values, scopeType);
            resolved = typeBasicDataMapper.selectActiveStockTargets(List.of());
        } else if (targetType == AssetType.STOCK && "BOARD".equals(scopeType)) {
            requireScopeValues(values, scopeType);
            // 基础资料当前提供的可用行业字段作为板块主数据来源；不会在代码中伪造板块枚举。
            resolved = typeBasicDataMapper.selectActiveStockTargets(values);
        } else if (targetType == AssetType.INDEX && "ALL".equals(scopeType)) {
            assertNoScopeValues(values, scopeType);
            resolved = typeBasicDataMapper.selectIndexTargets("", List.of());
        } else if (targetType == AssetType.INDEX && "INDEX_TYPE".equals(scopeType)) {
            requireScopeValues(values, scopeType);
            resolved = typeBasicDataMapper.selectIndexTargets("INDEX_TYPE", values);
        } else if (targetType == AssetType.INDEX && "MARKET".equals(scopeType)) {
            requireScopeValues(values, scopeType);
            resolved = typeBasicDataMapper.selectIndexTargets("MARKET", values);
        } else {
            throw new BusinessException(40001, "标的类型不支持该范围类型：" + scopeType);
        }
        return resolved.stream().filter(target -> StringUtils.hasText(target.getTargetCode()))
                .collect(Collectors.toMap(TargetScopeTarget::getTargetCode, target -> target, (first, ignored) -> first,
                        LinkedHashMap::new));
    }

    private List<TargetScopeTarget> resolveWatchGroupTargets(AssetType targetType, List<String> groupCodes) {
        requireScopeValues(groupCodes, "WATCH_GROUP");
        return watchTargetMapper.selectList(new LambdaQueryWrapper<WatchTarget>()
                .eq(WatchTarget::getTargetType, targetType.getValue())
                .in(WatchTarget::getGroupCode, groupCodes)
                .orderByAsc(WatchTarget::getSortNo))
                .stream().map(watch -> {
                    TargetScopeTarget target = new TargetScopeTarget();
                    target.setTargetCode(watch.getTargetCode());
                    target.setTargetName(watch.getTargetName());
                    return target;
                }).collect(Collectors.toList());
    }

    private void validateRequest(ScreeningQueryRequest request) {
        if (request == null || request.targetType() == null || request.targetScope() == null) {
            throw new BusinessException(40001, "筛选请求不完整");
        }
        assertSupportedTargetType(request.targetType());
        if (request.targetScope().targetType() != request.targetType()) {
            throw new BusinessException(40001, "请求标的类型与范围标的类型不一致");
        }
        validateScopeProtocol(request.targetType(), request.targetScope());
        if (request.conditionGroups() == null || request.conditionGroups().isEmpty()) {
            throw new BusinessException(40001, "至少需要一个条件组");
        }
        normalizeLogic(request.groupOperator(), "条件组连接符");
        for (MetricConditionGroupRequest group : request.conditionGroups()) {
            if (group == null || group.conditions() == null || group.conditions().isEmpty()) {
                throw new BusinessException(40001, "条件组至少需要一个条件");
            }
            normalizeLogic(group.operator(), "条件组连接符");
            for (MetricConditionRequest condition : group.conditions()) {
                validateCondition(condition);
            }
        }
        if (StringUtils.hasText(request.sortMetricCode()) && !collectMetricCodes(request.conditionGroups()).contains(request.sortMetricCode().trim())) {
            throw new BusinessException(40001, "排序指标必须属于本次筛选指标");
        }
        if (StringUtils.hasText(request.sortDirection()) && !Set.of("ASC", "DESC").contains(normalized(request.sortDirection()))) {
            throw new BusinessException(40001, "排序方向只支持 ASC 或 DESC");
        }
    }

    /** 在不读取基础资料的情况下校验范围协议，保存快照时也能拒绝非法 scopeType。 */
    private void validateScopeProtocol(AssetType targetType, TargetScopeRequest scope) {
        String scopeType = normalized(scope.scopeType());
        Set<String> supported = targetType == AssetType.STOCK
                ? Set.of("ALL", "BOARD", "WATCH_GROUP")
                : Set.of("ALL", "INDEX_TYPE", "MARKET", "WATCH_GROUP");
        if (!supported.contains(scopeType)) {
            throw new BusinessException(40001, "标的类型不支持该范围类型：" + scope.scopeType());
        }
        List<String> values = normalizeCodes(scope.scopeValues(), "范围值");
        if ("ALL".equals(scopeType) && !values.isEmpty()) {
            throw new BusinessException(40001, "ALL 范围不允许传范围值");
        }
        if (!"ALL".equals(scopeType) && values.isEmpty()) {
            throw new BusinessException(40001, scopeType + " 至少需要一个范围值");
        }
    }

    private void validateCondition(MetricConditionRequest condition) {
        String operator = normalized(condition.operator());
        if (!VALUE_OPERATORS.contains(operator) && !NULL_OPERATORS.contains(operator)) {
            throw new BusinessException(40001, "不支持的条件操作符：" + condition.operator());
        }
        if (NULL_OPERATORS.contains(operator)) {
            return;
        }
        if (condition.value() == null) {
            throw new BusinessException(40001, "操作符 " + operator + " 必须提供比较值");
        }
        if ("BETWEEN".equals(operator) && valueList(condition.value()).size() != 2) {
            throw new BusinessException(40001, "BETWEEN 必须提供两个边界值");
        }
        if (("IN".equals(operator) || "NOT_IN".equals(operator)) && valueList(condition.value()).isEmpty()) {
            throw new BusinessException(40001, operator + " 必须提供至少一个比较值");
        }
        if (NUMERIC_OPERATORS.contains(operator)) {
            List<Object> numbers = "BETWEEN".equals(operator) ? valueList(condition.value()) : List.of(condition.value());
            numbers.forEach(value -> toNumber(value, "比较值必须是数值"));
        }
    }

    private boolean matchesAllGroups(Map<String, Object> values, List<MetricConditionGroupRequest> groups, String groupOperator) {
        boolean useAnd = "AND".equals(normalizeLogic(groupOperator, "条件组连接符"));
        boolean result = useAnd;
        for (MetricConditionGroupRequest group : groups) {
            boolean groupMatched = matchesGroup(values, group);
            result = useAnd ? result && groupMatched : result || groupMatched;
        }
        return result;
    }

    private boolean matchesGroup(Map<String, Object> values, MetricConditionGroupRequest group) {
        boolean useAnd = "AND".equals(normalizeLogic(group.operator(), "条件组连接符"));
        boolean result = useAnd;
        for (MetricConditionRequest condition : group.conditions()) {
            boolean conditionMatched = matchesCondition(values.get(condition.metricCode().trim()), condition);
            result = useAnd ? result && conditionMatched : result || conditionMatched;
        }
        return result;
    }

    /** missing 对普通比较永远不命中；只有 IS_NULL 将 null/missing 视为命中。 */
    private boolean matchesCondition(Object actual, MetricConditionRequest condition) {
        String operator = normalized(condition.operator());
        if ("IS_NULL".equals(operator)) return actual == null;
        if ("IS_NOT_NULL".equals(operator)) return actual != null;
        if (actual == null) return false;
        if ("IN".equals(operator) || "NOT_IN".equals(operator)) {
            boolean contains = valueList(condition.value()).stream().anyMatch(expected -> valuesEqual(actual, expected));
            return "IN".equals(operator) ? contains : !contains;
        }
        if ("BETWEEN".equals(operator)) {
            List<Object> boundaries = valueList(condition.value());
            BigDecimal number = toNumber(actual, "指标值不是数值，不能执行范围比较");
            return number.compareTo(toNumber(boundaries.get(0), "比较值必须是数值")) >= 0
                    && number.compareTo(toNumber(boundaries.get(1), "比较值必须是数值")) <= 0;
        }
        if (NUMERIC_OPERATORS.contains(operator)) {
            int comparison = toNumber(actual, "指标值不是数值，不能执行数值比较")
                    .compareTo(toNumber(condition.value(), "比较值必须是数值"));
            return switch (operator) {
                case "GT" -> comparison > 0;
                case "GTE" -> comparison >= 0;
                case "LT" -> comparison < 0;
                case "LTE" -> comparison <= 0;
                default -> false;
            };
        }
        boolean equal = valuesEqual(actual, condition.value());
        return "EQ".equals(operator) ? equal : !equal;
    }

    private void sortRows(List<ScreeningResultRowView> rows, String sortMetricCode, String sortDirection, List<String> metricCodes) {
        if (!StringUtils.hasText(sortMetricCode)) return;
        String metricCode = sortMetricCode.trim();
        if (!metricCodes.contains(metricCode)) return;
        boolean descending = "DESC".equals(normalized(sortDirection));
        Comparator<ScreeningResultRowView> comparator = (left, right) -> compareNullable(
                left.getMetricValues().get(metricCode), right.getMetricValues().get(metricCode), descending);
        // 相同指标值按代码稳定排序，使服务端返回顺序可复现。
        rows.sort(comparator.thenComparing(ScreeningResultRowView::getTargetCode, Comparator.nullsLast(String::compareTo)));
    }

    private int compareNullable(Object left, Object right, boolean descending) {
        if (left == null && right == null) return 0;
        if (left == null) return 1; // 无论升降序，缺失值均置于结果末尾。
        if (right == null) return -1;
        int compared;
        try {
            compared = toNumber(left, "").compareTo(toNumber(right, ""));
        } catch (BusinessException ignored) {
            compared = String.valueOf(left).compareTo(String.valueOf(right));
        }
        return descending ? -compared : compared;
    }

    private List<String> collectMetricCodes(List<MetricConditionGroupRequest> groups) {
        LinkedHashSet<String> codes = new LinkedHashSet<>();
        groups.forEach(group -> group.conditions().forEach(condition -> codes.add(condition.metricCode().trim())));
        return new ArrayList<>(codes);
    }

    private List<TargetScopeOptionView> toOptions(List<TargetScopeTarget> targets) {
        return targets.stream().filter(target -> StringUtils.hasText(target.getTargetCode()))
                .map(target -> new TargetScopeOptionView(target.getTargetCode(), target.getTargetName()))
                .collect(Collectors.toList());
    }

    private List<String> normalizeCodes(Collection<String> rawValues, String label) {
        if (rawValues == null) return List.of();
        return rawValues.stream().filter(StringUtils::hasText).map(String::trim).distinct().collect(Collectors.toList());
    }

    private List<Object> valueList(Object rawValue) {
        if (rawValue instanceof Collection<?> collection) return new ArrayList<>(collection);
        return rawValue == null ? List.of() : List.of(rawValue);
    }

    private boolean valuesEqual(Object left, Object right) {
        if (left == null || right == null) return Objects.equals(left, right);
        try {
            return toNumber(left, "").compareTo(toNumber(right, "")) == 0;
        } catch (BusinessException ignored) {
            return String.valueOf(left).equals(String.valueOf(right));
        }
    }

    private BigDecimal toNumber(Object value, String message) {
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException ex) {
            throw new BusinessException(40001, message);
        }
    }

    private String normalizeLogic(String value, String label) {
        String normalized = normalized(value);
        if (!"AND".equals(normalized) && !"OR".equals(normalized)) {
            throw new BusinessException(40001, label + "只支持 AND 或 OR");
        }
        return normalized;
    }

    private String normalized(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private void requireScopeValues(List<String> values, String scopeType) {
        if (values.isEmpty()) throw new BusinessException(40001, scopeType + " 至少需要一个范围值");
    }

    private void assertNoScopeValues(List<String> values, String scopeType) {
        if (!values.isEmpty()) throw new BusinessException(40001, scopeType + " 不允许传范围值");
    }

    private void assertSupportedTargetType(AssetType targetType) {
        if (targetType != AssetType.STOCK && targetType != AssetType.INDEX) {
            throw new BusinessException(40001, "条件筛选当前仅支持股票和指数");
        }
    }
}
