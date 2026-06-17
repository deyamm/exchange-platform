package com.exchange.platform.collection.api.response.dataType;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DataTypeFieldView {
    private String dataTypeCode;
    private String fieldCode;
    private String fieldName;
    private String fieldType;
    private String defaultValue;
    private Boolean required;
    private Boolean uniqueKey;
    private Integer sortNo;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
