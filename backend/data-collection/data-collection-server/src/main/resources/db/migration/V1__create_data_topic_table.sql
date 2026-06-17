CREATE TABLE IF NOT EXISTS data_topic (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',

    data_topic_code VARCHAR(32) NOT NULL COMMENT '数据主题编码',
    data_topic_name VARCHAR(32) NOT NULL COMMENT '数据主题名称',
    data_topic_label VARCHAR(32) DEFAULT NULL COMMENT '数据主题标签',

    parent_code VARCHAR(32) DEFAULT NULL COMMENT '父级主题编码',
    node_level INT DEFAULT NULL COMMENT '节点层级',
    is_leaf TINYINT(1) DEFAULT 0 COMMENT '是否叶子节点，0-否，1-是',

    sort_no INT DEFAULT 0 COMMENT '排序号',

    status TINYINT(1) DEFAULT NULL COMMENT '状态, 0-禁用，1-启用',

    description VARCHAR(255) DEFAULT NULL COMMENT '描述',

    created_by VARCHAR(45) DEFAULT NULL COMMENT '创建人',
    created_at DATETIME DEFAULT NULL COMMENT '创建时间',
    updated_by VARCHAR(45) DEFAULT NULL COMMENT '更新人',
    updated_at DATETIME DEFAULT NULL COMMENT '更新时间',

    deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除标识：0-未删除，1-已删除',

    PRIMARY KEY (id),

    UNIQUE KEY uk_data_topic_code (data_topic_code),
    KEY idx_parent_code (parent_code)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_general_ci
  COMMENT='数据主题表';