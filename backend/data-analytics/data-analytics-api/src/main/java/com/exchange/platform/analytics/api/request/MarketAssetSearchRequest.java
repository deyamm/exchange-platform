package com.exchange.platform.analytics.api.request;

import com.exchange.platform.common.core.enums.MarketAssetType;

public record MarketAssetSearchRequest(String keyword, MarketAssetType marketAssetType, Integer limit) {
    public int safeLimit() { return limit == null || limit < 1 ? 20 : Math.min(limit, 200); }
}
