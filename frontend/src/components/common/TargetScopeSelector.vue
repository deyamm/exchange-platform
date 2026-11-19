<template>
    <el-dialog :model-value="open" :title="dialogTitle" width="620px" destroy-on-close @close="handleCancel">
        <el-form label-width="88px">
            <el-form-item label="范围类型">
                <el-radio-group v-model="draft.scopeType" @change="handleScopeTypeChange">
                    <el-radio v-for="item in availableScopeTypes" :key="item.value" :label="item.value">
                        {{ item.label }}
                    </el-radio>
                </el-radio-group>
            </el-form-item>

            <template v-if="draft.scopeType !== 'ALL'">
                <template v-if="draft.scopeType === 'BOARD'">
                    <el-form-item label="行业发布方">
                        <el-select v-model="draft.industryPublisherCode" placeholder="请选择发布方" @change="handleIndustryDimensionChange">
                            <el-option label="申万" value="SW" />
                            <el-option label="东方财富" value="EM" />
                            <el-option label="同花顺" value="THS" />
                        </el-select>
                    </el-form-item>
                    <el-form-item label="行业层级">
                        <el-select v-model="draft.industryLevel" placeholder="请选择层级" :disabled="!draft.industryPublisherCode" @change="handleIndustryDimensionChange">
                            <el-option v-for="level in industryLevels" :key="level" :label="`Level ${level}`" :value="level" />
                        </el-select>
                    </el-form-item>
                </template>
                <el-form-item :label="optionLabel">
                    <el-input v-model="keyword" clearable placeholder="搜索名称" />
                </el-form-item>
                <el-form-item label="可选项">
                    <div v-loading="loading" class="scope-option-panel">
                        <el-checkbox-group v-model="draft.scopeValues" class="scope-option-list">
                            <el-checkbox v-for="item in filteredOptions" :key="item.code" :label="item.code">
                                {{ item.name }}
                            </el-checkbox>
                        </el-checkbox-group>
                        <el-empty v-if="!loading && filteredOptions.length === 0" :image-size="64"
                            description="暂无可选项" />
                    </div>
                </el-form-item>
                <el-form-item label="已选择">
                    {{ draft.scopeValues.length }} 个{{ optionLabel }}
                </el-form-item>
            </template>
        </el-form>
        <template #footer>
            <el-button @click="handleCancel">取消</el-button>
            <el-button type="primary" @click="handleConfirm">确定</el-button>
        </template>
    </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { queryEnabledTargetScopeGroups } from '/@/api/commonSelectorApi';
import {
    emptyTargetScope,
    isScopeTypeSupported,
    scopeTypeOptions,
    type TargetScope,
    type TargetScopeOption,
    type TargetScopeOptions,
    type TargetScopeType,
    type TargetType,
} from '/@/types/targetScope';

const props = withDefaults(defineProps<{
    open: boolean;
    targetType: TargetType;
    value?: TargetScope;
    /** 页面传入的已发布主数据选项，组件不固化板块或指数枚举。 */
    boardOptions?: TargetScopeOption[];
    watchGroupOptions?: TargetScopeOption[];
    indexTypeOptions?: TargetScopeOption[];
    indexMarketOptions?: TargetScopeOption[];
}>(), {
    value: undefined,
    boardOptions: () => [],
    watchGroupOptions: () => [],
    indexTypeOptions: () => [],
    indexMarketOptions: () => [],
});

const emit = defineEmits<{
    (e: 'confirm', scope: TargetScope): void;
    (e: 'cancel'): void;
}>();

const draft = reactive<TargetScope>(emptyTargetScope(props.targetType));
const keyword = ref('');
const loading = ref(false);
const loadedOptions = reactive<TargetScopeOptions>({ board: [], watchGroup: [], indexType: [], market: [] });

const dialogTitle = computed(() => props.targetType === 'STOCK' ? '选择股票范围' : '选择指数范围');
const availableScopeTypes = computed(() => scopeTypeOptions(props.targetType));
const optionKey = computed<keyof TargetScopeOptions | undefined>(() => {
    if (draft.scopeType === 'BOARD') return 'board';
    if (draft.scopeType === 'WATCH_GROUP') return 'watchGroup';
    if (draft.scopeType === 'INDEX_TYPE') return 'indexType';
    // 后端为兼容历史响应字段仍使用 market 承载指数发布方枚举。
    if (draft.scopeType === 'PUBLISHER' || draft.scopeType === 'MARKET') return 'market';
    return undefined;
});
const optionLabel = computed(() => ({
    BOARD: '行业指数',
    WATCH_GROUP: '关注分组',
    INDEX_TYPE: '指数类型',
    PUBLISHER: '发布方',
    MARKET: '发布方',
}[draft.scopeType] || '选项'));
const activeOptions = computed<TargetScopeOption[]>(() => {
    switch (optionKey.value) {
        case 'board': return props.boardOptions.length ? props.boardOptions : loadedOptions.board;
        case 'watchGroup': return props.watchGroupOptions.length ? props.watchGroupOptions : loadedOptions.watchGroup;
        case 'indexType': return props.indexTypeOptions.length ? props.indexTypeOptions : loadedOptions.indexType;
        case 'market': return props.indexMarketOptions.length ? props.indexMarketOptions : loadedOptions.market;
        default: return [];
    }
});
const industryLevels = computed(() => Array.from(new Set(
    (props.boardOptions.length ? props.boardOptions : loadedOptions.board)
        .filter(item => item.publisherCode === draft.industryPublisherCode)
        .map(item => item.level)
        .filter((level): level is number => typeof level === 'number'),
)).sort((left, right) => left - right));
const filteredOptions = computed(() => {
    const normalizedKeyword = keyword.value.trim().toLowerCase();
    return activeOptions.value.filter((item) => {
        const matchesIndustryDimension = draft.scopeType !== 'BOARD'
            || (item.publisherCode === draft.industryPublisherCode && item.level === draft.industryLevel);
        return matchesIndustryDimension
            && (!normalizedKeyword || item.name.toLowerCase().includes(normalizedKeyword));
    });
});

watch(() => [props.open, props.targetType] as const, async ([visible]) => {
    if (!visible) return;
    resetDraft();
    await loadOptions();
}, { immediate: true });

function resetDraft() {
    const next = props.value && props.value.targetType === props.targetType && isScopeTypeSupported(props.targetType, props.value.scopeType)
        ? props.value
        : emptyTargetScope(props.targetType);
    draft.targetType = props.targetType;
    draft.scopeType = next.scopeType;
    draft.scopeValues = next.scopeType === 'ALL' ? [] : [...new Set(next.scopeValues || [])];
    draft.industryPublisherCode = next.industryPublisherCode;
    draft.industryLevel = next.industryLevel;
    keyword.value = '';
}

async function loadOptions() {
    loading.value = true;
    try {
        const loads: Promise<void>[] = [];
        if (!props.watchGroupOptions.length) {
            loads.push(queryEnabledTargetScopeGroups().then((options) => { loadedOptions.watchGroup = options; }));
        }
        await Promise.all(loads);
    } finally {
        loading.value = false;
    }
}

function handleScopeTypeChange(scopeType: TargetScopeType) {
    draft.scopeType = scopeType;
    draft.scopeValues = [];
    draft.industryPublisherCode = undefined;
    draft.industryLevel = undefined;
    keyword.value = '';
}

function handleConfirm() {
    if (draft.scopeType !== 'ALL' && draft.scopeValues.length === 0) {
        ElMessage.warning(`请至少选择一个${optionLabel.value}`);
        return;
    }
    if (draft.scopeType === 'BOARD' && (!draft.industryPublisherCode || !draft.industryLevel)) {
        ElMessage.warning('请选择行业发布方和层级');
        return;
    }
    emit('confirm', {
        targetType: props.targetType,
        scopeType: draft.scopeType,
        scopeValues: draft.scopeType === 'ALL' ? [] : [...new Set(draft.scopeValues)],
        industryPublisherCode: draft.scopeType === 'BOARD' ? draft.industryPublisherCode : undefined,
        industryLevel: draft.scopeType === 'BOARD' ? draft.industryLevel : undefined,
    });
}

function handleIndustryDimensionChange() {
    draft.scopeValues = [];
    if (!industryLevels.value.includes(draft.industryLevel || 0)) {
        draft.industryLevel = undefined;
    }
}

function handleCancel() {
    emit('cancel');
}
</script>

<style scoped lang="scss">
.scope-option-panel {
    min-height: 92px;
    width: 100%;
}

.scope-option-list {
    display: flex;
    flex-wrap: wrap;
    gap: 12px 24px;
}

.scope-option-list :deep(.el-checkbox) {
    margin-right: 0;
    min-width: 150px;
}
</style>
