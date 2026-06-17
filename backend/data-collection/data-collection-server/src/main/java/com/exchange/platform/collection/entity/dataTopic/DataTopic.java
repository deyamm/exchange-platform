package com.exchange.platform.collection.entity.dataTopic;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.exchange.platform.collection.api.enums.CommonStatus;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@TableName("data_topic")
public class DataTopic {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("data_topic_code")
    private String dataTopicCode;

    @TableField("data_topic_name")
    private String dataTopicName;

    @TableField("data_topic_label")
    private String dataTopicLabel;

    @TableField("parent_code")
    private String parentCode;

    @TableField("node_level")
    private Integer nodeLevel;

    @TableField("is_leaf")
    private Boolean isLeaf;

    @TableField("sort_no")
    private Integer sortNo;

    @TableField("status")
    private CommonStatus status;

    @TableField("description")
    private String description;

    @TableField(value = "created_by", fill = FieldFill.INSERT)
    private String createdBy;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_by", fill = FieldFill.INSERT_UPDATE)
    private String updatedBy;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

}
