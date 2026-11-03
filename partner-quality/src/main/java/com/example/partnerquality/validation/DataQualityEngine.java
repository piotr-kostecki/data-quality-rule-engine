package com.example.partnerquality.validation;

import com.example.partnerquality.PartnerData;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Runs a fixed set of rules and collects every finding, so a client can fix everything in one go.
 * Immutable and thread-safe; create it once and share it.
 */
public final class DataQualityEngine {

    private final List<DataQualityRule> rules;

    private DataQualityEngine(List<? extends DataQualityRule> rules) {
        this.rules = List.copyOf(rules);
    }

    public static DataQualityEngine withBuiltInRules() {
        return new DataQualityEngine(BuiltInRules.all());
    }

    public static DataQualityEngine of(List<? extends DataQualityRule> rules) {
        return new DataQualityEngine(rules);
    }

    /** Returns a new engine with the given rules added after the current ones. */
    public DataQualityEngine with(DataQualityRule... additionalRules) {
        return new DataQualityEngine(Stream.concat(rules.stream(), Stream.of(additionalRules)).toList());
    }

    public List<String> ruleIds() {
        return rules.stream().map(DataQualityRule::id).toList();
    }

    public ValidationReport validate(PartnerData data) {
        Objects.requireNonNull(data, "data");
        return new ValidationReport(rules.stream()
                .flatMap(rule -> safeCheck(rule, data).stream())
                .toList());
    }

    /**
     * A failing rule must not hide the results of all other rules. It is reported as an error, so
     * questionable data is never accepted just because a check crashed (fail closed).
     */
    private static List<RuleViolation> safeCheck(DataQualityRule rule, PartnerData data) {
        try {
            return rule.check(data);
        } catch (RuntimeException e) {
            return List.of(RuleViolation.error(RuleIds.RULE_FAILURE, null,
                    "Rule %s failed: %s".formatted(rule.id(), e)));
        }
    }
}
