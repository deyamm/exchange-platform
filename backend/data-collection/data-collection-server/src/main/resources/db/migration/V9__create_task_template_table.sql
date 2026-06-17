-- V9 创建采集任务模板主表 (T-05)
-- task_template 保存 Python 任务处理层上报的任务主档信息
-- 任务配置由 task_config 独立保存，不混用
CREATE TABLE IF NOT EXISTS `task_template` (
    `id`                 BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `task_code`          VARCHAR(64)  NOT NULL               COMMENT '任务编码，全局唯一',
    `task_name`          VARCHAR(128) NOT NULL               COMMENT '任务名称',
    `current_version_id` BIGINT                              COMMENT '当前模板版本主键',
    `current_version_no` INT                                 COMMENT '当前模板版本号',
    `sync_status`        TINYINT      NOT NULL DEFAULT 2 COMMENT '同步状态：SYNCED=0/FAILED=1/PENDING=2',
    `latest_sync_time`   DATETIME                            COMMENT '最近同步时间',
    `created_at`         DATETIME     NOT NULL               COMMENT '创建时间',
    `updated_at`         DATETIME     NOT NULL               COMMENT '更新时间',
    `created_by`         VARCHAR(64)                         COMMENT '创建人',
    `updated_by`         VARCHAR(64)                         COMMENT '更新人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_task_code` (`task_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采集任务模板主表';
