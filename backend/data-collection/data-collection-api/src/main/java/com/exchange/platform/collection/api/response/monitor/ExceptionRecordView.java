package com.exchange.platform.collection.api.response.monitor;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ExceptionRecordView {
    
    private String exceptionId;

    private String runId;

    /** 异常类型 */
    private String exceptionType;

    /** 异常信息 */
    private String exceptionMessage;

    /** 异常上下文 json */
    private String exceptionContext;

    /** 创建时间（自动填充） */
    private LocalDateTime createdAt;
}
