package com.exchange.platform.analytics.service.support;

import lombok.Data;

/** 基础资料解析出的最小标的身份，避免筛选层依赖基础表的其它字段。 */
@Data
public class TargetScopeTarget {
    private String targetCode;
    private String targetName;
}
