package com.exchange.platform.analytics.api.response;

import java.time.LocalDateTime;
import java.util.List;

import com.exchange.platform.analytics.api.enums.MasterDataType;
import com.exchange.platform.analytics.api.registry.MdFieldDefinition;

/** 已发布主数据类型的详情，以及页面渲染所需的字段结构。 */
public record PublishedMasterDataTypeDetailView(
        MasterDataType typeCode,
        String displayName,
        String domain,
        String category,
        String datasetCode,
        String datasetName,
        long totalCount,
        LocalDateTime publishedAt,
        List<MdFieldDefinition> fields) {

    public PublishedMasterDataTypeDetailView {
        fields = fields == null ? List.of() : List.copyOf(fields);
    }
}
