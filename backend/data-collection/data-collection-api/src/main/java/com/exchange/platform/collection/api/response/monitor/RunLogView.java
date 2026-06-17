package com.exchange.platform.collection.api.response.monitor;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class RunLogView {
    
    /** 日志编号 */
    private String logId;

    /** 执行记录编号 */
    private String runId;

    /** 日志级别 INFO/ERROR/WARN */
    private String logLevel;

    /** 日志内容 */
    private String logContent;

    /** 链路编号、具体需要修改 */
    private String traceId;

    /** 创建时间（自动填充） */
    private LocalDateTime createdAt;
}
