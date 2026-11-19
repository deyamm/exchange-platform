package com.exchange.platform.analytics.api.registry;

import java.util.List;
import com.exchange.platform.analytics.api.enums.MasterDataType;

public record MasterDataTypeDefinition(
    MasterDataType typeCode, 
    String displayName,
    String domain,
    String category,
    List<TargetFieldDefinition> fields
) {
    public MasterDataTypeDefinition {
        fields = fields == null ? List.of() : List.copyOf(fields);
    }
}
