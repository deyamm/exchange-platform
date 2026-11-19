package com.exchange.platform.analytics.api.response;

import com.exchange.platform.analytics.api.enums.MappingMode;

public record SourceFieldMappingView(
    Long id, 
    String targetField, 
    MappingMode mappingMode,
    String sourceFieldCode, 
    String constantValue
) { }
