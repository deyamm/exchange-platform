package com.exchange.platform.analytics.api.response;

import lombok.Data;

@Data
public class ComparisonTargetView {
    private String targetCode;
    private String targetName;

    public ComparisonTargetView() { }
    public ComparisonTargetView(String targetCode, String targetName) {
        this.targetCode = targetCode;
        this.targetName = targetName;
    }
}
