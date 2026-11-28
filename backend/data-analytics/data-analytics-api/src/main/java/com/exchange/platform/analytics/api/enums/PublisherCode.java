package com.exchange.platform.analytics.api.enums;

public enum PublisherCode {
    SSE("SSE", "上交所"),
    SZSE("SZSE", "深交所"),
    CSI("CSI", "中证"),
    CNI("CNI", "国证"),
    CICC("CICC", "中金"),
    SW("SW", "申万"), 
    MSCI("MSCI", "明晟"),
    THS("THS", "同花顺"),
    EM("EM", "东方财富"),
    OTHER("OTHER", "其他");

    private final String value;
    private final String name;

    PublisherCode(String value, String name) {
        this.value = value;
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    public static PublisherCode fromCode(String value) {
        if (value == null || value.isBlank())
            return null;
        for (PublisherCode item : values())
            if (item.value.equalsIgnoreCase(value.trim()))
                return item;
        throw new IllegalArgumentException("不支持的指数发布方: " + value);
    }
}
