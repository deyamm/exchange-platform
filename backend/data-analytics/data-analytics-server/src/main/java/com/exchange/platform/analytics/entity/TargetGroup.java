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
 * 标的分组，可以用于为不同的标的进行分组管理，比如行业、主题等，后续可以根据需要添加更多的分组维度
 */
@Data
@Accessors(chain = true)
@TableName("target_group")
public class TargetGroup {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("group_code")
    private String groupCode;

    @TableField("group_name")
    private String groupName;

    @TableField("sort_no")
    private Integer sortNo;

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