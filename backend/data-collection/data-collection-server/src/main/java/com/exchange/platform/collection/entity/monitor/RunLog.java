package com.exchange.platform.collection.entity.monitor;

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
@TableName("run_log")
public class RunLog {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 日志编号 */
    @TableField("log_id")
    private String logId;

    /** 执行记录编号 */
    @TableField("run_id")
    private String runId;

    /** 日志级别 INFO/ERROR/WARN */
    @TableField("log_level")
    private String logLevel;

    /** 日志内容 */
    @TableField("log_content")
    private String logContent;

    /** 链路编号、具体需要修改 */
    @TableField("trace_id")
    private String traceId;

    /** 创建时间（自动填充） */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
