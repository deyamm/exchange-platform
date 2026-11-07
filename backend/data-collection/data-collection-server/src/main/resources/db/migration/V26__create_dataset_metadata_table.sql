-- 数据集扩展元数据。存储定位信息也以 STORAGE 元数据项维护，不建设 DatasetStorage 表。
CREATE TABLE IF NOT EXISTS `dataset_metadata` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `dataset_id`      BIGINT       NOT NULL COMMENT '数据集主档 ID',
    `metadata_group`  VARCHAR(32)  NOT NULL COMMENT '元数据分组：BUSINESS/MANAGEMENT/STORAGE/EXTENSION',
    `metadata_key`    VARCHAR(64)  NOT NULL COMMENT '元数据编码',
    `metadata_name`   VARCHAR(128) NOT NULL COMMENT '元数据名称',
    `metadata_value`  TEXT                  COMMENT '元数据值',
    `value_type`      VARCHAR(32)           COMMENT '值类型',
    `remark`          VARCHAR(500)          COMMENT '说明',
    `created_at`      DATETIME     NOT NULL COMMENT '创建时间',
    `updated_at`      DATETIME     NOT NULL COMMENT '更新时间',
    `created_by`      VARCHAR(64)           COMMENT '创建人',
    `updated_by`      VARCHAR(64)           COMMENT '更新人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_dataset_metadata_key` (`dataset_id`, `metadata_group`, `metadata_key`),
    KEY `idx_dataset_metadata_dataset_id` (`dataset_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据集扩展元数据表';
