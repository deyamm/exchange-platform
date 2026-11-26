package com.exchange.platform.collection.api.response.execution;

import java.util.Map;

import com.exchange.platform.collection.api.enums.DatasetStatus;
import com.exchange.platform.common.core.enums.MarketAssetType;

import lombok.Data;

/**
 * 统一指标读取的受控 Dataset 绑定。
 * 物理定位信息仅供 data-analytics 服务转发到 Python 数据服务，不适用于普通业务 UI。
 */
@Data
public class MetricDatasetExecutionView {
    private String datasetCode;
    private String storageType;
    private String dataSourceCode;
    private MarketAssetType assetType;
    private DatasetStatus datasetStatus;
    private String schemaName;
    private String tableName;
    private String targetCodeColumn;
    private String dataTimeColumn;
    // 逻辑字段 → 物理列映射
    private Map<String, String> fields;
}
