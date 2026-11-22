package com.exchange.platform.analytics.api.response;

import java.util.Map;

import com.exchange.platform.analytics.api.enums.MasterDataType;

/** Type-independent master-data record returned by generic query endpoints. */
public record MasterDataRecordView(MasterDataType type, Map<String, Object> values) {
    public MasterDataRecordView {
        values = values == null ? Map.of() : Map.copyOf(values);
    }
}
