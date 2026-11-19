package com.exchange.platform.analytics.api.request;

public record BindingPublishRequest(Long bindingId, String taskRunId, String syncType) { }
