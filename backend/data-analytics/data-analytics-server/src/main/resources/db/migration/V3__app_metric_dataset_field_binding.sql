-- A-03：新增稳定的数据集/标准字段绑定。旧来源、展示和周期列保留，
-- 因为无法安全推断历史 data_source_code 是否等同于 dataset_code。
ALTER TABLE app_metric
    ADD COLUMN dataset_code VARCHAR(64) NULL COMMENT '来源 Dataset 稳定编码' AFTER unit,
    ADD COLUMN field_code VARCHAR(64) NULL COMMENT '来源标准字段稳定编码' AFTER dataset_code,
    ADD COLUMN description VARCHAR(500) NULL COMMENT '指标说明' AFTER caliber_desc;

CREATE INDEX idx_app_metric_dataset_field ON app_metric (dataset_code, field_code);
