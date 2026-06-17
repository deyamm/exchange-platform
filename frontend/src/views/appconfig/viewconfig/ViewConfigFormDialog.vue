<template>
    <el-dialog :model-value="modelValue" :title="mode === 'create' ? '新增视图配置' : '编辑视图配置'" width="760px" destroy-on-close
        @close="handleClose">
        <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
            <el-form-item label="视图编码" prop="viewCode">
                <el-input v-model="form.viewCode" :disabled="mode === 'edit'" placeholder="如 TARGET_DETAIL_DEFAULT" />
            </el-form-item>
            <el-form-item label="视图名称" prop="viewName">
                <el-input v-model="form.viewName" />
            </el-form-item>
            <el-form-item label="关联场景">
                <el-tree-select v-model="form.sceneCode" :data="sceneOptions" :loading="optionLoading" :props="treeSelectProps" node-key="value" value-key="value" clearable placeholder="请选择所属场景" style="width: 100%" />
            </el-form-item>
            <el-form-item label="视图类型">
                <el-select v-model="form.viewType" placeholder="请选择视图类型" style="width: 100%">
                    <el-option v-for="item in VIEW_TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
            </el-form-item>
            <el-form-item label="默认视图">
                <el-switch v-model="form.defaultFlag" />
            </el-form-item>
            <el-form-item label="字段配置JSON">
                <el-input v-model="fieldsText" type="textarea" :rows="5"
                    placeholder='[{"metricCode":"CLOSE_PRICE","fieldName":"收盘价","sortNo":10,"visible":true}]' /></el-form-item>
            <el-form-item label="排序配置JSON">
                <el-input v-model="sortRulesText" type="textarea" :rows="3" placeholder='[{"fieldCode":"tradeDate","direction":"DESC"}]' />
            </el-form-item>
            <el-form-item label="筛选配置JSON">
                <el-input v-model="filterConfigText" type="textarea" :rows="3" placeholder='{"entries":["marketCode","groupCode"]}' />
            </el-form-item>
            <el-form-item label="状态">
                <el-select v-model="form.status" style="width:180px">
                    <el-option v-for="item in COMMON_STATUS_OPTIONS" :key="item.value" :label="item.label"
                        :value="item.value" />
                </el-select>
            </el-form-item>
        </el-form>
        <template #footer><el-button @click="handleClose">取消</el-button><el-button type="primary" :loading="saving"
                @click="handleSubmit">保存</el-button></template>
    </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import { ElMessage } from 'element-plus';
import { saveOrUpdateViewConfig } from '/@/api/viewconfig/viewConfigApi';
import { createDefaultViewConfig, type ViewConfig } from '/@/types/viewconfig/entity';
import { COMMON_STATUS_OPTIONS, SCENE_TYPE_TEXT } from '/@/types/appscene/view';
import { getAllEnabledAppSceneList } from '/@/api/appscene/appSceneApi';
import { VIEW_TYPE_OPTIONS } from '/@/types/viewconfig/view';

interface TreeSelectOption {
    value: string
    label: string
    disabled?: boolean
    children?: TreeSelectOption[]
}

const treeSelectProps = {
    value: 'value',
    label: 'label',
    children: 'children',
    disabled: 'disabled'
}

const props = withDefaults(
    defineProps<{ 
        modelValue: boolean; 
        mode?: 'create' | 'edit'; 
        formData?: ViewConfig 
    }>(), { 
        mode: 'create', 
        formData: () => createDefaultViewConfig() 
    }
);

const emit = defineEmits<{ 
    (e: 'update:modelValue', value: boolean): void; 
    (e: 'success'): void 
}>();

const formRef = ref<FormInstance>(); 
const saving = ref(false); 
const form = reactive(createDefaultViewConfig());
const fieldsText = ref('[]'); 
const sortRulesText = ref('[]'); 
const optionLoading = ref(false);
const sceneOptions = ref<TreeSelectOption[]>([]);
const filterConfigText = ref('{}');
const rules: FormRules = { 
    viewCode: [{ required: true, message: '请输入视图编码', trigger: 'blur' }], 
    viewName: [{ required: true, message: '请输入视图名称', trigger: 'blur' }] 
};

watch(
    () => props.modelValue, 
    async (visible) => { 
        if (visible) { 
            Object.assign(form, createDefaultViewConfig(), props.formData || {}); 
            fieldsText.value = JSON.stringify(form.fields ?? [], null, 2); 
            sortRulesText.value = JSON.stringify(form.sortRules ?? [], null, 2); 
            filterConfigText.value = JSON.stringify(form.filterConfig ?? {}, null, 2);
            await loadSceneOptions(); 
        } 
    }
);

function parseJson(text: string, fallback: unknown) { 
    try { 
        return text ? JSON.parse(text) : fallback; 
    } catch { 
        return fallback; 
    } 
}
function handleClose() { 
    emit('update:modelValue', false); 
}

async function handleSubmit() { 
    await formRef.value?.validate(); 
    saving.value = true; 
    try { 
        await saveOrUpdateViewConfig({ 
            ...form, 
            fields: parseJson(fieldsText.value, []), 
            sortRules: parseJson(sortRulesText.value, []), 
            filterConfig: parseJson(filterConfigText.value, {}) 
        } as any); 
        ElMessage.success('保存成功'); 
        emit('success'); 
        handleClose(); 
    } finally { 
        saving.value = false; 
    } 
}

/**
 * 加载已启用场景选项列表，用于下拉选择，读取的原始数据为 AppScene[]，需要以AppSceneType为维度转换成 TreeSelectOption[] 结构
 */
async function loadSceneOptions() {
    optionLoading.value = true;
    // 以AppSceneType为维度构建初始的场景类型选项结构，且该节点不可选，仅作为分组展示
    for (const sceneType in SCENE_TYPE_TEXT) {
        sceneOptions.value.push({
            value: sceneType,
            label: SCENE_TYPE_TEXT[sceneType as keyof typeof SCENE_TYPE_TEXT],
            disabled: true,
            children: []
        });
    }
    try {
        const scenes = await getAllEnabledAppSceneList();
        for (const scene of scenes) {
            const parentOption = sceneOptions.value.find(opt => opt.value === scene.sceneType);
            if (parentOption) {
                parentOption.children?.push({
                    value: scene.sceneCode,
                    label: scene.sceneName,
                    disabled: false
                });
            }
        }
    } finally {
        optionLoading.value = false;
    }
}
</script>
