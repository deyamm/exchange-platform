<template>
    <div class="screening-page layout-pd">
        <div class="page-heading">
            <div>
                <h2>条件筛选</h2>
                <p>按截至时间取得每个指标的最新有效值，再由服务端统一执行条件表达式。</p>
            </div>
            <el-radio-group v-model="targetType" @change="changeTargetType">
                <el-radio-button label="STOCK">股票</el-radio-button>
                <el-radio-button label="INDEX">指数</el-radio-button>
            </el-radio-group>
        </div>
        <div class="screening-layout">
            <el-card class="configuration-card" shadow="never">
                <template #header>
                    <span>筛选配置</span>
                </template>
                <section class="configuration-section scope-section" @click="scopeDialogVisible = true">
                    <TargetScopeSummary :scope="scope" :options="scopeOptions" />
                    <el-button link type="primary">修改范围</el-button>
                </section>
                <section class="configuration-section">
                    <div class="section-title">截至时间</div>
                    <AsOfTimePicker v-model="asOfTime" label="" />
                </section>
                <section class="configuration-section">
                    <div class="conditions-heading">
                        <div class="section-title">筛选条件</div>
                        <el-select v-model="groupOperator" size="small" aria-label="条件组之间的关系">
                            <el-option label="组间 AND" value="AND" />
                            <el-option label="组间 OR" value="OR" />
                        </el-select>
                    </div>
                    <div v-for="(group, groupIndex) in conditionGroups" :key="group.id" class="condition-group">
                        <div class="group-toolbar">
                            <span>条件组 {{ groupIndex + 1 }}</span>
                            <div class="group-actions">
                                <el-select v-model="group.operator" size="small" class="logic-select">
                                    <el-option label="AND" value="AND" />
                                    <el-option label="OR" value="OR" />
                                </el-select>
                                <el-button v-if="conditionGroups.length > 1" link type="danger"
                                    @click="removeGroup(groupIndex)">
                                    删除组
                                </el-button>
                            </div>
                        </div>
                        <div v-if="!group.conditions.length" class="empty-group">请添加指标条件</div>
                        <div v-for="(condition, conditionIndex) in group.conditions" :key="condition.id"
                            class="condition-row">
                            <div class="metric-name">
                                <span>{{ metricInfo(condition.metricCode)?.metricName || condition.metricCode }}</span>
                                <small>{{ condition.metricCode }}</small>
                            </div>
                            <el-select v-model="condition.operator" class="operator-select"
                                @change="resetConditionValue(condition)">
                                <el-option v-for="operator in operatorOptions" :key="operator.value"
                                    :label="operator.label" :value="operator.value" />
                            </el-select>
                            <template v-if="condition.operator === 'BETWEEN'">
                                <el-input v-model="condition.value" class="value-input" placeholder="最小值" />
                                <span class="between-separator">至</span>
                                <el-input v-model="condition.secondValue" class="value-input" placeholder="最大值" />
                            </template>
                            <el-input v-else-if="!isNullOperator(condition.operator)" v-model="condition.value"
                                class="value-input" :placeholder="valuePlaceholder(condition.operator)" />
                            <span v-else class="null-description">
                                {{ condition.operator === 'IS_NULL' ? '指标缺失时命中' : '指标存在时命中' }}
                            </span>
                            <span class="metric-unit">{{ metricInfo(condition.metricCode)?.unit || '' }}</span>
                            <el-button link type="danger" @click="removeCondition(groupIndex, conditionIndex)">
                                移除
                            </el-button>
                        </div>
                    </div>
                    <div class="condition-actions">
                        <el-button @click="metricDialogVisible = true">+ 添加指标</el-button>
                        <el-button @click="addGroup">+ 添加条件组</el-button>
                    </div>
                </section>
                <div class="configuration-footer">
                    <el-button type="primary" :loading="loading" @click="executeScreening">执行筛选</el-button>
                </div>
            </el-card>
            <el-card class="result-card" shadow="never">
                <template #header>
                    <div class="result-header">
                        <span>
                            筛选结果
                            <em v-if="hasExecuted">命中 {{ result.total }} 个标的</em>
                        </span>
                        <span>
                            <el-button :disabled="!hasExecuted" @click="saveResultSnapshot">保存结果</el-button>
                            <el-button type="primary" :disabled="!selectedTargetCodes.length" @click="goCompare">进入对比</el-button>
                        </span>
                    </div>
                </template>
                <el-alert v-if="!hasExecuted" title="设置范围与条件后执行筛选" type="info" :closable="false" show-icon />
                <el-table v-else :data="result.rows" border stripe class="result-table" @sort-change="handleSortChange" @selection-change="selectedTargetCodes = $event.map((row: any) => row.targetCode)">
                    <el-table-column type="selection" width="48" />
                    <el-table-column prop="targetCode" label="代码" min-width="120" />
                    <el-table-column prop="targetName" label="名称" min-width="130" />
                    <el-table-column v-for="metricCode in usedMetricCodes" :key="metricCode"
                        :prop="`metricValues.${metricCode}`" :label="metricColumnLabel(metricCode)" min-width="130"
                        sortable="custom">
                        <template #default="{ row }">
                            {{ displayMetricValue(row.metricValues[metricCode], metricInfo(metricCode)?.unit) }}
                        </template>
                    </el-table-column>
                </el-table>
            </el-card>
        </div>
        <TargetScopeSelector 
            :open="scopeDialogVisible" 
            :target-type="targetType" 
            :value="scope"
            :board-options="scopeOptions.board" 
            :watch-group-options="scopeOptions.watchGroup"
            :index-type-options="scopeOptions.indexType" 
            :index-market-options="scopeOptions.market"
            @confirm="confirmScope" 
            @cancel="scopeDialogVisible = false" 
        />
        <MetricSelector 
            :open="metricDialogVisible" 
            :target-type="targetType" 
            selection-mode="MULTIPLE"
            :selected-metric-codes="usedMetricCodes" 
            @confirm="addMetrics" 
            @cancel="metricDialogVisible = false" 
        />
    </div>
</template>

<script setup lang="ts" name="pagesFiltering">
import { computed, onMounted, ref } from 'vue';
import { router } from '/@/router';
import { ElMessage } from 'element-plus';
import AsOfTimePicker from '/@/components/common/AsOfTimePicker.vue';
import MetricSelector from '/@/components/common/MetricSelector.vue';
import TargetScopeSelector from '/@/components/common/TargetScopeSelector.vue';
import TargetScopeSummary from '/@/components/common/TargetScopeSummary.vue';
import { querySelectableMetrics, type SelectableMetric } from '/@/api/commonSelectorApi';
import {
    queryScreeningResult,
    queryScreeningScopeOptions,
    saveScreeningResult
} from '/@/api/screeningApi';
import {
    emptyTargetScope,
    type TargetScope,
    type TargetScopeOptions,
    type TargetType
} from '/@/types/targetScope';
import type {
    LogicOperator,
    MetricConditionOperator,
    ScreeningQueryRequest,
    ScreeningResult,
    SortDirection
} from '/@/types/screening';

interface EditableCondition {
    id: string;
    metricCode: string;
    operator: MetricConditionOperator;
    value: string;
    secondValue: string;
}

interface EditableGroup {
    id: string;
    operator: LogicOperator;
    conditions: EditableCondition[];
}

const targetType = ref<TargetType>('STOCK');
const scope = ref<TargetScope>(emptyTargetScope(targetType.value));
const scopeOptions = ref<TargetScopeOptions>({
    board: [],
    watchGroup: [],
    indexType: [],
    market: []
});
const asOfTime = ref<string | null>(null);
const conditionGroups = ref<EditableGroup[]>([newGroup()]);
const groupOperator = ref<LogicOperator>('AND');
const metricCatalog = ref<Record<string, SelectableMetric>>({});
const result = ref<ScreeningResult>({ total: 0, rows: [] });
const hasExecuted = ref(false);
const loading = ref(false);
const scopeDialogVisible = ref(false);
const metricDialogVisible = ref(false);
const sortMetricCode = ref<string>();
const sortDirection = ref<SortDirection>();
const selectedTargetCodes = ref<string[]>([]);

const operatorOptions: Array<{ label: string; value: MetricConditionOperator }> = [
    { label: '= 等于', value: 'EQ' },
    { label: '≠ 不等于', value: 'NE' },
    { label: '> 大于', value: 'GT' },
    { label: '≥ 大于等于', value: 'GTE' },
    { label: '< 小于', value: 'LT' },
    { label: '≤ 小于等于', value: 'LTE' },
    { label: '介于', value: 'BETWEEN' },
    { label: '属于', value: 'IN' },
    { label: '不属于', value: 'NOT_IN' },
    { label: '为空', value: 'IS_NULL' },
    { label: '不为空', value: 'IS_NOT_NULL' },
];

const usedMetricCodes = computed(() => [
    ...new Set(
        conditionGroups.value.flatMap((group) =>
            group.conditions.map((condition) => condition.metricCode)
        )
    )
]);

onMounted(loadScopeOptions);

function newGroup(): EditableGroup {
    return { id: crypto.randomUUID(), operator: 'AND', conditions: [] };
}

async function loadScopeOptions() {
    scopeOptions.value = await queryScreeningScopeOptions(targetType.value);
}

async function changeTargetType() {
    // targetType 是页面上下文；切换时清空范围、指标和结果，绝不让旧类型条件继续参与新类型筛选。
    scope.value = emptyTargetScope(targetType.value);
    scopeOptions.value = {
        board: [],
        watchGroup: [],
        indexType: [],
        market: []
    };
    conditionGroups.value = [newGroup()];
    metricCatalog.value = {};
    result.value = { total: 0, rows: [] };
    hasExecuted.value = false;
    selectedTargetCodes.value = [];
    sortMetricCode.value = undefined;
    sortDirection.value = undefined;
    await loadScopeOptions();
}

function confirmScope(nextScope: TargetScope) {
    scope.value = nextScope;
    scopeDialogVisible.value = false;
    hasExecuted.value = false;
}

function addGroup() {
    conditionGroups.value.push(newGroup());
    hasExecuted.value = false;
}

function removeGroup(index: number) {
    conditionGroups.value.splice(index, 1);
    hasExecuted.value = false;
}

function removeCondition(groupIndex: number, conditionIndex: number) {
    conditionGroups.value[groupIndex].conditions.splice(conditionIndex, 1);
    hasExecuted.value = false;
}

async function addMetrics(selectedCodes: string[]) {
    metricDialogVisible.value = false;

    // MetricSelector 的确认值是“最终选择集合”，因此先移除用户在弹窗中取消的指标，
    // 再为新增指标创建条件项，避免条件区残留已经取消的指标。
    const selectedSet = new Set(selectedCodes);
    conditionGroups.value.forEach((group) => {
        group.conditions = group.conditions.filter((condition) =>
            selectedSet.has(condition.metricCode)
        );
    });

    const newCodes = selectedCodes.filter(
        (code) => !usedMetricCodes.value.includes(code)
    );
    if (!newCodes.length) {
        hasExecuted.value = false;
        return;
    }

    const page = await querySelectableMetrics({
        targetType: targetType.value,
        pageNo: 1,
        pageSize: 200
    });
    const selected = page.records.filter((metric) =>
        newCodes.includes(metric.metricCode)
    );

    selected.forEach((metric) => {
        metricCatalog.value[metric.metricCode] = metric;
    });

    const targetGroup = conditionGroups.value[conditionGroups.value.length - 1];
    selected.forEach((metric) =>
        targetGroup.conditions.push({
            id: crypto.randomUUID(),
            metricCode: metric.metricCode,
            operator: 'LT',
            value: '',
            secondValue: ''
        })
    );

    hasExecuted.value = false;
}

function metricInfo(metricCode: string) {
    return metricCatalog.value[metricCode];
}

function metricColumnLabel(metricCode: string) {
    const metric = metricInfo(metricCode);
    const sortLabel =
        sortMetricCode.value === metricCode && sortDirection.value
            ? ` (${sortDirection.value})`
            : '';

    return `${metric?.metricName || metricCode}${metric?.unit ? ` (${metric.unit})` : ''}${sortLabel}`;
}

function isNullOperator(operator: MetricConditionOperator) {
    return operator === 'IS_NULL' || operator === 'IS_NOT_NULL';
}

function valuePlaceholder(operator: MetricConditionOperator) {
    return operator === 'IN' || operator === 'NOT_IN'
        ? '多个值用逗号分隔'
        : '输入比较值';
}

function resetConditionValue(condition: EditableCondition) {
    condition.value = '';
    condition.secondValue = '';
    hasExecuted.value = false;
}

function toComparableValue(value: string): string | number {
    const trimmed = value.trim();
    return trimmed !== '' && Number.isFinite(Number(trimmed))
        ? Number(trimmed)
        : trimmed;
}

function toRequest(): ScreeningQueryRequest | undefined {
    if (!conditionGroups.value.every((group) => group.conditions.length)) {
        ElMessage.warning('每个条件组至少需要一个指标条件');
        return undefined;
    }

    try {
        const groups = conditionGroups.value.map((group) => ({
            operator: group.operator,
            conditions: group.conditions.map((condition) => {
                let value: unknown;

                if (condition.operator === 'BETWEEN') {
                    if (!condition.value.trim() || !condition.secondValue.trim())
                        throw new Error('BETWEEN 需要填写两个边界值');

                    value = [
                        toComparableValue(condition.value),
                        toComparableValue(condition.secondValue)
                    ];
                } else if (
                    condition.operator === 'IN' ||
                    condition.operator === 'NOT_IN'
                ) {
                    value = condition.value
                        .split(',')
                        .map((item) => item.trim())
                        .filter(Boolean)
                        .map(toComparableValue);

                    if (!(value as unknown[]).length)
                        throw new Error(`${condition.operator} 至少需要一个比较值`);
                } else if (!isNullOperator(condition.operator)) {
                    if (!condition.value.trim())
                        throw new Error('请填写比较值');

                    value = toComparableValue(condition.value);
                }

                return {
                    metricCode: condition.metricCode,
                    operator: condition.operator,
                    ...(value !== undefined ? { value } : {})
                };
            })
        }));

        return {
            targetType: targetType.value,
            targetScope: scope.value,
            asOfTime: asOfTime.value,
            conditionGroups: groups,
            groupOperator: groupOperator.value,
            ...(sortMetricCode.value && sortDirection.value
                ? {
                    sortMetricCode: sortMetricCode.value,
                    sortDirection: sortDirection.value
                }
                : {})
        };
    } catch (error) {
        ElMessage.warning(error instanceof Error ? error.message : '筛选条件不完整');
        return undefined;
    }
}

async function executeScreening() {
    const request = toRequest();
    if (!request) return;

    loading.value = true;
    try {
        result.value = await queryScreeningResult(request);
        hasExecuted.value = true;
        selectedTargetCodes.value = [];
    } finally {
        loading.value = false;
    }
}

function handleSortChange({
    prop,
    order
}: {
    prop?: string;
    order?: 'ascending' | 'descending' | null
}) {
    const metricCode = prop?.startsWith('metricValues.')
        ? prop.slice('metricValues.'.length)
        : undefined;

    if (!metricCode) return;

    sortMetricCode.value = order ? metricCode : undefined;
    sortDirection.value =
        order === 'ascending'
            ? 'ASC'
            : order === 'descending'
                ? 'DESC'
                : undefined;

    // 后端已拥有完整命中集，点击列头后重新执行，避免只排序当前页造成错误全局顺序。
    executeScreening();
}

function displayMetricValue(value: unknown, unit?: string) {
    if (value === null || value === undefined || value === '') return '-';
    return `${value}${unit ? ` ${unit}` : ''}`;
}

async function saveResultSnapshot() {
    const request = toRequest();
    if (!request) return;

    // 保存已经执行的返回值而非仅保存条件；回看快照时不会用“当前最新值”重新计算历史结果。
    const snapshot = await saveScreeningResult({
        request,
        result: result.value
    });
    ElMessage.success(`已保存筛选结果快照（${snapshot.resultId}）`);
}

async function goCompare() {
    const request = toRequest();
    if (!request) return;
    const snapshot = await saveScreeningResult({ request, result: result.value });
    router.push({
        path: '/app/ranking-compare',
        query: {
            sourceType: 'FILTER_RESULT',
            sourceId: snapshot.resultId,
            selectedTargetCodes: selectedTargetCodes.value.join(','),
            targetType: targetType.value
        }
    });
}
</script>

<style scoped lang="scss">
.screening-page {
    min-width: 0;
}

.page-heading,
.result-header,
.conditions-heading,
.group-toolbar,
.group-actions,
.condition-actions {
    display: flex;
    align-items: center;
}

.page-heading {
    justify-content: space-between;
    margin-bottom: 18px;
}

.page-heading h2 {
    margin: 0 0 6px;
    font-size: 20px;
}

.page-heading p,
.section-hint,
.result-note {
    margin: 0;
    color: var(--el-text-color-secondary);
    font-size: 13px;
}

/* 页面主体：左侧筛选配置保持紧凑，右侧结果区域占据主要空间 */
.screening-layout {
    display: grid;
    grid-template-columns: minmax(320px, 360px) minmax(0, 1fr);
    gap: 20px;
    align-items: start;
}

.configuration-card,
.result-card {
    min-width: 0;
    border: 1px solid var(--el-border-color);
    border-radius: 8px;
    overflow: hidden;
}

.configuration-card :deep(.el-card__header),
.result-card :deep(.el-card__header) {
    padding: 14px 16px;
    border-bottom: 1px solid var(--el-border-color-lighter);
    background: var(--el-fill-color-lighter);
}

.configuration-card :deep(.el-card__body),
.result-card :deep(.el-card__body) {
    padding: 16px;
}

/* 左侧各配置区域独立成块，增强组件之间的边界和留白 */
.configuration-section {
    margin-bottom: 12px;
    padding: 14px;
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 6px;
    background: var(--el-bg-color);
}

.configuration-section:last-of-type {
    margin-bottom: 0;
}

.scope-section {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 12px;
    cursor: pointer;
    transition: border-color 0.2s ease, background-color 0.2s ease;
}

.scope-section:hover {
    border-color: var(--el-color-primary-light-5);
    background: var(--el-color-primary-light-9);
}

.section-title {
    margin-bottom: 12px;
    font-weight: 600;
}

.conditions-heading,
.group-toolbar,
.result-header {
    justify-content: space-between;
}

.conditions-heading {
    gap: 10px;
}

.conditions-heading .section-title {
    margin: 0;
}

.condition-group {
    margin-top: 12px;
    padding: 12px;
    border: 1px solid var(--el-border-color);
    border-radius: 6px;
    background: var(--el-fill-color-extra-light);
}

.group-toolbar {
    margin-bottom: 10px;
    font-size: 13px;
    font-weight: 600;
}

.group-actions {
    gap: 6px;
}

.logic-select {
    width: 86px;
}

.condition-row {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
    padding: 10px 0;
    border-top: 1px dashed var(--el-border-color-lighter);
}

.condition-row:first-of-type {
    border-top: 0;
}

.metric-name {
    display: flex;
    flex: 1 1 128px;
    flex-direction: column;
    min-width: 128px;
}

.metric-name small {
    color: var(--el-text-color-secondary);
}

.operator-select {
    width: 108px;
}

.value-input {
    width: 112px;
}

.between-separator,
.metric-unit,
.null-description {
    color: var(--el-text-color-secondary);
    font-size: 13px;
}

.empty-group {
    padding: 6px 0;
    color: var(--el-text-color-secondary);
    font-size: 13px;
}

.condition-actions {
    gap: 8px;
    margin-top: 14px;
    flex-wrap: wrap;
}

.configuration-footer {
    margin-top: 16px;
    padding-top: 16px;
    border-top: 1px solid var(--el-border-color-lighter);
    text-align: right;
}

.result-header {
    gap: 12px;
    flex-wrap: wrap;
}

.result-header > span:last-child {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
}

.result-header > span:last-child :deep(.el-button + .el-button) {
    margin-left: 0;
}

.result-header em {
    margin-left: 10px;
    color: var(--el-color-primary);
    font-style: normal;
    font-size: 14px;
}

.result-table {
    width: 100%;
    margin-top: 14px;
}

.result-note {
    margin-top: 10px;
}

@media (max-width: 1024px) {
    .screening-layout {
        grid-template-columns: 1fr;
    }
}

@media (max-width: 640px) {
    .page-heading {
        align-items: flex-start;
        gap: 12px;
        flex-direction: column;
    }

    .screening-layout {
        grid-template-columns: minmax(0, 1fr);
        gap: 14px;
    }

    .configuration-card :deep(.el-card__body),
    .result-card :deep(.el-card__body) {
        padding: 12px;
    }

    .configuration-section {
        padding: 12px;
    }

    .condition-row {
        align-items: stretch;
    }

    .operator-select,
    .value-input {
        flex: 1 1 120px;
        width: auto;
    }
}
</style>