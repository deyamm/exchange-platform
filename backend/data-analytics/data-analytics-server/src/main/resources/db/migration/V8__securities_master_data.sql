CREATE TABLE IF NOT EXISTS md_market_asset_stock (
    asset_code VARCHAR(32) NOT NULL,
    asset_name VARCHAR(64) NOT NULL,
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
    publisher_code VARCHAR(32) NULL,
    index_category VARCHAR(32) NULL,
    launch_date DATE NULL,
    end_date DATE NULL,
    created_at DATETIME NULL,
    updated_at DATETIME NULL,
    KEY idx_md_market_asset_index_filter (publisher_code, index_category),
    UNIQUE KEY uk_md_stock_business_key (asset_code),
    CONSTRAINT ck_md_market_asset_index_dates CHECK (launch_date IS NULL OR end_date IS NULL OR launch_date <= end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='指数及广义组合主数据扩展';


