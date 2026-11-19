package com.exchange.platform.analytics.service.masterdata;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import com.exchange.platform.analytics.service.support.CollectionDatasetMetadataClient;
import com.exchange.platform.analytics.service.support.CollectionDatasetMetadataClient.DatasetMetricReadBinding;
import com.exchange.platform.data.client.DataServiceClient;
import com.exchange.platform.data.client.dto.DatasetRow;
import com.exchange.platform.data.client.dto.DatasetRowsQuery;

/**
 * 主数据唯一的 Dataset 读取适配器。
 *
 * <p>读取分两步完成：先从采集服务取得当前 StorageMapping，将逻辑 fieldCode
 * 解析为物理列；再把完整查询交给统一数据服务。主数据领域因此只依赖逻辑
 * Dataset 契约，不感知物理表、Schema 或具体存储类型。</p>
 */
@Component
public class UnifiedDatasetRowReader implements DatasetRowReader {
    private final CollectionDatasetMetadataClient metadataClient;
    private final DataServiceClient dataServiceClient;

    public UnifiedDatasetRowReader(CollectionDatasetMetadataClient metadataClient, DataServiceClient dataServiceClient) {
        this.metadataClient = metadataClient;
        this.dataServiceClient = dataServiceClient;
    }

    @Override
    public List<Map<String, Object>> read(DatasetReadRequest request) {
        // 每次读取都重新解析映射，确保采集侧变更后无需重启分析服务。
        DatasetMetricReadBinding binding = metadataClient.getMetricReadBinding(request.datasetCode());
        if (binding == null || binding.fieldColumns().isEmpty()) {
            throw new IllegalArgumentException("Dataset 缺少完整 StorageMapping：" + request.datasetCode());
        }
        // 仅将调用方请求的逻辑字段转换为物理列，避免无关字段扩大查询结果。
        Map<String, String> columns = request.fieldCodes().stream().collect(Collectors.toMap(code -> code,
                code -> binding.fieldColumns().get(code), (a, b) -> a));
        if (columns.values().stream().anyMatch(value -> !StringUtils.hasText(value))) {
            throw new IllegalArgumentException("Dataset 缺少所需字段映射：" + request.fieldCodes());
        }
        // 统一数据服务负责执行筛选、时间范围、排序和分页，并返回逻辑字段行。
        return dataServiceClient.queryDatasetRows(new DatasetRowsQuery(
                binding.assetType(), request.datasetCode(), binding.storageType(), binding.dataSourceCode(),
                binding.schemaName(), binding.tableName(), binding.targetCodeColumn(), binding.dataTimeColumn(),
                request.fieldCodes(), columns, request.filters(), request.assetCodes(), request.from(), request.to(),
                request.orderBy(), request.pageNo(), request.pageSize())).stream().map(DatasetRow::fields).toList();
    }
}
