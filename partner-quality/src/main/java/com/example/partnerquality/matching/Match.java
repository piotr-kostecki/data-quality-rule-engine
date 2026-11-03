package com.example.partnerquality.matching;

import java.util.Objects;

/**
 * A candidate that may be the same company as the probe.
 *
 * @param candidate the caller's own object, returned unchanged
 * @param score     similarity in [0, 1]
 * @param reason    the strongest evidence for the match
 * @param <T>       the caller's entity type
 */
public record Match<T>(T candidate, double score, MatchReason reason) {

    public Match {
        Objects.requireNonNull(candidate, "candidate");
        Objects.requireNonNull(reason, "reason");
        if (score < 0 || score > 1) {
            throw new IllegalArgumentException("score must be in [0, 1], got " + score);
        }
    }
}
