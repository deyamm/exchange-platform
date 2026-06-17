CREATE TABLE IF NOT EXISTS app_scene (
    id BIGINT NOT NULL AUTO_INCREMENT,
    scene_code VARCHAR(64) NOT NULL,
    scene_name VARCHAR(128) NOT NULL,
    scene_type tinyint(1) NOT NULL COMMENT '场景类型，枚举：0=MARKET_OVERVIEW（市场概览）、1=TARGET_RESEARCH（标的研究）、2=FINANCIAL_ANALYSIS（财务分析）、3=ANNOUNCEMENT_VIEW（公告查看）、4=SCREENING_COMPARISON（筛选对比）、5=ALERT_HANDLING（提醒处理）、6=RESEARCH_RECORD（研究记录）、7=TRACE_QUERY（追溯查询）',
    sort_no INT DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    description VARCHAR(500) NULL,
    created_at DATETIME DEFAULT NULL COMMENT '创建时间',
    updated_at DATETIME DEFAULT NULL COMMENT '更新时间',
    created_by VARCHAR(64) NULL,
    updated_by VARCHAR(64) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_app_scene_code (scene_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应用场景表';

CREATE TABLE IF NOT EXISTS target_group (
    id BIGINT NOT NULL AUTO_INCREMENT,
    group_code VARCHAR(64) NOT NULL,
    group_name VARCHAR(128) NOT NULL,
    sort_no INT DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    description VARCHAR(500) NULL,
    created_at DATETIME DEFAULT NULL COMMENT '创建时间',
    updated_at DATETIME DEFAULT NULL COMMENT '更新时间',
    created_by VARCHAR(64) NULL,
    updated_by VARCHAR(64) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_target_group_code (group_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标的分组表';

CREATE TABLE IF NOT EXISTS watch_target (
    id BIGINT NOT NULL AUTO_INCREMENT,
    target_code VARCHAR(64) NOT NULL,
    target_name VARCHAR(128) NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    market_code VARCHAR(32) NOT NULL,
    group_code VARCHAR(64) NULL,
    sort_no INT DEFAULT 0,
    important_flag TINYINT(1) NOT NULL DEFAULT 0,
    watch_status TINYINT NOT NULL DEFAULT 0,
    watch_reason VARCHAR(500) NULL,
    created_at DATETIME DEFAULT NULL COMMENT '创建时间',
    updated_at DATETIME DEFAULT NULL COMMENT '更新时间',
    created_by VARCHAR(64) NULL,
    updated_by VARCHAR(64) NULL,
    PRIMARY KEY (id),
    KEY idx_watch_target_code (target_code),
    KEY idx_watch_target_group_status (group_code, watch_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='关注标的表';

CREATE TABLE IF NOT EXISTS app_metric (
    id BIGINT NOT NULL AUTO_INCREMENT,
    metric_code VARCHAR(64) NOT NULL,
    metric_name VARCHAR(128) NOT NULL,
    metric_category VARCHAR(64) NULL,
    unit VARCHAR(32) NULL,
    display_format VARCHAR(32) NULL,
    data_source_code VARCHAR(64) NULL,
    calc_period VARCHAR(32) NULL,
    scene_codes_json JSON NULL,
    status TINYINT NOT NULL DEFAULT 1,
    caliber_desc VARCHAR(1000) NULL,
    created_at DATETIME DEFAULT NULL COMMENT '创建时间',
    updated_at DATETIME DEFAULT NULL COMMENT '更新时间',
    created_by VARCHAR(64) NULL,
    updated_by VARCHAR(64) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_app_metric_code (metric_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应用指标表';

CREATE TABLE IF NOT EXISTS view_config (
    id BIGINT NOT NULL AUTO_INCREMENT,
    view_code VARCHAR(64) NOT NULL,
    view_name VARCHAR(128) NOT NULL,
    scene_code VARCHAR(64) NULL,
    view_type VARCHAR(32) NULL,
    default_flag TINYINT(1) NOT NULL DEFAULT 0,
    field_config_json JSON NULL,
    sort_config_json JSON NULL,
    filter_config_json JSON NULL,
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME DEFAULT NULL COMMENT '创建时间',
    updated_at DATETIME DEFAULT NULL COMMENT '更新时间',
    created_by VARCHAR(64) NULL,
    updated_by VARCHAR(64) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_view_config_code (view_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视图配置表';

CREATE TABLE IF NOT EXISTS alert_rule (
    id BIGINT NOT NULL AUTO_INCREMENT,
    rule_code VARCHAR(64) NOT NULL,
    rule_name VARCHAR(128) NOT NULL,
    rule_type TINYINT NOT NULL DEFAULT 0,
    target_code VARCHAR(64) NULL,
    group_code VARCHAR(64) NULL,
    metric_code VARCHAR(64) NULL,
    condition_json JSON NULL,
    notice_channels_json JSON NULL,
    effective_start_time DATETIME NULL,
    effective_end_time DATETIME NULL,
    status TINYINT NOT NULL DEFAULT 1,
    description VARCHAR(500) NULL,
    created_at DATETIME DEFAULT NULL COMMENT '创建时间',
    updated_at DATETIME DEFAULT NULL COMMENT '更新时间',
    created_by VARCHAR(64) NULL,
    updated_by VARCHAR(64) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_alert_rule_code (rule_code),
    KEY idx_alert_rule_target (target_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提醒规则表';

CREATE TABLE IF NOT EXISTS research_tag (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tag_code VARCHAR(64) NOT NULL,
    tag_name VARCHAR(128) NOT NULL,
    tag_category VARCHAR(64) NULL,
    status TINYINT NOT NULL DEFAULT 1,
    description VARCHAR(500) NULL,
    created_at DATETIME DEFAULT NULL COMMENT '创建时间',
    updated_at DATETIME DEFAULT NULL COMMENT '更新时间',
    created_by VARCHAR(64) NULL,
    updated_by VARCHAR(64) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_research_tag_code (tag_code),
    KEY idx_research_tag_category_status (tag_category, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='研究标签表';

CREATE TABLE IF NOT EXISTS object_tag_relation (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tag_code VARCHAR(64) NOT NULL,
    object_type VARCHAR(64) NOT NULL,
    object_id VARCHAR(64) NOT NULL,
    created_at DATETIME DEFAULT NULL COMMENT '创建时间',
    updated_at DATETIME DEFAULT NULL COMMENT '更新时间',
    created_by VARCHAR(64) NULL,
    updated_by VARCHAR(64) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_object_tag_relation (tag_code, object_type, object_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对象标签关系表';
