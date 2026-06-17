-- V20 创建异常记录表 (T-17)
-- exception_record 保存执行过程中的异常信息，通过 run_id 关联执行记录
CREATE TABLE IF NOT EXISTS `exception_record` (
    `id`                     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `exception_id`           VARCHAR(64)  NOT NULL               COMMENT '异常编号，唯一',
    `run_id`                 VARCHAR(64)  NOT NULL               COMMENT '执行记录编号',
    `exception_type`         VARCHAR(64)                         COMMENT '异常类型',
    `exception_message`      TEXT                                COMMENT '异常信息',
    `exception_context`      JSON                                COMMENT '异常上下文',
    `created_at`             DATETIME     NOT NULL               COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_exception_id` (`exception_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='异常记录表';
