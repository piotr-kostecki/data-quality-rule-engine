package com.example.dq.engine;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Selects the rules of a run. Filters compose with {@link #and}, {@link #or} and {@link #negate}. Whatever a filter
 * selects, the engine additionally keeps {@link RuleStatus#RELEASED} rules only.
 */
@FunctionalInterface
public interface RuleFilter {

    boolean test(Rule rule);

    static RuleFilter all() {
        return rule -> true;
    }

    /** Rules with one of the given statuses. */
    static RuleFilter status(RuleStatus... statuses) {
        Set<RuleStatus> wanted = Set.of(statuses);
        return rule -> wanted.contains(rule.status());
    }

    /** Rules that have at least one of the given categories. */
    static RuleFilter category(String... categories) {
        Set<String> wanted = Set.of(categories);
        return rule -> rule.categories().stream().anyMatch(wanted::contains);
    }

    /**
     * Rules that can apply to records from at least one of the given countries: {@code WORLD} rules and rules
     * scoped to one of them. Narrows a run; each record still only gets the rules of its own country.
     */
    static RuleFilter country(String... countries) {
        Set<String> wanted = Arrays.stream(countries).map(CountryScope::normalize).collect(Collectors.toUnmodifiableSet());
        return rule -> wanted.stream().anyMatch(rule.countryScope()::includes);
    }

    default RuleFilter and(RuleFilter other) {
        Objects.requireNonNull(other, "other");
        return rule -> test(rule) && other.test(rule);
    }

    default RuleFilter or(RuleFilter other) {
        Objects.requireNonNull(other, "other");
        return rule -> test(rule) || other.test(rule);
    }

    default RuleFilter negate() {
        return rule -> !test(rule);
    }
}
