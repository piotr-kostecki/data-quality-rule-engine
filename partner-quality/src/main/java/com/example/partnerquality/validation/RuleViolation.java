package com.example.partnerquality.validation;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * One finding of one rule.
 *
 * @param ruleId   stable id of the rule that produced it, e.g. {@link RuleIds#IBAN}; safe to switch on
 * @param field    path of the offending field, e.g. {@code address.postalCode}; {@code null} if not field-specific
 * @param severity whether the finding should block acceptance
 * @param message  human-readable explanation; wording may change between releases
 */
public record RuleViolation(String ruleId, @Nullable String field, Severity severity, String message) {

    public RuleViolation {
        Objects.requireNonNull(ruleId, "ruleId");
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(message, "message");
    }

    public static RuleViolation error(String ruleId, @Nullable String field, String message) {
        return new RuleViolation(ruleId, field, Severity.ERROR, message);
    }

    public static RuleViolation warning(String ruleId, @Nullable String field, String message) {
        return new RuleViolation(ruleId, field, Severity.WARNING, message);
    }

    public boolean isError() {
        return severity == Severity.ERROR;
    }
}
