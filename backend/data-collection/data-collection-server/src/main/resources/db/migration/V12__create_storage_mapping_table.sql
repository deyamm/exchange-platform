-- V12 创建存储映射主表 (T-08)
-- storage_mapping 保存任务配置与物理表的映射主信息
CREATE TABLE IF NOT EXISTS `storage_mapping` (
    `id`                    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `storage_mapping_code`  VARCHAR(64)  NOT NULL               COMMENT '存储映射编码，唯一',
    `task_config_code`      VARCHAR(64)                         COMMENT '关联任务配置编码',
    `data_source_code`      VARCHAR(64)  NULL                   COMMENT '数据源编码',
    `physical_schema_name`  VARCHAR(128)                        COMMENT '目标物理库名',
    `physical_table_name`   VARCHAR(128)                        COMMENT '目标物理表名',
    `write_strategy`        VARCHAR(32)                         COMMENT '写入策略：INSERT/UPSERT/REPLACE',
    `mapping_status`        TINYINT      NOT NULL DEFAULT 0 COMMENT '映射状态：DRAFT=0/CONFIRMED=1/ENABLED=2/DISABLED=3',
    `confirm_remark`        VARCHAR(500)                        COMMENT '确认备注',
    `created_at`            DATETIME     NOT NULL               COMMENT '创建时间',
    `updated_at`            DATETIME     NOT NULL               COMMENT '更新时间',
    `created_by`            VARCHAR(64)                         COMMENT '创建人',
    `updated_by`            VARCHAR(64)                         COMMENT '更新人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_storage_mapping_code` (`storage_mapping_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='存储映射主表';
