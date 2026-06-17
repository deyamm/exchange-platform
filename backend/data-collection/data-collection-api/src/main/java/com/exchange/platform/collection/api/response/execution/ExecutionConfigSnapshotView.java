package com.exchange.platform.collection.api.response.execution;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ExecutionConfigSnapshotView {
    
    /* 快照编号 */
    private String snapshotId;

    /* 执行记录编号 */
    private String runId;

    /* 任务配置编码 */
    private String taskConfigCode;

    /* 任务编码 */
    private String taskCode;

    /* 执行时任务模板版本号 */
    private int taskTemplateVersion;

    /* 数据主题编码 */
    private String dataTopicCode;

    /* 数据类型编码 */
    private String dataTypeCode;
    
    /* 数据类型版本号 */
    private int dataTypeVersion;

    /* 执行入口 */
    private String handlerName;

    /* 快照内容 */
    private String snapshotContent;

    /* 创建时间 */
    private LocalDateTime createdAt;
}
