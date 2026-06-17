<template>
    <el-dialog
        :model-value="modelValue"
        :title="mode === 'create' ? '新增关注标的' : '编辑关注标的'"
        width="640px"
        destroy-on-close
        @close="handleClose"
    >
        <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
            <el-form-item label="标的编码" prop="targetCode">
                <el-input
                    v-model="form.targetCode"
                    :disabled="mode === 'edit'"
                    placeholder="如 000001.SZ"
                />
            </el-form-item>
            <el-form-item label="标的名称" prop="targetName">
                <el-input v-model="form.targetName" />
            </el-form-item>
            <el-form-item label="标的类型">
                <el-select v-model="form.targetType" style="width: 180px">
                    <el-option
                        v-for="item in ASSET_TYPE_OPTIONS"
                        :key="item.value"
                        :label="item.label"
                        :value="item.value"
                    />
                </el-select>
            </el-form-item>
            <el-form-item label="市场编码">
                <el-input v-model="form.marketCode" />
            </el-form-item>
            <el-form-item label="分组编码" prop="groupCodes">
                <el-input
                    v-model="groupCodeText"
                    :disabled="Boolean(lockedGroupCode)"
                    placeholder="请输入分组编码"
                />
            </el-form-item>
            <el-form-item label="排序号"><el-input-number v-model="form.sortNo" :min="1" :max="999999"
                    controls-position="right" /></el-form-item>
            <el-form-item label="重点标记">
                <el-switch v-model="form.importantFlag" />
            </el-form-item>
            <el-form-item label="观察状态">
                <el-select v-model="form.watchStatus" style="width: 180px">
                    <el-option
                        v-for="item in WATCH_STATUS_OPTIONS"
                        :key="item.value"
                        :label="item.label"
                        :value="item.value"
                    />
                </el-select>
            </el-form-item>
            <el-form-item label="关注理由">
                <el-input
                    v-model="form.watchReason"
                    type="textarea"
                    :rows="4"
                    maxlength="500"
                    show-word-limit
                />
            </el-form-item>
        </el-form>
        <template #footer>
            <el-button @click="handleClose">取消</el-button>
            <el-button type="primary" :loading="saving" @click="handleSubmit">保存</el-button>
        </template>
    </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import { ElMessage } from 'element-plus';
import { saveOrUpdateWatchTarget } from '/@/api/watchtarget/watchTargetApi';
import { createDefaultWatchTarget, type WatchTarget, type TargetGroup } from '/@/types/watchtarget/entity';
import { ASSET_TYPE_OPTIONS, WATCH_STATUS_OPTIONS } from '/@/types/watchtarget/view';
import { WatchTargetSaveRequest } from '/@/types/watchtarget/constants';

const props = withDefaults(
    defineProps<{
        modelValue: boolean;
        mode?: 'create' | 'edit';
        formData?: WatchTarget;
        lockedGroup?: TargetGroup | null;
    }>(),
    {
        mode: 'create',
        formData: () => createDefaultWatchTarget(),
        lockedGroup: null,
    },
);

const emit = defineEmits<{
    (e: 'update:modelValue', value: boolean): void;
    (e: 'success'): void;
}>();

const formRef = ref<FormInstance>();
const saving = ref(false);
const form = ref<WatchTargetSaveRequest>({} as WatchTargetSaveRequest);

const lockedGroupCode = computed(() => props.lockedGroup?.groupCode || '');

const groupCodeText = computed({
    get() {
        if (props.lockedGroup?.groupCode) {
            return props.lockedGroup.groupName
                ? `${props.lockedGroup.groupName}（${props.lockedGroup.groupCode}）`
                : props.lockedGroup.groupCode;
        }
        return normalizeGroupCodes(form.value.targetGroups)[0] || '';
    },
    set(value: string) {
        form.value.targetGroups = value?.trim() ? [value.trim()] : [];
    },
});

const rules: FormRules = {
    targetCode: [{ required: true, message: '请输入标的编码', trigger: 'blur' }],
    targetName: [{ required: true, message: '请输入标的名称', trigger: 'blur' }],
    groupCodes: [
        {
            validator: (_rule, _value, callback) => {
                applyLockedGroup();
                if (normalizeGroupCodes(form.value.targetGroups).length > 0) {
                    callback();
                } else {
                    callback(new Error('请设置分组编码'));
                }
            },
            trigger: 'blur',
        },
    ],
};

watch(
    () => props.modelValue,
    (visible) => {
        if (visible) {
            Object.assign(form.value, createDefaultWatchTarget(), props.formData || {});
            form.value.targetGroups = normalizeGroupCodes(form.value.targetGroups);
            applyLockedGroup();
        }
    },
);

function normalizeGroupCodes(groups?: Array<string | TargetGroup>) {
    if (!groups || !Array.isArray(groups)) {
        return [];
    }

    return Array.from(
        new Set(
            groups
                .map((item) => (typeof item === 'string' ? item : item?.groupCode))
                .filter((code): code is string => Boolean(code)),
        ),
    );
}

function applyLockedGroup() {
    if (!lockedGroupCode.value) {
        form.value.targetGroups = normalizeGroupCodes(form.value.targetGroups);
        return;
    }

    const codes = normalizeGroupCodes(form.value.targetGroups);
    if (!codes.includes(lockedGroupCode.value)) {
        codes.unshift(lockedGroupCode.value);
    }
    form.value.targetGroups = codes;
}

function handleClose() {
    emit('update:modelValue', false);
}

async function handleSubmit() {
    applyLockedGroup();
    await formRef.value?.validate();
    saving.value = true;
    try {
        await saveOrUpdateWatchTarget(form.value);
        ElMessage.success('保存成功');
        emit('success');
        handleClose();
    } finally {
        saving.value = false;
    }
}
</script>