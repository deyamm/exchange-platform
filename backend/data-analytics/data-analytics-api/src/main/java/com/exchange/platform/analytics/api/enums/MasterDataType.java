package com.exchange.platform.analytics.api.enums;

/**
 * 系统内置的主数据业务类型。它与 MarketAssetType 分离：MarketAssetType 描述资产， MasterDataType
 * 描述一次发布产生的实体型主数据。
 */
public enum MasterDataType {
    MARKET_ASSET_STOCK("股票主数据", "MARKET_ASSET", "ENTITY"), MARKET_ASSET_INDEX("指数主数据", "MARKET_ASSET", "ENTITY");

    private final String displayName;
    private final String domain;
    private final String category;

    MasterDataType(String displayName, String domain, String category) {
        this.displayName = displayName;
        this.domain = domain;
        this.category = category;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDomain() {
        return domain;
    }

    public String getCategory() {
        return category;
    }
}
