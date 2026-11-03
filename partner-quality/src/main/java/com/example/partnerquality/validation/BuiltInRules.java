package com.example.partnerquality.validation;

import com.example.partnerquality.internal.rules.CountryCodeRule;
import com.example.partnerquality.internal.rules.IbanRule;
import com.example.partnerquality.internal.rules.MandatoryFieldsRule;
import com.example.partnerquality.internal.rules.PostalCodeFormatRule;
import com.example.partnerquality.internal.rules.VatIdRule;

import java.util.List;

/**
 * Factory for the rules shipped with the library. The implementation classes are internal, so they can be
 * rewritten without breaking anyone; only these factory methods and the rule ids are API.
 */
public final class BuiltInRules {

    private BuiltInRules() {
    }

    /** All built-in rules, in a stable order. */
    public static List<DataQualityRule> all() {
        return List.of(mandatoryFields(), countryCode(), postalCodeFormat(), vatId(), iban());
    }

    /** Name, country and city are errors when missing; street and postal code are warnings. */
    public static DataQualityRule mandatoryFields() {
        return new MandatoryFieldsRule();
    }

    /** Country must be an ISO 3166-1 alpha-2 code. */
    public static DataQualityRule countryCode() {
        return new CountryCodeRule();
    }

    /** Postal code format for known countries; other countries are not checked. */
    public static DataQualityRule postalCodeFormat() {
        return new PostalCodeFormatRule();
    }

    /** VAT ID prefix, format and (for CH, DE, PL) check digit; a missing VAT ID is a warning. */
    public static DataQualityRule vatId() {
        return new VatIdRule();
    }

    /** Optional IBAN: structure, length and mod-97 check digits; a foreign bank account is a warning. */
    public static DataQualityRule iban() {
        return new IbanRule();
    }
}
