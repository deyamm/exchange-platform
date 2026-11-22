package com.exchange.platform.analytics.api.registry;

import java.util.List;
import com.exchange.platform.analytics.api.enums.MappingMode;

/** 由后端注册表定义的主数据字段。 */
public record MdFieldDefinition(
    String fieldCode, 
    String fieldName, 
    String valueType,
    boolean required, 
    String enumType, 
    List<MappingMode> allowedMappingModes
) {
    public MdFieldDefinition {
        allowedMappingModes = allowedMappingModes == null ? List.of() : List.copyOf(allowedMappingModes);
    }
}
