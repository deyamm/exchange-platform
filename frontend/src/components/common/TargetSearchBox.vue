<template>
    <!-- 根容器：同时作为候选下拉面板 absolute 定位的参照物。 -->
    <div class="stock-search-box">
        <!-- 搜索输入区：文本框和放大镜按钮横向排列。 -->
        <div class="search-input-row">
            <!-- 输入框：每次 input 都会触发检索，并支持键盘导航。 -->
            <el-input
                v-model="keyword"
                :placeholder="placeholder"
                clearable
                autocomplete="off"
                @input="handleInput"
                @focus="handleFocus"
                @blur="handleBlur"
                @keydown.down.prevent="moveActive(1)"
                @keydown.up.prevent="moveActive(-1)"
                @keydown.enter.prevent="handleEnter"
                @keydown.esc.prevent="closeDropdown"
            />

            <!-- 放大镜按钮：点击时复用 search()，再次检索当前输入内容。 -->
            <el-button
                type="primary"
                class="search-button"
                :loading="loading"
                :icon="Search"
                aria-label="搜索股票"
                @mousedown.prevent
                @click="handleSearchButtonClick"
            />
        </div>

        <!-- 候选下拉区域：输入框有焦点且内容非空时显示。 -->
        <div
            v-if="dropdownVisible"
            class="candidate-dropdown"
            @mousedown.prevent
        >
            <!-- 请求处理中显示加载状态。 -->
            <div v-if="loading" class="dropdown-state">
                正在检索...
            </div>

            <template v-else>
                <!-- 一条候选股票；点击后将对应 WatchTarget 返回父组件。 -->
                <button
                    v-for="(target, index) in candidates"
                    :key="`${target.marketCode}-${target.targetCode}`"
                    type="button"
                    class="candidate-item"
                    :class="{ 'is-active': index === activeIndex }"
                    @mouseenter="activeIndex = index"
                    @click="selectTarget(target)"
                >
                    <span class="candidate-code">{{ target.targetCode }}</span>
                    <span class="candidate-name">{{ target.targetName }}</span>
                </button>

                <!-- 请求完成但没有匹配结果时显示空状态。 -->
                <div v-if="candidates.length === 0" class="dropdown-state">
                    暂无匹配股票
                </div>
            </template>
        </div>
    </div>
</template>

<script setup lang="ts">
// ref：创建响应式状态；watch：监听父组件数据；computed：计算下拉框显示条件。
import { computed, ref, watch } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { searchStockTargets } from '/@/api/watchtarget/watchTargetApi'
import type { WatchTarget } from '/@/types/watchtarget/entity'

// Props：父组件传入当前选中的 WatchTarget，以及可选的输入框提示文字。
const props = withDefaults(
    defineProps<{
        modelValue?: WatchTarget | null
        placeholder?: string
    }>(),
    {
        modelValue: null,
        placeholder: '请输入股票代码或股票简称'
    }
)

// Emits：update:modelValue 用于支持 v-model；select 用于通知用户主动选中股票。
const emit = defineEmits<{
    (event: 'update:modelValue', value: WatchTarget | null): void
    (event: 'select', value: WatchTarget): void
}>()

// keyword：输入框当前显示的文本。
const keyword = ref('')
// loading：当前是否正在等待后端检索结果。
const loading = ref(false)
// focused：输入框是否处于焦点状态，用来控制下拉面板显示。
const focused = ref(false)
// candidates：后端返回的候选股票数组，每一项都是 WatchTarget。
const candidates = ref<WatchTarget[]>([])
// activeIndex：键盘当前高亮的候选项下标；-1 表示没有高亮项。
const activeIndex = ref(-1)

// 用于解决快速连续输入时的请求乱序问题。
// 只有最后一次发出的请求，才有资格更新候选列表。
// 请求序号：每次检索递增，确保只有最新请求可以覆盖候选列表。
let requestSerial = 0

// 监听父组件传入的 modelValue；当父组件设置了股票时，同步显示代码和简称。
watch(
    () => props.modelValue,
    (value) => {
        if (!value) {
            return
        }

        const selectedText = formatSelectedText(value)
        if (keyword.value !== selectedText) {
            keyword.value = selectedText
        }
    },
    { immediate: true }
)

// 将 WatchTarget 格式化为输入框展示文本，例如“000001 平安银行”。
function formatSelectedText(target: WatchTarget) {
    return `${target.targetCode} ${target.targetName}`.trim()
}

// 输入事件处理：用户每次编辑都会清除旧选择，并立即发起一次检索。
function handleInput(value: string) {
    // 用户重新编辑后，之前的选中值立即失效。
    emit('update:modelValue', null)
    activeIndex.value = -1

    // 每次 input 编辑事件都触发检索。
    void search(value)
}

// 输入框获得焦点：如果已有关键词，则重新查询一次。
function handleFocus() {
    focused.value = true

    if (keyword.value.trim()) {
        void search(keyword.value)
    }
}

// 输入框失去焦点时关闭下拉框；候选项的 mousedown.prevent 会避免点击时提前 blur。
function handleBlur() {
    // 候选项使用 @mousedown.prevent，点击候选时不会先触发 blur。
    focused.value = false
}

// 点击放大镜按钮时执行当前关键词检索。
function handleSearchButtonClick() {
    void search(keyword.value)
}

// 核心检索流程：校验关键词、分配请求序号、调用 API、更新候选项和 loading。
async function search(rawKeyword: string) {
    const normalizedKeyword = rawKeyword.trim()
    const currentSerial = ++requestSerial

    if (!normalizedKeyword) {
        loading.value = false
        candidates.value = []
        activeIndex.value = -1
        return
    }

    loading.value = true

    try {
        const result = await searchStockTargets(normalizedKeyword, 10)

        // 丢弃过期请求结果，避免快速输入时列表被旧响应覆盖。
        if (currentSerial !== requestSerial) {
            return
        }

        candidates.value = Array.isArray(result) ? result.slice(0, 10) : []
        activeIndex.value = candidates.value.length > 0 ? 0 : -1
    } catch (error) {
        if (currentSerial !== requestSerial) {
            return
        }

        console.error('检索股票失败', error)
        candidates.value = []
        activeIndex.value = -1
    } finally {
        if (currentSerial === requestSerial) {
            loading.value = false
        }
    }
}

// 键盘上下移动高亮项；step=1 向下，step=-1 向上，并支持首尾循环。
function moveActive(step: number) {
    if (!candidates.value.length) {
        return
    }

    const next = activeIndex.value + step

    if (next < 0) {
        activeIndex.value = candidates.value.length - 1
        return
    }

    if (next >= candidates.value.length) {
        activeIndex.value = 0
        return
    }

    activeIndex.value = next
}

// Enter：有高亮候选项时选中；否则再次执行检索。
function handleEnter() {
    if (activeIndex.value >= 0 && activeIndex.value < candidates.value.length) {
        selectTarget(candidates.value[activeIndex.value])
        return
    }

    void search(keyword.value)
}

// 选择股票：回填代码和简称、清空候选列表，并通过 v-model 与 select 事件返回对象。
function selectTarget(target: WatchTarget) {
    keyword.value = formatSelectedText(target)
    candidates.value = []
    activeIndex.value = -1
    focused.value = false

    emit('update:modelValue', target)
    emit('select', target)
}

// Esc：关闭候选列表并清除键盘高亮状态。
function closeDropdown() {
    focused.value = false
    activeIndex.value = -1
}

// 下拉面板的显示条件：输入框仍有焦点，并且关键词不是空白。
const dropdownVisible = computed(
    () => focused.value && keyword.value.trim().length > 0
)
</script>

<style scoped>
/* scoped 表示样式仅作用于本组件，避免影响其他页面元素。 */
/* 根容器使用 relative，作为候选面板 absolute 定位的锚点。 */
.stock-search-box {
    position: relative;
    width: 100%;
}

.search-input-row {
    display: flex;
    align-items: stretch;
    width: 100%;
}

.search-input-row :deep(.el-input__wrapper) {
    border-radius: 8px 0 0 8px;
}

.search-button {
    width: 44px;
    min-width: 44px;
    margin-left: -1px;
    border-radius: 0 8px 8px 0;
}

/* 候选面板浮动在输入框下方，不占用页面正常文档流空间。 */
.candidate-dropdown {
    position: absolute;
    z-index: 2100;
    top: calc(100% + 6px);
    left: 0;
    right: 0;
    overflow: hidden;
    padding: 6px;
    border: 1px solid #e5e7eb;
    border-radius: 10px;
    background: #ffffff;
    box-shadow: 0 12px 28px rgb(15 23 42 / 12%);
}

/** 单条候选项采用两列布局：股票代码、简称。 */
.candidate-item {
    display: grid;
    grid-template-columns: 96px minmax(0, 1fr);
    align-items: center;
    gap: 10px;
    width: 100%;
    min-height: 38px;
    padding: 7px 10px;
    border: 0;
    border-radius: 7px;
    background: transparent;
    color: #334155;
    text-align: left;
    cursor: pointer;
}

.candidate-item:hover,
.candidate-item.is-active {
    background: #eff6ff;
}

.candidate-code {
    color: #2563eb;
    font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
    font-weight: 700;
}

.candidate-name {
    color: #111827;
    white-space: normal;
    overflow: visible;
    text-overflow: clip;
    word-break: break-word;
}

.candidate-market {
    color: #64748b;
    font-size: 12px;
    text-align: right;
}

/* 加载中、无匹配结果等状态提示的样式。 */
.dropdown-state {
    padding: 11px 10px;
    color: #94a3b8;
    font-size: 13px;
    text-align: center;
}
</style>