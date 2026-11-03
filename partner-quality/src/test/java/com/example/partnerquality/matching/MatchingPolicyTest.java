package com.example.partnerquality.matching;

import com.example.partnerquality.Address;
import com.example.partnerquality.PartnerData;
import com.example.partnerquality.testing.PartnerFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class MatchingPolicyTest {

    /** Same name in another city: a possible duplicate under the default policy, scored between the thresholds. */
    private static final PartnerData SAME_NAME_ELSEWHERE =
            new PartnerData("Acme Chemicals GmbH", new Address(null, "80331", "München", "DE"), null, null);

    @ParameterizedTest(name = "candidate {0}, blocking {1}")
    @CsvSource({
            "-0.1, 0.9",   // below 0
            "0.5, 1.1",    // above 1
            "0.9, 0.8"     // candidate above blocking
    })
    void rejectsInconsistentThresholds(double candidate, double blocking) {
        assertThatIllegalArgumentException().isThrownBy(() -> new MatchingPolicy(candidate, blocking));
    }

    @Test
    void acceptsTheFullRangeAndEqualThresholds() {
        assertThat(new MatchingPolicy(0, 1)).isNotNull();
        assertThat(new MatchingPolicy(0.9, 0.9)).isNotNull();
    }

    @Test
    void thresholdsDecideWhetherACandidateIsReportedOrBlocks() {
        Match<PartnerData> match = matchUnder(MatchingPolicy.DEFAULT);
        assertThat(DuplicateDetector.withDefaultPolicy().isBlocking(match)).isFalse();

        DuplicateDetector strict = new DuplicateDetector(new MatchingPolicy(match.score(), match.score()));
        assertThat(strict.isBlocking(matchUnder(strict.policy()))).isTrue();

        DuplicateDetector lenient = new DuplicateDetector(new MatchingPolicy(Math.min(1.0, match.score() + 0.01), 1.0));
        assertThat(lenient.findMatches(SAME_NAME_ELSEWHERE, List.of(PartnerFixtures.acme()))).isEmpty();
    }

    private static Match<PartnerData> matchUnder(MatchingPolicy policy) {
        List<Match<PartnerData>> matches =
                new DuplicateDetector(policy).findMatches(SAME_NAME_ELSEWHERE, List.of(PartnerFixtures.acme()));
        assertThat(matches).hasSize(1);
        return matches.getFirst();
    }
}
