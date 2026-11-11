package com.exchange.platform.collection.api.response.execution;

import java.util.List;

import com.exchange.platform.collection.api.enums.DatasetStatus;
import com.exchange.platform.common.core.enums.AssetType;

import lombok.Data;

/**
 * 统一指标读取的受控 Dataset 绑定。
 * 物理定位信息仅供 data-analytics 服务转发到 Python 数据服务，不适用于普通业务 UI。
 */
@Data
public class DatasetMetricReadBindingView {
    private String datasetCode;
    private AssetType assetType;
    private DatasetStatus datasetStatus;
    private String physicalSchemaName;
    private String physicalTableName;
    private String targetCodeColumn;
    private String dataTimeColumn;
    private List<DatasetMetricFieldBindingView> fields;
}
