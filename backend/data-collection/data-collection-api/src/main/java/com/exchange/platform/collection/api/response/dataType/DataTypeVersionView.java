package com.exchange.platform.collection.api.response.dataType;

import java.time.LocalDateTime;

import com.exchange.platform.collection.api.enums.PublishStatus;

import lombok.Getter;
import lombok.Setter;

/* 数据类型版本视图 */
@Getter
@Setter
public class DataTypeVersionView {
    private Integer version;
    private String dataTypeCode;
    private String versionName;
    private String fieldSchemaContent;
    private PublishStatus publishStatus;
    private LocalDateTime publishTime;
    private String changeSummary;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
