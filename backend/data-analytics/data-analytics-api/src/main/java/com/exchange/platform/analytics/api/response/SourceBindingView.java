package com.exchange.platform.analytics.api.response;

import java.time.LocalDateTime;
import java.util.List;
import com.exchange.platform.analytics.api.enums.MasterDataType;
import com.exchange.platform.analytics.api.enums.ReadinessStatus;

public record SourceBindingView(
    Long id, 
    String bindingCode, 
    String bindingName, 
    MasterDataType masterDataType,
    String datasetCode, 
    String triggerMode, 
    Boolean enabled, 
    String description, 
    ReadinessStatus readiness,
    List<String> readinessErrors, 
    SyncBatchView lastBatch, 
    LocalDateTime createdAt, 
    LocalDateTime updatedAt
) { }
