package com.example.dq.engine;

import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Declarative mapping from a rule's computed value to a {@link Decision}: exact, case-sensitive matches plus a
 * default for every value that is not mapped. The mapping is data, so rule logic contains no if/else on decisions.
 *
 * @param cases           value to decision
 * @param defaultDecision decision for every value not in {@code cases}
 */
public record DecisionMapping(Map<String, Decision> cases, Decision defaultDecision) {

    /**
     * Default used when a rule gives none.
     *
     * <p>ASSUMPTION(A-1): an unexpected value goes to a human ({@code REVIEW}) instead of silently passing or
     * failing. Requires confirmation with architects / BA, see DESIGN.md §10.
     */
    public static final Decision IMPLICIT_DEFAULT = Decision.REVIEW;

    public DecisionMapping {
        cases = Map.copyOf(cases);
        Objects.requireNonNull(defaultDecision, "defaultDecision");
    }

    /** Mapping with an explicit default, or {@link #IMPLICIT_DEFAULT} when {@code defaultDecision} is {@code null}. */
    public static DecisionMapping of(Map<String, Decision> cases, @Nullable Decision defaultDecision) {
        return new DecisionMapping(cases, defaultDecision != null ? defaultDecision : IMPLICIT_DEFAULT);
    }

    /** Starts an ordered mapping: {@code DecisionMapping.when("ok", VALID).when("bad", INVALID).otherwise(REVIEW)}. */
    public static Builder when(String value, Decision decision) {
        return new Builder().when(value, decision);
    }

    public Decision decide(String value) {
        return cases.getOrDefault(value, defaultDecision);
    }

    /** Whether {@code value} is handled by the default rather than by an explicit case. */
    public boolean usesDefault(String value) {
        return !cases.containsKey(value);
    }

    /** Fluent construction of a {@link DecisionMapping}. */
    public static final class Builder {

        private final Map<String, Decision> cases = new LinkedHashMap<>();

        private Builder() {
        }

        public Builder when(String value, Decision decision) {
            if (cases.putIfAbsent(value, decision) != null) {
                throw new IllegalArgumentException("Value '" + value + "' is mapped twice");
            }
            return this;
        }

        public DecisionMapping otherwise(Decision defaultDecision) {
            return new DecisionMapping(cases, defaultDecision);
        }

        /** No explicit default: {@link #IMPLICIT_DEFAULT} applies. */
        public DecisionMapping withImplicitDefault() {
            return new DecisionMapping(cases, IMPLICIT_DEFAULT);
        }
    }
}
