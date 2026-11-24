package com.exchange.platform.analytics.api.enums;

/** Stable market classification persisted as its protocol code. */
public enum StockMarketType {
    
    MAIN("MAIN", "主板"),
    CYB("CYB", "创业板"),
    KCB("KCB", "科创板"),
    BSE("BSE", "北交所"),
    OTHER("OTHER", "其他");

    private final String value;
    private final String marketName;

    StockMarketType(String value, String marketName) {
        this.value = value;
        this.marketName = marketName;
    }
    
    public String getValue() { 
        return value; 
    }

    public String getMarketName() { 
        return marketName; 
    }

    public static StockMarketType fromCode(String value) {
        if (value == null || value.isBlank()) 
            return null;
        for (StockMarketType item : values()) 
            if (item.value.equalsIgnoreCase(value.trim())) 
                return item;
        throw new IllegalArgumentException("不支持的股票市场类型: " + value);
    }
}
