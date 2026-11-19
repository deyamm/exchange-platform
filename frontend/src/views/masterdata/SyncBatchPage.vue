<template>
    <div class="page">
        <el-card shadow="never">
            <template #header>
                <div>
                    <b>主数据同步记录</b>
                    <div class="hint">查看 Dataset 发布为正式主数据的批次与状态</div>
                </div>
            </template>
            <el-form :model="filters" inline>
                <el-form-item label="MasterDataType">
                    <el-select
                        v-model="filters.masterDataType"
                        clearable
                        placeholder="全部"
                        style="width: 180px"
                    >
                        <el-option label="股票主数据" value="MARKET_ASSET_STOCK" />
                        <el-option label="指数主数据" value="MARKET_ASSET_INDEX" />
                        <el-option label="指数成分关系" value="MARKET_ASSET_REL_INDEX_MEMBER" />
                    </el-select>
                </el-form-item>
                <el-form-item label="Dataset">
                    <el-input v-model="filters.datasetCode" clearable />
                </el-form-item>
                <el-form-item label="同步类型">
                    <el-select v-model="filters.syncType" clearable placeholder="全部" style="width: 110px">
                        <el-option
                            v-for="item in SYNC_TYPE_OPTIONS"
                            :key="item.value"
                            :label="item.label"
                            :value="item.value"
                        />
                    </el-select>
                </el-form-item>
                <el-form-item label="状态">
                    <el-select v-model="filters.status" clearable placeholder="全部" style="width: 110px">
                        <el-option
                            v-for="item in SYNC_BATCH_STATUS_OPTIONS"
                            :key="item.value"
                            :label="item.label"
                            :value="item.value"
                        />
                    </el-select>
                </el-form-item>
                <el-form-item label="时间范围">
                    <el-date-picker
                        v-model="timeRange"
                        type="datetimerange"
                        value-format="YYYY-MM-DDTHH:mm:ss"
                        range-separator="至"
                        start-placeholder="开始时间"
                        end-placeholder="结束时间"
                    />
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" @click="search">查询</el-button>
                    <el-button @click="reset">重置</el-button>
                    <el-button @click="load">刷新</el-button>
                </el-form-item>
            </el-form>
            <el-table
                v-loading="loading"
                :data="records"
                border
                row-key="id"
                empty-text="暂无同步记录"
                @row-click="openDetail"
            >
                <el-table-column label="状态" width="105">
                    <template #default="{ row }">
                        <el-tag :type="statusTagType(row.status)">{{ row.status }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="类型" width="85">
                    <template #default="{ row }">{{ row.assetType === 'STOCK' ? '股票' : row.assetType === 'INDEX' ? '指数' : '关系' }}</template>
                </el-table-column>
                <el-table-column prop="masterDataType" label="MasterDataType" min-width="145">
                    <template #default="{ row }">{{ row.masterDataType || '-' }}</template>
                </el-table-column>
                <el-table-column prop="bindingCode" label="来源绑定" min-width="120">
                    <template #default="{ row }">{{ valueOf(row.bindingCode) }}</template>
                </el-table-column>
                <el-table-column prop="triggerType" label="触发方式" width="100">
                    <template #default="{ row }">{{ valueOf(row.triggerType) }}</template>
                </el-table-column>
                <el-table-column prop="datasetCode" label="Dataset" min-width="145" />
                <el-table-column prop="sourceCode" label="来源" min-width="100">
                    <template #default="{ row }">{{ valueOf(row.sourceCode) }}</template>
                </el-table-column>
                <el-table-column prop="syncType" label="同步类型" width="100" />
                <el-table-column prop="totalCount" label="总数" width="80" />
                <el-table-column prop="addedCount" label="新增" width="80" />
                <el-table-column prop="updatedCount" label="更新" width="80" />
                <el-table-column prop="removedCount" label="失效" width="80" />
                <el-table-column prop="startedAt" label="开始时间" min-width="170">
                    <template #default="{ row }">{{ valueOf(row.startedAt) }}</template>
                </el-table-column>
                <el-table-column prop="publishedAt" label="发布时间" min-width="170">
                    <template #default="{ row }">{{ valueOf(row.publishedAt) }}</template>
                </el-table-column>
                <el-table-column label="操作" width="80" fixed="right">
                    <template #default="{ row }">
                        <el-button link type="primary" @click.stop="openDetail(row)">详情</el-button>
                    </template>
                </el-table-column>
            </el-table>
            <div class="pagination">
                <el-pagination
                    v-model:current-page="filters.pageNo"
                    v-model:page-size="filters.pageSize"
                    :total="total"
                    :page-sizes="[10, 20, 50]"
                    layout="total, sizes, prev, pager, next, jumper"
                    @size-change="search"
                    @current-change="load"
                />
            </div>
        </el-card>
        <SyncBatchDetailDrawer
            v-model="detailOpen"
            :batch="selectedBatch"
            :republishing="republishing"
            @republish="republish"
        />
    </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { querySyncBatches, republishSyncBatch } from '/@/api/masterdata/masterDataApi'
import SyncBatchDetailDrawer from './SyncBatchDetailDrawer.vue'
import type { SyncBatch, SyncBatchQuery } from '/@/types/masterdata'
import {
    statusTagType,
    SYNC_BATCH_STATUS_OPTIONS,
    SYNC_TYPE_OPTIONS
} from '/@/types/masterdata'
const route = useRoute()

// 同步页同样保持局部状态；重新发布 loading 与当前抽屉选择分离，避免重复提交。
const loading = ref(false), records = ref<SyncBatch[]>([]), total = ref(0), detailOpen = ref(false), selectedBatch = ref<SyncBatch | null>(null), republishing = ref(false)
const timeRange = ref<string[]>([])
const filters = reactive<SyncBatchQuery>({
    masterDataType: undefined,
    sourceCode: '',
    datasetCode: '',
    syncType: undefined,
    status: undefined,
    startTime: '',
    endTime: '',
    pageNo: 1,
    pageSize: 20
})
const valueOf = (value: unknown) => value === null || value === undefined || value === '' ? '-' : String(value)

// DatePicker 只负责输入格式转换，实际时间范围过滤完全由后端完成。
watch(timeRange, value => {
    filters.startTime = value?.[0] || '';
    filters.endTime = value?.[1] || ''
})

/** 若抽屉仍打开，刷新后以服务端返回的新批次状态覆盖旧对象。 */
async function load(): Promise<void> {
    loading.value = true;
    try {
        const result = await querySyncBatches(filters);
        records.value = (result.records || []).map(item => ({
            ...item,
            assetType: item.masterDataType === 'MARKET_ASSET_STOCK' ? 'STOCK' : item.masterDataType === 'MARKET_ASSET_INDEX' ? 'INDEX' : undefined
        }));
        total.value = result.total || 0;
        if (selectedBatch.value)
            selectedBatch.value = records.value.find(item => item.id === selectedBatch.value?.id) || selectedBatch.value
    }
    finally {
        loading.value = false
    }
}

function search(): void {
    filters.pageNo = 1;
    load()
}

function reset(): void {
    Object.assign(filters, {
        masterDataType: undefined,
        sourceCode: '',
        datasetCode: '',
        syncType: undefined,
        status: undefined,
        startTime: '',
        endTime: '',
        pageNo: 1
    });
    timeRange.value = [];
    load()
}

function openDetail(batch: SyncBatch): void {
    selectedBatch.value = batch;
    detailOpen.value = true
}

/** 成功后一定重新读取后端结果，不修改本地行来伪造发布成功。 */
async function republish(batch: SyncBatch): Promise<void> {
    if (republishing.value)
        return;
    republishing.value = true;
    try {
        await republishSyncBatch(batch.id);
        ElMessage.success('已提交重新发布');
        await load()
    }
    finally {
        republishing.value = false
    }
}

onMounted(() => {
    const masterDataType = route.query.masterDataType;
    if (typeof masterDataType === 'string')
        filters.masterDataType = masterDataType;
    load()
})
</script>

<style scoped>
.page {
    padding: 16px;
}

.hint {
    margin-top: 6px;
    color: var(--el-text-color-secondary);
    font-size: 13px;
}

.pagination {
    display: flex;
    justify-content: flex-end;
    margin-top: 16px;
}
</style>