package com.example.partnerquality.validation;

import com.example.partnerquality.Address;
import com.example.partnerquality.PartnerData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class AddressRulesTest {

    @Test
    void mandatoryFieldsDistinguishErrorsFromWarnings() {
        PartnerData data = new PartnerData("  ", new Address(null, "", null, null), null, null);

        assertThat(BuiltInRules.mandatoryFields().check(data))
                .extracting(RuleViolation::field, RuleViolation::severity)
                .containsExactlyInAnyOrder(
                        tuple("name", Severity.ERROR),
                        tuple("address.country", Severity.ERROR),
                        tuple("address.city", Severity.ERROR),
                        tuple("address.street", Severity.WARNING),
                        tuple("address.postalCode", Severity.WARNING));
    }

    @Test
    void countryMustBeIsoAlpha2() {
        DataQualityRule rule = BuiltInRules.countryCode();
        assertThat(rule.check(dataIn("XX", null))).hasSize(1);
        assertThat(rule.check(dataIn("DEU", null))).hasSize(1);
        assertThat(rule.check(dataIn("de", null))).isEmpty();
    }

    @ParameterizedTest(name = "{1} in {0} valid={2}")
    @CsvSource({
            "DE, 10115, true",
            "DE, 6705, false",
            "PL, 50-001, true",
            "PL, 50001, false",
            "CH, 9000, true",
            "NL, 1012 AB, true",
            "GB, SW1A 1AA, true",
            "BR, anything-goes, true"
    })
    void postalCodeFormatPerCountry(String country, String postalCode, boolean valid) {
        assertThat(BuiltInRules.postalCodeFormat().check(dataIn(country, postalCode)).isEmpty()).isEqualTo(valid);
    }

    private static PartnerData dataIn(String country, String postalCode) {
        return new PartnerData("Any", new Address("Street 1", postalCode, "City", country), null, null);
    }
}
