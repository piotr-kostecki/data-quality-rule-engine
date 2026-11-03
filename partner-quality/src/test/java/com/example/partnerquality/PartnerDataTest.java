package com.example.partnerquality;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class PartnerDataTest {

    @Test
    void normalizesInputOnConstruction() {
        PartnerData data = new PartnerData("  Acme  ", new Address(" ", null, "Berlin ", "de"), "de 136.695-976", " ");

        assertThat(data.name()).isEqualTo("Acme");
        assertThat(data.address().street()).isNull();
        assertThat(data.address().city()).isEqualTo("Berlin");
        assertThat(data.address().country()).isEqualTo("DE");
        assertThat(data.vatId()).isEqualTo("DE136695976");
        assertThat(data.iban()).isNull();
    }

    @Test
    void addressIsRequiredUseEmptyInstead() {
        assertThatNullPointerException()
                .isThrownBy(() -> new PartnerData("Acme", null, null, null))
                .withMessageContaining("Address.empty()");
    }
}
