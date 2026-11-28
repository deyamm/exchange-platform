package com.exchange.platform.analytics.service.impl;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exchange.platform.analytics.api.enums.MasterDataType;
import com.exchange.platform.analytics.api.enums.SyncBatchStatus;
import com.exchange.platform.analytics.api.enums.TriggerType;
import com.exchange.platform.analytics.api.registry.MdFieldDefinition;
import com.exchange.platform.analytics.api.request.PublishedMasterDataQueryRequest;
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

/** Published master-data query facade backed by MasterDataStore. */
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

    @Override
    @Transactional(readOnly = true)
    public PageResult<Map<String, Object>> queryPublished(PublishedMasterDataQueryRequest request) {
        MasterDataType type = request.typeCode();
        requireLatestPublishedBatch(type);
        MasterDataTypeDefinition definition = registry.require(type);
        Map<String, Object> equalsFilters = normalizeEqualsFilters(request.equalsFilters());
        Map<String, List<Object>> inFilters = normalizeInFilters(request.inFilters());
        validateFields(definition, equalsFilters.keySet());
        validateFields(definition, inFilters.keySet());
        List<String> keywordFields = keywordFields(definition, request.keywordFields());
        validateFields(definition, keywordFields);
        String orderBy = request.orderBy();
        if (!StringUtils.hasText(orderBy)) {
            orderBy = definition.businessKeyFields().get(0);
        }
        validateFields(definition, List.of(orderBy));
        MasterDataQuery query = new MasterDataQuery(equalsFilters, inFilters, request.keywords(), keywordFields,
            orderBy, request.ascending() == null || request.ascending(),
            request.pageNo() == null ? 1 : request.pageNo(), request.pageSize() == null ? 20 : request.pageSize());
        List<Map<String, Object>> records = store.query(definition, query);
        return new PageResult<>(query.pageNo(), query.pageSize(), store.countByQuery(definition, query), records);
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

    private Map<String, Object> normalizeEqualsFilters(Map<String, Object> filters) {
        if (filters == null || filters.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> normalized = new LinkedHashMap<>();
        filters.forEach((field, value) -> {
            if (!StringUtils.hasText(field) || value == null) {
                throw new BusinessException(40001, "等值筛选字段和值不能为空");
            }
            normalized.put(field.trim(), value);
        });
        return normalized;
    }

    private Map<String, List<Object>> normalizeInFilters(Map<String, List<Object>> filters) {
        if (filters == null || filters.isEmpty()) {
            return Map.of();
        }
        Map<String, List<Object>> normalized = new LinkedHashMap<>();
        filters.forEach((field, values) -> {
            if (!StringUtils.hasText(field) || values == null || values.isEmpty() || values.stream().anyMatch(value -> value == null)) {
                throw new BusinessException(40001, "集合筛选字段和值不能为空");
            }
            normalized.put(field.trim(), List.copyOf(values));
        });
        return normalized;
    }

    private List<String> keywordFields(MasterDataTypeDefinition definition, List<String> fields) {
        if (fields == null || fields.isEmpty()) {
            return definition.fields().stream().map(MdFieldDefinition::fieldCode).toList();
        }
        return fields.stream().map(field -> {
            if (!StringUtils.hasText(field)) {
                throw new BusinessException(40001, "关键词筛选字段不能为空");
            }
            return field.trim();
        }).toList();
    }

    private void validateFields(MasterDataTypeDefinition definition, Collection<String> fields) {
        Set<String> definedFields = definition.fields().stream().map(MdFieldDefinition::fieldCode)
            .collect(java.util.stream.Collectors.toUnmodifiableSet());
        fields.forEach(field -> {
            if (!definedFields.contains(field)) {
                throw new BusinessException(40001, "主数据类型 " + definition.typeCode() + " 不包含字段：" + field);
            }
        });
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
