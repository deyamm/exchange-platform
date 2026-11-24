<template>
    <div class="page">
        <el-card shadow="never">
            <template #header><b>主数据</b></template>
            <el-form inline @submit.prevent>
                <el-form-item label="关键字">
                    <el-input v-model="keyword" clearable placeholder="主数据类型、名称或来源数据集" @keyup.enter="search" />
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" @click="search">查询</el-button>
                    <el-button @click="reset">重置</el-button>
                </el-form-item>
            </el-form>

            <el-table v-loading="loading" :data="records" border empty-text="暂无已发布成功的主数据">
                <el-table-column prop="typeCode" label="MasterDataType" min-width="180" />
                <el-table-column prop="displayName" label="主数据名称" min-width="150" />
                <el-table-column prop="datasetName" label="来源数据集名称" min-width="180">
                    <template #default="{ row }">{{ valueOf(row.datasetName) }}</template>
                </el-table-column>
                <el-table-column prop="datasetCode" label="来源数据集编码" min-width="170" />
                <el-table-column prop="totalCount" label="数据总条数" width="120" align="right" />
                <el-table-column label="最近发布时间" min-width="170">
                    <template #default="{ row }">{{ valueOf(row.publishedAt) }}</template>
                </el-table-column>
                <el-table-column label="操作" width="90" fixed="right">
                    <template #default="{ row }">
                        <el-button link type="primary" @click="openDetail(row.typeCode)">详情</el-button>
                    </template>
                </el-table-column>
            </el-table>
        </el-card>
    </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { queryPublishedMasterDataTypes } from '/@/api/masterdata/masterDataApi'
import type { MasterDataTypeCode, PublishedMasterDataType } from '/@/types/masterdata'

const router = useRouter()
const keyword = ref('')
const loading = ref(false)
const records = ref<PublishedMasterDataType[]>([])

const valueOf = (value: unknown) => value === null || value === undefined || value === '' ? '-' : String(value)

async function load(): Promise<void> {
    loading.value = true
    try {
        records.value = await queryPublishedMasterDataTypes(keyword.value)
    }
    finally {
        loading.value = false
    }
}

function search(): void {
    load()
}

function reset(): void {
    keyword.value = ''
    load()
}

function openDetail(typeCode: MasterDataTypeCode): void {
    router.push({ name: 'masterDataPublishedDetail', params: { typeCode } })
}

onMounted(load)
</script>

<style scoped>
.page { padding: 16px; }
</style>
