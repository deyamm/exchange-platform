package com.exchange.platform.analytics.entity  ;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.exchange.platform.analytics.api.enums.AppSceneType;
import com.exchange.platform.analytics.api.enums.CommonStatus;

import lombok.Data;
import lombok.experimental.Accessors;


/**
 * 应用场景表
 */
@Data
@Accessors(chain = true)
@TableName("app_scene")
public class AppScene {
    
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("scene_code")
    private String sceneCode;

    @TableField("scene_name")
    private String sceneName;

    /** 场景类型，用于分类，暂时可以包括：市场观察、标的研究、财务分析、公告查看、筛选对比、提醒处理、研究记录和追溯查询 */
    @TableField("scene_type")
    private AppSceneType sceneType;

    @TableField("sort_no")
    private Integer sortNo;

    /** 状态, 0表示禁用，1表示启用，2表示下线 */
    @TableField("status")
    private CommonStatus status;

    @TableField("description")
    private String description;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT)
    private String createdBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updatedBy;

}
