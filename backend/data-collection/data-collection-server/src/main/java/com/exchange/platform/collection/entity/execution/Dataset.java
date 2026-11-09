package com.exchange.platform.collection.entity.execution;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.exchange.platform.collection.api.enums.DatasetStatus;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@TableName("dataset")
public class Dataset {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("dataset_code")
    private String datasetCode;

    @TableField("dataset_name")
    private String datasetName;

    /** 数据集正式标的类型，由本次实际执行使用的任务模板版本派生。 */
    @TableField("asset_type")
    private String assetType;

    @TableField("description")
    private String description;

    @TableField("task_code")
    private String taskCode;

    @TableField("task_config_code")
    private String taskConfigCode;

    @TableField("data_topic_code")
    private String dataTopicCode;

    @TableField("data_type_code")
    private String dataTypeCode;

    @TableField("data_type_version_no")
    private Integer dataTypeVersionNo;

    @TableField("storage_mapping_code")
    private String storageMappingCode;

    @TableField("run_id")
    private String runId;

    @TableField("summary_id")
    private String summaryId;

    @TableField("result_location")
    private String resultLocation;

    @TableField("generated_time")
    private LocalDateTime generatedTime;

    @TableField("latest_task_run_id")
    private String latestTaskRunId;

    @TableField("record_count")
    private Long recordCount;

    @TableField("data_start_time")
    private LocalDateTime dataStartTime;

    @TableField("data_end_time")
    private LocalDateTime dataEndTime;

    @TableField("last_collect_time")
    private LocalDateTime lastCollectTime;

    @TableField("last_success_time")
    private LocalDateTime lastSuccessTime;

    @TableField("dataset_status")
    private DatasetStatus datasetStatus;

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
