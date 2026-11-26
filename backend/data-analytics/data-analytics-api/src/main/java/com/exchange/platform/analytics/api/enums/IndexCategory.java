package com.exchange.platform.analytics.api.enums;

public enum IndexCategory {
    // 与 data-server/app/core/common/enum.py 中的 IndexCategory 保持一致
    COMPOSITE("COMPOSITE"),
    SIZE("SIZE"),
    STYLE("STYLE"), 
    THEMATIC("THEMATIC"),
    INDUSTRY("INDUSTRY"), 
    STRATEGY("STRATEGY"),
    GROWTH("GROWTH"),
    VALUE("VALUE"),
    CONCEPT("CONCEPT"), 
    OTHER("OTHER");

    private final String value;

    IndexCategory(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static IndexCategory fromCode(String value) {
        if (value == null || value.isBlank())
            return null;
        for (IndexCategory item : values())
            if (item.value.equalsIgnoreCase(value.trim()))
                return item;
        throw new IllegalArgumentException("不支持的指数分类: " + value);
    }
}
