-- V19 创建运行日志表 (T-16)
-- run_log 不可变留痕记录，通过 run_id 关联执行记录
CREATE TABLE IF NOT EXISTS `run_log` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `log_id`       VARCHAR(64)                          COMMENT '日志编号',
    `run_id`       VARCHAR(64)  NOT NULL                COMMENT '执行记录编号',
    `log_level`    VARCHAR(32)                          COMMENT '日志级别：INFO/WARN/ERROR',
    `log_content`  TEXT                                 COMMENT '日志内容',
    `trace_id`     VARCHAR(64)                          COMMENT '链路编号',
    `created_at`   DATETIME     NOT NULL                COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_log_run_created` (`run_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运行日志表';
