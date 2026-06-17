package com.exchange.platform.collection.api.response.monitor;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ProblemRecordView {

    private String problemId;

    /** 执行记录编号 */
    private String runId;

    /** 问题类型 */
    private String problemType;

    /** 问题说明 */
    private String problemMessage;

    /** 问题样本 JSON */
    private String sampleDataJson;

    /** 创建时间（自动填充） */
    private LocalDateTime createdAt;
}
