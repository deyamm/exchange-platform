-- V17 创建结果摘要表 (T-14)
-- result_summary 保存一次执行的最小结果摘要和结果定位信息
CREATE TABLE IF NOT EXISTS `result_summary` (
    `id`                    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `summary_id`            VARCHAR(64)  NOT NULL               COMMENT '结果摘要编号，唯一',
    `run_id`                VARCHAR(64)  NOT NULL               COMMENT '执行记录编号',
    `total_count`           BIGINT       DEFAULT 0          COMMENT '总处理条数',
    `success_count`         BIGINT       DEFAULT 0          COMMENT '成功条数',
    `failure_count`         BIGINT       DEFAULT 0          COMMENT '异常条数',
    `result_location`       VARCHAR(500)                        COMMENT '结果定位信息',
    `summary_content`       JSON                                COMMENT '摘要内容',
    `created_at`            DATETIME     NOT NULL               COMMENT '创建时间',
    `updated_at`            DATETIME     NOT NULL               COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_summary_id` (`summary_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='结果摘要表';
