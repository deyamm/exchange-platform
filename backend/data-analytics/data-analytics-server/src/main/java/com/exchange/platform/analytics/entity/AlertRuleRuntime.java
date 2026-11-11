package com.exchange.platform.analytics.entity;

import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@TableName("alert_rule_runtime")
public class AlertRuleRuntime {
    @TableId(type = IdType.AUTO) private Long id;
    @TableField("rule_id") private Long ruleId;
    @TableField("last_evaluated_data_time") private LocalDateTime lastEvaluatedDataTime;
    @TableField("condition_true") private Boolean conditionTrue;
    @TableField("updated_at") private LocalDateTime updatedAt;
}
