package com.exchange.platform.data.client.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 统一 Dataset 逻辑行读取协议。分析域只提交标准 fieldCode 和逻辑过滤，
 * 物理定位字段由受控的内部绑定适配器填充，页面永远不会接触此 DTO。
 */
public record DatasetRowsQuery(String targetType, String datasetCode, String storageType,
        String dataSourceCode, String schemaName, String tableName, String targetCodeColumn,
        String dataTimeColumn, List<String> fieldCodes, Map<String, String> physicalColumns, Map<String, Object> filters,
        List<String> assetCodes, LocalDateTime from, LocalDateTime to, String orderBy,
        Integer pageNo, Integer pageSize) { }
