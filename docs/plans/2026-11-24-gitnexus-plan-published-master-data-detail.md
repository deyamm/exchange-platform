# GitNexus Engineering Plan

> Task: 通用主数据已发布类型列表、详情页及分页主数据读取
> Evidence verified at commit `102ba84ca43fe4e416784715c18cfbcb73370c36`; GitNexus index is 1 commit behind HEAD, so graph claims are secondary and current source is authoritative.
> Evidence provenance schema 2; global dirty digest `1e3424d1608d34a45a860597aba0c5b7dee3840f03dc78f41aea2fd97c143649`; cited-path manifest 16 entries; generated plan path excluded.

## Objective (§1)

将主数据页面改为由 `MasterDataTypeRegistry` 驱动：进入页面展示已成功发布的主数据类型并支持关键字搜索；点击详情进入独立详情页，展示类型、来源 Dataset 名称/编码、Registry 字段结构、总条数及分页记录。读取必须继续经过 `MasterDataStore`（主数据）和 `UnifiedDatasetRowReader/DatasetRowReader`（Dataset/Python）。

## Current Behaviour (§2–3)

- [verified] `MasterDataController` 目前提供 Registry、SourceBinding、资产列表/搜索/单资产详情及同步批次接口，但没有“已发布类型列表”或通用类型详情/分页记录接口。
- [verified] `MasterDataServiceImpl.list/detail/search` 将请求映射为 STOCK/INDEX 两张 Registry 表，并通过 `MasterDataStore.query/count/findByBusinessKey` 读取。
- [verified] 发布链路为 `SourceBindingResolver → PublishPlan → MasterDataPublishExecutor → DatasetRowReader.readAll → MasterDataStore.upsert`；Dataset 物理读取由 `UnifiedDatasetRowReader` 转发到 Python `dataset-rows/query`。
- [verified] `MasterDataPage.vue` 是股票/指数专用筛选列表并打开 `MasterDataDetailDrawer`；路由没有主数据类型详情页。

## Findings (§4–5)

- [graph] GitNexus `query` 定位到 `MasterDataController`、`UnifiedDatasetRowReader`、`SourceBindingPage.publish` 和前端 `masterDataApi`；索引滞后 1 commit，未将其作为完整调用者证明。
- [verified] Registry 当前定义 `MARKET_ASSET_STOCK/INDEX`、字段、业务主键和物理表映射；新接口必须按 `typeCode` 查定义，不能在 Controller 复制字段列表。
- [verified] `MdSyncBatch` 持有 `masterDataType/bindingId/datasetCode/status/totalCount/publishedAt`，可用于判定“发布成功”的来源与批次；`MdSourceBinding` 提供 Dataset 编码，Dataset 名称/字段快照需通过 `CollectionDatasetMetadataClient` 获取。
- [verified] `MasterDataStore.query` 已支持安全的 Registry 字段投影、分页、关键字和 count；建议扩展为类型维度的复用，而非新增物理表访问。
- [verified] PDG 查询返回无可用层/无法锚定；不作语句级边声明，依源码和实际测试验证。

## Proposed Changes (§6)

1. 后端 API/响应：新增通用“已发布主数据类型摘要”和“类型详情”响应模型，包含 `MasterDataType`、displayName、来源 Dataset code/name、字段定义、总条数、最近成功批次/发布时间；新增按 typeCode 的分页记录响应。成功判定限定 `md_sync_batch.status=PUBLISHED`，无成功批次不展示。
2. `MasterDataService`/`MasterDataServiceImpl`：新增 listPublishedTypes、publishedTypeDetail、pagePublishedRows（命名按现有风格）；使用 Registry 获取定义，查询最近成功批次及 Binding，批量读取 Dataset 元数据；记录分页继续调用 `MasterDataStore.query/countByQuery`，不直接拼接未受信表名。
3. `MasterDataController`：在 `/api/v1/analytics/master-data` 下增加 published-types 列表/详情/rows GET 接口，校验未知 typeCode、分页上下限和无发布批次错误；保留现有资产与来源接口兼容性。
4. 前端 API/types：在 `masterDataApi.ts` 和 `types/masterdata.ts` 增加通用类型摘要、详情、分页行模型及请求方法；保留 SourceBinding/SyncBatch API。
5. 前端页面：重写 `MasterDataPage.vue` 为 Registry 类型列表+关键字搜索+“详情”按钮；新增 `MasterDataTypeDetailPage.vue`（基础信息、字段结构、Dataset 来源、总条数、分页动态列/行表格）；详情路由加入 `route.ts`，从列表携带 typeCode。
6. 细节与兼容：动态字段列以后端 `fields` 为准；空发布、Dataset 元数据不可用、读取失败均显示可理解错误；不修改既有发布执行器、Python Dataset 接口或数据库表结构，除非验证发现成功批次查询缺少必要索引。

## Implementation Sequence (§7)

1. 先确认现有未提交主数据改动的编译基线和 API 响应包装；补充/复用 response record、Mapper 查询投影（风险：工作区已有同文件改动，必须增量编辑）。
2. 实现 Service 查询编排和 Controller 三组接口；用 Registry、`MdSyncBatchMapper`、`MdSourceBindingMapper`、`CollectionDatasetMetadataClient` 组装元数据，用 Store 读取记录。
3. 增加前端类型/API 与详情路由；将列表搜索限制为后端类型摘要，不在浏览器合并数据。
4. 实现列表页和详情页的加载、分页、动态字段列、空态/错误态。
5. 运行后端编译、前端 TypeScript/Vite 构建；执行 `gitnexus-docker detect_changes`（索引可用时）并检查未预期影响。

## Test Strategy (§8)

- 后端单元/集成覆盖：无 PUBLISHED 批次不返回；多个批次取最新成功批次；未知类型、非法分页、Dataset 元数据缺失；Store count 与 rows 分页一致；动态 Registry 字段完整返回。
- API 边界覆盖：列表关键字只过滤类型 code/displayName；详情 rows 只接受 Registry 字段/分页参数；Python Dataset 读取链路不被 UI 直接调用。
- 前端构建覆盖：类型/API 导入、路由跳转、动态列渲染、分页换页和空态；手工验证成功/失败响应提示。
- 验证命令：`mvn -pl backend/data-analytics/data-analytics-server -am test`、`npm run build --prefix frontend`；若仓库已有更高层 Maven 聚合脚本，优先使用其脚本。

## Implementation Context (§11)

```yaml
implementation_context:
  task_summary: "Registry 驱动的已发布主数据类型列表、详情元数据与分页记录读取"
  acceptance_criteria:
    - "只展示至少存在一个 PUBLISHED 批次的 Registry 类型"
    - "详情包含 MasterDataType、Dataset 名称/编码、字段结构、总条数和分页记录"
    - "主数据读取经过 MasterDataStore，Dataset 读取沿用 UnifiedDatasetRowReader/DatasetRowReader"
    - "前端列表搜索、详情跳转、动态字段表格和分页可用"
    - "完成 Java 编译/测试及前端构建"
  evidence_provenance:
    schema_version: 2
    head_commit: "102ba84ca43fe4e416784715c18cfbcb73370c36"
    generated_plan_path: "docs/plans/2026-11-24-gitnexus-plan-published-master-data-detail.md"
    global_dirty_digest: {algorithm: sha256, canonicalization: "gitnexus-evidence-provenance-v2 NUL-framed UTF-8 records", value: "1e3424d1608d34a45a860597aba0c5b7dee3840f03dc78f41aea2fd97c143649"}
    cited_path_manifest: [{"path":"backend/data-analytics/data-analytics-server/pom.xml","object_kind":{"head":"regular","index":"regular","worktree":"regular","untracked":"absent"},"state":"unstaged","rename_from":null,"rename_to":null,"head_digest":"sha256:e45cfa6db7be60d0c514c3a4be36a08a7f814cd13fe5b2594adab5f219abdc10","index_digest":"sha256:e45cfa6db7be60d0c514c3a4be36a08a7f814cd13fe5b2594adab5f219abdc10","worktree_digest":"sha256:e61bfa5248d02119d657cc7903414eb1eeaf5b3d7a70dd9ea3e90d51e58f97df","untracked_digest":"absent"},{"path":"backend/data-analytics/data-analytics-server/src/main/java/com/exchange/platform/analytics/controller/MasterDataController.java","object_kind":{"head":"regular","index":"regular","worktree":"regular","untracked":"absent"},"state":"unstaged","rename_from":null,"rename_to":null,"head_digest":"sha256:91f475e76596f7208dc3378e8d18202d9febe96a38302e31cfba9bc34a4d2e31","index_digest":"sha256:91f475e76596f7208dc3378e8d18202d9febe96a38302e31cfba9bc34a4d2e31","worktree_digest":"sha256:1b2a6a6ab287275e1467d0c5d510cb6f081de8bfc5625693ac4aa6a68db8ced6","untracked_digest":"absent"},{"path":"backend/data-analytics/data-analytics-server/src/main/java/com/exchange/platform/analytics/service/MasterDataService.java","object_kind":{"head":"regular","index":"regular","worktree":"regular","untracked":"absent"},"state":"clean","rename_from":null,"rename_to":null,"head_digest":"sha256:b9269cac609d86c8a0fd67358a8227f162007cf669d0ce8315ed79844de286ce","index_digest":"sha256:b9269cac609d86c8a0fd67358a8227f162007cf669d0ce8315ed79844de286ce","worktree_digest":"sha256:b9269cac609d86c8a0fd67358a8227f162007cf669d0ce8315ed79844de286ce","untracked_digest":"absent"},{"path":"backend/data-analytics/data-analytics-server/src/main/java/com/exchange/platform/analytics/service/impl/MasterDataServiceImpl.java","object_kind":{"head":"regular","index":"regular","worktree":"regular","untracked":"absent"},"state":"unstaged","rename_from":null,"rename_to":null,"head_digest":"sha256:66dde0c53a0af10ce1b1cb1297784b0d76fe654ecfac2991ba776c46c8647535","index_digest":"sha256:66dde0c53a0af10ce1b1cb1297784b0d76fe654ecfac2991ba776c46c8647535","worktree_digest":"sha256:4a540be335c54d65ca00a52173adde94f7934652634a843bbe792e83836bb830","untracked_digest":"absent"},{"path":"backend/data-analytics/data-analytics-server/src/main/java/com/exchange/platform/analytics/service/masterdata/MasterDataPublishExecutor.java","object_kind":{"head":"regular","index":"regular","worktree":"regular","untracked":"absent"},"state":"unstaged","rename_from":null,"rename_to":null,"head_digest":"sha256:c0b3c89fb3a67163960dd9cbd54b856ae07e9d4835d59aae1397233f4cb824a9","index_digest":"sha256:c0b3c89fb3a67163960dd9cbd54b856ae07e9d4835d59aae1397233f4cb824a9","worktree_digest":"sha256:e135d77aa1d777b0dcaf727070fd2c3bf652d6067ffc89547fbf393c056832f8","untracked_digest":"absent"},{"path":"backend/data-analytics/data-analytics-server/src/main/java/com/exchange/platform/analytics/service/masterdata/MasterDataTypeRegistry.java","object_kind":{"head":"regular","index":"regular","worktree":"regular","untracked":"absent"},"state":"unstaged","rename_from":null,"rename_to":null,"head_digest":"sha256:1a7ed0daafe59fee6f3043912a1fa51b4aecc7b58da9f97c3fdec6a6cc6809c1","index_digest":"sha256:1a7ed0daafe59fee6f3043912a1fa51b4aecc7b58da9f97c3fdec6a6cc6809c1","worktree_digest":"sha256:d9ffdead6c87475cf83ec569bd881a8b3986c5fff6575a2f56782603ffcfd596","untracked_digest":"absent"},{"path":"backend/data-analytics/data-analytics-server/src/main/java/com/exchange/platform/analytics/service/masterdata/SourceBindingResolver.java","object_kind":{"head":"regular","index":"regular","worktree":"regular","untracked":"absent"},"state":"unstaged","rename_from":null,"rename_to":null,"head_digest":"sha256:f0708567c54580fea1e47ede5393d1aa78cdeba0b08b56c687f5c807f355025a","index_digest":"sha256:f0708567c54580fea1e47ede5393d1aa78cdeba0b08b56c687f5c807f355025a","worktree_digest":"sha256:0c01e16c095a43f9ba0812d78c14ea479fadf0d6d2e52e3d8d5d7e23bb891136","untracked_digest":"absent"},{"path":"backend/data-analytics/data-analytics-server/src/main/java/com/exchange/platform/analytics/service/masterdata/UnifiedDatasetRowReader.java","object_kind":{"head":"regular","index":"regular","worktree":"regular","untracked":"absent"},"state":"unstaged","rename_from":null,"rename_to":null,"head_digest":"sha256:e3dcd3ca1f7211c20ed070ed248619a438a831c8c35a9123d20a81549b4c1311","index_digest":"sha256:e3dcd3ca1f7211c20ed070ed248619a438a831c8c35a9123d20a81549b4c1311","worktree_digest":"sha256:d0aa55baf5e0aa679d0db808894de1d17c963016200543c3dca17d531a46a1ad","untracked_digest":"absent"},{"path":"backend/data-analytics/data-analytics-server/src/main/java/com/exchange/platform/analytics/service/support/CollectionDatasetMetadataClient.java","object_kind":{"head":"regular","index":"regular","worktree":"regular","untracked":"absent"},"state":"unstaged","rename_from":null,"rename_to":null,"head_digest":"sha256:8095c9994ed00940405b63b7cf8aa0ac7c6fb6eac221decf97a3d42df0a6de1a","index_digest":"sha256:8095c9994ed00940405b63b7cf8aa0ac7c6fb6eac221decf97a3d42df0a6de1a","worktree_digest":"sha256:36ce3683436c01afbf077c12902dd648b342e88d8fe9d9e17a09b742a7fbf10c","untracked_digest":"absent"},{"path":"backend/data-analytics/data-analytics-server/src/main/java/com/exchange/platform/analytics/utils/masterdata/MasterDataStore.java","object_kind":{"head":"regular","index":"regular","worktree":"regular","untracked":"absent"},"state":"unstaged","rename_from":null,"rename_to":null,"head_digest":"sha256:44232eec4108b3038fff46bc0a6e5f5ed088e400fa00a97df913fb716c54f70a","index_digest":"sha256:44232eec4108b3038fff46bc0a6e5f5ed088e400fa00a97df913fb716c54f70a","worktree_digest":"sha256:de1bd3906a9363ad588c81f2a4b3cc48e697dfb8490aae8e560cb16d7ce01289","untracked_digest":"absent"},{"path":"frontend/package.json","object_kind":{"head":"regular","index":"regular","worktree":"regular","untracked":"absent"},"state":"clean","rename_from":null,"rename_to":null,"head_digest":"sha256:99dd2dbab04a519711b8ad738ea5c1a1348a81d9d95018eef2cd4fc4056917df","index_digest":"sha256:99dd2dbab04a519711b8ad738ea5c1a1348a81d9d95018eef2cd4fc4056917df","worktree_digest":"sha256:99dd2dbab04a519711b8ad738ea5c1a1348a81d9d95018eef2cd4fc4056917df","untracked_digest":"absent"},{"path":"frontend/src/api/masterdata/masterDataApi.ts","object_kind":{"head":"regular","index":"regular","worktree":"regular","untracked":"absent"},"state":"clean","rename_from":null,"rename_to":null,"head_digest":"sha256:f5d3e69255ca42638e032d82b6d8cc882db7ad98783bf7db7ea79591d98767b1","index_digest":"sha256:f5d3e69255ca42638e032d82b6d8cc882db7ad98783bf7db7ea79591d98767b1","worktree_digest":"sha256:f5d3e69255ca42638e032d82b6d8cc882db7ad98783bf7db7ea79591d98767b1","untracked_digest":"absent"},{"path":"frontend/src/router/route.ts","object_kind":{"head":"regular","index":"regular","worktree":"regular","untracked":"absent"},"state":"clean","rename_from":null,"rename_to":null,"head_digest":"sha256:2bf444d13e2ceae29b5d03a35ec5569630423618ba69595989fbdbba4ea59e3d","index_digest":"sha256:2bf444d13e2ceae29b5d03a35ec5569630423618ba69595989fbdbba4ea59e3d","worktree_digest":"sha256:2bf444d13e2ceae29b5d03a35ec5569630423618ba69595989fbdbba4ea59e3d","untracked_digest":"absent"},{"path":"frontend/src/types/masterdata.ts","object_kind":{"head":"regular","index":"regular","worktree":"regular","untracked":"absent"},"state":"clean","rename_from":null,"rename_to":null,"head_digest":"sha256:64b448ed7e668706393aceb06cef79d57db246256cbf67a53e56e2468ae8e1c0","index_digest":"sha256:64b448ed7e668706393aceb06cef79d57db246256cbf67a53e56e2468ae8e1c0","worktree_digest":"sha256:64b448ed7e668706393aceb06cef79d57db246256cbf67a53e56e2468ae8e1c0","untracked_digest":"absent"},{"path":"frontend/src/views/masterdata/MasterDataPage.vue","object_kind":{"head":"regular","index":"regular","worktree":"regular","untracked":"absent"},"state":"unstaged","rename_from":null,"rename_to":null,"head_digest":"sha256:57f0e226674ccda0ea051898a17c000b312a6c87ec9f83e00d7eb008305b85c9","index_digest":"sha256:57f0e226674ccda0ea051898a17c000b312a6c87ec9f83e00d7eb008305b85c9","worktree_digest":"sha256:56a4196fa97f49fb25f4ec562a55e5f22c95955a91bafd8b8e05e7476638e51c","untracked_digest":"absent"}]
  primary_symbols:
    - {symbol: "MasterDataController", file: "backend/data-analytics/data-analytics-server/src/main/java/com/exchange/platform/analytics/controller/MasterDataController.java", lines: "25-183", role: "HTTP boundary"}
    - {symbol: "MasterDataServiceImpl", file: "backend/data-analytics/data-analytics-server/src/main/java/com/exchange/platform/analytics/service/impl/MasterDataServiceImpl.java", lines: "43-416", role: "query orchestration"}
    - {symbol: "MasterDataTypeRegistry", file: "backend/data-analytics/data-analytics-server/src/main/java/com/exchange/platform/analytics/service/masterdata/MasterDataTypeRegistry.java", lines: "22-107", role: "type/field/storage definitions"}
    - {symbol: "MasterDataStore", file: "backend/data-analytics/data-analytics-server/src/main/java/com/exchange/platform/analytics/utils/masterdata/MasterDataStore.java", lines: "18-205", role: "master data persistence/query"}
    - {symbol: "UnifiedDatasetRowReader", file: "backend/data-analytics/data-analytics-server/src/main/java/com/exchange/platform/analytics/service/masterdata/UnifiedDatasetRowReader.java", lines: "23-83", role: "Dataset read adapter"}
  related_symbols:
    - {symbol: "MasterDataPublishExecutor", relationship: "CALLS DatasetRowReader and MasterDataStore", relevance: "publication semantics"}
    - {symbol: "SourceBindingResolver", relationship: "resolves binding metadata", relevance: "source mapping/readiness"}
    - {symbol: "CollectionDatasetMetadataClient", relationship: "reads Dataset metadata", relevance: "Dataset name/fields"}
  execution_path: ["PUBLISHED batch → binding/dataset metadata → Registry definition → MasterDataStore count/query → API → Vue list/detail"]
  files_to_modify:
    - {file: "backend/data-analytics/data-analytics-api/src/main/java/com/exchange/platform/analytics/api/response/", symbols: ["new published type/row response records"], intended_change: "API DTOs"}
    - {file: "backend/data-analytics/data-analytics-server/src/main/java/com/exchange/platform/analytics/service/MasterDataService.java", symbols: ["new query methods"], intended_change: "service contract"}
    - {file: "backend/data-analytics/data-analytics-server/src/main/java/com/exchange/platform/analytics/service/impl/MasterDataServiceImpl.java", symbols: ["new query methods"], intended_change: "query orchestration"}
    - {file: "backend/data-analytics/data-analytics-server/src/main/java/com/exchange/platform/analytics/controller/MasterDataController.java", symbols: ["new GET endpoints"], intended_change: "HTTP API"}
    - {file: "frontend/src/api/masterdata/masterDataApi.ts", symbols: ["new API functions"], intended_change: "client calls"}
    - {file: "frontend/src/types/masterdata.ts", symbols: ["new view/query types"], intended_change: "client models"}
    - {file: "frontend/src/views/masterdata/MasterDataPage.vue", symbols: ["page UI"], intended_change: "published type list/search"}
    - {file: "frontend/src/views/masterdata/MasterDataTypeDetailPage.vue", symbols: ["new detail page"], intended_change: "metadata and paginated rows"}
    - {file: "frontend/src/router/route.ts", symbols: ["masterData detail route"], intended_change: "navigation"}
  tests:
    - {file: "backend/data-analytics/data-analytics-server/src/test", scenarios: ["published batch selection, unknown type, pagination, Store count/query"]}
    - {file: "frontend/package.json", scenarios: ["build validates Vue/TypeScript integration"]}
  verification_commands: ["mvn -pl backend/data-analytics/data-analytics-server -am test", "npm run build --prefix frontend", "gitnexus-docker detect_changes"]
  risks: ["existing dirty master-data files may conflict", "Dataset metadata call can fail independently of Store", "dynamic fields require stable Registry/API ordering", "large rows require bounded page size"]
  assumptions: ["PUBLISHED is the authoritative successful status", "latest successful batch is source-of-record for Dataset metadata", "Dataset config view can expose datasetName/fields; if not, add a narrow metadata client method"]
  open_questions: ["Whether product wants a hidden detail route or a visible submenu", "Whether source dataset code/name should come from binding or batch when binding was later edited"]
  avoid: ["Do not access physical Dataset tables from Java UI APIs", "Do not duplicate Registry field/storage definitions", "Do not reset or overwrite existing worktree changes", "Do not change Python dataset-rows contract without evidence"]
```

## Assumptions and Open Questions (§12)

- [assumed] “发布成功”定义为 `MdSyncBatch.status = PUBLISHED`；若业务要求仅显示当前启用来源，需要在执行前确认。
- [assumed] 来源 Dataset 名称可从现有 Dataset 配置客户端取得；若当前客户端仅返回字段而无名称，应扩展 DTO/调用，不改主数据存储。
- [open] 详情页是否必须作为可收藏的独立菜单项，还是仅隐藏路由；计划默认隐藏路由。
- [open] 来源绑定被编辑后，历史 PUBLISHED 批次详情应展示批次时的 Dataset 还是当前绑定 Dataset；计划默认展示最近成功批次关联值并在实现中固定查询语义。
- 工作区已有大量未提交主数据改动，执行前必须先确认这些改动属于当前需求并保留。

## Definition of Done (§13)

列表只呈现 Registry 中有成功发布批次的类型且可搜索；详情页基础信息、字段结构、Dataset 来源、总数、分页行数据正确；所有读路径遵守 Store/统一 Dataset reader；后端测试/编译和前端构建通过；detect_changes 无未解释的高风险影响；未覆盖既有未提交改动。
