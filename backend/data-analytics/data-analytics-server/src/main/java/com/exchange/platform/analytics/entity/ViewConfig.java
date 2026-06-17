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
 * 视图配置表
 */
@Data
@Accessors(chain = true)
@TableName("view_config")
public class ViewConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("view_code")
    private String viewCode;

    @TableField("view_name")
    private String viewName;

    @TableField("scene_code")
    private String sceneCode;

    @TableField("view_type")
    private String viewType;

    @TableField("default_flag")
    private Boolean defaultFlag;

    @TableField("field_config_json")
    private String fieldConfigJson;

    @TableField("sort_config_json")
    private String sortConfigJson;

    @TableField("filter_config_json")
    private String filterConfigJson;

    @TableField("status")
    private CommonStatus status;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField(value = "created_by", fill = FieldFill.INSERT)
    private String createdBy;

    @TableField(value = "updated_by", fill = FieldFill.INSERT_UPDATE)
    private String updatedBy;
}