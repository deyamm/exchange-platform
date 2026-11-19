package com.exchange.platform.analytics.api.request;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import com.exchange.platform.analytics.api.enums.MasterDataType;

public record SourceBindingSaveRequest(@NotBlank String bindingCode, @NotBlank String bindingName,
        @NotNull MasterDataType masterDataType, @NotBlank String datasetCode, String triggerMode,
        Boolean enabled, String description) { }
