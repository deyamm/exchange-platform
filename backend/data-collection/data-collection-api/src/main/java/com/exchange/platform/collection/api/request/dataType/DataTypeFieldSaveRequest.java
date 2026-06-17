package com.exchange.platform.collection.api.request.dataType;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.*;

/** 保存数据类型字段请求，按数据类型批量维护字段结构。 */
public record DataTypeFieldSaveRequest(
    @NotEmpty List<@Valid FieldItem> fields
) {
    public record FieldItem(
        @NotBlank @Size(max = 32) String fieldCode,
        @NotBlank @Size(max = 128) String fieldName,
        @NotBlank @Size(max = 64) String fieldType,
        @Size(max = 256) String defaultValue,
        Boolean required,
        Boolean uniqueKey,
        Integer sortNo,
        @Size(max = 255) String description
    ) {}
}