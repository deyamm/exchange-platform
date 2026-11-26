package com.exchange.platform.analytics.service.masterdata;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.exchange.platform.analytics.service.support.CollectionDatasetMetadataClient;
import com.exchange.platform.collection.api.response.execution.MetricDatasetExecutionView;
import com.exchange.platform.data.client.DataServiceClient;
import com.exchange.platform.data.client.dto.DatasetRow;
import com.exchange.platform.data.client.dto.DatasetRowsQuery;

/**
 * 主数据 Dataset 的统一读取适配器。
 *
 * <p>
 * 本类只负责“单页 I/O”。自动翻页由 DatasetRowReader.readAll() 统一实现， 因而读取适配与分页策略保持解耦。
 * </p>
 */
@Component
public class UnifiedDatasetRowReader implements DatasetRowReader {

    private final CollectionDatasetMetadataClient metadataClient;
    private final DataServiceClient dataServiceClient;

    public UnifiedDatasetRowReader(CollectionDatasetMetadataClient metadataClient,
            DataServiceClient dataServiceClient) {

        this.metadataClient = metadataClient;
        this.dataServiceClient = dataServiceClient;
    }

    @Override
    public List<Map<String, Object>> read(DatasetReadRequest request) {
        MetricDatasetExecutionView binding = metadataClient.getDatasetForMetricExecution(request.datasetCode());

        if (binding == null || binding.getFields() == null || binding.getFields().isEmpty()) {
            throw new IllegalArgumentException("Dataset 缺少完整 StorageMapping：" + request.datasetCode());
        }

        // 构建逻辑字段 → 物理列映射，用于 DatasetRowsQuery
        Map<String, String> columns = new LinkedHashMap<>();
        for (String fieldCode : request.fieldCodes()) {
            String physicalColumn = binding.getFields().get(fieldCode);

            if (!StringUtils.hasText(physicalColumn)) {
                throw new IllegalArgumentException(
                    "Dataset 缺少所需字段映射：" + fieldCode + "，dataset=" + request.datasetCode());
            }

            columns.put(fieldCode, physicalColumn);
        }

        DatasetRowsQuery query = new DatasetRowsQuery(
            binding.getAssetType().name(), 
            request.datasetCode(), 
            binding.getStorageType(),
            binding.getDataSourceCode(), 
            binding.getSchemaName(), 
            binding.getTableName(), 
            binding.getTargetCodeColumn(),
            binding.getDataTimeColumn(), 
            request.fieldCodes(), 
            columns, 
            request.filters(), 
            request.assetCodes(),
            request.from(), 
            request.to(), 
            request.orderBy(), 
            request.pageNo(), 
            request.pageSize()
        );

        return dataServiceClient.queryDatasetRows(query).stream().map(DatasetRow::fields).toList();
    }
}
