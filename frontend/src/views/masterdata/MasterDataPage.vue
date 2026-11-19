<template>
    <div class="page">
        <el-card shadow="never">
            <template #header>
                <div>
                    <b>证券主数据</b>
                    <div class="hint">查询平台当前正式生效的股票与指数主数据</div>
                </div>
            </template>
            <el-form :model="filters" inline>
                <el-form-item label="市场资产类型">
                    <el-select
                        v-model="filters.marketAssetType"
                        clearable
                        placeholder="全部"
                        style="width: 120px"
                        @change="handleAssetTypeChange"
                    >
                        <el-option
                            v-for="item in MASTER_DATA_ASSET_TYPE_OPTIONS"
                            :key="item.value"
                            :label="item.label"
                            :value="item.value"
                        />
                    </el-select>
                </el-form-item>
                <el-form-item label="关键字">
                    <el-input v-model="filters.keyword" clearable placeholder="代码或名称" @keyup.enter="search" />
                </el-form-item>
                <el-form-item label="状态">
                    <el-select v-model="filters.status" clearable placeholder="全部" style="width: 110px">
                        <el-option
                            v-for="item in MASTER_DATA_STATUS_OPTIONS"
                            :key="item.value"
                            :label="item.label"
                            :value="item.value"
                        />
                    </el-select>
                </el-form-item>
                <template v-if="filters.marketAssetType === 'STOCK'">
                    <el-form-item label="市场">
                        <el-select v-model="filters.marketType" clearable style="width: 120px">
                            <el-option
                                v-for="item in STOCK_MARKET_TYPE_OPTIONS"
                                :key="item.value"
                                :label="item.label"
                                :value="item.value"
                            />
                        </el-select>
                    </el-form-item>
                    <el-form-item label="交易所">
                        <el-input v-model="filters.exchangeCode" clearable />
                    </el-form-item>
                </template>
                <template v-if="filters.marketAssetType === 'INDEX'">
                    <el-form-item label="发布方">
                        <el-select v-model="filters.publisherCode" clearable style="width: 110px">
                            <el-option
                                v-for="item in PUBLISHER_CODE_OPTIONS"
                                :key="item.value"
                                :label="item.label"
                                :value="item.value"
                            />
                        </el-select>
                    </el-form-item>
                    <el-form-item label="指数分类">
                        <el-select v-model="filters.indexCategory" clearable style="width: 110px">
                            <el-option
                                v-for="item in INDEX_CATEGORY_OPTIONS"
                                :key="item.value"
                                :label="item.label"
                                :value="item.value"
                            />
                        </el-select>
                    </el-form-item>
                    <el-form-item label="业务体系">
                        <el-input v-model="filters.schemeCode" clearable />
                    </el-form-item>
                    <el-form-item label="Level">
                        <el-input-number
                            v-model="filters.level"
                            :min="1"
                            :precision="0"
                            controls-position="right"
                        />
                    </el-form-item>
                    <el-form-item label="交易所">
                        <el-input v-model="filters.exchangeCode" clearable />
                    </el-form-item>
                </template>
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
                empty-text="暂无符合条件的主数据"
                @row-click="openDetail"
            >
                <el-table-column label="类型" width="90">
                    <template #default="{ row }">{{ row.marketAssetType === 'STOCK' ? '股票' : '指数' }}</template>
                </el-table-column>
                <el-table-column prop="assetCode" label="代码" min-width="130" />
                <el-table-column prop="assetName" label="名称" min-width="150" />
                <el-table-column
                    v-if="filters.marketAssetType !== 'INDEX'"
                    prop="marketType"
                    label="市场"
                    width="110"
                >
                    <template #default="{ row }">{{ valueOf(row.marketType) }}</template>
                </el-table-column>
                <el-table-column
                    v-if="filters.marketAssetType !== 'STOCK'"
                    prop="publisherCode"
                    label="发布方"
                    width="100"
                >
                    <template #default="{ row }">{{ valueOf(row.publisherCode) }}</template>
                </el-table-column>
                <el-table-column label="状态" width="100">
                    <template #default="{ row }">
                        <el-tag :type="statusTagType(row.status)">{{ row.status }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="更新时间" min-width="170">
                    <template #default="{ row }">{{ valueOf(row.updatedAt) }}</template>
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
        <MasterDataDetailDrawer v-model="detailOpen" :asset="selectedAsset" @view-sync="goSync" />
    </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { queryMasterDataAssets } from '/@/api/masterdata/masterDataApi'
import MasterDataDetailDrawer from './MasterDataDetailDrawer.vue'
import type { MasterDataAsset, MasterDataAssetQuery } from '/@/types/masterdata'
import {

    INDEX_CATEGORY_OPTIONS,
    MASTER_DATA_ASSET_TYPE_OPTIONS,
    MASTER_DATA_STATUS_OPTIONS,
    PUBLISHER_CODE_OPTIONS,
    statusTagType,
    STOCK_MARKET_TYPE_OPTIONS
} from '/@/types/masterdata'
const router = useRouter()

// 页面状态仅在当前页面管理：筛选条件、分页、加载状态及当前抽屉资产均不会写入全局 store。
const loading = ref(false), records = ref<MasterDataAsset[]>([]), total = ref(0), detailOpen = ref(false), selectedAsset = ref<MasterDataAsset | null>(null)

// ACTIVE 是产品默认筛选值；clearable 允许用户显式查看全部状态。
const filters = reactive<MasterDataAssetQuery>({
    marketAssetType: undefined,
    keyword: '',
    status: 'ACTIVE',
    marketType: '',
    exchangeCode: '',
    publisherCode: '',
    indexCategory: '',
    schemeCode: '',
    level: undefined,
    pageNo: 1,
    pageSize: 20
})
const valueOf = (value: unknown) => value === null || value === undefined || value === '' ? '-' : String(value)

/** 参数原样交由主数据 API 处理；页面不在获得分页结果后进行任何二次筛选。 */
async function load(): Promise<void> {
    loading.value = true;
    try {
        const result = await queryMasterDataAssets(filters);
        records.value = result.records || [];
        total.value = result.total || 0
    }
    finally {
        loading.value = false
    }
}

function search(): void {
    filters.pageNo = 1;
    load()
}

/** 类型变更必须清空另一类型的私有过滤条件，避免把 STOCK / INDEX 语义混入同一次请求。 */
function handleAssetTypeChange(): void {
    filters.marketType = '';
    filters.exchangeCode = '';
    filters.publisherCode = '';
    filters.indexCategory = '';
    filters.schemeCode = '';
    filters.level = undefined;
    search()
}

/** 重置时恢复文档要求的 ACTIVE 默认值，并将分页返回首页。 */
function reset(): void {
    Object.assign(filters, {
        marketAssetType: undefined,
        keyword: '',
        status: 'ACTIVE',
        marketType: '',
        exchangeCode: '',
        publisherCode: '',
        indexCategory: '',
        schemeCode: '',
        level: undefined,
        pageNo: 1
    });
    load()
}

function openDetail(asset: MasterDataAsset): void {
    selectedAsset.value = asset;
    detailOpen.value = true
}

/** 同步信息不复制批次详情 UI，而是带资产类型进入唯一的同步记录页。 */
function goSync(asset: MasterDataAsset): void {
    detailOpen.value = false;
    router.push({
        path: '/masterData/syncBatches',
        query: {
            masterDataType: asset.marketAssetType === 'STOCK' ? 'MARKET_ASSET_STOCK' : 'MARKET_ASSET_INDEX'
        }
    })
}

onMounted(load)
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