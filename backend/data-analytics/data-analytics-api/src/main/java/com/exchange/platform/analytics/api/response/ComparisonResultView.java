package com.exchange.platform.analytics.api.response;

import java.util.List;

import lombok.Data;

import com.exchange.platform.common.core.enums.AssetType;

@Data
public class ComparisonResultView {
    private AssetType targetType;
    private List<ComparisonTargetView> targets;
    private List<ComparisonMetricRowView> rows;
}
