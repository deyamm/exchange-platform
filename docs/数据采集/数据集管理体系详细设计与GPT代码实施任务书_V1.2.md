# 个人辅助交易平台数据集管理体系

## 代码级实施设计与开发任务书

  --------------------------------------------------------------------------------------------------------------------
  项目                                内容
  ----------------------------------- --------------------------------------------------------------------------------
  文档名称                            数据集管理体系详细设计及代码实施说明

  适用项目                            个人辅助交易平台

  文档版本                            V1.0

  文档用途                            指导 GPT-5.6 结合现有代码库完成数据集管理功能设计、开发、联调和测试

  设计范围                            数据集主档、元数据、物理存储登记、写入前置校验、写入结果确认、数据集创建与更新

  核心技术                            Vue 3 / TypeScript、Spring Boot 2.7.18 / JDK 17、Python 3.11、HTTP/JSON

  设计状态                            待结合代码库核查并实施
  --------------------------------------------------------------------------------------------------------------------

------------------------------------------------------------------------

# 0. 给 GPT-5.6 的执行说明

你需要在当前项目代码库中实现本文定义的数据集管理能力。本文是业务和技术设计约束，不代表现有代码已经具备本文所述的类、表、接口或方法。**必须先检查代码库，再决定具体改造位置。不得根据本文虚构现有类名、表名、接口路径或代码结构。**

## 0.1 工作目标

基于现有个人辅助交易平台，实现数据集的自动生成、登记、更新和查询，打通以下执行链路：

``` text
Java 写入前置校验
    ↓
Java 生成执行配置快照并下发执行上下文
    ↓
Python 采集、清洗、字段映射
    ↓
Python 写入正式物理表
    ↓
Python 返回最终写入结果
    ↓
Java 确认执行及写入结果
    ↓
Java 创建或更新 Dataset
```

## 0.2 不可擅自改变的设计决策

1. 使用稳定的 `dataset_code` 标识一个长期存在的逻辑数据资产。
2. 本期不建设 `DatasetVersion`，只维护数据集当前状态。
3. 数据集字段结构统一引用 `DataTypeVersion`，不得在 Dataset 中复制一套字段标准。
4. 一个数据集固定对应一张正式物理表。
5. 一个采集任务配置在本期业务中只对应一个数据集。
6. 本期不建设独立的数据质量校验流程；数据集生成或更新以采集任务执行成功、物理写入成功为条件。
7. Java 负责业务配置、数据集相关写入前置校验、执行调度、结果确认和数据集登记。
8. Python 负责采集、清洗、字段映射和正式物理表写入。
9. Python 继续沿用项目当前的 `conn.execute(text(sql), rows)` 写入方式。不得未经核查擅自替换为其他写入框架。
10. 暂不建设写入凭证、回调丢失自动补偿、复杂重试机制、两阶段提交和强制暂存表机制。
11. 不设置 `DatasetMetadata` 表。存储类型、数据源编码、Schema 名称和物理表名称作为 DatasetMetadata 元数据项维护；其他原拟存储表字段不纳入本期数据集模型。
12. `dataset_status` 使用从 0 开始的整数枚举，不使用 VARCHAR，且不建立该字段索引。
14. 不推翻现有 DataTopic、DataType、DataTypeField、DataTypeVersion、TaskConfig、StorageMapping、TaskRun、ExecutionConfigSnapshot、ResultSummary 等对象。
15. 不在本次工作中建设业务消费系统关联、系统字段映射、数据血缘平台或完整数据治理平台。
16. 数据集写入前只校验数据集自身属性；数据类型、数据类型版本、存储映射、物理表、字段映射等配置继续由现有对应模块校验。
18. 不要求开展业务用例测试；交付前只进行必要的语法检查和项目编译检查。

## 0.3 必须遵守的代码实施方式

按以下顺序开展工作：

### 第一步：代码库盘点

- /backend  后端 SpringBoot 代码
- /backend/data-collection 采集任务相关代码
- /backend/data-analytics 数据分析相关代码，该部分代码你不需要检查与修改
- /backend/data-service-client springboot 调用 Python 的客户端代码，通过 executeTaskRun 方法向 python 下发执行请求
- /backend/ 下的其他包，通用内容，你不需要修改
- /backend/data-collection/data-collection-server/src/main/resources/db/migration 各数据库表的建库、索引等flyway脚本

- /data-server Python 数据处理代码
- /data-server/app/tasks/pipeline 采集任务的整体流程，你需要在该目录下查看整体采集任务是如何执行的
- /data-server/app/api/v1/collection 面向 SpringBoot 的采集任务的 API 接口
- /data-server/app/service/collection 执行采集任务并通过 SpringClient 向 SpringBoot 返回结果

- /frontend 前端 Vue 3 / TypeScript 代码
- /frontend/src/collectionconfig 采集任务配置相关页面和组件
- /fronted/src/collectionexecute 采集任务执行相关页面和组件
- /fronted/src/collectionMonitor 采集任务监控相关页面和组件

输出一份"现有实现与本文设计差异清单"。

### 第二步：形成改造计划

逐项说明：

-   需要新增的数据库表和字段；
-   需要修改的 Java 类及方法；
-   需要修改的 Python 执行上下文、写入结果结构及返回逻辑；
-   需要新增或调整的接口；
-   需要新增或调整的前端页面；
-   兼容性、迁移和回归测试影响。

如实际代码与本文存在差异，先说明差异和建议，不得静默改变业务设计。

### 第三步：实施

遵循现有项目的编码规范、包结构、命名、异常处理、事务管理、日志、鉴权和响应格式。优先复用现有代码，不重复建设同类能力。

### 第四步：基础检查

仅进行本次改动涉及范围内的基础语法检查和项目编译检查，例如 Python 语法检查、Java 编译及前端构建检查（如本次涉及前端）。本次不要求编写或执行业务用例测试、接口测试、联调测试或回归测试。

### 第五步：交付

输出：

-   改造文件清单；
-   数据库变更脚本；
-   接口变更说明；
-   核心流程说明；
-   基础语法与编译检查结果；
-   尚未实现或需要人工确认的事项。

------------------------------------------------------------------------

# 1. 建设目标与范围

## 1.1 建设目标

建立轻量化数据集资产管理能力，将采集任务成功写入物理表的数据登记为统一数据资产，实现：

1.  数据集稳定身份标识；
2.  数据集与数据主题、数据类型及数据类型版本关联；
3.  数据集元数据统一管理；
4.  数据集与唯一正式物理表建立一对一关系；
5.  数据集首次成功执行后自动创建；
6.  后续成功执行后更新原数据集当前信息；
7.  数据集目录、详情、字段结构、存储位置和来源执行信息可查询；
8.  通过现有执行记录和执行配置快照支持历史执行口径追溯。

## 1.2 本期范围

  ----------------------------------------------------------------------------------------------
  范围                                内容
  ----------------------------------- ----------------------------------------------------------
  数据集主档                          编码、名称、说明、主题、类型、状态

  数据集元数据                        业务描述、粒度、主键、时间字段、更新频率、负责人、标签等

  数据集存储信息                      数据库、Schema、物理表、分区字段等

  数据集生成                          首次成功执行后自动创建

  数据集更新                          后续成功执行后更新当前信息

  前置校验                            Java 在正式执行前校验数据集和存储配置

  结果确认                            Java 根据 Python 最终执行结果确认写入成功

  数据集查询                          列表、详情、元数据、结构、存储、来源

  状态管理                            草稿、启用、停用、归档
  ----------------------------------------------------------------------------------------------

## 1.3 本期明确不做

-   DatasetVersion 数据集内容版本表；
-   数据集历史内容快照；
-   数据质量规则引擎、质量评分和独立质量校验；
-   多物理表、多存储介质的复杂数据集模型；
-   数据集之间的关系管理；
-   数据集与业务消费系统关联；
-   字段级血缘；
-   写入凭证表；
-   回调丢失自动补偿机制；
-   两阶段提交协议；
-   强制暂存表；
-   与本次功能无关的采集体系重构。

------------------------------------------------------------------------

# 2. 核心概念与对象职责

## 2.1 核心概念

  --------------------------------------------------------------------------------------------------
  对象                      定义                    关键职责
  ------------------------- ----------------------- ------------------------------------------------
  DataTopic                 数据主题                业务目录分类

  DataType                  数据类型                标准逻辑数据结构定义

  DataTypeField             数据类型字段            标准字段定义

  DataTypeVersion           数据类型版本            固化某一时点的字段结构

  TaskConfig                采集任务配置            绑定任务模板、主题、类型版本、规则和存储映射

  StorageMapping            存储映射                绑定任务输出、数据集标识、物理表和字段映射

  TaskRun                   执行记录                记录一次任务执行过程

  ExecutionConfigSnapshot   执行配置快照            固化本次执行使用的配置上下文

  ResultSummary             执行结果摘要            保存本次处理数量、成功数量、异常数量及定位信息

  Dataset                   数据集主档              标识和管理长期存在的数据资产

  DatasetMetadata           数据集扩展元数据        保存业务描述类扩展属性

  --------------------------------------------------------------------------------------------------

## 2.2 数据集定义

数据集是按照统一数据标准、业务含义和数据组织规则，对实际采集并存储的数据形成的逻辑数据资产。

数据集不是采集任务，不是数据类型，也不等同于物理表。

一个数据集具有稳定 `dataset_code`。任务每次成功执行时，使用同一个
`dataset_code` 找到并更新同一数据集，不因执行次数创建新数据集。

## 2.3 数据集与数据类型的区别

-   DataType 回答"数据是什么类型、标准结构是什么"。
-   DataTypeVersion 回答"某个明确版本的数据结构包含哪些字段"。
-   Dataset 回答"实际沉淀的数据资产是什么"。
-   DatasetMetadata 回答"该数据集实际存储在哪张正式物理表"。

Dataset 只引用一个当前有效的 DataTypeVersion，不复制字段结构。

## 2.4 数据集与采集任务的关系

本期业务中，一个采集任务配置只对应一个数据集，通过该任务配置关联的 StorageMapping 提供稳定的 `dataset_code` 和目标物理表信息。

- 一个 TaskConfig 对应一个数据集输出；
- 一个 Dataset 由其对应的任务配置持续更新；
- 任务每次成功执行均使用相同的 `dataset_code`；
- 不按任务执行次数创建新数据集；
- 本期不设计一个任务配置输出多个数据集的能力。

# 3. 总体架构与职责边界

## 3.1 分层关系

``` text
数据集管理及查询
    Dataset / DatasetMetadata
                  ↑
数据标准
    DataTopic / DataType / DataTypeField / DataTypeVersion
                  ↑
Java 采集配置与执行控制
    TaskConfig / StorageMapping / TaskRun / ExecutionConfigSnapshot
                  ↓ HTTP/JSON
Python 任务处理
    采集 → 清洗 → 标准化 → 字段映射 → 物理表写入
                  ↓
物理数据存储
    数据库中的正式物理表
```

## 3.2 Java 职责

Java 负责：

1.  管理 Dataset、DatasetMetadata、DatasetMetadata；
2.  维护稳定 dataset_code；
3.  在执行前校验数据集身份和写入配置；
4.  生成执行配置快照；
5.  下发执行上下文；
6.  接收 Python 最终执行结果；
7.  核对执行结果与执行快照的一致性；
8.  更新 TaskRun 和 ResultSummary；
9.  创建或更新 Dataset；
10. 提供数据集查询和元数据查询接口。

Java 不负责：

-   实际采集数据；
-   执行 Python 数据清洗逻辑；
-   直接写入 Python 负责的业务物理表；
-   对正式写入的数据做全量质量扫描；
-   实现本期明确排除的写入凭证和复杂补偿机制。

## 3.3 Python 职责

Python 负责：

1.  接收 Java 下发的执行上下文；
2.  参数校验；
3.  数据采集；
4.  数据清洗；
5.  原始字段到标准字段的映射；
6.  标准字段到物理字段的映射；
7.  使用现有数据库连接方式写入物理表；
8.  在现有事务管理机制下完成提交或失败处理；
9.  返回最终执行状态、写入状态及必要统计信息。

Python 不负责：

-   创建或更新 Dataset；
-   决定数据集业务归属；
-   修改 Dataset 管理状态；
-   自行决定 DataTypeVersion；
-   自行改变目标数据库或物理表；
-   绕过 Java 配置直接选择数据集编码。

------------------------------------------------------------------------

# 4. 数据集身份、编码和粒度

## 4.1 数据集编码

采用稳定编码：

``` text
DS_{业务标识}_{数据内容标识}
```

示例：

  数据集名称     编码
  -------------- ------------------
  股票基础信息   DS_STOCK_BASIC
  股票日行情     DS_STOCK_DAILY
  股票周行情     DS_STOCK_WEEKLY
  股票财务数据   DS_STOCK_FINANCE
  股票公告信息   DS_STOCK_NOTICE

编码约束：

1.  全局唯一；
2.  大写英文、数字及下划线；
3.  创建后不可随意修改；
4.  不包含任务执行日期、批次号和内容版本号；
5.  不依赖物理表名称生成；
6.  不依赖任务编码生成；
7.  编码由 Java 配置管理，Python 只接收和回传。

## 4.2 数据集粒度

以逻辑数据资产为粒度，不按任务执行次数或简单按物理表自动创建。

建议判断条件：

-   业务含义相同；
-   记录粒度相同；
-   业务主键规则一致；
-   标准数据结构一致；
-   更新方式可以统一；
-   物理表归属明确。

示例：

  数据集         数据粒度               示例唯一键             更新方式
  -------------- ---------------------- ---------------------- -----------------
  股票基础信息   一只股票一条记录       市场+股票代码          全量覆盖/UPSERT
  股票日行情     股票+交易日            市场+股票代码+交易日   UPSERT
  股票财务数据   股票+报告期+报表类型   股票+报告期+报表类型   UPSERT
  股票公告数据   一条公告一条记录       公告编号               APPEND

上述业务主键仅为示例，实际以当前数据类型和项目业务定义为准。

## 4.3 数据集创建和复用规则

  情况                        处理
  --------------------------- -----------------------------------
  dataset_code 首次成功执行   创建 Dataset
  dataset_code 已存在         更新原 Dataset
  编码相同但 DataType 不同    前置校验失败
  编码相同但物理表不同        前置校验失败
  数据内容每天变化            更新原 Dataset
  DataTypeVersion 变化        经配置确认后更新 Dataset 当前引用
  物理表名称调整              本期不允许通过普通执行自动调整
  任务执行失败                不创建、不更新 Dataset

------------------------------------------------------------------------

# 5. 数据库设计

> 实际表名、字段类型、主键策略、审计字段、逻辑删除字段、时间类型及索引规范必须先检查现有数据库设计，再按项目规范实施。以下为逻辑设计和建议字段，不得不经核对直接覆盖已有表结构。

## 5.1 Dataset 数据集主档

建议表名：`dataset`

  ----------------------------------------------------------------------------------------------------------
  字段                   建议类型                          必填 说明
  ---------------------- ---------------- --------------------- --------------------------------------------
  id                     BIGINT                              是 主键

  dataset_code           VARCHAR(64)                         是 全局唯一、稳定编码

  dataset_name           VARCHAR(128)                        是 数据集名称

  dataset_desc           VARCHAR(1000)                       否 数据集说明

  topic_id               BIGINT                              是 所属数据主题

  data_type_id           BIGINT                              是 所属数据类型

  data_type_version_id   BIGINT                              是 当前引用的数据类型版本

  task_config_id         BIGINT                              是 当前关联任务配置

  storage_mapping_id     BIGINT                              是 当前存储映射

  latest_task_run_id     BIGINT                              否 最近一次成功执行记录

  dataset_status         TINYINT                         是 数据集状态

  record_count           BIGINT                              否 当前物理表记录数量；无法获得可信值时可为空

  data_start_time        DATETIME                            否 当前数据覆盖起始时间

  data_end_time          DATETIME                            否 当前数据覆盖结束时间

  last_collect_time      DATETIME                            否 最近采集时间

  last_success_time      DATETIME                            否 最近成功时间

  created_at             DATETIME                            是 创建时间

  updated_at             DATETIME                            是 更新时间

  created_by             VARCHAR                         按规范 创建人

  updated_by             VARCHAR                         按规范 更新人

  remark                 VARCHAR(500)                        否 备注
  ----------------------------------------------------------------------------------------------------------

约束：

-   `dataset_code` 唯一索引；
-   `topic_id`、`data_type_id` 建议索引；`dataset_status` 不建立索引。
-   `data_type_id` 创建后原则上不可变；
-   `dataset_code` 不可修改；
-   `record_count` 不能简单累加本次处理数量；
-   Dataset 不保存历史内容版本。

## 5.2 DatasetMetadata 数据集扩展元数据

建议表名：`dataset_metadata`。

用于保存数据集的业务描述、管理属性和存储定位属性。数据集核心属性保存在 Dataset；数据结构属性从 DataTypeVersion 获取；扩展属性通过 DatasetMetadata 管理。

建议字段：

| 字段 | 建议类型 | 说明 |
|---|---|---|
| id | BIGINT | 主键 |
| dataset_id | BIGINT | 数据集 ID |
| metadata_group | VARCHAR(32) | 元数据分类 |
| metadata_key | VARCHAR(64) | 元数据编码 |
| metadata_name | VARCHAR(128) | 元数据名称 |
| metadata_value | TEXT | 元数据值 |
| value_type | VARCHAR(32) | 值类型 |
| remark | VARCHAR(500) | 说明 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

建议元数据分类：

- `BUSINESS`：业务语义；
- `MANAGEMENT`：管理属性；
- `STORAGE`：存储定位属性；
- `EXTENSION`：扩展属性。

建议预置以下存储元数据项：

| metadata_group | metadata_key | metadata_name | 说明 |
|---|---|---|---|
| STORAGE | storage_type | 存储类型 | 数据存储类型 |
| STORAGE | datasource_code | 数据源编码 | 对应现有数据源配置编码 |
| STORAGE | schema_name | Schema 名称 | 数据库 Schema；不适用时为空 |
| STORAGE | table_name | 物理表名称 | 数据集对应的唯一正式物理表名称 |

以上四项作为 DatasetMetadata 记录维护；DatasetStorage 不作为数据库表或独立实体建设。

其他业务元数据示例：

| metadata_group | metadata_key | metadata_name | 示例 |
|---|---|---|---|
| BUSINESS | DATA_GRANULARITY | 数据粒度 | 股票+交易日 |
| BUSINESS | PRIMARY_KEY | 业务主键 | security_code,trade_date |
| BUSINESS | DATA_TIME_FIELD | 数据时间字段 | trade_date |
| BUSINESS | UPDATE_FREQUENCY | 更新频率 | 每个交易日 |
| MANAGEMENT | DATA_OWNER | 数据负责人 | 数据维护负责人 |
| EXTENSION | TAGS | 数据集标签 | 行情、日频 |

不建议在 DatasetMetadata 重复存储 DataTypeField 字段定义。字段结构统一从 Dataset 当前引用的 DataTypeVersion 获取。

## 5.3 不新增 DatasetVersion

本期明确不建立：

-   `dataset_version`；
-   数据集内容快照表；
-   数据集历史版本状态表。

历史执行仍通过以下对象管理：

-   TaskRun；
-   ExecutionConfigSnapshot；
-   ResultSummary；
-   RunLog；
-   ExceptionRecord；
-   ProblemRecord。

# 6. 数据集与现有对象关系

``` mermaid
erDiagram
    DataTopic ||--o{ Dataset : classifies
    DataType ||--o{ DataTypeVersion : versions
    DataType ||--o{ Dataset : defines
    DataTypeVersion ||--o{ Dataset : current_structure
    Dataset ||--o{ DatasetMetadata : describes
    Dataset ||--|| DatasetMetadata : locates

    TaskConfig ||--o{ StorageMapping : configures
    StorageMapping }o--|| Dataset : outputs

    TaskConfig ||--o{ TaskRun : executes
    TaskRun ||--|| ExecutionConfigSnapshot : snapshots
    TaskRun ||--|| ResultSummary : produces
```

说明：

1.  DataTopic 与 Dataset 为一对多。
2.  DataType 与 Dataset 为一对多。
3.  Dataset 当前引用一个 DataTypeVersion。
4.  Dataset 与 DatasetMetadata 为一对一。
5.  Dataset 与 DatasetMetadata 为一对多。
6.  TaskConfig 通过 StorageMapping 配置数据集输出。
7.  TaskRun 通过 ExecutionConfigSnapshot
    固化本次执行的数据集编码及目标表信息。
8.  TaskRun 和 ResultSummary 负责记录每次执行情况。
9.  Dataset 只保存当前状态，不承担历史执行记录职责。

------------------------------------------------------------------------

# 7. 任务配置和存储映射调整

## 7.1 数据集输出标识

在现有 StorageMapping 中增加或明确 `dataset_code`，用于标识该任务配置所对应的唯一数据集。

建议配置项：

- `dataset_code`：数据集稳定编码；
- `dataset_name`：可选，首次登记时的数据集名称；
- `dataset_desc`：可选，首次登记时的数据集说明。

如果当前 StorageMapping 已存在等价字段，优先复用，不重复增加。

本期一个 TaskConfig 只对应一个数据集输出。由该任务配置关联的 StorageMapping 提供 dataset_code；不设计多输出映射。

## 7.2 数据集属性配置校验

在任务配置确认或存储映射启用前，针对数据集自身属性进行必要检查：

1. `dataset_code` 非空且格式合法；
2. `dataset_code` 不与其他数据集身份冲突；
3. 已有数据集的状态允许继续使用；
4. 已有数据集的编码与当前输出配置一致。

数据类型、数据类型版本、存储映射、目标物理表、字段映射等属性，继续由现有数据类型管理、任务配置和存储映射管理流程校验。本节不重复定义这些已有校验。

# 8. 写入前置校验设计（Java）

## 8.1 执行前校验位置

在 M-05 创建正式执行请求、向 Python 下发执行请求之前执行。

建议调用链：

``` text
创建 TaskRun
    ↓
读取 TaskConfig
    ↓
读取 StorageMapping
    ↓
解析 dataset_code
    ↓
查询 Dataset
    ↓
执行数据集自身属性校验
    ↓
生成 ExecutionConfigSnapshot
    ↓
下发 Python
```

应结合现有代码确认 TaskRun 和 Snapshot 的创建顺序。关键要求是：任何正式
Python 写入请求发出前，前置校验必须完成。

## 8.2 校验规则

| 校验项 | 规则 | 失败处理 |
|---|---|---|
| dataset_code | 必须存在且格式合法 | 禁止执行 |
| 数据集状态 | 数据集已存在时，状态必须允许继续写入 | 禁止执行 |
| 数据集身份 | 按 dataset_code 查询，确保对应唯一数据集 | 禁止执行 |

首次生成数据集时，Dataset 主档允许不存在。Java 只需确认 dataset_code 已配置且合法；其他执行配置的有效性由现有流程负责。

## 8.3 已有数据集和首次生成的差异

### 首次生成

- Dataset 不存在；
- dataset_code 已在任务输出配置中确定且合法；
- 通过现有配置流程完成的其他校验继续沿用；
- 前置校验通过后允许执行；
- Python 写入成功后 Java 创建 Dataset。

### 后续更新

- 通过 dataset_code 找到 Dataset；
- 检查 Dataset 状态是否允许继续使用；
- 不在本模块重复检查数据类型、数据类型版本、存储映射、物理表及字段映射；
- Python 写入成功后 Java 更新 Dataset 当前信息。

# 9. ExecutionConfigSnapshot 调整

本次只对现有执行配置快照和下发给 Python 的上下文作最小必要扩展，不重构或重排已有上下文结构。

需要检查现有快照是否已经包含数据集编码：

- 如果已包含稳定的数据集编码，直接复用；
- 如果未包含，仅增加 `dataset_code`，其值来自现有任务输出配置/StorageMapping。

现有快照中的数据类型版本、存储映射、数据库、物理表、字段映射和执行参数等字段继续沿用，不重复定义或大幅调整。

首次生成数据集时，Dataset 主档可以不存在；Python 不需要接收 dataset_id，只需接收本次执行对应的 dataset_code。

# 10. Python 数据写入与结果返回

## 10.1 写入方式

沿用项目现有 Python 写入方式：

``` python
conn.execute(text(sql), rows)
```

实施前必须检查：

-   `conn` 的具体类型；
-   SQLAlchemy 版本；
-   `conn` 是 Connection 还是其他封装；
-   当前事务是如何开启和提交的；
-   异常时是否自动回滚；
-   `rows` 的参数结构；
-   批量执行返回值是否能准确提供新增、更新数量；
-   当前执行成功状态是在 SQL 执行后还是事务提交后设置。

不得假定 `execute()` 返回就必然代表事务已经提交。

应沿用项目当前事务模式，保证只有实际提交成功后才返回最终写入成功。

## 10.2 Python 现有执行过程及字段调整

Python 端必须完整遵循项目当前已经实现的采集执行过程、处理顺序、函数组织、异常处理和数据库写入方式。本次不重新编排、不拆分、不替换现有执行步骤，也不在本文中重新描述已有采集执行步骤。

本次只检查并按需调整 Java 传入的执行上下文和 Python 返回结果字段：

- 执行上下文：仅在现有结构尚未包含时增加 `dataset_code`；
- 执行结果：仅在现有结构尚未包含时增加 `datasetCode`，供 Java 将本次执行结果对应到数据集；
- 其他上下文、返回结果字段、字段命名、结构和处理流程均沿用当前实现。

继续使用现有 `conn.execute(text(sql), rows)` 写入方式。不得为了本次数据集管理改造大幅调整已有上下文或返回结果结构。

## 10.3 Python 返回结果

沿用当前 Python 执行结果的数据结构、字段命名和响应协议，不重新定义一套完整结果对象。

本次只增加数据集登记必需的信息：

- `datasetCode`：本次执行对应的稳定数据集编码。若现有结果中已有等价字段，直接复用。
- 写入成功状态：优先复用现有最终执行状态。现有结果若已能明确表示物理写入完成，不再增加重复状态字段；只有现有结果无法区分写入是否完成时，才考虑增加最小必要的写入状态字段，并先说明原因。

现有 taskRunId、执行状态、结果摘要、统计信息、错误信息及其他返回字段保持不变。数据库、物理表、处理数量等信息如已存在于现有执行上下文或结果中，继续复用，不为本次改造重复增加字段。

Java 应以执行快照中的 dataset_code 与 Python 返回的 datasetCode 进行对应确认。

## 10.4 Python 失败返回

写入失败时返回明确的失败状态和业务可读错误摘要。

不得把数据库连接密码、Token、完整敏感配置或不必要的内部堆栈直接返回前端。

------------------------------------------------------------------------

# 11. Java 写入结果确认与数据集登记

## 11.1 后置确认原则

Python 返回后，Java 沿用现有执行结果处理机制，只进行数据集登记所必需的确认，不重新执行写入前已完成的业务配置校验。

确认内容：

1. `taskRunId` 与当前执行记录匹配；
2. `datasetCode` 与执行配置快照中的 `dataset_code` 一致；
3. 沿用现有最终执行状态，确认任务及物理表写入已成功；
4. 现有返回结果中与本次执行相关的必要字段能够正常解析。

优先复用当前 Python 返回结果中的状态字段和执行信息，不新增重复的写入状态、数据库名称、物理表名称或统计字段。只有在检查现有结果结构后确认无法判断写入是否成功时，才提出最小必要字段补充，并说明原因。

## 11.2 登记前提

同时满足以下条件时，Java 创建或更新 Dataset：

- 当前执行记录与返回结果对应；
- 现有最终执行状态明确表示任务及物理表写入成功；
- Python 返回的 `datasetCode` 与执行快照中的 `dataset_code` 一致。

本期不再做独立数据质量校验，也不重复检查已由现有配置模块校验的数据类型、数据类型版本、存储映射、物理表和字段映射。

## 11.3 首次创建

根据 dataset_code 查询 Dataset。

不存在时：

1.  创建 Dataset；
2.  写入 dataset_code、dataset_name、topic_id、data_type_id；
3.  写入本次 data_type_version_id；
4.  写入 task_config_id、storage_mapping_id；
5.  写入 latest_task_run_id；
6.  写入当前可获得的统计信息；
7.  将存储类型、数据源编码、Schema 名称和物理表名称登记为 DatasetMetadata；
8.  登记必要的其他 DatasetMetadata；
9.  设置数据集状态为可用状态。

## 11.4 后续更新

Dataset 已存在时：

1.  保持 dataset_code 不变；
2.  保持 data_type_id 不变；
3.  保持正式物理表身份不变；
4.  更新当前 data_type_version_id；
5.  更新 task_config_id 和
    storage_mapping_id（仅在预先允许的配置变更范围内）；
6.  更新 latest_task_run_id；
7.  更新 last_collect_time 和 last_success_time；
8.  根据可信统计更新 record_count；
9.  根据可信数据范围更新 data_start_time、data_end_time；
10. 更新必要的元数据，包括 `storage_type`、`datasource_code`、`schema_name`、`table_name` 等存储元数据项；

不得覆盖用户手工维护的
dataset_name、dataset_desc、负责人、标签等字段，除非项目已有明确的自动同步规则。

## 11.5 record_count 更新规则

-   不得将 processedCount 直接累加到 record_count；
-   UPSERT 的处理数量不等于新增记录数；
-   如果 Python 返回的是当前总记录数且其统计口径可信，可以更新；
-   如果 Python 只返回本次处理数量，则不要用它更新当前总记录数；
-   不应为了获取 record_count 而在每次执行后强制扫描大表。

## 11.6 登记事务

Java 端 Dataset、DatasetMetadata 的一次创建或更新，应尽可能在同一个 Java 数据库事务中完成。

如果登记过程失败：

-   回滚本次 Java 数据集管理表的修改；
-   记录登记失败原因；
-   保留 TaskRun 的执行成功状态；
-   不回滚 Python 已经提交的物理数据。

本期不要求自动补偿调度。

------------------------------------------------------------------------

# 12. 状态设计

本节状态字段统一采用从 0 开始的整数枚举值，不使用 VARCHAR 保存状态名称。实施时必须优先核查现有代码和数据库中的枚举数值定义；已有实现已采用 0 起始数值编码时，应沿用现有数值，不得为符合文档而擅自改动既有编码或历史数据。

## 12.1 TaskRun 执行状态

沿用现有 TaskRun 执行状态及其数值映射。若当前实现尚未形成统一数值映射，建议采用以下从 0 开始的枚举：

| 枚举值 | 状态 | 含义 |
|---:|---|---|
| 0 | CREATED | 已创建 |
| 1 | PENDING | 待执行 |
| 2 | RUNNING | 执行中 |
| 3 | SUCCESS | 执行成功 |
| 4 | FAILED | 执行失败 |

如现有实现的数值定义与上述建议不同，以现有实现为准，并在代码改造说明中记录实际映射。

## 12.2 数据集状态

`dataset_status` 使用整数类型（建议 TINYINT 或 SMALLINT），不使用 VARCHAR，不建立该字段索引。

建议从 0 开始定义：

| 枚举值 | 状态 | 含义 |
|---:|---|---|
| 0 | DRAFT | 草稿 |
| 1 | ENABLED | 已启用 |
| 2 | DISABLED | 已停用 |
| 3 | ARCHIVED | 已归档 |

实施前检查现有数据集状态枚举及数据库存量值。如现有实现已使用从 0 开始的其他状态映射，应保持兼容并在设计落地说明中列明，不得直接改变既有状态语义。

数据集管理状态与 TaskRun 执行状态相互独立。

# 13. 核心业务流程

## 13.1 完整流程

``` mermaid
flowchart TD
    A[用户发起手动或定时任务] --> B[Java 创建 TaskRun]
    B --> C[Java 读取 TaskConfig 和 StorageMapping]
    C --> D[查询 Dataset 并执行写入前置校验]
    D --> E{校验通过}
    E -->|否| F[记录失败原因，不调用 Python]
    E -->|是| G[生成 ExecutionConfigSnapshot]
    G --> H[Java 下发 Python 执行请求]
    H --> I[Python 数据采集]
    I --> J[Python 清洗和字段映射]
    J --> K[Python conn.execute 写入物理表]
    K --> L{实际写入成功}
    L -->|否| M[Python 返回失败]
    L -->|是| N[Python 返回成功及写入统计]
    M --> O[Java 更新 TaskRun 失败]
    N --> P[Java 确认结果与执行快照一致]
    P --> Q{确认成功}
    Q -->|否| R[记录结果对应异常，不误改物理写入事实]
    Q -->|是| S[Java 更新 TaskRun SUCCESS 和 ResultSummary]
    S --> T[按 dataset_code 查询 Dataset]
    T --> U{Dataset 是否存在}
    U -->|否| V[创建 Dataset，并将存储属性登记到 DatasetMetadata]
    U -->|是| W[更新 Dataset 当前信息]
    V --> Z[完成数据集登记]
    W --> Z
```

## 13.2 首次生成

``` text
配置 dataset_code
    ↓
Java 前置校验（Dataset 不存在是允许的）
    ↓
Python 写入目标正式物理表
    ↓
Python 返回最终成功
    ↓
Java 创建 Dataset
    ↓
Java 登记存储相关 DatasetMetadata
    ↓
数据集可查询
```

## 13.3 后续更新

``` text
读取已有 Dataset
    ↓
Java 仅检查数据集状态；其他配置沿用现有校验
    ↓
Python 更新同一张正式物理表
    ↓
Python 返回最终成功
    ↓
Java 更新 Dataset 当前信息
    ↓
完成
```

## 13.4 执行失败

``` text
Java 前置校验失败
或
Python 采集/处理/写入失败
    ↓
TaskRun = FAILED
    ↓
不创建或更新 Dataset
```

## 13.5 Java 数据集登记失败

``` text
Python 正式物理表写入成功
    ↓
TaskRun = SUCCESS
    ↓
Java Dataset 登记失败
    ↓
记录登记异常信息，TaskRun 保持 SUCCESS
```

------------------------------------------------------------------------

# 14. 接口设计

必须优先检查现有接口并尽量复用，不要因为本文列出逻辑能力就机械地创建新接口。

## 14.1 数据集查询接口建议

  逻辑接口                     方法   说明
  ---------------------------- ------ -------------------------------
  `/datasets`                  GET    数据集列表
  `/datasets/{id}`             GET    数据集详情
  `/datasets/{id}/metadata`    GET    数据集扩展元数据
  `/datasets/{id}/metadata`    PUT    修改扩展元数据
  `/datasets/{id}/structure`   GET    查询 DataTypeVersion 字段结构
  `/datasets/{id}/source`      GET    查询来源任务和最近成功执行

实际路径应按现有 API 前缀、版本、统一响应体和命名规范调整。

## 14.2 列表查询条件

建议支持：

-   dataset_code；
-   dataset_name；
-   topic_id；
-   data_type_id；
-   dataset_status；
-   task_config_id；
-   最近成功时间范围。

## 14.3 数据集列表响应内容

建议返回：

-   id；
-   dataset_code；
-   dataset_name；
-   topic_name；
-   data_type_name；
-   data_type_version；
-   dataset_status；
-   storage_type、datasource_code、schema_name、table_name（从 DatasetMetadata 读取）；
-   record_count（有可信值时）；
-   last_success_time。

## 14.4 Python 执行结果接口

沿用现有异步执行结果回传机制。不要无故增加一套新的回调协议。

确保结果中包含：

-   taskRunId；
-   datasetCode；
-   executionStatus；
-   writeStatus；
-   databaseName；
-   tableName；
-   processedCount；
-   insertedCount（可选）；
-   updatedCount（可选）；
-   failedCount；
-   dataStartTime（可选）；
-   dataEndTime（可选）；
-   finishTime。

------------------------------------------------------------------------

# 15. 页面设计

本期建议提供三个页面或视图。

## 15.1 数据集目录页

按数据主题组织数据集目录，支持：

-   主题树浏览；
-   数据集名称和编码搜索；
-   数据类型筛选；
-   数据集状态筛选；
-   最近成功更新时间查看。

## 15.2 数据集详情页

建议分区展示：

### 基础信息

-   数据集编码；
-   数据集名称；
-   数据集说明；
-   所属主题；
-   所属数据类型；
-   当前数据类型版本；
-   数据集状态。

### 业务元数据

-   数据粒度；
-   业务主键；
-   数据时间字段；
-   统计周期；
-   更新频率；
-   数据负责人；
-   标签。

### 字段结构

从当前 DataTypeVersion 读取：

-   字段编码；
-   字段名称；
-   字段类型；
-   是否必填；
-   是否唯一键；
-   字段说明；
-   单位（若标准字段定义中存在）。

不得从 DatasetMetadata 另行复制字段结构。

### 存储信息

从 DatasetMetadata 的 `STORAGE` 分类读取：

- `storage_type`：存储类型；
- `datasource_code`：数据源编码；
- `schema_name`：Schema 名称；
- `table_name`：物理表名称。

### 最近执行信息

-   最近成功执行时间；
-   最近执行记录；
-   最近处理数量；
-   最近结果摘要；

## 15.3 元数据维护

允许维护：

-   数据集名称（如果项目业务规则允许）；
-   数据集说明；
-   业务粒度；
-   业务主键描述；
-   数据时间字段；
-   更新频率；
-   负责人；
-   标签；
-   备注。

不允许通过数据集页面直接修改 DataTypeVersion 字段定义和正式物理表结构。

------------------------------------------------------------------------

# 16. 异常处理与一致性边界

## 16.1 异常场景

  --------------------------------------------------------------------------
  场景                                处理
  ----------------------------------- --------------------------------------
  Java 前置校验失败                   不调用 Python

  Python 采集失败                     TaskRun FAILED，不登记 Dataset

  Python 清洗失败                     TaskRun FAILED，不登记 Dataset

  Python 字段映射失败                 TaskRun FAILED，不登记 Dataset

  Python 物理写入失败                 TaskRun FAILED，不登记 Dataset

  Python 写入成功，Java 确认成功      创建或更新 Dataset

  Python 写入成功，Java 数据集登记失败  TaskRun 保持 SUCCESS，记录登记异常

  数据集编码不一致                    记录结果协议异常，不登记到错误数据集

  物理表与执行快照不一致              记录异常，不登记到错误数据集

  数据集已停用                        前置校验阻止执行

  --------------------------------------------------------------------------

## 16.2 本期简化边界

本期明确暂不处理：

-   Python 提交成功但回调完全丢失后的自动发现；
-   写入凭证；
-   Java/Python 两阶段提交；
-   自动登记补偿调度；
-   物理数据回滚补偿；
-   复杂写入恢复机制。

## 16.3 重要风险说明

Java 与 Python 独立运行，且 Python
负责正式物理表写入。因此，物理写入成功与 Java Dataset
登记成功无法通过普通 HTTP 调用形成跨服务原子事务。

本期接受这一边界，但必须做到：

1.  尽可能把所有业务配置错误前置到写入之前；
2.  Python 明确返回最终写入结果；
3.  Java 不把登记失败误判为物理写入失败；
4.  登记操作具备基本幂等性；
5.  异常状态和错误原因可查询。

------------------------------------------------------------------------

# 17. 代码实施任务拆分

GPT-5.6 应根据实际代码库将以下任务映射到真实文件。

## 17.1 数据库层

-   [ ] 检查现有 dataset 表是否存在及其结构；
-   [ ] 补充 dataset_code 唯一约束；
-   [ ] 补充数据集当前结构、来源任务和存储映射关联字段；
-   [ ] 将 dataset_status 定义为从 0 开始的整数枚举，且不建立该字段索引；
-   [ ] 新建或完善 dataset_metadata，并登记 storage_type、datasource_code、schema_name、table_name 四项存储元数据；
-   [ ] 检查 ExecutionConfigSnapshot 是否保存 dataset_code 和目标表；
-   [ ] 编写数据库升级脚本；
-   [ ] 提供必要的回滚脚本或回滚说明。

## 17.2 Java 后端

-   [ ] Dataset Entity/DO；
-   [ ] DatasetMetadata Entity/DO；
-   [ ] Mapper/Repository；
-   [ ] Dataset Service；
-   [ ] 数据集编码查询和唯一性校验；
-   [ ] 写入前置校验服务；
-   [ ] ExecutionConfigSnapshot 字段扩展；
-   [ ] Python 执行上下文扩展；
-   [ ] Python 返回结果 DTO 扩展；
-   [ ] 写入结果确认逻辑；
-   [ ] Dataset 创建逻辑；
-   [ ] Dataset 更新逻辑；
-   [ ] Dataset 列表及详情查询；
-   [ ] 元数据维护接口；
-   [ ] 事务和异常处理；
-   [ ] 日志记录。

## 17.3 Python 任务处理层

- [ ] 检查现有执行上下文和返回结果结构；
- [ ] 仅在现有 Java 执行上下文中补充 `dataset_code`（如尚未包含）；
- [ ] 仅在现有执行结果中补充 `datasetCode`（如尚未包含）；
- [ ] 沿用现有采集执行过程、处理顺序及函数组织；
- [ ] 沿用现有 `conn.execute(text(sql), rows)` 写入方式；
- [ ] 不重构已有采集、清洗、字段映射、写入和结果回传流程。

## 17.4 前端

-   [ ] 数据集目录页；
-   [ ] 数据集详情页；
-   [ ] 元数据查看和维护；
-   [ ] 数据类型字段结构展示；
-   [ ] 存储信息展示；
-   [ ] 最近执行信息展示；

## 17.5 基础语法与编译检查

本次只进行基础检查，不要求编写或执行业务用例测试。

- [ ] 检查本次修改的 Python 文件语法；
- [ ] 执行 Java 项目编译检查；
- [ ] 如本次修改前端，执行对应前端构建或类型检查；
- [ ] 记录未通过的语法或编译问题及原因。

# 18. 开发过程中的禁止事项

GPT-5.6 在实施过程中不得：

1.  擅自新增 DatasetVersion；
2.  擅自新增 DatasetQuality 质量规则引擎；
3.  擅自新增写入凭证表；
4.  擅自引入两阶段提交；
5.  擅自要求 Python 改用其他数据写入框架；
6.  将 Python 的 SQL 执行成功等同于数据库事务提交成功；
7.  将 Dataset 登记失败等同于 TaskRun 执行失败；
8.  将 processedCount 当成数据集当前总记录数；
9.  让 Dataset 重复保存 DataTypeField 字段定义；
10. 让 Python 自行决定 dataset_code、DataTypeVersion 或目标物理表；
11. 让数据集详情页面直接修改物理表结构；
12. 为实现本功能而大规模重构现有采集执行链路；
13. 虚构不存在的项目类、接口、数据库字段或依赖；
14. 未经说明修改现有接口响应结构；
15. 未经确认删除或重命名现有数据库字段。

------------------------------------------------------------------------

# 19. 交付要求与 GPT-5.6 最终输出格式

完成实施后，按以下结构输出总结：

## 19.1 现有代码分析

-   项目技术结构；
-   现有采集执行链路；
-   现有数据库对象；
-   现有 Python 写入和事务处理方式；
-   与设计文档的差异。

## 19.2 设计落地情况

-   数据库对象及字段变更；
-   Java 模块变更；
-   Python 模块变更；
-   接口变更；
-   前端变更；
-   实际执行时序。

## 19.3 文件改造清单

逐项列出新增、修改的文件及用途。

## 19.4 基础语法与编译检查结果

仅列出本次改动涉及范围内的基础检查结果：

- Python 语法检查结果；
- Java 编译检查结果；
- 如涉及前端，前端构建或类型检查结果；
- 未通过项及原因。

本次不要求编写或执行业务用例测试。

## 19.5 遗留问题

明确列出：

-   尚未实现的能力；
-   需要人工确认的业务规则；
-   数据库事务能力限制；
-   当前方案接受的回调丢失风险；
-   后续可能扩展但本期不实施的能力。

------------------------------------------------------------------------

# 附录 A：核心设计决策速查

| 决策项 | 最终结论 |
|---|---|
| 数据集身份 | 稳定 dataset_code |
| 单任务输出 | 一个 TaskConfig 对应一个 Dataset |
| 数据集内容版本 | 不建设 |
| 数据结构来源 | 引用 DataTypeVersion |
| 数据集与物理表 | 一对一 |
| 存储信息 | storage_type、datasource_code、schema_name、table_name 作为 DatasetMetadata 元数据项 |
| 数据集状态 | 从 0 开始的整数枚举，dataset_status 不建立索引 |
| 数据质量校验 | 不单独建设，仅依据任务执行及写入结果 |
| 写入前校验 | Java，仅校验数据集自身属性 |
| 正式物理表写入 | Python |
| Python 写入方式 | 沿用 `conn.execute(text(sql), rows)` |
| Python 上下文和返回结果 | 仅增加必要的 dataset_code/datasetCode，其他字段沿用现有结构 |
| 数据集创建更新 | Java |
| 写入凭证 | 本期不建设 |
| 自动补偿 | 本期不建设 |
| 两阶段提交 | 本期不建设 |
| 暂存表 | 本期不强制建设 |
| 数据集并发校验 | 本期不建设 |
| 历史追溯 | 复用 TaskRun、ExecutionConfigSnapshot、ResultSummary |
| 测试范围 | 仅基础语法和编译检查，不要求业务用例测试 |

# 附录 B：推荐的最终业务流程

``` text
                    Java 应用服务层
                           |
                    用户发起采集任务
                           |
                     创建 TaskRun
                           |
                 读取 TaskConfig / StorageMapping
                           |
                    数据集前置校验
                           |
                   生成执行配置快照
                           |
                     下发 Python
                           |
                    Python 任务处理层
                           |
                       数据采集
                           |
                       数据清洗
                           |
                       字段映射
                           |
                conn.execute(text(sql), rows)
                           |
                    正式物理表写入
                           |
                    返回最终执行结果
                           |
                    Java 应用服务层
                           |
                   确认执行及写入成功
                           |
                    更新 TaskRun
                           |
                  根据 dataset_code 查询
                           |
                    +------+------+
                    |             |
                  不存在          已存在
                    |             |
                 创建 Dataset   更新 Dataset
                    |             |
                    +------+------+
                           |
                    登记完成
                           |
                    数据集可查询
```

**最终设计原则：Java 负责保证写入前配置合法，Python
负责完成正式物理数据写入并返回结果，Java
根据成功结果维护数据集当前状态。**
