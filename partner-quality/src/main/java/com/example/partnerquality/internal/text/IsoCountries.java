package com.example.partnerquality.internal.text;

import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.Set;

/** ISO 3166-1 alpha-2 country codes, as known to the JDK. */
public final class IsoCountries {

    private static final Set<String> CODES = Set.of(Locale.getISOCountries());

    private IsoCountries() {
    }

    public static boolean isValid(@Nullable String code) {
        return code != null && CODES.contains(code);
    }
}
