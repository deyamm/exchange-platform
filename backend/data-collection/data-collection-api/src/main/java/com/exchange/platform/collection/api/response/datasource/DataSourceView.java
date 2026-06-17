package com.exchange.platform.collection.api.response.datasource;

import java.time.LocalDateTime;

import com.exchange.platform.collection.api.enums.CommonStatus;
import lombok.Data;

/**
 * 数据源视图对象
 */
@Data
public class DataSourceView {

    /** 数据源编码 */
    private String dataSourceCode;

    /** 数据源名称 */
    private String dataSourceName;

    /** 数据源类型 */
    private String dataSourceType;

    /** 主机 */
    private String host;

    /** 端口 */
    private Integer port;

    /** 用户名 */
    private String username;

    /** 密码（脱敏） */
    private String password;

    /** 数据库名 */
    private String databaseName;

    /** 连接参数 */
    private String connectionParams;

    /** 启停状态 */
    private CommonStatus status;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
