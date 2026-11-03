package com.example.dq.engine;

import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** Which records a rule applies to: every record ({@link World}) or records from given countries. */
public sealed interface CountryScope {

    /** Whether a record with this country (already normalized, {@code null} if absent) is in scope. */
    boolean includes(@Nullable String country);

    static CountryScope world() {
        return World.INSTANCE;
    }

    /** Scope limited to the given ISO country codes; codes are trimmed and upper-cased. */
    static CountryScope of(String... countries) {
        return new Countries(Arrays.stream(countries).map(CountryScope::normalize).collect(Collectors.toUnmodifiableSet()));
    }

    /** Trims and upper-cases a country code, the form used for every scope comparison. */
    static String normalize(String country) {
        return country.strip().toUpperCase(Locale.ROOT);
    }

    /** Applies to every record, including records without a country. */
    enum World implements CountryScope {
        INSTANCE;

        @Override
        public boolean includes(@Nullable String country) {
            return true;
        }
    }

    /**
     * Applies to records whose country is one of {@code countries}.
     *
     * @param countries normalized ISO country codes, at least one
     */
    record Countries(Set<String> countries) implements CountryScope {

        public Countries {
            countries = Set.copyOf(countries);
            if (countries.isEmpty()) {
                throw new IllegalArgumentException("A country scope needs at least one country; use world() otherwise");
            }
        }

        @Override
        public boolean includes(@Nullable String country) {
            return country != null && countries.contains(country);
        }
    }
}
