package com.exchange.platform.analytics.service.impl;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exchange.platform.analytics.api.enums.IndexCategory;
import com.exchange.platform.analytics.api.enums.MasterDataType;
import com.exchange.platform.analytics.api.enums.SyncBatchStatus;
import com.exchange.platform.analytics.api.enums.StockMarketType;
import com.exchange.platform.analytics.api.enums.TriggerType;
import com.exchange.platform.analytics.api.response.PublishedMasterDataTypeDetailView;
import com.exchange.platform.analytics.api.response.PublishedMasterDataTypeView;
import com.exchange.platform.analytics.api.response.SyncBatchView;
import com.exchange.platform.analytics.api.registry.MasterDataTypeDefinition;
import com.exchange.platform.analytics.entity.masterdata.MdSyncBatch;
import com.exchange.platform.analytics.mapper.masterdata.MdSyncBatchMapper;
import com.exchange.platform.analytics.service.MasterDataService;
import com.exchange.platform.analytics.service.masterdata.MasterDataPublishApplicationService;
import com.exchange.platform.analytics.service.masterdata.MasterDataQuery;
import com.exchange.platform.analytics.service.masterdata.MasterDataTypeRegistry;
import com.exchange.platform.analytics.service.support.CollectionDatasetMetadataClient;
import com.exchange.platform.analytics.utils.masterdata.MasterDataStore;
import com.exchange.platform.collection.api.response.execution.MetricDatasetConfigView;
import com.exchange.platform.common.core.enums.MarketAssetType;
import com.exchange.platform.common.core.exception.BusinessException;
import com.exchange.platform.common.core.page.PageResult;

/** Type-scoped query facade backed by MasterDataStore. */
@Service
public class MasterDataServiceImpl implements MasterDataService {

    private static final int SCOPE_QUERY_LIMIT = 2000;

    private final MdSyncBatchMapper batchMapper;
    private final MasterDataPublishApplicationService publishApplicationService;
    private final MasterDataTypeRegistry registry;
    private final MasterDataStore store;
    private final CollectionDatasetMetadataClient datasetMetadataClient;

    public MasterDataServiceImpl(MdSyncBatchMapper batchMapper,
            MasterDataPublishApplicationService publishApplicationService, MasterDataTypeRegistry registry,
            MasterDataStore store, CollectionDatasetMetadataClient datasetMetadataClient) {
        this.batchMapper = batchMapper;
        this.publishApplicationService = publishApplicationService;
        this.registry = registry;
        this.store = store;
        this.datasetMetadataClient = datasetMetadataClient;
    }

    /**
     * 
     */
    @Override
    @Transactional(readOnly = true)
    public List<PublishedMasterDataTypeView> publishedTypes(String keyword) {
        // 查询已发布的数据集及最新的绑定信息
        Map<MasterDataType, MdSyncBatch> latestBatches = latestPublishedBatches();
        // 查询来源数据集的信息
        Map<String, MetricDatasetConfigView> datasets = datasetMetadataClient
            .getDatasetsForMetricConfiguration(latestBatches.values().stream()
                .map(MdSyncBatch::getDatasetCode).filter(StringUtils::hasText).toList());
        
        // 根据关键字过滤已发布的主数据类型视图
        return latestBatches.entrySet().stream()
            .map(entry -> toPublishedTypeView(entry.getKey(), entry.getValue(), datasets.get(entry.getValue().getDatasetCode())))
            .filter(view -> matchesPublishedTypeKeyword(view, keyword))
            .toList();
    }

    /**
     * 查询已发布主数据类型的详细信息。
     */
    @Override
    @Transactional(readOnly = true)
    public PublishedMasterDataTypeDetailView publishedTypeDetail(MasterDataType type) {
        MdSyncBatch batch = requireLatestPublishedBatch(type);
        MasterDataTypeDefinition definition = registry.require(type);
        MetricDatasetConfigView dataset = datasetMetadataClient
            .getDatasetsForMetricConfiguration(List.of(batch.getDatasetCode())).get(batch.getDatasetCode());
        return new PublishedMasterDataTypeDetailView(
            type, 
            definition.displayName(), 
            definition.domain(),
            definition.category(),
            batch.getDatasetCode(), 
            dataset == null ? null : dataset.getDatasetName(),
            store.countAll(definition), 
            batch.getPublishedAt(), 
            definition.fields()
        );
    }

    /**
     * 通过主数据统一读取 Store 分页查询指定 type 的具体主数据
     */
    @Override
    @Transactional(readOnly = true)
    public PageResult<Map<String, Object>> publishedRows(MasterDataType type, int pageNo, int pageSize) {
        requireLatestPublishedBatch(type);
        MasterDataTypeDefinition definition = registry.require(type);
        MasterDataQuery query = new MasterDataQuery(Map.of(), Map.of(), null, List.of(),
            definition.businessKeyFields().get(0), true, pageNo, pageSize);

        List<Map<String, Object>> records = store.query(definition, query);
        return new PageResult<>(query.pageNo(), query.pageSize(), store.countByQuery(definition, query), records);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> search(String keyword, MarketAssetType type, int limit) {
        if (!StringUtils.hasText(keyword))
            return List.of();
        MasterDataTypeDefinition definition = definition(type);
        MasterDataQuery query = new MasterDataQuery(Map.of(), Map.of(), keyword, List.of("assetCode", "assetName"),
                "assetCode", true, 1, Math.min(Math.max(limit, 1), 200));
        return store.query(definition, query);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Map<String, Object>> list(String keyword, MarketAssetType marketAssetType,
            StockMarketType marketType, String exchangeCode, String publisherCode, IndexCategory indexCategory,
            String schemeCode, Integer level, int pageNo, int pageSize) {
        MasterDataTypeDefinition definition = definition(marketAssetType);
        Map<String, Object> equals = new LinkedHashMap<>();
        if (marketType != null && StringUtils.hasText(marketType.getValue()))
            equals.put("marketType", marketType.getValue());
        if (exchangeCode != null && StringUtils.hasText(exchangeCode))
            equals.put("exchangeCode", exchangeCode);
        if (publisherCode != null && StringUtils.hasText(publisherCode))
            equals.put("publisherCode", publisherCode);
        if (indexCategory != null && StringUtils.hasText(indexCategory.getValue()))
            equals.put("indexCategory", indexCategory.getValue());
        if (schemeCode != null && StringUtils.hasText(schemeCode))
            equals.put("schemeCode", schemeCode);
        if (level != null)
            equals.put("level", level);
        MasterDataQuery query = new MasterDataQuery(equals, Map.of(), keyword, List.of(),
                "assetCode", true, pageNo, pageSize);
        List<Map<String, Object>> rows = store.query(definition, query);
        long total = store.countByQuery(definition, query);
        return new PageResult<>(pageNo, pageSize, total, rows);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> detail(MarketAssetType type, String assetCode) {
        if (type == null || !StringUtils.hasText(assetCode))
            throw new BusinessException(40004, "业务主键不能为空");
        Map<String, Object> row = store.findByBusinessKey(definition(type), Map.of("assetCode", assetCode.trim()));
        if (row == null)
            throw new BusinessException(40004, "主数据不存在：" + type + "/" + assetCode);
        return row;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> findByCodes(MarketAssetType type, Collection<String> assetCodes) {
        if (type == null)
            throw new BusinessException(40001, "标的类型不能为空");
        if (assetCodes == null || assetCodes.isEmpty())
            return List.of();

        return queryByInFilters(type, Map.of("assetCode", assetCodes));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> resolveScope(MarketAssetType type, String scopeType, List<String> scopeValues) {
        if (type == null)
            throw new BusinessException(40001, "标的类型不能为空");

        String scope = normalizeScope(scopeType);
        Collection<String> values = scopeValues == null ? List.of() : scopeValues;

        return switch (scope) {
        case "ALL" -> queryByInFilters(type, Map.of());
        case "ASSET_CODE" -> values.isEmpty() ? List.of() : queryByInFilters(type, Map.of("assetCode", values));
        case "INDEX_TYPE" -> {
            requireIndexType(type, scope);
            yield values.isEmpty() ? List.of() : queryByInFilters(type, Map.of("indexCategory", values));
        }
        case "PUBLISHER", "MARKET" -> {
            requireIndexType(type, scope);
            yield values.isEmpty() ? List.of() : queryByInFilters(type, Map.of("publisherCode", values));
        }
        default -> throw new BusinessException(40001, "主数据不支持该范围类型：" + scopeType);
        };
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<SyncBatchView> batches(String masterDataType, String datasetCode, String status, Long bindingId, String triggerType, int pageNo, int pageSize) {
        Page<MdSyncBatch> page = batchMapper.selectPage(
            new Page<>(Math.max(pageNo, 1), Math.min(Math.max(pageSize, 1), 200)),
            new LambdaQueryWrapper<MdSyncBatch>()
                .eq(StringUtils.hasText(datasetCode), MdSyncBatch::getDatasetCode, datasetCode)
                .eq(StringUtils.hasText(status), MdSyncBatch::getStatus, status)
                .eq(StringUtils.hasText(masterDataType), MdSyncBatch::getMasterDataType, masterDataType)
                .eq(bindingId != null, MdSyncBatch::getBindingId, bindingId)
                .eq(StringUtils.hasText(triggerType), MdSyncBatch::getTriggerType, triggerType)
                .orderByDesc(MdSyncBatch::getStartedAt));
        return new PageResult<>((int) page.getCurrent(), (int) page.getSize(), page.getTotal(),
            page.getRecords().stream().map(publishApplicationService::toView).toList());
    }

    @Override
    public SyncBatchView publish(String masterDataType, String sourceCode, String taskRunId, String datasetCode, String syncType) {
        return publishApplicationService.publish(MasterDataType.valueOf(masterDataType), sourceCode, taskRunId,
            datasetCode, syncType);
    }

    @Override
    public SyncBatchView republish(Long batchId) {
        return publishApplicationService.republish(batchId);
    }

    @Override
    public SyncBatchView publishBinding(Long bindingId, String taskRunId, String syncType, TriggerType triggerType) {
        return publishApplicationService.publishBinding(bindingId, taskRunId, syncType, triggerType);
    }

    private List<Map<String, Object>> queryByInFilters(MarketAssetType type, Map<String, ? extends Collection<?>> filters) {
        MasterDataQuery query = new MasterDataQuery(Map.of(), filters, null, List.of(), "assetCode", true, 1,
                SCOPE_QUERY_LIMIT);
        return store.query(definition(type), query);
    }

    private void requireIndexType(MarketAssetType type, String scopeType) {
        if (type != MarketAssetType.INDEX)
            throw new BusinessException(40001, "范围类型 " + scopeType + " 仅支持指数");
    }

    private String normalizeScope(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase() : "";
    }

    private MasterDataTypeDefinition definition(MarketAssetType type) {
        return registry.require(
            type == MarketAssetType.INDEX ? MasterDataType.MARKET_ASSET_INDEX : MasterDataType.MARKET_ASSET_STOCK);
    }

    /**
     * 查询所有已发布的主数据类型及其最新发布批次，按主数据类型顺序返回。
     * @return
     */
    private Map<MasterDataType, MdSyncBatch> latestPublishedBatches() {
        Map<MasterDataType, MdSyncBatch> result = new LinkedHashMap<>();
        batchMapper.selectList(new LambdaQueryWrapper<MdSyncBatch>()
            .eq(MdSyncBatch::getStatus, SyncBatchStatus.PUBLISHED)
            .orderByDesc(MdSyncBatch::getPublishedAt)
            .orderByDesc(MdSyncBatch::getId))
            .forEach(batch -> {
                if (batch.getMasterDataType() != null && registry.get(batch.getMasterDataType()) != null) {
                    result.putIfAbsent(batch.getMasterDataType(), batch);
                }
            });
        return result;
    }

    private MdSyncBatch requireLatestPublishedBatch(MasterDataType type) {
        if (type == null || registry.get(type) == null) {
            throw new BusinessException(40004, "主数据类型不存在：" + type);
        }
        MdSyncBatch batch = latestPublishedBatches().get(type);
        if (batch == null) {
            throw new BusinessException(40004, "主数据尚未发布成功：" + type);
        }
        return batch;
    }

    private PublishedMasterDataTypeView toPublishedTypeView(MasterDataType type, MdSyncBatch batch,
            MetricDatasetConfigView dataset) {
        MasterDataTypeDefinition definition = registry.require(type);
        return new PublishedMasterDataTypeView(type, definition.displayName(), batch.getDatasetCode(),
            dataset == null ? null : dataset.getDatasetName(), store.countAll(definition), batch.getPublishedAt());
    }

    /**
     * 根据关键字匹配已发布主数据类型视图，匹配字段包括 typeCode、displayName、datasetCode、datasetName。
     * @param view
     * @param keyword
     * @return
     */
    private boolean matchesPublishedTypeKeyword(PublishedMasterDataTypeView view, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String value = keyword.trim().toLowerCase();
        return containsIgnoreCase(view.typeCode().name(), value)
                || containsIgnoreCase(view.displayName(), value)
                || containsIgnoreCase(view.datasetCode(), value)
                || containsIgnoreCase(view.datasetName(), value);
    }

    private boolean containsIgnoreCase(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword);
    }
}
