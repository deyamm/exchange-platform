package com.exchange.platform.analytics.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@TableName("alert_message")
public class AlertMessage {
    @TableId(type = IdType.AUTO) private Long id;
    @TableField("rule_id") private Long ruleId;
    @TableField("target_type") private String targetType;
    @TableField("target_code") private String targetCode;
    @TableField("target_name_snapshot") private String targetNameSnapshot;
    @TableField("metric_code") private String metricCode;
    @TableField("metric_name_snapshot") private String metricNameSnapshot;
    @TableField("unit_snapshot") private String unitSnapshot;
    private String operator;
    private BigDecimal threshold;
    @TableField("trigger_value") private BigDecimal triggerValue;
    @TableField("message_title") private String messageTitle;
    @TableField("message_content") private String messageContent;
    @TableField("data_time") private LocalDateTime dataTime;
    @TableField("triggered_at") private LocalDateTime triggeredAt;
    @TableField("read_status") private String readStatus;
    @TableField("process_status") private String processStatus;
    @TableField("created_at") private LocalDateTime createdAt;
}
