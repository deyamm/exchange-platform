# Dataset 元数据体系改造实施说明（Codex 执行版）

## 1. 改造目标

在现有 `Dataset`、`DatasetMetadata`、`DataTypeVersion`、`StorageMapping`、`StorageColumnMapping`、`TaskRun` 等实现基础上，对数据集元数据体系进行规范化，并完成 Spring Boot 后端与 Vue 前端的配套改造。

本次改造范围主要集中在：

- 数据集元数据分类与标准属性；
- 元数据维护方式；
- 系统派生元数据；
- 任务执行成功后的统计元数据；
- 数据集详情页展示；
- 数据集元数据维护交互。

不重构现有采集、清洗、字段映射和任务执行主链路。

## 2. 基本约束

1. 不新建另一套 Dataset 或 DatasetMetadata 体系；
2. 优先查找并修改现有 `DatasetMetadata` Entity、Mapper/Repository、Service、Controller 和 DTO；
3. 优先查找现有 `getDatasetDetail`、`updateDatasetMetadata` 对应后端实现；
4. 前端继续使用现有 `getDatasetDetail(datasetCode)`、`updateDatasetMetadata(datasetCode, payload)`；
5. 不修改现有 API URL 和前端 API 方法名称；
6. `updateDatasetMetadata` 外层请求结构继续保持：

```ts
{
  datasetName,
  datasetDesc,
  items
}
```

7. 不修改现有 TaskRun 成功/失败判断规则；
8. 数据统计失败不得反向把已经成功的任务执行改成失败；
9. 不通过 DatasetMetadata 重复维护 DataTypeVersion 字段结构、StorageColumnMapping 字段映射或 TaskRun 执行信息；
10. 本期继续采用 DatasetMetadata 当前值模型，不建设元数据历史版本。

## 3. 首先定位现有实现

Codex 开始修改前，先在仓库中搜索：

```text
DatasetMetadata
dataset_metadata
getDatasetDetail
updateDatasetMetadata
DatasetDetail
StorageMapping
StorageColumnMapping
DataTypeVersion
uniqueKey
TaskRun
SUCCESS
ResultSummary
datasetCode
```

重点确认三条现有链路：

```text
DatasetDetailPage
    ↓
queryApi.getDatasetDetail
    ↓
Dataset Detail Controller
    ↓
Dataset Query Service
    ↓
Dataset + DatasetMetadata + DataTypeVersion
```

```text
updateDatasetMetadata
    ↓
Controller
    ↓
Dataset Metadata Service
    ↓
dataset_metadata
```

```text
TaskRun SUCCESS
    ↓
状态结果管理 / Dataset 登记
    ↓
Dataset 创建或更新
    ↓
DatasetMetadata STORAGE 登记
```

本次新增的派生元数据和统计元数据逻辑应接入现有 Dataset 创建/更新流程，不要另起平行处理流程。

## 4. 元数据分类

最终统一采用以下五类：

```text
BUSINESS
MANAGEMENT
STORAGE
STATISTICS
EXTENSION
```

| Group | 作用 | 主要来源 |
|---|---|---|
| BUSINESS | 描述数据集业务语义 | 人工 + 配置派生 |
| MANAGEMENT | 描述责任和管理方式 | 人工 |
| STORAGE | 描述物理存储和存储约束 | 系统派生 |
| STATISTICS | 描述当前数据集实际统计信息 | 执行后计算 |
| EXTENSION | 标准体系之外的补充属性 | 人工 |

`STATISTICS` 只表示当前数据集整体统计信息，不扩展到字段质量画像、字段分布、质量评分等能力。

## 5. 元数据维护方式

增加代码级维护方式：

```java
public enum DatasetMetadataMaintainMode {
    MANUAL,
    CONFIG_DERIVED,
    EXECUTION_DERIVED
}
```

含义：

```text
MANUAL
用户维护

CONFIG_DERIVED
根据 DataTypeVersion、StorageMapping、StorageColumnMapping 等配置生成

EXECUTION_DERIVED
任务成功并完成物理写入后，根据实际数据计算
```

本期不强制在 `dataset_metadata` 表中新增 `maintain_mode` 字段，可以先作为元数据定义中的代码属性存在。

## 6. 建立统一 Metadata Definition

后端增加统一标准元数据定义，例如：

```java
DatasetMetadataDefinition
```

如果项目已经存在类似枚举、字典或配置体系，则优先复用。

至少描述：

```text
group
key
name
valueType
maintainMode
```

后端 Metadata Definition 是标准元数据校验的最终依据。

## 7. BUSINESS 标准属性

### 7.1 人工维护

```text
business_definition
业务定义
STRING
MANUAL

business_scope
业务范围
STRING
MANUAL

data_granularity
数据粒度
JSON
MANUAL

data_time_field
数据时间字段
STRING
MANUAL

unit
数据单位
STRING
MANUAL

currency
货币类型
STRING
MANUAL
```

### 7.2 系统派生

```text
business_key
业务主键
JSON
CONFIG_DERIVED
```

`business_key` 保存当前数据类型版本中作为唯一键使用的逻辑字段编码集合，例如：

```json
["stock_code", "trade_date"]
```

该属性不允许人工修改。

## 8. MANAGEMENT 标准属性

```text
data_owner
数据负责人
STRING
MANUAL

maintainer
维护人员
STRING
MANUAL

contact
联系方式
STRING
MANUAL

update_frequency
更新频率
JSON
MANUAL

sensitivity_level
敏感级别
STRING
MANUAL

usage_scope
使用范围
STRING
MANUAL

retention_policy
保留策略
STRING
MANUAL
```

`update_frequency` 不从 Scheduler Cron 自动推导，由人工维护。

## 9. STORAGE 标准属性

保留：

```text
storage_type
datasource_code
schema_name
table_name
```

新增：

```text
storage_unique_key
JSON
CONFIG_DERIVED
```

所有 STORAGE 属性：

- 禁止普通元数据接口人工修改；
- 禁止普通元数据页面删除；
- 禁止前端新增；
- 根据已确认的存储映射和实际配置自动同步。

## 10. STATISTICS 标准属性

```text
record_count
数据总条数
NUMBER
EXECUTION_DERIVED

time_coverage_field
时间覆盖统计字段
STRING
EXECUTION_DERIVED

time_coverage_start
数据起始时间
STRING
EXECUTION_DERIVED

time_coverage_end
数据结束时间
STRING
EXECUTION_DERIVED

statistics_updated_at
统计更新时间
STRING
EXECUTION_DERIVED
```

时间继续使用项目现有 ISO 8601 表达。

`time_coverage_field` 属于内部关联属性，用于判断当前时间覆盖统计是否仍与 `data_time_field` 对应，前端不直接展示。

## 11. EXTENSION 标准属性（当前先不使用该类属性）

本期优先支持：

```text
tags
数据标签
JSON
MANUAL

keywords
关键词
JSON
MANUAL

custom_remark
扩展说明
STRING
MANUAL

custom_properties
自定义属性
JSON
MANUAL
```

本次前端先不开放任意输入 `metadataKey` 的能力。

## 12. data_granularity 数据结构

使用 JSON 单属性方案：

```json
{
  "unit": "DAY",
  "interval": 1
}
```

时间型 unit：

```text
SECOND
MINUTE
HOUR
DAY
WEEK
MONTH
QUARTER
YEAR
```

特殊类型：

```text
TICK
EVENT
SNAPSHOT
STATIC
```

时间型要求：

```text
interval >= 1
```

特殊类型不需要 `interval`。

示例：

```json
{"unit":"MINUTE","interval":5}
```

```json
{"unit":"DAY","interval":1}
```

```json
{"unit":"TICK"}
```

## 13. data_granularity 前端展示

字段标题已经是“数据粒度”，值不要再重复追加“粒度”。

转换规则：

```text
SECOND  + 5  → 5秒
MINUTE  + 1  → 1分钟
MINUTE  + 5  → 5分钟
HOUR    + 1  → 1小时
DAY     + 1  → 1日
DAY     + 5  → 5日
WEEK    + 1  → 1周
MONTH   + 1  → 1月
QUARTER + 1  → 1季度
YEAR    + 1  → 1年
```

特殊类型：

```text
TICK     → 逐笔
EVENT    → 事件
SNAPSHOT → 快照
STATIC   → 静态
```

不要把 `DAY + 1` 特殊转换为“日级”。

## 14. update_frequency 数据结构

`update_frequency` 与 `data_granularity` 使用类似结构，不增加 `calendar` 字段：

```json
{
  "unit": "DAY",
  "interval": 1
}
```

unit 使用：

```text
SECOND
MINUTE
HOUR
DAY
WEEK
MONTH
QUARTER
YEAR
```

要求：

```text
interval >= 1
```

展示示例：

```text
{"unit":"DAY","interval":1}
→ 每1日

{"unit":"WEEK","interval":1}
→ 每1周

{"unit":"MINUTE","interval":5}
→ 每5分钟
```

不要解析 Scheduler Cron。

## 15. business_key 自动生成

`business_key` 不允许人工维护。

在 Dataset 首次成功生成以及后续成功更新时，根据当前实际使用的数据类型版本和现有唯一键配置刷新：

```text
当前实际使用的 DataTypeVersion
        ↓
取得 uniqueKey=true 的逻辑字段
        ↓
按现有字段顺序排列
        ↓
生成 fieldCode 数组
        ↓
BUSINESS.business_key
```

如果实际代码中唯一键权威信息位于 StorageMapping 或 StorageColumnMapping，则使用项目当前已有权威来源，不再额外创建另一套唯一键配置。

## 16. storage_unique_key 自动生成

根据逻辑唯一键和 StorageColumnMapping 生成实际物理字段唯一键：

```text
business_key
    ↓
StorageColumnMapping
    ↓
物理列名数组
    ↓
STORAGE.storage_unique_key
```

示例：

```json
["stock_cd", "trade_dt"]
```

`BUSINESS.business_key` 表示逻辑业务唯一键；`STORAGE.storage_unique_key` 表示实际物理表唯一键列。

## 17. data_time_field 维护规则

`data_time_field` 由用户选择，但不能让用户直接输入 fieldCode。

前端使用：

```text
detail.structure
```

构建字段选择器。

底层保存 `fieldCode`，页面展示 `fieldName`。

统一原则：

```text
存储：fieldCode
展示：fieldName
```

后端保存前校验：

1. fieldCode 存在于当前 DataTypeVersion；
2. 字段类型适合作为时间字段；
3. 优先复用项目当前 DataTypeField 类型枚举和判断逻辑；
4. 不额外定义一套与现有字段类型冲突的体系。

## 18. business_key 前端展示

根据：

```text
business_key fieldCode
        ↓
detail.structure
        ↓
fieldName
```

展示为类似：

```text
业务主键

[股票代码] [交易日期]
```

字段编码只作为辅助信息或 Tooltip。

如果 metadata 中存在已经不属于当前 DataTypeVersion 的 fieldCode，可显示：

```text
stock_code（字段已失效）
```

不要静默丢弃。

## 19. 任务成功后的派生元数据刷新

找到现有：

```text
TaskRun SUCCESS
→ Dataset create/update
```

处理代码。

在 Dataset 成功登记或更新后调用：

```text
refreshConfigDerivedMetadata(...)
refreshDatasetStatistics(...)
```

不要放到任务执行前、Vue 前端或单独的新调度任务中。

## 20. CONFIG_DERIVED 元数据刷新

增加或整理内部方法，例如：

```java
refreshConfigDerivedMetadata(...)
```

负责更新：

```text
BUSINESS.business_key

STORAGE.storage_type
STORAGE.datasource_code
STORAGE.schema_name
STORAGE.table_name
STORAGE.storage_unique_key
```

触发时机：

```text
Dataset 首次成功生成
以及
后续成功更新
```

## 21. record_count 准确统计

由于当前物理表主要通过 UPSERT 更新，因此每次任务成功后准确执行：

```sql
SELECT COUNT(*)
FROM target_table
```

更新：

```text
STATISTICS.record_count
```

不要使用：

```text
oldCount + processedCount
```

## 22. 时间覆盖统计

首先读取：

```text
BUSINESS.data_time_field
```

### 未配置 data_time_field

仍计算：

```text
record_count
statistics_updated_at
```

但不计算：

```text
time_coverage_field
time_coverage_start
time_coverage_end
```

### 已配置 data_time_field

例如：

```text
trade_date
```

通过 StorageColumnMapping 找到实际物理列：

```text
trade_date
→ trade_dt
```

执行：

```sql
SELECT
    COUNT(*),
    MIN(trade_dt),
    MAX(trade_dt)
FROM target_table
```

更新：

```text
record_count
time_coverage_field = trade_date
time_coverage_start
time_coverage_end
statistics_updated_at
```

## 23. 修改 data_time_field 时不清空旧统计

当人工元数据接口把：

```text
data_time_field
trade_date
→ created_at
```

时，不额外删除或更新：

```text
time_coverage_field
time_coverage_start
time_coverage_end
```

`updateDatasetMetadata` 只负责保存新的 `BUSINESS.data_time_field`。

## 24. 前端判断时间覆盖是否有效

比较：

```text
BUSINESS.data_time_field
```

与：

```text
STATISTICS.time_coverage_field
```

### 未配置 data_time_field

```text
数据时间字段    未设置
时间覆盖        —
```

### 已设置，但与统计字段不同

```text
data_time_field = created_at
time_coverage_field = trade_date
```

显示：

```text
数据时间字段    创建时间
时间覆盖        待下次执行更新
```

不要显示旧时间范围。

### 字段一致

如果 start/end 存在：

```text
时间覆盖
2020-01-01 ～ 2026-11-07
```

如果 MIN/MAX 返回 NULL：

```text
暂无可统计的时间数据
```

## 25. 下一次任务成功后自动刷新时间覆盖

用户修改 `data_time_field` 后，不需要额外清理 STATISTICS。

下一次任务成功：

```text
读取当前 data_time_field
    ↓
解析实际物理字段
    ↓
COUNT + MIN + MAX
    ↓
更新 time_coverage_field / start / end
```

统计自动恢复有效。

## 26. statistics_updated_at

每次统计查询成功完成后更新：

```text
STATISTICS.statistics_updated_at
```

如果统计查询失败：

- 不修改旧统计值；
- 不修改旧 `statistics_updated_at`；
- 记录日志；
- 不影响已经成功的 TaskRun。

## 27. 动态 SQL 安全

统计逻辑需要访问动态 Schema、Table 和 Column。

禁止直接使用人工 Metadata 或请求参数拼接 SQL。

动态 identifier 必须来源于：

```text
已确认 StorageMapping
StorageColumnMapping
Dataset 系统登记信息
```

如果项目已经存在动态表名/字段名安全处理逻辑，直接复用；否则增加严格 identifier 校验。

## 28. DatasetMetadata 数据库结构检查

确认现有 `dataset_metadata` 是否已经包含：

```text
metadata_group
metadata_key
metadata_name
metadata_value
value_type
remark
```

如果已经存在则沿用。

如果缺少 `value_type`，按照项目现有数据库 migration 方式补充。

确认是否存在唯一约束：

```sql
UNIQUE (
    dataset_id,
    metadata_group,
    metadata_key
)
```

如果不存在，则按项目当前数据库迁移方式增加。

不要新建第二张 metadata 表。

## 29. DatasetMetadata UPSERT

整理或增加 DatasetMetadataService 内部统一方法：

```java
upsertMetadata(...)
```

参数至少支持：

```text
datasetId
group
key
name
value
valueType
```

人工维护、配置派生和执行派生都复用该基础能力。

对于允许移除的 MANUAL 属性，可以提供内部：

```java
deleteMetadata(...)
```

## 30. updateDatasetMetadata 后端处理

继续保留现有 API。

请求：

```json
{
  "datasetName": "...",
  "datasetDesc": "...",
  "items": [...]
}
```

处理规则：

1. 更新 Dataset 名称和说明；
2. 遍历 items；
3. 根据 `metadataGroup + metadataKey` 查 Metadata Definition；
4. 只有 `maintainMode == MANUAL` 才允许通过该接口写入；
5. STORAGE 拒绝；
6. STATISTICS 拒绝；
7. `business_key` 拒绝；
8. 校验 valueType；
9. JSON 类型做 JSON 格式校验；
10. `data_time_field` 做字段存在性和时间字段类型校验；
11. `data_granularity` 校验 unit / interval；
12. `update_frequency` 校验 unit / interval。

标准属性的 `metadataName`、`valueType` 应由 Metadata Definition 决定，不信任前端提交值。

## 31. 人工属性清空

对于 MANUAL 属性，如果用户清空并保存，推荐直接删除对应 DatasetMetadata 当前记录。

不要为所有空属性创建：

```text
metadata_value = ''
```

记录。

继续保持稀疏元数据模型。

## 32. 前端 Metadata Definition

前端增加独立配置文件，例如：

```text
datasetMetadataDefinitions.ts
```

不要继续在 DatasetDetailPage 内堆积大量 key 判断。

建议定义：

```ts
{
  group,
  key,
  name,
  valueType,
  maintainMode,
  component,
  options?
}
```

本期不新增 Metadata Definition 后端 API。

前端 definition 负责 UI，后端 definition 负责最终校验。

## 33. DatasetDetailPage 页面结构

页面顺序调整为：

```text
基础信息

业务信息

管理信息

数据统计

存储信息

扩展信息

字段结构
```

字段结构保持在所有元数据区域下方。

## 34. 业务信息展示

至少展示：

```text
业务定义
业务范围
数据粒度
业务主键
数据时间字段
数据单位
货币类型
```

缺少值时统一显示：

```text
—
```

`business_key` 使用字段业务名称展示。

`data_time_field` 使用字段业务名称展示。

## 35. 管理信息展示

至少展示：

```text
数据负责人
维护人员
联系方式
更新频率
敏感级别
使用范围
保留策略
```

## 36. 数据统计展示

只读展示：

```text
数据总条数
时间覆盖
统计更新时间
```

不要直接展示：

```text
time_coverage_field
```

## 37. 存储信息展示

只读展示：

```text
存储类型
数据源编码
Schema
物理表
物理唯一键
```

## 38. 元数据维护 Drawer

Drawer 只提供：

```text
maintainMode == MANUAL
```

的属性。

可以编辑：

```text
business_definition
business_scope
data_granularity
data_time_field
unit
currency

data_owner
maintainer
contact
update_frequency
sensitivity_level
usage_scope
retention_policy

tags
keywords
custom_remark
custom_properties
```

不能编辑：

```text
business_key
全部 STORAGE
全部 STATISTICS
```

## 39. 空元数据 Dataset 也必须能够维护

不要再根据当前 `detail.metadata` 已经存在什么属性来决定 Drawer 中显示什么。

应该：

```text
Metadata Definitions
        +
当前 detail.metadata
        ↓
构造编辑模型
```

即使 Dataset 当前没有 `BUSINESS.business_definition`，Drawer 仍然显示：

```text
业务定义
[请输入业务定义]
```

用户填写保存后再创建 DatasetMetadata 记录。

## 40. data_time_field 编辑组件

使用：

```text
detail.structure
```

构建字段选择器。

下拉显示：

```text
交易日期
DATE

创建时间
DATETIME
```

实际 value：

```text
fieldCode
```

不要提供纯文本输入。

## 41. data_granularity 编辑组件

使用：

```text
单位 Select
+
间隔 InputNumber
```

例如：

```text
单位：[日]
间隔：[1]
```

保存：

```json
{
  "unit": "DAY",
  "interval": 1
}
```

如果选择：

```text
TICK
EVENT
SNAPSHOT
STATIC
```

隐藏 interval。

## 42. update_frequency 编辑组件

同样使用：

```text
单位 Select
+
间隔 InputNumber
```

例如：

```text
单位：[日]
间隔：[1]
```

保存：

```json
{
  "unit": "DAY",
  "interval": 1
}
```

展示：

```text
每1日
```

例如：

```json
{
  "unit": "MINUTE",
  "interval": 5
}
```

展示：

```text
每5分钟
```

不增加 `calendar`。

## 43. 前端辅助函数

建议将以下逻辑从 template 中抽出：

```ts
formatGranularity(...)
formatUpdateFrequency(...)
resolveFieldName(...)
resolveFieldNames(...)
getMetadata(...)
```

避免 template 内包含复杂 JSON 解析和字段匹配逻辑。

## 44. 保存行为

继续沿用：

```text
updateDatasetMetadata(...)
    ↓
成功
    ↓
关闭 Drawer
    ↓
getDatasetDetail(...)
```

以后端返回的数据作为最终状态。

如果保存失败：

- Drawer 不关闭；
- 保留用户输入；
- 显示错误消息。

## 45. 数据兼容

对于已有 Dataset：

- 不强制一次性补齐所有 BUSINESS / MANAGEMENT / EXTENSION 属性；
- 继续保持稀疏元数据；
- STORAGE 和系统派生属性在下一次 Dataset 成功更新时自动补齐。

如果历史存在旧格式：

```text
BUSINESS.data_granularity = "股票+交易日"
```

前端读取时可以做简单兼容。

用户重新编辑并保存后，统一写成新的 JSON 格式。

如果系统当前尚无正式历史数据，则不需要增加复杂数据迁移逻辑。

## 46. 推荐实施顺序

```text
1. 定位现有 DatasetMetadata / Dataset 登记代码

2. 增加 Metadata Group、Definition、MaintainMode

3. 检查 dataset_metadata 表结构和唯一索引

4. 改造 updateDatasetMetadata 后端校验

5. 实现 business_key / storage_unique_key 派生

6. 实现 STATISTICS 准确统计

7. 将派生和统计接入现有 TaskRun SUCCESS → Dataset update 链路

8. 修改 DatasetDetailPage 页面结构

9. 修改元数据维护 Drawer

10. 实现 data_granularity / update_frequency 编辑与格式化

11. 实现 business_key / data_time_field 的字段名称解析

12. 实现 time_coverage_field 有效性判断

13. 处理现有代码中的编译错误和类型错误

14. 前端、后端分别完成基本编译
```

## 47. 完成要求

本次不要求额外建设完整自动化测试、验收脚本或专项测试框架。

修改完成后至少保证：

```text
Spring Boot 后端正常编译通过
```

以及：

```text
Vue / TypeScript 前端正常编译通过
```

如果仓库已有标准构建命令，使用项目现有命令执行编译检查。

不要为了本次改造额外重构无关模块。

## 48. Codex 特别注意

不要因为实现本需求而：

```text
重写现有采集任务执行框架

重写 StorageMapping

新增 DatasetStorage 表

新增 Metadata 历史版本表

修改现有 API URL

修改现有 queryApi 方法名称

将字段定义复制到 DatasetMetadata

根据 Cron 推导 update_frequency

给 update_frequency 增加 calendar 字段

在修改 data_time_field 时联动删除 STATISTICS

使用 processedCount 推算 record_count
```

最终保持以下职责：

```text
人工描述
    → MANUAL Metadata

配置能够确定的信息
    → CONFIG_DERIVED Metadata

真实数据才能确定的信息
    → EXECUTION_DERIVED Metadata
```

Dataset、DataTypeVersion、StorageMapping、TaskRun、DatasetMetadata 保持现有职责边界。
