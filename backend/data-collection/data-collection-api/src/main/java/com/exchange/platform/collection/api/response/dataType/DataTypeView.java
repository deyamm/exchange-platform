package com.exchange.platform.collection.api.response.dataType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.exchange.platform.collection.api.enums.CommonStatus;
import com.exchange.platform.collection.api.enums.DataTypeNodeType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DataTypeView {
    private String dataTypeCode;
    private String dataTypeName;
    private String dataTypeLabel;
    private String parentCode;
    private DataTypeNodeType nodeType;
    private Integer sortNo;
    private Integer nodeLevel;
    private Boolean isLeaf;
    private CommonStatus status;
    private Integer version;
    private String description;
    private Long fieldCount;
    private Long versionCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<DataTypeView> children = new ArrayList<>();
}
