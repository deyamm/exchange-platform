package com.exchange.platform.analytics.entity.masterdata;

import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import com.exchange.platform.analytics.api.enums.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data @Accessors(chain = true) @TableName("md_sync_batch")
public class MdSyncBatch {
    @TableId(type = IdType.AUTO) private Long id;
    private com.exchange.platform.analytics.api.enums.MasterDataType masterDataType;
    private Long bindingId;
    private com.exchange.platform.analytics.api.enums.TriggerType triggerType;
    private String sourceCode;
    private String taskRunId;
    private String datasetCode;
    private SyncType syncType;
    private SyncBatchStatus status;
    private Integer totalCount;
    private Integer addedCount;
    private Integer updatedCount;
    private Integer removedCount;
    private LocalDateTime startedAt;
    private LocalDateTime publishedAt;
    private String errorMessage;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
}
