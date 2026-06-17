-- V13 创建存储字段映射表 (T-09)
-- storage_column_mapping 保存来源字段 → 数据类型字段 → 物理列的字段级映射
CREATE TABLE IF NOT EXISTS `storage_column_mapping` (
    `id`                   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `storage_mapping_code` VARCHAR(64)  NOT NULL               COMMENT '所属存储映射编码',
    `source_field_code`    VARCHAR(64)                         COMMENT '来源字段编码',
    `source_field_name`    VARCHAR(128)                        COMMENT '来源字段名称',
    `source_field_type`    VARCHAR(64)                         COMMENT '来源字段类型',
    `data_type_field_code` VARCHAR(64)                         COMMENT '目标数据类型字段编码',
    `data_type_field_type` VARCHAR(64)                         COMMENT '目标数据类型字段类型',
    `physical_column_name` VARCHAR(128)                        COMMENT '目标物理列名',
    `physical_column_type` VARCHAR(64)                         COMMENT '目标物理列类型',
    `default_value`        VARCHAR(255)                        COMMENT '默认值（可选）',
    `required`             TINYINT(1)   NOT NULL DEFAULT 0     COMMENT '是否必填',
    `unique_key`           TINYINT(1)   NOT NULL DEFAULT 0     COMMENT '是否业务唯一键',
    `created_at`           DATETIME     NOT NULL               COMMENT '创建时间',
    `updated_at`           DATETIME     NOT NULL               COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mapping_column` (`storage_mapping_code`, `physical_column_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='存储字段映射表';
