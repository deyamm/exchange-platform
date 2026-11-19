package com.exchange.platform.analytics.api.response;

import java.time.LocalDateTime;
import com.exchange.platform.analytics.api.enums.SyncBatchStatus;
import com.exchange.platform.analytics.api.enums.SyncType;
import com.exchange.platform.analytics.api.enums.MasterDataType;
import com.exchange.platform.analytics.api.enums.TriggerType;

public record SyncBatchView(Long id, MasterDataType masterDataType, Long bindingId,
        String bindingCode, String bindingName, TriggerType triggerType,
        String sourceCode, String taskRunId, String datasetCode,
        SyncType syncType, SyncBatchStatus status, int totalCount, int addedCount, int updatedCount,
        int removedCount, LocalDateTime startedAt, LocalDateTime publishedAt, String errorMessage) {
}
