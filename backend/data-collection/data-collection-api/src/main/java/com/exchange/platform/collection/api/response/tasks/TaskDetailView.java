package com.exchange.platform.collection.api.response.tasks;

import lombok.Data;

/**
 * 采集任务详情视图对象，用于任务详情页面展示
 * 包含任务模板基本信息和最新的配置信息、存储映射信息等
 */
@Data
public class TaskDetailView {
    /** 任务模板摘要 */
    private TaskTemplateView taskInfo;

    /** 当前任务模板版本详情 */
    private TaskTemplateVersionView currentVersion;

    /** 当前任务配置 */
    private TaskConfigView config;

    /** 当前存储映射（含字段映射） */
    private StorageMappingView storageMapping;
}
