package com.exchange.platform.collection.api.request.dataType;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public record DataTypeVersionPublishRequest(
    @NotNull Integer version,
    @Size(max = 128) String versionName,
    @Size(max = 500) String changeSummary
) {}
