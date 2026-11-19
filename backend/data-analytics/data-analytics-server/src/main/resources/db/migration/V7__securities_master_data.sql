CREATE TABLE IF NOT EXISTS md_market_asset (
    id BIGINT NOT NULL AUTO_INCREMENT,
    market_asset_type VARCHAR(32) NOT NULL,
    asset_code VARCHAR(128) NOT NULL,
    asset_name VARCHAR(256) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NULL,
    updated_at DATETIME NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_md_market_asset_type_code (market_asset_type, asset_code),
    KEY idx_md_market_asset_name (asset_name),
    CONSTRAINT ck_md_market_asset_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='市场资产主数据公共实体';

CREATE TABLE IF NOT EXISTS md_market_asset_stock (
    market_asset_id BIGINT NOT NULL,
    market_type VARCHAR(32) NULL,
    exchange_code VARCHAR(64) NULL,
    currency_code VARCHAR(16) NULL,
    list_date DATE NULL,
    delist_date DATE NULL,
    created_at DATETIME NULL,
    updated_at DATETIME NULL,
    PRIMARY KEY (market_asset_id),
    KEY idx_md_market_asset_stock_market (market_type, exchange_code),
    CONSTRAINT fk_md_market_asset_stock_asset FOREIGN KEY (market_asset_id) REFERENCES md_market_asset(id),
    CONSTRAINT ck_md_market_asset_stock_dates CHECK (list_date IS NULL OR delist_date IS NULL OR list_date <= delist_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='股票主数据扩展';

CREATE TABLE IF NOT EXISTS md_market_asset_index (
    market_asset_id BIGINT NOT NULL,
    publisher_code VARCHAR(32) NULL,
    index_category VARCHAR(32) NULL,
    scheme_code VARCHAR(64) NULL,
    level INT NULL,
    exchange_code VARCHAR(64) NULL,
    launch_date DATE NULL,
    end_date DATE NULL,
    created_at DATETIME NULL,
    updated_at DATETIME NULL,
    PRIMARY KEY (market_asset_id),
    KEY idx_md_market_asset_index_filter (publisher_code, index_category, scheme_code, level),
    CONSTRAINT fk_md_market_asset_index_asset FOREIGN KEY (market_asset_id) REFERENCES md_market_asset(id),
    CONSTRAINT ck_md_market_asset_index_level CHECK (level IS NULL OR level >= 1),
    CONSTRAINT ck_md_market_asset_index_dates CHECK (launch_date IS NULL OR end_date IS NULL OR launch_date <= end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='指数及广义组合主数据扩展';

CREATE TABLE IF NOT EXISTS md_sync_batch (
    id BIGINT NOT NULL AUTO_INCREMENT,
    master_data_type VARCHAR(64) NOT NULL,
    source_code VARCHAR(128) NULL,
    task_run_id VARCHAR(128) NULL,
    dataset_code VARCHAR(128) NOT NULL,
    sync_type VARCHAR(32) NOT NULL,
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
    KEY idx_md_sync_batch_query (master_data_type, dataset_code, status, started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='主数据同步发布批次';

CREATE TABLE IF NOT EXISTS md_market_asset_member (
    id BIGINT NOT NULL AUTO_INCREMENT,
    parent_market_asset_id BIGINT NOT NULL,
    member_market_asset_id BIGINT NOT NULL,
    valid_from DATETIME NOT NULL,
    valid_to DATETIME NULL,
    batch_id BIGINT NOT NULL,
    created_at DATETIME NULL,
    updated_at DATETIME NULL,
    PRIMARY KEY (id),
    KEY idx_md_member_parent_valid (parent_market_asset_id, valid_from, valid_to),
    KEY idx_md_member_member_valid (member_market_asset_id, valid_from, valid_to),
    KEY idx_md_member_batch (batch_id),
    CONSTRAINT fk_md_member_parent FOREIGN KEY (parent_market_asset_id) REFERENCES md_market_asset(id),
    CONSTRAINT fk_md_member_member FOREIGN KEY (member_market_asset_id) REFERENCES md_market_asset(id),
    CONSTRAINT fk_md_member_batch FOREIGN KEY (batch_id) REFERENCES md_sync_batch(id),
    CONSTRAINT ck_md_member_not_self CHECK (parent_market_asset_id <> member_market_asset_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='指数成分历史关系';
