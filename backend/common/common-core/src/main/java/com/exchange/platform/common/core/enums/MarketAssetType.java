package com.exchange.platform.common.core.enums;

/** 市场资产内部类型；不表示 MasterData 发布/同步业务类型。 */
public enum MarketAssetType {
    STOCK("STOCK", "股票"),
    INDEX("INDEX", "指数"),
    FUTURE("FUTURE", "期货"),
    FUND("FUND", "基金"),
    BOND("BOND", "债券");

    private final String value;
    private final String displayName;

    MarketAssetType(String value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    public String getValue() { return value; }
    public String getDisplayName() { return displayName; }

    public static MarketAssetType fromCode(String code) {
        if (code == null || code.isBlank()) return null;
        try {
            return valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("不支持的市场资产类型: " + code, ex);
        }
    }
}
