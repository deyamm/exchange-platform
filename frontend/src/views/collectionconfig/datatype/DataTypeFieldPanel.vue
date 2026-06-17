<template>
    <div class="field-panel">
        <el-table :data="fields" border>
            <template #empty>
                <el-empty description="暂无字段">
                    <el-button type="primary" :icon="Plus" @click="handleAddField">新增字段</el-button>
                    <el-button @click="loadFieldsFromCollectionTask">从采集任务加载</el-button>
                </el-empty>
            </template>
            <el-table-column label="字段编码" min-width="100"><template #default="{ row }"><el-input v-model="row.fieldCode" placeholder="trade_date" /></template></el-table-column>
            <el-table-column label="字段名称" min-width="100"><template #default="{ row }"><el-input v-model="row.fieldName" placeholder="交易日期" /></template></el-table-column>
            <el-table-column label="字段类型" width="130">
                <template #default="{ row }">
                    <el-select v-model="row.fieldType">
                        <el-option v-for="option in FIELD_TYPE_OPTIONS" :key="option.value" :label="option.label" :value="option.value" />
                    </el-select>
                </template>
            </el-table-column>
            <el-table-column label="默认值" width="130"><template #default="{ row }"><el-input v-model="row.defaultValue" placeholder="默认值" /></template></el-table-column>
            <el-table-column label="必填" width="90" align="center"><template #default="{ row }"><el-switch v-model="row.required" /></template></el-table-column>
            <el-table-column label="唯一键" width="90" align="center"><template #default="{ row }"><el-switch v-model="row.uniqueKey" /></template></el-table-column>
            <!--el-table-column label="排序" width="100"><template #default="{ row }"><el-input-number v-model="row.sortNo" :min="0" size="small" /></template></el-table-column-->
            <el-table-column label="说明" min-width="180"><template #default="{ row }"><el-input v-model="row.description" /></template></el-table-column>
            <el-table-column label="操作" width="180" fixed="right" align="center">
                <template #default="{ $index }">
                    <el-tooltip content="新增字段" placement="top">
                        <el-button link type="primary" :icon="Plus" @click="handleAddField($index)" />
                    </el-tooltip>
                    <el-tooltip content="上移一行" placement="top">
                        <el-button link type="primary" :icon="Top" :disabled="$index === 0" @click="handleMoveField($index, -1)" />
                    </el-tooltip>
                    <el-tooltip content="下移一行" placement="top">
                        <el-button link type="primary" :icon="Bottom" :disabled="$index === fields.length - 1" @click="handleMoveField($index, 1)" />
                    </el-tooltip>
                    <el-tooltip content="删除字段" placement="top">
                        <el-button link type="danger" :icon="Delete" @click="handleDeleteField($index)" />
                    </el-tooltip>
                </template>
            </el-table-column>
        </el-table>

        <div class="panel-footer" v-if="fields.length > 0">
            <el-button type="primary" :disabled="!dataTypeCode" :loading="saving" @click="handleSave">保存字段</el-button>
        </div>

        <el-dialog
            v-model="collectionTaskDialogVisible"
            title="从采集任务加载字段"
            width="860px"
            destroy-on-close
        >
            <el-table
                v-loading="collectionTaskLoading"
                :data="collectionTaskList"
                border
                height="420"
            >
                <el-table-column prop="taskCode" label="采集任务编码" min-width="160" show-overflow-tooltip />
                <el-table-column prop="taskName" label="采集任务名称" min-width="180" show-overflow-tooltip />
                <el-table-column label="同步状态" width="120" align="center">
                    <template #default="{ row }">
                        <el-tag :type="SYNC_STATUS_TAG_TYPE[row.syncStatus as SyncStatus]">
                            {{ SYNC_STATUS_TEXT[row.syncStatus as SyncStatus] || row.syncStatus }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="110" align="center" fixed="right">
                    <template #default="{ row }">
                        <el-button
                            link
                            type="primary"
                            :loading="selectingTaskCode === row.taskCode"
                            :disabled="Boolean(selectingTaskCode) || !row.currentVersionNo"
                            @click="handleSelectCollectionTask(row)"
                        >
                            选择
                        </el-button>
                    </template>
                </el-table-column>
            </el-table>

            <div class="task-dialog-footer">
                <el-pagination
                    v-model:current-page="pagination.pageNo"
                    v-model:page-size="pagination.pageSize"
                    background
                    layout="total, sizes, prev, pager, next, jumper"
                    :page-sizes="[10]"
                    :total="pagination.total"
                    @current-change="loadTable"
                />
            </div>

            <template #footer>
                <el-button @click="collectionTaskDialogVisible = false">关闭</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Bottom, Delete, Plus, Top } from '@element-plus/icons-vue'
import { queryDataTypeFields, saveDataTypeFields } from '/@/api/datatype/dataTypeApi'
import { DataTypeField, createDefaultDataTypeField } from '/@/types/datatype/entity'
import { FIELD_TYPE_OPTIONS } from '/@/types/datatype/view'
import { queryTaskList, getTemplateVersionDetail } from '/@/api/collectiontask/collectionTaskApi'
import { parseParamFields } from '/@/utils/utils'
import { ParamField, SYNC_STATUS_TAG_TYPE, SYNC_STATUS_TEXT } from "/@/types/collectiontask/view";
import { TaskTemplate, TaskTemplateVersion } from '/@/types/collectiontask/entity'
import { SyncStatus } from '/@/types/collectiontask/constants'


const props = defineProps<{ dataTypeCode: string }>()
const fields = ref<DataTypeField[]>([])
const saving = ref(false)

const collectionTaskDialogVisible = ref(false)
const collectionTaskLoading = ref(false)
const collectionTaskList = ref<TaskTemplate[]>([])
const selectingTaskCode = ref('')
const pagination = reactive({
    pageNo: 1,
    pageSize: 10,
    total: 0
})

watch(() => props.dataTypeCode, () => loadFields(), { immediate: true })

/** 加载当前数据类型字段结构。 */
async function loadFields(): Promise<void> {
    if (!props.dataTypeCode) {
        fields.value = []
        return
    }
    fields.value = await queryDataTypeFields(props.dataTypeCode)
    refreshSortNo()
}

function handleAddField(index?: number): void {
    const insertIndex = typeof index === 'number' ? index + 1 : fields.value.length
    fields.value.splice(insertIndex, 0, createDefaultDataTypeField(props.dataTypeCode, insertIndex + 1))
    refreshSortNo()
}

function handleDeleteField(index: number): void {
    fields.value.splice(index, 1)
    refreshSortNo()
}

function handleMoveField(index: number, offset: -1 | 1): void {
    const targetIndex = index + offset
    if (targetIndex < 0 || targetIndex >= fields.value.length) {
        return
    }
    const [field] = fields.value.splice(index, 1)
    fields.value.splice(targetIndex, 0, field)
    refreshSortNo()
}

function refreshSortNo(): void {
    fields.value.forEach((field, fieldIndex) => {
        field.sortNo = fieldIndex + 1
    })
}

/** 打开已同步采集任务选择弹窗，并分页查询采集任务列表*/
async function loadFieldsFromCollectionTask(): Promise<void> {
    collectionTaskDialogVisible.value = true
    pagination.pageNo = 1
    await loadTable()
}

async function loadTable(): Promise<void> {
    collectionTaskLoading.value = true
    try {
        const result = await queryTaskList({
            pageNo: pagination.pageNo,
            pageSize: pagination.pageSize
        } as any)
        collectionTaskList.value = result.records || []
        pagination.total = result.total || 0
    } finally {
        collectionTaskLoading.value = false
    }
}

/** 选择采集任务后，根据当前版本获取输出字段并填充到当前数据类型字段列表。 */
async function handleSelectCollectionTask(row: TaskTemplate): Promise<void> {

    const versionNo = Number(row.currentVersionNo)
    if (!versionNo) {
        ElMessage.warning('该采集任务暂无当前版本，无法加载字段')
        return
    }

    selectingTaskCode.value = row.taskCode
    try {
        const detail = await getTemplateVersionDetail(row.taskCode, versionNo) as TaskTemplateVersion
        const parsedFields = parseParamFields(detail.outputFieldsJson)

        if (parsedFields.length === 0) {
            ElMessage.warning('该采集任务当前版本未配置输出字段')
            return
        }

        fields.value = parsedFields.map((field, index) => ({
            ...createDefaultDataTypeField(props.dataTypeCode, index + 1),
            fieldCode: field.fieldCode,
            fieldName: field.fieldName,
            fieldType: field.fieldType,
            required: field.required,
            uniqueKey: field.uniqueKey,
            description: field.fieldDesc,
            sortNo: index + 1
        } as DataTypeField))

        refreshSortNo()
        collectionTaskDialogVisible.value = false
        ElMessage.success(`已从采集任务加载 ${parsedFields.length} 个字段`)
    } finally {
        selectingTaskCode.value = ''
    }
}

/** 保存字段草稿。 */
async function handleSave(): Promise<void> {
    if (!props.dataTypeCode) {
        ElMessage.warning('请先选择数据类型')
        return
    }
    saving.value = true
    try {
        fields.value = await saveDataTypeFields(props.dataTypeCode, fields.value)
        refreshSortNo()
        ElMessage.success('字段保存成功')
    } finally {
        saving.value = false
    }
}

</script>

<style scoped>
.field-panel { margin-top: 12px; }
.panel-toolbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.panel-title { font-weight: 600; }
.panel-footer { display: flex; justify-content: center; margin-top: 12px; }
</style>