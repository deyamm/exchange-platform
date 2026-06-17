CREATE TABLE IF NOT EXISTS app_result (
    id BIGINT NOT NULL AUTO_INCREMENT,
    result_id VARCHAR(64) NOT NULL COMMENT '结果ID，唯一标识一次应用执行',
    result_type tinyint NOT NULL COMMENT '结果类型，枚举：0=MARKET_OVERVIEW（市场概览）、1=TARGET_DETAIL（标的详情）、2=FINANCIAL_SUMMARY（财务摘要）、3=RANKING（指标排行）、4=COMPARISON（标的对比）、5=ALERT（提醒结果）',
    scene_code VARCHAR(64) NULL COMMENT '应用场景编码',
    target_code VARCHAR(64) NULL COMMENT '标的代码，可能为空表示整体市场概览',
    metric_code VARCHAR(64) NULL COMMENT '指标代码，可能为空表示整体市场概览',
    related_object_type VARCHAR(64) NULL COMMENT '相关对象类型，如行业、概念等',
    related_object_id VARCHAR(64) NULL COMMENT '相关对象id',
    result_summary_json TEXT NULL COMMENT '结果摘要的JSON字符串，包含一些关键数值和结论，供列表展示使用',
    source_data_time DATETIME NULL COMMENT '结果对应的数据时间',
    created_at DATETIME DEFAULT NULL COMMENT '创建时间',
    updated_at DATETIME DEFAULT NULL COMMENT '更新时间',
    created_by VARCHAR(64) NULL,
    updated_by VARCHAR(64) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_app_result_id (result_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应用结果表';