package com.example.dq.engine;

import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A named data-quality check: {@link RuleLogic} computes a value from a record, and a {@link DecisionMapping}
 * turns that value into a {@link Decision}. Immutable; create with {@link #builder(String)}.
 */
public final class Rule {

    private final String id;
    private final String label;
    private final RuleLogic logic;
    private final DecisionMapping mapping;
    private final RuleStatus status;
    private final Severity severity;
    private final CountryScope countryScope;
    private final Set<String> categories;

    private Rule(Builder builder) {
        this.id = builder.id;
        this.label = builder.label != null ? builder.label : builder.id;
        this.logic = Objects.requireNonNull(builder.logic, () -> "Rule " + builder.id + " has no logic");
        this.mapping = Objects.requireNonNull(builder.mapping, () -> "Rule " + builder.id + " has no decision mapping");
        this.status = builder.status;
        this.severity = builder.severity;
        this.countryScope = builder.countryScope;
        this.categories = Set.copyOf(builder.categories);
    }

    /** Starts a rule with a stable id. Defaults: status {@code DRAFT}, severity {@code ERROR}, scope world. */
    public static Builder builder(String id) {
        return new Builder(id);
    }

    /** Stable identifier, reported in every result and error. */
    public String id() {
        return id;
    }

    /** Human-readable name. */
    public String label() {
        return label;
    }

    public RuleLogic logic() {
        return logic;
    }

    public DecisionMapping mapping() {
        return mapping;
    }

    public RuleStatus status() {
        return status;
    }

    public Severity severity() {
        return severity;
    }

    public CountryScope countryScope() {
        return countryScope;
    }

    /** Zero or more categories, e.g. {@code tax}. */
    public Set<String> categories() {
        return categories;
    }

    @Override
    public String toString() {
        return "Rule[" + id + ", " + status + ", " + severity + "]";
    }

    /** Builder for {@link Rule}. Not thread-safe. */
    public static final class Builder {

        private final String id;
        private @Nullable String label;
        private @Nullable RuleLogic logic;
        private @Nullable DecisionMapping mapping;
        private RuleStatus status = RuleStatus.DRAFT;
        private Severity severity = Severity.ERROR;
        private CountryScope countryScope = CountryScope.world();
        private Set<String> categories = Set.of();

        private Builder(String id) {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("Rule id must not be blank");
            }
            this.id = id;
        }

        public Builder label(String label) {
            this.label = label;
            return this;
        }

        public Builder logic(RuleLogic logic) {
            this.logic = logic;
            return this;
        }

        public Builder mapping(DecisionMapping mapping) {
            this.mapping = mapping;
            return this;
        }

        public Builder status(RuleStatus status) {
            this.status = Objects.requireNonNull(status, "status");
            return this;
        }

        public Builder severity(Severity severity) {
            this.severity = Objects.requireNonNull(severity, "severity");
            return this;
        }

        public Builder countryScope(CountryScope countryScope) {
            this.countryScope = Objects.requireNonNull(countryScope, "countryScope");
            return this;
        }

        public Builder categories(String... categories) {
            this.categories = Set.of(categories).stream().map(String::strip).collect(Collectors.toUnmodifiableSet());
            return this;
        }

        public Rule build() {
            return new Rule(this);
        }
    }
}
