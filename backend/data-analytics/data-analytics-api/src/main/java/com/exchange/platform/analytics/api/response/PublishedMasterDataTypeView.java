package com.exchange.platform.analytics.api.response;

import java.time.LocalDateTime;

import com.exchange.platform.analytics.api.enums.MasterDataType;

/** 已至少成功发布一次的主数据类型摘要。 */
public record PublishedMasterDataTypeView(
        MasterDataType typeCode,
        String displayName,
        String datasetCode,
        String datasetName,
        long totalCount,
        LocalDateTime publishedAt) {
}
