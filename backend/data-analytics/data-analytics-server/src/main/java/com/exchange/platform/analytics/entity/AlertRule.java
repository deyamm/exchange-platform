package com.exchange.platform.analytics.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.exchange.platform.analytics.api.enums.CommonStatus;
import com.exchange.platform.analytics.api.enums.RuleType;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 预警规则表
 */
@Data
@Accessors(chain = true)
@TableName("alert_rule")
public class AlertRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("rule_code")
    private String ruleCode;

    @TableField("rule_name")
    private String ruleName;

    @TableField("rule_type")
    private RuleType ruleType;

    @TableField("target_code")
    private String targetCode;

    @TableField("group_code")
    private String groupCode;

    @TableField("metric_code")
    private String metricCode;

    @TableField("condition_json")
    private String conditionJson;

    @TableField("notice_channels_json")
    private String noticeChannelsJson;

    @TableField("effective_start_time")
    private LocalDateTime effectiveStartTime;

    @TableField("effective_end_time")
    private LocalDateTime effectiveEndTime;

    @TableField("status")
    private CommonStatus status;

    @TableField("description")
    private String description;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField(value = "created_by", fill = FieldFill.INSERT)
    private String createdBy;

    @TableField(value = "updated_by", fill = FieldFill.INSERT_UPDATE)
    private String updatedBy;
}