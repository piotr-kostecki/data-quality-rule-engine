package com.example.partnerquality.validation;

import com.example.partnerquality.Address;
import com.example.partnerquality.PartnerData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Tested through the public factory, exactly as a consumer of the library would use the rule. */
class VatIdRuleTest {

    private final DataQualityRule rule = BuiltInRules.vatId();

    @ParameterizedTest(name = "{0} in {1} is valid")
    @CsvSource({
            "DE136695976, DE",
            "'DE 136 695 976', DE",
            "PL1234563218, PL",
            "PL-123-456-32-18, PL",
            "CHE-116.281.710 MWST, CH",
            "ATU12345678, AT",
            "EL123456789, GR"
    })
    void acceptsValidVatIds(String vatId, String country) {
        assertThat(check(vatId, country)).isEmpty();
    }

    @ParameterizedTest(name = "{0} in {1}: {2}")
    @CsvSource({
            "DE136695977, DE, invalid check digit",
            "PL1234563219, PL, invalid check digit",
            "CHE116281711, CH, invalid check digit",
            "DE12345, DE, invalid format",
            "PL1234563218, DE, does not match country",
            "GR123456789, GR, does not match country"
    })
    void rejectsInvalidVatIds(String vatId, String country, String expectedMessage) {
        assertThat(check(vatId, country))
                .singleElement()
                .satisfies(v -> {
                    assertThat(v.severity()).isEqualTo(Severity.ERROR);
                    assertThat(v.ruleId()).isEqualTo(RuleIds.VAT_ID);
                    assertThat(v.message()).contains(expectedMessage);
                });
    }

    @Test
    void missingVatIdIsOnlyAWarning() {
        assertThat(check(null, "DE"))
                .singleElement()
                .extracting(RuleViolation::severity)
                .isEqualTo(Severity.WARNING);
    }

    @Test
    void unknownPrefixDegradesToWarning() {
        assertThat(check("SE556677889901", "SE"))
                .singleElement()
                .satisfies(v -> assertThat(v.message()).contains("No format check"))
                .extracting(RuleViolation::severity)
                .isEqualTo(Severity.WARNING);
    }

    private List<RuleViolation> check(String vatId, String country) {
        return rule.check(new PartnerData("Any", new Address("Street 1", null, "City", country), vatId, null));
    }
}
