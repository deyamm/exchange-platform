package com.exchange.platform.collection.api.response.execution;

import lombok.Data;

/** 数据集扩展元数据视图。 */
@Data
public class DatasetMetadataView {
    /** 元数据分组。 */
    private String metadataGroup;
    /** 元数据编码。 */
    private String metadataKey;
    /** 元数据名称。 */
    private String metadataName;
    /** 元数据值。 */
    private String metadataValue;
    /** 值类型。 */
    private String valueType;
    /** 备注。 */
    private String remark;
}
