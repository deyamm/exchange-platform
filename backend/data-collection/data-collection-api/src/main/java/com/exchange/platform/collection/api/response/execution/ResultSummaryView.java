package com.exchange.platform.collection.api.response.execution;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ResultSummaryView {

    /** 摘要编号 */
    private String summaryId;

    /** 执行记录编号，关联TaskRun */
    private String runId;

    /** 总处理条数 */
    private Long totalCount;

    /** 成功条数 */
    private Long successCount;

    /** 失败条数 */
    private Long failureCount;

    /** 结果定位 */
    private String resultLocation;

    /** 摘要信息 json */
    private String summaryContent;

    /** 创建时间（自动填充） */
    private LocalDateTime createdAt;
}
