<template>
    <el-drawer v-model="visibleModel" title="主数据详情" size="680px" destroy-on-close>
        <el-empty v-if="!detail" description="暂无主数据详情" />
        <el-tabs v-else v-model="activeTab">
            <el-tab-pane label="基础信息" name="basic">
                <el-descriptions :column="2" border>
                    <el-descriptions-item label="市场资产类型">{{ assetTypeText(detail.marketAssetType) }}</el-descriptions-item>
                    <el-descriptions-item label="资产代码">{{ valueOf(detail.assetCode) }}</el-descriptions-item>
                    <el-descriptions-item label="资产名称">{{ valueOf(detail.assetName) }}</el-descriptions-item>
                    <el-descriptions-item label="状态">
                        <el-tag :type="statusTagType(detail.status)">{{ detail.status }}</el-tag>
                    </el-descriptions-item>
                    <el-descriptions-item label="创建时间">{{ valueOf(detail.createdAt) }}</el-descriptions-item>
                    <el-descriptions-item label="更新时间">{{ valueOf(detail.updatedAt) }}</el-descriptions-item>
                </el-descriptions>
            </el-tab-pane>
            <el-tab-pane label="类型属性" name="attributes">
                <el-descriptions :column="2" border>
                    <template v-if="detail.marketAssetType === 'STOCK'">
                        <el-descriptions-item label="市场">{{ valueOf(detail.marketType) }}</el-descriptions-item>
                        <el-descriptions-item label="交易所">{{ valueOf(detail.exchangeCode) }}</el-descriptions-item>
                        <el-descriptions-item label="币种">{{ valueOf(detail.currencyCode) }}</el-descriptions-item>
                        <el-descriptions-item label="上市日期">{{ valueOf(detail.listDate) }}</el-descriptions-item>
                        <el-descriptions-item label="退市日期">{{ valueOf(detail.delistDate) }}</el-descriptions-item>
                    </template>
                    <template v-else>
                        <el-descriptions-item label="发布方">{{ valueOf(detail.publisherCode) }}</el-descriptions-item>
                        <el-descriptions-item label="指数分类">{{ valueOf(detail.indexCategory) }}</el-descriptions-item>
                        <el-descriptions-item label="业务体系">{{ valueOf(detail.schemeCode) }}</el-descriptions-item>
                        <el-descriptions-item label="Level">{{ valueOf(detail.level) }}</el-descriptions-item>
                        <el-descriptions-item label="交易所">{{ valueOf(detail.exchangeCode) }}</el-descriptions-item>
                        <el-descriptions-item label="发布日期">{{ valueOf(detail.launchDate) }}</el-descriptions-item>
                        <el-descriptions-item label="终止日期">{{ valueOf(detail.endDate) }}</el-descriptions-item>
                    </template>
                </el-descriptions>
            </el-tab-pane>
            <el-tab-pane v-if="detail.marketAssetType === 'INDEX'" label="成分股" name="members">
                <el-table v-loading="membersLoading" :data="members" border empty-text="暂无当前有效成分股">
                    <el-table-column prop="memberAssetCode" label="股票代码" min-width="130" />
                    <el-table-column prop="memberAssetName" label="股票名称" min-width="140">
                        <template #default="{ row }">{{ valueOf(row.memberAssetName) }}</template>
                    </el-table-column>
                    <el-table-column prop="memberMarketType" label="市场" width="110">
                        <template #default="{ row }">{{ valueOf(row.memberMarketType) }}</template>
                    </el-table-column>
                    <el-table-column prop="validFrom" label="生效时间" min-width="170">
                        <template #default="{ row }">{{ valueOf(row.validFrom) }}</template>
                    </el-table-column>
                </el-table>
            </el-tab-pane>
            <el-tab-pane label="同步信息" name="sync">
                <el-alert
                    title="当前后端未提供按资产查询的同步历史接口；可进入同步记录页查看对应资产类型的发布批次。"
                    type="info"
                    :closable="false"
                    show-icon
                />
                <el-button class="sync-link" type="primary" link @click="$emit('view-sync', detail)">查看完整同步记录</el-button>
            </el-tab-pane>
        </el-tabs>
    </el-drawer>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { batchGetMasterDataAssets, getMasterDataAsset, queryIndexMembers } from '/@/api/masterdata/masterDataApi'
import type { AssetMemberDisplay, MasterDataAsset } from '/@/types/masterdata'
import { statusTagType } from '/@/types/masterdata'

const props = defineProps<{
    modelValue: boolean;
    asset?: MasterDataAsset | null
}>()
const emit = defineEmits<{
    (event: 'update:modelValue', value: boolean): void;
    (event: 'view-sync', asset: MasterDataAsset): void
}>()
const visibleModel = computed({
    get: () => props.modelValue,
    set: value => emit('update:modelValue', value)
})
const detail = ref<MasterDataAsset | null>(null)
const members = ref<AssetMemberDisplay[]>([])
const membersLoading = ref(false)
const activeTab = ref('basic')
const valueOf = (value: unknown) => value === null || value === undefined || value === '' ? '-' : String(value)
const assetTypeText = (type: string) => type === 'STOCK' ? '股票' : '指数'

/** 每次打开都以正式详情接口为准，避免将列表中的摘要误当作详情数据。 */
watch(() => [props.modelValue, props.asset] as const, async ([opened, asset]) => {
    if (!opened || !asset)
        return
    activeTab.value = 'basic'
    members.value = []
    detail.value = await getMasterDataAsset(asset.marketAssetType, asset.assetCode)
    if (detail.value.marketAssetType === 'INDEX')
        await loadMembers(detail.value.assetCode)
}, {
    deep: true
})

async function loadMembers(indexCode: string): Promise<void> {
    membersLoading.value = true
    try {
        // members 只代表当前有效关系；不传 asOfTime，因此不会在页面虚构历史成员查询能力。
        const relations = await queryIndexMembers(indexCode)
        // 关系返回的是代码，名称/市场必须从正式资产 API 取得，不能由前端规则推导。
        const stocks = relations.length ? await batchGetMasterDataAssets(relations.map(item => item.memberAssetCode)) : []
        const stockMap = new Map(stocks.map(item => [item.assetCode, item]))
        members.value = relations.map(item => ({
            ...item,
            memberAssetName: stockMap.get(item.memberAssetCode)?.assetName,
            memberMarketType: stockMap.get(item.memberAssetCode)?.marketType,
        }))
    }
    finally {
        membersLoading.value = false
    }
}
</script>

<style scoped>
.sync-link {
    margin-top: 12px;
}
</style>