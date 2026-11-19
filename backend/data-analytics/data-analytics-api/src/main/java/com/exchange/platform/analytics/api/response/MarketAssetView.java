package com.exchange.platform.analytics.api.response;

import java.time.LocalDate;
import com.exchange.platform.analytics.api.enums.IndexCategory;
import com.exchange.platform.analytics.api.enums.StockMarketType;
import com.exchange.platform.common.core.enums.MarketAssetType;
import com.exchange.platform.analytics.api.enums.MasterDataStatus;

// TODO: 当前是将股票和指数的字段都放在了这个类里，后续随着规模的扩大，需要拆分成不同的类
public record MarketAssetView(
        Long id, 
        MarketAssetType marketAssetType,
        String assetCode, 
        String assetName, 
        MasterDataStatus status,
        StockMarketType marketType, 
        String exchangeCode, 
        String currencyCode, 
        LocalDate listDate, 
        LocalDate delistDate,
        String publisherCode, 
        IndexCategory indexCategory, 
        String schemeCode, 
        Integer level,
        LocalDate launchDate, 
        LocalDate endDate
) { }
