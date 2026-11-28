package com.exchange.platform.analytics.api.enums;

public enum IndexCategory {
    // 与 data-server/app/core/common/enum.py 中的 IndexCategory 保持一致
    COMPOSITE("COMPOSITE", "综合指数"),
    SIZE("SIZE", "规模指数"),
    STYLE("STYLE", "风格指数"),
    THEMATIC("THEMATIC", "主题指数"),
    INDUSTRY("INDUSTRY", "行业指数"),
    STRATEGY("STRATEGY", "策略指数"),
    GROWTH("GROWTH", "成长指数"),
    VALUE("VALUE", "价值指数"),
    CONCEPT("CONCEPT", "概念指数"),
    OTHER("OTHER", "其他指数");

    private final String value;
    private final String name;

    IndexCategory(String value, String name) {
        this.value = value;
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public String getName() {
        return name;
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
