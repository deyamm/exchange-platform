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
                    <el-descriptions-item v-for="(value, field) in detail.attributes" :key="field" :label="String(field)">
                        {{ valueOf(value) }}
                    </el-descriptions-item>
                </el-descriptions>
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
import { getMasterDataAsset } from '/@/api/masterdata/masterDataApi'
import type { MasterDataAsset } from '/@/types/masterdata'
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
const activeTab = ref('basic')
const valueOf = (value: unknown) => value === null || value === undefined || value === '' ? '-' : String(value)
const assetTypeText = (type: string) => type === 'STOCK' ? '股票' : '指数'

/** 每次打开都以正式详情接口为准，避免将列表中的摘要误当作详情数据。 */
watch(() => [props.modelValue, props.asset] as const, async ([opened, asset]) => {
    if (!opened || !asset)
        return
    activeTab.value = 'basic'
    detail.value = await getMasterDataAsset(asset.marketAssetType, asset.assetCode)
}, {
    deep: true
})

</script>

<style scoped>
.sync-link {
    margin-top: 12px;
}
</style>
