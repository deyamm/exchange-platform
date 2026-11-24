CREATE TABLE IF NOT EXISTS md_sync_batch (
    id BIGINT NOT NULL AUTO_INCREMENT,
    binding_id BIGINT NULL,
    master_data_type VARCHAR(64) NOT NULL,
    source_code VARCHAR(128) NULL,
    task_run_id VARCHAR(128) NULL,
    dataset_code VARCHAR(128) NOT NULL,
    sync_type VARCHAR(32) NOT NULL,
    trigger_type VARCHAR(32) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'CREATED',
    total_count INT NOT NULL DEFAULT 0,
    added_count INT NOT NULL DEFAULT 0,
    updated_count INT NOT NULL DEFAULT 0,
    removed_count INT NOT NULL DEFAULT 0,
    started_at DATETIME NULL,
    published_at DATETIME NULL,
    error_message VARCHAR(2000) NULL,
    created_at DATETIME NULL,
    updated_at DATETIME NULL,
    PRIMARY KEY (id),
    KEY idx_md_sync_batch_query (master_data_type, dataset_code, status, started_at),
    CONSTRAINT fk_md_sync_batch_binding FOREIGN KEY (binding_id) REFERENCES md_source_binding(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='主数据同步发布批次';

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
