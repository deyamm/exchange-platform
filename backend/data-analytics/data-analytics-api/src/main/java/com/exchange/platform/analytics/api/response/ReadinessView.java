package com.exchange.platform.analytics.api.response;

import java.util.List;
import com.exchange.platform.analytics.api.enums.ReadinessStatus;

public record ReadinessView(ReadinessStatus status, List<String> errors) { }
