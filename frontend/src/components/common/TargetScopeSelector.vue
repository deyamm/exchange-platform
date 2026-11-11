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
import { queryEnabledTargetScopeGroups, queryIndexTargetScopeOptions } from '/@/api/commonSelectorApi';
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
    /** 板块主数据尚未提供公共只读接口时由调用方传入，避免在组件内固化示例板块。 */
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
    if (draft.scopeType === 'MARKET') return 'market';
    return undefined;
});
const optionLabel = computed(() => ({ BOARD: '板块', WATCH_GROUP: '关注分组', INDEX_TYPE: '指数类型', MARKET: '所属市场' }[draft.scopeType] || '选项'));
const activeOptions = computed<TargetScopeOption[]>(() => {
    switch (optionKey.value) {
        case 'board': return props.boardOptions.length ? props.boardOptions : loadedOptions.board;
        case 'watchGroup': return props.watchGroupOptions.length ? props.watchGroupOptions : loadedOptions.watchGroup;
        case 'indexType': return props.indexTypeOptions.length ? props.indexTypeOptions : loadedOptions.indexType;
        case 'market': return props.indexMarketOptions.length ? props.indexMarketOptions : loadedOptions.market;
        default: return [];
    }
});
const filteredOptions = computed(() => {
    const normalizedKeyword = keyword.value.trim().toLowerCase();
    if (!normalizedKeyword) return activeOptions.value;
    return activeOptions.value.filter((item) => item.name.toLowerCase().includes(normalizedKeyword));
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
    keyword.value = '';
}

async function loadOptions() {
    loading.value = true;
    try {
        const loads: Promise<void>[] = [];
        if (!props.watchGroupOptions.length) {
            loads.push(queryEnabledTargetScopeGroups().then((options) => { loadedOptions.watchGroup = options; }));
        }
        if (props.targetType === 'INDEX' && (!props.indexTypeOptions.length || !props.indexMarketOptions.length)) {
            loads.push(queryIndexTargetScopeOptions().then((options) => {
                if (!props.indexTypeOptions.length) loadedOptions.indexType = options.indexType;
                if (!props.indexMarketOptions.length) loadedOptions.market = options.market;
            }));
        }
        await Promise.all(loads);
    } finally {
        loading.value = false;
    }
}

function handleScopeTypeChange(scopeType: TargetScopeType) {
    draft.scopeType = scopeType;
    draft.scopeValues = [];
    keyword.value = '';
}

function handleConfirm() {
    if (draft.scopeType !== 'ALL' && draft.scopeValues.length === 0) {
        ElMessage.warning(`请至少选择一个${optionLabel.value}`);
        return;
    }
    emit('confirm', {
        targetType: props.targetType,
        scopeType: draft.scopeType,
        scopeValues: draft.scopeType === 'ALL' ? [] : [...new Set(draft.scopeValues)],
    });
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
