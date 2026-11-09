# AssetType 与应用指标标的类型改造实施说明（Codex 执行版）

## 1. 改造目标

在现有采集任务、Dataset 和 ApplicationMetric 设计基础上，统一标的类型在 Python、Java、Dataset、应用指标和统一指标取数链路中的使用方式。

本次改造目标：

1. Python 端继续使用已经统一的 `AssetType` 枚举；
2. Java 端增加对应 `AssetType` 枚举，并放在 `com.exchange.platform.common.core.enums` 包下；
3. `TaskTemplateVersion.asset_type` 使用统一枚举编码保存；
4. Dataset 增加正式核心属性 `asset_type`，由实际执行使用的 TaskTemplateVersion 派生；
5. ApplicationMetric 增加 `target_type`，由用户在创建指标时选择；
6. 创建指标时，根据 `target_type` 过滤可选 Dataset；
7. 保存指标时，后端再次校验 `ApplicationMetric.target_type == Dataset.asset_type`；
8. 下游筛选、排行、对比、提醒和统一指标取数链路统一使用 `targetType`；
9. 不将 `asset_type` 作为普通 DatasetMetadata 保存；
10. 不增加额外的跨语言 Schema/OpenAPI 枚举契约生成机制。

本次改造不重构现有采集任务框架、Dataset 主流程、ApplicationMetric 主体设计以及指标取数主链路。

## 2. AssetType 统一枚举

### 2.1 Python 端

Python 端已经统一为：

```python
from enum import Enum

class AssetType(str, Enum):
    STOCK = "STOCK"
    INDEX = "INDEX"
    FUTURE = "FUTURE"
    FUND = "FUND"
    BOND = "BOND"
```

采集任务注册时继续直接使用：

```python
asset_type=AssetType.STOCK
```

Python 端不再使用“股票”“指数”“基金”等中文字符串作为协议值。

### 2.2 Java 端

在：

```text
com.exchange.platform.common.core.enums
```

包下增加：

```java
AssetType
```

定义：

```java
package com.exchange.platform.common.core.enums;

public enum AssetType {

    STOCK("股票"),
    INDEX("指数"),
    FUTURE("期货"),
    FUND("基金"),
    BOND("债券");

    private final String displayName;

    AssetType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static AssetType fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        try {
            return AssetType.valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("不支持的资产类型: " + code);
        }
    }
}
```

系统内部、数据库和接口统一使用：

```text
STOCK
INDEX
FUTURE
FUND
BOND
```

中文名称只用于页面展示。

不要使用枚举 ordinal 保存数据库。

## 3. TaskTemplateVersion.asset_type

定位现有任务模板同步流程，包括：

```text
TaskTemplateInfo
TaskTemplateVersion
asset_type
assetType
任务模板同步
模板版本生成
```

Java 接收 Python 任务模板信息时，将 `asset_type` 按 Java `AssetType` 解析并保存标准编码。

处理链路：

```text
Python AssetType.STOCK
    ↓
JSON "STOCK"
    ↓
Java AssetType.STOCK
    ↓
task_template_version.asset_type = "STOCK"
```

如果收到不属于当前枚举的值，例如：

```text
EQUITY
股票
UNKNOWN
```

则任务模板同步失败并返回明确错误。

不要：

- 自动映射为 OTHER；
- 原样保存未知字符串；
- 长期保留中文旧值兼容逻辑。

如果开发数据库中存在旧中文值，可一次性迁移：

```text
股票 → STOCK
指数 → INDEX
期货 → FUTURE
基金 → FUND
债券 → BOND
```

## 4. Dataset 增加 asset_type

`asset_type` 作为 Dataset 正式核心属性，不放入 DatasetMetadata。

Dataset 表增加：

```text
asset_type
```

数据库保存：

```text
STOCK
INDEX
FUTURE
FUND
BOND
```

Dataset Entity / DO / DTO 增加对应字段，优先使用：

```java
private AssetType assetType;
```

或沿用项目现有枚举持久化方式。

## 5. Dataset.asset_type 来源

Dataset 的 `asset_type` 固定来源于实际成功执行所使用的 TaskTemplateVersion：

```text
TaskRun
    ↓
ExecutionConfigSnapshot
    ↓
本次实际使用的 TaskTemplateVersion
    ↓
TaskTemplateVersion.asset_type
    ↓
Dataset.asset_type
```

不要从 TaskTemplate 当前最新版本反向推断。

## 6. Dataset 首次创建与后续更新

首次创建：

```text
TaskTemplateVersion.asset_type = STOCK
→ Dataset.asset_type = STOCK
```

后续更新同一 `dataset_code` 时，比较：

```text
Dataset.asset_type
```

和：

```text
本次 TaskTemplateVersion.asset_type
```

一致则正常更新。

如果：

```text
Dataset.asset_type = STOCK
TaskTemplateVersion.asset_type = INDEX
```

则：

1. 不自动覆盖 `Dataset.asset_type`；
2. 记录 Dataset 登记或配置不一致异常；
3. 已成功物理写入的 TaskRun 不反向修改为 FAILED；
4. 不将原股票 Dataset 静默转换为指数 Dataset。

标的类型发生变化时，应使用新的 Dataset 身份。

## 7. Dataset 查询接口

Dataset 列表、详情 DTO 增加：

```text
assetType
assetTypeName
```

例如：

```text
assetType = STOCK
assetTypeName = 股票
```

`assetType` 为正式值，`assetTypeName` 为展示值。

## 8. Dataset 下拉查询增加 assetType 过滤

应用指标新增/编辑使用的 Dataset 下拉接口增加可选参数：

```text
assetType
```

例如：

```text
assetType=STOCK
```

只返回：

```text
Dataset.asset_type = STOCK
```

且处于可用于指标配置状态的数据集。

优先扩展现有 Dataset 下拉接口，不创建重复接口。

## 9. ApplicationMetric 增加 target_type

`app_metric` 增加：

```text
target_type
```

数据库保存：

```text
STOCK
INDEX
FUTURE
FUND
BOND
```

ApplicationMetric Entity / DO / DTO 增加：

```java
private AssetType targetType;
```

`target_type` 表示：

```text
该应用指标适用于哪一种标的类型
```

例如：

```text
PE_TTM
target_type = STOCK
```

该字段由用户创建指标时明确选择。

## 10. ApplicationMetric 新增表单

新增指标表单中增加“标的类型”。

推荐数据来源区域顺序：

```text
标的类型 *
来源数据集 *
来源字段 *
字段类型
```

标的类型固定选项：

```text
STOCK  股票
INDEX  指数
FUTURE 期货
FUND   基金
BOND   债券
```

## 11. 前端联动规则

### 11.1 选择 targetType

用户选择：

```text
targetType = STOCK
```

Dataset 下拉请求自动增加：

```text
assetType = STOCK
```

只展示股票 Dataset。

### 11.2 修改 targetType

如果已选择：

```text
datasetCode
fieldCode
```

后修改：

```text
STOCK → INDEX
```

立即清空：

```text
datasetCode
fieldCode
```

并重新查询 `assetType=INDEX` 的 Dataset。

### 11.3 修改 Dataset

修改 `datasetCode` 时立即清空：

```text
fieldCode
```

重新读取所选 Dataset 当前 DataTypeVersion 下的 DataTypeField。

### 11.4 字段选择链路

继续沿用：

```text
选择 Dataset
    ↓
读取 Dataset 当前 DataTypeVersion
    ↓
读取 DataTypeField
    ↓
选择 fieldCode
```

## 12. ApplicationMetric 保存校验

保存请求至少包含：

```text
metricCode
metricName
metricGroup
targetType
unit
description
datasetCode
fieldCode
status
```

Spring Boot 保存前执行：

1. `targetType` 必须属于 AssetType；
2. `datasetCode` 对应 Dataset 必须存在且可用；
3. 必须满足：

```text
ApplicationMetric.target_type
==
Dataset.asset_type
```

否则拒绝：

```text
指标标的类型与来源数据集标的类型不一致
```

4. `fieldCode` 必须存在于 Dataset 当前 DataTypeVersion。

完整约束：

```text
Metric.targetType == Dataset.assetType
```

以及：

```text
Dataset.currentDataTypeVersion contains Metric.fieldCode
```

## 13. ApplicationMetric 编辑规则

`metricCode` 创建后不可修改。

`targetType` 创建后同样不可修改。

编辑页面中：

```text
targetType
```

只读展示。

如果现有设计允许修改来源 Dataset，则新的 Dataset 必须仍满足：

```text
Dataset.asset_type == ApplicationMetric.target_type
```

修改 Dataset 后重新选择 fieldCode。

## 14. ApplicationMetric 列表与查询

指标列表增加：

```text
标的类型
```

显示中文名称。

指标查询接口增加可选：

```text
targetType
```

过滤条件。

例如股票筛选页面：

```text
status = ENABLED
targetType = STOCK
```

只返回股票指标。

## 15. 下游模块统一按 targetType 过滤指标

以下模块仍通过 `metricCode` 保存指标引用：

```text
A-11 条件筛选
A-12 指标排行
A-12 标的对比
A-05 指标提醒
```

不重复保存 Dataset 和 Field。

但加载可选指标时增加当前业务标的类型：

```text
targetType
```

例如股票业务只加载：

```text
ApplicationMetric.status = ENABLED
ApplicationMetric.target_type = STOCK
```

的指标。

## 16. WatchTarget 与 Metric 类型一致性

涉及具体标的的下游业务必须校验：

```text
WatchTarget.targetType
==
ApplicationMetric.targetType
```

如果当前 WatchTarget 使用的字段名不是 `targetType`，则沿用现有字段，不增加重复字段。

## 17. MetricValue 增加 targetType

现有统一结构：

```text
MetricValue
- targetCode
- metricCode
- value
- dataTime
```

调整为：

```text
MetricValue
- targetType
- targetCode
- metricCode
- value
- dataTime
```

示例：

```json
{
  "targetType": "STOCK",
  "targetCode": "600519.SH",
  "metricCode": "PE_TTM",
  "value": 22.5,
  "dataTime": "2026-11-06"
}
```

统一把：

```text
targetType + targetCode
```

作为标的业务身份。

## 18. Spring Boot 指标解析链路

统一调整为：

```text
metricCode
    ↓
ApplicationMetric
    ├─ targetType
    ├─ datasetCode
    └─ fieldCode
    ↓
Dataset
    ├─ assetType
    └─ currentDataTypeVersion
    ↓
校验 targetType == assetType
    ↓
按 Dataset 聚合字段取数
    ↓
调用 Python/FastAPI
    ↓
MetricValue
targetType + targetCode + metricCode + value + dataTime
```

业务模块仍只提交：

```text
metricCode
标的范围
业务时间参数
```

不提交物理表和物理字段。

## 19. Python/FastAPI 指标取数调整

Spring Boot 下发指标取数上下文时携带：

```text
metricCode
targetType
datasetCode
fieldCode
```

Python 形成统一结果：

```text
targetType
targetCode
metricCode
value
dataTime
```

不同 Dataset 继续独立查询，不在 SQL 层跨 Dataset JOIN。

## 20. 数据库调整

### 20.1 dataset

增加：

```sql
asset_type VARCHAR(...)
```

实际长度按项目现有枚举字段规范确定。

### 20.2 app_metric

增加：

```sql
target_type VARCHAR(...)
```

### 20.3 task_template_version

如果现有 `asset_type` 已是 VARCHAR，不修改字段结构，只统一保存标准编码。

## 21. 历史数据处理

### 21.1 TaskTemplateVersion

旧中文值一次性迁移：

```text
股票 → STOCK
指数 → INDEX
期货 → FUTURE
基金 → FUND
债券 → BOND
```

### 21.2 Dataset

已有 Dataset 如果：

```text
asset_type IS NULL
```

按 Dataset 与实际来源 TaskConfig / ExecutionConfigSnapshot / TaskTemplateVersion 的关联补充。

无法可靠确定的保持 NULL，不人工猜测。

`asset_type` 为空的 Dataset 不允许用于新建 ApplicationMetric。

### 21.3 ApplicationMetric

已有指标数据按其绑定 Dataset.asset_type 补充：

```text
app_metric.target_type
```

## 22. DatasetMetadata 调整

不要新增：

```text
BUSINESS.asset_type
EXTENSION.asset_type
```

`asset_type` 的权威位置固定为：

```text
Dataset.asset_type
```

## 23. 前端公共 AssetType 选项

前端建立统一映射：

```ts
export const ASSET_TYPE_OPTIONS = [
  { value: 'STOCK', label: '股票' },
  { value: 'INDEX', label: '指数' },
  { value: 'FUTURE', label: '期货' },
  { value: 'FUND', label: '基金' },
  { value: 'BOND', label: '债券' },
]
```

用于：

```text
Dataset 列表/详情
ApplicationMetric 新增
ApplicationMetric 列表
下游指标筛选
标的类型展示
```

如果项目已有公共 enum/options 文件，则放入现有公共位置。

## 24. 后端中文名称转换

Java 统一通过：

```java
AssetType.getDisplayName()
```

取得中文名称。

不要在 Controller、Service、Mapper 中重复编写字符串判断。

## 25. 推荐修改顺序

Codex 按以下顺序修改：

```text
1. 定位现有 asset_type 同步链路
2. 确认 Python 已使用 AssetType 枚举
3. 在 com.exchange.platform.common.core.enums 增加 Java AssetType
4. 调整 TaskTemplateVersion 同步 DTO / Service / Entity
5. 为 dataset 表和 Dataset 对象增加 asset_type
6. 在 Dataset 首次创建和后续更新流程中写入并校验 asset_type
7. Dataset 列表 / 详情 / 下拉接口增加 assetType
8. Dataset 下拉查询增加 assetType 过滤
9. 为 app_metric 增加 target_type
10. ApplicationMetric DTO / Entity / Service 增加 targetType
11. 修改应用指标新增页面，增加标的类型选择
12. 实现 targetType → Dataset 下拉联动过滤
13. 修改 targetType 时清空 datasetCode / fieldCode
14. 修改 Dataset 时清空 fieldCode
15. ApplicationMetric 保存时增加 targetType == Dataset.assetType 校验
16. ApplicationMetric 编辑时将 targetType 设为只读
17. 指标列表和可用指标查询增加 targetType 展示与过滤
18. A-11 / A-12 / A-05 加载指标时传入当前标的类型过滤
19. MetricValue 增加 targetType
20. Spring Boot → Python/FastAPI 指标取数上下文增加 targetType
21. 处理必要的开发环境历史数据
22. 执行 Spring Boot 和 Vue/TypeScript 基本编译检查
```

## 26. 完成要求

本次不要求额外建设完整测试框架。

修改完成后至少保证：

```text
Spring Boot 后端正常编译通过
```

以及：

```text
Vue / TypeScript 前端正常编译通过
```

如果 Python 项目本次涉及 DTO 或枚举引用调整，则保证现有基础类型检查或启动导入不报错。

## 27. Codex 特别注意

不要：

```text
继续使用中文 asset_type 作为协议值
增加 OTHER 类型兜底未知 asset_type
在 Java 中长期维护中文到枚举的兼容映射
增加 OpenAPI / JSON Schema 代码生成体系
把 Dataset.asset_type 放到 DatasetMetadata
让 ApplicationMetric.target_type 从 Dataset 自动隐藏生成
允许 targetType 与 Dataset.assetType 不一致
允许用户修改已创建指标的 targetType
让业务模块直接提交物理表名或物理字段
重写现有指标统一取数主链路
```

最终链路固定为：

```text
Python AssetType
    ↓
TaskTemplateVersion.asset_type
    ↓
Dataset.asset_type

用户选择 ApplicationMetric.target_type
    ↓
按 Dataset.asset_type 过滤来源 Dataset
    ↓
后端保存时再次校验类型一致
    ↓
ApplicationMetric.target_type
    ↓
筛选 / 排行 / 对比 / 提醒 / MetricValue
```
