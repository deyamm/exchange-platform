package com.exchange.platform.collection.api.response.tasks;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * 采集任务模板版本视图对象，用于任务详情页面展示
 * 包含版本信息、任务描述、资产信息、执行参数、返回参数等信息，包含采集任务的具体信息
 * TaskTemplateView则直到指针作用，指向最新版本的TaskTemplateVersionView，方便列表展示和详情展示使用
 */
@Data
public class TaskTemplateVersionView {
    /** 任务编码 */
    private String taskCode;

    /** 版本号 */
    private Integer versionNo;

    /** 任务名称快照 */
    private String taskName;

    /** 任务说明 */
    private String taskDesc;

    /** 处理入口（Python handler） */
    private String handlerName;

    /** 数据源 */
    private String dataSource;

    /** 资产类型 */
    private String assetType;

    /** 业务分类 */
    private String bizType;

    /** 执行参数结构 JSON */
    private String paramsSchemaJson;

    /** 接口可返回字段结构 JSON */
    private String outputFieldsJson;

    /** 返回字段哈希 */
    private String outputFieldsHash;

    /** 可选规则类别编码列表 JSON */
    private String ruleCategoryCodes;

    /** 变更摘要 */
    private String changeSummary;

    /** 同步时间 */
    private LocalDateTime syncTime;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
