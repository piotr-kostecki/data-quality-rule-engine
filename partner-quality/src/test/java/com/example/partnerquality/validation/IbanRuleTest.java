package com.example.partnerquality.validation;

import com.example.partnerquality.Address;
import com.example.partnerquality.PartnerData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IbanRuleTest {

    private final DataQualityRule rule = BuiltInRules.iban();

    @ParameterizedTest
    @ValueSource(strings = {
            "DE89370400440532013000",
            "DE89 3704 0044 0532 0130 00",
            "PL61109010140000071219812874",
            "CH9300762011623852957",
            "GB82WEST12345698765432"
    })
    void acceptsValidIbans(String iban) {
        String country = iban.substring(0, 2);
        assertThat(check(iban, country)).isEmpty();
    }

    @ParameterizedTest(name = "{0}: {1}")
    @CsvSource({
            "DE89370400440532013001, invalid check digits",
            "DE88370400440532013000, invalid check digits",
            "GB82WEST12345698765433, invalid check digits",
            "DE8937040044053201300, must have 22 characters",
            "D89370400440532013000, invalid structure"
    })
    void rejectsInvalidIbans(String iban, String expectedMessage) {
        assertThat(check(iban, "DE"))
                .singleElement()
                .satisfies(v -> {
                    assertThat(v.severity()).isEqualTo(Severity.ERROR);
                    assertThat(v.message()).contains(expectedMessage);
                });
    }

    @Test
    void flagsForeignBankAccountAsWarning() {
        assertThat(check("CH9300762011623852957", "DE"))
                .singleElement()
                .extracting(RuleViolation::severity)
                .isEqualTo(Severity.WARNING);
    }

    @Test
    void ibanIsOptional() {
        assertThat(check(null, "DE")).isEmpty();
    }

    private List<RuleViolation> check(String iban, String country) {
        return rule.check(new PartnerData("Any", new Address("Street 1", null, "City", country), null, iban));
    }
}
