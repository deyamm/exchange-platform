package com.exchange.platform.analytics.entity  ;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.exchange.platform.analytics.api.enums.WatchStatus;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 关注标的，这里只保存了应用域的信息，具体的标的还需要后续添加
 */
@Data
@Accessors(chain = true)
@TableName("watch_target")
public class WatchTarget {
    
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("target_code")
    private String targetCode;

    @TableField("target_name")
    private String targetName;

    @TableField("target_type")
    private String targetType;

    @TableField("market_code")
    private String marketCode;

    @TableField("group_code")
    private String groupCode;

    @TableField("sort_no")
    private Integer sortNo;

    @TableField("important_flag")
    private Boolean importantFlag;

    @TableField("watch_status")
    private WatchStatus watchStatus;
    
    @TableField("watch_reason")
    private String watchReason;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField(value = "created_by", fill = FieldFill.INSERT)
    private String createdBy;

    @TableField(value = "updated_by", fill = FieldFill.INSERT_UPDATE)
    private String updatedBy;
}
