package com.example.partnerquality.matching;

/**
 * Tunable thresholds for duplicate detection.
 *
 * @param candidateThreshold score from which a candidate is reported as a possible duplicate
 * @param blockingThreshold  score from which a candidate counts as a certain duplicate
 */
public record MatchingPolicy(double candidateThreshold, double blockingThreshold) {

    /** 0.80 / 0.95: report generously, block only near-certain duplicates. */
    public static final MatchingPolicy DEFAULT = new MatchingPolicy(0.80, 0.95);

    public MatchingPolicy {
        if (candidateThreshold < 0 || blockingThreshold > 1 || candidateThreshold > blockingThreshold) {
            throw new IllegalArgumentException(
                    "Expected 0 <= candidateThreshold <= blockingThreshold <= 1, got %s and %s"
                            .formatted(candidateThreshold, blockingThreshold));
        }
    }
}
