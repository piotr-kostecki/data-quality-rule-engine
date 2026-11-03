package com.example.partnerquality.internal.text;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class NameNormalizerTest {

    @ParameterizedTest(name = "''{0}'' -> ''{1}''")
    @CsvSource({
            "Acme Chemicals GmbH, acme chemicals",
            "ACME-Chemicals G.m.b.H., acme chemicals",
            "Müller Logistik GmbH & Co. KG, mueller logistik",
            "Przykład Logistyka sp. z o.o., przyklad logistyka",
            "Łódź Trading S.A., lodz trading",
            "Alpen Präzision AG, alpen praezision",
            "AG, ag"
    })
    void removesNoiseAndLegalForms(String input, String expected) {
        assertThat(NameNormalizer.normalize(input)).isEqualTo(expected);
    }
}
