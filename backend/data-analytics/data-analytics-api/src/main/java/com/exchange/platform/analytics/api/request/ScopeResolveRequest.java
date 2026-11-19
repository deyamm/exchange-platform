package com.exchange.platform.analytics.api.request;

import java.util.List;

public record ScopeResolveRequest(String scopeType, List<String> scopeValues, Integer limit) {
    public int safeLimit() { return limit == null || limit < 1 ? 200 : Math.min(limit, 2000); }
}
