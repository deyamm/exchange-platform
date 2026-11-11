CREATE TABLE IF NOT EXISTS alert_rule_runtime (
    id BIGINT NOT NULL AUTO_INCREMENT,
    rule_id BIGINT NOT NULL,
    last_evaluated_data_time DATETIME NULL,
    condition_true TINYINT(1) NOT NULL DEFAULT 0,
    updated_at DATETIME DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_alert_rule_runtime_rule (rule_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提醒规则运行状态';

CREATE TABLE IF NOT EXISTS alert_message (
    id BIGINT NOT NULL AUTO_INCREMENT,
    rule_id BIGINT NOT NULL,
    target_type VARCHAR(16) NOT NULL,
    target_code VARCHAR(64) NOT NULL,
    target_name_snapshot VARCHAR(128) NULL,
    metric_code VARCHAR(64) NOT NULL,
    metric_name_snapshot VARCHAR(128) NULL,
    unit_snapshot VARCHAR(32) NULL,
    operator VARCHAR(8) NOT NULL,
    threshold DECIMAL(28,10) NOT NULL,
    trigger_value DECIMAL(28,10) NOT NULL,
    message_title VARCHAR(256) NOT NULL,
    message_content VARCHAR(1000) NOT NULL,
    data_time DATETIME NOT NULL,
    triggered_at DATETIME NOT NULL,
    read_status VARCHAR(16) NOT NULL DEFAULT 'UNREAD',
    process_status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_alert_message_idempotent (rule_id, target_code, metric_code, data_time),
    KEY idx_alert_message_target_time (target_type, target_code, triggered_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='指标提醒消息';
