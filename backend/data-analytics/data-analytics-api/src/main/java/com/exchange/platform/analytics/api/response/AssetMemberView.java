package com.exchange.platform.analytics.api.response;

import java.time.LocalDateTime;

public record AssetMemberView(
        String parentAssetCode, 
        String memberAssetCode, 
        LocalDateTime validFrom,
        LocalDateTime validTo, 
        Long batchId) {
}
