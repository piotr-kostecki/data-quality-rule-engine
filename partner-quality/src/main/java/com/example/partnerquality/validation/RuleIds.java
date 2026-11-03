package com.example.partnerquality.validation;

/**
 * Ids of the built-in rules. These strings are part of the public contract: they are never renamed,
 * only added or deprecated.
 */
public final class RuleIds {

    public static final String MANDATORY_FIELDS = "MANDATORY_FIELDS";
    public static final String COUNTRY_CODE = "COUNTRY_CODE";
    public static final String POSTAL_CODE_FORMAT = "POSTAL_CODE_FORMAT";
    public static final String VAT_ID = "VAT_ID";
    public static final String IBAN = "IBAN";
    /** Reported when a rule throws instead of returning violations. */
    public static final String RULE_FAILURE = "RULE_FAILURE";

    private RuleIds() {
    }
}
