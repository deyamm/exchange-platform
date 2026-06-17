-- V11 创建采集任务配置表 (T-07)
-- task_config 保存后台确认后的配置，与 task_template 分离
CREATE TABLE IF NOT EXISTS `task_config` (
    `id`                       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `task_config_code`         VARCHAR(64)  NOT NULL               COMMENT '任务配置业务编码，唯一',
    `task_code`                VARCHAR(64)  NOT NULL               COMMENT '任务编码',
    `task_template_version_no` INT                                 COMMENT '绑定的模板版本号',
    `data_topic_code`          VARCHAR(32)                         COMMENT '所属数据主题编码',
    `data_type_code`           VARCHAR(32)                         COMMENT '数据类型编码',
    `data_type_version_no`     INT                                 COMMENT '数据类型版本号',
    `storage_mapping_code`     VARCHAR(64)                         COMMENT '关联存储映射编码',
    `rule_codes_json`          JSON                                COMMENT '绑定规则编码列表 JSON',
    `config_status`            TINYINT      NOT NULL DEFAULT 0 COMMENT '配置状态：DRAFT=0/CONFIRMED=1/ENABLED=2/DISABLED=3/EXPIRED=4',
    `status`                   TINYINT      NOT NULL DEFAULT 0 COMMENT '启停状态：ENABLED=1/DISABLED=0',
    `description`              VARCHAR(500)                        COMMENT '配置说明',
    `created_at`               DATETIME     NOT NULL               COMMENT '创建时间',
    `updated_at`               DATETIME     NOT NULL               COMMENT '更新时间',
    `created_by`               VARCHAR(64)                         COMMENT '创建人',
    `updated_by`               VARCHAR(64)                         COMMENT '更新人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_task_config_code` (`task_config_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采集任务配置表';
