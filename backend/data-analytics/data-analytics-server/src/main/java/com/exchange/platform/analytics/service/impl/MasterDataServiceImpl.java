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
import com.exchange.platform.analytics.api.enums.StockMarketType;
import com.exchange.platform.analytics.api.enums.TriggerType;
import com.exchange.platform.analytics.api.response.SyncBatchView;
import com.exchange.platform.analytics.api.registry.MasterDataTypeDefinition;
import com.exchange.platform.analytics.entity.masterdata.MdSyncBatch;
import com.exchange.platform.analytics.mapper.masterdata.MdSyncBatchMapper;
import com.exchange.platform.analytics.service.MasterDataService;
import com.exchange.platform.analytics.service.masterdata.MasterDataPublishApplicationService;
import com.exchange.platform.analytics.service.masterdata.MasterDataQuery;
import com.exchange.platform.analytics.service.masterdata.MasterDataTypeRegistry;
import com.exchange.platform.analytics.utils.masterdata.MasterDataStore;
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

    public MasterDataServiceImpl(MdSyncBatchMapper batchMapper,
            MasterDataPublishApplicationService publishApplicationService, MasterDataTypeRegistry registry,
            MasterDataStore store) {
        this.batchMapper = batchMapper;
        this.publishApplicationService = publishApplicationService;
        this.registry = registry;
        this.store = store;
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
        equals.put("marketType", marketType.getValue());
        equals.put("exchangeCode", exchangeCode);
        equals.put("publisherCode", publisherCode);
        equals.put("indexCategory", indexCategory.getValue());
        equals.put("schemeCode", schemeCode);
        equals.put("level", level);
        MasterDataQuery query = new MasterDataQuery(equals, Map.of(), keyword, List.of("assetCode", "assetName"),
                "assetCode", true, pageNo, pageSize);
        List<Map<String, Object>> rows = store.query(definition, query);
        long total = store.count(definition, query);
        return new PageResult<>(pageNo, pageSize, total, rows);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> detail(MarketAssetType type, String assetCode) {
        if (type == null || !StringUtils.hasText(assetCode))
            throw new BusinessException(40004, "业务主键不能为空");
        Map<String, Object> row = store.findOne(definition(type), Map.of("assetCode", assetCode.trim()));
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
    public PageResult<SyncBatchView> batches(String masterDataType, String datasetCode, String status, Long bindingId,
            String triggerType, int pageNo, int pageSize) {
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
    public SyncBatchView publish(String masterDataType, String sourceCode, String taskRunId, String datasetCode,
            String syncType) {
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
}
