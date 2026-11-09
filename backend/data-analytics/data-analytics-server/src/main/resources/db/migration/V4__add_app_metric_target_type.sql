-- ApplicationMetric.target_type 是用户明确选择的业务适用范围；不可从来源 Dataset 隐式替代。
ALTER TABLE app_metric
    ADD COLUMN target_type VARCHAR(16) NULL COMMENT '标的类型：STOCK/INDEX/FUTURE/FUND/BOND' AFTER metric_category;

CREATE INDEX idx_app_metric_target_status ON app_metric (target_type, status);

-- Dataset 与 app_metric 分属不同服务数据库，Flyway 不跨库猜测或回填。
-- 旧指标保持 NULL，需在界面中按真实 Dataset 标的类型重新绑定后保存。
