CREATE TABLE IF NOT EXISTS data_type_version (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',

    version INT NOT NULL COMMENT '版本号',
    data_type_code VARCHAR(32) NOT NULL COMMENT '数据类型编码',
    version_name VARCHAR(64) DEFAULT NULL COMMENT '版本名称',

    field_schema_content LONGTEXT DEFAULT NULL COMMENT '字段结构内容，通常存储字段 schema 的 JSON 字符串',

    publish_status TINYINT(1) DEFAULT NULL COMMENT '发布状态，0-草稿，1-已发布，2-已下线',
    publish_time DATETIME DEFAULT NULL COMMENT '发布时间',

    change_summary VARCHAR(500) DEFAULT NULL COMMENT '变更摘要',

    created_by VARCHAR(45) DEFAULT NULL COMMENT '创建人',
    created_at DATETIME DEFAULT NULL COMMENT '创建时间',
    updated_by VARCHAR(45) DEFAULT NULL COMMENT '更新人',
    updated_at DATETIME DEFAULT NULL COMMENT '更新时间',

    PRIMARY KEY (id),

    UNIQUE KEY uk_data_type_version (data_type_code, version),
    KEY idx_data_type_code (data_type_code)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_general_ci
  COMMENT='数据类型版本表';
