package com.exchange.platform.collection.entity.datasource;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.exchange.platform.collection.api.enums.CommonStatus;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 数据源配置实体（T-10 data_source_config）
 */
@Data
@Accessors(chain = true)
@TableName("data_source_config")
public class DataSourceConfig {

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 数据源编码（唯一） */
    @TableField("data_source_code")
    private String dataSourceCode;

    /** 数据源名称 */
    @TableField("data_source_name")
    private String dataSourceName;

    /** 数据源类型（MYSQL） */
    @TableField("data_source_type")
    private String dataSourceType;

    /** 主机地址 */
    @TableField("host")
    private String host;

    /** 端口 */
    @TableField("port")
    private Integer port;

    /** 用户名 */
    @TableField("username")
    private String username;

    /** 密码（明文存储） */
    @TableField("password")
    private String password;

    /** 数据库名 */
    @TableField("database_name")
    private String databaseName;

    /** 连接参数（可选） */
    @TableField("connection_params")
    private String connectionParams;

    /** 启停状态 */
    @TableField("status")
    private CommonStatus status;

    /** 备注 */
    @TableField("remark")
    private String remark;

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
