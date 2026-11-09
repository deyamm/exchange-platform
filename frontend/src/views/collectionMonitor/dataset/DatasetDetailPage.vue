<template>
    <div v-loading="loading" class="page">
        <div class="page-toolbar">
            <el-button @click="$router.back()">返回</el-button>
        </div>

        <template v-if="detail">
            <el-card class="card" header="基础信息">
                <el-descriptions class="detail-descriptions basic-descriptions" border :column="3">
                    <el-descriptions-item label="编码">
                        {{ displayValue(detail.dataset.datasetCode) }}
                    </el-descriptions-item>
                    <el-descriptions-item label="名称">
                        {{ displayValue(detail.dataset.datasetName) }}
                    </el-descriptions-item>
                    <el-descriptions-item label="标的类型">
                        {{ detail.dataset.assetTypeName || ASSET_TYPE_TEXT[detail.dataset.assetType as AssetType] || displayValue(detail.dataset.assetType) }}
                    </el-descriptions-item>
                    <el-descriptions-item label="状态">
                        {{ displayValue(detail.dataset.datasetStatus) }}
                    </el-descriptions-item>
                    <el-descriptions-item label="数据类型">
                        {{ displayValue(detail.dataset.dataTypeCode) }} /
                        V{{ displayValue(detail.dataset.dataTypeVersionNo) }}
                    </el-descriptions-item>
                    <el-descriptions-item label="最近执行">
                        {{ displayValue(detail.dataset.lastSuccessTime) }}
                    </el-descriptions-item>
                    <el-descriptions-item label="数据集说明" :span="3">
                        {{ displayValue(detail.dataset.description) }}
                    </el-descriptions-item>
                </el-descriptions>
            </el-card>

            <el-card v-for="section in sections" :key="section.group" class="card">
                <template #header>
                    <div class="card-header">
                        <span>{{ section.title }}</span>
                        <el-button v-if="section.group === 'BUSINESS'" type="primary" size="small" @click="openEdit">
                            维护元数据
                        </el-button>
                    </div>
                </template>

                <el-descriptions v-if="section.items.length" class="detail-descriptions metadata-descriptions" border
                    :column="3">
                    <el-descriptions-item v-for="item in section.items" :key="metadataItemKey(item)"
                        :label="item.metadataName || item.metadataKey" :span="getMetadataDisplaySpan(item)">
                        {{ formatMetadata(item) }}
                    </el-descriptions-item>
                </el-descriptions>
                <el-empty v-else :image-size="64" description="—" />
            </el-card>

            <el-card class="card" header="字段结构">
                <el-table :data="detail.structure || []" border>
                    <el-table-column prop="fieldCode" label="字段编码" />
                    <el-table-column prop="fieldName" label="字段名称" />
                    <el-table-column prop="fieldType" label="类型" />
                    <el-table-column prop="required" label="必填" />
                </el-table>
            </el-card>

            <!--
        元数据维护抽屉：
        1. 使用 Tabs 将大量可维护属性按业务分组，避免长表单一次性平铺；
        2. 每个 Tab 内普通字段使用两列布局，长文本等复杂字段独占整行；
        3. 所有 Tab 共享同一份编辑态，切换分组不会丢失已修改内容。
      -->
            <el-drawer v-model="editing" class="dataset-edit-drawer" title="维护数据集信息" size="860px"
                :before-close="beforeCloseEdit" destroy-on-close>
                <div class="drawer-body">
                    <el-form class="drawer-form" label-position="top">
                        <el-tabs v-model="activeEditSection" class="drawer-tabs">
                            <!-- 基础信息单独成组，避免与动态元数据混杂。 -->
                            <el-tab-pane label="基础信息" name="BASIC">
                                <div class="metadata-form-grid">
                                    <el-form-item label="数据集名称">
                                        <el-input v-model="editableName" clearable />
                                    </el-form-item>

                                    <el-form-item label="数据集说明" class="metadata-form-item--full">
                                        <el-input v-model="editableDesc" type="textarea" :rows="4" />
                                    </el-form-item>
                                </div>
                            </el-tab-pane>

                            <!--
                可维护元数据按 group 自动生成 Tab。
                新增元数据项时，只要加入定义配置即可自动进入对应分组。
              -->
                            <el-tab-pane v-for="section in editableSections" :key="section.group" :label="section.title"
                                :name="section.group">
                                <div class="metadata-form-grid">
                                    <el-form-item v-for="definition in section.items"
                                        :key="`${definition.group}:${definition.key}`" :label="definition.name" :class="{
                                            'metadata-form-item--full':
                                                getMetadataEditSpan(definition) === 2,
                                        }">
                                        <!-- 时间字段从当前数据集字段结构中选择，避免手工输入无效字段编码。 -->
                                        <el-select v-if="definition.component === 'time-field'"
                                            v-model="editValues[definition.key]" clearable placeholder="请选择字段">
                                            <el-option v-for="field in detail.structure || []" :key="field.fieldCode"
                                                :label="`${field.fieldName || field.fieldCode} (${field.fieldType || ''})`"
                                                :value="field.fieldCode" />
                                        </el-select>

                                        <!-- 粒度类配置由“单位 + 间隔”共同组成；特殊单位无需填写间隔。 -->
                                        <template v-else-if="definition.component === 'granularity'">
                                            <div class="granularity-editor">
                                                <el-select v-model="editJsonValues[definition.key].unit"
                                                    class="granularity-unit">
                                                    <el-option v-for="unit in definition.options" :key="unit"
                                                        :label="unitLabel(unit)" :value="unit" />
                                                </el-select>

                                                <el-input-number v-if="
                                                    !specialUnits.includes(
                                                        editJsonValues[definition.key].unit,
                                                    )
                                                " v-model="editJsonValues[definition.key].interval" :min="1" />
                                            </div>
                                        </template>

                                        <el-input v-else v-model="editValues[definition.key]" :type="definition.component === 'textarea'
                                                ? 'textarea'
                                                : 'text'
                                            " :rows="definition.component === 'textarea' ? 4 : undefined
                        " clearable />
                                    </el-form-item>
                                </div>
                            </el-tab-pane>
                        </el-tabs>
                    </el-form>
                </div>

                <template #footer>
                    <div class="drawer-footer">
                        <span class="drawer-footer-tip">
                            {{
                                isDirty
                                    ? '存在未保存修改'
                            : '修改任意字段后即可保存'
                            }}
                        </span>
                        <div class="drawer-footer-actions">
                            <el-button @click="cancelEdit">取消</el-button>
                            <el-button type="primary" :loading="saving" :disabled="!isDirty" @click="save">
                                保存修改
                            </el-button>
                        </div>
                    </div>
                </template>
            </el-drawer>
        </template>
    </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ASSET_TYPE_TEXT, type AssetType } from '/@/types/asset'

import {
    getDatasetDetail,
    updateDatasetMetadata,
} from '/@/api/monitor/queryApi'
import {
    datasetMetadataDefinitions,
    manualDatasetMetadataDefinitions,
} from '/@/config/datasetMetadataDefinitions'

type MetadataItem = {
    metadataGroup?: string
    metadataKey?: string
    metadataName?: string
    metadataValue?: any
}

const route = useRoute()

// 页面与抽屉的异步状态：分别控制详情加载、保存中状态和抽屉显隐。
const loading = ref(false)
const saving = ref(false)
const editing = ref(false)
const detail = ref<any>()

// Drawer 当前编辑分组；每次打开默认从基础信息开始。
const activeEditSection = ref('BASIC')

// Drawer 内的编辑态与详情数据解耦，取消编辑时不会污染页面当前展示数据。
const editableName = ref('')
const editableDesc = ref('')
const editValues = reactive<Record<string, any>>({})
const editJsonValues = reactive<Record<string, any>>({})
const editSnapshot = ref('')

const specialUnits = ['TICK', 'EVENT', 'SNAPSHOT', 'STATIC']

// 将接口返回的元数据转换为便于展示和检索的派生数据。
const metadata = computed<MetadataItem[]>(
    () => detail.value?.metadata || [],
)

const metadataMap = computed(
    () =>
        new Map(
            metadata.value.map((item) => [
                `${item.metadataGroup}:${item.metadataKey}`,
                item,
            ]),
        ),
)

// 按元数据分组生成详情页展示区块，并对统计类字段做白名单过滤。
const sections = computed(() =>
    ['BUSINESS', 'MANAGEMENT', 'STATISTICS', 'STORAGE'].map(
        (group) => ({
            group,
            title: (
                {
                    BUSINESS: '业务信息',
                    MANAGEMENT: '管理信息',
                    STATISTICS: '数据统计',
                    STORAGE: '存储信息',
                } as any
            )[group],
            items: datasetMetadataDefinitions
                .filter(
                    (definition) =>
                        definition.group === group &&
                        definition.key !== 'time_coverage_field',
                )
                .map(
                    (definition) =>
                        metadataMap.value.get(`${group}:${definition.key}`) || {
                            metadataGroup: group,
                            metadataKey: definition.key,
                            metadataName: definition.name,
                            metadataValue: '',
                        },
                )
                .filter(
                    (item) =>
                        group !== 'STATISTICS' ||
                        [
                            'record_count',
                            'time_coverage_start',
                            'time_coverage_end',
                            'statistics_updated_at',
                        ].includes(item.metadataKey || ''),
                ),
        }),
    ),
)

// 将当前编辑内容序列化，用于与打开 Drawer 时的快照比较，判断是否存在未保存修改。
/**
 * Drawer 可编辑分组：
 * 只根据 manualDatasetMetadataDefinitions 生成，系统维护的统计/存储字段不会误进入编辑区。
 * 未知分组也会保留，并使用分组编码作为标题，便于后续扩展。
 */
const editableMetadataDefinitions = manualDatasetMetadataDefinitions.filter(
    (definition: any) => definition.group !== 'EXTENSION',
)

const editableSections = computed(() => {
    const titles: Record<string, string> = {
        BUSINESS: '业务信息',
        MANAGEMENT: '管理信息',
    }

    const grouped = new Map<string, any[]>()

    editableMetadataDefinitions.forEach((definition: any) => {
        if (!grouped.has(definition.group)) {
            grouped.set(definition.group, [])
        }
        grouped.get(definition.group)?.push(definition)
    })

    return Array.from(grouped.entries()).map(([group, items]) => ({
        group,
        title: titles[group] || group,
        items: [...items].sort(
            (a, b) => Number(a.order ?? 0) - Number(b.order ?? 0),
        ),
    }))
})

/**
 * 详情页默认使用三列。
 * 若定义配置了 displaySpan 则优先使用；否则 textarea 或较长内容自动独占整行。
 */
function getMetadataDisplaySpan(item: MetadataItem) {
    const definition: any = datasetMetadataDefinitions.find(
        (candidate: any) =>
            candidate.group === item.metadataGroup &&
            candidate.key === item.metadataKey,
    )

    const configuredSpan = Number(definition?.displaySpan)
    if ([1, 2, 3].includes(configuredSpan)) {
        return configuredSpan
    }

    if (definition?.component === 'textarea') {
        return 3
    }

    const value = formatMetadata(item)
    return String(value).length > 60 ? 3 : 1
}

/**
 * Drawer 使用两列布局。
 * 若定义配置了 editSpan 则优先使用；textarea 默认占满两列，其余字段占一列。
 */
function getMetadataEditSpan(definition: any) {
    const configuredSpan = Number(definition?.editSpan)
    if ([1, 2].includes(configuredSpan)) {
        return configuredSpan
    }

    return definition?.component === 'textarea' ? 2 : 1
}

const currentEditState = computed(() =>
    JSON.stringify({
        datasetName: editableName.value,
        datasetDesc: editableDesc.value,
        values: editValues,
        json: editJsonValues,
    }),
)

const isDirty = computed(
    () =>
        Boolean(editSnapshot.value) &&
        currentEditState.value !== editSnapshot.value,
)

function displayValue(value: any) {
    return value === null || value === undefined || value === '' ? '—' : value
}

function metadataItemKey(item: MetadataItem) {
    return `${item.metadataGroup}:${item.metadataKey}`
}

function parseJson(value: any, fallback: any = {}) {
    try {
        return typeof value === 'string' ? JSON.parse(value) : value || fallback
    } catch {
        return fallback
    }
}

function resolveFieldName(code: string) {
    return (
        (detail.value?.structure || []).find(
            (field: any) => field.fieldCode === code,
        )?.fieldName || `${code}（字段已失效）`
    )
}

function resolveFieldNames(value: any) {
    return (
        parseJson(value, [])
            .map((code: string) => resolveFieldName(code))
            .join('、') || '—'
    )
}

function unitLabel(unit: string) {
    const labels: Record<string, string> = {
        SECOND: '秒',
        MINUTE: '分钟',
        HOUR: '小时',
        DAY: '日',
        WEEK: '周',
        MONTH: '月',
        QUARTER: '季度',
        YEAR: '年',
        TICK: '逐笔',
        EVENT: '事件',
        SNAPSHOT: '快照',
        STATIC: '静态',
    }

    return labels[unit] || unit
}

/**
 * 将不同类型的元数据转换为面向用户的展示文本。
 * 字段引用、时间粒度和时间覆盖范围在这里统一做特殊处理。
 */
function formatMetadata(item: MetadataItem) {
    const key = item.metadataKey || ''
    const value = item.metadataValue

    if (key === 'business_key') {
        return resolveFieldNames(value)
    }

    if (key === 'data_time_field') {
        return value ? resolveFieldName(value) : '未设置'
    }

    if (key === 'data_granularity' || key === 'update_frequency') {
        const parsed = parseJson(value, {})

        if (!parsed.unit) {
            return '—'
        }

        return specialUnits.includes(parsed.unit)
            ? unitLabel(parsed.unit)
            : `${key === 'update_frequency' ? '每' : ''}${parsed.interval || 1}${unitLabel(parsed.unit)}`
    }

    if (key === 'time_coverage_start' || key === 'time_coverage_end') {
        const field =
            metadataMap.value.get('STATISTICS:time_coverage_field')?.metadataValue
        const selected =
            metadataMap.value.get('BUSINESS:data_time_field')?.metadataValue

        if (!selected) {
            return '—'
        }

        if (selected !== field) {
            return '待下次执行更新'
        }
    }

    if (
        (key === 'time_coverage_start' || key === 'time_coverage_end') &&
        !value
    ) {
        return '暂无可统计的时间数据'
    }

    return displayValue(value)
}

/**
 * 打开编辑 Drawer：
 * 1. 从详情数据初始化基础信息；
 * 2. 按配置定义初始化普通值和 JSON 结构值；
 * 3. 记录初始快照，后续用于脏数据检测。
 */
function openEdit() {
    activeEditSection.value = 'BASIC'
    editableName.value = detail.value?.dataset?.datasetName || ''
    editableDesc.value = detail.value?.dataset?.description || ''

    editableMetadataDefinitions.forEach((definition) => {
        const item = metadataMap.value.get(
            `${definition.group}:${definition.key}`,
        )

        if (definition.component === 'granularity') {
            editJsonValues[definition.key] = Object.assign(
                {
                    unit: definition.options?.[0],
                    interval: 1,
                },
                parseJson(item?.metadataValue, {}),
            )
        } else {
            editValues[definition.key] = item?.metadataValue || ''
        }
    })

    editSnapshot.value = currentEditState.value
    editing.value = true
}

function closeEdit() {
    editing.value = false
    editSnapshot.value = ''
}

async function cancelEdit() {
    if (!isDirty.value || (await confirmDiscardChanges())) {
        closeEdit()
    }
}

// 关闭或取消前统一检查未保存修改，防止用户误操作丢失编辑内容。
async function confirmDiscardChanges() {
    if (!isDirty.value) {
        return true
    }

    try {
        await ElMessageBox.confirm(
            '当前存在未保存的修改，是否放弃？',
            '放弃修改',
            {
                type: 'warning',
            },
        )
        return true
    } catch {
        return false
    }
}

async function beforeCloseEdit(done: () => void) {
    if (await confirmDiscardChanges()) {
        done()
    }
}

/**
 * 保存流程：
 * 先将动态表单还原为接口需要的 items 结构，提交成功后关闭 Drawer，
 * 再重新拉取详情，确保页面展示的是服务端最终数据。
 */
async function save() {
    if (!isDirty.value || saving.value) {
        return
    }

    saving.value = true

    try {
        const items = editableMetadataDefinitions.map((definition) => ({
            metadataGroup: definition.group,
            metadataKey: definition.key,
            metadataValue:
                definition.component === 'granularity'
                    ? JSON.stringify(editJsonValues[definition.key])
                    : (editValues[definition.key] ?? ''),
        }))

        await updateDatasetMetadata(String(route.params.datasetCode), {
            datasetName: editableName.value,
            datasetDesc: editableDesc.value,
            items,
        })

        closeEdit()
        await load()
        ElMessage.success('元数据已保存')
    } catch (error) {
        console.error(error)
        ElMessage.error('元数据保存失败，请检查后重试')
    } finally {
        saving.value = false
    }
}

// 拉取数据集详情；首次进入页面和保存成功后都会复用该方法刷新数据。
async function load() {
    loading.value = true

    try {
        detail.value = await getDatasetDetail(
            String(route.params.datasetCode),
        )
    } finally {
        loading.value = false
    }
}

onMounted(load)
</script>

<style scoped>
/*
 * 详情属性表统一控制 label / value 比例：
 * - label 固定宽度，避免较长属性名把整列撑宽；
 * - value 自动占用剩余空间，并允许长内容换行。
 */
.detail-descriptions :deep(.el-descriptions__table) {
    table-layout: fixed;
    width: 100%;
}

.detail-descriptions :deep(.el-descriptions__content.el-descriptions__cell) {
    min-width: 0;
    padding: 10px 14px;
    color: var(--el-text-color-primary);
    font-size: 14px;
    line-height: 20px;
    word-break: break-word;
    overflow-wrap: anywhere;
    vertical-align: middle;
}

/* 基础信息字段名都较短，使用更紧凑的 label 宽度。 */
.basic-descriptions :deep(.el-descriptions__label.el-descriptions__cell) {
    width: 120px;
    min-width: 88px;
    max-width: 120px;
}

/*
 * 动态元数据名称相对更长，保留 108px；
 * 单个较长 label 允许换行，但不会继续挤占 value 区域。
 */
.metadata-descriptions :deep(.el-descriptions__label.el-descriptions__cell) {
    width: 120px;
    min-width: 108px;
    max-width: 120px;
}

.page {
    padding: 16px;
}

.page-toolbar {
    margin-bottom: 16px;
}

.card {
    margin-top: 16px;
}

.card:first-of-type {
    margin-top: 0;
}

.card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
}

/* Drawer 头部：适度收紧默认留白，并用分隔线增强层次。 */
:deep(.dataset-edit-drawer .el-drawer__header) {
    margin-bottom: 0;
    padding: 20px 24px 16px;
    border-bottom: 1px solid var(--el-border-color-lighter);
}

:deep(.dataset-edit-drawer .el-drawer__title) {
    color: var(--el-text-color-primary);
    font-size: 16px;
    font-weight: 600;
    line-height: 24px;
}

/*
 * Drawer 正文只负责滚动，实际水平留白统一由 .drawer-body 控制，
 * 避免 Tabs、表单项和边缘之间出现多层重复 padding。
 */
:deep(.dataset-edit-drawer .el-drawer__body) {
    padding: 0;
}

.drawer-body {
    box-sizing: border-box;
    width: 100%;
    min-height: 100%;
    padding: 0 24px 24px;
}

.drawer-form {
    width: 100%;
}

.drawer-tabs {
    width: 100%;
}

.drawer-tabs :deep(.el-tabs__header) {
    position: sticky;
    top: 0;
    z-index: 2;
    margin: 0 -24px 24px;
    padding: 0 24px;
    background: var(--el-bg-color);
}

.drawer-tabs :deep(.el-tabs__nav-wrap::after) {
    height: 1px;
}

.drawer-tabs :deep(.el-tabs__item) {
    height: 52px;
    padding: 0 18px;
}

/*
 * 每个 Tab 内使用两列表单：
 * - 普通 Input / Select 占一列；
 * - textarea 或配置 editSpan=2 的字段占满整行。
 */
.metadata-form-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    column-gap: 24px;
    row-gap: 0;
    width: 100%;
}

.metadata-form-item--full {
    grid-column: 1 / -1;
}

.drawer-form :deep(.el-form-item) {
    min-width: 0;
    margin-bottom: 22px;
}

.drawer-form :deep(.el-form-item__label) {
    height: auto;
    padding: 0 0 8px;
    color: var(--el-text-color-regular);
    font-weight: 500;
    line-height: 20px;
}

.drawer-form :deep(.el-input),
.drawer-form :deep(.el-select),
.drawer-form :deep(.el-textarea),
.drawer-form :deep(.el-input-number) {
    width: 100%;
}

.granularity-editor {
    display: grid;
    grid-template-columns: 140px minmax(0, 1fr);
    align-items: center;
    gap: 12px;
    width: 100%;
}

.granularity-unit {
    width: 140px;
}

/* Drawer 底部持续可见，让长表单编辑时无需滚到底部寻找操作按钮。 */
:deep(.dataset-edit-drawer .el-drawer__footer) {
    padding: 14px 24px;
    border-top: 1px solid var(--el-border-color-lighter);
    background: var(--el-bg-color);
}

.drawer-footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
}

.drawer-footer-tip {
    color: var(--el-text-color-secondary);
    font-size: 13px;
}

.drawer-footer-actions {
    display: flex;
    align-items: center;
    gap: 12px;
    flex-shrink: 0;
}

/*
 * 中等宽度下 Drawer 仍保持两列；
 * 真正窄屏时切换为单列，避免控件被压缩得过窄。
 */
@media (max-width: 1100px) {
    .detail-descriptions :deep(.el-descriptions__label.el-descriptions__cell) {
        width: 96px;
        min-width: 96px;
        max-width: 96px;
        padding-right: 10px;
        padding-left: 10px;
    }

    .basic-descriptions :deep(.el-descriptions__label.el-descriptions__cell) {
        width: 80px;
        min-width: 80px;
        max-width: 80px;
    }
}

@media (max-width: 900px) {
    :deep(.dataset-edit-drawer) {
        width: min(100% - 24px, 860px) !important;
    }

    :deep(.dataset-edit-drawer .el-drawer__header) {
        padding-right: 16px;
        padding-left: 16px;
    }

    .drawer-body {
        padding-right: 16px;
        padding-left: 16px;
    }

    .drawer-tabs :deep(.el-tabs__header) {
        margin-right: -16px;
        margin-left: -16px;
        padding-right: 16px;
        padding-left: 16px;
    }

    :deep(.dataset-edit-drawer .el-drawer__footer) {
        padding-right: 16px;
        padding-left: 16px;
    }
}

@media (max-width: 680px) {
    .metadata-form-grid {
        grid-template-columns: 1fr;
    }

    .metadata-form-item--full {
        grid-column: auto;
    }

    .granularity-editor {
        grid-template-columns: 1fr;
    }

    .granularity-unit {
        width: 100%;
    }

    .drawer-footer {
        align-items: flex-end;
    }

    .drawer-footer-tip {
        display: none;
    }
}
</style>
