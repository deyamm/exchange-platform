package com.exchange.platform.analytics.api.registry;

import java.util.List;

/** Declarative validation rule understood by the generic publisher. */
public record ValidationRule(
    String type, 
    List<String> arguments
) {
    public ValidationRule {
        arguments = arguments == null ? List.of() : List.copyOf(arguments);
    }
}
