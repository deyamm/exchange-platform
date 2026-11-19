package com.exchange.platform.analytics.api.request;

import com.exchange.platform.analytics.api.enums.IndexCategory;
import com.exchange.platform.analytics.api.enums.StockMarketType;
import com.exchange.platform.common.core.enums.MarketAssetType;

public record MarketAssetListRequest(String keyword, MarketAssetType marketAssetType, StockMarketType marketType,
        String exchangeCode, String publisherCode, IndexCategory indexCategory, String schemeCode,
        Integer level, Integer pageNo, Integer pageSize) {
    public int safePageNo() {
        return pageNo == null || pageNo < 1 ? 1 : pageNo;
    }

    public int safePageSize() {
        return pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 200);
    }
}
