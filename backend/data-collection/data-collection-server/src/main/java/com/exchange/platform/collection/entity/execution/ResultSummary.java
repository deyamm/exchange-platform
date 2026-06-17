package com.exchange.platform.collection.entity.execution;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@TableName("result_summary")
public class ResultSummary {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 摘要编号 */
    @TableField("summary_id")
    private String summaryId;

    /** 执行记录编号，关联TaskRun */
    @TableField("run_id")
    private String runId;

    /** 总处理条数 */
    @TableField("total_count")
    private Long totalCount;

    /** 成功条数 */
    @TableField("success_count")
    private Long successCount;

    /** 失败条数 */
    @TableField("failure_count")
    private Long failureCount;

    /** 结果定位 */
    @TableField("result_location")
    private String resultLocation;

    /** 摘要信息 json */
    @TableField("summary_content")
    private String summaryContent;

    /** 创建时间（自动填充） */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间（自动填充） */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
