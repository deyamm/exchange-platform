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
@TableName("problem_record")
public class ProblemRecord {

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 问题数据编号，全局唯一 */
    @TableField("problem_id")
    private String problemId;

    /** 执行记录编号 */
    @TableField("run_id")
    private String runId;

    /** 问题类型 */
    @TableField("problem_type")
    private String problemType;

    /** 问题说明 */
    @TableField("problem_message")
    private String problemMessage;

    /** 问题样本 JSON */
    @TableField("sample_data_json")
    private String sampleDataJson;

    /** 创建时间（自动填充） */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

}
