# A-03 应用指标配置模块：Codex 代码实现说明

> 文档用途：交给 Codex 在现有个人辅助交易平台代码仓库中实施 A-03
> 应用指标配置模块的轻量化改造。\
> 本文是代码实现任务说明，不是概要设计修订稿。Codex
> 应先检查仓库现状，再按本文范围修改代码；不得擅自扩展为指标平台。

## 1. 实施目标

将现有应用指标配置页面改造为一个轻量管理功能，维护应用指标基础信息及其与平台数据集标准字段之间的绑定关系，为以下四类业务功能提供统一指标主档：

1.  条件筛选；
2.  指标排行；
3.  标的对比；
4.  指标提醒。

本次只实现 **A-03 应用指标配置及其必要的 Dataset /
标准字段选择能力**。四个下游页面的指标选择、指标取数和业务计算不属于本次实现范围。

### 1.1 本次确定的核心原则

-   主页面采用"左侧单层分类按钮 + 右侧指标列表"的布局。
-   分类固定，不建设分类管理页面，不使用 `el-tree`。
-   "全部"是页面筛选项，不是实际指标分类值。
-   点击某个实际分类后，该分类作为新增指标时的默认分类。
-   指标只维护基础信息和数据来源，不建设展示规则、应用范围配置、指标口径管理、预览、复制、引用详情等功能。
-   指标来源允许来自不同 Dataset，不要求所有指标位于同一个宽表。
-   本次不实现跨 Dataset 取数，不调用 Python，不实现 SQL JOIN。
-   将来四个业务页面接入时，按"Spring Boot
    解析指标配置和组织取数请求，Python 分 Dataset
    取数并在服务端组合"的既定方向实施；本次仅保留清晰的数据配置边界，不提前建设该机制。
-   指标停用和删除采用相同的引用校验规则：只要存在后续业务引用，就拒绝操作。
-   复用项目现有 Vue3、TypeScript、Element Plus、Spring
    Boot、API、状态枚举和异常处理规范，不另起一套技术实现。

## 2. 开始编码前必须检查的现有工程

Codex
不得仅根据本文猜测后端接口、数据库字段或目录结构。开始修改前，先在仓库中检索并确认：

1.  `AppMetricPage.vue` 的实际路径及其引用关系。
2.  `AppMetricFormDialog.vue` 的实际路径及其引用关系。
3.  `/@/api/appmetric/appMetricApi` 对应的 API 文件。
4.  `/@/types/appmetric/entity` 对应的实体类型及
    `createDefaultAppMetric`。
5.  应用指标后端 Controller、Service、Mapper、Entity、DTO 和数据库表。
6.  Dataset 查询接口、Dataset 实体及数据集状态枚举。
7.  Dataset 当前关联的 DataTypeVersion 查询方式。
8.  DataTypeVersion 下标准字段列表的接口、DTO 和字段类型枚举。
9.  项目中已有的分页查询、状态启停、删除、业务异常和操作确认实现。
10. 条件筛选、排行、对比、提醒模块当前是否已有持久化配置对象，以及这些对象如何引用指标编码。

### 2.1 已提供前端文件中的现状

当前上传的 `AppMetricPage.vue` 主要包含：

-   关键字、状态查询；
-   指标列表；
-   新增、编辑、删除；
-   分页；
-   当前表格字段包括
    `metricCode`、`metricName`、`metricCategory`、`unit`、`displayFormat`、`dataSourceCode`、`calcPeriod`、`status`、`description`。

当前上传的 `AppMetricFormDialog.vue` 包含：

-   编码、名称、指标分类、单位；
-   展示格式、数据来源、计算周期；
-   状态、说明；
-   保存时调用 `saveOrUpdateAppMetric`。

本次应移除页面和表单中不再建设的 `displayFormat`、`calcPeriod`
等配置项，并将原有泛化的 `dataSourceCode` 来源输入调整为 Dataset
与标准字段绑定。

当前上传的 `AppMetricPage.vue` 中，API import 语句末尾存在多余反引号：

``` ts
import { queryAppMetricPage, deleteAppMetric } from '/@/api/appmetric/appMetricApi';``
```

如果仓库实际文件也存在该问题，应一并修复。

> 注意：上传文件只能证明当前前端组件的结构，不能证明后端
> API、数据库字段和 Dataset
> 查询接口的实际名称。后续实现必须以仓库代码为准。

## 3. 功能范围

### 3.1 本次需要实现

  功能           本次要求
  -------------- ---------------------------------------------------
  指标列表       分页查询、关键字查询、分类筛选、状态筛选
  左侧分类导航   固定单层按钮，点击后筛选指标列表
  新增指标       填写基础信息、选择 Dataset 和标准字段
  编辑指标       修改允许编辑的基础信息和来源绑定
  分类默认值     从具体分类进入新增时自动填充分类
  Dataset 选择   查询可用 Dataset，选择 Dataset 后加载当前标准字段
  字段选择       从 Dataset 当前 DataTypeVersion 的字段中选择
  状态启停       启用、停用
  删除           删除前检查是否存在后续业务引用
  停用校验       与删除采用同一套引用检查规则
  表单校验       编码、名称、分类、Dataset、字段等必填和有效性校验

### 3.2 本次明确不实现

以下内容不得在本次开发中顺手扩展：

-   指标分类管理页面、分类树维护、分类拖拽和分类排序；
-   指标复制；
-   指标引用详情、引用数量、引用影响分析页面；
-   指标数据预览；
-   展示格式、精度、缩放、空值展示规则；
-   指标应用范围配置（是否可筛选、排行、对比、提醒）；
-   指标业务口径、计算公式、统计周期维护；
-   指标版本管理；
-   指标血缘和数据来源追溯页面；
-   A-04 视图配置接入；
-   市场概览、标的详情、财务分析接入；
-   条件筛选、排行、对比、提醒四个业务页面的改造；
-   Spring Boot 指标解析和 Python 跨 Dataset 取数组合服务；
-   Python 服务接口新增；
-   SQL 层跨 Dataset JOIN；
-   统一宽表 Dataset 建设。

## 4. 固定指标分类

分类在前端以固定常量维护，不建设分类数据库表和分类 CRUD。

建议分类编码如下：

  编码          名称   说明
  ------------- ------ --------------------------------------
  `ALL`         全部   仅用于页面筛选，不允许保存到指标记录
  `MARKET`      行情   行情、价格、涨跌、成交、换手等
  `VALUATION`   估值   PE、PB、PS 等
  `FINANCE`     财务   ROE、利润率、资产负债等
  `GROWTH`      成长   营收增长、利润增长等
  `RISK`        风险   波动、风险类指标
  `OTHER`       其他   暂时无法明确归类的指标

如果仓库中已经存在正式分类编码或历史数据编码，Codex
必须先核对并兼容现有值，不得直接把历史值改成本文示例编码。只有在确认当前没有既有编码约束时，才采用上表编码。

`ALL` 只存在于页面状态和查询逻辑中：

``` ts
type MetricCategoryFilter =
  | 'ALL'
  | 'MARKET'
  | 'VALUATION'
  | 'FINANCE'
  | 'GROWTH'
  | 'RISK'
  | 'OTHER';
```

实际保存的 `metricCategory` 只能是六个业务分类之一，不能保存 `ALL`。

## 5. 主页面布局和交互

### 5.1 总体布局

采用左右两栏：

-   左侧：固定宽度的单层分类导航；
-   右侧：标题、查询区、指标表格、分页和新增按钮。

分类导航只是普通按钮列表或菜单样式，不使用 `el-tree`。

建议布局比例：

-   左侧分类区：约 160～190px；
-   右侧内容区：自适应占满剩余空间；
-   两栏间距：12～16px；
-   页面继续沿用现有 `el-card`、`el-table`、`el-form`、`el-pagination`
    风格。

页面结构示意：

``` text
┌──────────────────────────────────────────────────────────────────────────┐
│ 左侧分类区                 │ 右侧指标管理区                              │
│                            │                                              │
│ 指标分类                   │ 应用指标配置                [+ 新增指标]    │
│                            │                                              │
│ ● 全部                     │ 关键词 [名称/编码]  状态 [全部] [查询][重置] │
│ ○ 行情                     │                                              │
│ ○ 估值                     │ ───────────────────────────────────────────  │
│ ○ 财务                     │ 编码 | 名称 | 分类 | 数据集 | 字段 | 类型... │
│ ○ 成长                     │ ...                                          │
│ ○ 风险                     │ ...                                          │
│ ○ 其他                     │                                              │
│                            │                                  分页        │
└──────────────────────────────────────────────────────────────────────────┘
```

### 5.2 左侧分类导航

要求：

1.  展示"全部、行情、估值、财务、成长、风险、其他"。
2.  一次只能选中一个分类。
3.  当前选中分类使用 Element Plus 的
    `type="primary"`、高亮背景或项目统一选中样式。
4.  点击分类后：
    -   更新 `selectedCategory`；
    -   更新分页查询参数中的 `metricCategory`；
    -   将 `pageNo` 重置为 1；
    -   重新请求列表。
5.  不展示展开箭头，不实现父子层级。
6.  不增加分类管理入口。
7.  不要求显示分类数量，避免增加统计查询。

### 5.3 分类与新增表单联动

这是本次必须实现的交互：

  左侧当前选择   点击新增后的分类默认值
  -------------- ------------------------
  全部           不预选，用户必须选择
  行情           自动填充 MARKET
  估值           自动填充 VALUATION
  财务           自动填充 FINANCE
  成长           自动填充 GROWTH
  风险           自动填充 RISK
  其他           自动填充 OTHER

具体规则：

-   从"全部"进入新增：分类为空，表单分类必填。
-   从某个具体分类进入新增：将该分类传给弹窗作为默认分类。
-   默认分类仍允许用户修改。
-   编辑指标时必须使用该指标原有分类，不得被左侧当前选中分类覆盖。
-   保存成功后关闭弹窗并刷新列表。
-   如果当前选中具体分类，而用户在新增表单中改成其他分类，保存后记录可能不出现在当前列表中，这是正常筛选结果；可以提示"保存成功"，不必自动切换侧栏分类。

建议父组件通过 `defaultCategory` prop
传递默认分类，不要复用编辑数据对象来隐式传递分类。

### 5.4 顶部查询区

由于分类已经放在左侧，顶部不再重复放分类下拉框。

保留：

  查询项   控件          说明
  -------- ------------- ----------------------------
  关键字   `el-input`    同时匹配指标编码和名称
  状态     `el-select`   全部、启用、停用
  查询     按钮          查询当前分类下的数据
  重置     按钮          清空关键字、状态并重新查询

重置行为：

-   `keyword = ''`；
-   `status = ''`；
-   `pageNo = 1`；
-   `pageSize` 恢复项目默认值或保留当前项目既有行为；
-   **保留左侧当前选中分类**；
-   重新查询。

### 5.5 指标列表

建议字段：

  字段         说明
  ------------ -------------------------------------------
  指标编码     `metricCode`
  指标名称     `metricName`
  指标分类     显示中文分类名称
  来源数据集   优先显示 Dataset 名称，必要时辅助显示编码
  来源字段     `fieldCode`，可辅助显示字段名称
  字段类型     从 DataTypeField 派生，只读展示
  单位         `unit`
  状态         启用/停用
  更新时间     沿用项目审计字段
  操作         编辑、启用/停用、删除

列表不显示：

-   展示格式；
-   计算周期；
-   复杂指标口径；
-   应用范围；
-   引用关系详情；
-   数据预览。

如果当前分页接口没有返回
`datasetName`、`fieldName`、`valueType`，优先评估在后端列表 DTO
中一次性补齐必要展示字段。禁止在前端对表格每一行分别请求 Dataset
或字段接口，避免 N+1 请求。

### 5.6 操作列

只保留：

-   编辑；
-   启用或停用；
-   删除。

不要增加复制、详情、预览、引用查看等操作。

停用和删除必须分别调用后端操作接口，由后端执行引用校验。前端不能只根据当前页面数据自行判断是否可以停用或删除。

## 6. 新增 / 编辑弹窗

### 6.1 表单结构

弹窗继续使用单页 `el-dialog`，不增加 Tab、步骤条、详情抽屉或独立子页面。

分成两个视觉分组：

**基本信息**

  字段       字段名                   必填 说明
  ---------- ------------------ ---------- --------------------------------------
  指标编码   `metricCode`               是 稳定业务编码；编辑时只读
  指标名称   `metricName`               是 指标显示名称
  指标分类   `metricCategory`           是 固定分类下拉框
  单位       `unit`                     否 例如 `%`、元、倍、股
  指标说明   `description`              否 普通备注，不建设口径体系
  状态       `status`             系统控制 新增默认启用；编辑时通过列表操作启停

**数据来源**

  --------------------------------------------------------------------------------------------
  字段             字段名                            必填 说明
  ---------------- ---------------- --------------------- ------------------------------------
  来源数据集       `datasetCode`                       是 从已登记 Dataset 中选择

  来源字段         `fieldCode`                         是 从该 Dataset 当前 DataTypeVersion
                                                          的字段中选择

  字段类型         `valueType` /                 只读展示 从标准字段定义派生，不允许人工修改
                   `fieldType`                            
  --------------------------------------------------------------------------------------------

### 6.2 表单交互

1.  选择 Dataset。
2.  根据 Dataset 编码查询其当前 DataTypeVersion。
3.  获取该版本下的 DataTypeField 列表。
4.  用户选择 `fieldCode`。
5.  页面显示字段名称和字段类型。
6.  保存时提交 `datasetCode` 和 `fieldCode`，不提交物理表名、物理列名或
    StorageMapping。

Dataset 选择框建议支持搜索，显示 Dataset 名称和编码。

字段选择框建议显示：

``` text
字段编码       字段名称       字段类型
pe_ttm         市盈率TTM      DECIMAL
pb             市净率         DECIMAL
roe            净资产收益率   DECIMAL
```

字段名称和类型均为只读来源信息，实际保存以稳定的 `fieldCode` 为准。

### 6.3 Dataset 与字段联动规则

-   新增时，未选择 Dataset 前，字段选择框禁用。
-   选择 Dataset 后加载该 Dataset 当前 DataTypeVersion 的字段。
-   更换 Dataset 时，清空原 `fieldCode` 和字段类型，重新加载字段。
-   Dataset 查询失败时显示错误提示，不允许提交一个没有验证来源的指标。
-   字段查询失败时显示错误提示，不允许提交。
-   字段列表为空时显示"当前数据集没有可用标准字段"。
-   编辑时先回显原 Dataset，再加载其字段并回显原字段。
-   如果原 Dataset
    或原字段已失效，显示明确提示；不允许静默改绑到其他字段。
-   保存时后端必须再次校验 Dataset 状态和字段是否属于该 Dataset 当前
    DataTypeVersion，不能只信任前端传值。

### 6.4 Dataset 状态

《个人辅助交易平台一期概要设计 V1.9》定义 Dataset 状态：

    数值 状态       含义
  ------ ---------- --------
       0 DRAFT      草稿
       1 ENABLED    已启用
       2 DISABLED   已停用
       3 ARCHIVED   已归档

新增指标选择 Dataset 时，原则上只允许选择 `ENABLED (1)` 的 Dataset。

编辑已有指标时，如果原 Dataset
已经停用或归档，应确保原绑定能够正确回显并给出状态提示；不要因为选项接口默认只返回启用数据集，就导致编辑弹窗无法显示原值。是否允许保存其他基础字段、是否要求重新选择有效
Dataset，应结合现有业务校验规则实现，不得静默更改来源。

### 6.5 字段类型限制

本次不新增 `useForFilter`、`useForRank`、`useForCompare`、`useForAlert`
等字段。

字段类型由 DataTypeField 提供，A-03 只读显示，不重复维护。

**不要未经项目现有规则确认，就在 A-03 中硬编码"只能绑定数值型字段"。**
如果现有数据类型规范或后续业务契约已经明确指标只支持数值型，可以复用该约束；否则本次只保存字段绑定，具体业务模块在后续接入时按自身能力校验字段类型。

## 7. 应用指标数据模型

### 7.1 建议业务字段

目标应用指标实体至少包含：

  字段                        说明
  --------------------------- ----------------------------
  `id`                        主键，沿用项目现有主键规范
  `metricCode`                稳定指标编码
  `metricName`                指标名称
  `metricCategory`            固定分类编码
  `datasetCode`               来源 Dataset 稳定编码
  `fieldCode`                 来源标准字段编码
  `unit`                      单位
  `description`               简单说明
  `status`                    启用/停用
  `createdAt` / `updatedAt`   审计时间
  `createdBy` / `updatedBy`   审计用户

### 7.2 不重复保存字段结构

根据 V1.9 数据模型：

-   Dataset 使用稳定 `dataset_code` 标识数据资产；
-   Dataset 当前字段结构引用当前 DataTypeVersion；
-   DataTypeField 负责字段编码、字段名称、字段类型等定义；
-   DatasetMetadata 保存数据集描述及存储定位信息；
-   StorageMapping / StorageColumnMapping 负责物理存储映射。

因此 A-03 只保存：

``` text
datasetCode
fieldCode
```

字段名称、字段类型、DataTypeVersion 信息应通过 Dataset 当前关联的
DataTypeVersion 查询获得。

如果项目为查询性能需要在 DTO 中返回
`datasetName`、`fieldName`、`fieldType`，可以在查询时派生，不应把它们作为第二套可编辑标准定义。

### 7.3 旧字段兼容

现有页面出现 `displayFormat`、`dataSourceCode`、`calcPeriod` 等旧字段。

Codex 必须先检查 Entity、数据库列、API DTO 和历史数据：

-   页面和表单不再展示或提交 `displayFormat`、`calcPeriod`。
-   不得在未确认数据含义前，直接把 `dataSourceCode` 改名或映射为
    `datasetCode`。
-   如果 `dataSourceCode` 实际已经保存 Dataset
    编码，可评估兼容复用；如果含义不同，则增加明确的
    `datasetCode`、`fieldCode` 字段并编写数据库迁移。
-   不要为了简化页面而直接删除数据库旧列。
-   对已有应用指标记录，制定可验证的数据迁移或兼容读取方案；无法确定旧来源映射的记录应保留并标记待处理，不能随意猜测字段。
-   新旧字段并存期间，读写规则必须明确，避免同一指标出现两套来源。

## 8. API 与后端实现要求

### 8.1 复用现有应用指标 API

当前前端引用：

``` ts
import {
  queryAppMetricPage,
  deleteAppMetric
} from '/@/api/appmetric/appMetricApi';

import { saveOrUpdateAppMetric } from '/@/api/appmetric/appMetricApi';
```

Codex 应检查该 API 文件，并在原有接口风格上扩展，不要创建重复的
AppMetric API 模块。

需要具备的接口能力：

1.  指标分页查询：支持
    `keyword`、`metricCategory`、`status`、`pageNo`、`pageSize`。
2.  指标新增/修改：保存基本信息和 Dataset/字段绑定。
3.  指标启用/停用：单独状态变更，停用前执行引用检查。
4.  指标删除：删除前执行引用检查。
5.  Dataset 选项查询：返回可选 Dataset。
6.  Dataset 字段查询：按 `datasetCode` 返回当前 DataTypeVersion
    下的标准字段。

接口 URL、HTTP
方法和统一响应结构必须沿用项目现有规范。本文只定义接口能力，不强制 Codex
使用未经仓库确认的 URL。

### 8.2 分页查询参数

在现有分页参数基础上增加：

``` ts
interface AppMetricPageQuery {
  keyword?: string;
  metricCategory?: string; // 空值或不传表示全部
  status?: string;
  pageNo: number;
  pageSize: number;
}
```

左侧选择"全部"时，不传 `metricCategory`
或传空值；选择具体分类时传对应分类编码。

### 8.3 Dataset 选项接口的逻辑返回内容

建议至少返回：

``` ts
interface DatasetOption {
  datasetCode: string;
  datasetName: string;
  datasetStatus: number;
  dataTypeVersionId?: number | string;
}
```

实际字段名以项目 Dataset DTO 为准。

### 8.4 Dataset 标准字段接口的逻辑返回内容

建议至少返回：

``` ts
interface DatasetFieldOption {
  fieldCode: string;
  fieldName: string;
  fieldType: string;
  required?: boolean;
  sort?: number;
}
```

字段必须来自该 Dataset 当前关联的 DataTypeVersion，不得从物理表结构或
Python 返回字段 JSON 直接生成 A-03 的字段选项。

### 8.5 保存校验

后端保存时至少校验：

-   `metricCode` 非空且唯一；
-   `metricName` 非空；
-   `metricCategory` 属于允许的固定分类；
-   `datasetCode` 存在；
-   Dataset 状态满足绑定要求；
-   `fieldCode` 存在于 Dataset 当前 DataTypeVersion；
-   同一指标编码不能重复；
-   编辑时指标编码不能被修改；
-   必要的单位、说明长度和状态合法性校验。

前端校验只用于改善交互，后端校验才是最终约束。

## 9. 停用与删除：统一引用检查

### 9.1 业务规则

停用和删除采用相同的引用检查原则：

> 只有当前指标没有任何后续业务引用时，才允许停用或删除。

检查范围限于本期四类指标使用方：

1.  条件筛选配置；
2.  排行配置；
3.  对比配置；
4.  指标提醒规则。

如果某个模块目前尚未建立持久化配置对象，则不虚构表名或关联表。Codex
应在实现前列出现有可检查的引用来源，并对尚不存在的来源明确记录，不得假装已经完成检查。

### 9.2 服务端实现

建议在 AppMetric Service 中抽取一个内部公共方法，例如：

``` java
private void assertMetricNotReferenced(String metricCode) {
    // 检查当前工程中已经存在的筛选配置引用
    // 检查当前工程中已经存在的排行配置引用
    // 检查当前工程中已经存在的对比配置引用
    // 检查当前工程中已经存在的提醒规则引用
    // 发现任意引用则抛出业务异常
}
```

以上仅为逻辑示意。实际引用查询必须使用仓库中真实存在的
Entity、Mapper、表名和配置结构。

### 9.3 停用流程

``` text
用户点击停用
    ↓
前端调用停用接口
    ↓
Spring Boot 开启事务
    ↓
查询指标是否存在
    ↓
执行统一引用检查
    ├─ 存在引用：返回业务错误，保持原状态
    └─ 不存在引用：更新为 DISABLED
    ↓
返回成功
```

### 9.4 删除流程

``` text
用户点击删除
    ↓
前端二次确认
    ↓
调用删除接口
    ↓
Spring Boot 开启事务
    ↓
查询指标是否存在
    ↓
执行同一套引用检查
    ├─ 存在引用：返回业务错误，拒绝删除
    └─ 不存在引用：按项目现有规范删除
    ↓
返回成功
```

### 9.5 前端提示

停用失败：

``` text
当前指标已被业务配置引用，不能停用，请先处理相关业务配置。
```

删除失败：

``` text
当前指标已被业务配置引用，不能删除，请先处理相关业务配置。
```

不建设引用详情页面，不向前端返回引用清单。

### 9.6 并发与事务

引用检查和状态更新/删除必须放在同一服务端事务边界内，避免检查完成后、实际操作前又新增引用。

Codex
应根据当前数据库和项目事务规范评估是否需要行锁、乐观锁或其他并发控制。不得仅在前端检查，也不得只在
Controller 中检查后再调用另一个非事务服务完成更新。

## 10. 前端代码改造步骤

### 10.1 `AppMetricPage.vue`

按以下步骤修改：

1.  将现有单列表布局改为左右布局。
2.  左侧新增固定分类按钮列表。
3.  增加 `selectedCategory` 状态，初始值为 `ALL`。
4.  分类点击时同步更新分页查询参数并重新加载。
5.  查询区域删除分类下拉框，仅保留关键字和状态。
6.  重置时清空关键字和状态，但保留 `selectedCategory`。
7.  新增操作时，把当前具体分类传给表单；当前为 `ALL` 时不传默认分类。
8.  编辑操作只传当前行数据，不能使用左侧分类覆盖原分类。
9.  表格列替换旧展示字段，显示 Dataset 和标准字段绑定信息。
10. 操作列增加启用/停用，保留编辑和删除。
11. 对停用、删除接口返回的"存在引用"业务异常进行友好提示。
12. 保留现有分页、加载状态和成功后刷新逻辑。
13. 修复实际源码中存在的多余 import 反引号。

建议状态：

``` ts
const selectedCategory = ref<MetricCategoryFilter>('ALL');

const queryForm = reactive({
  keyword: '',
  metricCategory: '',
  status: '',
  pageNo: 1,
  pageSize: 20,
});
```

分类点击逻辑示意：

``` ts
function handleCategoryChange(category: MetricCategoryFilter) {
  selectedCategory.value = category;
  queryForm.metricCategory = category === 'ALL' ? '' : category;
  queryForm.pageNo = 1;
  fetchData();
}
```

新增默认分类示意：

``` ts
function openCreate() {
  dialogMode.value = 'create';

  const defaultCategory =
    selectedCategory.value === 'ALL'
      ? ''
      : selectedCategory.value;

  currentRow.value = {
    ...createDefaultAppMetric(),
    metricCategory: defaultCategory,
  };

  dialogVisible.value = true;
}
```

示例仅说明状态关系，最终代码应按当前 `AppMetric`
类型调整，避免把空字符串写入不允许为空的类型。

### 10.2 `AppMetricFormDialog.vue`

按以下步骤修改：

1.  保留现有 Dialog，不拆分多个页面。
2.  增加 `defaultCategory` prop，或由父组件传入已经预填的
    `formData`；二者择一，不要重复传递同一含义。
3.  指标分类从自由文本输入改为固定分类下拉框。
4.  删除展示格式、计算周期输入项。
5.  原"数据来源"文本输入改为 Dataset 选择。
6.  增加标准字段选择。
7.  增加字段类型只读展示。
8.  Dataset 变更时清空原字段并重新加载字段选项。
9.  编辑时回显 Dataset 和字段。
10. 指标编码编辑时只读。
11. 新增指标默认启用；编辑时不允许通过普通保存表单绕过停用引用检查。
12. 启用/停用通过列表操作调用独立状态接口。
13. 保存时只提交目标模型字段，不提交已取消的展示格式和计算周期。
14. 保留现有表单校验、保存 loading、成功事件和关闭事件。

必填校验：

-   `metricCode`
-   `metricName`
-   `metricCategory`
-   `datasetCode`
-   `fieldCode`

单位和说明为选填。

### 10.3 API 和 TypeScript 类型

修改现有：

-   `/@/api/appmetric/appMetricApi`
-   `/@/types/appmetric/entity`

目标 `AppMetric` 类型应反映实际后端字段，至少包含：

``` ts
interface AppMetric {
  id?: number | string;
  metricCode: string;
  metricName: string;
  metricCategory: string;
  datasetCode: string;
  fieldCode: string;
  unit?: string;
  description?: string;
  status: string;
  datasetName?: string;
  fieldName?: string;
  fieldType?: string;
  createdAt?: string;
  updatedAt?: string;
}
```

注意：

-   `datasetName`、`fieldName`、`fieldType` 如果是查询 DTO
    派生字段，可以只用于展示，不必写入数据库。
-   `fieldType` 不应由用户编辑。
-   如果项目当前使用不同的状态类型、时间字段或主键类型，沿用项目既有定义。
-   不要因为本文示例而强制修改全项目通用状态枚举。

## 11. 后端代码实施步骤

### 11.1 先复用 Dataset 和 DataTypeVersion 查询能力

Codex 应搜索现有 Dataset 管理模块，优先复用：

-   Dataset 分页/选项查询；
-   Dataset 详情查询；
-   当前 DataTypeVersion 关联查询；
-   DataTypeField 字段列表查询。

如果已有合适接口，直接复用，不新增重复查询接口。

如果没有合适接口，再在现有 Dataset / DataType
管理模块中增加最小只读查询能力，不把 Dataset 维护职责转移给 A-03。

### 11.2 AppMetric 查询

分页查询需要支持：

``` text
keyword
metricCategory
status
pageNo
pageSize
```

查询结果可补充：

``` text
datasetName
fieldName
fieldType
```

补充字段通过合理的后端关联查询或批量查询取得。不要在前端逐行调用 Dataset
接口。

### 11.3 AppMetric 保存

保存 Service 负责：

1.  校验指标编码和名称；
2.  校验分类编码；
3.  校验 Dataset 存在且状态符合绑定规则；
4.  获取 Dataset 当前 DataTypeVersion；
5.  校验 `fieldCode` 属于该版本字段；
6.  校验指标编码唯一性；
7.  新增或更新指标主档；
8.  写入项目现有审计字段。

不要在 A-03 中操作物理表、物理字段或存储映射。

### 11.4 状态接口

建议独立实现启用/停用接口或沿用项目现有状态变更模式。

-   启用：校验指标存在，更新为 ENABLED。
-   停用：先调用统一引用检查，无引用才更新为 DISABLED。
-   普通编辑接口不能绕过停用检查。

### 11.5 删除接口

删除 Service 必须执行同一引用检查。

如果项目采用逻辑删除，沿用逻辑删除规范；如果当前配置对象采用物理删除且没有历史引用，沿用现有方式。不要擅自引入新的删除框架。

## 12. 本次与未来统一取数机制的边界

### 12.1 本次只保存指标来源配置

本次 A-03 保存：

``` text
metricCode
    ↓
datasetCode
    ↓
fieldCode
```

本次不实现：

-   按 `metricCode` 批量解析指标；
-   多 Dataset 查询计划；
-   Python 取数接口；
-   Python 多 Dataset 结果组合；
-   按标的编码合并指标值；
-   条件筛选、排行、对比和提醒的实际计算；
-   SQL JOIN。

### 12.2 后续四个业务页面接入时的总体方向

后续在条件筛选、排行、对比、提醒页面实施时，再建设以下过程：

``` text
业务页面提交 metricCode
        ↓
Spring Boot 解析 AppMetric
        ↓
得到 datasetCode + fieldCode 等数据定位信息
        ↓
Spring Boot 组织按 Dataset 分组的取数请求
        ↓
Python 分别读取不同 Dataset
        ↓
Python 在服务端按统一标的身份和时间语义组合数据
        ↓
返回统一指标结果
        ↓
Spring Boot 返回业务模块所需结果
        ↓
筛选 / 排行 / 对比 / 提醒执行各自逻辑
```

明确约束：

-   不要求所有指标来自同一个宽表 Dataset。
-   不在 SQL 层对不同 Dataset 做 JOIN。
-   Spring Boot 负责指标元数据解析、请求组织和业务接口编排。
-   Python 负责按 Dataset 读取数据及服务端组合。
-   四个业务页面复用同一套取数契约，不各自实现一套 Dataset 解析逻辑。
-   以上内容只作为后续架构边界，本次不得提前实现。

## 13. 测试与验收清单

当前实现不需要进行各功能的测试用例设计，只需要进行基础的编译测试。

## 14. Codex 推荐执行顺序

要求 Codex 按以下顺序工作，避免一次性大范围修改：

### 第一步：仓库检查并输出实施计划

-   找到前后端相关文件。
-   确认 AppMetric 现有字段和数据库结构。
-   确认 Dataset、DataTypeVersion、DataTypeField 的查询接口。
-   确认四类业务配置的真实引用来源。
-   确认现有状态、分页、异常处理规范。
-   输出将修改和新增的文件清单。
-   标出旧 `dataSourceCode` 的实际语义和迁移方案。

### 第二步：先完成后端数据契约

-   确定 `datasetCode`、`fieldCode` 的持久化字段。
-   完成 Dataset 选项和字段选项查询复用或最小扩展。
-   完成 AppMetric 查询、保存、启停、删除接口。
-   完成停用/删除统一引用检查。
-   编写后端测试。

### 第三步：修改前端表单

-   固定分类下拉。
-   Dataset 选择。
-   字段级联选择。
-   字段类型只读展示。
-   新增默认分类回填。
-   编辑回显和表单校验。

### 第四步：修改前端主页面

-   左侧分类按钮。
-   右侧查询和列表。
-   分类筛选。
-   保留分类的重置逻辑。
-   编辑、启停、删除操作。
-   业务异常提示。

### 第五步：联调和验收

进行语法检查、编译测试。

### 第六步：交付说明

Codex 完成后应输出：

1.  修改文件清单；
2.  新增文件清单；
3.  数据库变更脚本及执行说明（如有）；
4.  API 变化说明；
5.  关键实现说明；
6.  测试执行结果；
7.  尚未完成的引用检查来源或数据迁移问题；
8.  明确声明未实现 Python 取数及跨 Dataset 组合。

## 15. 最终验收标准

本次实现完成后，用户应能够：

1.  在页面左侧点击固定分类，快速筛选指标。
2.  从某个具体分类点击新增，表单自动带入该分类。
3.  维护指标编码、名称、分类、单位、说明和状态。
4.  选择一个已启用 Dataset，并从其当前 DataTypeVersion 中选择标准字段。
5.  在列表中查看指标对应的 Dataset、字段和字段类型。
6.  编辑已有指标的基础信息和来源绑定。
7.  对没有任何后续业务引用的指标执行停用或删除。
8.  对存在筛选、排行、对比或提醒引用的指标执行停用/删除时，得到明确提示且数据保持不变。

最终 A-03 应保持为一个轻量配置模块：

``` text
左侧固定分类
      +
右侧指标列表
      +
基础信息表单
      +
Dataset / 标准字段绑定
      +
停用 / 删除引用保护
```

本次不建设统一取数服务。后续四个业务页面接入时，再按照 Spring Boot
解析指标配置、Python 分 Dataset 取数并组合的方式统一实施。
