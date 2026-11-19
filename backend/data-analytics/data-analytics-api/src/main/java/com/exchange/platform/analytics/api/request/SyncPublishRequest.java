package com.exchange.platform.analytics.api.request;

public record SyncPublishRequest(String masterDataType, String sourceCode, String taskRunId, String datasetCode, String syncType) { }
