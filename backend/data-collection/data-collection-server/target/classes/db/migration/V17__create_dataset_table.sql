-- V18 创建数据集表 (T-15)
-- dataset 执行成功后生成或登记的结果资产，归属某一数据类型版本
CREATE TABLE IF NOT EXISTS `dataset` (
    `id`                    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `dataset_code`          VARCHAR(64)  NOT NULL               COMMENT '数据集编码，唯一',
    `dataset_name`          VARCHAR(128)                        COMMENT '数据集名称',
    `run_id`                VARCHAR(64)                         COMMENT '来源执行记录编号',
    `summary_id`            VARCHAR(64)                         COMMENT '结果摘要编号',
    `task_code`             VARCHAR(64)                         COMMENT '来源任务编码',
    `data_topic_code`       VARCHAR(32)                         COMMENT '数据主题编码',
    `data_type_code`        VARCHAR(32)                         COMMENT '数据类型编码',
    `data_type_version_no`  INT                                 COMMENT '数据类型版本号',
    `storage_mapping_code`  VARCHAR(64)                         COMMENT '存储映射编码',
    `result_location`       VARCHAR(500)                        COMMENT '结果定位信息',
    `dataset_status`        TINYINT      NOT NULL DEFAULT 0 COMMENT '数据集状态：GENERATED=0/AVAILABLE=1/INVALID=2/ARCHIVED=3',
    `generated_time`        DATETIME                            COMMENT '生成或登记时间',
    `description`           VARCHAR(500)                        COMMENT '数据集说明',
    `created_at`            DATETIME     NOT NULL               COMMENT '创建时间',
    `updated_at`            DATETIME     NOT NULL               COMMENT '更新时间',
    `created_by`            VARCHAR(64)                         COMMENT '创建人',
    `updated_by`            VARCHAR(64)                         COMMENT '更新人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_dataset_code` (`dataset_code`),
    KEY `idx_dataset_run_id` (`run_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据集表';
