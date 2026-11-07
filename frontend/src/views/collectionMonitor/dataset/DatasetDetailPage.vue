<template>
    <div class="page" v-loading="loading">
        <el-button @click="$router.back()">返回</el-button>
        <template v-if="detail">
            <el-card class="card" header="基础信息">
                <el-descriptions border :column="3">
                    <el-descriptions-item label="编码">
                        {{ detail.dataset.datasetCode }}
                    </el-descriptions-item>
                    <el-descriptions-item label="名称">
                        {{ detail.dataset.datasetName }}
                    </el-descriptions-item>
                    <el-descriptions-item label="状态">
                        {{ detail.dataset.datasetStatus }}
                    </el-descriptions-item>
                    <el-descriptions-item label="数据类型">
                        {{ detail.dataset.dataTypeCode }} / V{{ detail.dataset.dataTypeVersionNo }}
                    </el-descriptions-item>
                    <el-descriptions-item label="物理表">
                        {{ detail.dataset.schemaName }}.{{ detail.dataset.tableName }}
                    </el-descriptions-item>
                    <el-descriptions-item label="最近执行">
                        {{ detail.dataset.lastSuccessTime }}
                    </el-descriptions-item>
                </el-descriptions>
            </el-card>

            <el-card class="card" header="字段结构">
                <el-table :data="detail.structure || []" border>
                    <el-table-column prop="fieldCode" label="字段编码" />
                    <el-table-column prop="fieldName" label="字段名称" />
                    <el-table-column prop="fieldType" label="类型" />
                    <el-table-column prop="required" label="必填" />
                </el-table>
            </el-card>

            <el-card class="card">
                <template #header>
                    <span>扩展元数据</span>
                    <el-button class="edit" type="primary" link @click="editing = true">维护</el-button>
                </template>
                <el-table :data="detail.metadata || []" border>
                    <el-table-column prop="metadataGroup" label="分组" />
                    <el-table-column prop="metadataName" label="名称" />
                    <el-table-column prop="metadataValue" label="值" />
                </el-table>
            </el-card>

            <el-dialog v-model="editing" title="维护扩展元数据" width="720px">
                <el-form label-width="90px">
                    <el-form-item label="数据集名称">
                        <el-input v-model="editableName" />
                    </el-form-item>
                    <el-form-item label="数据集说明">
                        <el-input v-model="editableDesc" type="textarea" />
                    </el-form-item>
                </el-form>
                <el-table :data="editable" border>
                    <el-table-column prop="metadataGroup" label="分组" />
                    <el-table-column prop="metadataKey" label="编码" />
                    <el-table-column prop="metadataName" label="名称" />
                    <el-table-column label="值">
                        <template #default="{ row }">
                            <el-input v-model="row.metadataValue" />
                        </template>
                    </el-table-column>
                </el-table>
                <template #footer>
                    <el-button @click="editing = false">取消</el-button>
                    <el-button type="primary" @click="save">保存</el-button>
                </template>
            </el-dialog>
        </template>
    </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { getDatasetDetail, updateDatasetMetadata } from '/@/api/monitor/queryApi';

const loading = ref(false),
    detail = ref<any>(),
    editing = ref(false),
    editable = ref<any[]>([]),
    editableName = ref(''),
    editableDesc = ref('');

const route = useRoute();

async function load() {
    loading.value = true;
    try {
        detail.value = await getDatasetDetail(String(route.params.datasetCode));
        editable.value = (detail.value.metadata || [])
            .filter((x: any) => x.metadataGroup !== 'STORAGE')
            .map((x: any) => ({ ...x }));
        editableName.value = detail.value.dataset.datasetName || '';
        editableDesc.value = detail.value.dataset.description || ''
    } finally {
        loading.value = false
    }
}

async function save() {
    await updateDatasetMetadata(String(route.params.datasetCode), {
        datasetName: editableName.value,
        datasetDesc: editableDesc.value,
        items: editable.value
    });
    editing.value = false;
    ElMessage.success('元数据已保存');
    await load()
}

onMounted(load)
</script>

<style scoped>
.page {
    padding: 16px
}

.card {
    margin-top: 16px
}

.edit {
    float: right
}
</style>