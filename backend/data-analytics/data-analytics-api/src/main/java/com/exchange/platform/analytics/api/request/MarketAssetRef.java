package com.exchange.platform.analytics.api.request;

import com.exchange.platform.common.core.enums.MarketAssetType;

public record MarketAssetRef(
    MarketAssetType marketAssetType,
    String assetCode
) { }
