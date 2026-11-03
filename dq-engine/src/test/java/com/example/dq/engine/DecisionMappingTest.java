package com.example.dq.engine;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class DecisionMappingTest {

    private final DecisionMapping mapping = DecisionMapping.when("blocked", Decision.INVALID).otherwise(Decision.NOT_APPLICABLE);

    @Test
    void mapsAnExplicitCase() {
        assertThat(mapping.decide("blocked")).isEqualTo(Decision.INVALID);
        assertThat(mapping.usesDefault("blocked")).isFalse();
    }

    @Test
    void mapsEverythingElseToTheDefault() {
        assertThat(mapping.decide("ok")).isEqualTo(Decision.NOT_APPLICABLE);
        assertThat(mapping.decide("BLOCKED")).as("matching is case-sensitive").isEqualTo(Decision.NOT_APPLICABLE);
        assertThat(mapping.usesDefault("ok")).isTrue();
    }

    @Test
    void A1_unmappedValueWithoutDefaultIsReview() {
        DecisionMapping withoutDefault = DecisionMapping.when("ok", Decision.VALID).when("bad", Decision.INVALID).withImplicitDefault();

        assertThat(withoutDefault.decide("unexpected")).isEqualTo(Decision.REVIEW);
        assertThat(DecisionMapping.of(Map.of("ok", Decision.VALID), null).defaultDecision()).isEqualTo(Decision.REVIEW);
    }

    @Test
    void rejectsAValueMappedTwice() {
        assertThatIllegalArgumentException().isThrownBy(() -> DecisionMapping.when("ok", Decision.VALID).when("ok", Decision.INVALID));
    }
}
