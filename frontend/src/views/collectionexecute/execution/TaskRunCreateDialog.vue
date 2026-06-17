<template>
    <el-dialog v-model="visible" :title="dialogTitle" width="720px" destroy-on-close>
        <el-form :model="createForm" label-width="120px" class="create-form">
            <el-form-item label="采集任务">
                <el-select
                    v-model="createForm.taskCode"
                    filterable
                    clearable
                    placeholder="选择已同步任务"
                    :loading="taskOptionsLoading"
                    style="width: 100%"
                    @change="handleTaskChange"
                >
                    <el-option
                        v-for="task in syncedTasks"
                        :key="task.taskCode"
                        :label="`${task.taskName}（${task.taskCode}）`"
                        :value="task.taskCode"
                    />
                </el-select>
            </el-form-item>

            <el-form-item v-if="!isScheduledOnly" label="执行方式">
                <el-radio-group v-model="createForm.triggerType">
                    <el-radio label="MANUAL">手动</el-radio>
                    <el-radio label="SCHEDULED">定时</el-radio>
                </el-radio-group>
            </el-form-item>

            <el-divider v-if="createForm.triggerType === 'SCHEDULED'" content-position="left">定时规则</el-divider>
            <el-form-item v-if="createForm.triggerType === 'SCHEDULED'" label="周期设置">
                <div class="schedule-rule-layout">
                    <div class="schedule-fields">
                        <span class="schedule-label">周期类型</span>
                        <el-select v-model="createForm.scheduleMode" style="width: 160px">
                            <el-option label="按秒" value="SECOND" />
                            <el-option label="按分钟" value="MINUTE" />
                            <el-option label="按小时" value="HOUR" />
                            <el-option label="按天" value="DAY" />
                            <el-option label="按周" value="WEEK" />
                            <el-option label="按月" value="MONTH" />
                            <el-option label="交易日" value="WEEKDAY" />
                        </el-select>

                        <template v-if="isIntervalMode(createForm.scheduleMode)">
                            <span class="schedule-label">每</span>
                            <el-input-number
                                v-model="createForm.scheduleInterval"
                                :min="1"
                                :max="9999"
                                controls-position="right"
                                style="width: 120px"
                            />
                            <span>{{ getScheduleUnitText(createForm.scheduleMode) }}</span>
                        </template>
                    </div>

                    <div v-if="createForm.scheduleMode === 'WEEK'" class="schedule-sub-block">
                        <div class="schedule-sub-title">每周执行日期</div>
                        <el-checkbox-group v-model="createForm.scheduleWeekDays">
                            <el-checkbox v-for="(key, value) in WEEKDAY_MAP" :label="value">{{ key }}</el-checkbox>
                        </el-checkbox-group>
                    </div>

                    <div v-if="createForm.scheduleMode === 'MONTH'" class="schedule-sub-block schedule-inline-row">
                        <span class="schedule-sub-title">每月执行日期</span>
                        <el-input-number
                            v-model="createForm.scheduleMonthDay"
                            :min="1"
                            :max="31"
                            controls-position="right"
                            style="width: 120px"
                        />
                        <span class="schedule-hint">日</span>
                    </div>

                    <div v-if="createForm.scheduleMode === 'WEEKDAY'" class="schedule-sub-block">
                        <span class="schedule-hint">将在每个交易日执行。</span>
                    </div>
                </div>
            </el-form-item>
            <el-form-item v-if="createForm.triggerType === 'SCHEDULED'" label="执行时间">
                <div class="schedule-rule-layout">
                    <div v-if="createForm.scheduleMode !== 'SECOND'" class="schedule-time-grid">
                        <div v-if="isHourMode(createForm.scheduleMode)" class="schedule-time-item">
                            <span class="schedule-label">小时</span>
                            <el-input-number
                                v-model="createForm.scheduleHour"
                                :min="0"
                                :max="23"
                                controls-position="right"
                            />
                        </div>
                        <div v-if="isMinuteMode(createForm.scheduleMode)" class="schedule-time-item">
                            <span class="schedule-label">分钟</span>
                            <el-input-number
                                v-model="createForm.scheduleMinute"
                                :min="0"
                                :max="59"
                                controls-position="right"
                            />
                        </div>
                        <div class="schedule-time-item">
                            <span class="schedule-label">秒</span>
                            <el-input-number
                                v-model="createForm.scheduleSecond"
                                :min="0"
                                :max="59"
                                controls-position="right"
                            />
                        </div>
                    </div>
                    <el-alert
                        v-else
                        title="按秒执行时无需设置额外执行时间。"
                        type="info"
                        :closable="false"
                        show-icon
                    />
                </div>
            </el-form-item>
            <el-form-item v-if="createForm.triggerType === 'SCHEDULED'" label="Cron 预览">
                <div class="cron-preview">
                    <code>{{ cronExpression }}</code>
                    <span class="schedule-hint">{{ getScheduleContent(createForm.scheduleMode, 'text') }}</span>
                </div>
            </el-form-item>

            <el-divider content-position="left">执行参数</el-divider>
            <template v-if="paramFields.length">
                <el-form-item
                    v-for="field in paramFields"
                    :key="field.fieldCode"
                    :label="`${field.fieldName}（${field.fieldCode}）`"
                    label-width="30%"
                    class="center-label"
                >
                    <el-input-number
                        v-if="isNumberField(field.fieldType)"
                        v-model="createForm.params[field.fieldCode]"
                        :min="-999999999"
                        :max="999999999"
                        controls-position="right"
                        style="width: 100%"
                    />
                    <div v-else-if="isDateField(field.fieldType) || isDateTimeField(field.fieldType)" class="param-date-field">
                        <el-input
                            v-model="createForm.params[field.fieldCode]"
                            :placeholder="getDatePlaceholder(field.fieldType, field.fieldDesc)"
                        />
                        <div class="param-token-row">
                            <el-button size="small" @click="applyParamToken(field, 'today')">今天</el-button>
                            <el-button size="small" @click="applyParamToken(field, 'tomorrow')">明天</el-button>
                            <el-button size="small" @click="applyParamToken(field, 'fireDate')">执行日期</el-button>
                            <el-button size="small" @click="applyParamToken(field, 'now')">现在</el-button>
                            <el-button size="small" @click="applyParamToken(field, 'fireTime')">触发时间</el-button>
                        </div>
                    </div>
                    <el-input v-else v-model="createForm.params[field.fieldCode]" :placeholder="field.fieldDesc || '请输入'" />
                </el-form-item>
            </template>
            <el-empty v-else description="该任务模板无执行参数" />
        </el-form>

        <template #footer>
            <el-button @click="visible = false">取消</el-button>
            <el-button type="primary" :loading="createLoading" @click="handleCreate">创建</el-button>
        </template>
    </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch, withDefaults } from 'vue'
import { ElMessage } from 'element-plus'
import { createTaskRun } from '/@/api/execution/executionApi'
import { queryTaskList, getTaskDetail } from '/@/api/collectiontask/collectionTaskApi'
import type { TriggerType } from '/@/types/execution/constants'
import type { TaskTemplate, TaskDetail } from '/@/types/collectiontask/entity'
import type { ParamField } from '/@/types/collectiontask/view'
import { WEEKDAY_MAP } from '/@/types/execution/view'
import { parseParamFields, generateRequestId, resolveTokenValue } from '/@/utils/utils'
import { buildScheduleContent, getScheduleTextFromCron } from '/@/utils/schedule'


const props = withDefaults(defineProps<{ visible: boolean; mode?: 'all' | 'scheduled' }>(), {
    mode: 'all'
})
const emit = defineEmits<{
    (event: 'update:visible', value: boolean): void
    (event: 'created'): void
}>()

const visible = computed({
    get: () => props.visible,
    set: (value: boolean) => emit('update:visible', value)
})

const isScheduledOnly = computed(() => props.mode === 'scheduled')
const dialogTitle = computed(() => (isScheduledOnly.value ? '新建定时任务' : '新建执行'))

const createLoading = ref(false)
const taskOptionsLoading = ref(false)
const syncedTasks = ref<TaskTemplate[]>([])
const selectedTaskDetail = ref<TaskDetail | null>(null)
const paramFields = ref<ParamField[]>([])

const createForm = reactive({
    taskCode: '',
    triggerType: 'MANUAL' as TriggerType,
    scheduleMode: 'DAY',
    scheduleInterval: 1,
    scheduleSecond: 0,
    scheduleMinute: 0,
    scheduleHour: 0,
    scheduleWeekDays: ['MON'],
    scheduleMonthDay: 1,
    params: {} as Record<string, any>
})

const cronExpression = computed(() => getScheduleContent(createForm.scheduleMode, 'cron'))

watch(
    () => props.visible,
    (next) => {
        if (next) {
            resetCreateForm()
            if (isScheduledOnly.value) {
                createForm.triggerType = 'SCHEDULED'
            }
            loadSyncedTasks()
        }
    }
)

async function loadSyncedTasks(): Promise<void> {
    taskOptionsLoading.value = true
    try {
        const result = await queryTaskList({ pageNo: 1, pageSize: 200 })
        syncedTasks.value = (result.records || []).filter((task: TaskTemplate) => task.status === 'ENABLED')
    } finally {
        taskOptionsLoading.value = false
    }
}

function resetCreateForm(): void {
    createForm.taskCode = ''
    createForm.triggerType = isScheduledOnly.value ? 'SCHEDULED' : 'MANUAL'
    createForm.scheduleMode = 'DAY'
    createForm.scheduleInterval = 1
    createForm.scheduleSecond = 0
    createForm.scheduleMinute = 0
    createForm.scheduleHour = 0
    createForm.scheduleWeekDays = ['MON']
    createForm.scheduleMonthDay = 1
    createForm.params = {}
    selectedTaskDetail.value = null
    paramFields.value = []
}

function isNumberField(fieldType?: string): boolean {
    if (!fieldType) return false
    const lower = fieldType.toLowerCase()
    return ['int', 'integer', 'long', 'double', 'float', 'number', 'decimal'].some((key) => lower.includes(key))
}

function isDateField(fieldType?: string): boolean {
    if (!fieldType) return false
    const lower = fieldType.toLowerCase()
    return lower.includes('date') && !lower.includes('datetime') && !lower.includes('timestamp')
}

function isDateTimeField(fieldType?: string): boolean {
    if (!fieldType) return false
    const lower = fieldType.toLowerCase()
    return lower.includes('datetime') || lower.includes('timestamp')
}

function getDatePlaceholder(fieldType?: string, desc?: string): string {
    if (desc) return desc
    return isDateField(fieldType) ? 'YYYY-MM-DD' : 'YYYY-MM-DD HH:mm:ss'
}

/**
 * 是否支持“每 N 个周期执行一次”。
 * 按周、交易日不展示间隔输入，避免和“选择周几”混淆。
 */
function isIntervalMode(mode: string): boolean {
    return ['SECOND', 'MINUTE', 'HOUR', 'DAY', 'MONTH'].includes(mode)
}

/**
 * 当前周期为天、周、月、交易日时，才需要设置小时。
 */
function isHourMode(mode: string): boolean {
    return ['DAY', 'WEEK', 'MONTH', 'WEEKDAY'].includes(mode)
}

/**
 * 当前周期为小时及以上时，才需要设置分钟。
 */
function isMinuteMode(mode: string): boolean {
    return ['HOUR', 'DAY', 'WEEK', 'MONTH', 'WEEKDAY'].includes(mode)
}

function getScheduleUnitText(mode: string): string {
    const unitMap: Record<string, string> = {
        SECOND: '秒',
        MINUTE: '分钟',
        HOUR: '小时',
        DAY: '天',
        MONTH: '个月'
    }
    return unitMap[mode] || ''
}

function getScheduleContent(mode: string, needContent: 'text' | 'cron', cronExpression?: string): string {
    if (cronExpression && needContent === 'text') {
        return getScheduleTextFromCron(cronExpression)
    }
    return buildScheduleContent(
        {
            mode,
            interval: createForm.scheduleInterval,
            second: createForm.scheduleSecond,
            minute: createForm.scheduleMinute,
            hour: createForm.scheduleHour,
            monthDay: createForm.scheduleMonthDay,
            weekDays: createForm.scheduleWeekDays
        },
        needContent
    )
}

async function handleTaskChange(taskCode: string): Promise<void> {
    if (!taskCode) {
        selectedTaskDetail.value = null
        paramFields.value = []
        createForm.params = {}
        return
    }

    const detailResult = await getTaskDetail(taskCode)
    selectedTaskDetail.value = detailResult
    paramFields.value = parseParamFields(detailResult.currentVersion?.paramsSchemaJson)

    const nextParams: Record<string, any> = {}
    paramFields.value.forEach((field) => {
        nextParams[field.fieldCode] = createForm.params[field.fieldCode] ?? ''
    })
    createForm.params = nextParams

    if (!detailResult.config?.taskConfigCode || detailResult.config.status!== 'ENABLED') {
        ElMessage.warning('该任务未启用，无法新建执行')
    }
}

async function handleCreate(): Promise<void> {
    if (!createForm.taskCode) {
        ElMessage.warning('请选择采集任务模板')
        return
    }

    if (!selectedTaskDetail.value?.config?.taskConfigCode) {
        ElMessage.warning('该任务未配置执行配置，无法新建执行')
        return
    }

    if (
        createForm.triggerType === 'SCHEDULED'
        && isIntervalMode(createForm.scheduleMode)
        && (!createForm.scheduleInterval || createForm.scheduleInterval < 1)
    ) {
        ElMessage.warning('请设置有效的定时间隔')
        return
    }

    for (const field of paramFields.value) {
        if (!field.required) continue
        const value = createForm.params[field.fieldCode]
        if (value === undefined || value === null || value === '') {
            ElMessage.warning(`请填写必填参数：${field.fieldName || field.fieldCode}`)
            return
        }
    }

    if (isScheduledOnly.value) {
        createForm.triggerType = 'SCHEDULED'
    }

    const params: Record<string, any> = { ...createForm.params }

    createLoading.value = true
    try {
        await createTaskRun({
            taskConfigCode: selectedTaskDetail.value.config.taskConfigCode,
            taskCode: createForm.taskCode,
            triggerType: createForm.triggerType,
            requestId: generateRequestId(),
            cronExpression: createForm.triggerType === 'SCHEDULED' ? cronExpression.value : undefined,
            params,
            saveOrUpdate: 'SAVE'
        })
        ElMessage.success('已创建执行记录')
        visible.value = false
        emit('created')
    } finally {
        createLoading.value = false
    }
}

function applyParamToken(field: ParamField, token: 'today' | 'tomorrow' | 'fireDate' | 'now' | 'fireTime'): void {
    createForm.params[field.fieldCode] = resolveTokenValue(selectedTaskDetail.value?.currentVersion?.dataSource || 'default', createForm.triggerType, token)
}


</script>

<style scoped>
.create-form {
    max-height: 520px;
    overflow: auto;
    padding-right: 8px;
}

.schedule-rule-layout {
    display: flex;
    flex-direction: column;
    gap: 14px;
    width: 100%;
    border: 1px solid #ebeef5;
    border-radius: 8px;
    padding: 14px 16px;
    background: #fafafa;
}

.schedule-section-header {
    margin-bottom: 12px;
}
.center-label :deep(.el-form-item__label) {
    justify-content: center;
    text-align: center;
}

.schedule-section-title {
    font-size: 14px;
    font-weight: 600;
    color: #303133;
    margin-bottom: 4px;
}

.schedule-section-desc {
    font-size: 12px;
    color: #909399;
    line-height: 1.5;
}

.schedule-fields {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-wrap: wrap;
}

.schedule-label {
    color: #606266;
    font-size: 13px;
    white-space: nowrap;
}

.schedule-hint {
    color: #909399;
}

.schedule-sub-block {
    margin-top: 12px;
    padding-top: 12px;
    border-top: 1px dashed #dcdfe6;
}

.schedule-sub-title {
    color: #606266;
    font-size: 13px;
    margin-right: 8px;
    margin-bottom: 8px;
}

.schedule-inline-row {
    display: flex;
    align-items: center;
    gap: 8px;
}

.schedule-time-grid {
    display: flex;
    align-items: center;
    gap: 12px;
    flex-wrap: wrap;
}

.schedule-time-item {
    display: flex;
    align-items: center;
    gap: 8px;
}

.schedule-time-item :deep(.el-input-number) {
    width: 120px;
}

.schedule-example {
    margin-top: 10px;
    color: #909399;
    font-size: 12px;
}

.cron-preview {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    padding: 10px 12px;
    border-radius: 6px;
    background: #f5f7fa;
    color: #606266;
    font-size: 14px;
    width: 100%;
}

.cron-preview-label {
    color: #303133;
    font-weight: 600;
}

.cron-preview code {
    color: #409eff;
    background: transparent;
    font-family: Consolas, Monaco, monospace;
}

.param-date-field {
    display: flex;
    flex-direction: column;
    gap: 8px;
    width: 100%;
}

.param-token-row {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
}
</style>