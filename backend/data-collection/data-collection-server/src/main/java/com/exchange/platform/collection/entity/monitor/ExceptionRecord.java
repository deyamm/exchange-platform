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
@TableName("exception_record")
public class ExceptionRecord {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("exception_id")
    private String exceptionId;

    @TableField("run_id")
    private String runId;

    /** 异常类型 */
    @TableField("exception_type")
    private String exceptionType;

    /** 异常信息 */
    @TableField("exception_message")
    private String exceptionMessage;

    /** 异常上下文 json */
    @TableField("exception_context")
    private String exceptionContext;

    /** 创建时间（自动填充） */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
