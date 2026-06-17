-- V22 创建调度配置表
CREATE TABLE IF NOT EXISTS `execution_scheduler` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `scheduler_id`     VARCHAR(64)  NOT NULL               COMMENT '调度编号，唯一',
    `task_config_code` VARCHAR(64)                         COMMENT '任务配置编码',
    `task_code`        VARCHAR(64)                         COMMENT '任务编码',
    `cron_expression`  VARCHAR(128) NOT NULL               COMMENT 'Cron 表达式',
    `time_zone`        VARCHAR(64)  NOT NULL               COMMENT '时区',
    `status`           TINYINT      NOT NULL DEFAULT 1     COMMENT '调度状态：PAUSED=0/ACTIVE=1',
    `params_template`  JSON                                 COMMENT '执行参数模板',
    `next_fire_time`   DATETIME                            COMMENT '下次执行时间',
    `last_fire_time`   DATETIME                            COMMENT '最近执行时间',
    `last_run_id`      VARCHAR(64)                         COMMENT '最近执行的 run_id',
    `request_id`       VARCHAR(64)                         COMMENT '幂等请求号',
    `created_at`       DATETIME     NOT NULL               COMMENT '创建时间',
    `updated_at`       DATETIME     NOT NULL               COMMENT '更新时间',
    `created_by`       VARCHAR(64)                         COMMENT '创建人',
    `updated_by`       VARCHAR(64)                         COMMENT '更新人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_scheduler_id` (`scheduler_id`),
    UNIQUE KEY `uk_scheduler_request_id` (`request_id`),
    KEY `idx_scheduler_status_next` (`status`, `next_fire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='调度配置表';
