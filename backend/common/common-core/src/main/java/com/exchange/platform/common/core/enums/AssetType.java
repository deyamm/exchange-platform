package com.exchange.platform.common.core.enums;

/**
 * 跨采集和数据分析模块使用的标的类型协议。
 * <p>数据库、接口和跨服务消息只保存枚举编码；中文名称仅供界面展示，禁止以 ordinal 持久化。</p>
 */
public enum AssetType {
    STOCK("STOCK", "股票"),
    INDEX("INDEX", "指数"),
    FUTURE("FUTURE", "期货"),
    FUND("FUND", "基金"),
    BOND("BOND", "债券");

    private final String code;
    private final String displayName;

    AssetType(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getValue() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** 严格从跨语言协议编码解析；未知值必须让同步或保存操作失败。 */
    public static AssetType fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        try {
            return AssetType.valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("不支持的资产类型: " + code, ex);
        }
    }
}
