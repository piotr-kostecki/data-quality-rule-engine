package com.example.partnerquality.validation;

import java.util.List;

/** All findings of one validation run. Immutable. */
public record ValidationReport(List<RuleViolation> violations) {

    public ValidationReport {
        violations = List.copyOf(violations);
    }

    public boolean hasErrors() {
        return violations.stream().anyMatch(RuleViolation::isError);
    }

    public List<RuleViolation> errors() {
        return violations.stream().filter(RuleViolation::isError).toList();
    }

    public List<RuleViolation> warnings() {
        return violations.stream().filter(v -> !v.isError()).toList();
    }
}
