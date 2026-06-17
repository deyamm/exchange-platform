package com.exchange.platform.collection.api.response.tasks;

import java.time.LocalDateTime;

import com.exchange.platform.collection.api.enums.CommonStatus;
import com.exchange.platform.collection.api.enums.SyncStatus;

import lombok.Data;

/**
 * 采集任务模板视图对象（TaskTemplateView）
 * 用于展示采集任务列表/摘要信息，包含 TaskTemplate 的核心字段以及关联的 TaskConfig 和 TaskMapping 的部分信息。
 */
@Data
public class TaskTemplateView {

    /** 任务编码 */
    private String taskCode;

    /** 任务名称 */
    private String taskName;

    /** 当前模板版本号 */
    private Integer currentVersionNo;

    /** 同步状态 */
    private SyncStatus syncStatus;

    /** 启停状态 */
    private CommonStatus status;

    /** 最近同步时间 */
    private LocalDateTime latestSyncTime;

    /** 当前绑定的数据主题编码（来自 task_config） */
    private String dataTopicCode;

    /** 当前绑定的数据类型编码 */
    private String dataTypeCode;

    /** 当前绑定的数据类型版本号 */
    private Integer dataTypeVersionNo;

    /** 任务配置状态 */
    private String configStatus;

    /** 存储映射状态（最新映射） */
    private String mappingStatus;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
