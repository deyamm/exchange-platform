-- V15 创建执行记录表 (T-12)
-- task_run 历史追溯主线，保存一次执行的基础信息和执行时绑定的关键编码
CREATE TABLE IF NOT EXISTS `task_run` (
    `id`                       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `run_id`                   VARCHAR(64)  NOT NULL               COMMENT '执行记录编号，唯一',
    `task_config_code`         VARCHAR(64)                         COMMENT '执行时任务配置编码',
    `task_code`                VARCHAR(64)                         COMMENT '任务编码',
    `trigger_type`             TINYINT      NOT NULL DEFAULT 0 COMMENT '触发方式：MANUAL=0/SCHEDULED=1',
    `scheduler_id`             VARCHAR(64)  NULL                   COMMENT '调度配置编号',
    `request_id`               VARCHAR(64)                         COMMENT '幂等请求号',
    `run_status`               TINYINT      NOT NULL DEFAULT 0 COMMENT '执行状态：CREATED=0/PENDING=1/RUNNING=2/SUCCESS=3/FAILED=4',
    `start_time`               DATETIME                            COMMENT '开始时间',
    `scheduled_fire_time`      DATETIME     NULL                   COMMENT '调度触发时间',
    `end_time`                 DATETIME                            COMMENT '结束时间',
    `run_info`                 VARCHAR(1000)                       COMMENT '执行信息，用于记录执行结果信息、错误信息等',
    `created_at`               DATETIME     NOT NULL               COMMENT '创建时间',
    `updated_at`               DATETIME     NOT NULL               COMMENT '更新时间',
    `created_by`               VARCHAR(64)                         COMMENT '创建人',
    `updated_by`               VARCHAR(64)                         COMMENT '更新人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_run_id` (`run_id`),
    UNIQUE KEY `uk_request_id` (`request_id`),
    KEY `idx_task_run_scheduler_id` (`scheduler_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='执行记录表';
