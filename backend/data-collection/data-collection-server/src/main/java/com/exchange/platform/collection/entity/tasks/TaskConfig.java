package com.exchange.platform.collection.entity.tasks;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.exchange.platform.collection.api.enums.CommonStatus;
import com.exchange.platform.collection.api.enums.ConfigStatus;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 采集任务配置实体（T-07 task_config）
 * <p>保存后台确认后的当前任务配置，包含数据主题、数据类型版本、
 * 规则绑定、存储映射和启停状态等后台管理信息；
 * 与 task_template 分离，不互相覆盖。</p>
 */
@Data
@Accessors(chain = true)
@TableName("task_config")
public class TaskConfig {

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 任务配置业务编码，全局唯一 */
    @TableField("task_config_code")
    private String taskConfigCode;

    /** 所属任务编码（关联 task_template.task_code） */
    @TableField("task_code")
    private String taskCode;

    /** 绑定的任务模板版本号 */
    @TableField("task_template_version_no")
    private Integer taskTemplateVersionNo;

    /** 所属数据主题编码 */
    @TableField("data_topic_code")
    private String dataTopicCode;

    /** 数据类型编码 */
    @TableField("data_type_code")
    private String dataTypeCode;

    /** 数据类型版本号 */
    @TableField("data_type_version_no")
    private Integer dataTypeVersionNo;

    /** 关联的存储映射编码 */
    @TableField("storage_mapping_code")
    private String storageMappingCode;

    /** 绑定规则编码列表（JSON 数组） */
    @TableField("rule_codes_json")
    private String ruleCodesJson;

    /** 配置状态：DRAFT / CONFIRMED / ENABLED / DISABLED / EXPIRED */
    @TableField("config_status")
    private ConfigStatus configStatus;

    /** 启停状态：ENABLED / DISABLED */
    @TableField("status")
    private CommonStatus status;

    /** 配置说明 */
    @TableField("description")
    private String description;

    /** 创建时间（自动填充） */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间（自动填充） */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 创建人（自动填充） */
    @TableField(value = "created_by", fill = FieldFill.INSERT)
    private String createdBy;

    /** 更新人（自动填充） */
    @TableField(value = "updated_by", fill = FieldFill.INSERT_UPDATE)
    private String updatedBy;
}
