package com.example.partnerquality.validation;

/** How serious a {@link RuleViolation} is. */
public enum Severity {
    /** The data is wrong; it should not be accepted. */
    ERROR,
    /** The data is acceptable but should be reviewed or completed. */
    WARNING
}
