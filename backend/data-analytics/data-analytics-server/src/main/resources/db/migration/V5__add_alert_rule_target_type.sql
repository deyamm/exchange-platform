-- 具体标的提醒以 target_type + target_code 唯一标识标的，并在保存时与应用指标类型校验。
-- 历史提醒无法从 target_code 安全推断类型，因此保持 NULL，要求维护后重新保存。
ALTER TABLE alert_rule
    ADD COLUMN target_type VARCHAR(16) NULL COMMENT '标的类型：STOCK/INDEX/FUTURE/FUND/BOND' AFTER target_code;

CREATE INDEX idx_alert_rule_target_type_code ON alert_rule (target_type, target_code);
