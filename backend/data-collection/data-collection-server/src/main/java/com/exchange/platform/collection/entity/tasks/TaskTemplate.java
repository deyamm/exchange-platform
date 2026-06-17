package com.exchange.platform.collection.entity.tasks;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.exchange.platform.collection.api.enums.SyncStatus;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 采集任务模板实体（T-05 task_template）
 * <p>保存 Python 任务处理层上报的任务主档信息；
 * 任务配置中的业务属性（数据主题、数据类型版本、规则等）由 TaskConfig 独立保存，
 * 不与此表混用。</p>
 */
@Data
@Accessors(chain = true)
@TableName("task_template")
public class TaskTemplate {

    /** 主键（自增） */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 任务编码，全局唯一，由任务处理层上报确定 */
    @TableField("task_code")
    private String taskCode;

    /** 任务名称 */
    @TableField("task_name")
    private String taskName;

    /** 当前任务模板版本主键（指向 task_template_version.id） */
    @TableField("current_version_id")
    private Long currentVersionId;

    /** 当前任务模板版本号 */
    @TableField("current_version_no")
    private Integer currentVersionNo;

    /** 同步状态：SYNCED / FAILED / PENDING */
    @TableField("sync_status")
    private SyncStatus syncStatus;

    /** 最近同步时间 */
    @TableField("latest_sync_time")
    private LocalDateTime latestSyncTime;

    /** 创建时间（自动填充） */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间（自动填充） */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 创建人（自动填充） */
    @TableField(value = "created_by", fill = FieldFill.INSERT)
    private String createdBy;

    /** 更新人（自动填充） */
    @TableField(value = "updated_by", fill = FieldFill.INSERT_UPDATE)
    private String updatedBy;
}
