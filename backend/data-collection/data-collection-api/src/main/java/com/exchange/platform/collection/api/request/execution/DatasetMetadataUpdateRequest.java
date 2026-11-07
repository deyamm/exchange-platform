package com.exchange.platform.collection.api.request.execution;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * 数据集扩展元数据维护请求。
 * <p>名称和说明可维护；存储定位元数据及数据类型字段结构不允许通过该请求修改。</p>
 *
 * @param datasetName 数据集名称
 * @param datasetDesc 数据集说明
 * @param items       待维护的扩展元数据项
 */
public record DatasetMetadataUpdateRequest(String datasetName, String datasetDesc, @NotNull @Valid List<Item> items) {
    /** 单项元数据维护内容。 */
    public record Item(
        /** 元数据分组。 */
        String metadataGroup,
        /** 元数据编码。 */
        String metadataKey,
        /** 元数据名称。 */
        String metadataName,
        /** 元数据值。 */
        String metadataValue,
        /** 值类型。 */
        String valueType,
        /** 备注。 */
        String remark
    ) {}
}
