package com.exchange.platform.analytics.api.registry;

import java.util.List;
import com.exchange.platform.analytics.api.enums.MappingMode;

/** 不允许用户新增/删除的核心目标字段契约。 */
public record TargetFieldDefinition(
    String fieldCode, 
    String fieldName, 
    String valueType,
    boolean required, 
    String enumType, 
    List<MappingMode> allowedMappingModes
) {
    public TargetFieldDefinition {
        allowedMappingModes = allowedMappingModes == null ? List.of() : List.copyOf(allowedMappingModes);
    }
}
