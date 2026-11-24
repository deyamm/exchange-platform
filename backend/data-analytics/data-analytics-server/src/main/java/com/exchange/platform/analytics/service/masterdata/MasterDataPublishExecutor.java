package com.exchange.platform.analytics.service.masterdata;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.exchange.platform.analytics.api.enums.SyncBatchStatus;
import com.exchange.platform.analytics.api.registry.MasterDataTypeDefinition;
import com.exchange.platform.analytics.api.registry.MdFieldDefinition;
import com.exchange.platform.analytics.entity.masterdata.MdSyncBatch;
import com.exchange.platform.analytics.mapper.masterdata.MdSyncBatchMapper;
import com.exchange.platform.analytics.utils.masterdata.MasterDataStore;
import com.exchange.platform.analytics.utils.masterdata.MasterDataValidator;
import com.exchange.platform.analytics.utils.masterdata.MasterDataValueConverter;

@Service
public class MasterDataPublishExecutor {
    
    private static final int DATASET_PAGE_SIZE = 2000;
    private final MdSyncBatchMapper batchMapper;
    private final DatasetRowReader rowReader;
    private final MasterDataTypeRegistry registry;
    private final MasterDataStore store;

    public MasterDataPublishExecutor(MdSyncBatchMapper batchMapper, DatasetRowReader rowReader, MasterDataTypeRegistry registry,  MasterDataStore store) {
        this.batchMapper = batchMapper;
        this.rowReader = rowReader;
        this.registry = registry;
        this.store = store;
    }

    @Transactional(rollbackFor = Exception.class)
    public void execute(MdSyncBatch batch, PublishPlan plan) {
        validateBatchMatchesPlan(batch, plan);

        MasterDataTypeDefinition definition = registry.require(plan.masterDataType());

        batch.setStatus(SyncBatchStatus.VALIDATING);
        batchMapper.updateById(batch);

        List<Map<String, Object>> candidates = readAndMapRows(plan).stream().map(row -> normalize(row, definition)).toList();
        MasterDataValidator.validate(definition, candidates);
        
        long existing = store.countAll(definition);
        if (existing > 0 && candidates.size() < existing * 0.7d) {
            throw new IllegalArgumentException("主数据数量异常下降，拒绝覆盖正式主数据");
        }
        int added = 0, updated = 0;
        for (Map<String, Object> candidate : candidates) {
            Map<String, Object> key = definition.businessKeyOf(candidate);
            if (store.findByBusinessKey(definition, key) == null)
                added++;
            else
                updated++;
            store.upsert(definition, candidate);
        }
        batch.setTotalCount(candidates.size())
            .setAddedCount(added)
            .setUpdatedCount(updated)
            .setStatus(SyncBatchStatus.PUBLISHED)
            .setPublishedAt(LocalDateTime.now());
        batchMapper.updateById(batch);
    }

    private Map<String, Object> normalize(Map<String, Object> row, MasterDataTypeDefinition definition) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (MdFieldDefinition field : definition.fields()) {
            values.put(field.fieldCode(), MasterDataValueConverter.convert(field, row.get(field.fieldCode())));
        }
        return values;
    }

    /**
     * 调用数据集读取器读取指定 PublishPlan 的所有数据，并映射为主数据字段。
     * @param plan
     * @return
     */
    private List<Map<String, Object>> readAndMapRows(PublishPlan plan) {
        DatasetRowReader.DatasetReadRequest request = new DatasetRowReader.DatasetReadRequest(plan.datasetCode(),
            plan.datasetFieldCodes(), Map.of(), List.of(), null, null, plan.orderByFieldCode(), 1, DATASET_PAGE_SIZE);
        return rowReader.readAll(request).stream().map(plan::mapRow).toList();
    }

    private void validateBatchMatchesPlan(MdSyncBatch batch, PublishPlan plan) {
        if (batch == null || plan == null || !Objects.equals(batch.getMasterDataType(), plan.masterDataType())
                || !Objects.equals(batch.getDatasetCode(), plan.datasetCode())
                || !Objects.equals(batch.getBindingId(), plan.bindingId())) {
            throw new IllegalArgumentException("同步批次与 PublishPlan 不匹配，拒绝执行");
        }
    }
}
