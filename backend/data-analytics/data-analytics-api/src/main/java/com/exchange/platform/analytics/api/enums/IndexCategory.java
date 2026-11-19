package com.exchange.platform.analytics.api.enums;

public enum IndexCategory {
    BROAD("BROAD"), INDUSTRY("INDUSTRY"), CONCEPT("CONCEPT"), THEME("THEME"), STYLE("STYLE"), STRATEGY("STRATEGY"), OTHER("OTHER");

    private final String value;
    IndexCategory(String value) { this.value = value; }
    public String getValue() { return value; }

    public static IndexCategory fromCode(String value) {
        if (value == null || value.isBlank()) return null;
        for (IndexCategory item : values()) if (item.value.equalsIgnoreCase(value.trim())) return item;
        throw new IllegalArgumentException("不支持的指数分类: " + value);
    }
}
