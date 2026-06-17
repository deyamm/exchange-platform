-- V10 创建采集任务模板版本表 (T-06)
-- task_template_version 保存 Python 同步形成的模板快照，用于历史追溯
CREATE TABLE IF NOT EXISTS `task_template_version` (
    `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `task_code`           VARCHAR(64)  NOT NULL               COMMENT '任务编码',
    `version_no`          INT          NOT NULL               COMMENT '版本号',
    `task_name`           VARCHAR(128)                        COMMENT '任务名称快照',
    `task_desc`           VARCHAR(500)                        COMMENT '任务说明',
    `handler_name`        VARCHAR(128)                        COMMENT '任务处理入口',
    `data_source`         VARCHAR(64)                         COMMENT '数据源',
    `asset_type`          VARCHAR(64)                         COMMENT '资产类型',
    `biz_type`            VARCHAR(64)                         COMMENT '业务分类',
    `params_schema_json`  JSON                                COMMENT '执行参数结构 JSON',
    `output_fields_json`  JSON                                COMMENT '接口可返回字段结构 JSON',
    `output_fields_hash`  VARCHAR(128)                        COMMENT '返回字段哈希（幂等依据）',
    `rule_category_codes` VARCHAR(512)                        COMMENT '可选规则类别编码列表，以,分隔的字符串',
    `change_summary`      VARCHAR(500)                        COMMENT '变更摘要',
    `sync_time`           DATETIME                            COMMENT '同步时间',
    `created_at`          DATETIME     NOT NULL               COMMENT '创建时间',
    `created_by`          VARCHAR(64)                         COMMENT '创建人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_task_version` (`task_code`, `version_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采集任务模板版本表';
