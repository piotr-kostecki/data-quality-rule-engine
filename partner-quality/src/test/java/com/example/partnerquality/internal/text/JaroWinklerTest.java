package com.example.partnerquality.internal.text;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class JaroWinklerTest {

    /** Reference values from Winkler's paper, used by most implementations as test vectors. */
    @ParameterizedTest(name = "{0} ~ {1} = {2}")
    @CsvSource({
            "MARTHA, MARHTA, 0.961",
            "DWAYNE, DUANE, 0.840",
            "DIXON, DICKSONX, 0.813"
    })
    void matchesReferenceValues(String a, String b, double expected) {
        assertThat(JaroWinkler.similarity(a, b)).isCloseTo(expected, within(0.001));
    }

    @Test
    void isSymmetric() {
        assertThat(JaroWinkler.similarity("acme chemicals", "acme chemical"))
                .isEqualTo(JaroWinkler.similarity("acme chemical", "acme chemicals"));
    }

    @Test
    void handlesEdgeCases() {
        assertThat(JaroWinkler.similarity("same", "same")).isEqualTo(1.0);
        assertThat(JaroWinkler.similarity("", "abc")).isEqualTo(0.0);
        assertThat(JaroWinkler.similarity("abc", "xyz")).isEqualTo(0.0);
    }
}
