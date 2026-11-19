CREATE TABLE IF NOT EXISTS md_source_binding (
    id BIGINT NOT NULL AUTO_INCREMENT,
    binding_code VARCHAR(64) NOT NULL,
    binding_name VARCHAR(128) NOT NULL,
    master_data_type VARCHAR(64) NOT NULL,
    dataset_code VARCHAR(128) NOT NULL,
    trigger_mode VARCHAR(32) NOT NULL DEFAULT 'MANUAL',
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    description VARCHAR(500) NULL,
    created_at DATETIME NULL,
    updated_at DATETIME NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_md_source_binding_code (binding_code),
    UNIQUE KEY uk_md_source_binding_dataset_type (dataset_code, master_data_type),
    KEY idx_md_source_binding_query (master_data_type, dataset_code, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='主数据 Dataset 来源绑定';

CREATE TABLE IF NOT EXISTS md_source_field_mapping (
    id BIGINT NOT NULL AUTO_INCREMENT,
    binding_id BIGINT NOT NULL,
    target_field VARCHAR(128) NOT NULL,
    mapping_mode VARCHAR(16) NOT NULL,
    source_field_code VARCHAR(128) NULL,
    constant_value VARCHAR(512) NULL,
    created_at DATETIME NULL,
    updated_at DATETIME NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_md_source_field_mapping (binding_id, target_field),
    CONSTRAINT fk_md_source_field_mapping_binding FOREIGN KEY (binding_id) REFERENCES md_source_binding(id),
    CONSTRAINT ck_md_source_field_mapping_mode CHECK (mapping_mode IN ('FIELD', 'CONSTANT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='主数据来源字段映射';

ALTER TABLE md_sync_batch
ADD COLUMN binding_id BIGINT NULL;

ALTER TABLE md_sync_batch
ADD COLUMN trigger_type VARCHAR(32) NULL;

ALTER TABLE md_sync_batch
ADD KEY idx_md_sync_batch_binding (binding_id, started_at);

ALTER TABLE md_sync_batch
ADD CONSTRAINT fk_md_sync_batch_binding
FOREIGN KEY (binding_id) REFERENCES md_source_binding(id);
