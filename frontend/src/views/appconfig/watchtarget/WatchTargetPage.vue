<template>
    <div class="application-config-page">
        <el-card v-if="pageMode === 'groups'" shadow="never" class="list-card">
            <template #header>
                <div class="card-header">
                    <span class="card-title">标的分组管理</span>
                </div>
            </template>

            <div class="toolbar">
                <el-button type="primary" @click="openGroupCreate">新增标的分组</el-button>
            </div>

            <el-form :model="groupQuery" inline class="query-form">
                <el-form-item label="关键字">
                    <el-input
                        v-model="groupQuery.keyword"
                        clearable
                        placeholder="编码/名称"
                        style="width: 220px"
                    />
                </el-form-item>
                <el-form-item label="状态">
                    <el-select v-model="groupQuery.status" clearable style="width: 140px">
                        <el-option
                            v-for="item in COMMON_STATUS_OPTIONS"
                            :key="item.value"
                            :label="item.label"
                            :value="item.value"
                        />
                    </el-select>
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" @click="searchGroups">查询</el-button>
                    <el-button @click="resetGroups">重置</el-button>
                </el-form-item>
            </el-form>

            <el-table
                v-loading="groupLoading"
                :data="groupData"
                border
                row-key="groupCode"
                height="calc(100vh - 450px)"
            >
                <el-table-column prop="groupCode" label="分组编码" min-width="120" align="center" />
                <el-table-column prop="groupName" label="分组名称" min-width="120" align="center" />
                <el-table-column prop="sortNo" label="排序" width="90" align="center" />
                <el-table-column prop="status" label="状态" width="100" align="center">
                    <template #default="{ row }">
                        <el-tag :type="COMMON_STATUS_TAG_TYPE[row.status as CommonStatus] || ''">
                            {{ COMMON_STATUS_TEXT[row.status as CommonStatus] || row.status || '-' }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column
                    prop="description"
                    label="说明"
                    min-width="260"
                    show-overflow-tooltip
                    align="center"
                />
                <el-table-column label="操作" width="230" fixed="right" align="center">
                    <template #default="{ row }">
                        <el-button link type="primary" @click="viewGroupTargets(row)">查看自选</el-button>
                        <el-button link type="primary" @click="openGroupEdit(row)">编辑</el-button>
                        <el-button link type="danger" @click="removeGroup(row)">删除</el-button>
                    </template>
                </el-table-column>
            </el-table>

            <div class="pagination-wrapper">
                <el-pagination
                    v-model:current-page="groupQuery.pageNo"
                    v-model:page-size="groupQuery.pageSize"
                    :total="groupTotal"
                    :page-sizes="[10, 20, 50, 100]"
                    layout="total, sizes, prev, pager, next, jumper"
                    background
                    @size-change="handleGroupSizeChange"
                    @current-change="handleGroupCurrentChange"
                />
            </div>
        </el-card>

        <el-card v-else-if="selectedGroup" shadow="never" class="list-card target-section">
            <template #header>
                <div class="section-header">
                    <div>
                        <span class="card-title">
                            自选标的列表：{{ selectedGroup.groupName }}（{{ selectedGroup.groupCode }}）
                        </span>
                    </div>
                    <div class="section-actions">
                        <el-button @click="backToGroupList">返回分组</el-button>
                        <el-button type="primary" @click="openTargetCreate">添加标的</el-button>
                    </div>
                </div>
            </template>

                <el-form :model="targetQuery" inline class="query-form">
                    <el-form-item label="标的代码">
                        <el-input
                            v-model="targetQuery.targetCode"
                            clearable
                            placeholder="如 000001.SZ"
                            style="width: 180px"
                        />
                    </el-form-item>
                    <el-form-item label="标的名称">
                        <el-input
                            v-model="targetQuery.targetName"
                            clearable
                            placeholder="标的名称"
                            style="width: 180px"
                        />
                    </el-form-item>
                    <el-form-item label="市场类型">
                        <el-input
                            v-model="targetQuery.marketCode"
                            clearable
                            placeholder="如 SZ/SH/HK"
                            style="width: 150px"
                        />
                    </el-form-item>
                    <el-form-item>
                        <el-button type="primary" @click="searchTargets">查询</el-button>
                        <el-button @click="resetTargets">重置</el-button>
                    </el-form-item>
                </el-form>

                <el-table
                    v-loading="targetLoading"
                    :data="targetData"
                    border
                    :row-key="getTargetRowKey"
                    height="calc(100vh - 450px)"
                >
                    <el-table-column prop="targetCode" label="标的编码" min-width="140" align="center" />
                    <el-table-column prop="targetName" label="标的名称" min-width="140" align="center" />
                    <el-table-column prop="targetType" label="类型" width="100" align="center">
                        <template #default="{ row }">
                            {{ ASSET_TYPE_TEXT[row.targetType as AssetType] || row.targetType || '-' }}
                        </template>
                    </el-table-column>
                    <el-table-column prop="marketCode" label="市场" width="90" align="center" />
                    <el-table-column prop="importantFlag" label="重点" width="80" align="center">
                        <template #default="{ row }">
                            <el-tag v-if="row.importantFlag" type="warning">重点</el-tag>
                            <span v-else>-</span>
                        </template>
                    </el-table-column>
                    <el-table-column prop="watchStatus" label="观察状态" width="110" align="center">
                        <template #default="{ row }">
                            <el-tag :type="WATCH_STATUS_TAG_TYPE[row.watchStatus as WatchStatus] || ''">
                                {{ WATCH_STATUS_TEXT[row.watchStatus as WatchStatus] || row.watchStatus || '-' }}
                            </el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column
                        prop="watchReason"
                        label="关注理由"
                        min-width="220"
                        show-overflow-tooltip
                        align="center"
                    />
                    <el-table-column label="操作" width="170" fixed="right" align="center">
                        <template #default="{ row }">
                            <el-button link type="primary" @click="openTargetEdit(row)">编辑</el-button>
                            <el-button link type="primary" @click="viewTarget(row)">查看</el-button>
                            <el-button link type="danger" @click="removeTarget(row)">删除</el-button>
                        </template>
                    </el-table-column>
                </el-table>

                <div class="pagination-wrapper">
                    <el-pagination
                        v-model:current-page="targetQuery.pageNo"
                        v-model:page-size="targetQuery.pageSize"
                        :total="targetTotal"
                        :page-sizes="[10, 20, 50, 100]"
                        layout="total, sizes, prev, pager, next, jumper"
                        background
                        @size-change="handleTargetSizeChange"
                        @current-change="handleTargetCurrentChange"
                    />
                </div>
        </el-card>

        <WatchTargetFormDialog
            v-model="targetDialogVisible"
            :mode="targetDialogMode"
            :form-data="currentTarget"
            :locked-group="selectedGroup"
            @success="handleTargetSaved"
        />
        <TargetGroupFormDialog
            v-model="groupDialogVisible"
            :mode="groupDialogMode"
            :form-data="currentGroup"
            @success="loadGroups"
        />
    </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
    queryWatchTargetPage,
    deleteWatchTarget,
    queryTargetGroupPage,
    deleteTargetGroup,
} from '/@/api/watchtarget/watchTargetApi';
import {
    createDefaultWatchTarget,
    createDefaultTargetGroup,
    type WatchTarget,
    type TargetGroup,
} from '/@/types/watchtarget/entity';
import type { WatchTargetQueryParams, TargetGroupQueryParams, AssetType } from '/@/types/watchtarget/constants';
import type { CommonStatus } from '/@/types/appscene/constants';
import type { WatchStatus } from '/@/types/watchtarget/constants';
import { WATCH_STATUS_TEXT, WATCH_STATUS_TAG_TYPE, ASSET_TYPE_TEXT } from '/@/types/watchtarget/view';
import { COMMON_STATUS_OPTIONS, COMMON_STATUS_TEXT, COMMON_STATUS_TAG_TYPE } from '/@/types/appscene/view';
import WatchTargetFormDialog from './WatchTargetFormDialog.vue';
import TargetGroupFormDialog from './TargetGroupFormDialog.vue';

const groupQuery = reactive<TargetGroupQueryParams>({
    keyword: '',
    status: '',
    pageNo: 1,
    pageSize: 20,
});

const targetQuery = reactive<WatchTargetQueryParams>({
    targetCode: '',
    targetName: '',
    marketCode: '',
    groupCode: '',
    pageNo: 1,
    pageSize: 20,
});

const groupLoading = ref(false);
const targetLoading = ref(false);
const groupData = ref<TargetGroup[]>([]);
const targetData = ref<WatchTarget[]>([]);
const groupTotal = ref(0);
const targetTotal = ref(0);

const targetDialogVisible = ref(false);
const groupDialogVisible = ref(false);
const targetDialogMode = ref<'create' | 'edit'>('create');
const groupDialogMode = ref<'create' | 'edit'>('create');

const currentTarget = ref<WatchTarget>(createDefaultWatchTarget());
const currentGroup = ref<TargetGroup>(createDefaultTargetGroup());
const selectedGroup = ref<TargetGroup | null>(null);
const pageMode = ref<'groups' | 'targets'>('groups');

async function loadGroups() {
    groupLoading.value = true;
    try {
        const res = await queryTargetGroupPage(groupQuery);
        groupData.value = res.records || [];
        groupTotal.value = res.total || 0;
    } finally {
        groupLoading.value = false;
    }
}

async function loadTargets() {
    if (!selectedGroup.value) {
        targetData.value = [];
        targetTotal.value = 0;
        return;
    }

    targetLoading.value = true;
    try {
        const res = await queryWatchTargetPage({
            ...targetQuery,
            groupCode: selectedGroup.value.groupCode,
        });
        targetData.value = res.records || [];
        targetTotal.value = res.total || 0;
    } finally {
        targetLoading.value = false;
    }
}

function searchGroups() {
    groupQuery.pageNo = 1;
    loadGroups();
}

function resetGroups() {
    Object.assign(groupQuery, {
        keyword: '',
        status: '',
        pageNo: 1,
        pageSize: 20,
    });
    loadGroups();
}

function handleGroupSizeChange(size: number) {
    groupQuery.pageSize = size;
    groupQuery.pageNo = 1;
    loadGroups();
}

function handleGroupCurrentChange(pageNo: number) {
    groupQuery.pageNo = pageNo;
    loadGroups();
}

function searchTargets() {
    targetQuery.pageNo = 1;
    loadTargets();
}

function resetTargets() {
    Object.assign(targetQuery, {
        targetCode: '',
        targetName: '',
        marketCode: '',
        groupCode: selectedGroup.value?.groupCode || '',
        pageNo: 1,
        pageSize: 20,
    });
    loadTargets();
}

function handleTargetSizeChange(size: number) {
    targetQuery.pageSize = size;
    targetQuery.pageNo = 1;
    loadTargets();
}

function handleTargetCurrentChange(pageNo: number) {
    targetQuery.pageNo = pageNo;
    loadTargets();
}

async function viewGroupTargets(row: TargetGroup) {
    selectedGroup.value = { ...row };
    pageMode.value = 'targets';
    Object.assign(targetQuery, {
        targetCode: '',
        targetName: '',
        marketCode: '',
        groupCode: row.groupCode,
        pageNo: 1,
        pageSize: targetQuery.pageSize || 20,
    });
    await loadTargets();
}

function backToGroupList() {
    pageMode.value = 'groups';
    selectedGroup.value = null;
    targetData.value = [];
    targetTotal.value = 0;
}

function openGroupCreate() {
    groupDialogMode.value = 'create';
    currentGroup.value = createDefaultTargetGroup();
    groupDialogVisible.value = true;
}

function openGroupEdit(row: TargetGroup) {
    groupDialogMode.value = 'edit';
    currentGroup.value = { ...row };
    groupDialogVisible.value = true;
}

function openTargetCreate() {
    if (!selectedGroup.value) {
        ElMessage.warning('请先选择一个标的分组');
        return;
    }

    targetDialogMode.value = 'create';
    currentTarget.value = {
        ...createDefaultWatchTarget(),
        targetGroups: [selectedGroup.value],
    };
    targetDialogVisible.value = true;
}

function openTargetEdit(row: WatchTarget) {
    if (!selectedGroup.value) {
        ElMessage.warning('请先选择一个标的分组');
        return;
    }

    targetDialogMode.value = 'edit';
    currentTarget.value = {
        ...row,
        targetGroups: row.targetGroups,
    };
    targetDialogVisible.value = true;
}

function viewTarget(_row: WatchTarget) {
    // 预留查看功能，后续按业务需要补充。
}

async function handleTargetSaved() {
    await loadTargets();
    await loadGroups();
}

async function removeTarget(row: WatchTarget) {
    if (!selectedGroup.value) {
        ElMessage.warning('请先选择一个标的分组');
        return;
    }

    await ElMessageBox.confirm(
        `确认从分组「${selectedGroup.value.groupName}」中删除标的「${row.targetName}」？`,
        '提示',
        { type: 'warning' },
    );
    await deleteWatchTarget(row.targetCode, {
        targetType: row.targetType,
        groupCode: selectedGroup.value.groupCode,
    });
    ElMessage.success('删除成功');
    await loadTargets();
    await loadGroups();
}

async function removeGroup(row: TargetGroup) {
    await ElMessageBox.confirm(`确认删除标的分组「${row.groupName}」？`, '提示', { type: 'warning' });
    await deleteTargetGroup(row.groupCode);
    ElMessage.success('删除成功');
    if (selectedGroup.value?.groupCode === row.groupCode) {
        backToGroupList();
    }
    loadGroups();
}

function getTargetRowKey(row: WatchTarget) {
    return `${row.targetCode}_${row.targetType}_${row.marketCode}`;
}

onMounted(() => {
    loadGroups();
});
</script>

<style scoped>
.application-config-page {
    padding: 16px;
    background: #f5f7fa;
    min-height: 100%;
}

.list-card {
    border-radius: 8px;
}

.card-header,
.toolbar,
.section-header,
.section-actions {
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.toolbar {
    margin-bottom: 12px;
}

.section-actions {
    gap: 8px;
}

.card-title {
    font-size: 16px;
    font-weight: 600;
    color: #303133;
}

.query-form {
    margin-bottom: 12px;
    padding: 12px 12px 0;
    background: #fafafa;
    border-radius: 6px;
}

.pagination-wrapper {
    display: flex;
    justify-content: center;
    margin-top: 12px;
}

.target-section {
    margin-top: 0;
}
</style>