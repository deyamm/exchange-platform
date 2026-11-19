package com.exchange.platform.analytics.api.enums;

/** Stable market classification persisted as its protocol code. */
public enum StockMarketType {
    A_SHARE("A_SHARE"),
    B_SHARE("B_SHARE"),
    H_SHARE("H_SHARE"),
    US("US"),
    OTHER("OTHER");

    private final String value;

    StockMarketType(String value) { this.value = value; }

    public String getValue() { return value; }

    public static StockMarketType fromCode(String value) {
        if (value == null || value.isBlank()) return null;
        for (StockMarketType item : values()) if (item.value.equalsIgnoreCase(value.trim())) return item;
        throw new IllegalArgumentException("不支持的股票市场类型: " + value);
    }
}
