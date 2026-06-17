package com.exchange.platform.collection.api.response.tasks;

import java.time.LocalDateTime;

import com.exchange.platform.collection.api.enums.CommonStatus;
import com.exchange.platform.collection.api.enums.ConfigStatus;

import lombok.Data;

/**
 * 采集任务配置视图
 * 展示任务配置的完整绑定关系，包含数据主题、数据类型版本、规则和存储映射。
 */
@Data
public class TaskConfigView {

    /** 任务配置编码 */
    private String taskConfigCode;

    /** 任务编码 */
    private String taskCode;

    /** 绑定的模板版本号 */
    private Integer taskTemplateVersionNo;

    /** 数据主题编码 */
    private String dataTopicCode;

    /** 数据类型编码 */
    private String dataTypeCode;

    /** 数据类型版本号 */
    private Integer dataTypeVersionNo;

    /** 存储映射编码 */
    private String storageMappingCode;

    /** 绑定规则编码列表 JSON */
    private String ruleCodesJson;

    /** 配置状态 */
    private ConfigStatus configStatus;

    /** 启停状态 */
    private CommonStatus status;

    /** 配置说明 */
    private String description;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
