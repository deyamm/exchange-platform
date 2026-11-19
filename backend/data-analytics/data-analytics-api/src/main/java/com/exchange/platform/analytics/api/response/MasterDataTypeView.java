package com.exchange.platform.analytics.api.response;

import java.util.List;
import com.exchange.platform.analytics.api.enums.MasterDataType;
import com.exchange.platform.analytics.api.registry.TargetFieldDefinition;

public record MasterDataTypeView(MasterDataType typeCode, String displayName, String domain, String category,
        List<TargetFieldDefinition> fields) { }
