package com.exchange.platform.analytics.api.enums;

public enum PublisherCode {
    SSE("SSE"),
    SZSE("SZSE"),
    CSI("CSI"), 
    CNI("CNI"),
    CICC("CICC"),
    SW("SW"), 
    MSCI("MSCI"),
    THS("THS"), 
    EM("EM"), 
    OTHER("OTHER");

    private final String value;

    PublisherCode(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
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
