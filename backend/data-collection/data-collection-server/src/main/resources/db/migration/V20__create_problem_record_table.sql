-- V21 创建问题数据表 (T-18)
-- problem_record 保存采集处理过程中识别的问题数据样本，通过 run_id 关联执行记录
CREATE TABLE IF NOT EXISTS `problem_record` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `problem_id`       VARCHAR(64)  NOT NULL               COMMENT '问题数据编号，唯一',
    `run_id`           VARCHAR(64)  NOT NULL               COMMENT '执行记录编号',
    `problem_type`     VARCHAR(64)                         COMMENT '问题类型',
    `problem_message`  TEXT                                COMMENT '问题说明',
    `sample_data_json` JSON                                COMMENT '问题样本',
    `created_at`       DATETIME     NOT NULL               COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_problem_id` (`problem_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问题数据表';
