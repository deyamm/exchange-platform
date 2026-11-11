package com.exchange.platform.analytics.entity  ;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.exchange.platform.analytics.api.enums.CommonStatus;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 指标定义表
 */
@Data
@Accessors(chain = true)
@TableName("app_metric")
public class AppMetric {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("metric_code")
    private String metricCode;

    @TableField("metric_name")
    private String metricName;

    @TableField("metric_category")
    private String metricCategory;

    /** 指标适用的标的类型，由用户创建时选择，后续禁止变更。 AssetType类型的枚举值。 */
    @TableField("target_type")
    private String targetType;

    @TableField("unit")
    private String unit;

    /** 新版来源绑定：旧 data_source_code 保留在表中仅用于历史记录回溯。 */
    @TableField("dataset_code")
    private String datasetCode;

    /** 新版来源绑定的标准字段编码。 */
    @TableField("field_code")
    private String fieldCode;

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
