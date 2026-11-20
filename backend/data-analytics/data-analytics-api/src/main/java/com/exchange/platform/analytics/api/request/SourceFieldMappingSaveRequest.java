package com.exchange.platform.analytics.api.request;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

public record SourceFieldMappingSaveRequest(
    @NotEmpty List<@Valid Item> mappings
) {
    public record Item(
        String targetField, 
        String mappingMode, 
        String sourceFieldCode, 
        String constantValue
    ) { }
}
