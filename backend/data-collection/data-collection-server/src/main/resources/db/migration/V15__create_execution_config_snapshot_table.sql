-- V16 创建执行配置快照表 (T-13)
-- execution_config_snapshot 创建执行记录时冻结的配置上下文，历史追溯以快照为准
CREATE TABLE IF NOT EXISTS `execution_config_snapshot` (
    `id`                             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `snapshot_id`                    VARCHAR(64)  NOT NULL               COMMENT '快照编号，唯一',
    `run_id`                         VARCHAR(64)  NOT NULL               COMMENT '执行记录编号',
    `task_config_code`               VARCHAR(64)                         COMMENT '任务配置编码',
    `task_code`                      VARCHAR(64)                         COMMENT '任务编码',
    `task_template_version`          INT                                 COMMENT '任务模板版本号',
    `data_topic_code`                VARCHAR(32)                         COMMENT '数据主题编码',
    `data_type_code`                 VARCHAR(32)                         COMMENT '数据类型编码',
    `data_type_version`              INT                                 COMMENT '数据类型版本号',
    `handler_name`                   VARCHAR(64)                         COMMENT '处理器名称',
    `snapshot_content`               JSON                                COMMENT '执行时任务配置快照',
    `created_at`                     DATETIME     NOT NULL               COMMENT '创建时间',
    `created_by`                     VARCHAR(64)                         COMMENT '创建人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_snapshot_id` (`snapshot_id`),
    UNIQUE KEY `uk_snapshot_run_id` (`run_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='执行配置快照表';
