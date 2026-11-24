<template>
    <div v-loading="loading" class="page">
        <div class="page-toolbar"><el-button @click="$router.back()">返回</el-button></div>
        <template v-if="detail">
            <el-card class="card" shadow="never" header="主数据基础信息">
                <el-descriptions border :column="3">
                    <el-descriptions-item label="MasterDataType">{{ detail.typeCode }}</el-descriptions-item>
                    <el-descriptions-item label="主数据名称">{{ detail.displayName }}</el-descriptions-item>
                    <el-descriptions-item label="数据总条数">{{ detail.totalCount }}</el-descriptions-item>
                    <el-descriptions-item label="来源数据集名称">{{ valueOf(detail.datasetName) }}</el-descriptions-item>
                    <el-descriptions-item label="来源数据集编码">{{ detail.datasetCode }}</el-descriptions-item>
                    <el-descriptions-item label="最近发布时间">{{ valueOf(detail.publishedAt) }}</el-descriptions-item>
                </el-descriptions>
            </el-card>

            <el-card class="card" shadow="never" header="主数据字段结构">
                <el-table :data="detail.fields" border>
                    <el-table-column prop="fieldCode" label="字段编码" min-width="150" />
                    <el-table-column prop="fieldName" label="字段名称" min-width="140" />
                    <el-table-column prop="valueType" label="字段类型" width="120" />
                    <el-table-column label="必填" width="90">
                        <template #default="{ row }"><el-tag :type="row.required ? 'warning' : 'info'">{{ row.required ? '是' : '否' }}</el-tag></template>
                    </el-table-column>
                </el-table>
            </el-card>

            <el-card class="card" shadow="never" header="主数据记录">
                <el-table v-loading="rowsLoading" :data="rows" border empty-text="暂无主数据记录">
                    <el-table-column 
                        v-for="field in detail.fields" 
                        :key="field.fieldCode" 
                        :label="field.fieldName" 
                        :prop="field.fieldCode" 
                        min-width="150" 
                        show-overflow-tooltip
                    >
                        <template #default="{ row }">{{ valueOf(row[field.fieldCode]) }}</template>
                    </el-table-column>
                </el-table>
                <div class="pagination">
                    <el-pagination 
                        v-model:current-page="pagination.pageNo" 
                        v-model:page-size="pagination.pageSize" 
                        :total="total" 
                        :page-sizes="[10, 20, 50]" 
                        layout="total, sizes, prev, pager, next, jumper" 
                        @size-change="changePageSize" 
                        @current-change="loadRows" 
                    />
                </div>
            </el-card>
        </template>
        <el-empty v-else-if="!loading" description="暂无已发布成功的主数据详情" />
    </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getPublishedMasterDataType, queryPublishedMasterDataRows } from '/@/api/masterdata/masterDataApi'
import type { MasterDataTypeCode, PublishedMasterDataTypeDetail } from '/@/types/masterdata'

const route = useRoute()
const typeCode = computed(() => route.params.typeCode as MasterDataTypeCode)
const detail = ref<PublishedMasterDataTypeDetail | null>(null)
const rows = ref<Record<string, any>[]>([])
const loading = ref(false)
const rowsLoading = ref(false)
const total = ref(0)
const pagination = reactive({ pageNo: 1, pageSize: 20 })

const valueOf = (value: unknown) => {
    if (value === null || value === undefined || value === '') return '-'
    return typeof value === 'object' ? JSON.stringify(value) : String(value)
}

async function loadRows(): Promise<void> {
    if (!typeCode.value) return
    rowsLoading.value = true
    try {
        const result = await queryPublishedMasterDataRows(typeCode.value, pagination)
        rows.value = result.records || []
        console.log('rows.value', rows.value)
        total.value = result.total || 0
    }
    finally {
        rowsLoading.value = false
    }
}

async function load(): Promise<void> {
    if (!typeCode.value) return
    loading.value = true
    try {
        detail.value = await getPublishedMasterDataType(typeCode.value)
        pagination.pageNo = 1
        await loadRows()
    }
    finally {
        loading.value = false
    }
}

function changePageSize(): void {
    pagination.pageNo = 1
    loadRows()
}

watch(typeCode, load, { immediate: true })
</script>

<style scoped>
.page { padding: 16px; }
.page-toolbar { margin-bottom: 16px; }
.card { margin-bottom: 16px; }
.pagination { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
