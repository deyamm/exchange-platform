-- V24 创建数据源配置表 (T-10)
-- data_source_config 保存外部数据源连接信息
CREATE TABLE IF NOT EXISTS `data_source_config` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `data_source_code`  VARCHAR(64)  NOT NULL               COMMENT '数据源编码，唯一',
    `data_source_name`  VARCHAR(128) NOT NULL               COMMENT '数据源名称',
    `data_source_type`  VARCHAR(32)  NOT NULL               COMMENT '数据源类型（MYSQL）',
    `host`              VARCHAR(128) NOT NULL               COMMENT '主机地址',
    `port`              INT          NOT NULL               COMMENT '端口',
    `username`          VARCHAR(128) NOT NULL               COMMENT '用户名',
    `password`          VARCHAR(256) NOT NULL               COMMENT '密码（明文存储）',
    `database_name`     VARCHAR(128) NOT NULL               COMMENT '数据库名',
    `connection_params` VARCHAR(512)                          COMMENT '连接参数（可选）',
    `status`            TINYINT      NOT NULL DEFAULT 1 COMMENT '启停状态：ENABLED=1/DISABLED=0/OFFLINE=2',
    `remark`            VARCHAR(500)                          COMMENT '备注',
    `created_at`        DATETIME     NOT NULL               COMMENT '创建时间',
    `updated_at`        DATETIME     NOT NULL               COMMENT '更新时间',
    `created_by`        VARCHAR(64)                          COMMENT '创建人',
    `updated_by`        VARCHAR(64)                          COMMENT '更新人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_data_source_code` (`data_source_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据源配置表';
