CREATE TABLE IF NOT EXISTS data_type_field (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',

    data_type_code VARCHAR(32) NOT NULL COMMENT '数据类型编码',

    field_code VARCHAR(32) NOT NULL COMMENT '字段编码',
    field_name VARCHAR(128) NOT NULL COMMENT '字段名称',
    field_type VARCHAR(32) NOT NULL COMMENT '字段类型',

    default_value VARCHAR(255) DEFAULT NULL COMMENT '默认值',

    required TINYINT(1) DEFAULT 0 COMMENT '是否必填，0-否，1-是',
    primary_key TINYINT(1) DEFAULT 0 COMMENT '是否主键字段，0-否，1-是',
    time_field_flag TINYINT(1) DEFAULT 0 COMMENT '是否时间字段，0-否，1-是',

    sort_no INT DEFAULT 0 COMMENT '排序号',

    description VARCHAR(255) DEFAULT NULL COMMENT '描述',

    created_by VARCHAR(45) DEFAULT NULL COMMENT '创建人',
    created_at DATETIME DEFAULT NULL COMMENT '创建时间',
    updated_by VARCHAR(45) DEFAULT NULL COMMENT '更新人',
    updated_at DATETIME DEFAULT NULL COMMENT '更新时间',

    PRIMARY KEY (id),

    UNIQUE KEY uk_data_type_field_code (data_type_code, field_code),
    KEY idx_data_type_code (data_type_code)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_general_ci
  COMMENT='数据类型字段表';
