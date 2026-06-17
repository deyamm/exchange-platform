package com.exchange.platform.collection.api.request.dataType;

import javax.validation.constraints.*;

import com.exchange.platform.collection.api.enums.CommonStatus;
import com.exchange.platform.collection.api.enums.DataTypeNodeType;



public record DataTypeSaveRequest(
    @NotBlank String saveOrUpdate,
    @NotBlank @Size(max = 32) @Pattern(regexp = "^[O-Z][0-9]*$", message = "数据类型编码必须以O-Z开头，后续为数字") String dataTypeCode,
    @NotBlank @Size(max = 32) String dataTypeName,
    @Size(max = 32) @Pattern(regexp = "^[A-Z_]*$", message = "数据类型标签必须为大写字母或下划线") String dataTypeLabel,
    @Size(max = 32) String parentCode,
    @NotNull DataTypeNodeType nodeType,
    Integer sortNo,
    Integer nodeLevel,
    Boolean isLeaf,
    @NotNull CommonStatus status,
    @Size(max = 255) String description
) {}
