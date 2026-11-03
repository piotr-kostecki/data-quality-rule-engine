package com.example.partnerquality.matching;

import com.example.partnerquality.Address;
import com.example.partnerquality.PartnerData;
import com.example.partnerquality.internal.text.JaroWinkler;
import com.example.partnerquality.internal.text.NameNormalizer;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Scores a probe against existing records. Identifiers (VAT ID, IBAN) are strong evidence; without them
 * the score combines name similarity (85%) and location agreement (15%). Immutable and thread-safe.
 *
 * <p>The detector compares against whatever pool it is given. Narrowing the pool to plausible candidates
 * ("blocking", e.g. same country) is the caller's job, because only the caller knows its storage.
 */
public final class DuplicateDetector {

    private static final double SAME_VAT_ID_SCORE = 1.0;
    private static final double SAME_IBAN_SCORE = 0.90;
    private static final double NAME_WEIGHT = 0.85;
    private static final double LOCATION_WEIGHT = 1 - NAME_WEIGHT;

    private final MatchingPolicy policy;

    public DuplicateDetector(MatchingPolicy policy) {
        this.policy = Objects.requireNonNull(policy, "policy");
    }

    public static DuplicateDetector withDefaultPolicy() {
        return new DuplicateDetector(MatchingPolicy.DEFAULT);
    }

    public MatchingPolicy policy() {
        return policy;
    }

    /**
     * Returns the candidates scoring at least {@link MatchingPolicy#candidateThreshold()}, best match first.
     *
     * @param probe  the data to look for
     * @param pool   existing records of the caller's type
     * @param dataOf extracts the partner data from a record
     */
    public <T> List<Match<T>> findMatches(PartnerData probe, Collection<? extends T> pool, Function<? super T, PartnerData> dataOf) {
        Objects.requireNonNull(probe, "probe");
        String probeName = NameNormalizer.normalize(probe.name());
        return pool.stream()
                .map(candidate -> bestMatch(probe, probeName, candidate, dataOf.apply(candidate)))
                .filter(match -> match.score() >= policy.candidateThreshold())
                .sorted(Comparator.comparingDouble(Match<T>::score).reversed())
                .toList();
    }

    /** Overload for callers whose pool is plain {@link PartnerData}. */
    public List<Match<PartnerData>> findMatches(PartnerData probe, Collection<PartnerData> pool) {
        return findMatches(probe, pool, Function.identity());
    }

    public boolean isBlocking(Match<?> match) {
        return match.score() >= policy.blockingThreshold();
    }

    private static <T> Match<T> bestMatch(PartnerData probe, String probeName, T candidate, PartnerData existing) {
        Match<T> byName = new Match<>(candidate,
                nameAndLocationScore(probeName, probe.address(), existing), MatchReason.SIMILAR_NAME_AND_LOCATION);
        return Stream.of(
                        sameIdentifier(probe.vatId(), existing.vatId(), candidate, SAME_VAT_ID_SCORE, MatchReason.SAME_VAT_ID),
                        sameIdentifier(probe.iban(), existing.iban(), candidate, SAME_IBAN_SCORE, MatchReason.SAME_IBAN),
                        Optional.of(byName))
                .flatMap(Optional::stream)
                .max(Comparator.comparingDouble(Match<T>::score))
                .orElseThrow();
    }

    private static <T> Optional<Match<T>> sameIdentifier(
            @Nullable String probeValue, @Nullable String existingValue, T candidate, double score, MatchReason reason) {
        return probeValue != null && probeValue.equals(existingValue)
                ? Optional.of(new Match<>(candidate, score, reason))
                : Optional.empty();
    }

    private static double nameAndLocationScore(String probeName, Address probeAddress, PartnerData existing) {
        double nameSimilarity = JaroWinkler.similarity(probeName, NameNormalizer.normalize(existing.name()));
        return NAME_WEIGHT * nameSimilarity + LOCATION_WEIGHT * locationAgreement(probeAddress, existing.address());
    }

    /** 1.0 same postal code and city, 0.7 same city, 0.5 unknown, 0.0 different city. */
    static double locationAgreement(Address a, Address b) {
        if (a.city() == null || b.city() == null) {
            return 0.5;
        }
        if (!NameNormalizer.normalize(a.city()).equals(NameNormalizer.normalize(b.city()))) {
            return 0.0;
        }
        boolean samePostalCode = a.postalCode() != null && Objects.equals(
                a.postalCode().replace(" ", ""), Objects.toString(b.postalCode(), "").replace(" ", ""));
        return samePostalCode ? 1.0 : 0.7;
    }
}
