package com.exchange.platform.collection.api.response.execution;

import java.util.List;

import com.exchange.platform.collection.api.enums.DatasetStatus;
import com.exchange.platform.collection.api.response.dataType.DataTypeFieldView;
import com.exchange.platform.common.core.enums.AssetType;

import lombok.Data;

/**
 * 应用指标模块使用的数据集绑定视图。
 * <p>字段列表来自 Dataset 当前绑定的 DataTypeVersion 快照，因此调用方不会误用
 * 数据类型草稿字段，也不会接触物理存储定义。</p>
 */
@Data
public class DatasetMetricBindingView {
    private String datasetCode;
    private String datasetName;
    /** 应用指标保存时用来与 targetType 比对的 Dataset 权威标的类型。 */
    private AssetType assetType;
    private DatasetStatus datasetStatus;
    private Integer dataTypeVersionNo;
    private List<DataTypeFieldView> fields;
}
