package com.exchange.platform.analytics.api.enums;

public enum ExchangeType {
    SSE("SSE", "上交所"),
    SZSE("SZSE", "深交所"),
    BSE("BSE", "北交所"),
    OTHER("OTHER", "其他");

    private final String value;
    private final String exchangeName;

    ExchangeType(String value, String exchangeName) {
        this.value = value;
        this.exchangeName = exchangeName;
    }

    public String getValue() {
        return value;
    }

    public String getExchangeName() {
        return exchangeName;
    }

    public static ExchangeType fromCode(String value) {
        if (value == null || value.isBlank()) 
            return null;
        for (ExchangeType item : values()) 
            if (item.value.equalsIgnoreCase(value.trim())) 
                return item;
        throw new IllegalArgumentException("不支持的交易所类型: " + value);
    }
}
