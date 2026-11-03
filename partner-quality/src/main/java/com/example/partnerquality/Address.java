package com.example.partnerquality;

import com.example.partnerquality.internal.text.Text;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * A postal address. Every field is optional at this level; whether it is required is decided by rules.
 * Values are stripped, blank values become {@code null}, and the country is upper-cased.
 */
public record Address(
        @Nullable String street,
        @Nullable String postalCode,
        @Nullable String city,
        @Nullable String country) {

    private static final Address EMPTY = new Address(null, null, null, null);

    public Address {
        street = Text.clean(street);
        postalCode = Text.clean(postalCode);
        city = Text.clean(city);
        country = country == null ? null : Text.clean(country.toUpperCase(Locale.ROOT));
    }

    public static Address empty() {
        return EMPTY;
    }
}
