<template>
    <el-dialog
        v-model="dialogVisible"
        :title="title"
        width="560px"
        destroy-on-close
        append-to-body
        :close-on-click-modal="false"
    >
        <div class="selected-target">
            <span>当前标的</span>
            <strong>{{ targetCode || '-' }}</strong>
            <em>{{ targetName || '-' }}</em>
        </div>

        <section class="group-section">
            <h3>分组</h3>

            <el-table
                v-loading="groupLoading"
                :data="groupData"
                border
                class="group-table"
                empty-text="暂无分组"
                style="width: 100%"
            >
                <el-table-column width="72" align="center">
                    <template #default="{ row }">
                        <label
                            class="round-checkbox"
                            :class="{ 'is-checked': selectedGroupIds.includes(row.groupCode) }"
                            :aria-label="`选择${row.name}`"
                        >
                            <input
                                type="checkbox"
                                :checked="selectedGroupIds.includes(row.groupCode)"
                                @change="toggleGroup(row, $event)"
                            />
                            <span />
                        </label>
                    </template>
                </el-table-column>

                <el-table-column prop="groupName" label="分组名称" min-width="180" />
            </el-table>

            <div class="pagination-wrapper">
                <el-pagination
                    v-model:current-page="groupQuery.pageNo"
                    :page-size="PAGE_SIZE"
                    :total="groupTotal"
                    layout="total, prev, next"
                    background
                    @current-change="handleGroupCurrentChange"
                />
            </div>
        </section>

        <template #footer>
            <div class="dialog-footer">
                <el-button @click="handleCancel">取消</el-button>
                <el-button type="primary" @click="handleConfirm">确定</el-button>
            </div>
        </template>
    </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { TargetGroupQueryParams } from '/@/types/watchtarget/constants'
import { getWatchTargetGroups, queryTargetGroupPage } from '/@/api/watchtarget/watchTargetApi'
import type { TargetGroup, WatchTarget } from '/@/types/watchtarget/entity'


const PAGE_SIZE = 10

const props = defineProps<{
    modelValue: boolean
    targetCode: string
    targetName?: string
    targetType: string
    title: string
}>()


const emit = defineEmits<{
    (event: 'update:modelValue', value: boolean): void
    (event: 'confirm', payload: { targetCode: string; targetType: string; groupCodes: string[] }): void
}>()

const groupLoading = ref(false)
const groupData = ref<TargetGroup[]>([])
const groupTotal = ref(0)
const groupQuery = ref<TargetGroupQueryParams>({
    status: 'ENABLED',
    pageNo: 1,
    pageSize: PAGE_SIZE
})

const selectedGroupIds = ref<string[]>([])

const dialogVisible = computed({
    get: () => props.modelValue,
    set: (value: boolean) => emit('update:modelValue', value)
})

watch(
    () => props.modelValue,
    (visible) => {
        if (visible) {
            resetDialogState()
            loadGroupPage()
        }
    }
)

function resetDialogState() {
    selectedGroupIds.value = []
    groupData.value = []
    groupTotal.value = 0
    groupQuery.value = {
        status: 'ENABLED',
        pageNo: 1,
        pageSize: PAGE_SIZE
    }
}

async function loadGroupPage() {
    groupLoading.value = true

    try {
        const pageResult =  await queryTargetGroupPage(groupQuery.value)
        const currentTarget: WatchTarget = await getWatchTargetGroups(props.targetCode, props.targetType)

        groupData.value = pageResult.records || []
        groupTotal.value = pageResult.total || 0

        selectedGroupIds.value = currentTarget?.targetGroups?.map((g) => g.groupCode) || []
        
    } catch (error) {
        console.error('加载分组列表失败', error)
        groupData.value = []
        groupTotal.value = 0
        ElMessage.error('加载分组列表失败')
    } finally {
        groupLoading.value = false
    }
}

function handleGroupCurrentChange(pageNo: number) {
    groupQuery.value.pageNo = pageNo
    groupQuery.value.pageSize = PAGE_SIZE
    loadGroupPage()
}

function toggleGroup(row: TargetGroup, event: Event) {
    const checked = (event.target as HTMLInputElement).checked

    // 选中
    if (checked) {
        if (!selectedGroupIds.value.includes(row.groupCode)) {
            selectedGroupIds.value.push(row.groupCode)
        }
        return
    }

    // 取消选中
    selectedGroupIds.value = selectedGroupIds.value.filter((id) => id !== row.groupCode)
}

function handleCancel() {
    dialogVisible.value = false
}

function handleConfirm() {
    // 这里返回选中的分组列表
    console.log('确认选择的分组', selectedGroupIds.value)
    emit('confirm', {
        targetCode: props.targetCode,
        targetType: props.targetType,
        groupCodes: selectedGroupIds.value
    })

    dialogVisible.value = false
}
</script>

<style scoped>
.selected-target {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 16px;
    padding: 12px 14px;
    border-radius: 12px;
    background: #f8fafc;
    color: #475569;
    font-size: 13px;
}

.selected-target strong {
    color: #2563eb;
    font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

.selected-target em {
    color: #111827;
    font-style: normal;
    font-weight: 700;
}

.group-section h3 {
    margin: 0 0 10px;
    color: #111827;
    font-size: 16px;
}

.group-table {
    font-size: 13px;
}

.group-table :deep(.el-table__header th) {
    background: #f8fafc;
    color: #334155;
    font-weight: 700;
}

.group-table :deep(.el-table__cell) {
    padding: 8px 0;
}

.pagination-wrapper {
    display: flex;
    justify-content: flex-end;
    margin-top: 12px;
}

.round-checkbox {
    position: relative;
    display: inline-flex;
    width: 20px;
    height: 20px;
    cursor: pointer;
}

.round-checkbox input {
    position: absolute;
    inset: 0;
    width: 20px;
    height: 20px;
    margin: 0;
    opacity: 0;
    cursor: pointer;
}

.round-checkbox span {
    width: 20px;
    height: 20px;
    box-sizing: border-box;
    border: 2px solid #cbd5e1;
    border-radius: 50%;
    background: #fff;
    transition: all 0.2s ease;
}

.round-checkbox.is-checked span {
    border-color: #2563eb;
    background: #2563eb;
    box-shadow: inset 0 0 0 4px #fff;
}

.dialog-footer {
    display: flex;
    justify-content: flex-end;
    gap: 10px;
}
</style>