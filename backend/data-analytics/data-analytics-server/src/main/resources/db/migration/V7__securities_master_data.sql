CREATE TABLE IF NOT EXISTS md_market_asset_stock (
    asset_code VARCHAR(32) NOT NULL,
    asset_name VARCHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    market_type VARCHAR(32) NULL,
    exchange_code VARCHAR(64) NULL,
    currency_code VARCHAR(16) NULL,
    list_date DATE NULL,
    delist_date DATE NULL,
    created_at DATETIME NULL,
    updated_at DATETIME NULL,
    UNIQUE KEY uk_md_stock_business_key (asset_code),
    KEY idx_md_market_asset_stock_market (market_type, exchange_code),
    CONSTRAINT ck_md_market_asset_stock_dates CHECK (list_date IS NULL OR delist_date IS NULL OR list_date <= delist_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='股票主数据扩展';

CREATE TABLE IF NOT EXISTS md_market_asset_index (
    asset_code VARCHAR(32) NOT NULL,
    asset_name VARCHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    publisher_code VARCHAR(32) NULL,
    index_category VARCHAR(32) NULL,
    scheme_code VARCHAR(64) NULL,
    level INT NULL,
    exchange_code VARCHAR(64) NULL,
    launch_date DATE NULL,
    end_date DATE NULL,
    created_at DATETIME NULL,
    updated_at DATETIME NULL,
    KEY idx_md_market_asset_index_filter (publisher_code, index_category, scheme_code, level),
    UNIQUE KEY uk_md_stock_business_key (asset_code),
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

