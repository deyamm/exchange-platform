package com.exchange.platform.analytics.entity  ;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.exchange.platform.analytics.api.enums.ResultType;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 应用结果表（T-17）。
 * 统一承载市场概览、标的详情、财务摘要、排行、对比等结果摘要，
 * 通过 result_type 区分结果类型。
 */
@Data
@Accessors(chain = true)
@TableName("app_result")
public class AppResult {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("result_id")
    private String resultId;

    /** 结果类型，使用 result_type 枚举 */
    @TableField("result_type")
    private ResultType resultType;

    @TableField("scene_code")
    private String sceneCode;

    @TableField("target_code")
    private String targetCode;

    @TableField("metric_code")
    private String metricCode;

    @TableField("related_object_type")
    private String relatedObjectType;

    @TableField("related_object_id")
    private String relatedObjectId;

    /** 结果摘要，JSON 字符串 */
    @TableField("result_summary_json")
    private String resultSummaryJson;

    @TableField("source_data_time")
    private LocalDateTime sourceDataTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
