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

    @TableField("unit")
    private String unit;

    @TableField("display_format")
    private String displayFormat;

    @TableField("data_source_code")
    private String dataSourceCode;

    @TableField("calc_period")
    private String calcPeriod;

    @TableField("scene_codes_json")
    private String sceneCodesJson;

    @TableField("status")
    private CommonStatus status;

    @TableField("caliber_desc")
    private String caliberDesc;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField(value = "created_by", fill = FieldFill.INSERT)
    private String createdBy;

    @TableField(value = "updated_by", fill = FieldFill.INSERT_UPDATE)
    private String updatedBy;
}