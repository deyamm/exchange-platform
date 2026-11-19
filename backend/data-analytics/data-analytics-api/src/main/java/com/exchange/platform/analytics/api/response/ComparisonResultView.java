package com.exchange.platform.analytics.api.response;

import java.util.List;

import lombok.Data;

import com.exchange.platform.common.core.enums.MarketAssetType;

@Data
public class ComparisonResultView {
    private MarketAssetType targetType;
    private List<ComparisonTargetView> targets;
    private List<ComparisonMetricRowView> rows;
}
