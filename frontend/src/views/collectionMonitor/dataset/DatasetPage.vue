<template>
    <div class="page">
        <el-card>
            <el-form inline>
                <el-form-item label="编码">
                    <el-input v-model="query.datasetCode" clearable />
                </el-form-item>
                <el-form-item label="名称">
                    <el-input v-model="query.datasetName" clearable />
                </el-form-item>
                <el-form-item label="标的类型">
                    <el-select v-model="query.assetType" clearable placeholder="全部" style="width: 130px">
                        <el-option v-for="item in ASSET_TYPE_OPTIONS" :key="item.value" :label="item.label"
                            :value="item.value" />
                    </el-select>
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" @click="search">查询</el-button>
                </el-form-item>
            </el-form>

            <el-table v-loading="loading" :data="records" border>
                <el-table-column prop="datasetCode" label="数据集编码" min-width="180" />
                <el-table-column prop="datasetName" label="名称" min-width="160" />
                <el-table-column label="标的类型" min-width="100">
                    <template #default="{ row }">
                        {{ row.assetTypeName || ASSET_TYPE_TEXT[row.assetType as AssetType] || row.assetType || '—' }}
                    </template>
                </el-table-column>
                <el-table-column prop="dataTopicCode" label="数据主题" />
                <el-table-column prop="dataTypeCode" label="数据类型" />
                <el-table-column prop="tableName" label="物理表" />
                <el-table-column prop="lastSuccessTime" label="最近成功时间" min-width="170" />
                <el-table-column label="操作" width="90">
                    <template #default="{ row }">
                        <el-button link type="primary"
                            @click="$router.push(`/collectionMonitor/datasets/${row.datasetCode}`)">
                            详情
                        </el-button>
                    </template>
                </el-table-column>
            </el-table>

            <el-pagination v-model:current-page="query.pageNo" v-model:page-size="query.pageSize" :total="total"
                layout="total, prev, pager, next" @current-change="load" />
        </el-card>
    </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { queryDatasets } from '/@/api/monitor/queryApi'
import { ASSET_TYPE_OPTIONS, ASSET_TYPE_TEXT, type AssetType } from '/@/types/asset'
import type { Dataset } from '/@/types/execution/entity'
import type { DatasetQueryParams } from '/@/types/execution/constants'

const loading = ref(false),
    records = ref<Dataset[]>([]),
    total = ref(0)

const query = reactive<DatasetQueryParams>({
    datasetCode: '',
    datasetName: '',
    assetType: '',
    pageNo: 1,
    pageSize: 20
})

async function load() {
    loading.value = true;
    try {
        const r = await queryDatasets(query);
        records.value = r.records || [];
        total.value = r.total || 0
    } finally {
        loading.value = false
    }
}

function search() {
    query.pageNo = 1;
    load()
}

onMounted(load)
</script>

<style scoped>
.page {
    padding: 16px
}
</style>
